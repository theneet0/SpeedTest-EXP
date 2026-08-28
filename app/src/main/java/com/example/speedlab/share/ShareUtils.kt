package com.example.speedlab.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.speedlab.data.HistoryEntity
import com.example.speedlab.engine.SpeedTestResult
import com.example.speedlab.model.formatBytes
import java.io.File
import java.text.DateFormat
import java.util.Date
import org.json.JSONObject

enum class ShareFormat { TEXT, JSON }

fun shareResult(context: Context, result: SpeedTestResult, format: ShareFormat) {
    val content = when (format) {
        ShareFormat.TEXT -> buildString {
            appendLine("SpeedLab result")
            appendLine("Download: %.2f Mbps".format(result.downloadMbps))
            appendLine("Upload: %.2f Mbps".format(result.uploadMbps))
            appendLine("Ping: %.1f ms".format(result.pingMillis))
            appendLine("Jitter: %.1f ms".format(result.jitterMillis))
            appendLine("Server: ${result.server.name} (${result.server.host})")
            appendLine("Network: ${result.network.transport.label}")
            append("Transferred: ${formatBytes(result.transferredBytes)}")
        }
        ShareFormat.JSON -> JSONObject()
            .put("download_mbps", result.downloadMbps)
            .put("upload_mbps", result.uploadMbps)
            .put("ping_ms", result.pingMillis)
            .put("jitter_ms", result.jitterMillis)
            .put("peak_mbps", result.peakMbps)
            .put("server_name", result.server.name)
            .put("server_host", result.server.host)
            .put("network_type", result.network.transport.name)
            .put("transferred_bytes", result.transferredBytes)
            .put("duration_ms", result.durationMillis)
            .toString(2)
    }
    shareText(context, content, if (format == ShareFormat.JSON) "application/json" else "text/plain")
}

fun shareHistoryRecord(context: Context, record: HistoryEntity, format: ShareFormat) {
    val content = when (format) {
        ShareFormat.TEXT -> buildString {
            appendLine("SpeedLab result")
            appendLine("Date: ${DateFormat.getDateTimeInstance().format(Date(record.timestampMillis))}")
            appendLine("Download: %.2f Mbps".format(record.downloadMbps))
            appendLine("Upload: %.2f Mbps".format(record.uploadMbps))
            appendLine("Ping: %.1f ms".format(record.pingMillis))
            appendLine("Jitter: %.1f ms".format(record.jitterMillis))
            appendLine("Server: ${record.serverName} (${record.serverHost})")
            appendLine("Network: ${record.networkType}")
            append("Status: ${record.completionStatus}")
        }
        ShareFormat.JSON -> historyJson(record).toString(2)
    }
    shareText(context, content, if (format == ShareFormat.JSON) "application/json" else "text/plain")
}

fun shareHistoryCsv(context: Context, records: List<HistoryEntity>) {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, "SpeedLab-history.csv")
    file.bufferedWriter().use { output ->
        output.appendLine(
            "timestamp,download_mbps,upload_mbps,ping_ms,jitter_ms,peak_mbps," +
                "server_name,server_host,network_type,test_mode,transferred_bytes,duration_ms,status,error",
        )
        records.forEach { record ->
            output.appendLine(
                listOf(
                    record.timestampMillis,
                    record.downloadMbps,
                    record.uploadMbps,
                    record.pingMillis,
                    record.jitterMillis,
                    record.peakMbps,
                    record.serverName,
                    record.serverHost,
                    record.networkType,
                    record.testMode,
                    record.transferredBytes,
                    record.durationMillis,
                    record.completionStatus,
                    record.errorMessage.orEmpty(),
                ).joinToString(",") { csvCell(it.toString()) },
            )
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export SpeedLab history"))
}

private fun historyJson(record: HistoryEntity): JSONObject = JSONObject()
    .put("timestamp", record.timestampMillis)
    .put("download_mbps", record.downloadMbps)
    .put("upload_mbps", record.uploadMbps)
    .put("ping_ms", record.pingMillis)
    .put("jitter_ms", record.jitterMillis)
    .put("peak_mbps", record.peakMbps)
    .put("server_name", record.serverName)
    .put("server_host", record.serverHost)
    .put("network_type", record.networkType)
    .put("test_mode", record.testMode)
    .put("transferred_bytes", record.transferredBytes)
    .put("duration_ms", record.durationMillis)
    .put("status", record.completionStatus)
    .put("error", record.errorMessage ?: JSONObject.NULL)

private fun shareText(context: Context, content: String, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share SpeedLab result"))
}

private fun csvCell(value: String): String {
    if (value.none { it == ',' || it.code == 34 || it.code == 10 }) return value
    val quote = 34.toChar().toString()
    return quote + value.replace(quote, quote + quote) + quote
}
