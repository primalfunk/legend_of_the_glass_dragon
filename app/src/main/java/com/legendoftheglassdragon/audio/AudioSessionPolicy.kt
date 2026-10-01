package com.legendoftheglassdragon.audio

enum class MusicCommand {
    START_NEXT,
    RESUME_CURRENT,
    PAUSE_CURRENT,
    NONE,
}

/** Pure lifecycle decisions kept separate from Android player mechanics. */
object AudioSessionPolicy {
    fun onEnterForeground(hasCurrentPlayer: Boolean): MusicCommand =
        if (hasCurrentPlayer) MusicCommand.RESUME_CURRENT else MusicCommand.START_NEXT

    fun onLeaveForeground(isPlaying: Boolean): MusicCommand =
        if (isPlaying) MusicCommand.PAUSE_CURRENT else MusicCommand.NONE

    fun onTrackCompleted(isForeground: Boolean): MusicCommand =
        if (isForeground) MusicCommand.START_NEXT else MusicCommand.NONE

    fun onMusicSettingChanged(
        enabled: Boolean,
        isForeground: Boolean,
        hasCurrentPlayer: Boolean,
    ): MusicCommand = when {
        !enabled -> MusicCommand.PAUSE_CURRENT
        !isForeground -> MusicCommand.NONE
        hasCurrentPlayer -> MusicCommand.RESUME_CURRENT
        else -> MusicCommand.START_NEXT
    }

    fun shouldPlaySelection(clickSoundEnabled: Boolean): Boolean = clickSoundEnabled
}
