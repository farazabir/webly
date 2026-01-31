package com.farazinc.webly.data.model

data class DownloadItem(
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val startTime: Long,
    val endTime: Long? = null,
    val filePath: String? = null,
    val reason: String? = null
) {
    val progress: Int
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt()
        } else 0
    
    val isComplete: Boolean
        get() = status == DownloadStatus.SUCCESSFUL
    
    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED
    
    val isPaused: Boolean
        get() = status == DownloadStatus.PAUSED
    
    val isRunning: Boolean
        get() = status == DownloadStatus.RUNNING
}

enum class DownloadStatus(val value: Int) {
    PENDING(1),
    RUNNING(2),
    PAUSED(4),
    SUCCESSFUL(8),
    FAILED(16);
    
    companion object {
        fun fromValue(value: Int): DownloadStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}
