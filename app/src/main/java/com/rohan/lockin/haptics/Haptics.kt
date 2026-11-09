package com.rohan.lockin.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {

    private val HEAVY_PATTERN = longArrayOf(0, 600, 200, 600, 200, 900)

    fun tick(context: Context) = vibrate(context, VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))

    fun pulse(context: Context) = vibrate(context, VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))

    fun heavy(context: Context) = vibrate(context, VibrationEffect.createWaveform(HEAVY_PATTERN, -1))

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
