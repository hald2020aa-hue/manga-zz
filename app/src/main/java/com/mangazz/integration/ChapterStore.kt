package com.mangazz.integration

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

/** Stores page images in private app storage so the native reader works without internet. */
class ChapterStore(private val context: Context, private val client: OkHttpClient = OkHttpClient()) {
    private val root: File by lazy { File(context.filesDir, "mangazz_chapters").apply { mkdirs() } }

    private fun folder(id: String) = File(root, digest(id))
    fun getLocalPageFiles(chapterId: String): List<File> =
        folder(chapterId).listFiles()?.filter { it.isFile && it.name.matches(Regex("^\\d{4}\\.(jpg|png|webp|gif|avif)$")) }
            ?.sortedBy { it.name } ?: emptyList()

    /** Must be called from a coroutine. Progress callback is invoked on the IO dispatcher. */
    suspend fun downloadChapter(
        chapterId: String,
        title: String,
        pages: List<MangaPage>,
        userAgent: String,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "Chapter has no image pages" }
        val finalDir = folder(chapterId)
        if (finalDir.exists() && getLocalPageFiles(chapterId).size == pages.size) return@withContext finalDir
        val staging = File(root, "${digest(chapterId)}.partial")
        staging.deleteRecursively()
        check(staging.mkdirs()) { "Cannot create temporary chapter folder" }
        try {
            pages.forEachIndexed { i, page ->
                coroutineContext.ensureActive()
                val url = page.imageUrl
                require(url.startsWith("https://")) { "Invalid image URL" }
                val request = Request.Builder().url(url)
                    .header("User-Agent", userAgent)
                    .header("Referer", page.referer)
                    .apply {
                        CookieManager.getInstance().getCookie(url)?.let { header("Cookie", it) }
                    }.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Image ${i + 1}: HTTP ${response.code}")
                    val mime = response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase() ?: ""
                    val extension = when (mime) {
                        "image/jpeg", "image/jpg" -> "jpg"
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/gif" -> "gif"
                        "image/avif" -> "avif"
                        else -> throw IOException("Not an image on page ${i + 1}: $mime")
                    }
                    val body = response.body ?: throw IOException("Empty image on page ${i + 1}")
                    if (body.contentLength()>50_000_000) throw IOException("Image too large")
                    body.byteStream().use { input ->
                        File(staging, "%04d.%s".format(i + 1, extension))
                            .outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                }
                onProgress(i + 1, pages.size)
            }
            val manifest = JSONObject().apply {
                put("chapterId", chapterId)
                put("title", title)
                put("pages", JSONArray(getImages(staging).map { it.name }))
            }
            File(staging, "chapter.json").writeText(manifest.toString(2))
            if (finalDir.exists()) check(finalDir.deleteRecursively()) { "Cannot replace old chapter" }
            check(staging.renameTo(finalDir)) { "Cannot finalize chapter" }
            finalDir
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
    }

    /** Optional: export already-downloaded images to CBZ under app-private storage. */
    suspend fun exportCbz(chapterId: String): File = withContext(Dispatchers.IO) {
        val pages = getLocalPageFiles(chapterId)
        require(pages.isNotEmpty()) { "Download the chapter first" }
        val archive = File(root, "${digest(chapterId)}.cbz")
        val temp = File(root, "${digest(chapterId)}.cbz.partial")
        try {
            ZipOutputStream(temp.outputStream().buffered()).use { zip ->
                pages.forEach { file ->
                    coroutineContext.ensureActive()
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            if (archive.exists()) check(archive.delete()) { "Cannot replace CBZ" }
            check(temp.renameTo(archive)) { "Cannot finalize CBZ" }
            archive
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }

    fun deleteChapter(chapterId: String): Boolean = folder(chapterId).deleteRecursively()

    private fun getImages(dir: File): List<File> =
        dir.listFiles()?.filter { it.isFile && it.name.matches(Regex("^\\d{4}\\.(jpg|png|webp|gif|avif)$")) }
            ?.sortedBy { it.name } ?: emptyList()

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(16).joinToString("") { "%02x".format(it) }
}
