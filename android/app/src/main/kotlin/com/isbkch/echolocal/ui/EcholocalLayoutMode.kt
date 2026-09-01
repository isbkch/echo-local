package com.isbkch.echolocal.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class EcholocalLayoutMode { Compact, Expanded }

val LocalEcholocalLayoutMode = staticCompositionLocalOf { EcholocalLayoutMode.Compact }
