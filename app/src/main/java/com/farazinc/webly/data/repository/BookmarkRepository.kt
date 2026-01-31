package com.farazinc.webly.data.repository

import com.farazinc.webly.data.local.dao.BookmarkDao
import com.farazinc.webly.data.local.entity.BookmarkEntity
import com.farazinc.webly.data.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    
    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toBookmark() }
        }
    }
    
    suspend fun getBookmarkById(id: Long): Bookmark? {
        return bookmarkDao.getBookmarkById(id)?.toBookmark()
    }
    
    suspend fun addBookmark(bookmark: Bookmark): Long {
        return bookmarkDao.insert(BookmarkEntity.fromBookmark(bookmark))
    }
    
    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.update(BookmarkEntity.fromBookmark(bookmark))
    }
    
    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.delete(BookmarkEntity.fromBookmark(bookmark))
    }
    
    suspend fun deleteBookmarkById(id: Long) {
        bookmarkDao.deleteById(id)
    }
    
    suspend fun deleteAllBookmarks() {
        bookmarkDao.deleteAll()
    }
    
    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }
    
    suspend fun getBookmarkByUrl(url: String): Bookmark? {
        return bookmarkDao.getBookmarkByUrl(url)?.toBookmark()
    }
    
    suspend fun getBookmarkCount(): Int {
        return bookmarkDao.getBookmarkCount()
    }

    suspend fun exportToJson(): String {
        val bookmarks = bookmarkDao.getAllBookmarks()
        return "[]"
    }

    suspend fun importFromJson(json: String) {
    }
}
