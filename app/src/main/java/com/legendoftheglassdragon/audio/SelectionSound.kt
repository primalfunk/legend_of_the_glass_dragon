package com.legendoftheglassdragon.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

fun interface SelectionSound {
    fun playSelection()
}

interface AudioControls : SelectionSound {
    fun setMusicEnabled(enabled: Boolean)
    fun setClickSoundEnabled(enabled: Boolean)
}

private val SilentSelectionSound = SelectionSound {}

val LocalSelectionSound = staticCompositionLocalOf { SilentSelectionSound }

@Composable
fun GameAudioProvider(
    selectionSound: SelectionSound,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSelectionSound provides selectionSound, content = content)
}
