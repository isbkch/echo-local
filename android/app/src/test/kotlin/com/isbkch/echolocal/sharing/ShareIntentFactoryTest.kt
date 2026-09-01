package com.isbkch.echolocal.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentFactoryTest {
    @Test
    fun sharesOnlyCacheWavWithTemporaryReadPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wav = File(context.cacheDir, "generated/test.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }

        val intent = ShareIntentFactory.create(context, wav)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("audio/wav", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals("content", intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.scheme)
        assertFalse(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
    }

    @Test
    fun refusesFilesOutsideTheGeneratedCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outside = File(context.filesDir, "not-shareable.wav").apply { writeBytes(byteArrayOf(1)) }

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ShareIntentFactory.create(context, outside)
        }
    }
}
