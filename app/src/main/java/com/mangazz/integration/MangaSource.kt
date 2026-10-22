package com.mangazz.integration

/** Source contract. The URL, selectors and permission to access content belong to the selected source. */
data class MangaChapter(val id: String, val title: String, val url: String)
data class MangaPage(val imageUrl: String, val referer: String)

interface MangaSource {
    val sourceName: String
    suspend fun getChapters(mangaUrl: String): List<MangaChapter>
    suspend fun getPages(chapter: MangaChapter): List<MangaPage>
}
