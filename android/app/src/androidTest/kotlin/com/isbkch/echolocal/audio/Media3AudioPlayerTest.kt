package com.isbkch.echolocal.audio

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3AudioPlayerTest {
    @Test
    fun generatedWavCanLoadPlayPauseAndSeek() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val wav = File(context.cacheDir, "generated/player-contract.wav").apply {
            parentFile?.mkdirs()
            writeBytes(WavEncoder.encode(FloatArray(2_400) { 0.05f }, 24_000))
        }
        var audioPlayer: Media3AudioPlayer? = null
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                audioPlayer = Media3AudioPlayer(context).also { it.load(wav) }
            }
            waitFor { audioPlayer?.snapshot?.value?.durationMillis?.let { it > 0L } == true }

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                audioPlayer?.playPause()
                audioPlayer?.playPause()
                audioPlayer?.seekTo(50)
            }

            assertTrue(audioPlayer?.snapshot?.value?.positionMillis in 0L..100L)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { audioPlayer?.close() }
            wav.delete()
        }
    }

    private fun waitFor(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000L
        while (!predicate() && System.currentTimeMillis() < deadline) Thread.sleep(25L)
        assertTrue(predicate())
    }
}
