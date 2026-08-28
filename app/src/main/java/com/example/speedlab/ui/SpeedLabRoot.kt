package com.example.speedlab.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.speedlab.data.HistoryEntity

private enum class AppTab(val label: String, val glyph: String) {
    TEST("Test", "⌁"),
    HISTORY("History", "↺"),
    SETTINGS("Settings", "⚙"),
}

@Composable
fun SpeedLabRoot(
    state: SpeedLabUiState,
    history: List<HistoryEntity>,
    viewModel: SpeedLabViewModel,
) {
    var tab by remember { mutableStateOf(AppTab.TEST) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Text(
                                item.glyph,
                                fontWeight = if (tab == item) FontWeight.Black else FontWeight.Normal,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            AppTab.TEST -> TestScreen(
                state = state,
                onSelectMode = viewModel::selectMode,
                onStart = viewModel::requestStart,
                onCancel = viewModel::cancelTest,
                onConfirmCellular = viewModel::confirmCellularStart,
                onDismissCellular = viewModel::dismissCellularWarning,
                onDismissError = viewModel::clearError,
                modifier = Modifier.padding(padding),
            )
            AppTab.HISTORY -> HistoryScreen(
                records = history,
                onDelete = viewModel::deleteHistory,
                onClear = viewModel::clearHistory,
                modifier = Modifier.padding(padding),
            )
            AppTab.SETTINGS -> SettingsScreen(
                settings = state.settings,
                enabled = !state.isActive,
                onSave = viewModel::saveSettings,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
