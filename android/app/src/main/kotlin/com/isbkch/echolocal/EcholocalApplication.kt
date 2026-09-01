package com.isbkch.echolocal

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.isbkch.echolocal.app.EcholocalViewModel
import com.isbkch.echolocal.audio.GeneratedAudioStore
import com.isbkch.echolocal.audio.Media3AudioPlayer
import com.isbkch.echolocal.data.DataStoreSettingsRepository
import com.isbkch.echolocal.model.KokoroModelDownloadWorker
import com.isbkch.echolocal.model.KokoroModelRepository
import com.isbkch.echolocal.model.ModelInstaller
import com.isbkch.echolocal.model.OkHttpAssetFetcher
import com.isbkch.echolocal.model.WorkManagerModelDownloadScheduler
import com.isbkch.echolocal.sharing.ShareIntentFactory
import com.isbkch.echolocal.tts.SoniqoTtsEngineFactory
import com.isbkch.echolocal.tts.SpeechGenerator
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

private val Context.echolocalSettings by preferencesDataStore(name = "echolocal_settings")

class EcholocalApplication : Application() {
    lateinit var container: EcholocalContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = EcholocalContainer(this)
    }

    fun viewModelFactory(): ViewModelProvider.Factory = EcholocalViewModelFactory(container)
}

class EcholocalContainer(private val application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient = OkHttpClient()
    private val modelInstaller = ModelInstaller(
        root = File(application.filesDir, KokoroModelDownloadWorker.MODEL_ROOT),
        fetcher = OkHttpAssetFetcher(httpClient),
    )

    val settingsRepository = DataStoreSettingsRepository(application.echolocalSettings)
    val modelRepository by lazy(LazyThreadSafetyMode.NONE) {
        KokoroModelRepository(
            modelInstaller,
            WorkManagerModelDownloadScheduler(application),
            applicationScope,
        )
    }
    val speechGenerator = SpeechGenerator(
        SoniqoTtsEngineFactory(),
        GeneratedAudioStore(File(application.cacheDir, "generated")),
    )

    fun createViewModel(savedStateHandle: SavedStateHandle): EcholocalViewModel {
        val audioPlayer = Media3AudioPlayer(application)
        return EcholocalViewModel(
            savedStateHandle = savedStateHandle,
            settingsRepository = settingsRepository,
            modelRepository = modelRepository,
            generator = speechGenerator,
            audioPlayer = audioPlayer,
            shareIntent = { wavFile -> ShareIntentFactory.create(application, wavFile) },
        )
    }
}

private class EcholocalViewModelFactory(
    private val container: EcholocalContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(EcholocalViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        return container.createViewModel(extras.createSavedStateHandle()) as T
    }
}
