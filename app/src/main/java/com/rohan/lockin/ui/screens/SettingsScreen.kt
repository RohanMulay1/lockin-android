package com.rohan.lockin.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rohan.lockin.domain.COOLDOWN_MIN
import com.rohan.lockin.domain.LOCKOUT_MIN
import com.rohan.lockin.domain.MARGIN_CAP
import com.rohan.lockin.domain.ONE_R
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.TRADE_CAP
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.guard.BrokerGuardService
import com.rohan.lockin.notify.Notifications
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.GhostButton
import com.rohan.lockin.ui.components.KeyValueRow
import com.rohan.lockin.ui.components.MonoField
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.PrimaryButton
import com.rohan.lockin.ui.components.ScreenHeader
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.components.ToggleRow
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

@Composable
fun SettingsScreen(ui: UiState, vm: LockInViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Settings")
        UpdatesPanel(ui, onCheck = vm::checkForUpdates, onInstall = vm::installUpdate)
        HardLimitsPanel()
        KiteBlockerPanel()
        AlarmsPanel(ui, vm)
        TestToolsPanel(vm)
        FortressPanel(ui, vm)
        LotSizesPanel(ui, vm)
    }
}

@Composable
private fun HardLimitsPanel() {
    Panel(accent = Crimson) {
        Eyebrow("Hard limits · locked by design", color = Crimson)
        KeyValueRow("1R per session", rupees(ONE_R), Crimson)
        KeyValueRow("Max margin", rupees(MARGIN_CAP))
        KeyValueRow("Trade cap", "$TRADE_CAP / day")
        KeyValueRow("Time stop", "$TIME_STOP_MIN min")
        KeyValueRow("No new trades", "10:30–11:15")
        KeyValueRow("Lockout", "$LOCKOUT_MIN min")
        KeyValueRow("Cooldown · 2 losses", "$COOLDOWN_MIN min")
        KeyValueRow("Give-back lock", "50% of peak")
        Text("No editing in the heat of the moment. Change them in code, on a weekend.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
private fun KiteBlockerPanel() {
    val context = LocalContext.current
    val enabled = BrokerGuardService.isEnabled(context)
    Panel(accent = if (enabled) Emerald else Crimson, glow = !enabled) {
        Row(Modifier.fillMaxWidth()) {
            Eyebrow("Kite blocker", Modifier.weight(1f))
            StatusChip(if (enabled) "On" else "Off", if (enabled) Emerald else Crimson, live = enabled)
        }
        Text(
            if (enabled) "Kite is covered during lockouts, cooldowns, no-trade days and 10:30–11:15." else "Kite is not blocked. Two steps to turn it on:",
            style = MaterialTheme.typography.bodyMedium,
            color = OnBg,
        )
        if (!enabled) {
            Text("1 · App info → ⋮ → Allow restricted settings", style = MaterialTheme.typography.bodySmall, color = Muted)
            Text("2 · Accessibility → LockIn Kite blocker → On", style = MaterialTheme.typography.bodySmall, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("App info", modifier = Modifier.weight(1f), onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                })
                PrimaryButton("Accessibility", modifier = Modifier.weight(1f), onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                })
            }
        }
    }
}

@Composable
private fun AlarmsPanel(ui: UiState, vm: LockInViewModel) {
    val context = LocalContext.current
    val enabled = Notifications.canPost(context)
    Panel {
        Row(Modifier.fillMaxWidth()) {
            Eyebrow("Alarms & notifications", Modifier.weight(1f))
            StatusChip(if (enabled) "On" else "Off", if (enabled) Emerald else Crimson)
        }
        ToggleRow("Market holiday today · skip alarms", ui.isHolidayToday, vm::setHolidayToday)
        Text("Alarms play on the alarm stream. Keep alarm volume up.", style = MaterialTheme.typography.bodySmall, color = Muted)
        GhostButton("Notification settings", modifier = Modifier.fillMaxWidth(), onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        })
    }
}

@Composable
private fun TestToolsPanel(vm: LockInViewModel) {
    Panel {
        Eyebrow("Test tools")
        Text("Each fires the real alarm: sound plus strong vibration.", style = MaterialTheme.typography.bodySmall, color = Muted)
        GhostButton("Test hourly audit", onClick = vm::fireTestAudit, modifier = Modifier.fillMaxWidth())
        GhostButton("Test 30-min time stop", onClick = vm::fireTestTimeStop, modifier = Modifier.fillMaxWidth())
        GhostButton("Test 10:30 no-trade alarm", onClick = vm::fireTestNoTradeWindow, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            "Test lockout + Kite block · 30 s",
            onClick = vm::fireTestLockout,
            modifier = Modifier.fillMaxWidth(),
            container = Crimson,
            contentColor = OnBg,
        )
        Text("Start the test lockout, then open Kite: the block screen covers it until the 30 s end.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
private fun FortressPanel(ui: UiState, vm: LockInViewModel) {
    var fortress by rememberSaveable(ui.app.fortressAmount) { mutableStateOf(ui.app.fortressAmount.toString()) }
    Panel {
        Eyebrow("Fortress amount")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoField(fortress, { v -> fortress = v.filter(Char::isDigit) }, "₹", Modifier.weight(1f))
            PrimaryButton("Save", onClick = { vm.setFortress(fortress) })
        }
    }
}

@Composable
private fun LotSizesPanel(ui: UiState, vm: LockInViewModel) {
    var niftyLot by rememberSaveable(ui.app.niftyLot) { mutableStateOf(ui.app.niftyLot.toString()) }
    var sensexLot by rememberSaveable(ui.app.sensexLot) { mutableStateOf(ui.app.sensexLot.toString()) }
    Panel {
        Eyebrow("Lot sizes · update if the exchange revises them")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoField(niftyLot, { niftyLot = it.filter(Char::isDigit) }, "Nifty", Modifier.weight(1f))
            MonoField(sensexLot, { sensexLot = it.filter(Char::isDigit) }, "Sensex", Modifier.weight(1f))
        }
        PrimaryButton("Save lot sizes", onClick = { vm.setLotSizes(niftyLot, sensexLot) }, modifier = Modifier.fillMaxWidth())
    }
}
