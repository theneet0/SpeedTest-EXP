package com.example.speedlab.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedlab.engine.TestStage
import com.example.speedlab.model.SpeedUnit
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.durationEstimateMillis
import com.example.speedlab.model.estimatedMaximumBytes
import com.example.speedlab.model.formatBytes
import com.example.speedlab.share.ShareFormat
import com.example.speedlab.share.shareResult
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun TestScreen(
    state: SpeedLabUiState,
    onSelectMode: (TestMode) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onConfirmCellular: () -> Unit,
    onDismissCellular: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDiagnostics by remember { mutableStateOf(false) }
    val unit = state.settings.speedUnit

    if (state.showCellularWarning) {
        AlertDialog(
            onDismissRequest = onDismissCellular,
            title = { Text("Use mobile data?") },
            text = {
                Text(
                    "This ${state.settings.profile.label} test can transfer up to " +
                        "${formatBytes(estimatedMaximumBytes(state.settings))}. Carrier charges may apply.",
                )
            },
            confirmButton = { TextButton(onClick = onConfirmCellular) { Text("Continue") } },
            dismissButton = { TextButton(onClick = onDismissCellular) { Text("Cancel") } },
        )
    }

    if (state.errorMessage != null && state.stage != TestStage.CANCELLED) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(if (state.stage == TestStage.FAILED) "Test stopped" else "Check settings") },
            text = { Text(state.errorMessage) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppHeader(state)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TestMode.entries.forEach { mode ->
                    val label = when (mode) {
                        TestMode.DOWNLOAD -> "Download"
                        TestMode.UPLOAD -> "Upload"
                        TestMode.BOTH -> "Both"
                    }
                    FilterChip(
                        selected = state.settings.testMode == mode,
                        onClick = { onSelectMode(mode) },
                        enabled = !state.isActive,
                        label = { Text(label, maxLines = 1, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            ServerCard(state)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.stage.label.uppercase(),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                    )
                    SpeedGauge(
                        mbps = state.currentMbps,
                        unit = unit,
                        modifier = Modifier.fillMaxWidth().height(244.dp),
                    )
                    if (state.isActive || state.stage == TestStage.COMPLETED) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = SpeedLabAqua,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (state.isActive) {
                        Text(
                            "${formatBytes(state.transferredBytes)} transferred",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val estimateSeconds = durationEstimateMillis(state.settings) / 1_000
                        Text(
                            "Estimated maximum ${formatBytes(estimatedMaximumBytes(state.settings))} • about ${estimateSeconds}s",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        item {
            MetricsGrid(state, unit)
        }
        if (state.samplesMbps.size >= 2) {
            item {
                ThroughputGraph(
                    samples = state.samplesMbps.map(unit::fromMbps),
                    unit = unit.symbol,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }
        }
        item {
            Button(
                onClick = if (state.isActive) onCancel else onStart,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isActive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    if (state.isActive) "CANCEL TEST" else "START TEST",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
        if (state.lastResult != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { shareResult(context, state.lastResult, ShareFormat.TEXT) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Share result") }
                    OutlinedButton(
                        onClick = { shareResult(context, state.lastResult, ShareFormat.JSON) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Share JSON") }
                }
            }
        }
        item {
            DiagnosticsCard(
                state = state,
                expanded = showDiagnostics,
                onToggle = { showDiagnostics = !showDiagnostics },
            )
        }
    }
}

@Composable
private fun AppHeader(state: SpeedLabUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("S", color = MaterialTheme.colorScheme.onPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text("SpeedLab", style = MaterialTheme.typography.titleLarge)
            Text(
                "Private, native internet measurement",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            modifier = Modifier.background(
                if (state.network.available) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                },
                CircleShape,
            ).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(7.dp).background(
                    if (state.network.available) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    CircleShape,
                ),
            )
            Spacer(Modifier.width(6.dp))
            Text(state.network.transport.label, style = MaterialTheme.typography.labelLarge, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ServerCard(state: SpeedLabUiState) {
    val server = state.selectedServer
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(SpeedLabAqua.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("◎", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(server?.name ?: state.settings.serverMode.label, fontWeight = FontWeight.Bold)
                Text(
                    server?.let { "${it.location} • ${it.host}" } ?: "Server will be selected at test time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            if (server != null && state.serverSelectionLatencyMillis > 0.0) {
                Text(
                    "%.0f ms".format(state.serverSelectionLatencyMillis),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SpeedGauge(mbps: Double, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val converted = unit.fromMbps(mbps)
    val scale = when {
        converted <= 10.0 -> 10.0
        converted <= 50.0 -> 50.0
        converted <= 100.0 -> 100.0
        converted <= 500.0 -> 500.0
        converted <= 1_000.0 -> 1_000.0
        else -> ceil(converted / 1_000.0) * 1_000.0
    }
    val target = (converted / scale).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(target, animationSpec = tween(180), label = "gauge")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val needle = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.semantics {
            contentDescription = "Current speed ${formatSpeed(converted)} ${unit.symbol}"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(238.dp)) {
            val stroke = 16.dp.toPx()
            val inset = stroke
            val arcRect = Rect(inset, inset, size.width - inset, size.height - inset)
            drawArc(
                color = track,
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(SpeedLabAqua, SpeedLabBlue, SpeedLabAqua)),
                startAngle = 145f,
                sweepAngle = 250f * animated,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            repeat(11) { index ->
                val angle = Math.toRadians((145.0 + index * 25.0))
                val outer = size.minDimension / 2f - 30.dp.toPx()
                val inner = outer - if (index % 5 == 0) 10.dp.toPx() else 5.dp.toPx()
                val center = this.center
                drawLine(
                    color = track,
                    start = Offset(
                        center.x + cos(angle).toFloat() * inner,
                        center.y + sin(angle).toFloat() * inner,
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * outer,
                        center.y + sin(angle).toFloat() * outer,
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            val needleAngle = Math.toRadians(145.0 + 250.0 * animated)
            val needleLength = 63.dp.toPx()
            drawLine(
                color = needle,
                start = center,
                end = Offset(
                    center.x + cos(needleAngle).toFloat() * needleLength,
                    center.y + sin(needleAngle).toFloat() * needleLength,
                ),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(needle, 8.dp.toPx(), center)
            drawCircle(SpeedLabAqua, 3.dp.toPx(), center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 92.dp)) {
            Text(
                formatSpeed(converted),
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(unit.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricsGrid(state: SpeedLabUiState, unit: SpeedUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("DOWNLOAD", formatSpeed(unit.fromMbps(state.downloadMbps)), unit.symbol, Modifier.weight(1f))
            MetricCard("UPLOAD", formatSpeed(unit.fromMbps(state.uploadMbps)), unit.symbol, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("PING", "%.1f".format(state.pingMillis), "ms", Modifier.weight(1f))
            MetricCard("JITTER", "%.1f".format(state.jitterMillis), "ms", Modifier.weight(1f))
            MetricCard("PEAK", formatSpeed(unit.fromMbps(state.peakMbps)), unit.symbol, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ThroughputGraph(samples: List<Double>, unit: String, modifier: Modifier = Modifier) {
    val lineColor = SpeedLabAqua
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LIVE THROUGHPUT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Canvas(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp).semantics {
                    contentDescription = "Live throughput graph with ${samples.size} samples"
                },
            ) {
                repeat(4) { index ->
                    val y = size.height * index / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val maximum = samples.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val path = Path()
                samples.forEachIndexed { index, value ->
                    val x = if (samples.size == 1) 0f else size.width * index / (samples.size - 1f)
                    val y = size.height - (value / maximum).toFloat().coerceIn(0f, 1f) * size.height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(state: SpeedLabUiState, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (expanded) "Hide diagnostics" else "Show diagnostics",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Text(if (expanded) "⌃" else "⌄")
            }
            if (expanded) {
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                DiagnosticRow("Engine", state.diagnostics.engineType)
                DiagnosticRow("Endpoint", state.diagnostics.endpoint.ifBlank { "Not selected" })
                DiagnosticRow("Transport", state.network.transport.label)
                DiagnosticRow("Connections", state.diagnostics.connectionCount.toString())
                DiagnosticRow("Transferred", formatBytes(state.diagnostics.transferredBytes))
                DiagnosticRow("Samples", state.diagnostics.sampleCount.toString())
                DiagnosticRow("HTTP status", state.diagnostics.httpStatus?.toString() ?: "—")
                DiagnosticRow("Duration", "${state.diagnostics.durationMillis} ms")
                if (state.diagnostics.lastEngineError != null) {
                    DiagnosticRow("Last error", state.diagnostics.lastEngineError)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f), maxLines = 1)
    }
}

private fun formatSpeed(value: Double): String = when {
    !value.isFinite() || value < 0.0 -> "0.0"
    value >= 1_000.0 -> "%,.0f".format(value)
    value >= 100.0 -> "%.1f".format(value)
    value >= 10.0 -> "%.2f".format(value)
    else -> "%.2f".format(value)
}
