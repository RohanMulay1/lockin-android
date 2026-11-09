package com.rohan.lockin.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rohan.lockin.domain.MarketClock
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.TIME_STOP_WARN_MIN
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    private const val REQ_AUDIT = 2001
    private const val REQ_TIME_STOP_WARN = 2002
    private const val REQ_TIME_STOP_EXIT = 2003
    private const val MINUTE_MS = 60_000L

    /** Arms the next hourly audit (09:15..15:15, Mon–Fri). Each audit re-arms the following one. */
    fun scheduleNextAudit(context: Context, skipToday: Boolean) {
        val next = MarketClock.nextAuditSlot(LocalDateTime.now(), skipToday)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setExact(context, at, broadcast(context, REQ_AUDIT, Intent(context, HourlyAuditReceiver::class.java)))
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
