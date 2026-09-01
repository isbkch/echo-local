package com.isbkch.echolocal.tts

import audio.soniqo.speech.SpeechSynthesisResult
import audio.soniqo.speech.SpeechSynthesizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoniqoTtsEngineTest {
    @Test
    fun adapterDecodesLittleEndianPcmAndForwardsVoice() = runTest {
        val synth = RecordingSynthesizer(
            SpeechSynthesisResult(24_000, byteArrayOf(0x00, 0x40, 0x00, 0xC0.toByte())),
        )
        val engine = SoniqoTtsEngine(synth, StandardTestDispatcher(testScheduler))

        val result = engine.synthesize("Hello", "en", "bf_emma")

        assertArrayEquals(shortArrayOf(16_384, -16_384), result.samples)
        assertEquals(24_000, result.sampleRate)
        assertEquals("Hello", synth.lastText)
        assertEquals("en", synth.lastLanguage)
        assertEquals("bf_emma", synth.lastVoice)
    }

    @Test
    fun cancellationCallsPublishedStopApi() {
        val synth = RecordingSynthesizer(SpeechSynthesisResult(24_000, byteArrayOf()))

        SoniqoTtsEngine(synth).cancel()

        assertTrue(synth.stopCalled)
    }

    private class RecordingSynthesizer(
        private val result: SpeechSynthesisResult,
    ) : SpeechSynthesizer {
        override val sampleRate: Int get() = result.sampleRate
        var lastText: String? = null
        var lastLanguage: String? = null
        var lastVoice: String? = null
        var stopCalled = false

        override fun synthesize(text: String, language: String): SpeechSynthesisResult = result

        override fun synthesize(text: String, language: String, voice: String): SpeechSynthesisResult {
            lastText = text
            lastLanguage = language
            lastVoice = voice
            return result
        }

        override fun stop() {
            stopCalled = true
        }

        override fun close() = Unit
    }
}
