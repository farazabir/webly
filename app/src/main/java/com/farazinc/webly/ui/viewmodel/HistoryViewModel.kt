package com.farazinc.webly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.data.local.BrowserDatabase
import com.farazinc.webly.data.model.HistoryEntry
import com.farazinc.webly.data.repository.HistoryRepository
import com.farazinc.webly.ui.screen.getDateHeader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = BrowserDatabase.getDatabase(application)
    private val repository = HistoryRepository(database.historyDao())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val allHistory: StateFlow<List<HistoryEntry>> = repository.getRecentHistory(500)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val history: StateFlow<List<HistoryEntry>> = combine(
        allHistory,
        searchQuery
    ) { history, query ->
        if (query.isBlank()) {
            history
        } else {
            history.filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                entry.url.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val groupedHistory: StateFlow<Map<String, List<HistoryEntry>>> = history.map { entries ->
        entries.groupBy { entry ->
            getDateHeader(entry.visitedAt)
        }.toSortedMap(compareBy {
            when (it) {
                "Today" -> 0
                "Yesterday" -> 1
                else -> 2
            }
        })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            repository.deleteHistory(entry)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }

    fun deleteOldHistory(days: Int = 30) {
        viewModelScope.launch {
            repository.deleteOldHistory(days)
        }
    }
}
