package com.example.speedlab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.speedlab.AppContainer
import com.example.speedlab.data.HistoryEntity
import com.example.speedlab.data.HistoryRepository
import com.example.speedlab.data.SettingsRepository
import com.example.speedlab.engine.SpeedServer
import com.example.speedlab.engine.SpeedTestEngine
import com.example.speedlab.engine.SpeedTestEvent
import com.example.speedlab.engine.SpeedTestException
import com.example.speedlab.engine.SpeedTestRequest
import com.example.speedlab.engine.SpeedTestResult
import com.example.speedlab.engine.TestStage
import com.example.speedlab.engine.TransferDirection
import com.example.speedlab.model.AppSettings
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.measurementConfig
import com.example.speedlab.model.validationErrors
import com.example.speedlab.network.NetworkMonitor
import com.example.speedlab.network.NetworkSnapshot
import com.example.speedlab.network.NetworkTransport
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiagnosticsState(
    val engineType: String = "Native HTTPS multi-stream",
    val endpoint: String = "",
    val connectionCount: Int = 0,
    val transferredBytes: Long = 0L,
    val durationMillis: Long = 0L,
    val httpStatus: Int? = null,
    val lastEngineError: String? = null,
    val sampleCount: Int = 0,
)

data class SpeedLabUiState(
    val settings: AppSettings = AppSettings(),
    val network: NetworkSnapshot = NetworkSnapshot(),
    val stage: TestStage = TestStage.IDLE,
    val isActive: Boolean = false,
    val currentMbps: Double = 0.0,
    val averageMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val pingMillis: Double = 0.0,
    val jitterMillis: Double = 0.0,
    val progress: Float = 0f,
    val transferredBytes: Long = 0L,
    val completedDownloadBytes: Long = 0L,
    val samplesMbps: List<Double> = emptyList(),
    val selectedServer: SpeedServer? = null,
    val serverSelectionLatencyMillis: Double = 0.0,
    val errorMessage: String? = null,
    val showCellularWarning: Boolean = false,
    val diagnostics: DiagnosticsState = DiagnosticsState(),
    val lastResult: SpeedTestResult? = null,
)

class SpeedLabViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val networkMonitor: NetworkMonitor,
    private val engine: SpeedTestEngine,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SpeedLabUiState())
    val state: StateFlow<SpeedLabUiState> = mutableState.asStateFlow()
    val history: StateFlow<List<HistoryEntity>> = historyRepository.records.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private var activeJob: Job? = null
    private var startedAtMillis: Long = 0L
    private val terminalSaved = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { value ->
                mutableState.value = mutableState.value.copy(settings = value)
            }
        }
        viewModelScope.launch {
            networkMonitor.updates.collectLatest { value ->
                mutableState.value = mutableState.value.copy(network = value)
            }
        }
    }

    fun saveSettings(value: AppSettings) {
        if (mutableState.value.isActive) return
        mutableState.value = mutableState.value.copy(settings = value, errorMessage = null)
        viewModelScope.launch { settingsRepository.save(value) }
    }

    fun selectMode(mode: TestMode) = saveSettings(mutableState.value.settings.copy(testMode = mode))

    fun requestStart() {
        if (activeJob?.isActive == true || mutableState.value.isActive) return
        val settings = mutableState.value.settings
        val errors = settings.validationErrors()
        if (errors.isNotEmpty()) {
            mutableState.value = mutableState.value.copy(errorMessage = errors.first())
            return
        }
        val network = networkMonitor.current()
        if (!network.available) {
            mutableState.value = mutableState.value.copy(
                network = network,
                stage = TestStage.FAILED,
                errorMessage = "No internet connection is available.",
            )
            return
        }
        if (settings.warnOnCellular && (network.transport == NetworkTransport.CELLULAR || network.metered)) {
            mutableState.value = mutableState.value.copy(showCellularWarning = true, network = network)
        } else {
            startNow(network)
        }
    }

    fun confirmCellularStart() {
        mutableState.value = mutableState.value.copy(showCellularWarning = false)
        startNow(networkMonitor.current())
    }

    fun dismissCellularWarning() {
        mutableState.value = mutableState.value.copy(showCellularWarning = false)
    }

    private fun startNow(network: NetworkSnapshot) {
        if (activeJob?.isActive == true) return
        if (!network.available) {
            mutableState.value = mutableState.value.copy(errorMessage = "The network is no longer available.")
            return
        }
        val settings = mutableState.value.settings
        terminalSaved.set(false)
        startedAtMillis = System.currentTimeMillis()
        mutableState.value = mutableState.value.copy(
            network = network,
            stage = TestStage.PREPARING,
            isActive = true,
            currentMbps = 0.0,
            averageMbps = 0.0,
            peakMbps = 0.0,
            downloadMbps = 0.0,
            uploadMbps = 0.0,
            pingMillis = 0.0,
            jitterMillis = 0.0,
            progress = 0f,
            transferredBytes = 0L,
            completedDownloadBytes = 0L,
            samplesMbps = emptyList(),
            selectedServer = null,
            errorMessage = null,
            lastResult = null,
            diagnostics = DiagnosticsState(connectionCount = settings.measurementConfig().parallelConnections),
        )
        activeJob = viewModelScope.launch {
            try {
                engine.run(
                    SpeedTestRequest(
                        settings = settings,
                        config = settings.measurementConfig(),
                        initialNetwork = network,
                    ),
                ).collect(::handleEvent)
            } catch (cancelled: CancellationException) {
                finishCancelled()
            } catch (error: Throwable) {
                finishFailed(error)
            }
        }
    }

    private suspend fun handleEvent(event: SpeedTestEvent) {
        when (event) {
            is SpeedTestEvent.StageChanged -> {
                mutableState.value = mutableState.value.copy(stage = event.stage)
            }
            is SpeedTestEvent.ServerSelected -> {
                mutableState.value = mutableState.value.copy(
                    selectedServer = event.server,
                    serverSelectionLatencyMillis = event.selectionLatencyMillis,
                    diagnostics = mutableState.value.diagnostics.copy(
                        endpoint = event.server.host,
                    ),
                )
            }
            is SpeedTestEvent.LatencyMeasured -> {
                mutableState.value = mutableState.value.copy(
                    pingMillis = event.result.pingMillis,
                    jitterMillis = event.result.jitterMillis,
                )
            }
            is SpeedTestEvent.TransferUpdated -> {
                val current = mutableState.value
                val both = current.settings.testMode == TestMode.BOTH
                val overallProgress = when {
                    !both -> event.metrics.progress
                    event.metrics.direction == TransferDirection.DOWNLOAD -> event.metrics.progress * 0.5f
                    else -> 0.5f + event.metrics.progress * 0.5f
                }
                mutableState.value = current.copy(
                    currentMbps = event.metrics.currentMbps,
                    averageMbps = event.metrics.averageMbps,
                    peakMbps = maxOf(current.peakMbps, event.metrics.peakMbps),
                    downloadMbps = if (event.metrics.direction == TransferDirection.DOWNLOAD) {
                        event.metrics.averageMbps
                    } else current.downloadMbps,
                    uploadMbps = if (event.metrics.direction == TransferDirection.UPLOAD) {
                        event.metrics.averageMbps
                    } else current.uploadMbps,
                    progress = overallProgress,
                    transferredBytes = if (event.metrics.direction == TransferDirection.DOWNLOAD) {
                        event.metrics.transferredBytes
                    } else {
                        current.completedDownloadBytes + event.metrics.transferredBytes
                    },
                    completedDownloadBytes = if (event.metrics.direction == TransferDirection.DOWNLOAD) {
                        event.metrics.transferredBytes
                    } else {
                        current.completedDownloadBytes
                    },
                    samplesMbps = event.metrics.samplesMbps,
                    diagnostics = current.diagnostics.copy(
                        transferredBytes = if (event.metrics.direction == TransferDirection.DOWNLOAD) {
                            event.metrics.transferredBytes
                        } else current.completedDownloadBytes + event.metrics.transferredBytes,
                        sampleCount = event.metrics.samplesMbps.size,
                    ),
                )
            }
            is SpeedTestEvent.Completed -> finishCompleted(event.result)
        }
    }

    private suspend fun finishCompleted(result: SpeedTestResult) {
        mutableState.value = mutableState.value.copy(
            stage = TestStage.COMPLETED,
            isActive = false,
            currentMbps = when (mutableState.value.settings.testMode) {
                TestMode.DOWNLOAD -> result.downloadMbps
                TestMode.UPLOAD, TestMode.BOTH -> result.uploadMbps
            },
            averageMbps = when (mutableState.value.settings.testMode) {
                TestMode.DOWNLOAD -> result.downloadMbps
                TestMode.UPLOAD, TestMode.BOTH -> result.uploadMbps
            },
            peakMbps = result.peakMbps,
            downloadMbps = result.downloadMbps,
            uploadMbps = result.uploadMbps,
            pingMillis = result.pingMillis,
            jitterMillis = result.jitterMillis,
            progress = 1f,
            transferredBytes = result.transferredBytes,
            lastResult = result,
            diagnostics = mutableState.value.diagnostics.copy(
                transferredBytes = result.transferredBytes,
                durationMillis = result.durationMillis,
                httpStatus = result.lastHttpStatus,
                sampleCount = result.sampleCount,
            ),
        )
        persistTerminal("COMPLETED", null)
        activeJob = null
    }

    fun cancelTest() {
        if (!mutableState.value.isActive) return
        engine.cancel()
        activeJob?.cancel()
        activeJob = null
        viewModelScope.launch { finishCancelled() }
    }

    private suspend fun finishCancelled() {
        if (!mutableState.value.isActive && mutableState.value.stage == TestStage.CANCELLED) return
        mutableState.value = mutableState.value.copy(
            stage = TestStage.CANCELLED,
            isActive = false,
            currentMbps = 0.0,
            errorMessage = "Test cancelled.",
            diagnostics = mutableState.value.diagnostics.copy(
                durationMillis = elapsedWallMillis(),
                lastEngineError = "User cancellation",
            ),
        )
        persistTerminal("CANCELLED", null)
        activeJob = null
    }

    private suspend fun finishFailed(error: Throwable) {
        val message = when (error) {
            is SpeedTestException -> error.message ?: "The speed test failed."
            else -> "The speed test stopped unexpectedly. Try another server or network."
        }
        mutableState.value = mutableState.value.copy(
            stage = TestStage.FAILED,
            isActive = false,
            currentMbps = 0.0,
            errorMessage = message,
            diagnostics = mutableState.value.diagnostics.copy(
                durationMillis = elapsedWallMillis(),
                lastEngineError = error::class.java.simpleName,
            ),
        )
        persistTerminal("FAILED", message)
        activeJob = null
    }

    private suspend fun persistTerminal(status: String, error: String?) {
        if (!terminalSaved.compareAndSet(false, true)) return
        val snapshot = mutableState.value
        val server = snapshot.selectedServer
        historyRepository.add(
            HistoryEntity(
                timestampMillis = startedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
                downloadMbps = snapshot.downloadMbps,
                uploadMbps = snapshot.uploadMbps,
                pingMillis = snapshot.pingMillis,
                jitterMillis = snapshot.jitterMillis,
                peakMbps = snapshot.peakMbps,
                serverName = server?.name ?: "Unselected",
                serverHost = server?.host.orEmpty(),
                networkType = snapshot.network.transport.name,
                testMode = snapshot.settings.testMode.name,
                transferredBytes = snapshot.transferredBytes,
                durationMillis = snapshot.lastResult?.durationMillis ?: elapsedWallMillis(),
                completionStatus = status,
                errorMessage = error,
            ),
        )
    }

    private fun elapsedWallMillis(): Long =
        if (startedAtMillis == 0L) 0L else (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)

    fun clearError() {
        mutableState.value = mutableState.value.copy(errorMessage = null)
    }

    fun deleteHistory(record: HistoryEntity) {
        viewModelScope.launch { historyRepository.delete(record) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    override fun onCleared() {
        engine.cancel()
        activeJob?.cancel()
        super.onCleared()
    }
}

class SpeedLabViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeedLabViewModel::class.java)) {
            return SpeedLabViewModel(
                settingsRepository = container.settingsRepository,
                historyRepository = container.historyRepository,
                networkMonitor = container.networkMonitor,
                engine = container.speedTestEngine,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
