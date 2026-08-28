package com.example.speedlab.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    val records: Flow<List<HistoryEntity>> = dao.observeAll()

    suspend fun add(record: HistoryEntity): Long = dao.insert(record)
    suspend fun delete(record: HistoryEntity) = dao.delete(record)
    suspend fun clear() = dao.clear()
}
