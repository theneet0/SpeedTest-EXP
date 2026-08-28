package com.example.speedlab.engine

import com.example.speedlab.model.AppSettings
import com.example.speedlab.model.MeasurementConfig
import com.example.speedlab.network.NetworkSnapshot

enum class TestStage(val label: String) {
    IDLE("Ready"),
    PREPARING("Preparing"),
    FINDING_SERVER("Finding server"),
    TESTING_LATENCY("Testing latency"),
    TESTING_DOWNLOAD("Testing download"),
    TESTING_UPLOAD("Testing upload"),
    FINISHING("Finishing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    FAILED("Failed"),
}

enum class TransferDirection { DOWNLOAD, UPLOAD }

data class SpeedTestRequest(
    val settings: AppSettings,
    val config: MeasurementConfig,
    val initialNetwork: NetworkSnapshot,
)

data class LatencyResult(
    val pingMillis: Double,
    val jitterMillis: Double,
)

data class TransferMetrics(
    val direction: TransferDirection,
    val currentMbps: Double,
    val averageMbps: Double,
    val peakMbps: Double,
    val progress: Float,
    val transferredBytes: Long,
    val samplesMbps: List<Double>,
)

data class TransferResult(
    val finalMbps: Double,
    val averageMbps: Double,
    val peakMbps: Double,
    val transferredBytes: Long,
    val samplesMbps: List<Double>,
)

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMillis: Double,
    val jitterMillis: Double,
    val peakMbps: Double,
    val transferredBytes: Long,
    val durationMillis: Long,
    val server: SpeedServer,
    val network: NetworkSnapshot,
    val connectionCount: Int,
    val sampleCount: Int,
    val lastHttpStatus: Int?,
)

sealed interface SpeedTestEvent {
    data class StageChanged(val stage: TestStage) : SpeedTestEvent
    data class ServerSelected(val server: SpeedServer, val selectionLatencyMillis: Double) : SpeedTestEvent
    data class LatencyMeasured(val result: LatencyResult) : SpeedTestEvent
    data class TransferUpdated(val metrics: TransferMetrics) : SpeedTestEvent
    data class Completed(val result: SpeedTestResult) : SpeedTestEvent
}

sealed class SpeedTestException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class NoInternet : SpeedTestException("No working internet connection was detected.")
    class NetworkChanged : SpeedTestException("The active network changed during the test. Please run it again.")
    class InvalidServer(detail: String) : SpeedTestException("Invalid server configuration: $detail")
    class Dns(cause: Throwable) : SpeedTestException("The server address could not be resolved.", cause)
    class Timeout : SpeedTestException("The speed-test server took too long to respond.")
    class Tls(cause: Throwable) : SpeedTestException("A secure connection to the server could not be established.", cause)
    class Http(val status: Int) : SpeedTestException("The speed-test server returned HTTP $status.")
    class Unavailable(cause: Throwable? = null) :
        SpeedTestException("The selected speed-test server is unavailable.", cause)
}
