package com.isbkch.echolocal.model

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ModelRepository {
    val state: StateFlow<ModelInstallState>
    fun refresh()
    fun startDownload()
    fun readyDirectory(): File?
}

class KokoroModelRepository(
    private val installer: ModelInstaller,
    private val scheduler: ModelDownloadScheduler,
    private val scope: CoroutineScope,
    private val verificationDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ModelRepository {
    private val mutableState = MutableStateFlow<ModelInstallState>(ModelInstallState.Checking)
    override val state: StateFlow<ModelInstallState> = mutableState.asStateFlow()

    init {
        refresh()
        scope.launch {
            scheduler.snapshots.collect { snapshot ->
                when (snapshot) {
                    ModelDownloadSnapshot.Enqueued -> mutableState.value = ModelInstallState.Downloading(
                        0,
                        KokoroModelManifest.current.totalBytes,
                        "",
                    )
                    is ModelDownloadSnapshot.Downloading -> mutableState.value = ModelInstallState.Downloading(
                        snapshot.completedBytes,
                        snapshot.totalBytes,
                        snapshot.fileName,
                    )
                    ModelDownloadSnapshot.Verifying -> mutableState.value = ModelInstallState.Verifying
                    ModelDownloadSnapshot.Completed -> refresh()
                    is ModelDownloadSnapshot.Failed -> mutableState.value = ModelInstallState.Failed(snapshot.message)
                }
            }
        }
    }

    override fun refresh() {
        mutableState.value = ModelInstallState.Checking
        scope.launch {
            val ready = withContext(verificationDispatcher) { installer.readyDirectoryOrNull() }
            mutableState.value = ready?.let(ModelInstallState::Ready) ?: ModelInstallState.Missing
        }
    }

    override fun startDownload() {
        when (mutableState.value) {
            ModelInstallState.Missing, is ModelInstallState.Failed -> Unit
            else -> return
        }
        mutableState.value = ModelInstallState.Downloading(0, KokoroModelManifest.current.totalBytes, "")
        scope.launch {
            try {
                withContext(verificationDispatcher) { installer.ensureSufficientStorage() }
                scheduler.enqueue()
            } catch (failure: InsufficientStorageException) {
                mutableState.value = ModelInstallState.Failed(failure.message.orEmpty())
            }
        }
    }

    override fun readyDirectory(): File? = (mutableState.value as? ModelInstallState.Ready)?.directory
}
