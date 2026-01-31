package com.farazinc.webly.domain

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.farazinc.webly.data.model.DownloadItem
import com.farazinc.webly.data.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DownloadManagerWrapper(private val context: Context) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun startDownload(
        url: String,
        fileName: String,
        mimeType: String? = null,
        userAgent: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            
            mimeType?.let { setMimeType(it) }
            userAgent?.let { addRequestHeader("User-Agent", it) }
        }
        
        downloadManager.enqueue(request)
    }

    suspend fun getAllDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query()
        val cursor = downloadManager.query(query)
        
        val downloads = mutableListOf<DownloadItem>()
        cursor?.use {
            while (it.moveToNext()) {
                downloads.add(getDownloadFromCursor(it))
            }
        }
        downloads
    }

    suspend fun getActiveDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query().apply {
            setFilterByStatus(
                DownloadManager.STATUS_RUNNING or 
                DownloadManager.STATUS_PENDING or 
                DownloadManager.STATUS_PAUSED
            )
        }
        val cursor = downloadManager.query(query)
        
        val downloads = mutableListOf<DownloadItem>()
        cursor?.use {
            while (it.moveToNext()) {
                downloads.add(getDownloadFromCursor(it))
            }
        }
        downloads
    }

    suspend fun getCompletedDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query().apply {
            setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        }
        val cursor = downloadManager.query(query)
        
        val downloads = mutableListOf<DownloadItem>()
        cursor?.use {
            while (it.moveToNext()) {
                downloads.add(getDownloadFromCursor(it))
            }
        }
        downloads
    }

    suspend fun getDownload(downloadId: Long): DownloadItem? = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        cursor?.use {
            if (it.moveToFirst()) {
                return@withContext getDownloadFromCursor(it)
            }
        }
        null
    }

    suspend fun removeDownload(downloadId: Long): Boolean = withContext(Dispatchers.IO) {
        downloadManager.remove(downloadId) > 0
    }

    fun openDownload(downloadId: Long): Uri? {
        return downloadManager.getUriForDownloadedFile(downloadId)
    }

    fun observeDownload(downloadId: Long): Flow<DownloadItem?> = flow {
        while (true) {
            val download = getDownload(downloadId)
            emit(download)
            
            if (download?.isComplete == true || download?.isFailed == true) {
                break
            }
            
            kotlinx.coroutines.delay(500)
        }
    }.flowOn(Dispatchers.IO)

    private fun getDownloadFromCursor(cursor: Cursor): DownloadItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
        val url = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))
        val fileName = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
        val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
        val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
        
        return DownloadItem(
            id = id,
            url = url,
            fileName = fileName ?: "Unknown",
            mimeType = mimeType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = convertStatus(status),
            startTime = lastModified,
            filePath = localUri,
            reason = if (status == DownloadManager.STATUS_FAILED) "Error code: $reason" else null
        )
    }

    private fun convertStatus(status: Int): DownloadStatus {
        return when (status) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
            DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> DownloadStatus.PENDING
        }
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
        
        fun getFileExtension(fileName: String): String {
            return fileName.substringAfterLast('.', "")
        }
        
        fun getMimeTypeIcon(mimeType: String?): String {
            return when {
                mimeType == null -> "📄"
                mimeType.startsWith("image/") -> "🖼️"
                mimeType.startsWith("video/") -> "🎬"
                mimeType.startsWith("audio/") -> "🎵"
                mimeType.startsWith("application/pdf") -> "📕"
                mimeType.startsWith("application/zip") -> "📦"
                mimeType.startsWith("text/") -> "📝"
                else -> "📄"
            }
        }
    }
}
