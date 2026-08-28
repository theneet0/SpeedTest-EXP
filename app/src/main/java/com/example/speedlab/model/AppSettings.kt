package com.example.speedlab.model

import kotlin.math.roundToLong

enum class TestMode(val label: String) {
    DOWNLOAD("Download"),
    UPLOAD("Upload"),
    BOTH("Download + Upload"),
}

enum class SpeedUnit(val label: String, val symbol: String) {
    MBPS("Megabits per second", "Mbps"),
    MB_PER_SEC("Megabytes per second", "MB/s"),
    KBPS("Kilobits per second", "Kbps"),
    GBPS("Gigabits per second", "Gbps");

    fun fromMbps(value: Double): Double = when (this) {
        MBPS -> value
        MB_PER_SEC -> value / 8.0
        KBPS -> value * 1_000.0
        GBPS -> value / 1_000.0
    }
}

enum class TestProfile(val label: String, val summary: String) {
    DATA_SAVER("Data Saver", "Short tests with a 24 MB traffic cap"),
    BALANCED("Balanced", "Good everyday accuracy with controlled traffic"),
    ACCURATE("Accurate", "Longer multi-stream tests for stable connections"),
    CUSTOM("Custom", "Use the advanced limits below"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class ServerMode(val label: String) {
    AUTO("Automatic"),
    MANUAL("Manual"),
    CUSTOM("Custom"),
}

data class AppSettings(
    val testMode: TestMode = TestMode.BOTH,
    val speedUnit: SpeedUnit = SpeedUnit.MBPS,
    val profile: TestProfile = TestProfile.BALANCED,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val serverMode: ServerMode = ServerMode.AUTO,
    val manualServerId: String = "cloudflare-global",
    val customServerName: String = "My server",
    val customServerLocation: String = "",
    val customDownloadUrl: String = "",
    val customUploadUrl: String = "",
    val warnOnCellular: Boolean = true,
    val customDownloadSeconds: Int = 10,
    val customUploadSeconds: Int = 10,
    val customWarmupMillis: Int = 800,
    val customTimeoutSeconds: Int = 10,
    val customConnections: Int = 4,
    val customSamplingMillis: Int = 200,
    val customMaxDataMb: Int = 250,
    val customChunkKb: Int = 2_048,
)

data class MeasurementConfig(
    val downloadDurationMillis: Long,
    val uploadDurationMillis: Long,
    val warmupMillis: Long,
    val timeoutMillis: Int,
    val parallelConnections: Int,
    val samplingIntervalMillis: Long,
    val maxTransferredBytes: Long,
    val chunkBytes: Int,
) {
    fun budgetFor(mode: TestMode): Long = when (mode) {
        TestMode.BOTH -> maxTransferredBytes / 2L
        TestMode.DOWNLOAD, TestMode.UPLOAD -> maxTransferredBytes
    }
}

fun AppSettings.measurementConfig(): MeasurementConfig = when (profile) {
    TestProfile.DATA_SAVER -> MeasurementConfig(
        downloadDurationMillis = 5_000,
        uploadDurationMillis = 5_000,
        warmupMillis = 300,
        timeoutMillis = 6_000,
        parallelConnections = 2,
        samplingIntervalMillis = 250,
        maxTransferredBytes = 24L * 1_024 * 1_024,
        chunkBytes = 512 * 1_024,
    )
    TestProfile.BALANCED -> MeasurementConfig(
        downloadDurationMillis = 8_000,
        uploadDurationMillis = 8_000,
        warmupMillis = 600,
        timeoutMillis = 8_000,
        parallelConnections = 4,
        samplingIntervalMillis = 200,
        maxTransferredBytes = 160L * 1_024 * 1_024,
        chunkBytes = 2 * 1_024 * 1_024,
    )
    TestProfile.ACCURATE -> MeasurementConfig(
        downloadDurationMillis = 12_000,
        uploadDurationMillis = 12_000,
        warmupMillis = 1_000,
        timeoutMillis = 10_000,
        parallelConnections = 8,
        samplingIntervalMillis = 160,
        maxTransferredBytes = 512L * 1_024 * 1_024,
        chunkBytes = 4 * 1_024 * 1_024,
    )
    TestProfile.CUSTOM -> MeasurementConfig(
        downloadDurationMillis = customDownloadSeconds.coerceIn(2, 60) * 1_000L,
        uploadDurationMillis = customUploadSeconds.coerceIn(2, 60) * 1_000L,
        warmupMillis = customWarmupMillis.coerceIn(0, 5_000).toLong(),
        timeoutMillis = customTimeoutSeconds.coerceIn(2, 30) * 1_000,
        parallelConnections = customConnections.coerceIn(1, 16),
        samplingIntervalMillis = customSamplingMillis.coerceIn(100, 1_000).toLong(),
        maxTransferredBytes = customMaxDataMb.coerceIn(5, 2_048).toLong() * 1_024 * 1_024,
        chunkBytes = customChunkKb.coerceIn(64, 8_192) * 1_024,
    )
}

fun AppSettings.validationErrors(): List<String> = buildList {
    if (profile == TestProfile.CUSTOM) {
        if (customDownloadSeconds !in 2..60) add("Download duration must be 2–60 seconds.")
        if (customUploadSeconds !in 2..60) add("Upload duration must be 2–60 seconds.")
        if (customWarmupMillis !in 0..5_000) add("Warm-up must be 0–5000 ms.")
        if (customTimeoutSeconds !in 2..30) add("Timeout must be 2–30 seconds.")
        if (customConnections !in 1..16) add("Connections must be 1–16.")
        if (customSamplingMillis !in 100..1_000) add("Sampling must be 100–1000 ms.")
        if (customMaxDataMb !in 5..2_048) add("Data cap must be 5–2048 MB.")
        if (customChunkKb !in 64..8_192) add("Chunk size must be 64–8192 KB.")
    }
    if (serverMode == ServerMode.CUSTOM) {
        if (customServerName.isBlank()) add("Custom server name is required.")
        if (!customDownloadUrl.isSecureUrl()) add("A valid HTTPS download URL is required.")
        if (!customUploadUrl.isSecureUrl()) add("A valid HTTPS upload URL is required.")
    }
}

private fun String.isSecureUrl(): Boolean =
    startsWith("https://", ignoreCase = true) && length > "https://a.b".length

fun estimatedMaximumBytes(settings: AppSettings): Long {
    val config = settings.measurementConfig()
    val measurement = config.budgetFor(settings.testMode) *
        if (settings.testMode == TestMode.BOTH) 2 else 1
    val latencyAllowance = 32L * 1_024
    return (measurement + latencyAllowance).coerceAtMost(config.maxTransferredBytes + latencyAllowance)
}

fun durationEstimateMillis(settings: AppSettings): Long {
    val config = settings.measurementConfig()
    val measurement = when (settings.testMode) {
        TestMode.DOWNLOAD -> config.downloadDurationMillis
        TestMode.UPLOAD -> config.uploadDurationMillis
        TestMode.BOTH -> config.downloadDurationMillis + config.uploadDurationMillis
    }
    return measurement + config.warmupMillis + 2_500L
}

fun mbpsFromBytes(bytes: Long, elapsedNanos: Long): Double {
    if (bytes <= 0L || elapsedNanos <= 0L) return 0.0
    return bytes.toDouble() * 8.0 * 1_000.0 / elapsedNanos.toDouble()
}

fun estimateBytesForSpeed(mbps: Double, durationMillis: Long): Long =
    ((mbps.coerceAtLeast(0.0) * durationMillis.coerceAtLeast(0L)) / 8.0).roundToLong()

fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    return when {
        safe >= 1_073_741_824.0 -> "%.2f GB".format(safe / 1_073_741_824.0)
        safe >= 1_048_576.0 -> "%.1f MB".format(safe / 1_048_576.0)
        safe >= 1_024.0 -> "%.1f KB".format(safe / 1_024.0)
        else -> "${safe.toLong()} B"
    }
}
