package com.farazinc.webly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.data.local.BrowserDatabase
import com.farazinc.webly.data.model.Bookmark
import com.farazinc.webly.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = BrowserDatabase.getDatabase(application)
    private val repository = BookmarkRepository(database.bookmarkDao())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    private val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    
    private val allBookmarks: StateFlow<List<Bookmark>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val bookmarks: StateFlow<List<Bookmark>> = combine(
        allBookmarks,
        searchQuery,
        sortOrder
    ) { bookmarks, query, order ->
        var filtered = bookmarks
        
        if (query.isNotBlank()) {
            filtered = filtered.filter { bookmark ->
                bookmark.title.contains(query, ignoreCase = true) ||
                bookmark.url.contains(query, ignoreCase = true)
            }
        }
        
        when (order) {
            SortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.createdAt }
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    fun sortByName() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.NAME_ASC) {
            SortOrder.NAME_DESC
        } else {
            SortOrder.NAME_ASC
        }
    }

    fun sortByDate() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.DATE_ASC) {
            SortOrder.DATE_DESC
        } else {
            SortOrder.DATE_ASC
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            val bookmark = Bookmark(
                title = title,
                url = url,
                createdAt = System.currentTimeMillis()
            )
            repository.addBookmark(bookmark)
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun deleteAllBookmarks() {
        viewModelScope.launch {
            repository.deleteAllBookmarks()
        }
    }

    suspend fun getBookmarkCount(): Int {
        return repository.getBookmarkCount()
    }
}

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC
}
