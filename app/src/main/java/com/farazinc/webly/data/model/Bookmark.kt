package com.farazinc.webly.data.model

data class Bookmark(
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val favicon: String? = null
)
