package com.rohan.lockin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.ONE_R
import com.rohan.lockin.domain.PREFLIGHT_CHECKLIST
import com.rohan.lockin.domain.RiskEngine
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.TIME_STOP_WARN_MIN
import com.rohan.lockin.domain.mmss
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.domain.signedRupees
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.CheckRow
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.MonoField
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.PrimaryButton
import com.rohan.lockin.ui.components.ProgressRail
import com.rohan.lockin.ui.components.ToggleRow
import com.rohan.lockin.ui.theme.Amber
import com.rohan.lockin.ui.theme.Black0
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

private const val MINUTE_MS = 60_000L

@Composable
fun ReadinessCard(onSubmit: (Boolean, Boolean, Boolean, Boolean) -> Unit) {
    var slept by rememberSaveable { mutableStateOf(false) }
    var notTravelling by rememberSaveable { mutableStateOf(false) }
    var noBigLoss by rememberSaveable { mutableStateOf(false) }
    var calm by rememberSaveable { mutableStateOf(false) }
    Panel(accent = Amber, glow = true) {
        Eyebrow("Morning check-in · before 09:15", color = Amber)
        Text("Are you fit to trade today?", style = MaterialTheme.typography.titleLarge, color = OnBg)
        ToggleRow("Slept 6+ hours", slept) { slept = it }
        ToggleRow("Not travelling today", notTravelling) { notTravelling = it }
        ToggleRow("No big loss yesterday", noBigLoss) { noBigLoss = it }
        ToggleRow("Calm, not chasing a number", calm) { calm = it }
        PrimaryButton(
            "Lock in check-in",
            onClick = { onSubmit(slept, notTravelling, noBigLoss, calm) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PnlHero(ui: UiState, onSet: (String) -> Unit) {
    val s = ui.app
    var input by rememberSaveable { mutableStateOf("") }
    val pnlColor = when {
        s.sessionPnl < 0 -> Crimson
        s.sessionPnl > 0 -> Emerald
        else -> OnBg
    }
    val drawdownUsed = (-s.sessionPnl).coerceIn(0, ONE_R).toFloat() / ONE_R
    Panel(accent = if (s.sessionPnl == 0) null else pnlColor, glow = true) {
        Eyebrow("Session P&L")
        MonoNumber(signedRupees(s.sessionPnl), color = pnlColor, size = 52.sp)
        ProgressRail(drawdownUsed, Crimson)
        Row(Modifier.fillMaxWidth()) {
            Text("Room to −1R", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Muted)
            MonoNumber(rupees((ONE_R + s.sessionPnl).coerceAtLeast(0)), size = 14.sp, color = OnBg)
        }
        RiskEngine.giveBackFloor(s.peakPnl)?.let { floor ->
            Row(Modifier.fillMaxWidth()) {
                Text("Peak ${rupees(s.peakPnl)} · give-back lock at", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Amber)
                MonoNumber(rupees(floor), size = 14.sp, color = Amber)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() || it == '-' } },
                label = "Net P&L now (₹)",
                modifier = Modifier.weight(1f),
            )
            PrimaryButton("Set", onClick = { onSet(input); input = "" }, modifier = Modifier.width(84.dp), container = OnBg)
        }
    }
}

@Composable
fun CooldownCard(ui: UiState) {
    Panel(accent = Crimson, glow = true) {
        Eyebrow("Cooldown · 2 losses in a row", color = Crimson)
        MonoNumber(mmss(ui.cooldownRemainingMs), color = Crimson, size = 40.sp)
        Text("Stand up. Water. No chart until the timer ends.", style = MaterialTheme.typography.bodyMedium, color = OnBg)
    }
}

@Composable
fun PreflightCard(ui: UiState, onToggle: (String) -> Unit, onEnter: () -> Unit) {
    val block = ui.blockReason
    val checked = PREFLIGHT_CHECKLIST.count { it.id in ui.app.checklist }
    Panel(accent = if (block == null) Emerald else null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Pre-flight gate", Modifier.weight(1f))
            MonoNumber("$checked/${PREFLIGHT_CHECKLIST.size}", size = 14.sp, color = if (checked == PREFLIGHT_CHECKLIST.size) Emerald else Muted)
        }
        PREFLIGHT_CHECKLIST.forEach { item ->
            CheckRow(item.label, checked = item.id in ui.app.checklist) { onToggle(item.id) }
        }
        PrimaryButton("Enter trade · 1 lot", onClick = onEnter, enabled = block == null, modifier = Modifier.fillMaxWidth())
        block?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Crimson) }
    }
}

@Composable
fun ActiveTradeCard(ui: UiState, onClose: (String) -> Unit) {
    var result by rememberSaveable { mutableStateOf("") }
    val elapsed = ui.tradeElapsedMs
    val limitMs = TIME_STOP_MIN * MINUTE_MS
    val color = when {
        elapsed >= limitMs -> Crimson
        elapsed >= TIME_STOP_WARN_MIN * MINUTE_MS -> Amber
        else -> Emerald
    }
    val remaining = limitMs - elapsed
    Panel(accent = color, glow = true) {
        Eyebrow("Trade open · 30-min time stop", color = color)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimeStopRing(progress = elapsed.toFloat() / limitMs, color = color)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MonoNumber(mmss(elapsed), color = OnBg, size = 40.sp)
                Text(
                    if (remaining > 0) "${mmss(remaining)} left" else "EXIT NOW",
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                )
            }
        }
        Text("SL order placed? No adding. No averaging down.", style = MaterialTheme.typography.bodySmall, color = Muted)
        MonoField(
            value = result,
            onValueChange = { v -> result = v.filter { it.isDigit() || it == '-' } },
            label = "Result in ₹ (e.g. 2015 or -748)",
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            "Close trade & log",
            onClick = { onClose(result); result = "" },
            modifier = Modifier.fillMaxWidth(),
            container = OnBg,
            contentColor = Black0,
        )
    }
}

@Composable
private fun TimeStopRing(progress: Float, color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(196.dp)) {
        val stroke = 10.dp.toPx()
        drawCircle(color = Line, style = Stroke(width = stroke))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
