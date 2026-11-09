package com.rohan.lockin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {

    private val now = 1_000_000L

    @Test
    fun `lockout sets 60 minute lock and closes session`() {
        val locked = SessionRules.applyVerdict(AppState(tradeStartTs = 5L), Verdict.Lockout(LockReason.MAX_DRAWDOWN), now)
        assertEquals(now + 60 * 60_000L, locked.lockUntil)
        assertTrue(locked.sessionClosed)
        assertEquals(1, locked.lockouts)
        assertEquals(0L, locked.tradeStartTs)
        assertEquals("MAX_DRAWDOWN", locked.lockReason)
    }

    @Test
    fun `cooldown resets the loss streak`() {
        val cooled = SessionRules.applyVerdict(AppState(consecutiveLosses = 2), Verdict.Cooldown, now)
        assertEquals(now + 15 * 60_000L, cooled.cooldownUntil)
        assertEquals(0, cooled.consecutiveLosses)
    }

    @Test
    fun `entering a trade counts it, starts the timer and clears the checklist`() {
        val (next, verdict) = SessionRules.enterTrade(AppState(checklist = setOf("htf")), now)
        assertEquals(Verdict.Clear, verdict)
        assertEquals(1, next.tradesToday)
        assertEquals(now, next.tradeStartTs)
        assertTrue(next.checklist.isEmpty())
    }

    @Test
    fun `entering a trade beyond the cap locks out without starting it`() {
        val (next, verdict) = SessionRules.enterTrade(AppState(tradesToday = TRADE_CAP), now)
        assertEquals(Verdict.Lockout(LockReason.TRADE_CAP), verdict)
        assertEquals(TRADE_CAP, next.tradesToday)
        assertEquals(0L, next.tradeStartTs)
        assertEquals(1, next.violations)
    }

    @Test
    fun `closing a losing trade held past the time stop adds a violation`() {
        val closed = SessionRules.closeTrade(AppState(sessionPnl = 500, peakPnl = 500, tradeStartTs = 1L), -800, holdMinutes = 31)
        assertEquals(-300, closed.sessionPnl)
        assertEquals(500, closed.peakPnl)
        assertEquals(1, closed.consecutiveLosses)
        assertEquals(1, closed.violations)
        assertEquals(0L, closed.tradeStartTs)
    }

    @Test
    fun `a winning trade resets the loss streak and raises the peak`() {
        val closed = SessionRules.closeTrade(AppState(consecutiveLosses = 1), 1_500, holdMinutes = 4)
        assertEquals(0, closed.consecutiveLosses)
        assertEquals(1_500, closed.peakPnl)
        assertEquals(0, closed.violations)
    }

    @Test
    fun `rollover extends streak after a clean trading day`() {
        val next = SessionRules.rolledOver(AppState(sessionDay = 10, tradesToday = 3, cleanDayStreak = 2), 11)
        assertEquals(3, next.cleanDayStreak)
        assertEquals(11L, next.sessionDay)
        assertEquals(0, next.tradesToday)
    }

    @Test
    fun `rollover breaks streak after a day with a lockout`() {
        val next = SessionRules.rolledOver(AppState(sessionDay = 10, tradesToday = 3, lockouts = 1, cleanDayStreak = 5), 11)
        assertEquals(0, next.cleanDayStreak)
        assertEquals(0, next.lockouts)
    }

    @Test
    fun `rollover keeps streak on a day without trades`() {
        val next = SessionRules.rolledOver(AppState(sessionDay = 10, cleanDayStreak = 4), 11)
        assertEquals(4, next.cleanDayStreak)
    }
}
