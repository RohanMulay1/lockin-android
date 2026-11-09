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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.data.CsvExporter
import com.rohan.lockin.data.db.DailyLogEntity
import com.rohan.lockin.data.db.TradeEntity
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.signedRupees
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.ScreenHeader
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg
import com.rohan.lockin.ui.theme.Surface2
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")
private val timeFormat = DateTimeFormatter.ofPattern("d MMM · HH:mm")

@Composable
fun HistoryScreen(ui: UiState) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("History") {
                FilledTonalIconButton(
                    onClick = { CsvExporter.share(context, ui.logs, ui.trades) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Surface2, contentColor = OnBg),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Export CSV")
                }
            }
        }
        item { Eyebrow("Daily log") }
        if (ui.logs.isEmpty()) item { EmptyState("Days are archived here after midnight.") }
        items(ui.logs, key = { it.epochDay }) { DayRow(it) }
        item { Eyebrow("Recent trades", Modifier.padding(top = 12.dp)) }
        if (ui.trades.isEmpty()) {
            item { EmptyState("Closed trades appear here.") }
        } else {
            item { TradesPanel(ui.trades) }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Panel { Text(text, style = MaterialTheme.typography.bodyMedium, color = Muted) }
}

@Composable
private fun DayRow(log: DailyLogEntity) {
    Panel(accent = if (log.clean) Emerald else null, contentPadding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(LocalDate.ofEpochDay(log.epochDay).format(dayFormat), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = OnBg)
            MonoNumber(signedRupees(log.pnl), color = if (log.pnl < 0) Crimson else Emerald, size = 20.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("${log.trades} trades", Muted)
            if (log.violations > 0) StatusChip("${log.violations} breaks", Crimson)
            if (log.lockouts > 0) StatusChip("${log.lockouts} locks", Crimson)
            if (log.clean) StatusChip("Clean", Emerald)
        }
    }
}

@Composable
private fun TradesPanel(trades: List<TradeEntity>) {
    Panel(contentPadding = 16.dp) {
        trades.forEachIndexed { i, t ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            TradeRow(t)
        }
    }
}

@Composable
private fun TradeRow(t: TradeEntity) {
    val entry = Instant.ofEpochMilli(t.entryTs).atZone(ZoneId.systemDefault()).format(timeFormat)
    val overHeld = t.holdMinutes > TIME_STOP_MIN
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry, style = MaterialTheme.typography.bodyMedium, color = OnBg)
            Text("${t.holdMinutes} min${if (overHeld) " · over time stop" else ""}", style = MaterialTheme.typography.labelSmall, color = if (overHeld) Crimson else Muted)
        }
        MonoNumber(signedRupees(t.pnl), color = if (t.pnl < 0) Crimson else Emerald, size = 16.sp)
    }
}
