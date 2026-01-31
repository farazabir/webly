package com.farazinc.webly.domain

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

class ReaderModeParser {
    
    data class Article(
        val title: String,
        val byline: String?,
        val content: String,
        val textContent: String,
        val excerpt: String,
        val siteName: String?,
        val publishedTime: String?
    )
    
    companion object {
        private val UNLIKELY_CANDIDATES = Regex(
            "banner|breadcrumbs?|combx|comment|community|cover-wrap|disqus|extra|footer|" +
            "gdpr|header|legends|menu|related|remark|replies|rss|shoutbox|sidebar|skyscraper|" +
            "social|sponsor|supplemental|ad-break|agegate|pagination|pager|popup|yom-remote",
            RegexOption.IGNORE_CASE
        )
        
        private val POSITIVE_CANDIDATES = Regex(
            "article|body|content|entry|hentry|h-entry|main|page|pagination|post|text|blog|story",
            RegexOption.IGNORE_CASE
        )
        
        private val BLOCK_ELEMENTS = setOf(
            "div", "article", "section", "main", "aside", "blockquote", "pre"
        )
        
        private const val MIN_CONTENT_LENGTH = 25
    }

    fun parse(html: String, url: String): Article? {
        return try {
            val doc = Jsoup.parse(html, url)
            
            removeUnwantedElements(doc)
            
            val title = extractTitle(doc)
            val byline = extractByline(doc)
            val siteName = extractSiteName(doc)
            val publishedTime = extractPublishedTime(doc)
            
            val contentElement = findMainContent(doc) ?: return null
            
            cleanContent(contentElement)
            
            val content = contentElement.html()
            val textContent = contentElement.text()
            val excerpt = generateExcerpt(textContent)
            
            if (textContent.length < MIN_CONTENT_LENGTH) {
                return null
            }
            
            Article(
                title = title,
                byline = byline,
                content = content,
                textContent = textContent,
                excerpt = excerpt,
                siteName = siteName,
                publishedTime = publishedTime
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun removeUnwantedElements(doc: Document) {
        doc.select("script, style, noscript, iframe, embed, object, link").remove()
        doc.select("form, button, input, select, textarea").remove()
        doc.select(".hidden, [style*='display:none'], [style*='visibility:hidden']").remove()
        doc.select("[aria-hidden='true']").remove()
    }

    private fun extractTitle(doc: Document): String {
        doc.select("meta[property=og:title]").first()?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        doc.select("meta[name=twitter:title]").first()?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        doc.select("h1").first()?.text()?.let {
            if (it.isNotBlank()) return it
        }
        
        return doc.title()
    }

    private fun extractByline(doc: Document): String? {
        doc.select("meta[name=author]").first()?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        doc.select(".author, .byline, [rel=author]").first()?.text()?.let {
            if (it.isNotBlank()) return it
        }
        
        return null
    }

    private fun extractSiteName(doc: Document): String? {
        doc.select("meta[property=og:site_name]").first()?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        return null
    }

    private fun extractPublishedTime(doc: Document): String? {
        doc.select("meta[property=article:published_time]").first()?.attr("content")?.let {
            if (it.isNotBlank()) return it
        }
        
        doc.select("time[datetime]").first()?.attr("datetime")?.let {
            if (it.isNotBlank()) return it
        }
        
        return null
    }

    private fun findMainContent(doc: Document): Element? {
        doc.select("article").firstOrNull()?.let { return it }
        doc.select("main").firstOrNull()?.let { return it }
        doc.select("[role=main]").firstOrNull()?.let { return it }
        
        val candidates = doc.select("div, section, article")
            .filter { element ->
                val className = element.className()
                val id = element.id()
                
                if (UNLIKELY_CANDIDATES.containsMatchIn(className) || 
                    UNLIKELY_CANDIDATES.containsMatchIn(id)) {
                    return@filter false
                }
                
                element.text().length > 100
            }
        
        if (candidates.isEmpty()) {
            return doc.body()
        }
        
        val scored = candidates.map { element ->
            var score = 0
            
            val className = element.className()
            val id = element.id()
            if (POSITIVE_CANDIDATES.containsMatchIn(className) || 
                POSITIVE_CANDIDATES.containsMatchIn(id)) {
                score += 25
            }
            
            score += element.select("p").size * 3
            
            val textLength = element.text().length
            score += (textLength / 100).coerceAtMost(50)
            
            score += element.text().count { it == ',' }.coerceAtMost(10)
            
            val linkDensity = calculateLinkDensity(element)
            if (linkDensity > 0.5) {
                score -= 25
            }
            
            element to score
        }
        
        return scored.maxByOrNull { it.second }?.first
    }

    private fun calculateLinkDensity(element: Element): Double {
        val textLength = element.text().length
        if (textLength == 0) return 1.0
        
        val linkLength = element.select("a").sumOf { it.text().length }
        return linkLength.toDouble() / textLength
    }

    private fun cleanContent(element: Element) {
        element.select("*").forEach { child ->
            if (calculateLinkDensity(child) > 0.8 && child.text().length < 100) {
                child.remove()
            }
        }
        
        element.select("p").forEach { p ->
            if (p.text().trim().isEmpty()) {
                p.remove()
            }
        }
        
        element.select("nav, aside, footer, header").remove()
        
        element.select(".comment, .comments, #comments, #disqus_thread").remove()
        
        element.select("*").forEach { child ->
            child.removeAttr("class")
            child.removeAttr("id")
            child.removeAttr("style")
            child.removeAttr("onclick")
        }
        
        val allowedTags = setOf("p", "div", "span", "a", "img", "h1", "h2", "h3", "h4", "h5", "h6", 
                                "strong", "em", "b", "i", "u", "ul", "ol", "li", "blockquote", 
                                "code", "pre", "br", "hr")
        
        element.select("*").forEach { child ->
            if (child.tagName() !in allowedTags) {
                child.unwrap()
            }
        }
    }

    private fun generateExcerpt(text: String, maxLength: Int = 200): String {
        val cleaned = text.trim()
        return if (cleaned.length <= maxLength) {
            cleaned
        } else {
            cleaned.take(maxLength).substringBeforeLast(' ') + "..."
        }
    }
}
