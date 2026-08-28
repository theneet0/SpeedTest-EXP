package com.example.speedlab.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.speedlab.data.HistoryEntity

private enum class AppTab(val label: String, val title: String, val icon: ImageVector) {
    TEST("Test", "SpeedLab", Icons.Filled.Home),
    HISTORY("History", "Test history", Icons.Filled.DateRange),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedLabRoot(
    state: SpeedLabUiState,
    history: List<HistoryEntity>,
    viewModel: SpeedLabViewModel,
) {
    var tab by remember { mutableStateOf(AppTab.TEST) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
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
