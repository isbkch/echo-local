package com.isbkch.echolocal.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KokoroModelManifestTest {
    @Test
    fun manifestPinsEveryRequiredAsset() {
        val manifest = KokoroModelManifest.current

        assertEquals("2895b2025f1046fad6b51f8773debc3da8ba05df", manifest.revision)
        assertEquals(15, manifest.assets.size)
        assertEquals(171_185_109L, manifest.totalBytes)
        assertEquals(
            setOf("af_heart.bin", "af_bella.bin", "am_michael.bin", "bf_emma.bin", "bm_george.bin"),
            manifest.assets.filter { it.relativePath.startsWith("voices/") }.map { it.fileName }.toSet(),
        )
        assertTrue(manifest.assets.all { it.sha256.matches(Regex("[0-9a-f]{64}")) })
        assertTrue(manifest.assets.all { it.url.encodedPath.contains(manifest.revision) })
    }
}
