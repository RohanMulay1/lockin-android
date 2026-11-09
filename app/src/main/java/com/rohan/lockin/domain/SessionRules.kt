package com.rohan.lockin.domain

/** Pure state transitions for a trading session. Every function returns a new AppState. */
object SessionRules {

    private const val MINUTE_MS = 60_000L

    fun applyVerdict(s: AppState, v: Verdict, now: Long, limits: RiskLimits = RiskLimits()): AppState =
        when (v) {
            is Verdict.Lockout -> s.copy(
                lockUntil = now + limits.lockoutMinutes * MINUTE_MS,
                lockReason = v.reason.name,
                sessionClosed = true,
                lockouts = s.lockouts + 1,
                tradeStartTs = 0L,
            )
            Verdict.Cooldown -> s.copy(
                cooldownUntil = now + limits.cooldownMinutes * MINUTE_MS,
                consecutiveLosses = 0,
            )
            Verdict.Clear -> s
        }

    fun withPnl(s: AppState, pnl: Int): AppState =
        s.copy(sessionPnl = pnl, peakPnl = maxOf(s.peakPnl, pnl))

    /** Starts a trade, or returns a TRADE_CAP lockout (without starting) when the cap is already used. */
    fun enterTrade(s: AppState, now: Long, limits: RiskLimits = RiskLimits()): Pair<AppState, Verdict> {
        val started = s.copy(tradesToday = s.tradesToday + 1, tradeStartTs = now, checklist = emptySet())
        val verdict = RiskEngine.evaluate(started.snapshot(), limits)
        return if (verdict is Verdict.Lockout && verdict.reason == LockReason.TRADE_CAP) {
            applyVerdict(s.copy(violations = s.violations + 1), verdict, now, limits) to verdict
        } else {
            started to Verdict.Clear
        }
    }

    fun closeTrade(s: AppState, result: Int, holdMinutes: Int): AppState {
        val withResult = withPnl(s, s.sessionPnl + result)
        return withResult.copy(
            consecutiveLosses = if (result < 0) s.consecutiveLosses + 1 else 0,
            tradeStartTs = 0L,
            violations = s.violations + if (holdMinutes > TIME_STOP_MIN) 1 else 0,
        )
    }

    /** Archives nothing itself; resets session fields for [today] and updates the clean-day streak. */
    fun rolledOver(s: AppState, today: Long): AppState {
        val traded = s.tradesToday > 0
        val clean = s.violations == 0 && s.lockouts == 0
        val streak = when {
            s.sessionDay <= 0 || !traded -> s.cleanDayStreak
            clean -> s.cleanDayStreak + 1
            else -> 0
        }
        return s.copy(
            sessionDay = today,
            sessionPnl = 0,
            peakPnl = 0,
            tradesToday = 0,
            consecutiveLosses = 0,
            violations = 0,
            lockouts = 0,
            checklist = emptySet(),
            tradeStartTs = 0L,
            cooldownUntil = 0L,
            sessionClosed = false,
            cigarettesToday = 0,
            cleanDayStreak = streak,
        )
    }
}
