package com.isbkch.echolocal.model

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface ModelDownloadScheduler {
    val snapshots: Flow<ModelDownloadSnapshot>
    fun enqueue()
}

class WorkManagerModelDownloadScheduler(context: Context) : ModelDownloadScheduler {
    private val workManager = WorkManager.getInstance(context)

    override val snapshots: Flow<ModelDownloadSnapshot> = callbackFlow {
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME)
        val observer = Observer<List<WorkInfo>> { infos ->
            infos.maxByOrNull { it.runAttemptCount }?.let { trySend(it.toSnapshot()) }
        }
        liveData.observeForever(observer)
        awaitClose { liveData.removeObserver(observer) }
    }

    override fun enqueue() {
        val request = OneTimeWorkRequestBuilder<KokoroModelDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private fun WorkInfo.toSnapshot(): ModelDownloadSnapshot = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ModelDownloadSnapshot.Enqueued
        WorkInfo.State.RUNNING -> when (progress.getString(KokoroModelDownloadWorker.KEY_PHASE)) {
            KokoroModelDownloadWorker.PHASE_VERIFYING -> ModelDownloadSnapshot.Verifying
            else -> ModelDownloadSnapshot.Downloading(
                progress.getLong(KokoroModelDownloadWorker.KEY_COMPLETED_BYTES, 0),
                progress.getLong(KokoroModelDownloadWorker.KEY_TOTAL_BYTES, KokoroModelManifest.current.totalBytes),
                progress.getString(KokoroModelDownloadWorker.KEY_FILE_NAME).orEmpty(),
            )
        }
        WorkInfo.State.SUCCEEDED -> ModelDownloadSnapshot.Completed
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> ModelDownloadSnapshot.Failed(
            outputData.getString(KokoroModelDownloadWorker.KEY_ERROR) ?: "Model installation failed.",
        )
    }

    companion object {
        private const val WORK_NAME = "echolocal-kokoro-model-v1"
    }
}
