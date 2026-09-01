package com.isbkch.echolocal.audio

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedAudioStoreTest {
    @Test
    fun publishesOnlyACompletedWav() {
        val root = Files.createTempDirectory("echolocal-audio-store").toFile()
        try {
            val artifact = GeneratedAudioStore(root).publish(byteArrayOf(1, 2, 3))

            assertTrue(artifact.isFile)
            assertArrayEquals(byteArrayOf(1, 2, 3), artifact.readBytes())
            assertFalse(root.walkTopDown().any { it.name.endsWith(".staging") })
        } finally {
            root.deleteRecursively()
        }
    }
}
