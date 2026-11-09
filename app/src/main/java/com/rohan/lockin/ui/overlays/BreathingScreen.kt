package com.rohan.lockin.ui.overlays

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.mmss
import com.rohan.lockin.haptics.Haptics
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.blockTouches
import com.rohan.lockin.ui.theme.Black0
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

private const val PHASE_MS = 4_000L
private const val TOTAL_MS = 180_000L
private const val MIN_SCALE = 0.4f
private val PHASES = listOf("INHALE", "HOLD", "EXHALE", "HOLD")

/** Box breathing 4-4-4-4 for 180 s. Unskippable: back and touches are blocked until it finishes. */
@Composable
fun BreathingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onDone)
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        var lastPhase = -1
        while (elapsedMs < TOTAL_MS) {
            elapsedMs = withFrameMillis { it } - start
            val phase = ((elapsedMs / PHASE_MS) % PHASES.size).toInt()
            if (phase != lastPhase) {
                Haptics.pulse(context)
                lastPhase = phase
            }
        }
        finish()
    }

    val phase = ((elapsedMs / PHASE_MS) % PHASES.size).toInt()
    val t = (elapsedMs % PHASE_MS).toFloat() / PHASE_MS
    val scale = when (phase) {
        0 -> MIN_SCALE + (1f - MIN_SCALE) * t
        1 -> 1f
        2 -> 1f - (1f - MIN_SCALE) * t
        else -> MIN_SCALE
    }

    BackHandler(enabled = true) {}
    Box(Modifier.fillMaxSize().background(Black0).blockTouches(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(PHASES[phase], color = Emerald, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Canvas(Modifier.size(260.dp)) {
                val radius = size.minDimension / 2f * scale
                drawCircle(color = Emerald.copy(alpha = 0.18f), radius = radius)
                drawCircle(color = Emerald, radius = radius, style = Stroke(width = 6.dp.toPx()))
            }
            MonoNumber(mmss(TOTAL_MS - elapsedMs), color = OnBg, size = 32.sp)
            Text(
                "Put phone face down. Drink cold water. You are on emotional tilt.",
                color = Muted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
