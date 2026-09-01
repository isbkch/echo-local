package com.isbkch.echolocal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun EcholocalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = EcholocalAccent,
            onPrimary = Color.White,
            background = semantic.canvas,
            onBackground = semantic.ink,
            surface = semantic.paper,
            onSurface = semantic.ink,
            surfaceVariant = semantic.raised,
            onSurfaceVariant = semantic.secondaryInk,
            outline = semantic.line,
        )
    } else {
        lightColorScheme(
            primary = EcholocalAccent,
            onPrimary = Color.White,
            background = semantic.canvas,
            onBackground = semantic.ink,
            surface = semantic.paper,
            onSurface = semantic.ink,
            surfaceVariant = semantic.raised,
            onSurfaceVariant = semantic.secondaryInk,
            outline = semantic.line,
        )
    }
    CompositionLocalProvider(LocalEcholocalColors provides semantic) {
        MaterialTheme(colorScheme = scheme, typography = EcholocalTypography, content = content)
    }
}
