package com.rohan.lockin.domain

import java.time.LocalDateTime

enum class BrokerBlock(val title: String, val detail: String) {
    LOCKOUT("LOCKOUT ACTIVE", "You hit a hard limit. Close Kite and walk away."),
    COOLDOWN("COOLDOWN", "2 losses in a row. 15 minutes away from the chart."),
    SESSION_CLOSED("SESSION CLOSED", "Trading is done for today. Back tomorrow."),
    NO_TRADE_DAY("NO-TRADE DAY", "Readiness check failed. Protect capital today."),
    NO_TRADE_WINDOW("NO NEW TRADES 10:30–11:15", "Mid-morning chop window. New entries unlock at 11:15."),
}

/** Decides whether the broker app must be covered by the block screen. Pure, so it is unit tested. */
object BrokerGuard {

    val BROKER_PACKAGES: Set<String> = setOf("com.zerodha.kite3")
    const val EXIT_PASS_MIN = 2
    private const val MINUTE_MS = 60_000L

    fun blockFor(s: AppState, nowMs: Long, now: LocalDateTime): BrokerBlock? {
        if (s.brokerPassUntil > nowMs) return null
        if (s.lockUntil > nowMs) return BrokerBlock.LOCKOUT
        if (!MarketClock.isMarketHours(now)) return null
        val today = now.toLocalDate().toEpochDay()
        if (s.holidayDay == today) return null
        return when {
            s.cooldownUntil > nowMs -> BrokerBlock.COOLDOWN
            s.sessionClosed && s.sessionDay == today -> BrokerBlock.SESSION_CLOSED
            s.readinessDay == today && !s.readinessCleared -> BrokerBlock.NO_TRADE_DAY
            MarketClock.isNoTradeWindow(now) && s.tradeStartTs == 0L -> BrokerBlock.NO_TRADE_WINDOW
            else -> null
        }
    }

    /** Short pass to close positions; it never unlocks new entries in LockIn itself. */
    fun grantExitPass(s: AppState, nowMs: Long): AppState =
        s.copy(brokerPassUntil = nowMs + EXIT_PASS_MIN * MINUTE_MS, exitPassesToday = s.exitPassesToday + 1)
}
