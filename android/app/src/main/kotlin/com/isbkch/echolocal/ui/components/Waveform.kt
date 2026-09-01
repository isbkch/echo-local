package com.isbkch.echolocal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun EcholocalWaveform(
    samples: FloatArray,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEcholocalColors.current
    val envelope = remember(samples) {
        if (samples.isNotEmpty()) samples else FloatArray(120) { index ->
            val position = index.toDouble() / 119.0
            max(0.04, sin(position * PI) * (0.38 + 0.14 * sin(position * PI * 7))).toFloat()
        }
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        if (envelope.size < 2) return@Canvas
        val peak = max(envelope.maxOrNull() ?: 1f, 0.001f)
        val centerY = size.height / 2f
        val amplitude = size.height * 0.42f
        val xStep = size.width / (envelope.size - 1)
        val path = Path().apply {
            moveTo(0f, centerY)
            envelope.forEachIndexed { index, value ->
                val shaped = max(0.035f, (value / peak).coerceAtLeast(0f).pow(0.72f))
                lineTo(index * xStep, centerY - shaped * amplitude)
            }
            for (index in envelope.indices.reversed()) {
                val shaped = max(0.035f, (envelope[index] / peak).coerceAtLeast(0f).pow(0.72f))
                lineTo(index * xStep, centerY + shaped * amplitude)
            }
            close()
        }
        drawPath(path, colors.faintInk.copy(alpha = 0.24f))
        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
            drawPath(path, EcholocalAccent)
        }
        drawLine(colors.line.copy(alpha = 0.5f), Offset(0f, centerY), Offset(size.width, centerY))
    }
}
