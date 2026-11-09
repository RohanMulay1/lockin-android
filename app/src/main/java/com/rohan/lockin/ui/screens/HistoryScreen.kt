package com.rohan.lockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.data.CsvExporter
import com.rohan.lockin.data.db.DailyLogEntity
import com.rohan.lockin.data.db.TradeEntity
import com.rohan.lockin.domain.signedRupees
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.SectionCard
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm")

@Composable
fun HistoryScreen(ui: UiState) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = { CsvExporter.share(context, ui.logs, ui.trades) }, modifier = Modifier.fillMaxWidth()) {
                Text("EXPORT CSV FOR WEEKLY REVIEW")
            }
        }
        item { Header("Daily log") }
        if (ui.logs.isEmpty()) item { Text("Days are archived here after midnight.", color = Muted) }
        items(ui.logs, key = { it.epochDay }) { DayRow(it) }
        item { Header("Recent trades") }
        if (ui.trades.isEmpty()) item { Text("Closed trades appear here.", color = Muted) }
        items(ui.trades, key = { it.id }) { TradeRow(it) }
    }
}

@Composable
private fun Header(text: String) {
    Text(text.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp)
}

@Composable
private fun DayRow(log: DailyLogEntity) {
    val color = if (log.pnl < 0) Crimson else Emerald
    SectionCard(title = null, accent = if (log.clean) Emerald else Line) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(LocalDate.ofEpochDay(log.epochDay).format(dayFormat), color = OnBg, fontWeight = FontWeight.Bold)
            MonoNumber(signedRupees(log.pnl), color = color, size = 18.sp)
        }
        Text(
            "${log.trades} trades · ${log.violations} rule breaks · ${log.lockouts} lockouts · ${log.cigarettes} cigs" +
                if (log.clean) " · CLEAN" else "",
            color = Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TradeRow(t: TradeEntity) {
    val entry = Instant.ofEpochMilli(t.entryTs).atZone(ZoneId.systemDefault()).format(timeFormat)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$entry · ${t.holdMinutes}m", color = if (t.holdMinutes > 30) Crimson else Muted, fontSize = 13.sp)
        MonoNumber(signedRupees(t.pnl), color = if (t.pnl < 0) Crimson else Emerald, size = 14.sp)
    }
}
