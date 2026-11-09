package com.rohan.lockin.data

import com.rohan.lockin.data.db.DailyLogEntity
import com.rohan.lockin.data.db.LockInDatabase
import com.rohan.lockin.data.db.SmokeEventEntity
import com.rohan.lockin.data.db.TradeEntity
import com.rohan.lockin.domain.AppState
import com.rohan.lockin.domain.SessionRules
import kotlinx.coroutines.flow.Flow

/** Single source of truth: live state in DataStore, history in Room. */
class LockInRepository(
    private val store: SettingsStore,
    private val db: LockInDatabase,
) {
    val state: Flow<AppState> = store.state
    val dailyLogs: Flow<List<DailyLogEntity>> = db.dailyLogDao().observeAll()
    val recentTrades: Flow<List<TradeEntity>> = db.tradeDao().observeRecent(RECENT_TRADES)

    suspend fun update(transform: (AppState) -> AppState): AppState = store.update(transform)

    /** On the first call of a new day, archives the previous session to Room and resets session fields. */
    suspend fun ensureToday(today: Long) {
        var archived: AppState? = null
        store.update { s ->
            if (s.sessionDay == today) {
                s
            } else {
                if (s.sessionDay > 0L) archived = s
                SessionRules.rolledOver(s, today)
            }
        }
        archived?.let { db.dailyLogDao().upsert(it.toDailyLog()) }
    }

    suspend fun insertTrade(trade: TradeEntity) = db.tradeDao().insert(trade)

    suspend fun insertSmoke(ts: Long) = db.smokeEventDao().insert(SmokeEventEntity(ts = ts))

    private fun AppState.toDailyLog() = DailyLogEntity(
        epochDay = sessionDay,
        pnl = sessionPnl,
        trades = tradesToday,
        violations = violations,
        lockouts = lockouts,
        cigarettes = cigarettesToday,
        clean = violations == 0 && lockouts == 0,
    )

    private companion object {
        const val RECENT_TRADES = 100
    }
}
