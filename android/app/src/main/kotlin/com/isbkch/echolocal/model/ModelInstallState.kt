package com.isbkch.echolocal.model

import java.io.File

sealed interface ModelInstallState {
    data object Checking : ModelInstallState
    data object Missing : ModelInstallState
    data class Downloading(
        val completedBytes: Long,
        val totalBytes: Long,
        val fileName: String,
    ) : ModelInstallState
    data object Verifying : ModelInstallState
    data class Ready(val directory: File) : ModelInstallState
    data class Failed(val message: String) : ModelInstallState
}

sealed interface ModelDownloadSnapshot {
    data object Enqueued : ModelDownloadSnapshot
    data class Downloading(val completedBytes: Long, val totalBytes: Long, val fileName: String) : ModelDownloadSnapshot
    data object Verifying : ModelDownloadSnapshot
    data object Completed : ModelDownloadSnapshot
    data class Failed(val message: String) : ModelDownloadSnapshot
}
