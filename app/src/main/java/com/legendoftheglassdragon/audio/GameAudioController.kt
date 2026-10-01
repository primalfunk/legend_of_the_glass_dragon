package com.legendoftheglassdragon.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.legendoftheglassdragon.R

/** One activity-scoped music player and one short-effect pool for the whole game. */
class GameAudioController(
    context: Context,
    musicInitiallyEnabled: Boolean = true,
    clickSoundInitiallyEnabled: Boolean = true,
) : DefaultLifecycleObserver, AudioControls {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val shuffleBag = MusicShuffleBag(MusicTrack.entries)
    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val effectAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(effectAttributes)
        .build()
    private val clickSoundId: Int
    private var clickLoaded = false
    private var mediaPlayer: MediaPlayer? = null
    private var foreground = false
    private var hasAudioFocus = false
    private var resumeAfterFocusGain = false
    private var consecutiveTrackFailures = 0
    private var musicEnabled = musicInitiallyEnabled
    private var clickSoundEnabled = clickSoundInitiallyEnabled

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(musicAttributes)
        .setOnAudioFocusChangeListener(::onAudioFocusChanged)
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            clickLoaded = status == 0
            if (clickLoaded) {
                Log.i(TAG, "selection_sound_loaded")
            } else {
                Log.e(TAG, "selection_sound_failed status=$status")
            }
        }
        clickSoundId = soundPool.load(appContext, R.raw.ui_click, 1)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        if (!musicEnabled) return
        hasAudioFocus = requestAudioFocus()
        if (!hasAudioFocus) return
        when (AudioSessionPolicy.onEnterForeground(mediaPlayer != null)) {
            MusicCommand.START_NEXT -> startNextTrack()
            MusicCommand.RESUME_CURRENT -> mediaPlayer?.start()
            else -> Unit
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        when (AudioSessionPolicy.onLeaveForeground(mediaPlayer?.isPlaying == true)) {
            MusicCommand.PAUSE_CURRENT -> mediaPlayer?.pause()
            else -> Unit
        }
        resumeAfterFocusGain = false
        if (hasAudioFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasAudioFocus = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    override fun playSelection() {
        if (clickLoaded && AudioSessionPolicy.shouldPlaySelection(clickSoundEnabled)) {
            soundPool.play(clickSoundId, CLICK_VOLUME, CLICK_VOLUME, 1, 0, 1f)
            Log.d(TAG, "selection_click")
        }
    }

    override fun setMusicEnabled(enabled: Boolean) {
        if (musicEnabled == enabled) return
        musicEnabled = enabled
        when (
            AudioSessionPolicy.onMusicSettingChanged(
                enabled = enabled,
                isForeground = foreground,
                hasCurrentPlayer = mediaPlayer != null,
            )
        ) {
            MusicCommand.PAUSE_CURRENT -> {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
                resumeAfterFocusGain = false
                if (hasAudioFocus) audioManager.abandonAudioFocusRequest(focusRequest)
                hasAudioFocus = false
            }
            MusicCommand.RESUME_CURRENT -> {
                hasAudioFocus = hasAudioFocus || requestAudioFocus()
                if (hasAudioFocus) mediaPlayer?.start()
            }
            MusicCommand.START_NEXT -> {
                hasAudioFocus = hasAudioFocus || requestAudioFocus()
                if (hasAudioFocus) startNextTrack()
            }
            MusicCommand.NONE -> Unit
        }
    }

    override fun setClickSoundEnabled(enabled: Boolean) {
        clickSoundEnabled = enabled
    }

    fun release() {
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.setOnErrorListener(null)
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool.release()
        if (hasAudioFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasAudioFocus = false
        foreground = false
    }

    private fun startNextTrack() {
        if (!musicEnabled || !foreground || !hasAudioFocus || mediaPlayer != null) return
        val track = shuffleBag.next()
        val player = MediaPlayer.create(
            appContext,
            rawResource(track),
            musicAttributes,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        if (player == null) {
            handleTrackFailure(track, "player creation returned null")
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener { completed ->
            if (mediaPlayer !== completed) {
                completed.release()
                return@setOnCompletionListener
            }
            completed.release()
            mediaPlayer = null
            consecutiveTrackFailures = 0
            if (musicEnabled &&
                AudioSessionPolicy.onTrackCompleted(foreground) == MusicCommand.START_NEXT
            ) {
                startNextTrack()
            }
        }
        player.setOnErrorListener { failed, what, extra ->
            if (mediaPlayer === failed) mediaPlayer = null
            failed.release()
            handleTrackFailure(track, "MediaPlayer error what=$what extra=$extra")
            true
        }
        player.start()
        consecutiveTrackFailures = 0
        Log.i(TAG, "music_start=${track.packagedName} remaining=${shuffleBag.remainingCount()}")
    }

    private fun handleTrackFailure(track: MusicTrack, detail: String) {
        consecutiveTrackFailures += 1
        Log.e(TAG, "music_failed=${track.packagedName} $detail")
        if (musicEnabled && foreground && hasAudioFocus &&
            consecutiveTrackFailures < MusicTrack.entries.size
        ) {
            startNextTrack()
        }
    }

    private fun onAudioFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                mediaPlayer?.setVolume(1f, 1f)
                if (musicEnabled && foreground && resumeAfterFocusGain) mediaPlayer?.start()
                if (musicEnabled && foreground && mediaPlayer == null) startNextTrack()
                resumeAfterFocusGain = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                mediaPlayer?.setVolume(DUCK_VOLUME, DUCK_VOLUME)
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                resumeAfterFocusGain = mediaPlayer?.isPlaying == true
                mediaPlayer?.pause()
                if (change == AudioManager.AUDIOFOCUS_LOSS) hasAudioFocus = false
            }
        }
    }

    private fun rawResource(track: MusicTrack): Int = when (track) {
        MusicTrack.MUSS_01 -> R.raw.muss_01
        MusicTrack.MUSS_02 -> R.raw.muss_02
        MusicTrack.MUSS_03 -> R.raw.muss_03
        MusicTrack.MUSS_04 -> R.raw.muss_04
        MusicTrack.MUSS_05 -> R.raw.muss_05
        MusicTrack.MUSS_06 -> R.raw.muss_06
        MusicTrack.MUSS_07 -> R.raw.muss_07
        MusicTrack.MUSS_08 -> R.raw.muss_08
        MusicTrack.MUSS_09 -> R.raw.muss_09
        MusicTrack.MUSS_10 -> R.raw.muss_10
        MusicTrack.MUSS_11 -> R.raw.muss_11
        MusicTrack.MUSS_12 -> R.raw.muss_12
        MusicTrack.MUSS_13 -> R.raw.muss_13
        MusicTrack.MUSS_14 -> R.raw.muss_14
        MusicTrack.MUSS_15 -> R.raw.muss_15
    }

    private fun requestAudioFocus(): Boolean =
        audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    private companion object {
        const val TAG = "LGD_AUDIO"
        const val CLICK_VOLUME = 0.58f
        const val DUCK_VOLUME = 0.22f
    }
}
