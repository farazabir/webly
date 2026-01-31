package com.farazinc.webly.engine.adblock

import com.farazinc.webly.data.model.AdBlockRule
import com.farazinc.webly.data.model.ResourceType
import java.net.URI

class FilterMatcher {
    
    private val domainRules = mutableMapOf<String, MutableList<AdBlockRule.DomainRule>>()
    
    private val urlPatternRules = mutableListOf<AdBlockRule.UrlPattern>()
    
    private val regexRules = mutableListOf<AdBlockRule.RegexRule>()
    
    private val elementHidingRules = mutableMapOf<String, MutableList<AdBlockRule.ElementHiding>>()
    
    private val exceptionRules = mutableListOf<AdBlockRule>()
    
    private val matchCache = object : LinkedHashMap<String, Boolean>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > 1000
        }
    }

    fun addRule(rule: AdBlockRule) {
        when (rule) {
            is AdBlockRule.DomainRule -> {
                rule.domains.forEach { domain ->
                    domainRules.getOrPut(domain) { mutableListOf() }.add(rule)
                }
            }
            is AdBlockRule.UrlPattern -> {
                if (rule.isException) {
                    exceptionRules.add(rule)
                } else {
                    urlPatternRules.add(rule)
                }
            }
            is AdBlockRule.RegexRule -> {
                if (rule.isException) {
                    exceptionRules.add(rule)
                } else {
                    regexRules.add(rule)
                }
            }
            is AdBlockRule.ElementHiding -> {
                if (rule.domains.isEmpty()) {
                    elementHidingRules.getOrPut("*") { mutableListOf() }.add(rule)
                } else {
                    rule.domains.forEach { domain ->
                        elementHidingRules.getOrPut(domain) { mutableListOf() }.add(rule)
                    }
                }
            }
        }
    }

    fun shouldBlock(url: String, documentUrl: String? = null, resourceType: ResourceType = ResourceType.OTHER): Boolean {
        val cacheKey = "$url|$documentUrl|$resourceType"
        matchCache[cacheKey]?.let { return it }
        
        val urlDomain = extractDomain(url)
        val documentDomain = documentUrl?.let { extractDomain(it) }
        val isThirdParty = documentDomain != null && urlDomain != documentDomain
        
        if (matchesExceptionRule(url, urlDomain, documentDomain, isThirdParty, resourceType)) {
            matchCache[cacheKey] = false
            return false
        }
        
        if (urlDomain != null && domainRules.containsKey(urlDomain)) {
            val rules = domainRules[urlDomain]!!
            for (rule in rules) {
                if (matchesDomainRule(rule, url, documentDomain, isThirdParty, resourceType)) {
                    matchCache[cacheKey] = true
                    return true
                }
            }
        }
        
        for (rule in urlPatternRules) {
            if (matchesUrlPattern(rule, url, isThirdParty, resourceType)) {
                matchCache[cacheKey] = true
                return true
            }
        }
        
        for (rule in regexRules) {
            if (matchesRegexRule(rule, url, isThirdParty, resourceType)) {
                matchCache[cacheKey] = true
                return true
            }
        }
        
        matchCache[cacheKey] = false
        return false
    }

    fun getElementHidingSelectors(domain: String): List<String> {
        val selectors = mutableListOf<String>()
        
        elementHidingRules["*"]?.forEach { rule ->
            if (!rule.isException) {
                selectors.add(rule.selector)
            }
        }
        
        elementHidingRules[domain]?.forEach { rule ->
            if (!rule.isException) {
                selectors.add(rule.selector)
            }
        }
        
        return selectors
    }

    private fun matchesExceptionRule(
        url: String,
        urlDomain: String?,
        documentDomain: String?,
        isThirdParty: Boolean,
        resourceType: ResourceType
    ): Boolean {
        for (rule in exceptionRules) {
            when (rule) {
                is AdBlockRule.UrlPattern -> {
                    if (matchesUrlPattern(rule, url, isThirdParty, resourceType)) {
                        return true
                    }
                }
                is AdBlockRule.RegexRule -> {
                    if (matchesRegexRule(rule, url, isThirdParty, resourceType)) {
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    private fun matchesDomainRule(
        rule: AdBlockRule.DomainRule,
        url: String,
        documentDomain: String?,
        isThirdParty: Boolean,
        resourceType: ResourceType
    ): Boolean {
        if (documentDomain != null) {
            val matchesDomain = rule.domains.isEmpty() || rule.domains.any { documentDomain.endsWith(it) }
            val excludedDomain = rule.excludeDomains.any { documentDomain.endsWith(it) }
            
            if (!matchesDomain || excludedDomain) {
                return false
            }
        }
        
        if (!matchesOptions(rule.options, isThirdParty, resourceType)) {
            return false
        }
        
        return url.matches(Regex(rule.pattern, RegexOption.IGNORE_CASE))
    }

    private fun matchesUrlPattern(
        rule: AdBlockRule.UrlPattern,
        url: String,
        isThirdParty: Boolean,
        resourceType: ResourceType
    ): Boolean {
        if (!matchesOptions(rule.options, isThirdParty, resourceType)) {
            return false
        }
        
        return url.matches(Regex(rule.pattern, RegexOption.IGNORE_CASE))
    }

    private fun matchesRegexRule(
        rule: AdBlockRule.RegexRule,
        url: String,
        isThirdParty: Boolean,
        resourceType: ResourceType
    ): Boolean {
        if (!matchesOptions(rule.options, isThirdParty, resourceType)) {
            return false
        }
        
        return rule.regex.containsMatchIn(url)
    }

    private fun matchesOptions(
        options: com.farazinc.webly.data.model.FilterOptions,
        isThirdParty: Boolean,
        resourceType: ResourceType
    ): Boolean {
        if (options.thirdParty != null && options.thirdParty != isThirdParty) {
            return false
        }
        
        if (options.types.isNotEmpty() && !options.types.contains(resourceType)) {
            return false
        }
        
        return true
    }

    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    fun getStats(): FilterStats {
        return FilterStats(
            domainRules = domainRules.values.sumOf { it.size },
            urlPatternRules = urlPatternRules.size,
            regexRules = regexRules.size,
            elementHidingRules = elementHidingRules.values.sumOf { it.size },
            exceptionRules = exceptionRules.size,
            cacheSize = matchCache.size
        )
    }

    fun clearCache() {
        matchCache.clear()
    }
}

data class FilterStats(
    val domainRules: Int,
    val urlPatternRules: Int,
    val regexRules: Int,
    val elementHidingRules: Int,
    val exceptionRules: Int,
    val cacheSize: Int
) {
    val totalRules: Int
        get() = domainRules + urlPatternRules + regexRules + elementHidingRules + exceptionRules
}
