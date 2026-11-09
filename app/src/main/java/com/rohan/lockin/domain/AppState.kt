package com.rohan.lockin.domain

const val ONE_R = 2_000
const val MARGIN_CAP = 10_000
const val TRADE_CAP = 6
const val TIME_STOP_MIN = 30
const val TIME_STOP_WARN_MIN = 25
const val LOCKOUT_MIN = 60
const val COOLDOWN_MIN = 15
const val GIVE_BACK_MIN_PEAK = 1_000
const val DEFAULT_FORTRESS = 400_000
const val DEFAULT_NIFTY_LOT = 65
const val DEFAULT_SENSEX_LOT = 20

/** Everything the app persists in DataStore. Session fields reset on each new day. */
data class AppState(
    val quitTs: Long = 0L,
    val fortressAmount: Int = DEFAULT_FORTRESS,
    val niftyLot: Int = DEFAULT_NIFTY_LOT,
    val sensexLot: Int = DEFAULT_SENSEX_LOT,
    val lockUntil: Long = 0L,
    val lockReason: String = "",
    val cooldownUntil: Long = 0L,
    val sessionClosed: Boolean = false,
    val sessionDay: Long = 0L,
    val sessionPnl: Int = 0,
    val peakPnl: Int = 0,
    val tradesToday: Int = 0,
    val consecutiveLosses: Int = 0,
    val violations: Int = 0,
    val lockouts: Int = 0,
    val checklist: Set<String> = emptySet(),
    val tradeStartTs: Long = 0L,
    val readinessDay: Long = -1L,
    val readinessCleared: Boolean = false,
    val holidayDay: Long = -1L,
    val cleanDayStreak: Int = 0,
    val cigarettesToday: Int = 0,
    val brokerPassUntil: Long = 0L,
    val exitPassesToday: Int = 0,
)

fun AppState.snapshot() = SessionSnapshot(
    pnl = sessionPnl,
    peakPnl = peakPnl,
    tradesToday = tradesToday,
    consecutiveLosses = consecutiveLosses,
)

data class ChecklistItem(val id: String, val label: String)

val PREFLIGHT_CHECKLIST = listOf(
    ChecklistItem("htf", "Higher timeframe trend alignment (15m/1h structure)"),
    ChecklistItem("mentor", "Mentor setup confluence (zero impulsive guessing)"),
    ChecklistItem("invalidation", "Defined invalidation (SL set before entry)"),
    ChecklistItem("one_lot", "Strict 1 lot (no lot stacking / averaging)"),
    ChecklistItem("calm", "Heart rate & physiological calm (no revenge impulse)"),
)
