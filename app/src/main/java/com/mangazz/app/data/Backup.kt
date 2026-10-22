package com.mangazz.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

fun safeWebUrl(value: String): String {
    val normalized = value.trim()
    val uri = URI(normalized)
    require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && normalized.length < 2048 && uri.userInfo == null) { "استخدم رابط HTTPS صالحًا" }
    return uri.toASCIIString()
}

data class BackupData(val sources: List<Source>, val manga: List<Manga>, val folders: List<Folder>, val pages: List<Page>)

object Backup {
    fun export(sources: List<Source>, manga: List<Manga>, folders: List<Folder>, pages: List<Page>): String {
        val root = JSONObject().put("app", "Manga ZZ").put("version", 1)
        root.put("sources", JSONArray().apply { sources.forEach { put(JSONObject().put("name", it.name).put("url", it.url).put("custom", it.custom)) } })
        root.put("manga", JSONArray().apply { manga.forEach { put(JSONObject().put("title", it.title).put("url", it.url).put("cover", it.cover).put("favorite", it.favorite).put("folder", it.folder).put("page", it.page).put("pages", it.pages).put("lastRead", it.lastRead)) } })
        root.put("folders", JSONArray().apply { folders.forEach { put(it.name) } })
        // Android Storage Access Framework URIs are device-local; exported as reference only.
        root.put("pages", JSONArray().apply { pages.forEach { put(JSONObject().put("mangaId", it.mangaId).put("position", it.position).put("imageUri", it.imageUri)) } })
        return root.toString(2)
    }
    fun parse(text: String): BackupData {
        require(text.length < 6_000_000) { "حجم النسخة كبير" }
        val root = JSONObject(text)
        require(root.getString("app") == "Manga ZZ" && root.getInt("version") == 1) { "نسخة غير متوافقة" }
        val s = root.getJSONArray("sources"); val m = root.getJSONArray("manga"); val f = root.getJSONArray("folders")
        require(s.length() < 1000 && m.length() < 10000 && f.length() < 1000) { "عدد عناصر غير مقبول" }
        val sources = (0 until s.length()).map { val x = s.getJSONObject(it); Source(name = x.getString("name").take(200), url = safeWebUrl(x.getString("url")), custom = x.optBoolean("custom", true)) }.distinctBy { it.url }
        val mangas = (0 until m.length()).map { val x = m.getJSONObject(it); Manga(title = x.getString("title").take(300), url = safeWebUrl(x.getString("url")), cover = x.optString("cover").take(2000), favorite = x.optBoolean("favorite"), folder = x.optString("folder", "الكل").take(100), page = x.optInt("page").coerceAtLeast(0), pages = x.optInt("pages").coerceAtLeast(0), lastRead = x.optLong("lastRead")) }.distinctBy { it.url }
        val folders = (0 until f.length()).map { Folder(name = f.getString(it).take(100)) }.distinctBy { it.name }
        // Page references are intentionally not restored because manga ids and SAF permissions differ across devices.
        return BackupData(sources, mangas, folders, emptyList())
    }
}
