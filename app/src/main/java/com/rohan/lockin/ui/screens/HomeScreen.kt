package com.rohan.lockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.MARGIN_CAP
import com.rohan.lockin.domain.ONE_R
import com.rohan.lockin.domain.RiskEngine
import com.rohan.lockin.domain.TRADE_CAP
import com.rohan.lockin.domain.mmss
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.UpdateState
import com.rohan.lockin.ui.components.BigRedButton
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.ScreenHeader
import com.rohan.lockin.ui.components.StatTile
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.theme.Amber
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

@Composable
fun HomeScreen(ui: UiState, vm: LockInViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Trade desk") { SessionChip(ui) } }
        if (ui.update is UpdateState.Available || ui.update is UpdateState.Downloading) {
            item { UpdateBanner(ui.update, vm::installUpdate) }
        }
        if (!ui.readinessDone) item { ReadinessCard(vm::submitReadiness) }
        item { PnlHero(ui, vm::setPnl) }
        if (ui.inNoTradeWindow) {
            item { AlertBanner("No new trades · 10:30–11:15", "Ends in ${mmss(ui.noTradeWindowRemainingMs)}. Manage open trades only.", Amber) }
        }
        if (ui.isCooldown) item { CooldownCard(ui) }
        if (ui.app.sessionPnl < 0) {
            item { AlertBanner("Red F&O day", "No equity orders today. Your portfolio is not a recovery tool.", Crimson) }
        }
        item {
            if (ui.tradeOpen) ActiveTradeCard(ui, vm::closeTrade) else PreflightCard(ui, vm::toggleCheck, vm::enterTrade)
        }
        item { BigRedButton("Trigger tilt protocol", onClick = vm::startBreathing) }
        item { StatsBento(ui) }
        item { FortressPanel(ui.app.fortressAmount) }
    }
}

@Composable
private fun SessionChip(ui: UiState) {
    val (label, color, live) = when {
        ui.isLocked -> Triple("Locked", Crimson, true)
        ui.isCooldown -> Triple("Cooldown", Amber, true)
        ui.app.sessionClosed -> Triple("Closed", Crimson, false)
        ui.isHolidayToday -> Triple("Holiday", Muted, false)
        !ui.readinessDone -> Triple("Check-in", Amber, true)
        !ui.app.readinessCleared -> Triple("No-trade day", Crimson, false)
        ui.inNoTradeWindow -> Triple("Chop window", Amber, true)
        ui.tradeOpen -> Triple("In trade", Emerald, true)
        else -> Triple("Cleared", Emerald, true)
    }
    StatusChip(label, color, live)
}

@Composable
fun AlertBanner(title: String, body: String, color: Color) {
    Panel(accent = color, glow = true) {
        Eyebrow(title, color = color)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = OnBg)
    }
}

@Composable
private fun StatsBento(ui: UiState) {
    val s = ui.app
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Trades", "${s.tradesToday}/$TRADE_CAP", Modifier.weight(1f), if (s.tradesToday >= TRADE_CAP) Crimson else OnBg)
            StatTile("Clean streak", "${s.cleanDayStreak}d", Modifier.weight(1f), Emerald)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("1R / session", rupees(ONE_R), Modifier.weight(1f), Crimson)
            StatTile("Max margin", rupees(MARGIN_CAP), Modifier.weight(1f))
        }
        Panel(contentPadding = 16.dp) {
            Eyebrow("SL budget · 1 lot")
            Row(Modifier.fillMaxWidth()) {
                SlBudget("Nifty ${s.niftyLot}", RiskEngine.allowedSlPoints(ONE_R, s.niftyLot), Modifier.weight(1f))
                SlBudget("Sensex ${s.sensexLot}", RiskEngine.allowedSlPoints(ONE_R, s.sensexLot), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SlBudget(label: String, points: Double, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        MonoNumber("%.1f pts".format(points), size = 20.sp)
    }
}

@Composable
private fun FortressPanel(amount: Int) {
    Panel(accent = Emerald, glow = true) {
        Eyebrow("Fortress", color = Emerald)
        MonoNumber(rupees(amount), size = 34.sp)
        Text(
            "In equity · non-negotiable. Never margin, never F&O, never sold to fund a trade.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}
