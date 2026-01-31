package com.farazinc.webly.domain

import android.os.Bundle
import android.webkit.WebView
import com.farazinc.webly.data.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TabManager(
    private val maxTabs: Int = 10
) {
    
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    
    private val webViewCache = mutableMapOf<String, WebView>()
    
    init {
        createTab()
    }

    fun createTab(url: String = "", switchToTab: Boolean = true): String? {
        if (_tabs.value.size >= maxTabs) {
            return null
        }
        
        val newTab = Tab.createNew().copy(url = url)
        
        _tabs.value = _tabs.value.map { it.copy(isActive = false) } + newTab
        
        if (switchToTab) {
            _activeTabId.value = newTab.id
        }
        
        return newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabIndex = currentTabs.indexOfFirst { it.id == tabId }
        
        if (tabIndex == -1) return
        
        webViewCache.remove(tabId)
        
        val newTabs = currentTabs.filterNot { it.id == tabId }
        _tabs.value = newTabs
        
        if (_activeTabId.value == tabId && newTabs.isNotEmpty()) {
            val newActiveIndex = if (tabIndex > 0) tabIndex - 1 else 0
            switchToTab(newTabs[newActiveIndex].id)
        } else if (newTabs.isEmpty()) {
            createTab()
        }
    }

    fun switchToTab(tabId: String) {
        _tabs.value = _tabs.value.map { tab ->
            tab.copy(isActive = tab.id == tabId)
        }
        _activeTabId.value = tabId
    }

    fun getActiveTab(): Tab? {
        return _tabs.value.firstOrNull { it.isActive }
    }

    fun getTab(tabId: String): Tab? {
        return _tabs.value.firstOrNull { it.id == tabId }
    }

    fun updateTab(tabId: String, update: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) update(tab) else tab
        }
    }

    fun saveTabState(tabId: String, webView: WebView) {
        val bundle = Bundle()
        webView.saveState(bundle)
        
        updateTab(tabId) { tab ->
            tab.copy(webViewState = bundle)
        }
    }

    fun restoreTabState(tabId: String, webView: WebView): Boolean {
        val tab = getTab(tabId) ?: return false
        val state = tab.webViewState ?: return false
        
        return try {
            webView.restoreState(state)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun registerWebView(tabId: String, webView: WebView) {
        webViewCache[tabId] = webView
    }

    fun getWebView(tabId: String): WebView? {
        return webViewCache[tabId]
    }

    fun removeWebView(tabId: String) {
        webViewCache.remove(tabId)
    }

    fun getTabCount(): Int = _tabs.value.size

    fun closeOtherTabs(keepTabId: String) {
        _tabs.value.forEach { tab ->
            if (tab.id != keepTabId) {
                webViewCache.remove(tab.id)
            }
        }
        
        _tabs.value = _tabs.value.filter { it.id == keepTabId }
        switchToTab(keepTabId)
    }

    fun closeAllTabs() {
        webViewCache.clear()
        _tabs.value = emptyList()
        createTab()
    }
}
