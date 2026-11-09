package com.rohan.lockin.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rohan.lockin.domain.MarketClock
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.TIME_STOP_WARN_MIN
import com.rohan.lockin.domain.epochMsOf
import java.time.LocalDateTime

object AlarmScheduler {

    private const val REQ_AUDIT = 2001
    private const val REQ_TIME_STOP_WARN = 2002
    private const val REQ_TIME_STOP_EXIT = 2003
    private const val REQ_NO_TRADE_WINDOW = 2004
    private const val MINUTE_MS = 60_000L

    /** Arms every recurring market alarm; each receiver re-arms its own next occurrence. */
    fun scheduleDaily(context: Context, skipToday: Boolean) {
        scheduleNextAudit(context, skipToday)
        scheduleNoTradeWindow(context, skipToday)
    }

    /** Next hourly audit (09:15..15:15, Mon–Fri). */
    fun scheduleNextAudit(context: Context, skipToday: Boolean) {
        val at = epochMsOf(MarketClock.nextAuditSlot(LocalDateTime.now(), skipToday))
        setExact(context, at, broadcast(context, REQ_AUDIT, Intent(context, HourlyAuditReceiver::class.java)))
    }

    /** Next 10:30 no-trade-window alarm. */
    fun scheduleNoTradeWindow(context: Context, skipToday: Boolean) {
        val at = epochMsOf(MarketClock.nextNoTradeWindowStart(LocalDateTime.now(), skipToday))
        setExact(context, at, broadcast(context, REQ_NO_TRADE_WINDOW, Intent(context, NoTradeWindowReceiver::class.java)))
    }

    fun scheduleTimeStops(context: Context, tradeStartMs: Long) {
        setExact(context, tradeStartMs + TIME_STOP_WARN_MIN * MINUTE_MS, timeStopIntent(context, REQ_TIME_STOP_WARN, TIME_STOP_WARN_MIN))
        setExact(context, tradeStartMs + TIME_STOP_MIN * MINUTE_MS, timeStopIntent(context, REQ_TIME_STOP_EXIT, TIME_STOP_MIN))
    }

    fun cancelTimeStops(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(timeStopIntent(context, REQ_TIME_STOP_WARN, TIME_STOP_WARN_MIN))
        am.cancel(timeStopIntent(context, REQ_TIME_STOP_EXIT, TIME_STOP_MIN))
    }

    private fun timeStopIntent(context: Context, requestCode: Int, minute: Int): PendingIntent =
        broadcast(
            context,
            requestCode,
            Intent(context, TimeStopReceiver::class.java).putExtra(TimeStopReceiver.EXTRA_MINUTE, minute),
        )

    private fun broadcast(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun setExact(context: Context, atMs: Long, pending: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pending)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pending)
        }
    }
}
