package com.farazinc.webly.data.repository

import com.farazinc.webly.data.local.dao.HistoryDao
import com.farazinc.webly.data.local.entity.HistoryEntity
import com.farazinc.webly.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val historyDao: HistoryDao) {
    
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntry>> {
        return historyDao.getRecentHistory(limit).map { entities ->
            entities.map { it.toHistoryEntry() }
        }
    }
    
    suspend fun addHistory(title: String, url: String) {
        historyDao.insertOrUpdate(title, url)
    }
    
    suspend fun deleteHistory(entry: HistoryEntry) {
        historyDao.delete(HistoryEntity.fromHistoryEntry(entry))
    }
    
    suspend fun deleteHistoryById(id: Long) {
        historyDao.deleteById(id)
    }
    
    suspend fun deleteAllHistory() {
        historyDao.deleteAll()
    }
    
    suspend fun deleteOldHistory(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        historyDao.deleteOlderThan(cutoffTime)
    }
    
    suspend fun searchHistory(query: String): List<HistoryEntry> {
        return historyDao.searchHistory(query).map { it.toHistoryEntry() }
    }
    
    suspend fun getHistoryCount(): Int {
        return historyDao.getHistoryCount()
    }
}
