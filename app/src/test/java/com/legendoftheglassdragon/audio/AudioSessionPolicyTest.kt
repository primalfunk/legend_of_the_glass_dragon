package com.legendoftheglassdragon.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSessionPolicyTest {
    @Test
    fun foregroundStartsOnlyWhenNoTrackExistsOtherwiseResumes() {
        assertEquals(MusicCommand.START_NEXT, AudioSessionPolicy.onEnterForeground(false))
        assertEquals(MusicCommand.RESUME_CURRENT, AudioSessionPolicy.onEnterForeground(true))
    }

    @Test
    fun backgroundPausesOnlyActivePlayback() {
        assertEquals(MusicCommand.PAUSE_CURRENT, AudioSessionPolicy.onLeaveForeground(true))
        assertEquals(MusicCommand.NONE, AudioSessionPolicy.onLeaveForeground(false))
    }

    @Test
    fun completionAdvancesOnlyWhileGameIsForeground() {
        assertEquals(MusicCommand.START_NEXT, AudioSessionPolicy.onTrackCompleted(true))
        assertEquals(MusicCommand.NONE, AudioSessionPolicy.onTrackCompleted(false))
    }

    @Test
    fun musicSettingTakesEffectWithoutCreatingDuplicateSessions() {
        assertEquals(
            MusicCommand.PAUSE_CURRENT,
            AudioSessionPolicy.onMusicSettingChanged(false, true, true),
        )
        assertEquals(
            MusicCommand.RESUME_CURRENT,
            AudioSessionPolicy.onMusicSettingChanged(true, true, true),
        )
        assertEquals(
            MusicCommand.START_NEXT,
            AudioSessionPolicy.onMusicSettingChanged(true, true, false),
        )
        assertEquals(
            MusicCommand.NONE,
            AudioSessionPolicy.onMusicSettingChanged(true, false, true),
        )
    }

    @Test
    fun clickPreferenceGatesOnlySelectionAudio() {
        assertTrue(AudioSessionPolicy.shouldPlaySelection(true))
        assertFalse(AudioSessionPolicy.shouldPlaySelection(false))
    }
}
