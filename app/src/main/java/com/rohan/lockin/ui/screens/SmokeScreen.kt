package com.rohan.lockin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.Milestone
import com.rohan.lockin.domain.SmokeMath
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.BigRedButton
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.GhostButton
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.ProgressRail
import com.rohan.lockin.ui.components.ScreenHeader
import com.rohan.lockin.ui.components.StatTile
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Faint
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg
import com.rohan.lockin.ui.theme.Surface1

@Composable
fun SmokeScreen(ui: UiState, vm: LockInViewModel) {
    var confirmSmoked by remember { mutableStateOf(false) }
    val quitTs = ui.app.quitTs
    val next = SmokeMath.MILESTONES.firstOrNull { !SmokeMath.isUnlocked(it, quitTs, ui.nowMs) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Smoke-free") {
                val count = ui.app.cigarettesToday
                StatusChip(if (count == 0) "0 today" else "$count today", if (count == 0) Emerald else Crimson, live = count == 0)
            }
        }
        item { StopwatchHero(quitTs, ui.nowMs, next) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val count = ui.app.cigarettesToday
                StatTile("Today", "$count", Modifier.weight(1f), if (count == 0) Emerald else Crimson)
                StatTile("Target", "0", Modifier.weight(1f))
            }
        }
        item { BigRedButton("Craving emergency · 3 min", onClick = vm::startGrounding) }
        item { GhostButton("I smoked one", onClick = { confirmSmoked = true }, modifier = Modifier.fillMaxWidth(), color = Muted) }
        item { Eyebrow("Healing timeline", Modifier.padding(top = 12.dp)) }
        itemsIndexed(SmokeMath.MILESTONES) { i, m ->
            TimelineRow(m, quitTs, ui.nowMs, isLast = i == SmokeMath.MILESTONES.lastIndex)
        }
    }
    if (confirmSmoked) {
        AlertDialog(
            onDismissRequest = { confirmSmoked = false },
            containerColor = Surface1,
            title = { Text("Log a cigarette?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("The timer restarts from now. One slip is data, not failure. Start again immediately.") },
            confirmButton = {
                TextButton(onClick = { vm.logCigarette(); confirmSmoked = false }) { Text("LOG IT", color = Crimson) }
            },
            dismissButton = { TextButton(onClick = { confirmSmoked = false }) { Text("CANCEL", color = Muted) } },
        )
    }
}

@Composable
private fun StopwatchHero(quitTs: Long, nowMs: Long, next: Milestone?) {
    val e = SmokeMath.elapsed(quitTs, nowMs)
    Panel(accent = Emerald, glow = true) {
        Eyebrow("Since last cigarette", color = Emerald)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimeUnit(e.days, "days")
            TimeUnit(e.hours, "hrs")
            TimeUnit(e.minutes, "min")
            TimeUnit(e.seconds, "sec")
        }
        next?.let { m ->
            val p = SmokeMath.progress(m, quitTs, nowMs)
            ProgressRail(p, Emerald)
            Row(Modifier.fillMaxWidth()) {
                Text("Next · ${m.title}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Muted)
                MonoNumber("${(p * 100).toInt()}%", size = 13.sp, color = Emerald)
            }
        }
    }
}

@Composable
private fun TimeUnit(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MonoNumber("%02d".format(value), color = OnBg, size = 44.sp)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun TimelineRow(m: Milestone, quitTs: Long, nowMs: Long, isLast: Boolean) {
    val unlocked = SmokeMath.isUnlocked(m, quitTs, nowMs)
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 6.dp).size(12.dp).clip(CircleShape).background(if (unlocked) Emerald else Faint))
            if (!isLast) Box(Modifier.width(1.dp).height(64.dp).background(Line))
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, bottom = 8.dp)) {
            Text(m.title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) Emerald else OnBg)
            Text(m.body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
