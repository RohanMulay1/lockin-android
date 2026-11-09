package com.rohan.lockin.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rohan.lockin.MainActivity
import com.rohan.lockin.R
import com.rohan.lockin.domain.AppState
import com.rohan.lockin.domain.SmokeMath
import com.rohan.lockin.domain.rupees
import com.rohan.lockin.haptics.Haptics

object Notifications {

    // Channel settings are immutable once created, so louder channels get new ids.
    const val CH_AUDIT = "audit_v2"
    const val CH_STATUS = "status"
    const val CH_ALERTS = "alerts_v2"
    private val LEGACY_CHANNELS = listOf("audit", "alerts")

    const val ID_AUDIT = 1001
    const val ID_STATUS = 1002
    const val ID_LOCK = 1003
    const val ID_COOLDOWN = 1004
    const val ID_TIMESTOP = 1005
    const val ID_WINDOW = 1006

    const val EXTRA_OPEN_AUDIT = "open_audit"

    private const val TAG = "LockInNotify"
    private const val COLOR_ALERT = 0xFFEF4444.toInt()
    private const val COLOR_AUDIT = 0xFFF59E0B.toInt()
    private val ALARM_VIBE = longArrayOf(0, 900, 250, 900, 250, 900, 250, 1_400)

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        LEGACY_CHANNELS.forEach { nm.deleteNotificationChannel(it) }
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val audit = NotificationChannel(CH_AUDIT, "Hourly audits (loud)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Hourly tilt & execution audit during market hours"
            enableVibration(true)
            vibrationPattern = ALARM_VIBE
            setSound(alarmSound, alarmAttrs)
        }
        val status = NotificationChannel(CH_STATUS, "Fortress status", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Persistent smoke-free timer and fortress badge"
            setShowBadge(false)
        }
        val alerts = NotificationChannel(CH_ALERTS, "Risk alarms (loud)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Lockouts, cooldowns, time stops and the 10:30 no-trade window"
            enableVibration(true)
            vibrationPattern = ALARM_VIBE
            setSound(alarmSound, alarmAttrs)
        }
        nm.createNotificationChannels(listOf(audit, status, alerts))
    }

    fun postAudit(context: Context) {
        val text = "1 lot only? SL order placed? Calm, no revenge? Tap to answer the audit."
        val n = NotificationCompat.Builder(context, CH_AUDIT)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("⏰ Hourly Tilt & Execution Audit")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(COLOR_AUDIT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(ALARM_VIBE)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openAudit = true, requestCode = ID_AUDIT))
            .build()
        notify(context, ID_AUDIT, n)
        Haptics.alarm(context)
    }

    fun postStatus(context: Context, s: AppState) {
        val days = SmokeMath.elapsed(s.quitTs, System.currentTimeMillis()).days
        val n = NotificationCompat.Builder(context, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Smoke-free · Day $days")
            .setContentText("${rupees(s.fortressAmount)} Fortress Safe")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(s.quitTs > 0L)
            .setWhen(s.quitTs)
            .setShowWhen(s.quitTs > 0L)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp(context, openAudit = false, requestCode = ID_STATUS))
            .build()
        notify(context, ID_STATUS, n)
    }

    /** Loud, alarm-style alert: alarm sound on the alarm stream plus ~6 s of full-strength vibration. */
    fun postAlert(context: Context, id: Int, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(COLOR_ALERT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(ALARM_VIBE)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openAudit = false, requestCode = id))
            .build()
        notify(context, id, n)
        Haptics.alarm(context)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context, openAudit: Boolean, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_AUDIT, openAudit)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission revoked while posting $id", e)
        }
    }
}
