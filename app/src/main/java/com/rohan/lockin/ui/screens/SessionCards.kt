package com.rohan.lockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.SectionCard
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
fun ReadinessCard(ui: UiState, onSubmit: (Boolean, Boolean, Boolean, Boolean) -> Unit) {
    if (ui.readinessDone) {
        val cleared = ui.app.readinessCleared
        SectionCard("Morning readiness", accent = if (cleared) Emerald else Crimson) {
            Text(
                if (cleared) "CLEARED TO TRADE" else "NO-TRADE DAY",
                color = if (cleared) Emerald else Crimson,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
            Text(
                if (cleared) "Rules on. 1 lot. SL order at entry. 30-min time stop." else "Bad sleep, travel, or tilt = no trades. The market will be here tomorrow.",
                color = Muted,
                fontSize = 13.sp,
            )
        }
        return
    }
    var slept by rememberSaveable { mutableStateOf(false) }
    var notTravelling by rememberSaveable { mutableStateOf(false) }
    var noBigLoss by rememberSaveable { mutableStateOf(false) }
    var calm by rememberSaveable { mutableStateOf(false) }
    SectionCard("Morning readiness (before 09:15)", accent = Amber) {
        ToggleRow("Slept 6+ hours", slept) { slept = it }
        ToggleRow("Not travelling today", notTravelling) { notTravelling = it }
        ToggleRow("No big loss yesterday", noBigLoss) { noBigLoss = it }
        ToggleRow("Calm, not chasing a number", calm) { calm = it }
        Button(onClick = { onSubmit(slept, notTravelling, noBigLoss, calm) }, modifier = Modifier.fillMaxWidth()) {
            Text("LOCK IN READINESS")
        }
    }
}

@Composable
fun PnlCard(ui: UiState, onSet: (String) -> Unit) {
    val s = ui.app
    var input by rememberSaveable { mutableStateOf("") }
    val pnlColor = if (s.sessionPnl < 0) Crimson else if (s.sessionPnl > 0) Emerald else OnBg
    val drawdownUsed = ((-s.sessionPnl).coerceIn(0, ONE_R)).toFloat() / ONE_R
    SectionCard("Session P&L", accent = pnlColor) {
        MonoNumber(signedRupees(s.sessionPnl), color = pnlColor, size = 40.sp)
        LinearProgressIndicator(
            progress = { drawdownUsed },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Crimson,
            trackColor = Line,
        )
        Text("Room to -1R: ${rupees((ONE_R + s.sessionPnl).coerceAtLeast(0))}", color = Muted, fontSize = 12.sp)
        RiskEngine.giveBackFloor(s.peakPnl)?.let { floor ->
            Text("Peak ${rupees(s.peakPnl)} · give-back lock at ${rupees(floor)}", color = Amber, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() || it == '-' } },
                label = { Text("Net P&L now (₹, e.g. -1200)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onSet(input); input = "" }) { Text("SET") }
        }
    }
}

@Composable
fun CooldownCard(ui: UiState) {
    SectionCard(title = null, accent = Crimson) {
        Text("COOLDOWN — 2 LOSSES IN A ROW", color = Crimson, fontWeight = FontWeight.Black)
        MonoNumber(mmss(ui.cooldownRemainingMs), color = Crimson)
        Text("Stand up. Water. No chart until the timer ends.", color = OnBg, fontSize = 13.sp)
    }
}

@Composable
fun PreflightCard(ui: UiState, onToggle: (String) -> Unit, onEnter: () -> Unit) {
    val block = ui.blockReason
    SectionCard("Pre-flight gate", accent = if (block == null) Emerald else Line) {
        PREFLIGHT_CHECKLIST.forEach { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.id in ui.app.checklist, onCheckedChange = { onToggle(item.id) })
                Text(item.label, color = OnBg, fontSize = 14.sp)
            }
        }
        Button(
            onClick = onEnter,
            enabled = block == null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Black0),
        ) {
            Text("ENTER TRADE (1 LOT)", fontWeight = FontWeight.Black)
        }
        block?.let { Text(it, color = Crimson, fontSize = 13.sp) }
    }
}

@Composable
fun ActiveTradeCard(ui: UiState, onClose: (String) -> Unit) {
    var result by rememberSaveable { mutableStateOf("") }
    val elapsed = ui.tradeElapsedMs
    val color = when {
        elapsed >= TIME_STOP_MIN * MINUTE_MS -> Crimson
        elapsed >= TIME_STOP_WARN_MIN * MINUTE_MS -> Amber
        else -> Emerald
    }
    SectionCard("Trade open", accent = color) {
        MonoNumber(mmss(elapsed), color = color, size = 44.sp)
        val remaining = TIME_STOP_MIN * MINUTE_MS - elapsed
        Text(
            if (remaining > 0) "Time stop in ${mmss(remaining)} · SL order placed?" else "TIME STOP HIT — EXIT NOW",
            color = color,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = result,
            onValueChange = { v -> result = v.filter { it.isDigit() || it == '-' } },
            label = { Text("Result in ₹ (e.g. 2015 or -748)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onClose(result); result = "" },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnBg, contentColor = Black0),
        ) {
            Text("CLOSE TRADE & LOG", fontWeight = FontWeight.Black)
        }
    }
}
