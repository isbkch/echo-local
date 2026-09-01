package com.isbkch.echolocal.ui.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val documents = remember {
        listOf(
            "Notices" to "licenses/third-party-notices.txt",
            "Apache License 2.0" to "licenses/apache-2.0.txt",
            "ONNX Runtime MIT License" to "licenses/onnxruntime-mit.txt",
            "LiteRT and native notices" to "licenses/litert-third-party-notices.txt",
        ).map { (title, asset) ->
            title to context.assets.open(asset).bufferedReader().use { it.readText() }
        }
    }
    val colors = LocalEcholocalColors.current
    Column(
        Modifier.fillMaxSize().background(colors.canvas).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().background(colors.paper).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Open-source licenses", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.line)
        LazyColumn(Modifier.fillMaxSize()) {
            items(documents) { (title, body) ->
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
                )
                Text(
                    body,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.secondaryInk,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}
