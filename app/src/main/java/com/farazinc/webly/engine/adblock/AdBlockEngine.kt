package com.farazinc.webly.engine.adblock

import android.content.Context
import com.farazinc.webly.data.model.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class AdBlockEngine(private val context: Context) {
    
    private val parser = FilterParser()
    private val matcher = FilterMatcher()
    private var isLoaded = false

    suspend fun loadFilters(filterFiles: List<String> = listOf("filters.txt")) = withContext(Dispatchers.IO) {
        var loadedCount = 0
        var skippedCount = 0
        
        filterFiles.forEach { fileName ->
            try {
                context.assets.open(fileName).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                        lines.forEach { line ->
                            val rule = parser.parseRule(line)
                            if (rule != null) {
                                matcher.addRule(rule)
                                loadedCount++
                            } else {
                                skippedCount++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        isLoaded = true
        
        FilterLoadResult(
            loadedRules = loadedCount,
            skippedLines = skippedCount,
            stats = matcher.getStats()
        )
    }

    fun shouldBlock(
        url: String,
        documentUrl: String? = null,
        resourceType: ResourceType = ResourceType.OTHER
    ): Boolean {
        if (!isLoaded) return false
        return matcher.shouldBlock(url, documentUrl, resourceType)
    }

    fun getElementHidingSelectors(domain: String): List<String> {
        if (!isLoaded) return emptyList()
        return matcher.getElementHidingSelectors(domain)
    }

    fun getStats(): FilterStats? {
        if (!isLoaded) return null
        return matcher.getStats()
    }

    fun clearCache() {
        matcher.clearCache()
    }

    fun isReady(): Boolean = isLoaded
}

data class FilterLoadResult(
    val loadedRules: Int,
    val skippedLines: Int,
    val stats: FilterStats
)
