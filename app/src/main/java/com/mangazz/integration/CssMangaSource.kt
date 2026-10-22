package com.mangazz.integration

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Configurable parser for STATIC HTML manga websites with authorized access.
 * It cannot decode every site's JavaScript, or discover site-specific APIs automatically.
 * Never hardcode private cookies, passwords, challenge tokens or bypass access controls.
 */
data class CssRules(
    val chapterLinks: String, // e.g. "a.chapter-link"
    val pageImages: String, // e.g. ".reader img"
    val chapterNameAttribute: String = "text",
    val chapterHrefAttribute: String = "href"
)

class CssMangaSource(
    override val sourceName: String,
    private val rules: CssRules,
    private val userAgent: String,
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
) : MangaSource {
    override suspend fun getChapters(mangaUrl: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val document = getDocument(mangaUrl)
        document.select(rules.chapterLinks).mapNotNull { element ->
            val link = absolute(document.baseUri(), element.attr(rules.chapterHrefAttribute))
            if (!link.startsWith("https://")) null else {
                MangaChapter(
                    id = link,
                    title = if (rules.chapterNameAttribute == "text") element.text().ifBlank { "فصل" }
                            else element.attr(rules.chapterNameAttribute).ifBlank { "فصل" },
                    url = link
                )
            }
        }.distinctBy { it.id }
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
        val document = getDocument(chapter.url)
        document.select(rules.pageImages).mapNotNull { element ->
            val raw = listOf("data-src", "data-original", "data-lazy-src", "src")
                .map { element.attr(it) }
                .firstOrNull { it.isNotBlank() && !it.startsWith("data:") }
                ?: return@mapNotNull null
            val url = absolute(document.baseUri(), raw)
            if (url.startsWith("https://"))
                MangaPage(url, chapter.url) else null
        }.distinctBy { it.imageUrl }
    }

    private fun getDocument(url: String): Document {
        require(url.startsWith("https://")) { "Only HTTPS sources supported" }
        val request = Request.Builder().url(url)
            .header("User-Agent", userAgent)
            .apply {
                CookieManager.getInstance().getCookie(url)?.let { header("Cookie", it) }
            }.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} @ ${response.request.url.host}")
            val body = response.body ?: throw IOException("Empty HTML")
            if ((body.contentLength()) > 3_000_000) throw IOException("HTML too large")
            val html = body.string().take(3_000_001)
            if (html.length>3_000_000) throw IOException("HTML too large")
            return Jsoup.parse(html, response.request.url.toString())
        }
    }

    private fun absolute(base: String, value: String) =
        try { URI(base).resolve(value.trim()).toString() } catch (_: Exception) { "" }
}
