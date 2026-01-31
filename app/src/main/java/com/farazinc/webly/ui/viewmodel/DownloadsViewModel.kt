package com.farazinc.webly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.data.model.DownloadItem
import com.farazinc.webly.domain.DownloadManagerWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val downloadManager = DownloadManagerWrapper(application)
    
    private val _refreshTrigger = MutableStateFlow(0L)
    
    val activeDownloads: StateFlow<List<DownloadItem>> = _refreshTrigger
        .flatMapLatest {
            flow {
                while (true) {
                    emit(downloadManager.getActiveDownloads())
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val completedDownloads: StateFlow<List<DownloadItem>> = _refreshTrigger
        .flatMapLatest {
            flow {
                emit(downloadManager.getCompletedDownloads())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    init {
        refresh()
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            downloadManager.removeDownload(downloadId)
            refresh()
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            val completed = downloadManager.getCompletedDownloads()
            completed.forEach { download ->
                downloadManager.removeDownload(download.id)
            }
            refresh()
        }
    }

    fun startDownload(url: String, fileName: String, mimeType: String? = null) {
        viewModelScope.launch {
            downloadManager.startDownload(url, fileName, mimeType)
            refresh()
        }
    }
}
