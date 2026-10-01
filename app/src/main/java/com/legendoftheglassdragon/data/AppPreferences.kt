package com.legendoftheglassdragon.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class LocalAppState(
    val musicEnabled: Boolean = true,
    val clickSoundEnabled: Boolean = true,
)

internal interface BooleanKeyValueStore {
    fun read(key: String, defaultValue: Boolean): Boolean
    fun write(key: String, value: Boolean)
}

/** Preferences for presentation-only state. Durable game state has a separate owner. */
class AppPreferences internal constructor(
    private val values: BooleanKeyValueStore,
) {
    fun load(): LocalAppState = LocalAppState(
        musicEnabled = values.read(KEY_MUSIC_ENABLED, true),
        clickSoundEnabled = values.read(KEY_CLICK_SOUND_ENABLED, true),
    )

    fun setMusicEnabled(enabled: Boolean) {
        values.write(KEY_MUSIC_ENABLED, enabled)
    }

    fun setClickSoundEnabled(enabled: Boolean) {
        values.write(KEY_CLICK_SOUND_ENABLED, enabled)
    }

    companion object {
        private const val FILE_NAME = "glass_dragon_local_state"
        internal const val KEY_MUSIC_ENABLED = "music_enabled"
        internal const val KEY_CLICK_SOUND_ENABLED = "click_sound_enabled"

        fun from(context: Context): AppPreferences {
            val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return AppPreferences(SharedPreferencesBooleanStore(preferences))
        }
    }
}

private class SharedPreferencesBooleanStore(
    private val preferences: SharedPreferences,
) : BooleanKeyValueStore {
    override fun read(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun write(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }
}
