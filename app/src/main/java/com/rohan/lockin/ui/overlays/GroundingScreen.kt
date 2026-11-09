package com.rohan.lockin.ui.overlays

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
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
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

private data class GroundingStep(val title: String, val body: String, val durationMs: Long)

private val STEPS = listOf(
    GroundingStep("COLD WATER", "Drink a full glass of cold water slowly. Splash your face. Vagus nerve reset.", 60_000L),
    GroundingStep("15 PUSHUPS", "Drop and do 15 pushups. Burn the adrenaline the craving is riding on.", 60_000L),
    GroundingStep("PHYSIOLOGICAL SIGHS", "Two short inhales through the nose, one long exhale through the mouth. Repeat.", 60_000L),
)
private val TOTAL_MS = STEPS.sumOf { it.durationMs }

/** 3-minute craving protocol. Back and touches are blocked until the last step ends. */
@Composable
fun GroundingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onDone)
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        var lastStep = -1
        while (elapsedMs < TOTAL_MS) {
            elapsedMs = withFrameMillis { it } - start
            val step = stepIndex(elapsedMs)
            if (step != lastStep) {
                Haptics.heavy(context)
                lastStep = step
            }
        }
        finish()
    }

    val index = stepIndex(elapsedMs)
    val step = STEPS[index]
    val stepStart = STEPS.take(index).sumOf { it.durationMs }
    val stepRemaining = step.durationMs - (elapsedMs - stepStart)

    BackHandler(enabled = true) {}
    Box(Modifier.fillMaxSize().background(Black0).blockTouches(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("STEP ${index + 1} OF ${STEPS.size}", color = Muted, letterSpacing = 2.sp)
            Text(step.title, color = Emerald, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(step.body, color = OnBg, fontSize = 18.sp, textAlign = TextAlign.Center)
            MonoNumber(mmss(stepRemaining), color = OnBg, size = 48.sp)
            LinearProgressIndicator(
                progress = { (elapsedMs.toFloat() / TOTAL_MS).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Emerald,
                trackColor = Line,
            )
            Text("The craving peaks and passes in minutes. Ride it out.", color = Muted, textAlign = TextAlign.Center)
        }
    }
}

private fun stepIndex(elapsedMs: Long): Int {
    var acc = 0L
    STEPS.forEachIndexed { i, s ->
        acc += s.durationMs
        if (elapsedMs < acc) return i
    }
    return STEPS.lastIndex
}
