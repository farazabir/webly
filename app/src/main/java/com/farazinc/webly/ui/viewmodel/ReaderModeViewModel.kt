package com.farazinc.webly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farazinc.webly.domain.ReaderModeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReaderModeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val parser = ReaderModeParser()
    
    private val _article = MutableStateFlow<ReaderModeParser.Article?>(null)
    val article: StateFlow<ReaderModeParser.Article?> = _article.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _fontSize = MutableStateFlow(16)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    fun parseArticle(html: String, url: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            try {
                val parsed = parser.parse(html, url)
                _article.value = parsed
            } catch (e: Exception) {
                e.printStackTrace()
                _article.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
    }
}
