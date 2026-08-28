package com.example.speedlab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM speed_test_history ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HistoryEntity): Long

    @Delete
    suspend fun delete(record: HistoryEntity)

    @Query("DELETE FROM speed_test_history")
    suspend fun clear()
}
