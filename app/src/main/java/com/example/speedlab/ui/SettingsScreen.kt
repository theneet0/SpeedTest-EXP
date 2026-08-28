package com.example.speedlab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedlab.engine.ServerCatalog
import com.example.speedlab.model.AppSettings
import com.example.speedlab.model.ServerMode
import com.example.speedlab.model.SpeedUnit
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.TestProfile
import com.example.speedlab.model.ThemeMode
import com.example.speedlab.model.formatBytes
import com.example.speedlab.model.measurementConfig
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    enabled: Boolean,
    onSave: (AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                if (enabled) "Tune accuracy, traffic, servers, and appearance." else "Settings are locked during an active test.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item {
            SettingsCard("TEST MODE", "Choose exactly which transfer directions run.") {
                ChoiceChips(
                    choices = TestMode.entries.map { it.label },
                    selectedIndex = TestMode.entries.indexOf(settings.testMode),
                    enabled = enabled,
                    onSelect = { onSave(settings.copy(testMode = TestMode.entries[it])) },
                )
            }
        }
        item {
            SettingsCard("MEASUREMENT PROFILE", "Profiles control duration, traffic, and parallel streams.") {
                TestProfile.entries.forEach { profile ->
                    ProfileRow(
                        profile = profile,
                        selected = settings.profile == profile,
                        enabled = enabled,
                        onClick = { onSave(settings.copy(profile = profile)) },
                    )
                }
                val config = settings.measurementConfig()
                Text(
                    "${config.parallelConnections} streams • " +
                        "${formatBytes(config.maxTransferredBytes)} cap • " +
                        "${config.samplingIntervalMillis} ms samples",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (settings.profile == TestProfile.CUSTOM) {
            item {
                CustomProfileCard(settings, enabled, onSave)
            }
        }
        item {
            SettingsCard("SPEED UNIT", "Stored results remain in Mbps and are converted for display.") {
                SpeedUnit.entries.forEach { unit ->
                    SelectableRow(
                        title = unit.symbol,
                        subtitle = unit.label,
                        selected = settings.speedUnit == unit,
                        enabled = enabled,
                        onClick = { onSave(settings.copy(speedUnit = unit)) },
                    )
                }
            }
        }
        item {
            SettingsCard("SERVER", "Automatic mode probes reachable servers with tiny requests and chooses the lowest latency.") {
                ChoiceChips(
                    choices = ServerMode.entries.map { it.label },
                    selectedIndex = ServerMode.entries.indexOf(settings.serverMode),
                    enabled = enabled,
                    onSelect = { onSave(settings.copy(serverMode = ServerMode.entries[it])) },
                )
                when (settings.serverMode) {
                    ServerMode.AUTO -> {
                        Text(
                            "Built-in candidates: ${ServerCatalog.builtIn.joinToString { it.name }}. A valid custom server is also considered.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    ServerMode.MANUAL -> {
                        ServerCatalog.builtIn.forEach { server ->
                            SelectableRow(
                                title = server.name,
                                subtitle = "${server.location} • ${server.host}",
                                selected = settings.manualServerId == server.id,
                                enabled = enabled,
                                onClick = { onSave(settings.copy(manualServerId = server.id)) },
                            )
                        }
                    }
                    ServerMode.CUSTOM -> CustomServerFields(settings, enabled, onSave)
                }
            }
        }
        item {
            SettingsCard("MOBILE DATA", "A warning helps prevent accidental high cellular usage.") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Warn before testing on mobile data", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Also warns for metered VPN and Wi-Fi connections.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.warnOnCellular,
                        onCheckedChange = { onSave(settings.copy(warnOnCellular = it)) },
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            SettingsCard("APPEARANCE", "Follow the phone theme or choose a fixed mode.") {
                ChoiceChips(
                    choices = ThemeMode.entries.map { it.label },
                    selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                    enabled = enabled,
                    onSelect = { onSave(settings.copy(themeMode = ThemeMode.entries[it])) },
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Privacy by design", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                    Text(
                        "No ads, analytics, account, location permission, or broad storage permission. " +
                            "History stays on this device. All built-in test traffic uses normal HTTPS certificate validation.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomProfileCard(
    settings: AppSettings,
    enabled: Boolean,
    onSave: (AppSettings) -> Unit,
) {
    SettingsCard("CUSTOM PROFILE", "All values are constrained to safe ranges.") {
        ValueSlider(
            label = "Download duration",
            value = settings.customDownloadSeconds,
            range = 2..60,
            suffix = "s",
            enabled = enabled,
            onChange = { onSave(settings.copy(customDownloadSeconds = it)) },
        )
        ValueSlider(
            label = "Upload duration",
            value = settings.customUploadSeconds,
            range = 2..60,
            suffix = "s",
            enabled = enabled,
            onChange = { onSave(settings.copy(customUploadSeconds = it)) },
        )
        ValueSlider(
            label = "Warm-up",
            value = settings.customWarmupMillis,
            range = 0..5_000,
            suffix = "ms",
            enabled = enabled,
            onChange = { onSave(settings.copy(customWarmupMillis = it)) },
        )
        ValueSlider(
            label = "Connection timeout",
            value = settings.customTimeoutSeconds,
            range = 2..30,
            suffix = "s",
            enabled = enabled,
            onChange = { onSave(settings.copy(customTimeoutSeconds = it)) },
        )
        ValueSlider(
            label = "Sampling interval",
            value = settings.customSamplingMillis,
            range = 100..1_000,
            suffix = "ms",
            enabled = enabled,
            onChange = { onSave(settings.copy(customSamplingMillis = it)) },
        )
        ValueSlider(
            label = "Maximum transferred data",
            value = settings.customMaxDataMb,
            range = 5..2_048,
            suffix = "MB",
            enabled = enabled,
            onChange = { onSave(settings.copy(customMaxDataMb = it)) },
        )
        Text("Parallel connections", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        ChoiceChips(
            choices = listOf("1", "2", "4", "8", "12", "16"),
            selectedIndex = listOf(1, 2, 4, 8, 12, 16).indexOf(settings.customConnections).coerceAtLeast(0),
            enabled = enabled,
            onSelect = { onSave(settings.copy(customConnections = listOf(1, 2, 4, 8, 12, 16)[it])) },
        )
        Text("Request chunk / buffer budget", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        ChoiceChips(
            choices = listOf("256 KB", "512 KB", "1 MB", "2 MB", "4 MB", "8 MB"),
            selectedIndex = listOf(256, 512, 1_024, 2_048, 4_096, 8_192)
                .indexOf(settings.customChunkKb).coerceAtLeast(0),
            enabled = enabled,
            onSelect = {
                onSave(settings.copy(customChunkKb = listOf(256, 512, 1_024, 2_048, 4_096, 8_192)[it]))
            },
        )
    }
}

@Composable
private fun CustomServerFields(
    settings: AppSettings,
    enabled: Boolean,
    onSave: (AppSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        OutlinedTextField(
            value = settings.customServerName,
            onValueChange = { onSave(settings.copy(customServerName = it.take(60))) },
            label = { Text("Server name") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = settings.customServerLocation,
            onValueChange = { onSave(settings.copy(customServerLocation = it.take(80))) },
            label = { Text("Location (optional)") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = settings.customDownloadUrl,
            onValueChange = { onSave(settings.copy(customDownloadUrl = it.take(500))) },
            label = { Text("HTTPS download URL") },
            supportingText = { Text("Use {bytes} where the requested byte count belongs; otherwise SpeedLab adds ?bytes=.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = settings.customUploadUrl,
            onValueChange = { onSave(settings.copy(customUploadUrl = it.take(500))) },
            label = { Text("HTTPS upload URL") },
            supportingText = { Text("Must accept a binary HTTPS POST body.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Cleartext HTTP is deliberately blocked. Normal TLS certificate validation is always used.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(title.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp, bottom = 9.dp))
            content()
        }
    }
}

@Composable
private fun ChoiceChips(
    choices: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        choices.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                enabled = enabled,
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun ProfileRow(
    profile: TestProfile,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.padding(start = 7.dp)) {
            Text(profile.label, fontWeight = FontWeight.SemiBold)
            Text(profile.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.padding(start = 7.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ValueSlider(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("$value $suffix", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}
