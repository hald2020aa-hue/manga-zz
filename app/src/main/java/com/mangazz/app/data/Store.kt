package com.mangazz.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sources", indices = [Index(value = ["url"], unique = true)])
data class Source(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val url: String, val custom: Boolean = true)

@Entity(tableName = "manga", indices = [Index(value = ["url"], unique = true)])
data class Manga(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val url: String,
    val cover: String = "", val favorite: Boolean = false, val folder: String = "الكل",
    val page: Int = 0, val pages: Int = 0, val lastRead: Long = 0L)

@Entity(tableName = "pages", primaryKeys = ["mangaId", "position"])
data class Page(val mangaId: Long, val position: Int, val imageUri: String)

@Entity(tableName = "folders", indices = [Index(value = ["name"], unique = true)])
data class Folder(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM sources ORDER BY name") fun sources(): Flow<List<Source>>
    @Query("SELECT * FROM manga ORDER BY lastRead DESC, title") fun manga(): Flow<List<Manga>>
    @Query("SELECT * FROM folders ORDER BY name") fun folders(): Flow<List<Folder>>
    @Query("SELECT * FROM pages WHERE mangaId=:id ORDER BY position") fun pages(id: Long): Flow<List<Page>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addSource(s: Source): Long
    @Update suspend fun updateSource(s: Source)
    @Delete suspend fun deleteSource(s: Source)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addManga(m: Manga): Long
    @Update suspend fun updateManga(m: Manga)
    @Delete suspend fun deleteManga(m: Manga)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addFolder(f: Folder): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addPages(items: List<Page>)
    @Query("DELETE FROM pages WHERE mangaId=:id") suspend fun deletePages(id: Long)
    @Query("DELETE FROM sources") suspend fun clearSources()
    @Query("DELETE FROM pages") suspend fun clearPages()
    @Query("DELETE FROM manga") suspend fun clearManga()
    @Query("DELETE FROM folders") suspend fun clearFolders()
    @Transaction suspend fun replaceAll(sources: List<Source>, manga: List<Manga>, folders: List<Folder>, pages: List<Page>) {
        clearPages(); clearManga(); clearSources(); clearFolders()
        sources.forEach { addSource(it) }
        manga.forEach { addManga(it) }
        folders.forEach { addFolder(it) }
        addPages(pages)
    }
}

@Database(entities = [Source::class, Manga::class, Folder::class, Page::class], version = 1, exportSchema = false)
abstract class MangaDatabase : RoomDatabase() {
    abstract fun catalog(): CatalogDao
}
