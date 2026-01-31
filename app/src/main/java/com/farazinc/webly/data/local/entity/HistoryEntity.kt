package com.farazinc.webly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farazinc.webly.data.model.HistoryEntry

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long,
    val visitCount: Int = 1
) {
    fun toHistoryEntry(): HistoryEntry {
        return HistoryEntry(
            id = id,
            title = title,
            url = url,
            visitedAt = visitedAt,
            visitCount = visitCount
        )
    }
    
    companion object {
        fun fromHistoryEntry(entry: HistoryEntry): HistoryEntity {
            return HistoryEntity(
                id = entry.id,
                title = entry.title,
                url = entry.url,
                visitedAt = entry.visitedAt,
                visitCount = entry.visitCount
            )
        }
    }
}
