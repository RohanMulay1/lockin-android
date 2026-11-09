package com.rohan.lockin.domain

data class RiskLimits(
    val oneR: Int = ONE_R,
    val tradeCap: Int = TRADE_CAP,
    val giveBackMinPeak: Int = GIVE_BACK_MIN_PEAK,
    val giveBackFraction: Double = 0.5,
    val lossStreakForCooldown: Int = 2,
    val cooldownMinutes: Int = COOLDOWN_MIN,
    val lockoutMinutes: Int = LOCKOUT_MIN,
)

data class SessionSnapshot(
    val pnl: Int,
    val peakPnl: Int,
    val tradesToday: Int,
    val consecutiveLosses: Int,
)

enum class LockReason(val headline: String, val detail: String) {
    MAX_DRAWDOWN("MAX DRAWDOWN REACHED (-1R)", "CLOSE BROKER IMMEDIATELY. WALK AWAY."),
    TRADE_CAP("TRADE CAP EXCEEDED", "Overtrading kills your edge. CLOSE BROKER. WALK AWAY."),
    GIVE_BACK("YOU GAVE BACK HALF THE DAY", "Protect the green. CLOSE BROKER. WALK AWAY."),
}

sealed interface Verdict {
    data object Clear : Verdict
    data object Cooldown : Verdict
    data class Lockout(val reason: LockReason) : Verdict
}

object RiskEngine {

    fun evaluate(s: SessionSnapshot, limits: RiskLimits = RiskLimits()): Verdict = when {
        s.pnl <= -limits.oneR -> Verdict.Lockout(LockReason.MAX_DRAWDOWN)
        s.tradesToday > limits.tradeCap -> Verdict.Lockout(LockReason.TRADE_CAP)
        isGiveBack(s, limits) -> Verdict.Lockout(LockReason.GIVE_BACK)
        s.consecutiveLosses >= limits.lossStreakForCooldown -> Verdict.Cooldown
        else -> Verdict.Clear
    }

    fun isGiveBack(s: SessionSnapshot, limits: RiskLimits = RiskLimits()): Boolean =
        s.peakPnl >= limits.giveBackMinPeak && s.pnl <= s.peakPnl * limits.giveBackFraction

    /** P&L level at which the give-back lockout fires, or null while the peak is too small to protect. */
    fun giveBackFloor(peakPnl: Int, limits: RiskLimits = RiskLimits()): Int? =
        if (peakPnl >= limits.giveBackMinPeak) (peakPnl * limits.giveBackFraction).toInt() else null

    /** Stop-loss distance in option points that keeps a full loss within 1R. */
    fun allowedSlPoints(oneR: Int, qty: Int): Double = if (qty <= 0) 0.0 else oneR.toDouble() / qty
}
