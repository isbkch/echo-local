package com.isbkch.echolocal.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavTooLargeException : IllegalArgumentException("The recording is too long to export as a WAV file.")

object WavEncoder {
    private const val HEADER_BYTES = 44L
    private const val BYTES_PER_SAMPLE = 2L

    fun encode(samples: FloatArray, sampleRate: Int): ByteArray {
        require(sampleRate > 0) { "Sample rate must be positive." }
        val audioByteCount = samples.size.toLong() * BYTES_PER_SAMPLE
        if (audioByteCount > UInt.MAX_VALUE.toLong() - HEADER_BYTES) throw WavTooLargeException()

        val output = ByteBuffer
            .allocate((HEADER_BYTES + audioByteCount).toInt())
            .order(ByteOrder.LITTLE_ENDIAN)
        output.put("RIFF".encodeToByteArray())
        output.putInt((audioByteCount + 36L).toInt())
        output.put("WAVE".encodeToByteArray())
        output.put("fmt ".encodeToByteArray())
        output.putInt(16)
        output.putShort(1)
        output.putShort(1)
        output.putInt(sampleRate)
        output.putInt(sampleRate * BYTES_PER_SAMPLE.toInt())
        output.putShort(BYTES_PER_SAMPLE.toShort())
        output.putShort(16)
        output.put("data".encodeToByteArray())
        output.putInt(audioByteCount.toInt())

        samples.forEach { sample ->
            val finite = sample.takeIf(Float::isFinite) ?: 0f
            output.putShort((finite.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }
        return output.array()
    }
}
