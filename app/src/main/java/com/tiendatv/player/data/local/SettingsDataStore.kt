package com.tiendatv.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tiendatv.player.player.BufferPreset
import com.tiendatv.player.player.VideoAspectMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tiendaDataStore: DataStore<Preferences> by preferencesDataStore(name = "tiendatv_settings")

enum class AppLanguage(val code: String) {
    ES("es"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ES
    }
}

data class UserSettings(
    val darkCinemaMode: Boolean = true,
    val language: AppLanguage = AppLanguage.ES,
    val bufferPreset: BufferPreset = BufferPreset.BALANCED,
    val preferSoftwareDecoder: Boolean = false,
    val defaultAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val parentalEnabled: Boolean = false,
    val parentalPin: String = "0000"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DARK_CINEMA_MODE = booleanPreferencesKey("dark_cinema_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val BUFFER_PRESET = stringPreferencesKey("buffer_preset")
        val PREFER_SOFTWARE_DECODER = booleanPreferencesKey("prefer_software_decoder")
        val DEFAULT_ASPECT_MODE = stringPreferencesKey("default_aspect_mode")
        val PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
    }

    val settingsFlow: Flow<UserSettings> = context.tiendaDataStore.data.map { prefs ->
        UserSettings(
            darkCinemaMode = prefs[Keys.DARK_CINEMA_MODE] ?: true,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            bufferPreset = BufferPreset.fromName(prefs[Keys.BUFFER_PRESET]),
            preferSoftwareDecoder = prefs[Keys.PREFER_SOFTWARE_DECODER] ?: false,
            defaultAspectMode = VideoAspectMode.fromName(prefs[Keys.DEFAULT_ASPECT_MODE]),
            parentalEnabled = prefs[Keys.PARENTAL_ENABLED] ?: false,
            parentalPin = prefs[Keys.PARENTAL_PIN] ?: "0000"
        )
    }

    suspend fun setDarkCinemaMode(enabled: Boolean) {
        context.tiendaDataStore.edit { it[Keys.DARK_CINEMA_MODE] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.tiendaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setBufferPreset(preset: BufferPreset) {
        context.tiendaDataStore.edit { it[Keys.BUFFER_PRESET] = preset.name }
    }

    suspend fun setPreferSoftwareDecoder(prefer: Boolean) {
        context.tiendaDataStore.edit { it[Keys.PREFER_SOFTWARE_DECODER] = prefer }
    }

    suspend fun setDefaultAspectMode(mode: VideoAspectMode) {
        context.tiendaDataStore.edit { it[Keys.DEFAULT_ASPECT_MODE] = mode.name }
    }

    suspend fun setParentalControl(enabled: Boolean, pin: String) {
        context.tiendaDataStore.edit {
            it[Keys.PARENTAL_ENABLED] = enabled
            if (pin.length == 4) {
                it[Keys.PARENTAL_PIN] = pin
            }
        }
    }
}
