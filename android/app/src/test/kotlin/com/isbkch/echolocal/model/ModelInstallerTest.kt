package com.isbkch.echolocal.model

import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelInstallerTest {
    @Test
    fun validStagingIsPromotedAtomically() = withTemporaryDirectory { root ->
        val payload = "voice".encodeToByteArray()
        val manifest = manifestOf("voices/af_heart.bin", payload)
        val installer = ModelInstaller(root, writingFetcher(payload), manifest, stagingMarginBytes = 0)

        val ready = runBlocking { installer.install { _, _, _ -> } }

        assertEquals(payload.decodeToString(), ready.resolve("voices/af_heart.bin").readText())
        assertFalse(root.resolve("staging-v${manifest.version}").exists())
        assertEquals(ready, installer.readyDirectoryOrNull())
    }

    @Test
    fun invalidHashNeverReplacesTheReadyModel() = withTemporaryDirectory { root ->
        val oldPayload = "old-ready".encodeToByteArray()
        val oldManifest = manifestOf("old.bin", oldPayload, version = 1)
        val oldInstaller = ModelInstaller(root, writingFetcher(oldPayload), oldManifest, stagingMarginBytes = 0)
        val oldReady = runBlocking { oldInstaller.install { _, _, _ -> } }
        val oldVersion = oldReady.resolve("version.txt").readText()

        val expected = "expected".encodeToByteArray()
        val newManifest = manifestOf("new.bin", expected, version = 2)
        val newInstaller = ModelInstaller(
            root,
            writingFetcher("corrupt".encodeToByteArray()),
            newManifest,
            stagingMarginBytes = 0,
        )

        val failure = runCatching { runBlocking { newInstaller.install { _, _, _ -> } } }.exceptionOrNull()

        assertTrue(failure is ModelIntegrityException)
        assertEquals(oldVersion, root.resolve("ready/version.txt").readText())
        assertTrue(root.resolve("ready/old.bin").isFile)
        assertFalse(root.resolve("ready/new.bin").exists())
    }

    @Test
    fun aPartialResponseAppendsToTheExistingRange() = withTemporaryDirectory { root ->
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(206).setBody("def"))
        server.start()
        try {
            val destination = root.resolve("asset.part").apply { writeText("abc") }
            val asset = ModelAsset("asset.bin", 6, sha256("abcdef".encodeToByteArray()), server.url("asset.bin"))

            runBlocking { OkHttpAssetFetcher(OkHttpClient()).fetch(asset, destination) { } }

            assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
            assertEquals("abcdef", destination.readText())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun aFullResponseRestartsInsteadOfAppending() = withTemporaryDirectory { root ->
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("abcdef"))
        server.start()
        try {
            val destination = root.resolve("asset.part").apply { writeText("abc") }
            val asset = ModelAsset("asset.bin", 6, sha256("abcdef".encodeToByteArray()), server.url("asset.bin"))

            runBlocking { OkHttpAssetFetcher(OkHttpClient()).fetch(asset, destination) { } }

            assertEquals("abcdef", destination.readText())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun aCompleteCorruptPartialIsDiscardedBeforeRetry() = withTemporaryDirectory { root ->
        val expected = "right".encodeToByteArray()
        val manifest = manifestOf("asset.bin", expected)
        root.resolve("staging-v${manifest.version}/asset.bin.part").apply {
            parentFile?.mkdirs()
            writeText("wrong")
        }
        var existingLengthAtFetch = -1L
        val installer = ModelInstaller(
            root,
            object : AssetFetcher {
                override suspend fun fetch(asset: ModelAsset, destination: File, onProgress: (Long) -> Unit) {
                    existingLengthAtFetch = if (destination.exists()) destination.length() else 0L
                    destination.writeBytes(expected)
                    onProgress(expected.size.toLong())
                }
            },
            manifest,
            stagingMarginBytes = 0,
        )

        runBlocking { installer.install { _, _, _ -> } }

        assertEquals(0L, existingLengthAtFetch)
        assertEquals("right", root.resolve("ready/asset.bin").readText())
    }

    private fun writingFetcher(payload: ByteArray) = object : AssetFetcher {
        override suspend fun fetch(asset: ModelAsset, destination: File, onProgress: (Long) -> Unit) {
            destination.parentFile?.mkdirs()
            destination.writeBytes(payload)
            onProgress(payload.size.toLong())
        }
    }

    private fun manifestOf(path: String, payload: ByteArray, version: Int = 1) = ModelManifest(
        version = version,
        revision = "test-revision-$version",
        assets = listOf(ModelAsset(path, payload.size.toLong(), sha256(payload), okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("example.com")
            .addPathSegment(path)
            .build())),
    )

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("echolocal-model-installer").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
