package com.example.speedlab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMillis: Double,
    val jitterMillis: Double,
    val peakMbps: Double,
    val serverName: String,
    val serverHost: String,
    val networkType: String,
    val testMode: String,
    val transferredBytes: Long,
    val durationMillis: Long,
    val completionStatus: String,
    val errorMessage: String? = null,
)
