package com.farazinc.webly.data.model

sealed class AdBlockRule {

    data class UrlPattern(
        val pattern: String,
        val isException: Boolean = false,
        val options: FilterOptions = FilterOptions()
    ) : AdBlockRule()

    data class DomainRule(
        val pattern: String,
        val domains: Set<String>,
        val excludeDomains: Set<String> = emptySet(),
        val options: FilterOptions = FilterOptions()
    ) : AdBlockRule()

    data class RegexRule(
        val regex: Regex,
        val isException: Boolean = false,
        val options: FilterOptions = FilterOptions()
    ) : AdBlockRule()

    data class ElementHiding(
        val domains: Set<String>,
        val selector: String,
        val isException: Boolean = false
    ) : AdBlockRule()
}

data class FilterOptions(
    val types: Set<ResourceType> = emptySet(),
    val thirdParty: Boolean? = null,
    val matchCase: Boolean = false,
    val collapse: Boolean = true
)

enum class ResourceType {
    SCRIPT,
    IMAGE,
    STYLESHEET,
    OBJECT,
    XMLHTTPREQUEST,
    SUBDOCUMENT,
    PING,
    WEBSOCKET,
    MEDIA,
    FONT,
    OTHER
}
