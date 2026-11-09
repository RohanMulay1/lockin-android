package com.rohan.lockin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_log")
data class DailyLogEntity(
    @PrimaryKey val epochDay: Long,
    val pnl: Int,
    val trades: Int,
    val violations: Int,
    val lockouts: Int,
    val cigarettes: Int,
    val clean: Boolean,
)

@Entity(tableName = "trade")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val entryTs: Long,
    val exitTs: Long,
    val pnl: Int,
    val holdMinutes: Int,
)

@Entity(tableName = "smoke_event")
data class SmokeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,
)
