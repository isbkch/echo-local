package com.isbkch.echolocal.model

import okhttp3.HttpUrl

data class ModelAsset(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val url: HttpUrl,
) {
    val fileName: String get() = relativePath.substringAfterLast('/')
}

data class ModelManifest(
    val version: Int,
    val revision: String,
    val assets: List<ModelAsset>,
) {
    val totalBytes: Long = assets.sumOf(ModelAsset::sizeBytes)
}
