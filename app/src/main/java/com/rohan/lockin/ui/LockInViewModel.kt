package com.rohan.lockin.ui

import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rohan.lockin.LockInApp
import com.rohan.lockin.data.LockInRepository
import com.rohan.lockin.data.db.TradeEntity
import com.rohan.lockin.domain.AppState
import com.rohan.lockin.domain.COOLDOWN_MIN
import com.rohan.lockin.domain.LOCKOUT_MIN
import com.rohan.lockin.domain.LockReason
import com.rohan.lockin.domain.RiskEngine
import com.rohan.lockin.domain.SessionRules
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.Verdict
import com.rohan.lockin.domain.epochDayOf
import com.rohan.lockin.domain.snapshot
import com.rohan.lockin.domain.todayEpochDay
import com.rohan.lockin.haptics.Haptics
import com.rohan.lockin.notify.AlarmScheduler
import com.rohan.lockin.notify.NoTradeWindowReceiver
import com.rohan.lockin.notify.Notifications
import com.rohan.lockin.notify.TimeStopReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LockInViewModel(
    private val app: LockInApp,
    private val repo: LockInRepository,
) : AndroidViewModel(app) {

    private val overlay = MutableStateFlow(Overlay.None)

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MS)
        }
    }

    val ui: StateFlow<UiState> =
        combine(repo.state, ticker, repo.dailyLogs, repo.recentTrades, overlay) { s, now, logs, trades, ov ->
            UiState(app = s, nowMs = now, logs = logs, trades = trades, overlay = ov)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        viewModelScope.launch { bootstrap() }
        viewModelScope.launch {
            while (true) {
                delay(ROLLOVER_CHECK_MS)
                repo.ensureToday(todayEpochDay())
            }
        }
    }

    private suspend fun bootstrap() {
        repo.ensureToday(todayEpochDay())
        val s = repo.update { if (it.quitTs == 0L) it.copy(quitTs = System.currentTimeMillis()) else it }
        Notifications.postStatus(app, s)
        AlarmScheduler.scheduleDaily(app, skipToday = s.holidayDay == todayEpochDay())
    }

    fun refreshStatus() {
        viewModelScope.launch { Notifications.postStatus(app, repo.state.first()) }
    }

    // ---- Trading session ----

    fun setPnl(text: String) {
        val pnl = text.trim().toIntOrNull() ?: return
        mutate { s, now ->
            val next = SessionRules.withPnl(s, pnl)
            val verdict = RiskEngine.evaluate(next.snapshot())
            SessionRules.applyVerdict(next, verdict, now) to verdict
        }
    }

    fun toggleCheck(id: String) {
        Haptics.tick(app)
        viewModelScope.launch {
            repo.update { s -> s.copy(checklist = if (id in s.checklist) s.checklist - id else s.checklist + id) }
        }
    }

    fun enterTrade() {
        if (ui.value.blockReason != null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var verdict: Verdict = Verdict.Clear
            val next = repo.update { s ->
                val (entered, v) = SessionRules.enterTrade(s, now)
                verdict = v
                entered
            }
            react(verdict)
            if (next.tradeStartTs == now) {
                Haptics.tick(app)
                AlarmScheduler.scheduleTimeStops(app, now)
            }
        }
    }

    fun closeTrade(resultText: String) {
        val result = resultText.trim().toIntOrNull() ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var verdict: Verdict = Verdict.Clear
            var startTs = 0L
            repo.update { s ->
                startTs = s.tradeStartTs
                if (startTs == 0L) {
                    s
                } else {
                    val closed = SessionRules.closeTrade(s, result, holdMinutes(startTs, now))
                    verdict = RiskEngine.evaluate(closed.snapshot())
                    SessionRules.applyVerdict(closed, verdict, now)
                }
            }
            if (startTs == 0L) return@launch
            AlarmScheduler.cancelTimeStops(app)
            repo.insertTrade(
                TradeEntity(
                    epochDay = epochDayOf(now),
                    entryTs = startTs,
                    exitTs = now,
                    pnl = result,
                    holdMinutes = holdMinutes(startTs, now),
                ),
            )
            react(verdict)
        }
    }

    fun submitReadiness(slept: Boolean, notTravelling: Boolean, noBigLoss: Boolean, calm: Boolean) {
        val cleared = slept && notTravelling && noBigLoss && calm
        if (cleared) Haptics.tick(app) else Haptics.heavy(app)
        viewModelScope.launch {
            repo.update { it.copy(readinessDay = todayEpochDay(), readinessCleared = cleared) }
        }
    }

    fun submitAudit(oneLot: Boolean, slPlaced: Boolean, calm: Boolean) {
        if (oneLot && slPlaced && calm) {
            Haptics.tick(app)
            return
        }
        val ruleBroken = !oneLot || !slPlaced
        viewModelScope.launch {
            repo.update { s -> s.copy(violations = s.violations + if (ruleBroken) 1 else 0) }
        }
        overlay.value = Overlay.Breathing
    }

    // ---- Overlays ----

    fun startBreathing() {
        overlay.value = Overlay.Breathing
    }

    fun startGrounding() {
        overlay.value = Overlay.Grounding
    }

    fun closeOverlay() {
        overlay.value = Overlay.None
    }

    // ---- Smoking ----

    fun logCigarette() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val next = repo.update { s -> s.copy(quitTs = now, cigarettesToday = s.cigarettesToday + 1) }
            repo.insertSmoke(now)
            Notifications.postStatus(app, next)
        }
    }

    // ---- Settings ----

    fun setFortress(text: String) {
        val amount = text.filter(Char::isDigit).toIntOrNull()?.takeIf { it > 0 } ?: return
        viewModelScope.launch { Notifications.postStatus(app, repo.update { it.copy(fortressAmount = amount) }) }
    }

    fun setLotSizes(niftyText: String, sensexText: String) {
        val nifty = niftyText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return
        val sensex = sensexText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return
        viewModelScope.launch { repo.update { it.copy(niftyLot = nifty, sensexLot = sensex) } }
    }

    fun setHolidayToday(on: Boolean) {
        viewModelScope.launch {
            val today = todayEpochDay()
            repo.update { it.copy(holidayDay = if (on) today else -1L) }
            AlarmScheduler.scheduleDaily(app, skipToday = on)
        }
    }

    fun fireTestAudit() {
        Notifications.postAudit(app)
    }

    /** 30-second lock that exercises the overlay, alarm and Kite block without closing the session. */
    fun fireTestLockout() {
        viewModelScope.launch {
            repo.update { it.copy(lockUntil = System.currentTimeMillis() + TEST_LOCK_MS, lockReason = LockReason.TEST.name) }
            Notifications.postAlert(app, Notifications.ID_LOCK, "🔒 ${LockReason.TEST.headline}", LockReason.TEST.detail)
        }
    }

    /** Sends the real time-stop broadcast, so the test covers the same path as a live trade. */
    fun fireTestTimeStop() {
        app.sendBroadcast(Intent(app, TimeStopReceiver::class.java).putExtra(TimeStopReceiver.EXTRA_MINUTE, TIME_STOP_MIN))
    }

    fun fireTestNoTradeWindow() {
        app.sendBroadcast(Intent(app, NoTradeWindowReceiver::class.java))
    }

    // ---- Internals ----

    private fun mutate(block: (AppState, Long) -> Pair<AppState, Verdict>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var verdict: Verdict = Verdict.Clear
            repo.update { s ->
                val (next, v) = block(s, now)
                verdict = v
                next
            }
            react(verdict)
        }
    }

    private fun react(verdict: Verdict) {
        when (verdict) {
            is Verdict.Lockout -> {
                AlarmScheduler.cancelTimeStops(app)
                Notifications.postAlert(
                    app,
                    Notifications.ID_LOCK,
                    "🔒 LOCKED FOR $LOCKOUT_MIN MIN — ${verdict.reason.headline}",
                    "${verdict.reason.detail} Kite is blocked; exit-only pass if a position is open.",
                )
            }
            Verdict.Cooldown -> {
                Notifications.postAlert(
                    app,
                    Notifications.ID_COOLDOWN,
                    "🧊 $COOLDOWN_MIN-MIN COOLDOWN — 2 LOSSES IN A ROW",
                    "Step away from the chart. Kite is blocked until the cooldown ends.",
                )
            }
            Verdict.Clear -> Unit
        }
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val ROLLOVER_CHECK_MS = 60_000L
        private const val TEST_LOCK_MS = 30_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LockInApp
                LockInViewModel(app, app.container.repository)
            }
        }
    }
}

private fun holdMinutes(startMs: Long, endMs: Long): Int = ((endMs - startMs) / 60_000L).toInt()
