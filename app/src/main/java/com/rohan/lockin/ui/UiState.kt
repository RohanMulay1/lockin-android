package com.rohan.lockin.ui

import com.rohan.lockin.data.ReleaseInfo
import com.rohan.lockin.data.db.DailyLogEntity
import com.rohan.lockin.data.db.TradeEntity
import com.rohan.lockin.domain.AppState
import com.rohan.lockin.domain.LockReason
import com.rohan.lockin.domain.MarketClock
import com.rohan.lockin.domain.PREFLIGHT_CHECKLIST
import com.rohan.lockin.domain.epochDayOf
import com.rohan.lockin.domain.epochMsOf
import com.rohan.lockin.domain.localDateTimeOf
import com.rohan.lockin.domain.mmss

enum class Overlay { None, Breathing, Grounding }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class Failed(val message: String) : UpdateState
}

data class UiState(
    val app: AppState = AppState(),
    val nowMs: Long = 0L,
    val logs: List<DailyLogEntity> = emptyList(),
    val trades: List<TradeEntity> = emptyList(),
    val overlay: Overlay = Overlay.None,
    val update: UpdateState = UpdateState.Idle,
    val installedVersion: Long = 0L,
) {
    val today: Long get() = epochDayOf(nowMs)
    val isLocked: Boolean get() = app.lockUntil > nowMs
    val lockRemainingMs: Long get() = (app.lockUntil - nowMs).coerceAtLeast(0L)
    val lockReason: LockReason
        get() = LockReason.entries.firstOrNull { it.name == app.lockReason } ?: LockReason.MAX_DRAWDOWN
    val isCooldown: Boolean get() = app.cooldownUntil > nowMs
    val cooldownRemainingMs: Long get() = (app.cooldownUntil - nowMs).coerceAtLeast(0L)
    val readinessDone: Boolean get() = app.readinessDay == today
    val isHolidayToday: Boolean get() = app.holidayDay == today
    val tradeOpen: Boolean get() = app.tradeStartTs > 0L
    val tradeElapsedMs: Long get() = if (tradeOpen) (nowMs - app.tradeStartTs).coerceAtLeast(0L) else 0L
    val allChecked: Boolean get() = PREFLIGHT_CHECKLIST.all { it.id in app.checklist }
    val inNoTradeWindow: Boolean get() = MarketClock.isNoTradeWindow(localDateTimeOf(nowMs))
    val noTradeWindowRemainingMs: Long
        get() = (epochMsOf(localDateTimeOf(nowMs).toLocalDate().atTime(MarketClock.NO_TRADE_END)) - nowMs)
            .coerceAtLeast(0L)

    /** Why ENTER TRADE is disabled, or null when the gate is open. */
    val blockReason: String?
        get() = when {
            isLocked -> "Locked — ${mmss(lockRemainingMs)} remaining"
            app.sessionClosed -> "Session closed for today. Back tomorrow."
            isHolidayToday -> "Market holiday"
            !readinessDone -> "Complete the morning readiness check first"
            !app.readinessCleared -> "NO-TRADE DAY — protect capital"
            inNoTradeWindow -> "No new trades 10:30–11:15 — ${mmss(noTradeWindowRemainingMs)} left"
            isCooldown -> "Cooldown after 2 losses — ${mmss(cooldownRemainingMs)}"
            !allChecked -> "Tick all 5 pre-flight checks"
            else -> null
        }
}
