package com.isbkch.echolocal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.model.KokoroModelManifest
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.ui.components.EcholocalPrimaryButton
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.EcholocalSuccess
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun ModelSetupOverlay(modelState: ModelInstallState, onDownload: () -> Unit) {
    val colors = LocalEcholocalColors.current
    Box(Modifier.fillMaxSize().background(colors.paper), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(82.dp).background(colors.ink, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(width = 38.dp, height = 42.dp)) {
                    listOf(0.32f, 0.66f, 1f, 0.61f, 0.28f).forEachIndexed { index, fraction ->
                        val x = size.width * index / 4f
                        val half = size.height * fraction / 2f
                        drawLine(colors.paper, Offset(x, size.height / 2 - half), Offset(x, size.height / 2 + half), 3.4.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            Text(
                "Private speech,\non this device.",
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = colors.ink,
                modifier = Modifier.padding(top = 26.dp),
            )
            Text(
                "Download Kokoro once. After that, your writing and generated speech stay here—even in Airplane Mode.",
                textAlign = TextAlign.Center,
                color = colors.secondaryInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            Box(Modifier.padding(top = 28.dp), contentAlignment = Alignment.Center) {
                when (modelState) {
                    ModelInstallState.Checking -> CircularProgressIndicator()
                    ModelInstallState.Missing -> EcholocalPrimaryButton(onDownload) { Text("Download local model") }
                    is ModelInstallState.Downloading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val progress = (modelState.completedBytes.toFloat() / modelState.totalBytes.coerceAtLeast(1)).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                        Text(
                            "Downloading ${modelState.fileName.ifBlank { "local model" }}… ${(progress * 100).toInt()}%",
                            color = colors.secondaryInk,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    ModelInstallState.Verifying -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Verifying the local model…", modifier = Modifier.padding(top = 10.dp), fontSize = 12.sp)
                    }
                    is ModelInstallState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(modelState.message, color = EcholocalAccent, textAlign = TextAlign.Center, fontSize = 12.sp)
                        EcholocalPrimaryButton(onDownload, modifier = Modifier.padding(top = 12.dp)) { Text("Try download again") }
                    }
                    is ModelInstallState.Ready -> Text("Ready offline", color = EcholocalSuccess)
                }
            }
            Column(Modifier.padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About ${modelSizeMiB()} MiB", color = colors.secondaryInk, fontSize = 11.sp)
                Text("Five English voices", color = colors.secondaryInk, fontSize = 11.sp)
                Text("No network used for generation", color = colors.secondaryInk, fontSize = 11.sp)
            }
        }
    }
}

private fun modelSizeMiB(): Long {
    val mib = 1024L * 1024L
    return (KokoroModelManifest.current.totalBytes + mib - 1L) / mib
}
