package com.farazinc.webly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farazinc.webly.data.model.Bookmark

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long,
    val favicon: String? = null
) {
    fun toBookmark(): Bookmark {
        return Bookmark(
            id = id,
            title = title,
            url = url,
            createdAt = createdAt,
            favicon = favicon
        )
    }
    
    companion object {
        fun fromBookmark(bookmark: Bookmark): BookmarkEntity {
            return BookmarkEntity(
                id = bookmark.id,
                title = bookmark.title,
                url = bookmark.url,
                createdAt = bookmark.createdAt,
                favicon = bookmark.favicon
            )
        }
    }
}
