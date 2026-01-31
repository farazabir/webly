package com.farazinc.webly.data.local.dao

import androidx.room.*
import com.farazinc.webly.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    
    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): BookmarkEntity?
    
    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long
    
    @Update
    suspend fun update(bookmark: BookmarkEntity)
    
    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
    
    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarkCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean
}
