package com.rohan.lockin.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerGuardTest {

    private val monday = LocalDate.of(2026, 11, 16)
    private val today = monday.toEpochDay()
    private val nowMs = 1_000_000_000L
    private val cleared = AppState(sessionDay = today, readinessDay = today, readinessCleared = true)

    private fun at(h: Int, m: Int): LocalDateTime = monday.atTime(h, m)

    @Test
    fun `clean state in market hours is not blocked`() {
        assertNull(BrokerGuard.blockFor(cleared, nowMs, at(10, 0)))
    }

    @Test
    fun `active lockout blocks even after market hours`() {
        val locked = cleared.copy(lockUntil = nowMs + 1)
        assertEquals(BrokerBlock.LOCKOUT, BrokerGuard.blockFor(locked, nowMs, at(18, 0)))
    }

    @Test
    fun `cooldown, closed session and failed readiness block during market hours`() {
        assertEquals(BrokerBlock.COOLDOWN, BrokerGuard.blockFor(cleared.copy(cooldownUntil = nowMs + 1), nowMs, at(12, 0)))
        assertEquals(BrokerBlock.SESSION_CLOSED, BrokerGuard.blockFor(cleared.copy(sessionClosed = true), nowMs, at(12, 0)))
        assertEquals(BrokerBlock.NO_TRADE_DAY, BrokerGuard.blockFor(cleared.copy(readinessCleared = false), nowMs, at(12, 0)))
    }

    @Test
    fun `closed session from a previous day does not block`() {
        val stale = cleared.copy(sessionClosed = true, sessionDay = today - 1)
        assertNull(BrokerGuard.blockFor(stale, nowMs, at(12, 0)))
    }

    @Test
    fun `no-trade window blocks new entries but not an open trade`() {
        assertEquals(BrokerBlock.NO_TRADE_WINDOW, BrokerGuard.blockFor(cleared, nowMs, at(10, 30)))
        assertEquals(BrokerBlock.NO_TRADE_WINDOW, BrokerGuard.blockFor(cleared, nowMs, at(11, 14)))
        assertNull(BrokerGuard.blockFor(cleared, nowMs, at(11, 15)))
        assertNull(BrokerGuard.blockFor(cleared.copy(tradeStartTs = 5L), nowMs, at(10, 45)))
    }

    @Test
    fun `exit pass unblocks even a lockout until it expires`() {
        val locked = cleared.copy(lockUntil = nowMs + 3_600_000L)
        val passed = BrokerGuard.grantExitPass(locked, nowMs)
        assertNull(BrokerGuard.blockFor(passed, nowMs + 60_000L, at(12, 0)))
        assertEquals(BrokerBlock.LOCKOUT, BrokerGuard.blockFor(passed, nowMs + 2 * 60_000L, at(12, 2)))
        assertEquals(1, passed.exitPassesToday)
    }

    @Test
    fun `holiday and weekends are never blocked without a lockout`() {
        assertNull(BrokerGuard.blockFor(cleared.copy(holidayDay = today, sessionClosed = true), nowMs, at(12, 0)))
        assertNull(BrokerGuard.blockFor(cleared.copy(sessionClosed = true), nowMs, monday.minusDays(1).atTime(12, 0)))
    }

    @Test
    fun `market clock window boundaries`() {
        assertFalse(MarketClock.isNoTradeWindow(at(10, 29)))
        assertTrue(MarketClock.isNoTradeWindow(at(10, 30)))
        assertFalse(MarketClock.isNoTradeWindow(monday.minusDays(2).atTime(10, 45)))
        assertEquals(at(10, 30), MarketClock.nextNoTradeWindowStart(at(9, 0)))
        assertEquals(monday.plusDays(1).atTime(10, 30), MarketClock.nextNoTradeWindowStart(at(10, 30)))
    }

    @Test
    fun `rollover resets exit passes`() {
        val next = SessionRules.rolledOver(cleared.copy(exitPassesToday = 3), today + 1)
        assertEquals(0, next.exitPassesToday)
    }
}
