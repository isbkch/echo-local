package com.isbkch.echolocal.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object WaveformReducer {
    fun reduce(samples: FloatArray, bucketCount: Int = 420): FloatArray {
        if (samples.isEmpty() || bucketCount <= 0) return floatArrayOf()
        val stride = max(1, samples.size / bucketCount)
        val result = ArrayList<Float>(min(samples.size, bucketCount + 1))

        var start = 0
        while (start < samples.size) {
            val end = min(samples.size, start + stride)
            var peak = 0f
            for (index in start until end) {
                val candidate = samples[index].takeIf(Float::isFinite)?.let(::abs) ?: 0f
                peak = max(peak, candidate)
            }
            result += peak.coerceIn(0f, 1f)
            start += stride
        }
        return result.toFloatArray()
    }
}
