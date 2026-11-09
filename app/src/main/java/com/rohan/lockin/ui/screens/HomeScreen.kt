package com.rohan.lockin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.MARGIN_CAP
import com.rohan.lockin.domain.ONE_R
import com.rohan.lockin.domain.RiskEngine
import com.rohan.lockin.domain.TRADE_CAP
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.BigRedButton
import com.rohan.lockin.ui.components.SectionCard
import com.rohan.lockin.ui.components.StatCell
import com.rohan.lockin.ui.theme.Black0
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Mono
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

@Composable
fun HomeScreen(ui: UiState, vm: LockInViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { FortressBanner(ui.app.fortressAmount) }
        item { BigRedButton("TRIGGER TILT PROTOCOL", onClick = vm::startBreathing) }
        item { ReadinessCard(ui, vm::submitReadiness) }
        item { RiskCard(ui) }
        item { PnlCard(ui, vm::setPnl) }
        if (ui.app.sessionPnl < 0) item { RedDayCard() }
        if (ui.isCooldown) item { CooldownCard(ui) }
        item {
            if (ui.tradeOpen) {
                ActiveTradeCard(ui, vm::closeTrade)
            } else {
                PreflightCard(ui, vm::toggleCheck, vm::enterTrade)
            }
        }
    }
}

@Composable
private fun FortressBanner(amount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Black0),
        border = BorderStroke(2.dp, Emerald),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "FORTRESS: ${rupees(amount)} IN EQUITY (NON-NEGOTIABLE)",
                color = Emerald,
                fontFamily = Mono,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            Text("Long-term capital. Never margin. Never F&O. Never sold to fund a trade.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RiskCard(ui: UiState) {
    val s = ui.app
    val niftyPts = RiskEngine.allowedSlPoints(ONE_R, s.niftyLot)
    val sensexPts = RiskEngine.allowedSlPoints(ONE_R, s.sensexLot)
    SectionCard("1R risk engine") {
        Row(Modifier.fillMaxWidth()) {
            StatCell("1R / session", rupees(ONE_R), Modifier.weight(1f), Crimson)
            StatCell("Max margin", rupees(MARGIN_CAP), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            val capColor = if (s.tradesToday >= TRADE_CAP) Crimson else OnBg
            StatCell("Trades today", "${s.tradesToday}/$TRADE_CAP", Modifier.weight(1f), capColor)
            StatCell("Clean-day streak", "${s.cleanDayStreak}d", Modifier.weight(1f), Emerald)
        }
        Text(
            "SL budget for 1 lot → Nifty (${s.niftyLot}): %.1f pts · Sensex (${s.sensexLot}): %.1f pts".format(niftyPts, sensexPts),
            color = Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun RedDayCard() {
    SectionCard(title = null, accent = Crimson) {
        Text("RED F&O DAY", color = Crimson, fontWeight = FontWeight.Black)
        Text("No equity orders today. Your long-term portfolio is not a recovery tool.", color = OnBg, fontSize = 13.sp)
    }
}
