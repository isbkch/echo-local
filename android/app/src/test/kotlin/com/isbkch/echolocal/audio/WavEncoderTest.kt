package com.isbkch.echolocal.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class WavEncoderTest {
    @Test
    fun writesMonoSixteenBitPcmHeaderAndPayload() {
        val data = WavEncoder.encode(floatArrayOf(0f, 0.5f, -0.5f, 1f, -1f), 24_000)
        val littleEndian = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals("RIFF", data.copyOfRange(0, 4).decodeToString())
        assertEquals("WAVE", data.copyOfRange(8, 12).decodeToString())
        assertEquals(1, littleEndian.getShort(20).toInt())
        assertEquals(1, littleEndian.getShort(22).toInt())
        assertEquals(24_000, littleEndian.getInt(24))
        assertEquals(16, littleEndian.getShort(34).toInt())
        assertEquals("data", data.copyOfRange(36, 40).decodeToString())
        assertEquals(54, data.size)
        assertEquals(Short.MAX_VALUE, littleEndian.getShort(50))
        assertEquals((-Short.MAX_VALUE).toShort(), littleEndian.getShort(52))
    }
}
