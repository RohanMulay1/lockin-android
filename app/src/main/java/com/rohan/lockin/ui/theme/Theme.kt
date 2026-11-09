package com.rohan.lockin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rohan.lockin.R

// Surfaces: true-black canvas, near-black panels, hairline borders.
val Black0 = Color(0xFF0A0A0A)
val Surface1 = Color(0xFF111111)
val Surface2 = Color(0xFF181818)
val Line = Color(0xFF222222)

// Semantic color only: emerald = safe, crimson = risk, amber = caution.
val Emerald = Color(0xFF10B981)
val Crimson = Color(0xFFEF4444)
val Amber = Color(0xFFF59E0B)

val OnBg = Color(0xFFFAFAFA)
val Muted = Color(0xFF8F8F8F)
val Faint = Color(0xFF4D4D4D)
val LockRed = Color(0xFF0D0505)

val Sans = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

val Mono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_semibold, FontWeight.SemiBold),
    Font(R.font.geist_mono_bold, FontWeight.Bold),
)

private val LockInTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, letterSpacing = (-2).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = (-0.1).sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.2.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.6.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.4.sp),
)

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
    surfaceContainerHigh = Surface1,
    surfaceContainerHighest = Surface2,
    error = Crimson,
    onError = Color.White,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun LockInTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AmoledScheme, typography = LockInTypography, content = content)
}
