package com.isbkch.echolocal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isbkch.echolocal.app.EcholocalEvent
import com.isbkch.echolocal.app.EcholocalViewModel
import com.isbkch.echolocal.ui.EcholocalActions
import com.isbkch.echolocal.ui.EcholocalApp
import com.isbkch.echolocal.ui.EcholocalLayoutMode
import com.isbkch.echolocal.ui.LocalEcholocalLayoutMode
import com.isbkch.echolocal.ui.theme.EcholocalTheme

class MainActivity : ComponentActivity() {
    private val viewModel: EcholocalViewModel by viewModels {
        (application as EcholocalApplication).viewModelFactory()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcholocalTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val layoutMode = if (calculateWindowSizeClass(this).widthSizeClass == WindowWidthSizeClass.Expanded) {
                    EcholocalLayoutMode.Expanded
                } else {
                    EcholocalLayoutMode.Compact
                }
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is EcholocalEvent.Share -> startActivity(Intent.createChooser(event.intent, "Share WAV"))
                        }
                    }
                }
                CompositionLocalProvider(LocalEcholocalLayoutMode provides layoutMode) {
                    EcholocalApp(
                        state,
                        EcholocalActions(
                            onTextChanged = viewModel::onTextChanged,
                            onPaste = viewModel::onPaste,
                            onClear = viewModel::onClear,
                            onSelectVoice = viewModel::onSelectVoice,
                            onParagraphPauseChanged = viewModel::onParagraphPauseChanged,
                            onNormalizeChanged = viewModel::onNormalizeChanged,
                            onTrimChanged = viewModel::onTrimChanged,
                            onAutoplayChanged = viewModel::onAutoplayChanged,
                            onDownloadModel = viewModel::onDownloadModel,
                            onGenerate = viewModel::onGenerate,
                            onCancelGeneration = viewModel::onCancelGeneration,
                            onPlayPause = viewModel::onPlayPause,
                            onSeek = viewModel::onSeek,
                            onShare = viewModel::onShare,
                            dismissError = viewModel::dismissError,
                        ),
                    )
                }
            }
        }
    }
}
