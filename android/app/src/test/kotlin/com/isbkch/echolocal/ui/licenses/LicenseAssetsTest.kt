package com.isbkch.echolocal.ui.licenses

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicenseAssetsTest {
    @Test
    fun bundledNoticesCoverNativeRuntimeAndModel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notices = context.assets.open("licenses/third-party-notices.txt").bufferedReader().readText()

        listOf("Soniqo", "speech-core", "Kokoro", "ONNX Runtime", "LiteRT").forEach {
            assertTrue("Missing $it", notices.contains(it))
        }
        assertTrue(context.assets.open("licenses/apache-2.0.txt").available() > 0)
        assertTrue(context.assets.open("licenses/onnxruntime-mit.txt").available() > 0)
        assertTrue(context.assets.open("licenses/litert-third-party-notices.txt").available() > 0)
    }
}
