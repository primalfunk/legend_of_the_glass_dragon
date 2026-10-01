package com.legendoftheglassdragon

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.legendoftheglassdragon.audio.GameAudioController
import com.legendoftheglassdragon.audio.GameAudioProvider
import com.legendoftheglassdragon.data.AppPreferences
import com.legendoftheglassdragon.data.GameSaveRepository
import com.legendoftheglassdragon.data.TrophyRepository
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.ui.GlassDragonApp
import com.legendoftheglassdragon.ui.theme.LegendOfTheGlassDragonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val appPreferences = AppPreferences.from(applicationContext)
        val localAppState = appPreferences.load()
        val gameSaves = GameSaveRepository.from(applicationContext)
        val completedSeasons = GameSaveRepository.completedSeasonFrom(applicationContext)
        val trophyRepository = TrophyRepository.from(applicationContext)
        val newGameFactory = NewGameFactory()
        val audioController = GameAudioController(
            context = applicationContext,
            musicInitiallyEnabled = localAppState.musicEnabled,
            clickSoundInitiallyEnabled = localAppState.clickSoundEnabled,
        )
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        lifecycle.addObserver(audioController)
        setContent {
            GameAudioProvider(audioController) {
                LegendOfTheGlassDragonTheme {
                    GlassDragonApp(
                        appPreferences = appPreferences,
                        gameSaves = gameSaves,
                        completedSeasons = completedSeasons,
                        trophyRepository = trophyRepository,
                        newGameFactory = newGameFactory,
                        audioControls = audioController,
                        versionName = versionName,
                        onExit = ::finish,
                    )
                }
            }
        }
    }
}
