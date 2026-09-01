package com.isbkch.echolocal.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareIntentFactory {
    fun create(context: Context, wavFile: File): Intent {
        val generatedDirectory = File(context.cacheDir, "generated").canonicalFile
        val shareableFile = wavFile.canonicalFile
        require(
            shareableFile.isFile &&
                shareableFile.extension.equals("wav", ignoreCase = true) &&
                shareableFile.path.startsWith(generatedDirectory.path + File.separator),
        ) { "Only generated cache WAV files can be shared." }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            shareableFile,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = WAV_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Echolocal speech", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private const val WAV_MIME_TYPE = "audio/wav"
}
