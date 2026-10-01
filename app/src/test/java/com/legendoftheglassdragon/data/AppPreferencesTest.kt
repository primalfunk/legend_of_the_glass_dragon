package com.legendoftheglassdragon.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesTest {
    @Test
    fun audioPreferencesDefaultOn() {
        assertEquals(
            LocalAppState(musicEnabled = true, clickSoundEnabled = true),
            AppPreferences(InMemoryBooleanStore()).load(),
        )
    }

    @Test
    fun audioPreferencesSurviveRoundTrip() {
        val storage = InMemoryBooleanStore()
        AppPreferences(storage).apply {
            setMusicEnabled(false)
            setClickSoundEnabled(false)
        }

        val reloaded = AppPreferences(storage).load()
        assertEquals(false, reloaded.musicEnabled)
        assertEquals(false, reloaded.clickSoundEnabled)
    }
}

private class InMemoryBooleanStore : BooleanKeyValueStore {
    private val values = mutableMapOf<String, Boolean>()

    override fun read(key: String, defaultValue: Boolean): Boolean = values[key] ?: defaultValue

    override fun write(key: String, value: Boolean) {
        values[key] = value
    }
}
