package com.example.speedlab.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
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
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text(if (state.stage == TestStage.FAILED) "Test stopped" else "Check settings") },
            text = { Text(state.errorMessage) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { NetworkStatus(state) }
        item {
            TestModeSelector(
                selected = state.settings.testMode,
                enabled = !state.isActive,
                onSelect = onSelectMode,
            )
        }
        item { ServerCard(state) }
        item { MeasurementCard(state, unit) }
        item { MetricsGrid(state, unit) }
        if (state.samplesMbps.size >= 2) {
            item {
                ThroughputGraph(
                    samples = state.samplesMbps.map(unit::fromMbps),
                    unit = unit.symbol,
                    modifier = Modifier.fillMaxWidth().height(168.dp),
                )
            }
        }
        item {
            Button(
                onClick = if (state.isActive) onCancel else onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = if (state.isActive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    imageVector = if (state.isActive) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (state.isActive) "Cancel test" else "Start speed test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
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
private fun NetworkStatus(state: SpeedLabUiState) {
    val container = if (state.network.available) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (state.network.available) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Internet speed test", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Real-time native measurement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(color = container, contentColor = content, shape = CircleShape) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(content, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(state.network.transport.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TestModeSelector(
    selected: TestMode,
    enabled: Boolean,
    onSelect: (TestMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Test mode", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestMode.entries.forEach { mode ->
                val label = if (mode == TestMode.BOTH) "Both" else mode.label
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    enabled = enabled,
                    label = { Text(label, maxLines = 1) },
                    leadingIcon = if (selected == mode) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ServerCard(state: SpeedLabUiState) {
    val server = state.selectedServer
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    server?.name ?: "${state.settings.serverMode.label} server",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    server?.let { "${it.location} • ${it.host}" } ?: "Selected when the test starts",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            if (server != null && state.serverSelectionLatencyMillis > 0.0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        "%.0f ms".format(state.serverSelectionLatencyMillis),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(state: SpeedLabUiState, unit: SpeedUnit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    state.stage.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(18.dp))
            SpeedRing(
                mbps = state.currentMbps,
                unit = unit,
                modifier = Modifier.size(238.dp),
            )
            Spacer(Modifier.height(18.dp))
            if (state.isActive || state.stage == TestStage.COMPLETED) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.height(12.dp))
            val supportingText = if (state.isActive) {
                "${formatBytes(state.transferredBytes)} transferred"
            } else {
                val estimateSeconds = durationEstimateMillis(state.settings) / 1_000
                "Up to ${formatBytes(estimatedMaximumBytes(state.settings))} • about ${estimateSeconds}s"
            }
            Text(
                supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SpeedRing(mbps: Double, unit: SpeedUnit, modifier: Modifier = Modifier) {
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
    val animated by animateFloatAsState(target, animationSpec = tween(180), label = "speed ring")

    Box(
        modifier = modifier.semantics {
            contentDescription = "Current speed ${formatSpeed(converted)} ${unit.symbol}"
        },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 14.dp,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CURRENT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )
            Text(
                formatSpeed(converted),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(unit.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                "Scale ${formatSpeed(scale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricsGrid(state: SpeedLabUiState, unit: SpeedUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Download", formatSpeed(unit.fromMbps(state.downloadMbps)), unit.symbol, Modifier.weight(1f))
            MetricCard("Upload", formatSpeed(unit.fromMbps(state.uploadMbps)), unit.symbol, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Average", formatSpeed(unit.fromMbps(state.averageMbps)), unit.symbol, Modifier.weight(1f))
            MetricCard("Peak", formatSpeed(unit.fromMbps(state.peakMbps)), unit.symbol, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Ping", "%.1f".format(state.pingMillis), "ms", Modifier.weight(1f))
            MetricCard("Jitter", "%.1f".format(state.jitterMillis), "ms", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThroughputGraph(samples: List<Double>, unit: String, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Live throughput", style = MaterialTheme.typography.titleSmall)
                Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            Canvas(
                modifier = Modifier.fillMaxSize().padding(top = 12.dp).semantics {
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (expanded) "Hide diagnostics" else "Show diagnostics",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
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
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
            maxLines = 1,
        )
    }
}

private fun formatSpeed(value: Double): String = when {
    !value.isFinite() || value < 0.0 -> "0.0"
    value >= 1_000.0 -> "%,.0f".format(value)
    value >= 100.0 -> "%.1f".format(value)
    value >= 10.0 -> "%.2f".format(value)
    else -> "%.2f".format(value)
}
