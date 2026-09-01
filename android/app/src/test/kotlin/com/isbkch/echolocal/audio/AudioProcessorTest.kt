package com.isbkch.echolocal.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProcessorTest {
    @Test
    fun littleEndianPcm16ConvertsToFiniteFloats() {
        val bytes = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(0)
            .putShort(16_384)
            .putShort((-16_384).toShort())
            .putShort(Short.MIN_VALUE)
            .array()

        assertArrayEquals(floatArrayOf(0f, 0.5f, -0.5f, -1f), AudioProcessor.pcm16ToFloat(bytes), 0.0001f)
    }

    @Test
    fun normalizationTargetsMinusOneDecibel() {
        val output = AudioProcessor.finish(
            floatArrayOf(0f, 0.1f, -0.2f, 0.05f, 0f),
            sampleRate = 100,
            normalize = true,
            trimSilence = false,
        )

        assertEquals(0.891f, output.maxOf { abs(it) }, 0.002f)
    }

    @Test
    fun trimmingKeepsTwentyFiveMillisecondsOfPadding() {
        val input = FloatArray(100) + floatArrayOf(0.2f, 0.4f, 0.2f) + FloatArray(100)

        val output = AudioProcessor.finish(input, 100, normalize = false, trimSilence = true)

        assertTrue(output.size in 4 until input.size)
    }

    @Test
    fun finishingSanitizesNonFiniteSamples() {
        val output = AudioProcessor.finish(
            floatArrayOf(Float.NaN, 0.2f, Float.NEGATIVE_INFINITY, -0.4f, 0f),
            sampleRate = 100,
            normalize = true,
            trimSilence = false,
        )

        assertTrue(output.all(Float::isFinite))
        assertEquals(0.891f, output.maxOf { abs(it) }, 0.002f)
    }

    @Test
    fun waveformHasRequestedBoundedBuckets() {
        val input = FloatArray(1_000) { sin(it / 15.0).toFloat() }

        val output = WaveformReducer.reduce(input, 100)

        assertEquals(100, output.size)
        assertTrue(output.all { it in 0f..1f })
    }
}
