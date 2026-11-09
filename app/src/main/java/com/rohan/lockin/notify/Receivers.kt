package com.rohan.lockin.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rohan.lockin.domain.TIME_STOP_MIN
import com.rohan.lockin.domain.todayEpochDay
import com.rohan.lockin.haptics.Haptics
import com.rohan.lockin.lockInApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Fires at each hourly slot: posts the audit, refreshes the status notification, arms the next slot. */
class HourlyAuditReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync {
        val state = context.lockInApp.container.store.state.first()
        val holiday = state.holidayDay == todayEpochDay()
        if (!holiday) Notifications.postAudit(context)
        Notifications.postStatus(context, state)
        AlarmScheduler.scheduleNextAudit(context, skipToday = holiday)
    }
}

/** 25-minute warning and 30-minute hard exit for the open trade. */
class TimeStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra(EXTRA_MINUTE, TIME_STOP_MIN)
        if (minute >= TIME_STOP_MIN) {
            Haptics.heavy(context)
            Notifications.postAlert(
                context,
                Notifications.ID_TIMESTOP,
                "EXIT NOW — 30-MIN TIME STOP",
                "Your trades held past 30 min lost ₹45,646 combined. Close it.",
            )
        } else {
            Notifications.postAlert(
                context,
                Notifications.ID_TIMESTOP,
                "5 MIN LEFT ON THIS TRADE",
                "Time stop at 30 min. If it isn't working, exit now.",
            )
        }
    }

    companion object {
        const val EXTRA_MINUTE = "minute"
    }
}

/** Alarms don't survive reboot or clock changes; re-arm them and re-post the ongoing notification. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync {
        val state = context.lockInApp.container.store.state.first()
        AlarmScheduler.scheduleNextAudit(context, skipToday = state.holidayDay == todayEpochDay())
        Notifications.postStatus(context, state)
        if (state.tradeStartTs > 0L) AlarmScheduler.scheduleTimeStops(context, state.tradeStartTs)
    }
}
