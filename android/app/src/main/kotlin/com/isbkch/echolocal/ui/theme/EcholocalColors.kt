package com.isbkch.echolocal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val EcholocalAccent = Color(0xFFCC4F30)
val EcholocalAccentSoftLight = Color(0x1FCC4F30)
val EcholocalAccentSoftDark = Color(0x33CC4F30)
val EcholocalSuccess = Color(0xFF2E8563)

data class EcholocalSemanticColors(
    val canvas: Color,
    val paper: Color,
    val raised: Color,
    val ink: Color,
    val secondaryInk: Color,
    val faintInk: Color,
    val line: Color,
    val accentSoft: Color,
)

internal val LightSemanticColors = EcholocalSemanticColors(
    canvas = Color(0xFFF2F2F7),
    paper = Color(0xFFFFFFFF),
    raised = Color(0xFFF2F2F7),
    ink = Color(0xFF1C1C1E),
    secondaryInk = Color(0xFF636366),
    faintInk = Color(0xFFAEAEB2),
    line = Color(0xFFC6C6C8),
    accentSoft = EcholocalAccentSoftLight,
)

internal val DarkSemanticColors = EcholocalSemanticColors(
    canvas = Color(0xFF000000),
    paper = Color(0xFF1C1C1E),
    raised = Color(0xFF2C2C2E),
    ink = Color(0xFFF2F2F7),
    secondaryInk = Color(0xFFAEAEB2),
    faintInk = Color(0xFF636366),
    line = Color(0xFF48484A),
    accentSoft = EcholocalAccentSoftDark,
)

val LocalEcholocalColors = staticCompositionLocalOf { LightSemanticColors }
