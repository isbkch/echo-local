package com.isbkch.echolocal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.EcholocalLocalWordmarkStyle
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun EcholocalWordmark(modifier: Modifier = Modifier) {
    val colors = LocalEcholocalColors.current
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Echo Local" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(width = 18.dp, height = 15.dp)) {
            listOf(0.35f, 0.78f, 1f, 0.66f, 0.3f).forEachIndexed { index, fraction ->
                val x = size.width * index / 4f
                val half = size.height * fraction / 2f
                drawLine(
                    EcholocalAccent,
                    Offset(x, size.height / 2f - half),
                    Offset(x, size.height / 2f + half),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Text("Echo", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Local", color = colors.ink, style = EcholocalLocalWordmarkStyle)
    }
}
