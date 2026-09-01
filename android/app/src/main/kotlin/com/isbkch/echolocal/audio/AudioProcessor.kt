package com.isbkch.echolocal.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object AudioProcessor {
    private const val TRIM_THRESHOLD = 0.004f
    private const val TRIM_PADDING_SECONDS = 0.025
    private const val EDGE_FADE_SECONDS = 0.008
    private val NORMALIZE_TARGET = 10.0.pow(-1.0 / 20.0).toFloat()
    private const val MAX_GAIN = 6f

    fun pcm16ToFloat(pcm16: ByteArray): FloatArray {
        val input = ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(pcm16.size / Short.SIZE_BYTES) {
            val sample = input.short.toInt()
            if (sample == Short.MIN_VALUE.toInt()) -1f else sample / Short.MAX_VALUE.toFloat()
        }
    }

    fun finish(
        samples: FloatArray,
        sampleRate: Int,
        normalize: Boolean,
        trimSilence: Boolean,
    ): FloatArray {
        if (samples.isEmpty()) return samples

        var output = FloatArray(samples.size) { index ->
            samples[index].takeIf(Float::isFinite) ?: 0f
        }
        if (trimSilence) output = trim(output, sampleRate)
        if (normalize) normalizePeak(output)
        applyEdgeFade(output, sampleRate)
        return output
    }

    fun silence(durationSeconds: Double, sampleRate: Int): FloatArray =
        FloatArray(max(0, (durationSeconds * sampleRate).toInt()))

    private fun trim(samples: FloatArray, sampleRate: Int): FloatArray {
        val first = samples.indexOfFirst { abs(it) >= TRIM_THRESHOLD }
        val last = samples.indexOfLast { abs(it) >= TRIM_THRESHOLD }
        if (first == -1 || last == -1) return samples

        val padding = (sampleRate * TRIM_PADDING_SECONDS).toInt()
        val lower = max(0, first - padding)
        val upper = min(samples.lastIndex, last + padding)
        return samples.copyOfRange(lower, upper + 1)
    }

    private fun normalizePeak(samples: FloatArray) {
        val peak = samples.maxOfOrNull { abs(it) } ?: 0f
        if (peak <= 0.0001f) return

        val gain = min(NORMALIZE_TARGET / peak, MAX_GAIN)
        for (index in samples.indices) {
            samples[index] = (samples[index] * gain).coerceIn(-1f, 1f)
        }
    }

    private fun applyEdgeFade(samples: FloatArray, sampleRate: Int) {
        val fadeCount = min(samples.size / 2, max(1, (sampleRate * EDGE_FADE_SECONDS).toInt()))
        if (fadeCount <= 1) return

        for (index in 0 until fadeCount) {
            val gain = index.toFloat() / (fadeCount - 1).toFloat()
            samples[index] *= gain
            samples[samples.lastIndex - index] *= gain
        }
    }
}
