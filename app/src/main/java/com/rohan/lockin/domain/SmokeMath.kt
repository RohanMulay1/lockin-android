package com.rohan.lockin.domain

data class Elapsed(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

data class Milestone(val hours: Long, val title: String, val body: String)

object SmokeMath {

    private const val HOUR_MS = 3_600_000L

    val MILESTONES = listOf(
        Milestone(24, "24h · Blood pressure reset", "Heart rate and blood pressure settle; carbon monoxide drops."),
        Milestone(48, "48h · Nicotine eliminated", "Nicotine is largely cleared; taste and smell start returning."),
        Milestone(72, "72h · Dopamine baseline", "Withdrawal peaks and starts easing; reward system recalibrating."),
        Milestone(168, "1 week · Habit loop cracked", "Cravings get shorter and less frequent."),
        Milestone(336, "2 weeks · Circulation up", "Circulation and lung function begin improving."),
    )

    fun elapsed(fromMs: Long, nowMs: Long): Elapsed {
        val total = if (fromMs <= 0L) 0L else ((nowMs - fromMs) / 1_000L).coerceAtLeast(0L)
        return Elapsed(
            days = total / 86_400L,
            hours = (total % 86_400L) / 3_600L,
            minutes = (total % 3_600L) / 60L,
            seconds = total % 60L,
        )
    }

    fun progress(m: Milestone, fromMs: Long, nowMs: Long): Float {
        if (fromMs <= 0L) return 0f
        val ratio = (nowMs - fromMs).toDouble() / (m.hours * HOUR_MS)
        return ratio.coerceIn(0.0, 1.0).toFloat()
    }

    fun isUnlocked(m: Milestone, fromMs: Long, nowMs: Long): Boolean = progress(m, fromMs, nowMs) >= 1f
}
