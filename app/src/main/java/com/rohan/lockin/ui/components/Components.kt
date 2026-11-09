package com.rohan.lockin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Mono
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg
import com.rohan.lockin.ui.theme.Surface1

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    accent: Color = Line,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            title?.let {
                Text(it.uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            }
            content()
        }
    }
}

@Composable
fun MonoNumber(text: String, modifier: Modifier = Modifier, color: Color = OnBg, size: TextUnit = 28.sp) {
    Text(text, modifier = modifier, color = color, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = size)
}

@Composable
fun BigRedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Color.White),
    ) {
        Text(text, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = OnBg)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun StatCell(label: String, value: String, modifier: Modifier = Modifier, color: Color = OnBg) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 11.sp)
        MonoNumber(value, color = color, size = 18.sp)
    }
}

/** Swallows every pointer event so nothing underneath a full-screen overlay is reachable. */
fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
