package com.isbkch.echolocal.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient

class KokoroModelDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val manifest = KokoroModelManifest.current
        val installer = ModelInstaller(
            root = File(applicationContext.filesDir, MODEL_ROOT),
            fetcher = OkHttpAssetFetcher(OkHttpClient()),
            manifest = manifest,
        )
        return try {
            installer.install { completedBytes, totalBytes, fileName ->
                setProgressAsync(
                    workDataOf(
                        KEY_PHASE to PHASE_DOWNLOADING,
                        KEY_COMPLETED_BYTES to completedBytes,
                        KEY_TOTAL_BYTES to totalBytes,
                        KEY_FILE_NAME to fileName,
                    ),
                )
            }
            setProgress(workDataOf(KEY_PHASE to PHASE_VERIFYING))
            Result.success()
        } catch (failure: InsufficientStorageException) {
            Result.failure(workDataOf(KEY_ERROR to failure.message))
        } catch (failure: ModelIntegrityException) {
            Result.failure(workDataOf(KEY_ERROR to failure.message))
        } catch (failure: IOException) {
            Result.retry()
        } catch (failure: Throwable) {
            Result.failure(workDataOf(KEY_ERROR to (failure.message ?: "Model installation failed.")))
        }
    }

    companion object {
        const val MODEL_ROOT = "models/kokoro"
        const val KEY_PHASE = "phase"
        const val KEY_COMPLETED_BYTES = "completedBytes"
        const val KEY_TOTAL_BYTES = "totalBytes"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_ERROR = "error"
        const val PHASE_DOWNLOADING = "downloading"
        const val PHASE_VERIFYING = "verifying"
    }
}
