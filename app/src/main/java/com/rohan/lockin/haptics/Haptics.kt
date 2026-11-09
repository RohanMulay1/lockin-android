package com.rohan.lockin.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {

    private val HEAVY_PATTERN = longArrayOf(0, 600, 200, 600, 200, 900)

    /** ~6 s of full-strength pulses, played on the alarm channel so it isn't muted with touch haptics. */
    private val ALARM_TIMINGS = longArrayOf(0, 900, 250, 900, 250, 900, 250, 900, 250, 1_400)
    private val ALARM_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)

    fun tick(context: Context) = vibrate(context, VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))

    fun pulse(context: Context) = vibrate(context, VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))

    fun heavy(context: Context) = vibrate(context, VibrationEffect.createWaveform(HEAVY_PATTERN, -1))

    fun alarm(context: Context) {
        val vibrator = vibrator(context)?.takeIf { it.hasVibrator() } ?: return
        val effect = VibrationEffect.createWaveform(ALARM_TIMINGS, ALARM_AMPLITUDES, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attrs)
        }
    }

    private fun vibrate(context: Context, effect: VibrationEffect) {
        vibrator(context)?.takeIf { it.hasVibrator() }?.vibrate(effect)
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
