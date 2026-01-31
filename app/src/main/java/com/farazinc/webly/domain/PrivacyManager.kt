package com.farazinc.webly.domain

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyManager(private val context: Context) {
    
    private val cookieManager = CookieManager.getInstance()

    suspend fun clearCookies() = withContext(Dispatchers.Main) {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    suspend fun clearCache() = withContext(Dispatchers.Main) {
        WebView(context).apply {
            clearCache(true)
            destroy()
        }
    }

    suspend fun clearFormData() = withContext(Dispatchers.Main) {
        WebView(context).apply {
            clearFormData()
            destroy()
        }
    }

    suspend fun clearWebStorage() = withContext(Dispatchers.Main) {
        WebStorage.getInstance().deleteAllData()
    }

    suspend fun clearAllData() {
        clearCookies()
        clearCache()
        clearFormData()
        clearWebStorage()
    }

    fun setCookiesEnabled(enabled: Boolean) {
        cookieManager.setAcceptCookie(enabled)
    }

    fun setThirdPartyCookiesEnabled(webView: WebView, enabled: Boolean) {
        cookieManager.setAcceptThirdPartyCookies(webView, enabled)
    }

    fun areCookiesEnabled(): Boolean {
        return cookieManager.acceptCookie()
    }

    fun getCookies(url: String): String? {
        return cookieManager.getCookie(url)
    }

    suspend fun removeCookiesForDomain(domain: String) = withContext(Dispatchers.Main) {
        val cookies = cookieManager.getCookie(domain)
        cookies?.split(";")?.forEach { cookie ->
            val cookieName = cookie.substringBefore("=").trim()
            cookieManager.setCookie(domain, "$cookieName=; Max-Age=0")
        }
        cookieManager.flush()
    }
}
