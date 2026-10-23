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
import com.mangazz.integration.MangaPage
import java.io.File
import org.json.JSONObject
import com.mangazz.integration.SourceRulesStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val db=Room.databaseBuilder(applicationContext,MangaDatabase::class.java,"mangazz.db").build()
        val settings=LocalSettings(applicationContext)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MangaZZ(db.catalog(),settings) } }
    }
}

enum class Screen(val ar:String){ HOME("الرئيسية"),SOURCES("المصادر"),LIBRARY("المكتبة"),DETAIL("التفاصيل"),BROWSER("المتصفح"),READER("القارئ"),REMOTE_READER("صفحات الفصل"),DOWNLOADS("التنزيلات"),SETTINGS("الإعدادات"),BACKUP("النسخ الاحتياطي") }

@Composable fun MangaZZ(dao:CatalogDao,settings:LocalSettings) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    val sources by dao.sources().collectAsState(initial=emptyList())
    val manga by dao.manga().collectAsState(initial=emptyList())
    val folders by dao.folders().collectAsState(initial=emptyList())
    val theme by settings.theme.collectAsState(initial="blue")
    val reading by settings.reading.collectAsState(initial="عمودي")
    val colors=v2Palette(theme)
    val accent=colors.accent
    val motion = remember { ZZMotionController() }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var chosen by remember { mutableStateOf<Manga?>(null) }
    var startingUrl by remember { mutableStateOf("https://mangadex.org/") }
    var importedManga by remember { mutableLongStateOf(-1L) }
    var remotePages by remember { mutableStateOf<List<Page>>(emptyList()) }
    var remoteTitle by remember { mutableStateOf("") }
    var pendingDownloadUrl by remember { mutableStateOf("") }
    fun go(s:Screen){screen=s}
    fun openSite(url:String){startingUrl=url;go(Screen.BROWSER)}
    fun toast(s:String){Toast.makeText(context,s,Toast.LENGTH_LONG).show()}
    val images=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if(importedManga>0 && picked.isNotEmpty()) {
            val id=importedManga
            picked.forEach { try {context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch (_:Exception){} }
            scope.launch { val existing=dao.pages(id).first(); val start=(existing.maxOfOrNull{it.position}?:-1)+1
                dao.addPages(picked.mapIndexed { pos,uri->Page(id,start+pos,uri.toString()) })
                motion.show(ZZMotionAssets.bookmark, "تم استيراد ${picked.size} صفحة")
                toast("تمت إضافة ${picked.size} صفحة")
            }
        }
    }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri->
        if(uri!=null) scope.launch { try {
            val allPages=manga.flatMap { dao.pages(it.id).first() }
            val json=JSONObject(Backup.export(sources,manga,folders,allPages))
                .put("cssRules",SourceRulesStore(context).exportRules())
                .put("themeV2",theme).put("readingModeV2",reading)
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toString(2).toByteArray(Charsets.UTF_8)) }
            motion.show(ZZMotionAssets.backupCompleted, "اكتمل تصدير النسخة الاحتياطية")
            toast("تم تصدير المكتبة والثيمات وقواعد المصادر")
        }catch(e:Exception){motion.show(ZZMotionAssets.failed, "فشل تصدير النسخة");toast(e.message?:"فشل التصدير")} }
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
        if(uri!=null) scope.launch {try {
            val text=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:error("تعذر فتح النسخة")
            val backup=Backup.parse(text)
            val metadata=JSONObject(text)
            dao.replaceAll(backup.sources,backup.manga,backup.folders,backup.pages)
            metadata.optJSONObject("cssRules")?.let { SourceRulesStore(context).importRules(it) }
            metadata.optString("themeV2").takeIf { it in listOf("blue","red","green","purple","gold") }?.let { settings.setTheme(it) }
            metadata.optString("readingModeV2").takeIf { it in listOf("عمودي","أفقي") }?.let { settings.setReading(it) }
            chosen=null;motion.show(ZZMotionAssets.backupCompleted, "تمت استعادة الإعدادات والمكتبة");toast("تمت استعادة المكتبة والثيم؛ أعد ربط صور الجهاز إذا لزم")
        } catch(e:Exception){motion.show(ZZMotionAssets.failed, "فشلت الاستعادة");toast(e.message?:"فشل الاستيراد")} }
    }
    CompositionLocalProvider(LocalZzPalette provides colors, LocalZZMotionController provides motion) {
    MaterialTheme(colorScheme=darkColorScheme(primary=accent,background=colors.background,surface=colors.surface,onSurface=colors.text,onBackground=colors.text)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.surface,colors.background,colors.background)))) {
            if(screen!=Screen.READER && screen!=Screen.REMOTE_READER){Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("Manga ZZ",color=TextBright,fontSize=31.sp,style=MaterialTheme.typography.headlineLarge)
                Text("عالم المانجا بين يديك",color=TextDim,fontSize=13.sp)
            }}
            Box(Modifier.weight(1f)) {
                AnimatedContent(screen,transitionSpec={fadeIn(tween(190))+slideInHorizontally(tween(190)){it/6} togetherWith fadeOut(tween(110))},label="navigate") { target ->
                    when(target) {
                        Screen.HOME -> HomeScreen(manga,sources,accent,onSources={go(Screen.SOURCES)},onLibrary={go(Screen.LIBRARY)},onDetail={chosen=it;go(Screen.DETAIL)},onBrowser=::openSite)
                        Screen.SOURCES -> SourcesScreenV2(sources,accent,onOpen=::openSite,onAdd={name,url->scope.launch{try{
                            val result=dao.addSource(Source(name=name,url=safeWebUrl(url)))
                            motion.show(if(result==-1L)ZZMotionAssets.failed else ZZMotionAssets.sourceSaved, if(result==-1L)"المصدر موجود مسبقًا" else "تم حفظ المصدر")
                            toast(if(result==-1L)"المصدر موجود من قبل" else "تم حفظ المصدر")
                        }catch(e:Exception){motion.show(ZZMotionAssets.failed,"فشل حفظ المصدر");toast(e.message?:"فشل الحفظ")}}},onUpdate={scope.launch {try {dao.updateSource(it);motion.show(ZZMotionAssets.sourceSaved,"تم تعديل المصدر");toast("تم تعديل المصدر")}catch(e:Exception){toast(e.message?:"تعذر التعديل")}}},onDelete={scope.launch {dao.deleteSource(it);motion.show(ZZMotionAssets.bookmark,"تم حذف المصدر");toast("تم حذف المصدر")}})
                        Screen.LIBRARY -> LibraryScreenV2(manga,folders,accent,onSelect={chosen=it;go(Screen.DETAIL)},onFavorite={scope.launch{dao.updateManga(it.copy(favorite=!it.favorite));motion.show(ZZMotionAssets.favorite,if(it.favorite)"أزيل من المفضلة" else "حُفظ في المفضلة")}},onAdd={title,url->scope.launch{try{val result=dao.addManga(Manga(title=title,url=safeWebUrl(url)));motion.show(if(result==-1L)ZZMotionAssets.failed else ZZMotionAssets.bookmark,if(result==-1L)"العمل محفوظ مسبقًا" else "تمت إضافة العمل")}catch(e:Exception){motion.show(ZZMotionAssets.failed,"تعذرت إضافة العمل");toast(e.message?:"خطأ في الرابط")}}},onFolder={scope.launch{val result=dao.addFolder(Folder(name=it));motion.show(if(result==-1L)ZZMotionAssets.failed else ZZMotionAssets.bookmark,if(result==-1L)"المجلد موجود مسبقًا" else "تم إنشاء المجلد")}},onDelete={scope.launch{dao.deletePages(it.id);dao.deleteManga(it);motion.show(ZZMotionAssets.bookmark,"حُذف العمل")}},onMove={b,f->scope.launch{dao.updateManga(b.copy(folder=f));motion.show(ZZMotionAssets.bookmark,"نُقل العمل إلى $f")}})
                        Screen.DETAIL -> { val book=chosen?.let{c->manga.firstOrNull{it.id==c.id}?:c}; if(book!=null) {
                            val pages by dao.pages(book.id).collectAsState(initial=emptyList())
                            DetailScreen(book,pages.size,accent,onBack={go(Screen.LIBRARY)},onRead={chosen=book;go(Screen.READER)},onOpen={openSite(book.url)},onPick={importedManga=book.id;images.launch(arrayOf("image/*"))},onFavorite={scope.launch{dao.updateManga(book.copy(favorite=!book.favorite));motion.show(ZZMotionAssets.favorite,if(book.favorite)"أزيل من المفضلة" else "حُفظ في المفضلة")}},extra={
                                ChapterSectionV2(book,accent,onReadRemote={found,title->
                                    motion.show(ZZMotionAssets.pageFlip,"فُتحت صفحات الفصل")
                                    remoteTitle=title
                                    remotePages=found.mapIndexed { i,page->Page(book.id,i,page.imageUrl) }
                                    screen=Screen.REMOTE_READER
                                },onDownloadComplete={title,files->scope.launch{
                                    val previous=dao.pages(book.id).first()
                                    val start=(previous.maxOfOrNull{it.position}?:-1)+1
                                    dao.addPages(files.mapIndexed {i,file->Page(book.id,start+i,Uri.fromFile(file).toString())})
                                    dao.updateManga(book.copy(pages=start+files.size))
                                    motion.show(ZZMotionAssets.downloadCompleted,"اكتمل تنزيل الفصل")
                                    toast("تم تنزيل الفصل وإضافته للقارئ: $title")
                                }})
                            })
                        }else BlankState("اختر مانجا من المكتبة") }
                        Screen.BROWSER -> BrowserScreen(startingUrl,accent,onSave={title,url->scope.launch{try{
                            val result=dao.addManga(Manga(title=title.ifBlank{url},url=safeWebUrl(url)))
                            motion.show(if(result==-1L)ZZMotionAssets.failed else ZZMotionAssets.bookmark,if(result==-1L)"الرابط محفوظ مسبقًا" else "تم حفظ الرابط في المكتبة")
                            toast(if(result==-1L)"هذا الرابط محفوظ مسبقًا" else "تم حفظ الرابط في المكتبة")
                        }catch(e:Exception){motion.show(ZZMotionAssets.failed,"تعذّر الحفظ");toast(e.message?:"الرابط غير صالح")}}},onDownload={url->
                            pendingDownloadUrl=url;go(Screen.DOWNLOADS)
                        })
                        Screen.READER -> { val book=chosen; if(book!=null) {
                            val pages by dao.pages(book.id).collectAsState(initial=emptyList())
                            ReaderScreen(book,pages,reading,accent,onBack={go(Screen.DETAIL)},onImport={importedManga=book.id;images.launch(arrayOf("image/*"))},onMode={scope.launch{settings.setReading(it)}},onProgress={page->scope.launch{dao.updateManga(book.copy(page=page,pages=pages.size,lastRead=System.currentTimeMillis()))}})
                        }else BlankState("افتح عملًا من المكتبة") }
                        Screen.REMOTE_READER -> {
                            val book=chosen
                            if(book!=null)ReaderScreen(book.copy(title=remoteTitle),remotePages,reading,accent,
                                onBack={go(Screen.DETAIL)},onImport={toast("للاحتفاظ بالصفحات، نزّل الفصل المصرّح به أولًا")},
                                onMode={scope.launch{settings.setReading(it)}},onProgress={})
                            else BlankState("لا توجد صفحات للفصل")
                        }
                        Screen.DOWNLOADS -> DownloadsScreen(accent,initialUrl=pendingDownloadUrl)
                        Screen.SETTINGS -> SettingsScreen(theme,reading,accent,onTheme={scope.launch{settings.setTheme(it);motion.show(ZZMotionAssets.themeChanged,"تم تطبيق الثيم")}},onReading={scope.launch{settings.setReading(it);motion.show(ZZMotionAssets.pageFlip,"تم تغيير نمط القراءة")}},onBackup={go(Screen.BACKUP)},onDownloads={go(Screen.DOWNLOADS)})
                        Screen.BACKUP -> BackupScreen(sources.size,manga.size,accent,onExport={export.launch("MangaZZ-Backup.json")},onRestore={restore.launch(arrayOf("application/json","text/plain"))})
                    }
                }
                ZZEventFeedback(motion.event, Modifier.align(Alignment.TopCenter).padding(horizontal=18.dp,vertical=8.dp))
            }
            if(screen!=Screen.READER && screen!=Screen.REMOTE_READER && screen!=Screen.BROWSER) {
                val bottom=listOf(Triple(Screen.HOME,Icons.Default.Home,"الرئيسية"),Triple(Screen.SOURCES,Icons.Default.Language,"المصادر"),Triple(Screen.LIBRARY,Icons.Default.CollectionsBookmark,"المكتبة"),Triple(Screen.DOWNLOADS,Icons.Default.Download,"تنزيلات"),Triple(Screen.SETTINGS,Icons.Default.Settings,"الإعدادات"))
                Row(Modifier.fillMaxWidth().background(colors.surface).border(1.dp,colors.border).padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceAround) {
                    bottom.forEach{(page,icon,title)->
                        val active=page==screen
                        Column(Modifier.weight(1f).clickable{if(screen!=page){go(page);motion.show(ZZMotionAssets.tabSwitch,"${page.ar}")}}.padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Icon(icon,title,tint=if(active)accent else TextDim,modifier=Modifier.size(25.dp))
                            Text(title,color=if(active)accent else TextDim,fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
}
}
