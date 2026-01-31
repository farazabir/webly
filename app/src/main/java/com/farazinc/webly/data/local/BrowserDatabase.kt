package com.farazinc.webly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.farazinc.webly.data.local.dao.BookmarkDao
import com.farazinc.webly.data.local.dao.HistoryDao
import com.farazinc.webly.data.local.entity.BookmarkEntity
import com.farazinc.webly.data.local.entity.HistoryEntity

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null
        
        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "webly_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
