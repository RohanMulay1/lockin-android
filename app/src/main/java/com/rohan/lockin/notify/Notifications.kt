package com.rohan.lockin.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

object Notifications {

    const val CH_AUDIT = "audit"
    const val CH_STATUS = "status"
    const val CH_ALERTS = "alerts"

    const val ID_AUDIT = 1001
    const val ID_STATUS = 1002
    const val ID_LOCK = 1003
    const val ID_COOLDOWN = 1004
    const val ID_TIMESTOP = 1005

    const val EXTRA_OPEN_AUDIT = "open_audit"

    private const val TAG = "LockInNotify"
    private val AUDIT_VIBE = longArrayOf(0, 400, 150, 400)
    private val ALERT_VIBE = longArrayOf(0, 700, 200, 700, 200, 900)

    fun createChannels(context: Context) {
        val audit = NotificationChannel(CH_AUDIT, "Hourly audits", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Hourly tilt & execution audit during market hours"
            enableVibration(true)
            vibrationPattern = AUDIT_VIBE
        }
        val status = NotificationChannel(CH_STATUS, "Fortress status", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Persistent smoke-free timer and fortress badge"
            setShowBadge(false)
        }
        val alerts = NotificationChannel(CH_ALERTS, "Risk alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Lockouts, cooldowns and time stops"
            enableVibration(true)
            vibrationPattern = ALERT_VIBE
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannels(listOf(audit, status, alerts))
    }

    fun postAudit(context: Context) {
        val n = NotificationCompat.Builder(context, CH_AUDIT)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Hourly Tilt & Execution Audit")
            .setContentText("Are you sticking to 1 Lot? Emotional status check. Tap to audit.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(AUDIT_VIBE)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openAudit = true, requestCode = ID_AUDIT))
            .build()
        notify(context, ID_AUDIT, n)
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

    fun postAlert(context: Context, id: Int, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(ALERT_VIBE)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openAudit = false, requestCode = id))
            .build()
        notify(context, id, n)
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
