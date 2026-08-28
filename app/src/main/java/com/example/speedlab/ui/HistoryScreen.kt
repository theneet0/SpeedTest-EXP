package com.example.speedlab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedlab.data.HistoryEntity
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.formatBytes
import com.example.speedlab.share.ShareFormat
import com.example.speedlab.share.shareHistoryCsv
import com.example.speedlab.share.shareHistoryRecord
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

private enum class DateFilter(val label: String, val days: Int?) {
    ALL("All time", null),
    WEEK("7 days", 7),
    MONTH("30 days", 30),
}

private enum class NetworkFilter(val label: String) {
    ALL("All networks"),
    WIFI("Wi-Fi"),
    CELLULAR("Cellular"),
    VPN("VPN"),
}

@Composable
fun HistoryScreen(
    records: List<HistoryEntity>,
    onDelete: (HistoryEntity) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<HistoryEntity?>(null) }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var networkFilter by remember { mutableStateOf(NetworkFilter.ALL) }
    var modeFilter by remember { mutableStateOf<TestMode?>(null) }
    var newestFirst by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }

    val cutoff = dateFilter.days?.let { System.currentTimeMillis() - it * 86_400_000L }
    val visibleRecords = records.asSequence()
        .filter { cutoff == null || it.timestampMillis >= cutoff }
        .filter { networkFilter == NetworkFilter.ALL || it.networkType == networkFilter.name }
        .filter { modeFilter == null || it.testMode == modeFilter?.name }
        .sortedBy { if (newestFirst) -it.timestampMillis else it.timestampMillis }
        .toList()

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("This permanently removes every locally saved speed-test result.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }

    selected?.let { record ->
        HistoryDetailDialog(
            record = record,
            onDismiss = { selected = null },
            onDelete = {
                selected = null
                onDelete(record)
            },
            onShareText = { shareHistoryRecord(context, record, ShareFormat.TEXT) },
            onShareJson = { shareHistoryRecord(context, record, ShareFormat.JSON) },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Results", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${records.size} locally stored results",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { shareHistoryCsv(context, visibleRecords) },
                    enabled = visibleRecords.isNotEmpty(),
                ) { Text("Export CSV") }
            }
        }
        if (records.count { it.completionStatus == "COMPLETED" } >= 2) {
            item { RecentComparison(records.filter { it.completionStatus == "COMPLETED" }.take(2)) }
        }
        item {
            FilterStrip(
                dateFilter = dateFilter,
                onDate = { dateFilter = it },
                networkFilter = networkFilter,
                onNetwork = { networkFilter = it },
                modeFilter = modeFilter,
                onMode = { modeFilter = it },
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${visibleRecords.size} shown",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { newestFirst = !newestFirst }) {
                    Text(if (newestFirst) "Newest first" else "Oldest first")
                }
                TextButton(onClick = { confirmClear = true }, enabled = records.isNotEmpty()) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (visibleRecords.isEmpty()) {
            item {
                EmptyHistory(hasAny = records.isNotEmpty())
            }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                HistoryRow(record = record, onClick = { selected = record })
            }
        }
    }
}

@Composable
private fun FilterStrip(
    dateFilter: DateFilter,
    onDate: (DateFilter) -> Unit,
    networkFilter: NetworkFilter,
    onNetwork: (NetworkFilter) -> Unit,
    modeFilter: TestMode?,
    onMode: (TestMode?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DateFilter.entries.forEach {
                FilterChip(selected = dateFilter == it, onClick = { onDate(it) }, label = { Text(it.label) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NetworkFilter.entries.forEach {
                FilterChip(
                    selected = networkFilter == it,
                    onClick = { onNetwork(it) },
                    label = { Text(it.label) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FilterChip(selected = modeFilter == null, onClick = { onMode(null) }, label = { Text("All modes") })
            TestMode.entries.forEach {
                FilterChip(selected = modeFilter == it, onClick = { onMode(it) }, label = { Text(it.label) })
            }
        }
    }
}

@Composable
private fun HistoryRow(record: HistoryEntity, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(record.timestampMillis)),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${record.serverName} • ${friendlyNetwork(record.networkType)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                StatusPill(record.completionStatus)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HistoryMetric("Download", record.downloadMbps)
                HistoryMetric("Upload", record.uploadMbps)
                Column {
                    Text("PING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("%.1f ms".format(record.pingMillis), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: Double) {
    Column {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        Text("%.2f".format(value), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text("Mbps", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "COMPLETED" -> MaterialTheme.colorScheme.secondary
        "CANCELLED" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier.background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(status.lowercase().replaceFirstChar(Char::uppercase), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecentComparison(records: List<HistoryEntity>) {
    val latest = records[0]
    val previous = records[1]
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Recent comparison", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(9.dp))
            Row {
                ComparisonMetric("Download", latest.downloadMbps, previous.downloadMbps, Modifier.weight(1f))
                ComparisonMetric("Upload", latest.uploadMbps, previous.uploadMbps, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, latest: Double, previous: Double, modifier: Modifier) {
    val delta = if (previous > 0.0) ((latest - previous) / previous) * 100.0 else 0.0
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f), fontSize = 12.sp)
        Text("%.1f Mbps".format(latest), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "${if (delta >= 0) "+" else ""}%.1f%% vs previous".format(delta),
            color = if (delta >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun EmptyHistory(hasAny: Boolean) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (hasAny) "No matching results" else "No tests yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasAny) "Adjust the filters to see more history." else "Completed, cancelled, and failed tests will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HistoryDetailDialog(
    record: HistoryEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShareText: () -> Unit,
    onShareJson: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Test details")
                Text(
                    DateFormat.getDateTimeInstance().format(Date(record.timestampMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                DetailRow("Download", "%.2f Mbps".format(record.downloadMbps))
                DetailRow("Upload", "%.2f Mbps".format(record.uploadMbps))
                DetailRow("Peak", "%.2f Mbps".format(record.peakMbps))
                DetailRow("Ping", "%.1f ms".format(record.pingMillis))
                DetailRow("Jitter", "%.1f ms".format(record.jitterMillis))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DetailRow("Server", record.serverName)
                DetailRow("Host", record.serverHost.ifBlank { "—" })
                DetailRow("Network", friendlyNetwork(record.networkType))
                DetailRow("Mode", record.testMode.lowercase().replace('_', ' '))
                DetailRow("Transferred", formatBytes(record.transferredBytes))
                DetailRow("Duration", "%.1f s".format(record.durationMillis / 1_000.0))
                DetailRow("Status", record.completionStatus)
                record.errorMessage?.let { DetailRow("Error", it) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onShareText, modifier = Modifier.weight(1f)) { Text("Share") }
                    OutlinedButton(onClick = onShareJson, modifier = Modifier.weight(1f)) { Text("JSON") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

private fun friendlyNetwork(value: String): String =
    value.lowercase().replaceFirstChar(Char::uppercase)
