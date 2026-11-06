package com.artcompanion.data

import android.content.Context
import com.artcompanion.hid.ConnectionPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App settings persisted as one JSON value in SharedPreferences. */
class SettingsRepository(context: Context, private val connection: ConnectionPrefs) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun read(): AppSettings {
        val s = sp.getString(KEY, null)?.let {
            runCatching { ProfileRepository.json.decodeFromString(AppSettings.serializer(), it) }.getOrNull()
        } ?: AppSettings()
        return s.copy(autoReconnect = connection.autoReconnect)
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        connection.autoReconnect = next.autoReconnect
        sp.edit().putString(KEY, ProfileRepository.json.encodeToString(AppSettings.serializer(), next)).apply()
    }

    fun reset() = update { AppSettings() }

    private companion object { const val KEY = "app_settings" }
}
