package com.example.speedlab

import android.app.Application
import com.example.speedlab.data.HistoryRepository
import com.example.speedlab.data.SettingsRepository
import com.example.speedlab.data.SpeedLabDatabase
import com.example.speedlab.engine.NativeHttpSpeedTestEngine
import com.example.speedlab.engine.SpeedTestEngine
import com.example.speedlab.network.NetworkMonitor

class SpeedLabApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val historyRepository = HistoryRepository(SpeedLabDatabase.get(application).historyDao())
    val networkMonitor = NetworkMonitor(application)
    val speedTestEngine: SpeedTestEngine =
        NativeHttpSpeedTestEngine(networkProvider = networkMonitor::current)
}
