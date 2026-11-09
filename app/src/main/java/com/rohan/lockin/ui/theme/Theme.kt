package com.rohan.lockin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val Black0 = Color(0xFF0A0A0A)
val Surface1 = Color(0xFF141414)
val Surface2 = Color(0xFF1C1C1C)
val Line = Color(0xFF2A2A2A)
val Emerald = Color(0xFF10B981)
val Crimson = Color(0xFFEF4444)
val Amber = Color(0xFFF59E0B)
val OnBg = Color(0xFFEDEDED)
val Muted = Color(0xFF8A8A8A)
val LockRed = Color(0xFF1A0505)

val Mono = FontFamily.Monospace

private val AmoledScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Black0,
    secondary = Emerald,
    onSecondary = Black0,
    background = Black0,
    onBackground = OnBg,
    surface = Surface1,
    onSurface = OnBg,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainer = Black0,
    error = Crimson,
    onError = Color.White,
    outline = Line,
)

@Composable
fun LockInTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AmoledScheme, typography = Typography(), content = content)
}
