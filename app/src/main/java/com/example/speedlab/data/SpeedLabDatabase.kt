package com.example.speedlab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class SpeedLabDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: SpeedLabDatabase? = null

        fun get(context: Context): SpeedLabDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SpeedLabDatabase::class.java,
                "speedlab.db",
            ).build().also { instance = it }
        }
    }
}
