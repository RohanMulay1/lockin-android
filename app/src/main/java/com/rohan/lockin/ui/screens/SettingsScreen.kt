package com.rohan.lockin.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.rohan.lockin.guard.BrokerGuardService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.COOLDOWN_MIN
import com.rohan.lockin.domain.LOCKOUT_MIN
import com.rohan.lockin.domain.MARGIN_CAP
import com.rohan.lockin.domain.ONE_R
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.TRADE_CAP
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.notify.Notifications
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.SectionCard
import com.rohan.lockin.ui.components.ToggleRow
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

@Composable
fun SettingsScreen(ui: UiState, vm: LockInViewModel) {
    val context = LocalContext.current
    val s = ui.app
    var fortress by rememberSaveable(s.fortressAmount) { mutableStateOf(s.fortressAmount.toString()) }
    var niftyLot by rememberSaveable(s.niftyLot) { mutableStateOf(s.niftyLot.toString()) }
    var sensexLot by rememberSaveable(s.sensexLot) { mutableStateOf(s.sensexLot.toString()) }
    val numberKeys = KeyboardOptions(keyboardType = KeyboardType.Number)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("Hard limits (locked by design)", accent = Crimson) {
            listOf(
                "1R per session" to rupees(ONE_R),
                "Max margin" to rupees(MARGIN_CAP),
                "Trade cap" to "$TRADE_CAP / day",
                "Time stop" to "$TIME_STOP_MIN min",
                "Lockout" to "$LOCKOUT_MIN min",
                "Cooldown after 2 losses" to "$COOLDOWN_MIN min",
                "Give-back lock" to "50% of peak (peak ≥ ₹1,000)",
                "No new trades" to "10:30–11:15",
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, color = Muted, fontSize = 13.sp)
                    Text(v, color = OnBg, fontSize = 13.sp)
                }
            }
            Text("No editing in the heat of the moment. Change them in code, on a weekend.", color = Muted, fontSize = 12.sp)
        }

        KiteBlockerCard()

        SectionCard("Fortress") {
            OutlinedTextField(
                value = fortress,
                onValueChange = { v -> fortress = v.filter(Char::isDigit) },
                label = { Text("Fortress amount (₹)") },
                singleLine = true,
                keyboardOptions = numberKeys,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { vm.setFortress(fortress) }) { Text("SAVE") }
        }

        SectionCard("Lot sizes (update if the exchange revises them)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(niftyLot, { niftyLot = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Nifty") }, singleLine = true, keyboardOptions = numberKeys)
                OutlinedTextField(sensexLot, { sensexLot = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Sensex") }, singleLine = true, keyboardOptions = numberKeys)
            }
            Button(onClick = { vm.setLotSizes(niftyLot, sensexLot) }) { Text("SAVE") }
        }

        SectionCard("Alarms & notifications") {
            ToggleRow("Market holiday today (skip audits)", ui.isHolidayToday, vm::setHolidayToday)
            val enabled = Notifications.canPost(context)
            Text(
                if (enabled) "Notifications: ON" else "Notifications: OFF — audits and lockout alerts won't show",
                color = if (enabled) Emerald else Crimson,
                fontSize = 13.sp,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }) { Text("Open notification settings") }
        }

        TestToolsCard(vm)
    }
}

@Composable
private fun TestToolsCard(vm: LockInViewModel) {
    SectionCard("Test tools") {
        Text(
            "Each button fires the real alarm (sound + strong vibration). Keep alarm volume up.",
            color = Muted,
            fontSize = 12.sp,
        )
        Button(onClick = vm::fireTestAudit, modifier = Modifier.fillMaxWidth()) { Text("TEST HOURLY AUDIT") }
        Button(onClick = vm::fireTestTimeStop, modifier = Modifier.fillMaxWidth()) { Text("TEST 30-MIN TIME STOP ALARM") }
        Button(onClick = vm::fireTestNoTradeWindow, modifier = Modifier.fillMaxWidth()) { Text("TEST 10:30 NO-TRADE ALARM") }
        OutlinedButton(onClick = vm::fireTestLockout, modifier = Modifier.fillMaxWidth()) {
            Text("TEST LOCKOUT + KITE BLOCK (30 S)", color = Crimson)
        }
        Text(
            "After the test lockout starts, open Kite: the block screen should cover it until the 30 s end.",
            color = Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun KiteBlockerCard() {
    val context = LocalContext.current
    val enabled = BrokerGuardService.isEnabled(context)
    SectionCard("Kite blocker", accent = if (enabled) Emerald else Crimson) {
        Text(
            if (enabled) "ON — Kite is covered during lockouts, cooldowns and 10:30–11:15" else "OFF — Kite is not blocked",
            color = if (enabled) Emerald else Crimson,
            fontSize = 14.sp,
        )
        if (!enabled) {
            Text(
                "1. Tap App info → ⋮ (top right) → Allow restricted settings.\n" +
                    "2. Tap Accessibility → Installed apps → LockIn Kite blocker → On.",
                color = Muted,
                fontSize = 12.sp,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            }) { Text("1 · App info") }
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text("2 · Accessibility settings")
            }
        }
    }
}
