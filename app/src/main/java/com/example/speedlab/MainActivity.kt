package com.example.speedlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speedlab.ui.SpeedLabRoot
import com.example.speedlab.ui.SpeedLabTheme
import com.example.speedlab.ui.SpeedLabViewModel
import com.example.speedlab.ui.SpeedLabViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: SpeedLabViewModel by viewModels {
        SpeedLabViewModelFactory((application as SpeedLabApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val history by viewModel.history.collectAsStateWithLifecycle()
            SpeedLabTheme(state.settings.themeMode) {
                SpeedLabRoot(
                    state = state,
                    history = history,
                    viewModel = viewModel,
                )
            }
        }
    }
}
