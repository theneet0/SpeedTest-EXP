package com.example.speedlab.engine

import com.example.speedlab.model.ServerMode
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.validationErrors
import com.example.speedlab.network.NetworkSnapshot
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.max
import kotlin.math.min

class NativeHttpSpeedTestEngine(
    private val networkProvider: () -> NetworkSnapshot,
    private val nanoTime: () -> Long = System::nanoTime,
) : SpeedTestEngine {
    private val activeConnections =
        Collections.newSetFromMap(ConcurrentHashMap<HttpURLConnection, Boolean>())

    @Volatile private var cancelled = false
    @Volatile private var lastHttpStatus: Int? = null

    override fun cancel() {
        cancelled = true
        activeConnections.toList().forEach(HttpURLConnection::disconnect)
        activeConnections.clear()
    }

    override fun run(request: SpeedTestRequest): Flow<SpeedTestEvent> = channelFlow {
        cancelled = false
        lastHttpStatus = null
        val startedAt = nanoTime()
        var download = TransferResult(0.0, 0.0, 0.0, 0L, emptyList())
        var upload = TransferResult(0.0, 0.0, 0.0, 0L, emptyList())

        send(SpeedTestEvent.StageChanged(TestStage.PREPARING))
        ensureNetwork(request.initialNetwork)
        request.settings.validationForEngine()

        val candidates = serversFor(request)
        val selected = if (request.settings.serverMode == ServerMode.AUTO) {
            send(SpeedTestEvent.StageChanged(TestStage.FINDING_SERVER))
            selectFastest(candidates, request.config.timeoutMillis)
        } else {
            val server = candidates.first()
            validate(server)
            server to singleProbe(server, request.config.timeoutMillis)
        }
        send(SpeedTestEvent.ServerSelected(selected.first, selected.second))

        send(SpeedTestEvent.StageChanged(TestStage.TESTING_LATENCY))
        val latency = measureLatency(selected.first, request.config.timeoutMillis)
        ensureNetwork(request.initialNetwork)
        send(SpeedTestEvent.LatencyMeasured(latency))

        if (request.settings.testMode != TestMode.UPLOAD) {
            send(SpeedTestEvent.StageChanged(TestStage.TESTING_DOWNLOAD))
            download = measureTransfer(
                direction = TransferDirection.DOWNLOAD,
                server = selected.first,
                request = request,
            ) { send(SpeedTestEvent.TransferUpdated(it)) }
        }

        if (request.settings.testMode != TestMode.DOWNLOAD) {
            send(SpeedTestEvent.StageChanged(TestStage.TESTING_UPLOAD))
            upload = measureTransfer(
                direction = TransferDirection.UPLOAD,
                server = selected.first,
                request = request,
            ) { send(SpeedTestEvent.TransferUpdated(it)) }
        }

        send(SpeedTestEvent.StageChanged(TestStage.FINISHING))
        ensureNetwork(request.initialNetwork)
        val elapsedMillis = (nanoTime() - startedAt) / 1_000_000L
        send(
            SpeedTestEvent.Completed(
                SpeedTestResult(
                    downloadMbps = download.finalMbps,
                    uploadMbps = upload.finalMbps,
                    pingMillis = latency.pingMillis,
                    jitterMillis = latency.jitterMillis,
                    peakMbps = max(download.peakMbps, upload.peakMbps),
                    transferredBytes = download.transferredBytes + upload.transferredBytes,
                    durationMillis = elapsedMillis,
                    server = selected.first,
                    network = request.initialNetwork,
                    connectionCount = request.config.parallelConnections,
                    sampleCount = download.samplesMbps.size + upload.samplesMbps.size,
                    lastHttpStatus = lastHttpStatus,
                ),
            ),
        )
    }.flowOn(Dispatchers.IO)

    private fun serversFor(request: SpeedTestRequest): List<SpeedServer> = when (request.settings.serverMode) {
        ServerMode.AUTO -> buildList {
            addAll(ServerCatalog.builtIn)
            if (
                request.settings.customDownloadUrl.startsWith("https://") &&
                request.settings.customUploadUrl.startsWith("https://")
            ) add(ServerCatalog.custom(request.settings))
        }
        ServerMode.MANUAL -> listOf(
            ServerCatalog.builtIn.firstOrNull { it.id == request.settings.manualServerId }
                ?: ServerCatalog.builtIn.first(),
        )
        ServerMode.CUSTOM -> listOf(ServerCatalog.custom(request.settings))
    }

    private suspend fun selectFastest(
        candidates: List<SpeedServer>,
        timeoutMillis: Int,
    ): Pair<SpeedServer, Double> = coroutineScope {
        val attempts = candidates.map { server ->
            async(Dispatchers.IO) {
                runCatching {
                    validate(server)
                    server to singleProbe(server, min(timeoutMillis, 4_000))
                }.getOrNull()
            }
        }.mapNotNull { it.await() }
        attempts.minByOrNull { it.second } ?: throw SpeedTestException.Unavailable()
    }

    private suspend fun measureLatency(server: SpeedServer, timeoutMillis: Int): LatencyResult {
        val probes = mutableListOf<Double>()
        repeat(5) {
            currentCoroutineContext().ensureActive()
            probes += singleProbe(server, timeoutMillis)
            if (it < 4) delay(80)
        }
        return LatencyResult(
            pingMillis = MeasurementMath.median(probes),
            jitterMillis = MeasurementMath.jitterMillis(probes),
        )
    }

    private suspend fun singleProbe(server: SpeedServer, timeoutMillis: Int): Double {
        val started = nanoTime()
        val connection = open(server.downloadUrl(1, started), timeoutMillis).apply {
            requestMethod = "GET"
        }
        activeConnections += connection
        try {
            checkCancelled()
            val status = connection.responseCode
            lastHttpStatus = status
            if (status !in 200..299) throw SpeedTestException.Http(status)
            connection.inputStream.use { it.read() }
            return (nanoTime() - started) / 1_000_000.0
        } catch (error: Throwable) {
            throw error.asSpeedTestException()
        } finally {
            activeConnections -= connection
            connection.disconnect()
        }
    }

    private suspend fun measureTransfer(
        direction: TransferDirection,
        server: SpeedServer,
        request: SpeedTestRequest,
        report: suspend (TransferMetrics) -> Unit,
    ): TransferResult = coroutineScope {
        val config = request.config
        val durationMillis = if (direction == TransferDirection.DOWNLOAD) {
            config.downloadDurationMillis
        } else {
            config.uploadDurationMillis
        }
        val stageBudget = config.budgetFor(request.settings.testMode)
        val warmupBudget = min(2L * 1_024 * 1_024, stageBudget / 10L)
        val warmupBytes = if (config.warmupMillis > 0 && warmupBudget > 0) {
            warmUp(direction, server, config.timeoutMillis, config.warmupMillis, warmupBudget)
        } else {
            0L
        }

        ensureNetwork(request.initialNetwork)
        val remaining = AtomicLong((stageBudget - warmupBytes).coerceAtLeast(0L))
        val transferred = AtomicLong(0L)
        val successes = AtomicInteger(0)
        val lastFailure = AtomicReference<Throwable?>(null)
        val started = nanoTime()
        val deadline = started + durationMillis * 1_000_000L

        val workers = List(config.parallelConnections) {
            async(Dispatchers.IO) {
                val buffer = ByteArray(min(64 * 1_024, config.chunkBytes).coerceAtLeast(8 * 1_024))
                while (nanoTime() < deadline && !cancelled) {
                    currentCoroutineContext().ensureActive()
                    val reserved = reserve(remaining, config.chunkBytes.toLong())
                    if (reserved <= 0L) break
                    try {
                        transferChunk(
                            direction = direction,
                            server = server,
                            bytes = reserved,
                            timeoutMillis = config.timeoutMillis,
                            buffer = buffer,
                            onBytes = transferred::addAndGet,
                        )
                        successes.incrementAndGet()
                    } catch (error: Throwable) {
                        if (error is CancellationException || cancelled) throw CancellationException()
                        lastFailure.compareAndSet(null, error)
                        delay(60)
                    }
                }
            }
        }

        val samples = mutableListOf<Double>()
        var previousBytes = 0L
        var previousTime = started
        var peak = 0.0

        while (nanoTime() < deadline && workers.any { it.isActive }) {
            checkCancelled()
            delay(config.samplingIntervalMillis)
            ensureNetwork(request.initialNetwork)
            val now = nanoTime()
            val total = transferred.get()
            val current = com.example.speedlab.model.mbpsFromBytes(total - previousBytes, now - previousTime)
            val average = com.example.speedlab.model.mbpsFromBytes(total, now - started)
            peak = max(peak, current)
            samples += current
            val progressByTime = (now - started).toDouble() / (deadline - started).toDouble()
            val progressByData = if (stageBudget > 0) {
                (warmupBytes + total).toDouble() / stageBudget.toDouble()
            } else 1.0
            report(
                TransferMetrics(
                    direction = direction,
                    currentMbps = current,
                    averageMbps = average,
                    peakMbps = peak,
                    progress = max(progressByTime, progressByData).coerceIn(0.0, 1.0).toFloat(),
                    transferredBytes = warmupBytes + total,
                    samplesMbps = samples.takeLast(120),
                ),
            )
            previousBytes = total
            previousTime = now
            if (remaining.get() <= 0L && workers.all { it.isCompleted }) break
        }

        workers.forEach { it.cancelAndJoin() }
        if (transferred.get() == 0L && successes.get() == 0) {
            throw (lastFailure.get() ?: SpeedTestException.Unavailable()).asSpeedTestException()
        }
        val final = MeasurementMath.robustThroughputMbps(samples)
        TransferResult(
            finalMbps = final,
            averageMbps = MeasurementMath.averageMbps(samples),
            peakMbps = peak,
            transferredBytes = warmupBytes + transferred.get(),
            samplesMbps = samples,
        )
    }

    private suspend fun warmUp(
        direction: TransferDirection,
        server: SpeedServer,
        timeoutMillis: Int,
        durationMillis: Long,
        budget: Long,
    ): Long {
        val transferred = AtomicLong(0L)
        val remaining = AtomicLong(budget)
        val buffer = ByteArray(32 * 1_024)
        val deadline = nanoTime() + durationMillis * 1_000_000L
        while (nanoTime() < deadline && remaining.get() > 0L) {
            currentCoroutineContext().ensureActive()
            val reserved = reserve(remaining, min(256L * 1_024, remaining.get()))
            if (reserved <= 0L) break
            transferChunk(
                direction,
                server,
                reserved,
                timeoutMillis,
                buffer,
                transferred::addAndGet,
            )
        }
        return transferred.get()
    }

    private fun transferChunk(
        direction: TransferDirection,
        server: SpeedServer,
        bytes: Long,
        timeoutMillis: Int,
        buffer: ByteArray,
        onBytes: (Long) -> Unit,
    ): Long {
        checkCancelled()
        val connection = when (direction) {
            TransferDirection.DOWNLOAD -> open(
                server.downloadUrl(bytes, nanoTime()),
                timeoutMillis,
            ).apply { requestMethod = "GET" }
            TransferDirection.UPLOAD -> open(server.uploadUrl, timeoutMillis).apply {
                requestMethod = "POST"
                doOutput = true
                setFixedLengthStreamingMode(bytes)
                setRequestProperty("Content-Type", "application/octet-stream")
            }
        }
        activeConnections += connection
        var actual = 0L
        try {
            when (direction) {
                TransferDirection.DOWNLOAD -> {
                    val status = connection.responseCode
                    lastHttpStatus = status
                    if (status !in 200..299) throw SpeedTestException.Http(status)
                    connection.inputStream.use { input ->
                        while (actual < bytes) {
                            checkCancelled()
                            val read = input.read(buffer, 0, min(buffer.size.toLong(), bytes - actual).toInt())
                            if (read < 0) break
                            actual += read
                            onBytes(read.toLong())
                        }
                    }
                }
                TransferDirection.UPLOAD -> {
                    connection.outputStream.use { output ->
                        while (actual < bytes) {
                            checkCancelled()
                            val count = min(buffer.size.toLong(), bytes - actual).toInt()
                            output.write(buffer, 0, count)
                            actual += count
                            onBytes(count.toLong())
                        }
                        output.flush()
                    }
                    val status = connection.responseCode
                    lastHttpStatus = status
                    if (status !in 200..299) throw SpeedTestException.Http(status)
                    runCatching { connection.inputStream.use { it.read(buffer) } }
                }
            }
            return actual
        } catch (error: Throwable) {
            if (cancelled) throw CancellationException("Speed test cancelled")
            throw error.asSpeedTestException()
        } finally {
            activeConnections -= connection
            connection.disconnect()
        }
    }

    private fun open(url: String, timeoutMillis: Int): HttpURLConnection {
        val parsed = try {
            URL(url)
        } catch (error: MalformedURLException) {
            throw SpeedTestException.InvalidServer("Malformed URL")
        }
        if (!parsed.protocol.equals("https", ignoreCase = true)) {
            throw SpeedTestException.InvalidServer("HTTPS is required")
        }
        return (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-store, no-cache")
            setRequestProperty("User-Agent", "SpeedLab/0.1 Android")
        }
    }

    private fun validate(server: SpeedServer) {
        if (
            server.name.isBlank() ||
            server.host.isBlank() ||
            !server.downloadUrlTemplate.startsWith("https://", ignoreCase = true) ||
            !server.uploadUrl.startsWith("https://", ignoreCase = true)
        ) throw SpeedTestException.InvalidServer("A name and two valid HTTPS endpoints are required")
    }

    private fun ensureNetwork(initial: NetworkSnapshot) {
        checkCancelled()
        val current = networkProvider()
        if (!current.available) throw SpeedTestException.NoInternet()
        if (initial.fingerprint != current.fingerprint) throw SpeedTestException.NetworkChanged()
    }

    private fun checkCancelled() {
        if (cancelled) throw CancellationException("Speed test cancelled")
    }

    private fun reserve(remaining: AtomicLong, requested: Long): Long {
        while (true) {
            val available = remaining.get()
            if (available <= 0L) return 0L
            val amount = min(available, requested)
            if (remaining.compareAndSet(available, available - amount)) return amount
        }
    }

    private fun Throwable.asSpeedTestException(): SpeedTestException = when (this) {
        is SpeedTestException -> this
        is UnknownHostException -> SpeedTestException.Dns(this)
        is SocketTimeoutException -> SpeedTestException.Timeout()
        is SSLException -> SpeedTestException.Tls(this)
        is IOException -> SpeedTestException.Unavailable(this)
        else -> SpeedTestException.Unavailable(this)
    }

    private fun com.example.speedlab.model.AppSettings.validationForEngine() {
        val error = validationErrors().firstOrNull()
        if (error != null) throw SpeedTestException.InvalidServer(error)
    }
}
