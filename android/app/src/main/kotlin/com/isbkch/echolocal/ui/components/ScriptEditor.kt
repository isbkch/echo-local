package com.isbkch.echolocal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.EcholocalEditorStyle
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun ScriptEditor(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEcholocalColors.current
    Box(modifier.fillMaxSize().background(colors.paper), contentAlignment = Alignment.TopStart) {
        if (text.isEmpty()) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                Text(
                    "Paste something worth hearing.",
                    color = colors.faintInk,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 25.sp,
                )
                Text(
                    "An article, a draft, a page of notes—your text stays on this device.",
                    color = colors.faintInk,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            textStyle = EcholocalEditorStyle.copy(color = colors.ink),
            cursorBrush = SolidColor(EcholocalAccent),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .semantics { contentDescription = "Text to turn into speech" },
        )
    }
}
