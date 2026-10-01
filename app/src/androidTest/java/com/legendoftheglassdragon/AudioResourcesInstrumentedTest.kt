package com.legendoftheglassdragon

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.legendoftheglassdragon.audio.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AudioResourcesInstrumentedTest {
    @Test
    fun allPrincipalAudioAssetsArePackagedAndDecodable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources
        val musicIds = MusicTrack.entries.map { track ->
            resources.getIdentifier(track.packagedName, "raw", context.packageName).also { id ->
                assertNotEquals("Missing ${track.packagedName}", 0, id)
                val player = MediaPlayer.create(context, id)
                assertNotNull("Undecodable ${track.packagedName}", player)
                assertTrue("Empty ${track.packagedName}", player!!.duration > 0)
                player.release()
            }
        }

        val clickId = resources.getIdentifier("ui_click", "raw", context.packageName)
        assertNotEquals("Missing Android-compatible click derivative", 0, clickId)
        assertTrue("ui_click.wav entered music catalog", clickId !in musicIds)
        val clickLoad = CountDownLatch(1)
        val clickStatus = AtomicInteger(Int.MIN_VALUE)
        val soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            clickStatus.set(status)
            clickLoad.countDown()
        }
        assertTrue("SoundPool timed out loading click.mp3", soundPool.load(context, clickId, 1) > 0)
        assertTrue("SoundPool timed out loading click.mp3", clickLoad.await(5, TimeUnit.SECONDS))
        assertEquals("SoundPool could not decode ui_click.wav", 0, clickStatus.get())
        soundPool.release()
        assertEquals(15, musicIds.distinct().size)
    }
}
