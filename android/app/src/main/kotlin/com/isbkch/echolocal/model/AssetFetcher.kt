package com.isbkch.echolocal.model

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface AssetFetcher {
    suspend fun fetch(asset: ModelAsset, destination: File, onProgress: (downloadedBytes: Long) -> Unit)
}

class OkHttpAssetFetcher(private val client: OkHttpClient) : AssetFetcher {
    override suspend fun fetch(
        asset: ModelAsset,
        destination: File,
        onProgress: (downloadedBytes: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        destination.parentFile?.let { check(it.mkdirs() || it.isDirectory) }
        val existingBytes = destination.takeIf(File::isFile)?.length() ?: 0L
        val request = Request.Builder()
            .url(asset.url)
            .header("User-Agent", "Echolocal-Android/0.1")
            .apply { if (existingBytes > 0) header("Range", "bytes=$existingBytes-") }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Model download failed with HTTP ${response.code}.")
            val body = response.body ?: throw IOException("Model download returned an empty response.")
            val appending = existingBytes > 0 && response.code == 206
            var downloadedBytes = if (appending) existingBytes else 0L
            FileOutputStream(destination, appending).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count
                        onProgress(downloadedBytes)
                    }
                }
                output.fd.sync()
            }
        }
    }
}
