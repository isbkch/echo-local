package com.isbkch.echolocal.model

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KokoroModelRepositoryTest {
    @Test
    fun schedulerProgressBecomesObservableModelState() = runTest {
        val root = Files.createTempDirectory("echolocal-model-repository").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val scheduler = FakeScheduler()
            val installer = ModelInstaller(root, NoOpFetcher, emptyManifest, stagingMarginBytes = 0)
            val repository = KokoroModelRepository(
                installer,
                scheduler,
                repositoryScope,
                dispatcher,
            )
            advanceUntilIdle()
            assertEquals(ModelInstallState.Missing, repository.state.value)

            repository.startDownload()
            advanceUntilIdle()
            assertEquals(1, scheduler.enqueueCount)
            scheduler.snapshots.emit(ModelDownloadSnapshot.Downloading(4, 10, "model.onnx"))
            advanceUntilIdle()

            assertEquals(ModelInstallState.Downloading(4, 10, "model.onnx"), repository.state.value)
        } finally {
            repositoryScope.cancel()
            root.deleteRecursively()
        }
    }

    @Test
    fun completedWorkRefreshesTheVerifiedReadyDirectory() = runTest {
        val root = Files.createTempDirectory("echolocal-model-repository-ready").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val payload = "ready".encodeToByteArray()
            val manifest = ModelManifest(
                1,
                "test",
                listOf(ModelAsset("model.bin", payload.size.toLong(), sha256(payload), "https://example.com/model.bin".toHttpUrl())),
            )
            val installer = ModelInstaller(root, object : AssetFetcher {
                override suspend fun fetch(asset: ModelAsset, destination: File, onProgress: (Long) -> Unit) {
                    destination.writeBytes(payload)
                    onProgress(payload.size.toLong())
                }
            }, manifest, stagingMarginBytes = 0)
            val scheduler = FakeScheduler()
            val repository = KokoroModelRepository(
                installer,
                scheduler,
                repositoryScope,
                dispatcher,
            )
            advanceUntilIdle()
            assertEquals(ModelInstallState.Missing, repository.state.value)

            installer.install { _, _, _ -> }
            scheduler.snapshots.emit(ModelDownloadSnapshot.Completed)
            advanceUntilIdle()

            assertTrue(repository.state.value is ModelInstallState.Ready)
            assertTrue(repository.readyDirectory()?.resolve("model.bin")?.isFile == true)
        } finally {
            repositoryScope.cancel()
            root.deleteRecursively()
        }
    }

    @Test
    fun insufficientStorageFailsBeforeWorkIsEnqueued() = runTest {
        val root = Files.createTempDirectory("echolocal-model-repository-space").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val payload = "model".encodeToByteArray()
            val manifest = ModelManifest(
                1,
                "test",
                listOf(ModelAsset("model.bin", payload.size.toLong(), sha256(payload), "https://example.com/model.bin".toHttpUrl())),
            )
            val scheduler = FakeScheduler()
            val installer = ModelInstaller(
                root,
                NoOpFetcher,
                manifest,
                stagingMarginBytes = 0,
                availableSpace = { 0L },
            )
            val repository = KokoroModelRepository(installer, scheduler, repositoryScope, dispatcher)
            advanceUntilIdle()

            repository.startDownload()
            advanceUntilIdle()

            val failed = repository.state.value as ModelInstallState.Failed
            assertTrue(failed.message.contains("required"))
            assertTrue(failed.message.contains("available"))
            assertEquals(0, scheduler.enqueueCount)
        } finally {
            repositoryScope.cancel()
            root.deleteRecursively()
        }
    }

    private class FakeScheduler : ModelDownloadScheduler {
        override val snapshots = MutableSharedFlow<ModelDownloadSnapshot>(extraBufferCapacity = 4)
        var enqueueCount = 0
        override fun enqueue() {
            enqueueCount += 1
            snapshots.tryEmit(ModelDownloadSnapshot.Enqueued)
        }
    }

    private object NoOpFetcher : AssetFetcher {
        override suspend fun fetch(asset: ModelAsset, destination: File, onProgress: (Long) -> Unit) = Unit
    }

    companion object {
        private val emptyManifest = ModelManifest(1, "empty", emptyList())

        private fun sha256(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
