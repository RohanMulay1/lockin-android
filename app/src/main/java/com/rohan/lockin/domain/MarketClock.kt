package com.rohan.lockin.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object MarketClock {

    val AUDIT_SLOTS: List<LocalTime> = (9..15).map { LocalTime.of(it, 15) }

    fun isTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY

    /** Next audit slot strictly after [now]; [skipToday] jumps to the next trading day (market holiday). */
    fun nextAuditSlot(now: LocalDateTime, skipToday: Boolean = false): LocalDateTime {
        val today = now.toLocalDate()
        if (!skipToday && isTradingDay(today)) {
            AUDIT_SLOTS.firstOrNull { today.atTime(it).isAfter(now) }?.let { return today.atTime(it) }
        }
        var date = today.plusDays(1)
        while (!isTradingDay(date)) date = date.plusDays(1)
        return date.atTime(AUDIT_SLOTS.first())
    }
}

fun epochDayOf(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
