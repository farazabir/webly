package com.farazinc.webly.ui.components

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.farazinc.webly.engine.*

@Composable
fun BrowserWebView(
    url: String,
    adBlockEngine: com.farazinc.webly.engine.adblock.AdBlockEngine,
    onPageStarted: (String, Bitmap?) -> Unit,
    onPageFinished: (String, String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    webViewRef: (WebView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val webView = WebViewEngine.createWebView(context)
            
            webView.webViewClient = WebViewClientImpl(
                adBlockEngine = adBlockEngine,
                callbacks = object : WebViewCallbacks {
                    override fun onPageStarted(url: String?, favicon: Bitmap?) {
                        onPageStarted(url ?: "", favicon)
                    }
                    
                    override fun onPageFinished(url: String?) {
                        val title = webView.title ?: ""
                        onPageFinished(url ?: "", title)
                        
                        onNavigationStateChanged(webView.canGoBack(), webView.canGoForward())
                    }
                    
                    override fun onProgressChanged(progress: Int) {
                        onProgressChanged(progress)
                    }
                    
                    override fun onReceivedError(errorCode: Int, description: String?, failingUrl: String?) {
                    }
                }
            )
            
            webView.webChromeClient = WebChromeClientImpl(
                callbacks = object : ChromeClientCallbacks {
                    override fun onProgressChanged(progress: Int) {
                        onProgressChanged(progress)
                    }
                    
                    override fun onReceivedTitle(title: String) {
                    }
                    
                    override fun onReceivedIcon(icon: Bitmap?) {
                    }
                }
            )
            
            webViewRef(webView)
            webView
        },
        update = { webView ->
            if (url.isNotEmpty() && webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
