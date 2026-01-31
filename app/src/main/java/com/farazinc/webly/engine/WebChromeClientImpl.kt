package com.farazinc.webly.engine

import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

class WebChromeClientImpl(
    private val callbacks: ChromeClientCallbacks
) : WebChromeClient() {
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callbacks.onProgressChanged(newProgress)
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        callbacks.onReceivedTitle(title ?: "")
    }
    
    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        callbacks.onReceivedIcon(icon)
    }
    
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return super.onJsAlert(view, url, message, result)
    }
    
    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return super.onJsConfirm(view, url, message, result)
    }
    
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null && android.util.Log.isLoggable("WebView", android.util.Log.DEBUG)) {
            android.util.Log.d(
                "WebView",
                "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
            )
        }
        return true
    }
    
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, false, false)
    }
    
    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.deny()
    }
}

interface ChromeClientCallbacks {
    fun onProgressChanged(progress: Int)
    fun onReceivedTitle(title: String)
    fun onReceivedIcon(icon: Bitmap?)
}
