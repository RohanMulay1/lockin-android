package com.rohan.lockin.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object MarketClock {

    val AUDIT_SLOTS: List<LocalTime> = (9..15).map { LocalTime.of(it, 15) }
    val NO_TRADE_START: LocalTime = LocalTime.of(10, 30)
    val NO_TRADE_END: LocalTime = LocalTime.of(11, 15)
    private val MARKET_OPEN: LocalTime = LocalTime.of(9, 0)
    private val MARKET_CLOSE: LocalTime = LocalTime.of(15, 30)

    fun isTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY

    fun isMarketHours(now: LocalDateTime): Boolean =
        isTradingDay(now.toLocalDate()) && isBetween(now.toLocalTime(), MARKET_OPEN, MARKET_CLOSE)

    /** 10:30 (inclusive) to 11:15 (exclusive) on trading days: no new entries. */
    fun isNoTradeWindow(now: LocalDateTime): Boolean =
        isTradingDay(now.toLocalDate()) && isBetween(now.toLocalTime(), NO_TRADE_START, NO_TRADE_END)

    /** Next audit slot strictly after [now]; [skipToday] jumps to the next trading day (market holiday). */
    fun nextAuditSlot(now: LocalDateTime, skipToday: Boolean = false): LocalDateTime =
        nextSlot(now, AUDIT_SLOTS, skipToday)

    fun nextNoTradeWindowStart(now: LocalDateTime, skipToday: Boolean = false): LocalDateTime =
        nextSlot(now, listOf(NO_TRADE_START), skipToday)

    private fun nextSlot(now: LocalDateTime, slots: List<LocalTime>, skipToday: Boolean): LocalDateTime {
        val today = now.toLocalDate()
        if (!skipToday && isTradingDay(today)) {
            slots.firstOrNull { today.atTime(it).isAfter(now) }?.let { return today.atTime(it) }
        }
        var date = today.plusDays(1)
        while (!isTradingDay(date)) date = date.plusDays(1)
        return date.atTime(slots.first())
    }

    private fun isBetween(t: LocalTime, start: LocalTime, end: LocalTime): Boolean =
        !t.isBefore(start) && t.isBefore(end)
}

fun epochDayOf(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

fun localDateTimeOf(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

fun epochMsOf(dateTime: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
    dateTime.atZone(zone).toInstant().toEpochMilli()

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
