package com.isbkch.echolocal.model

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelIntegrityException(relativePath: String) :
    IllegalStateException("Model asset failed integrity validation: $relativePath")

class InsufficientStorageException(val requiredBytes: Long, val availableBytes: Long) :
    IllegalStateException(
        "Not enough storage for the local model: $requiredBytes bytes required, $availableBytes bytes available.",
    )

class ModelInstaller(
    private val root: File,
    private val fetcher: AssetFetcher,
    private val manifest: ModelManifest = KokoroModelManifest.current,
    private val stagingMarginBytes: Long = 32L * 1024L * 1024L,
    private val availableSpace: () -> Long = { root.usableSpace },
) {
    private val readyDirectory = root.resolve("ready")
    private val stagingDirectory = root.resolve("staging-v${manifest.version}")
    private val backupDirectory = root.resolve("ready.backup")

    suspend fun install(onProgress: (Long, Long, String) -> Unit): File = withContext(Dispatchers.IO) {
        check(root.mkdirs() || root.isDirectory) { "Unable to create the model directory." }
        if (isValidInstallation(readyDirectory)) return@withContext readyDirectory

        ensureSufficientStorage()

        var completedBytes = 0L
        for (asset in manifest.assets) {
            val target = stagingDirectory.resolve(asset.relativePath)
            val partial = stagingDirectory.resolve("${asset.relativePath}.part")
            if (isValidAsset(target, asset)) {
                completedBytes += asset.sizeBytes
                onProgress(completedBytes, manifest.totalBytes, asset.fileName)
                continue
            }
            if (target.exists()) target.delete()

            if (!isValidAsset(partial, asset)) {
                if (partial.length() >= asset.sizeBytes) partial.delete()
                fetcher.fetch(asset, partial) { assetBytes ->
                    onProgress(completedBytes + assetBytes, manifest.totalBytes, asset.fileName)
                }
            }
            if (!isValidAsset(partial, asset)) {
                partial.delete()
                throw ModelIntegrityException(asset.relativePath)
            }

            target.parentFile?.let { check(it.mkdirs() || it.isDirectory) }
            move(partial, target, replace = true)
            completedBytes += asset.sizeBytes
            onProgress(completedBytes, manifest.totalBytes, asset.fileName)
        }

        writeVersionMarker(stagingDirectory)
        if (!isValidInstallation(stagingDirectory)) throw ModelIntegrityException("installation")
        promoteStaging()
        readyDirectory
    }

    fun readyDirectoryOrNull(): File? = readyDirectory.takeIf(::isValidInstallation)

    fun ensureSufficientStorage() {
        check(root.mkdirs() || root.isDirectory) { "Unable to create the model directory." }
        check(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
            "Unable to create model staging storage."
        }
        val alreadyValidatedBytes = manifest.assets.sumOf { asset ->
            when {
                isValidAsset(stagingDirectory.resolve(asset.relativePath), asset) -> asset.sizeBytes
                isValidAsset(stagingDirectory.resolve("${asset.relativePath}.part"), asset) -> asset.sizeBytes
                else -> 0L
            }
        }
        val requiredBytes = manifest.totalBytes - alreadyValidatedBytes + stagingMarginBytes
        val availableBytes = availableSpace()
        if (availableBytes < requiredBytes) {
            throw InsufficientStorageException(requiredBytes, availableBytes)
        }
    }

    private fun promoteStaging() {
        if (backupDirectory.exists() && !backupDirectory.deleteRecursively()) {
            error("Unable to remove an obsolete model backup.")
        }
        var movedReadyToBackup = false
        try {
            if (readyDirectory.exists()) {
                move(readyDirectory, backupDirectory, replace = false)
                movedReadyToBackup = true
            }
            move(stagingDirectory, readyDirectory, replace = false)
            if (!isValidInstallation(readyDirectory)) throw ModelIntegrityException("promoted installation")
            if (backupDirectory.exists() && !backupDirectory.deleteRecursively()) {
                error("Unable to remove the replaced model backup.")
            }
        } catch (failure: Throwable) {
            if (!readyDirectory.exists() && movedReadyToBackup && backupDirectory.exists()) {
                move(backupDirectory, readyDirectory, replace = false)
            }
            throw failure
        }
    }

    private fun writeVersionMarker(directory: File) {
        val marker = directory.resolve("version.txt")
        FileOutputStream(marker, false).use { output ->
            output.write(versionMarker().encodeToByteArray())
            output.fd.sync()
        }
    }

    private fun isValidInstallation(directory: File): Boolean {
        if (!directory.isDirectory) return false
        if (directory.resolve("version.txt").takeIf(File::isFile)?.readText() != versionMarker()) return false
        return manifest.assets.all { isValidAsset(directory.resolve(it.relativePath), it) }
    }

    private fun isValidAsset(file: File, asset: ModelAsset): Boolean =
        file.isFile && file.length() == asset.sizeBytes && sha256(file) == asset.sha256

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun versionMarker(): String = "version=${manifest.version}\nrevision=${manifest.revision}\n"

    private fun move(source: File, destination: File, replace: Boolean) {
        val options = if (replace) {
            arrayOf(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } else {
            arrayOf(StandardCopyOption.ATOMIC_MOVE)
        }
        try {
            Files.move(source.toPath(), destination.toPath(), *options)
        } catch (_: AtomicMoveNotSupportedException) {
            val fallback = if (replace) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
            Files.move(source.toPath(), destination.toPath(), *fallback)
        }
    }
}
