package com.isbkch.echolocal.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class GeneratedAudioStore(private val directory: File) {
    fun publish(wavData: ByteArray): File {
        check(directory.mkdirs() || directory.isDirectory) { "Unable to create generated-audio cache." }
        val staging = File(directory, "generated.staging")
        FileOutputStream(staging, false).use { output ->
            output.write(wavData)
            output.fd.sync()
        }

        val published = File(directory, "${UUID.randomUUID()}.wav")
        try {
            Files.move(
                staging.toPath(),
                published.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staging.toPath(), published.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return published
    }
}
