package com.rohan.lockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.Milestone
import com.rohan.lockin.domain.SmokeMath
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.BigRedButton
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.SectionCard
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

@Composable
fun SmokeScreen(ui: UiState, vm: LockInViewModel) {
    var confirmSmoked by remember { mutableStateOf(false) }
    val quitTs = ui.app.quitTs
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { StopwatchCard(quitTs, ui.nowMs) }
        item {
            SectionCard("Today") {
                val count = ui.app.cigarettesToday
                MonoNumber("$count / target 0", color = if (count == 0) Emerald else Crimson)
            }
        }
        item { BigRedButton("CRAVING EMERGENCY (3 MIN)", onClick = vm::startGrounding) }
        item {
            OutlinedButton(onClick = { confirmSmoked = true }, modifier = Modifier.fillMaxWidth()) {
                Text("I smoked one (resets timer)", color = Muted)
            }
        }
        item { Text("HEALING MILESTONES", color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp) }
        items(SmokeMath.MILESTONES) { m -> MilestoneCard(m, quitTs, ui.nowMs) }
    }
    if (confirmSmoked) {
        AlertDialog(
            onDismissRequest = { confirmSmoked = false },
            title = { Text("Log a cigarette?") },
            text = { Text("The timer restarts from now. One slip is data, not failure. Start again immediately.") },
            confirmButton = {
                TextButton(onClick = { vm.logCigarette(); confirmSmoked = false }) { Text("Log it", color = Crimson) }
            },
            dismissButton = { TextButton(onClick = { confirmSmoked = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StopwatchCard(quitTs: Long, nowMs: Long) {
    val e = SmokeMath.elapsed(quitTs, nowMs)
    SectionCard("Smoke-free for", accent = Emerald) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimeUnit(e.days, "DAYS")
            TimeUnit(e.hours, "HRS")
            TimeUnit(e.minutes, "MIN")
            TimeUnit(e.seconds, "SEC")
        }
    }
}

@Composable
private fun TimeUnit(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MonoNumber("%02d".format(value), color = Emerald, size = 36.sp)
        Text(label, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MilestoneCard(m: Milestone, quitTs: Long, nowMs: Long) {
    val unlocked = SmokeMath.isUnlocked(m, quitTs, nowMs)
    SectionCard(title = null, accent = if (unlocked) Emerald else Line) {
        Text(m.title, color = if (unlocked) Emerald else OnBg, fontWeight = FontWeight.Bold)
        Text(m.body, color = Muted, fontSize = 13.sp)
        LinearProgressIndicator(
            progress = { SmokeMath.progress(m, quitTs, nowMs) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Emerald,
            trackColor = Line,
        )
    }
}
