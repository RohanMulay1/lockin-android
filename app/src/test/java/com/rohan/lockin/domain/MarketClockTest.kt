package com.rohan.lockin.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketClockTest {

    private val friday = LocalDate.of(2026, 11, 13)
    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun `test dates fall on the expected weekdays`() {
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
    }

    @Test
    fun `before the open the first slot is 0915 today`() {
        val next = MarketClock.nextAuditSlot(friday.atTime(8, 0))
        assertEquals(friday.atTime(9, 15), next)
    }

    @Test
    fun `exactly on a slot moves to the next hour`() {
        val next = MarketClock.nextAuditSlot(friday.atTime(10, 15))
        assertEquals(friday.atTime(11, 15), next)
    }

    @Test
    fun `after the last Friday slot jumps to Monday 0915`() {
        val next = MarketClock.nextAuditSlot(LocalDateTime.of(friday, LocalTime.of(15, 20)))
        assertEquals(monday.atTime(9, 15), next)
    }

    @Test
    fun `weekend skips to Monday`() {
        val saturday = friday.plusDays(1)
        assertEquals(monday.atTime(9, 15), MarketClock.nextAuditSlot(saturday.atTime(11, 0)))
    }

    @Test
    fun `holiday skip moves to the next trading day`() {
        val next = MarketClock.nextAuditSlot(monday.atTime(8, 0), skipToday = true)
        assertEquals(monday.plusDays(1).atTime(9, 15), next)
    }

    @Test
    fun `there are seven hourly slots from 0915 to 1515`() {
        assertEquals(7, MarketClock.AUDIT_SLOTS.size)
        assertEquals(LocalTime.of(15, 15), MarketClock.AUDIT_SLOTS.last())
    }
}
