package com.farazinc.webly.engine.adblock

import com.farazinc.webly.data.model.AdBlockRule
import com.farazinc.webly.data.model.FilterOptions
import com.farazinc.webly.data.model.ResourceType

class FilterParser {
    
    companion object {
        private const val EXCEPTION_PREFIX = "@@"
        private const val DOMAIN_ANCHOR = "||"
        private const val ELEMENT_HIDING_SEPARATOR = "##"
        private const val ELEMENT_HIDING_EXCEPTION_SEPARATOR = "#@#"
        private const val OPTIONS_SEPARATOR = "$"
        
        private val REGEX_SPECIAL_CHARS = setOf('.', '+', '?', '[', ']', '(', ')', '{', '}', '\\')
    }

    fun parseRule(line: String): AdBlockRule? {
        val trimmed = line.trim()
        
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[")) {
            return null
        }
        
        if (trimmed.contains(ELEMENT_HIDING_SEPARATOR) || 
            trimmed.contains(ELEMENT_HIDING_EXCEPTION_SEPARATOR)) {
            return parseElementHidingRule(trimmed)
        }
        
        return parseUrlRule(trimmed)
    }

    private fun parseUrlRule(rule: String): AdBlockRule? {
        var pattern = rule
        val isException = pattern.startsWith(EXCEPTION_PREFIX)
        
        if (isException) {
            pattern = pattern.substring(EXCEPTION_PREFIX.length)
        }
        
        val (urlPattern, options) = extractOptions(pattern)
        
        if (urlPattern.startsWith("/") && urlPattern.endsWith("/") && urlPattern.length > 2) {
            return parseRegexRule(urlPattern, isException, options)
        }
        
        if (options.domains.isNotEmpty() || options.excludeDomains.isNotEmpty()) {
            return AdBlockRule.DomainRule(
                pattern = convertPatternToRegex(urlPattern),
                domains = options.domains,
                excludeDomains = options.excludeDomains,
                options = options.filterOptions
            )
        }
        
        return AdBlockRule.UrlPattern(
            pattern = convertPatternToRegex(urlPattern),
            isException = isException,
            options = options.filterOptions
        )
    }

    private fun parseElementHidingRule(rule: String): AdBlockRule? {
        val isException = rule.contains(ELEMENT_HIDING_EXCEPTION_SEPARATOR)
        val separator = if (isException) ELEMENT_HIDING_EXCEPTION_SEPARATOR else ELEMENT_HIDING_SEPARATOR
        
        val parts = rule.split(separator, limit = 2)
        if (parts.size != 2) return null
        
        val domainsPart = parts[0]
        val selector = parts[1]
        
        val domains = if (domainsPart.isEmpty()) {
            emptySet()
        } else {
            domainsPart.split(",").map { it.trim() }.toSet()
        }
        
        return AdBlockRule.ElementHiding(
            domains = domains,
            selector = selector,
            isException = isException
        )
    }

    private fun parseRegexRule(pattern: String, isException: Boolean, options: ParsedOptions): AdBlockRule? {
        return try {
            val regexPattern = pattern.substring(1, pattern.length - 1)
            AdBlockRule.RegexRule(
                regex = Regex(regexPattern, RegexOption.IGNORE_CASE),
                isException = isException,
                options = options.filterOptions
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractOptions(pattern: String): Pair<String, ParsedOptions> {
        val optionsIndex = pattern.indexOf(OPTIONS_SEPARATOR)
        if (optionsIndex == -1) {
            return Pair(pattern, ParsedOptions())
        }
        
        val urlPattern = pattern.substring(0, optionsIndex)
        val optionsString = pattern.substring(optionsIndex + 1)
        
        val options = parseOptions(optionsString)
        return Pair(urlPattern, options)
    }

    private fun parseOptions(optionsString: String): ParsedOptions {
        val types = mutableSetOf<ResourceType>()
        val domains = mutableSetOf<String>()
        val excludeDomains = mutableSetOf<String>()
        var thirdParty: Boolean? = null
        var matchCase = false
        var collapse = true
        
        optionsString.split(",").forEach { option ->
            val opt = option.trim()
            when {
                opt.startsWith("domain=") -> {
                    val domainList = opt.substring(7).split("|")
                    domainList.forEach { domain ->
                        if (domain.startsWith("~")) {
                            excludeDomains.add(domain.substring(1))
                        } else {
                            domains.add(domain)
                        }
                    }
                }
                opt == "third-party" -> thirdParty = true
                opt == "~third-party" -> thirdParty = false
                opt == "match-case" -> matchCase = true
                opt == "collapse" -> collapse = true
                opt == "~collapse" -> collapse = false
                else -> {
                    parseResourceType(opt)?.let { types.add(it) }
                }
            }
        }
        
        return ParsedOptions(
            filterOptions = FilterOptions(
                types = types,
                thirdParty = thirdParty,
                matchCase = matchCase,
                collapse = collapse
            ),
            domains = domains,
            excludeDomains = excludeDomains
        )
    }

    private fun parseResourceType(option: String): ResourceType? {
        return when (option.lowercase()) {
            "script" -> ResourceType.SCRIPT
            "image" -> ResourceType.IMAGE
            "stylesheet" -> ResourceType.STYLESHEET
            "object" -> ResourceType.OBJECT
            "xmlhttprequest" -> ResourceType.XMLHTTPREQUEST
            "subdocument" -> ResourceType.SUBDOCUMENT
            "ping" -> ResourceType.PING
            "websocket" -> ResourceType.WEBSOCKET
            "media" -> ResourceType.MEDIA
            "font" -> ResourceType.FONT
            else -> null
        }
    }

    private fun convertPatternToRegex(pattern: String): String {
        var result = pattern
        
        if (result.startsWith(DOMAIN_ANCHOR)) {
            result = result.substring(2)
            result = "^https?://([a-z0-9\\-]+\\.)*?" + escapeRegex(result)
        } else {
            result = escapeRegex(result)
        }
        
        result = result.replace("\\^", "($|[^a-zA-Z0-9._%\\-])")
        
        result = result.replace("\\*", ".*")
        
        if (result.startsWith("\\|")) {
            result = "^" + result.substring(2)
        }
        if (result.endsWith("\\|")) {
            result = result.substring(0, result.length - 2) + "$"
        }
        
        return result
    }

    private fun escapeRegex(str: String): String {
        return str.map { char ->
            if (REGEX_SPECIAL_CHARS.contains(char)) "\\$char" else char.toString()
        }.joinToString("")
    }

    private data class ParsedOptions(
        val filterOptions: FilterOptions = FilterOptions(),
        val domains: Set<String> = emptySet(),
        val excludeDomains: Set<String> = emptySet()
    )
}
