package com.farazinc.webly.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.data.local.BrowserDatabase
import com.farazinc.webly.data.repository.BookmarkRepository
import com.farazinc.webly.data.repository.HistoryRepository
import com.farazinc.webly.data.preferences.SettingsDataStore
import com.farazinc.webly.domain.TabManager
import com.farazinc.webly.engine.adblock.AdBlockEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = BrowserDatabase.getDatabase(application)
    val bookmarkRepository = BookmarkRepository(database.bookmarkDao())
    val historyRepository = HistoryRepository(database.historyDao())
    val settingsDataStore = SettingsDataStore(application)
    val adBlockEngine = AdBlockEngine(application)
    val tabManager = TabManager()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()
    
    private val _currentFavicon = MutableStateFlow<Bitmap?>(null)
    val currentFavicon: StateFlow<Bitmap?> = _currentFavicon.asStateFlow()
    
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()
    
    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()
    
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()
    
    private val _showTabSwitcher = MutableStateFlow(false)
    val showTabSwitcher: StateFlow<Boolean> = _showTabSwitcher.asStateFlow()
    
    private val _currentHtml = MutableStateFlow("")
    val currentHtml: StateFlow<String> = _currentHtml.asStateFlow()
    
    init {
        viewModelScope.launch {
            val result = adBlockEngine.loadFilters()
            println("Ad-blocking loaded: ${result.loadedRules} rules, ${result.stats.totalRules} total")
        }
        
        viewModelScope.launch {
            tabManager.activeTabId.collect { tabId ->
                tabId?.let { updateTabState(it) }
            }
        }
    }

    private fun updateTabState(tabId: String) {
        val tab = tabManager.getTab(tabId)
        tab?.let {
            _currentUrl.value = it.url
            _currentTitle.value = it.title
            _currentFavicon.value = it.favicon
            _canGoBack.value = it.canGoBack
            _canGoForward.value = it.canGoForward
            _isLoading.value = it.isLoading
            _progress.value = it.progress
            
            viewModelScope.launch {
                _isBookmarked.value = bookmarkRepository.isBookmarked(it.url)
            }
        }
    }

    fun loadUrl(url: String) {
        val processedUrl = processUrl(url)
        val activeTab = tabManager.getActiveTab()
        
        activeTab?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(url = processedUrl, isLoading = true)
            }
            _currentUrl.value = processedUrl
        }
    }

    private fun processUrl(input: String): String {
        val trimmed = input.trim()
        
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> {
                val searchEngine = SettingsDataStore.DEFAULT_SEARCH_ENGINE
                "$searchEngine${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
            }
        }
    }

    fun onPageStarted(url: String, favicon: Bitmap?) {
        _isLoading.value = true
        _progress.value = 0
        _currentUrl.value = url
        _currentFavicon.value = favicon
        
        val activeTab = tabManager.getActiveTab()
        activeTab?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(url = url, favicon = favicon, isLoading = true, progress = 0)
            }
        }
    }

    fun onPageFinished(url: String, title: String, html: String = "") {
        _isLoading.value = false
        _progress.value = 100
        _currentTitle.value = title
        _currentHtml.value = html
        
        val activeTab = tabManager.getActiveTab()
        activeTab?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(
                    url = url,
                    title = title,
                    isLoading = false,
                    progress = 100
                )
            }
            
            viewModelScope.launch {
                historyRepository.addHistory(title, url)
            }
            
            viewModelScope.launch {
                _isBookmarked.value = bookmarkRepository.isBookmarked(url)
            }
        }
    }

    fun onProgressChanged(progress: Int) {
        _progress.value = progress
        
        val activeTab = tabManager.getActiveTab()
        activeTab?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(progress = progress)
            }
        }
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
        
        val activeTab = tabManager.getActiveTab()
        activeTab?.let { tab ->
            tabManager.updateTab(tab.id) {
                it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val url = _currentUrl.value
            if (url.isNotEmpty()) {
                if (_isBookmarked.value) {
                    bookmarkRepository.getBookmarkByUrl(url)?.let {
                        bookmarkRepository.deleteBookmark(it)
                    }
                    _isBookmarked.value = false
                } else {
                    bookmarkRepository.addBookmark(
                        com.farazinc.webly.data.model.Bookmark(
                            title = _currentTitle.value.ifEmpty { url },
                            url = url
                        )
                    )
                    _isBookmarked.value = true
                }
            }
        }
    }

    fun createNewTab(url: String = "") {
        tabManager.createTab(url = url, switchToTab = true)
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }

    fun switchToTab(tabId: String) {
        tabManager.switchToTab(tabId)
        _showTabSwitcher.value = false
    }

    fun toggleTabSwitcher() {
        _showTabSwitcher.value = !_showTabSwitcher.value
    }

    fun goHome() {
        viewModelScope.launch {
            settingsDataStore.homePage.first().let { homePage ->
                loadUrl(homePage)
            }
        }
    }
}
