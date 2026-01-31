package com.farazinc.webly.data.local.dao

import androidx.room.*
import com.farazinc.webly.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?
    
    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 50")
    suspend fun searchHistory(query: String): List<HistoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long
    
    @Update
    suspend fun update(history: HistoryEntity)
    
    @Delete
    suspend fun delete(history: HistoryEntity)
    
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM history")
    suspend fun deleteAll()
    
    @Query("DELETE FROM history WHERE visitedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int

    @Transaction
    suspend fun insertOrUpdate(title: String, url: String) {
        val existing = getHistoryByUrl(url)
        if (existing != null) {
            update(existing.copy(
                title = title,
                visitedAt = System.currentTimeMillis(),
                visitCount = existing.visitCount + 1
            ))
        } else {
            insert(HistoryEntity(
                title = title,
                url = url,
                visitedAt = System.currentTimeMillis(),
                visitCount = 1
            ))
        }
    }
}
