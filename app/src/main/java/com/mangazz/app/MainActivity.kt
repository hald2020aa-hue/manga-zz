package com.mangazz.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import com.mangazz.app.data.*
import com.mangazz.app.ui.*
import com.mangazz.app.ui.screens.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val db=Room.databaseBuilder(applicationContext,MangaDatabase::class.java,"mangazz.db").build()
        val settings=LocalSettings(applicationContext)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MangaZZ(db.catalog(),settings) } }
    }
}

enum class Screen(val ar:String){ HOME("الرئيسية"),SOURCES("المصادر"),LIBRARY("المكتبة"),DETAIL("التفاصيل"),BROWSER("المتصفح"),READER("القارئ"),DOWNLOADS("التنزيلات"),SETTINGS("الإعدادات"),BACKUP("النسخ الاحتياطي") }

@Composable fun MangaZZ(dao:CatalogDao,settings:LocalSettings) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    val sources by dao.sources().collectAsState(initial=emptyList())
    val manga by dao.manga().collectAsState(initial=emptyList())
    val folders by dao.folders().collectAsState(initial=emptyList())
    val theme by settings.theme.collectAsState(initial="blue")
    val reading by settings.reading.collectAsState(initial="عمودي")
    val accent=themeAccent(theme)
    var screen by remember { mutableStateOf(Screen.HOME) }
    var chosen by remember { mutableStateOf<Manga?>(null) }
    var startingUrl by remember { mutableStateOf("https://mangadex.org/") }
    var importedManga by remember { mutableLongStateOf(-1L) }
    fun go(s:Screen){screen=s}
    fun openSite(url:String){startingUrl=url;go(Screen.BROWSER)}
    fun toast(s:String){Toast.makeText(context,s,Toast.LENGTH_LONG).show()}
    val images=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if(importedManga>0 && picked.isNotEmpty()) {
            val id=importedManga
            picked.forEach { try {context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch (_:Exception){} }
            scope.launch { val existing=dao.pages(id).first(); val start=(existing.maxOfOrNull{it.position}?:-1)+1
                dao.addPages(picked.mapIndexed { pos,uri->Page(id,start+pos,uri.toString()) })
                toast("تمت إضافة ${picked.size} صفحة")
            }
        }
    }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri->
        if(uri!=null) scope.launch { try {
            val allPages=manga.flatMap { dao.pages(it.id).first() }
            context.contentResolver.openOutputStream(uri)?.use { it.write(Backup.export(sources,manga,folders,allPages).toByteArray(Charsets.UTF_8)) }
            toast("تم تصدير النسخة المحلية")
        }catch(e:Exception){toast(e.message?:"فشل التصدير")} }
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
        if(uri!=null) scope.launch {try {
            val text=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:error("تعذر فتح النسخة")
            val backup=Backup.parse(text)
            dao.replaceAll(backup.sources,backup.manga,backup.folders,backup.pages)
            chosen=null;toast("تمت استعادة البيانات الوصفية؛ ملفات الصور تحتاج إعادة ربط")
        } catch(e:Exception){toast(e.message?:"فشل الاستيراد")} }
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=accent,background=Background,surface=SurfaceGlass,onSurface=TextBright,onBackground=TextBright)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF071528),Background,Color(0xFF020911))))) {
            if(screen!=Screen.READER){Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("Manga ZZ",color=TextBright,fontSize=31.sp,style=MaterialTheme.typography.headlineLarge)
                Text("عالم المانجا بين يديك",color=TextDim,fontSize=13.sp)
            }}
            Box(Modifier.weight(1f)) {
                AnimatedContent(screen,transitionSpec={fadeIn(tween(190))+slideInHorizontally(tween(190)){it/6} togetherWith fadeOut(tween(110))},label="navigate") { target ->
                    when(target) {
                        Screen.HOME -> HomeScreen(manga,sources,accent,onSources={go(Screen.SOURCES)},onLibrary={go(Screen.LIBRARY)},onDetail={chosen=it;go(Screen.DETAIL)},onBrowser=::openSite)
                        Screen.SOURCES -> SourcesScreen(sources,accent,onOpen=::openSite,onAdd={name,url->scope.launch{try{dao.addSource(Source(name=name,url=safeWebUrl(url)))}catch(e:Exception){toast(e.message?:"خطأ")}}},onUpdate={scope.launch {dao.updateSource(it)}},onDelete={scope.launch {dao.deleteSource(it)}})
                        Screen.LIBRARY -> LibraryScreen(manga,folders,accent,onSelect={chosen=it;go(Screen.DETAIL)},onFavorite={scope.launch{dao.updateManga(it.copy(favorite=!it.favorite))}},onAdd={title,url->scope.launch{try{dao.addManga(Manga(title=title,url=safeWebUrl(url)))}catch(e:Exception){toast(e.message?:"خطأ في الرابط")}}},onFolder={scope.launch{dao.addFolder(Folder(name=it))}},onDelete={scope.launch{dao.deleteManga(it);dao.deletePages(it.id)}})
                        Screen.DETAIL -> { val book=chosen?.let{c->manga.firstOrNull{it.id==c.id}?:c}; if(book!=null) {
                            val pages by dao.pages(book.id).collectAsState(initial=emptyList())
                            DetailScreen(book,pages.size,accent,onBack={go(Screen.LIBRARY)},onRead={chosen=book;go(Screen.READER)},onOpen={openSite(book.url)},onPick={importedManga=book.id;images.launch(arrayOf("image/*"))},onFavorite={scope.launch{dao.updateManga(book.copy(favorite=!book.favorite))}})
                        }else BlankState("اختر مانجا من المكتبة") }
                        Screen.BROWSER -> BrowserScreen(startingUrl,accent,onSave={title,url->scope.launch{try{dao.addManga(Manga(title=title.ifBlank{url},url=safeWebUrl(url)));toast("تم الحفظ في المكتبة") }catch(e:Exception){toast(e.message?:"الرابط محفوظ أو غير صالح")}}})
                        Screen.READER -> { val book=chosen; if(book!=null) {
                            val pages by dao.pages(book.id).collectAsState(initial=emptyList())
                            ReaderScreen(book,pages,reading,accent,onBack={go(Screen.DETAIL)},onImport={importedManga=book.id;images.launch(arrayOf("image/*"))},onMode={scope.launch{settings.setReading(it)}},onProgress={page->scope.launch{dao.updateManga(book.copy(page=page,pages=pages.size,lastRead=System.currentTimeMillis()))}})
                        }else BlankState("افتح عملًا من المكتبة") }
                        Screen.DOWNLOADS -> DownloadsScreen(accent)
                        Screen.SETTINGS -> SettingsScreen(theme,reading,accent,onTheme={scope.launch{settings.setTheme(it)}},onReading={scope.launch{settings.setReading(it)}},onBackup={go(Screen.BACKUP)},onDownloads={go(Screen.DOWNLOADS)})
                        Screen.BACKUP -> BackupScreen(sources.size,manga.size,accent,onExport={export.launch("MangaZZ-Backup.json")},onRestore={restore.launch(arrayOf("application/json","text/plain"))})
                    }
                }
            }
            if(screen!=Screen.READER && screen!=Screen.BROWSER) {
                val bottom=listOf(Triple(Screen.HOME,Icons.Default.Home,"الرئيسية"),Triple(Screen.SOURCES,Icons.Default.Language,"المصادر"),Triple(Screen.LIBRARY,Icons.Default.CollectionsBookmark,"المكتبة"),Triple(Screen.DOWNLOADS,Icons.Default.Download,"تنزيلات"),Triple(Screen.SETTINGS,Icons.Default.Settings,"الإعدادات"))
                Row(Modifier.fillMaxWidth().background(SurfaceGlass).border(1.dp,Edge).padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceAround) {
                    bottom.forEach{(page,icon,title)->
                        val active=page==screen
                        Column(Modifier.weight(1f).clickable{go(page)}.padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Icon(icon,title,tint=if(active)accent else TextDim,modifier=Modifier.size(25.dp))
                            Text(title,color=if(active)accent else TextDim,fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
}
