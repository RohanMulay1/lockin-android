package com.rohan.lockin.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.ui.theme.Black0
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Faint
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Mono
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg
import com.rohan.lockin.ui.theme.Surface1
import com.rohan.lockin.ui.theme.Surface2

private val PanelShape = RoundedCornerShape(20.dp)
private val ControlShape = RoundedCornerShape(16.dp)

/** Hairline panel. [accent] tints the border; [glow] adds a soft radial light from the top-left. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    glow: Boolean = false,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glowColor = accent ?: Emerald
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Surface1)
            .then(if (glow) Modifier.radialGlow(glowColor) else Modifier)
            .border(1.dp, accent?.copy(alpha = 0.35f) ?: Line, PanelShape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Titled panel; kept as the common building block for screens. */
@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    accent: Color = Line,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tint = accent.takeIf { it != Line }
    Panel(modifier = modifier, accent = tint) {
        title?.let { Eyebrow(it, color = tint ?: Muted) }
        content()
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun ScreenHeader(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Eyebrow("LockIn", color = Emerald)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = OnBg)
        }
        trailing()
    }
}

/** Monospace numerals with tabular figures so digits don't jitter while counting. */
@Composable
fun MonoNumber(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OnBg,
    size: TextUnit = 28.sp,
    weight: FontWeight = FontWeight.SemiBold,
) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = Mono,
            fontWeight = weight,
            fontSize = size,
            letterSpacing = (-0.04f * size.value).sp,
            fontFeatureSettings = "tnum",
            color = color,
        ),
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = OnBg) {
    Panel(modifier = modifier, contentPadding = 16.dp) {
        Eyebrow(label)
        MonoNumber(value, color = color, size = 22.sp)
    }
}

@Composable
fun LiveDot(color: Color, pulse: Boolean = true, size: Dp = 8.dp) {
    val alpha = if (pulse) {
        val transition = rememberInfiniteTransition(label = "dot")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dotAlpha",
        )
        a
    } else {
        1f
    }
    Box(Modifier.size(size).alpha(alpha).clip(CircleShape).background(color))
}

@Composable
fun StatusChip(text: String, color: Color, live: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LiveDot(color, pulse = live, size = 7.dp)
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Emergency action: tinted crimson slab with a pulsing dot; loud without being a flat red block. */
@Composable
fun BigRedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(ControlShape)
            .background(Brush.verticalGradient(listOf(Crimson.copy(alpha = 0.24f), Crimson.copy(alpha = 0.10f))))
            .border(1.dp, Crimson.copy(alpha = 0.7f), ControlShape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveDot(Crimson)
        Text(
            text.uppercase(),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 1.8.sp),
            color = Crimson,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Emerald,
    contentColor: Color = Black0,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
            disabledContainerColor = Surface2,
            disabledContentColor = Faint,
        ),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = OnBg) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = ControlShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = OnBg)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Black0,
                checkedTrackColor = Emerald,
                checkedBorderColor = Emerald,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Line,
            ),
        )
    }
}

/** Large tappable checklist row with a round check, instead of a stock checkbox. */
@Composable
fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (checked) Emerald.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (checked) Emerald.copy(alpha = 0.35f) else Line, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Emerald else Color.Transparent)
                .border(1.5.dp, if (checked) Emerald else Faint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = Black0, modifier = Modifier.size(14.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (checked) OnBg else Muted)
    }
}

@Composable
fun MonoField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = OnBg),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald,
            unfocusedBorderColor = Line,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = Muted,
            cursorColor = Emerald,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

/** Thin animated progress rail. */
@Composable
fun ProgressRail(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(400), label = "rail")
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(CircleShape).background(color))
    }
}

@Composable
fun KeyValueRow(key: String, value: String, valueColor: Color = OnBg) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(key, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Muted)
        MonoNumber(value, color = valueColor, size = 14.sp, weight = FontWeight.Medium)
    }
}

fun Modifier.radialGlow(color: Color, strength: Float = 0.16f): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = strength), Color.Transparent),
            center = Offset(size.width * 0.15f, 0f),
            radius = size.maxDimension * 0.9f,
        ),
    )
}

/** Swallows every pointer event so nothing underneath a full-screen overlay is reachable. */
fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
