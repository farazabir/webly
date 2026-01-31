package com.farazinc.webly.data.model

import android.graphics.Bitmap
import android.os.Bundle

data class Tab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "",
    val favicon: Bitmap? = null,
    val isActive: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val webViewState: Bundle? = null
) {
    companion object {
        fun createNew(id: String = java.util.UUID.randomUUID().toString()): Tab {
            return Tab(id = id, isActive = true)
        }
    }
}
