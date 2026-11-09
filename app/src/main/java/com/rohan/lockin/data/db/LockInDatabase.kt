package com.rohan.lockin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyLogEntity::class, TradeEntity::class, SmokeEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LockInDatabase : RoomDatabase() {
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun tradeDao(): TradeDao
    abstract fun smokeEventDao(): SmokeEventDao

    companion object {
        fun build(context: Context): LockInDatabase =
            Room.databaseBuilder(context.applicationContext, LockInDatabase::class.java, "lockin.db").build()
    }
}
