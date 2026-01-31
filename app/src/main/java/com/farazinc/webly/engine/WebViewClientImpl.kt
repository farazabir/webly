package com.farazinc.webly.engine

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.farazinc.webly.data.model.ResourceType
import com.farazinc.webly.engine.adblock.AdBlockEngine
import java.io.ByteArrayInputStream
import java.net.URLDecoder

class WebViewClientImpl(
    private val adBlockEngine: AdBlockEngine,
    private val callbacks: WebViewCallbacks,
    private val context: Context? = null
) : WebViewClient() {
    
    private var currentUrl: String? = null

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        val resourceType = detectResourceType(request)
        
        if (adBlockEngine.shouldBlock(url, currentUrl, resourceType)) {
            return createEmptyResponse()
        }
        
        return super.shouldInterceptRequest(view, request)
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentUrl = url
        callbacks.onPageStarted(url, favicon)
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        
        url?.let { pageUrl ->
            val domain = extractDomain(pageUrl)
            if (domain != null) {
                val selectors = adBlockEngine.getElementHidingSelectors(domain)
                if (selectors.isNotEmpty()) {
                    injectElementHidingCSS(view, selectors)
                }
            }
        }
        
        callbacks.onPageFinished(url)
    }
    
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        callbacks.onProgressChanged(view?.progress ?: 0)
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        return false
    }

    private fun detectResourceType(request: WebResourceRequest): ResourceType {
        val url = request.url.toString()
        val headers = request.requestHeaders
        
        val accept = headers["Accept"]?.lowercase() ?: ""
        
        return when {
            accept.contains("text/css") -> ResourceType.STYLESHEET
            accept.contains("application/javascript") || 
            accept.contains("text/javascript") -> ResourceType.SCRIPT
            accept.contains("image/") -> ResourceType.IMAGE
            accept.contains("video/") || 
            accept.contains("audio/") -> ResourceType.MEDIA
            accept.contains("font/") || 
            url.contains(Regex("\\.(woff|woff2|ttf|otf|eot)")) -> ResourceType.FONT
            url.endsWith(".js") -> ResourceType.SCRIPT
            url.endsWith(".css") -> ResourceType.STYLESHEET
            url.contains(Regex("\\.(jpg|jpeg|png|gif|webp|svg|ico)")) -> ResourceType.IMAGE
            url.contains(Regex("\\.(mp4|webm|ogg|mp3|wav)")) -> ResourceType.MEDIA
            else -> ResourceType.OTHER
        }
    }

    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    private fun injectElementHidingCSS(view: WebView?, selectors: List<String>) {
        if (view == null || selectors.isEmpty()) return
        
        val css = selectors.joinToString(", ") { it }
        
        val js = """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.innerHTML = '$css { display: none !important; visibility: hidden !important; }';
                document.head.appendChild(style);
            })();
        """.trimIndent()
        
        view.evaluateJavascript(js, null)
    }

    private fun extractDomain(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }
}

interface WebViewCallbacks {
    fun onPageStarted(url: String?, favicon: Bitmap?)
    fun onPageFinished(url: String?)
    fun onProgressChanged(progress: Int)
    fun onReceivedError(errorCode: Int, description: String?, failingUrl: String?)
}
