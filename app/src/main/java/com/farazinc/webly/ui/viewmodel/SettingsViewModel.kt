package com.farazinc.webly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.data.local.BrowserDatabase
import com.farazinc.webly.data.preferences.SettingsDataStore
import com.farazinc.webly.domain.PrivacyManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsDataStore = SettingsDataStore(application)
    private val privacyManager = PrivacyManager(application)
    private val database = BrowserDatabase.getDatabase(application)
    
    val isJavaScriptEnabled: StateFlow<Boolean> = settingsDataStore.isJavaScriptEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val isAdBlockingEnabled: StateFlow<Boolean> = settingsDataStore.isAdBlockingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val areImagesEnabled: StateFlow<Boolean> = settingsDataStore.areImagesEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val areCookiesEnabled: StateFlow<Boolean> = settingsDataStore.areCookiesEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val blockThirdPartyCookies: StateFlow<Boolean> = settingsDataStore.blockThirdPartyCookies
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val sendDoNotTrack: StateFlow<Boolean> = settingsDataStore.sendDoNotTrack
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val searchEngine: StateFlow<String> = settingsDataStore.searchEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsDataStore.DEFAULT_SEARCH_ENGINE)
    
    val homePage: StateFlow<String> = settingsDataStore.homePage
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsDataStore.DEFAULT_HOME_PAGE)
    
    val isDesktopMode: StateFlow<Boolean> = settingsDataStore.isDesktopMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setJavaScriptEnabled(enabled)
        }
    }
    
    fun setAdBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAdBlockingEnabled(enabled)
        }
    }
    
    fun setImagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setImagesEnabled(enabled)
        }
    }
    
    fun setCookiesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCookiesEnabled(enabled)
            privacyManager.setCookiesEnabled(enabled)
        }
    }
    
    fun setBlockThirdPartyCookies(block: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBlockThirdPartyCookies(block)
        }
    }
    
    fun setSendDoNotTrack(send: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSendDoNotTrack(send)
        }
    }
    
    fun setSearchEngine(url: String) {
        viewModelScope.launch {
            settingsDataStore.setSearchEngine(url)
        }
    }
    
    fun setHomePage(url: String) {
        viewModelScope.launch {
            val processedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }
            settingsDataStore.setHomePage(processedUrl)
        }
    }
    
    fun setDesktopMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDesktopMode(enabled)
        }
    }

    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearHistory: Boolean) {
        viewModelScope.launch {
            if (clearCookies) {
                privacyManager.clearCookies()
            }
            if (clearCache) {
                privacyManager.clearCache()
            }
            if (clearHistory) {
                database.historyDao().deleteAll()
            }
        }
    }
}
