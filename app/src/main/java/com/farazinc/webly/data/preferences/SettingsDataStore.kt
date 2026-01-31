package com.farazinc.webly.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    
    companion object {
        private val KEY_JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        private val KEY_AD_BLOCKING_ENABLED = booleanPreferencesKey("ad_blocking_enabled")
        private val KEY_IMAGES_ENABLED = booleanPreferencesKey("images_enabled")
        private val KEY_COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        private val KEY_THIRD_PARTY_COOKIES = booleanPreferencesKey("third_party_cookies")
        private val KEY_DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        private val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
        private val KEY_HOME_PAGE = stringPreferencesKey("home_page")
        private val KEY_DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        
        const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="
        const val DEFAULT_HOME_PAGE = "https://www.google.com"
    }
    
    val isJavaScriptEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_JAVASCRIPT_ENABLED] ?: true
    }
    
    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_JAVASCRIPT_ENABLED] = enabled
        }
    }
    
    val isAdBlockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AD_BLOCKING_ENABLED] ?: true
    }
    
    suspend fun setAdBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AD_BLOCKING_ENABLED] = enabled
        }
    }
    
    val areImagesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IMAGES_ENABLED] ?: true
    }
    
    suspend fun setImagesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IMAGES_ENABLED] = enabled
        }
    }
    
    val areCookiesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_COOKIES_ENABLED] ?: true
    }
    
    suspend fun setCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COOKIES_ENABLED] = enabled
        }
    }
    
    val blockThirdPartyCookies: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_THIRD_PARTY_COOKIES] ?: true
    }
    
    suspend fun setBlockThirdPartyCookies(block: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THIRD_PARTY_COOKIES] = block
        }
    }
    
    val sendDoNotTrack: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DO_NOT_TRACK] ?: true
    }
    
    suspend fun setSendDoNotTrack(send: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DO_NOT_TRACK] = send
        }
    }
    
    val searchEngine: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SEARCH_ENGINE] ?: DEFAULT_SEARCH_ENGINE
    }
    
    suspend fun setSearchEngine(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEARCH_ENGINE] = url
        }
    }
    
    val homePage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_HOME_PAGE] ?: DEFAULT_HOME_PAGE
    }
    
    suspend fun setHomePage(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_PAGE] = url
        }
    }
    
    val isDesktopMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DESKTOP_MODE] ?: false
    }
    
    suspend fun setDesktopMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DESKTOP_MODE] = enabled
        }
    }
}
