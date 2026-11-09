package com.rohan.lockin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rohan.lockin.domain.AppState
import com.rohan.lockin.domain.DEFAULT_FORTRESS
import com.rohan.lockin.domain.DEFAULT_NIFTY_LOT
import com.rohan.lockin.domain.DEFAULT_SENSEX_LOT
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.lockInDataStore: DataStore<Preferences> by preferencesDataStore(name = "lockin")

/** Persists [AppState] in DataStore Preferences so nothing resets when the app is killed. */
class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.lockInDataStore

    val state: Flow<AppState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { read(it) }

    /** Atomically reads, transforms and writes the state; returns the new state. */
    suspend fun update(transform: (AppState) -> AppState): AppState {
        var result = AppState()
        dataStore.edit { prefs ->
            val next = transform(read(prefs))
            write(prefs, next)
            result = next
        }
        return result
    }

    private object Keys {
        val quitTs = longPreferencesKey("quit_ts")
        val fortressAmount = intPreferencesKey("fortress_amount")
        val niftyLot = intPreferencesKey("nifty_lot")
        val sensexLot = intPreferencesKey("sensex_lot")
        val lockUntil = longPreferencesKey("lock_until")
        val lockReason = stringPreferencesKey("lock_reason")
        val cooldownUntil = longPreferencesKey("cooldown_until")
        val sessionClosed = booleanPreferencesKey("session_closed")
        val sessionDay = longPreferencesKey("session_day")
        val sessionPnl = intPreferencesKey("session_pnl")
        val peakPnl = intPreferencesKey("peak_pnl")
        val tradesToday = intPreferencesKey("trades_today")
        val consecutiveLosses = intPreferencesKey("consecutive_losses")
        val violations = intPreferencesKey("violations")
        val lockouts = intPreferencesKey("lockouts")
        val checklist = stringSetPreferencesKey("checklist")
        val tradeStartTs = longPreferencesKey("trade_start_ts")
        val readinessDay = longPreferencesKey("readiness_day")
        val readinessCleared = booleanPreferencesKey("readiness_cleared")
        val holidayDay = longPreferencesKey("holiday_day")
        val cleanDayStreak = intPreferencesKey("clean_day_streak")
        val cigarettesToday = intPreferencesKey("cigarettes_today")
    }

    private fun read(p: Preferences) = AppState(
        quitTs = p[Keys.quitTs] ?: 0L,
        fortressAmount = p[Keys.fortressAmount] ?: DEFAULT_FORTRESS,
        niftyLot = p[Keys.niftyLot] ?: DEFAULT_NIFTY_LOT,
        sensexLot = p[Keys.sensexLot] ?: DEFAULT_SENSEX_LOT,
        lockUntil = p[Keys.lockUntil] ?: 0L,
        lockReason = p[Keys.lockReason] ?: "",
        cooldownUntil = p[Keys.cooldownUntil] ?: 0L,
        sessionClosed = p[Keys.sessionClosed] ?: false,
        sessionDay = p[Keys.sessionDay] ?: 0L,
        sessionPnl = p[Keys.sessionPnl] ?: 0,
        peakPnl = p[Keys.peakPnl] ?: 0,
        tradesToday = p[Keys.tradesToday] ?: 0,
        consecutiveLosses = p[Keys.consecutiveLosses] ?: 0,
        violations = p[Keys.violations] ?: 0,
        lockouts = p[Keys.lockouts] ?: 0,
        checklist = p[Keys.checklist] ?: emptySet(),
        tradeStartTs = p[Keys.tradeStartTs] ?: 0L,
        readinessDay = p[Keys.readinessDay] ?: -1L,
        readinessCleared = p[Keys.readinessCleared] ?: false,
        holidayDay = p[Keys.holidayDay] ?: -1L,
        cleanDayStreak = p[Keys.cleanDayStreak] ?: 0,
        cigarettesToday = p[Keys.cigarettesToday] ?: 0,
    )

    private fun write(p: MutablePreferences, s: AppState) {
        p[Keys.quitTs] = s.quitTs
        p[Keys.fortressAmount] = s.fortressAmount
        p[Keys.niftyLot] = s.niftyLot
        p[Keys.sensexLot] = s.sensexLot
        p[Keys.lockUntil] = s.lockUntil
        p[Keys.lockReason] = s.lockReason
        p[Keys.cooldownUntil] = s.cooldownUntil
        p[Keys.sessionClosed] = s.sessionClosed
        p[Keys.sessionDay] = s.sessionDay
        p[Keys.sessionPnl] = s.sessionPnl
        p[Keys.peakPnl] = s.peakPnl
        p[Keys.tradesToday] = s.tradesToday
        p[Keys.consecutiveLosses] = s.consecutiveLosses
        p[Keys.violations] = s.violations
        p[Keys.lockouts] = s.lockouts
        p[Keys.checklist] = s.checklist
        p[Keys.tradeStartTs] = s.tradeStartTs
        p[Keys.readinessDay] = s.readinessDay
        p[Keys.readinessCleared] = s.readinessCleared
        p[Keys.holidayDay] = s.holidayDay
        p[Keys.cleanDayStreak] = s.cleanDayStreak
        p[Keys.cigarettesToday] = s.cigarettesToday
    }
}
