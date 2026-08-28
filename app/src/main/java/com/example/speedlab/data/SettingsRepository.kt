package com.example.speedlab.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.speedlab.model.AppSettings
import com.example.speedlab.model.ServerMode
import com.example.speedlab.model.SpeedUnit
import com.example.speedlab.model.TestMode
import com.example.speedlab.model.TestProfile
import com.example.speedlab.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.speedLabDataStore by preferencesDataStore(name = "speedlab_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.speedLabDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(::toSettings)

    suspend fun save(value: AppSettings) {
        context.speedLabDataStore.edit { preferences ->
            preferences[Keys.TEST_MODE] = value.testMode.name
            preferences[Keys.SPEED_UNIT] = value.speedUnit.name
            preferences[Keys.PROFILE] = value.profile.name
            preferences[Keys.THEME] = value.themeMode.name
            preferences[Keys.SERVER_MODE] = value.serverMode.name
            preferences[Keys.MANUAL_SERVER] = value.manualServerId
            preferences[Keys.CUSTOM_NAME] = value.customServerName
            preferences[Keys.CUSTOM_LOCATION] = value.customServerLocation
            preferences[Keys.CUSTOM_DOWNLOAD] = value.customDownloadUrl.trim()
            preferences[Keys.CUSTOM_UPLOAD] = value.customUploadUrl.trim()
            preferences[Keys.WARN_CELLULAR] = value.warnOnCellular
            preferences[Keys.DOWNLOAD_SECONDS] = value.customDownloadSeconds
            preferences[Keys.UPLOAD_SECONDS] = value.customUploadSeconds
            preferences[Keys.WARMUP_MILLIS] = value.customWarmupMillis
            preferences[Keys.TIMEOUT_SECONDS] = value.customTimeoutSeconds
            preferences[Keys.CONNECTIONS] = value.customConnections
            preferences[Keys.SAMPLING_MILLIS] = value.customSamplingMillis
            preferences[Keys.MAX_DATA_MB] = value.customMaxDataMb
            preferences[Keys.CHUNK_KB] = value.customChunkKb
        }
    }

    private fun toSettings(p: Preferences): AppSettings = AppSettings(
        testMode = p[Keys.TEST_MODE].enumOr(TestMode.BOTH),
        speedUnit = p[Keys.SPEED_UNIT].enumOr(SpeedUnit.MBPS),
        profile = p[Keys.PROFILE].enumOr(TestProfile.BALANCED),
        themeMode = p[Keys.THEME].enumOr(ThemeMode.SYSTEM),
        serverMode = p[Keys.SERVER_MODE].enumOr(ServerMode.AUTO),
        manualServerId = p[Keys.MANUAL_SERVER] ?: "cloudflare-global",
        customServerName = p[Keys.CUSTOM_NAME] ?: "My server",
        customServerLocation = p[Keys.CUSTOM_LOCATION].orEmpty(),
        customDownloadUrl = p[Keys.CUSTOM_DOWNLOAD].orEmpty(),
        customUploadUrl = p[Keys.CUSTOM_UPLOAD].orEmpty(),
        warnOnCellular = p[Keys.WARN_CELLULAR] ?: true,
        customDownloadSeconds = p[Keys.DOWNLOAD_SECONDS] ?: 10,
        customUploadSeconds = p[Keys.UPLOAD_SECONDS] ?: 10,
        customWarmupMillis = p[Keys.WARMUP_MILLIS] ?: 800,
        customTimeoutSeconds = p[Keys.TIMEOUT_SECONDS] ?: 10,
        customConnections = p[Keys.CONNECTIONS] ?: 4,
        customSamplingMillis = p[Keys.SAMPLING_MILLIS] ?: 200,
        customMaxDataMb = p[Keys.MAX_DATA_MB] ?: 250,
        customChunkKb = p[Keys.CHUNK_KB] ?: 2_048,
    )

    private object Keys {
        val TEST_MODE = stringPreferencesKey("test_mode")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val PROFILE = stringPreferencesKey("profile")
        val THEME = stringPreferencesKey("theme")
        val SERVER_MODE = stringPreferencesKey("server_mode")
        val MANUAL_SERVER = stringPreferencesKey("manual_server")
        val CUSTOM_NAME = stringPreferencesKey("custom_name")
        val CUSTOM_LOCATION = stringPreferencesKey("custom_location")
        val CUSTOM_DOWNLOAD = stringPreferencesKey("custom_download")
        val CUSTOM_UPLOAD = stringPreferencesKey("custom_upload")
        val WARN_CELLULAR = booleanPreferencesKey("warn_cellular")
        val DOWNLOAD_SECONDS = intPreferencesKey("download_seconds")
        val UPLOAD_SECONDS = intPreferencesKey("upload_seconds")
        val WARMUP_MILLIS = intPreferencesKey("warmup_millis")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val CONNECTIONS = intPreferencesKey("connections")
        val SAMPLING_MILLIS = intPreferencesKey("sampling_millis")
        val MAX_DATA_MB = intPreferencesKey("max_data_mb")
        val CHUNK_KB = intPreferencesKey("chunk_kb")
    }
}

private inline fun <reified T : Enum<T>> String?.enumOr(default: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
