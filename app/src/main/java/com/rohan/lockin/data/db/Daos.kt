package com.rohan.lockin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Upsert
    suspend fun upsert(log: DailyLogEntity)

    @Query("SELECT * FROM daily_log ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<DailyLogEntity>>
}

@Dao
interface TradeDao {
    @Insert
    suspend fun insert(trade: TradeEntity)

    @Query("SELECT * FROM trade ORDER BY exitTs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TradeEntity>>
}

@Dao
interface SmokeEventDao {
    @Insert
    suspend fun insert(event: SmokeEventEntity)
}
