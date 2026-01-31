package com.farazinc.webly.engine

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebSettings
import android.webkit.WebView
import java.net.URLDecoder

object WebViewEngine {

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        enableJavaScript: Boolean = true,
        enableImages: Boolean = true,
        enableCache: Boolean = true
    ): WebView {
        val webView = WebView(context)
        
        webView.settings.apply {
            javaScriptEnabled = enableJavaScript
            javaScriptCanOpenWindowsAutomatically = false
            
            loadsImagesAutomatically = enableImages
            blockNetworkImage = !enableImages
            
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = if (enableCache) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
            
            domStorageEnabled = true
            databaseEnabled = true
            
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            
            useWideViewPort = true
            loadWithOverviewMode = true
            
            textZoom = 100
            minimumFontSize = 8
            
            mediaPlaybackRequiresUserGesture = false
            
            allowFileAccess = false
            allowContentAccess = true
            
            userAgentString = buildUserAgent(userAgentString)
            
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            
        }
        
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        
        webView.setDownloadListener(createDownloadListener(context))
        
        return webView
    }

    private fun createDownloadListener(context: Context): DownloadListener {
        return DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            try {
                val fileName = extractFileName(url, contentDisposition, mimeType)
                
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(fileName)
                    setDescription("Downloading...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                    addRequestHeader("User-Agent", userAgent)
                    setMimeType(mimeType)
                }
                
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        contentDisposition?.let { disposition ->
            val fileNamePattern = "filename=\"?([^\"]+)\"?".toRegex()
            val match = fileNamePattern.find(disposition)
            if (match != null) {
                return URLDecoder.decode(match.groupValues[1], "UTF-8")
            }
        }
        
        try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrEmpty()) {
                return URLDecoder.decode(lastSegment, "UTF-8")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val extension = getExtensionFromMimeType(mimeType)
        return "download_${System.currentTimeMillis()}$extension"
    }

    private fun getExtensionFromMimeType(mimeType: String?): String {
        return when (mimeType) {
            "text/html" -> ".html"
            "text/plain" -> ".txt"
            "application/pdf" -> ".pdf"
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "video/mp4" -> ".mp4"
            "audio/mpeg" -> ".mp3"
            "application/zip" -> ".zip"
            "application/json" -> ".json"
            else -> ""
        }
    }

    fun configurePrivateMode(webView: WebView) {
        webView.settings.apply {
            cacheMode = WebSettings.LOAD_NO_CACHE
            saveFormData = false
        }
        
        CookieManager.getInstance().removeAllCookies(null)
    }

    fun setDesktopMode(webView: WebView, enabled: Boolean) {
        val settings = webView.settings
        settings.userAgentString = if (enabled) {
            DESKTOP_USER_AGENT
        } else {
            buildUserAgent(settings.userAgentString)
        }
        settings.useWideViewPort = enabled
        settings.loadWithOverviewMode = enabled
    }

    fun clearAllData(context: Context) {
        WebView(context).clearCache(true)
        
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        
        WebView(context).clearFormData()
        
        WebView(context).clearHistory()
    }

    private fun buildUserAgent(defaultUA: String): String {
        return "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun pauseWebView(webView: WebView) {
        webView.onPause()
        webView.pauseTimers()
    }

    fun resumeWebView(webView: WebView) {
        webView.onResume()
        webView.resumeTimers()
    }

    fun destroyWebView(webView: WebView) {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }
}
