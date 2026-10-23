package com.mangazz.app.ui.screens

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.mangazz.app.data.*
import com.mangazz.app.ui.*
import com.mangazz.integration.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/** User configured CSS selectors; only permitted static HTML sources can use this feature. */
@Composable fun ChapterSectionV2(book:Manga,accent:Color,
    onReadRemote:(List<MangaPage>,String)->Unit,
    onDownloadComplete:(String,List<File>)->Unit){
    val ctx=LocalContext.current
    val motion=LocalZZMotionController.current
    val scope=rememberCoroutineScope()
    val sourceRules=remember(ctx){SourceRulesStore(ctx)}
    val store=remember(ctx){ChapterStore(ctx)}
    val rules=sourceRules.get(book.url)
    var chapters by remember(book.url){mutableStateOf<List<MangaChapter>>(emptyList())}
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var completed by remember{mutableIntStateOf(0)}
    var total by remember{mutableIntStateOf(0)}
    var operation by remember{mutableStateOf<Job?>(null)}
    var cbzToShare by remember{mutableStateOf<File?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.comicbook+zip")){uri->
        val source=cbzToShare
        if(uri!=null && source!=null)scope.launch{runCatching{ctx.contentResolver.openOutputStream(uri)?.use{source.inputStream().use { input -> input.copyTo(it) }}}
            .onSuccess{status="تم تصدير CBZ";motion.show(ZZMotionAssets.downloadCompleted,"تم تصدير CBZ")}.onFailure{status="فشل تصدير CBZ: ${it.message}";motion.show(ZZMotionAssets.failed,"فشل تصدير CBZ")}}
    }
    fun configured():MangaSource?{
        val r=sourceRules.get(book.url)
        if(r==null){status="جهّز قواعد CSS من صفحة المصادر أولًا";return null}
        return CssMangaSource("المصدر المحدد",r,"MangaZZ/0.2 Android")
    }
    V2Card(Modifier.fillMaxWidth()){
        Text("الفصول الذكية (اختياري)",color=LocalZzPalette.current.text,fontSize=17.sp)
        Text("تعمل مع HTML ثابت عندما تكون محددات CSS مضبوطة ويسمح الموقع بذلك؛ لا تدعم تجاوز الحماية أو فصول JavaScript تلقائيًا.",color=LocalZzPalette.current.secondary,fontSize=12.sp)
        Spacer(Modifier.height(8.dp))
        V2Action("اكتشاف الفصول",{
            val source=configured()
            if(source!=null) operation=scope.launch{busy=true;status="جارٍ قراءة الفصول..."
                try{chapters=source.getChapters(book.url).take(300);status="عُثر على ${chapters.size} فصل";motion.show(ZZMotionAssets.sourceSaved,"اكتمل فحص الفصول")}
                catch(e:Exception){status="تعذر استخراج الفصول: ${e.localizedMessage}";motion.show(ZZMotionAssets.failed,"تعذّر استخراج الفصول")}finally{busy=false}}
        },Modifier.fillMaxWidth(),selected=true)
        if(status.isNotBlank())Text(status,color=if(status.startsWith("فشل")||status.startsWith("تعذر"))Color(0xFFFF748B) else LocalZzPalette.current.accent,fontSize=12.sp)
        if(busy){ZZMotion(com.mangazz.app.R.raw.xw_loading_bars,Modifier.size(44.dp),repeat=true)
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=accent)
            if(total>0) Text("$completed / $total صفحات",color=LocalZzPalette.current.secondary,fontSize=11.sp)
            V2Action("إلغاء العملية",{operation?.cancel();busy=false;status="تم إلغاء العملية"})
        }
        chapters.forEach {chapter->
            HorizontalDivider(color=LocalZzPalette.current.border)
            Text(chapter.title,color=LocalZzPalette.current.text,fontSize=13.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
                V2Action("قراءة",{
                    val source=configured()
                    if(source!=null)operation=scope.launch{busy=true;status="جارٍ جلب صفحات الفصل..."
                        try{val found=source.getPages(chapter);if(found.isEmpty())status="لا توجد صفحات" else {onReadRemote(found,chapter.title);status="${found.size} صفحة";motion.show(ZZMotionAssets.pageFlip,"تم فتح الفصل")}}
                        catch(e:Exception){status=e.localizedMessage?:"تعذر قراءة الفصل";motion.show(ZZMotionAssets.failed,"تعذّر فتح الفصل")}finally{busy=false}}
                },Modifier.weight(1f))
                V2Action("تنزيل",{
                    val source=configured()
                    if(source!=null)operation=scope.launch{busy=true;completed=0;total=0;status="جارٍ تنزيل الصفحات..."
                        try{
                            val found=source.getPages(chapter)
                            total=found.size
                            if(found.isEmpty()){status="لا توجد صفحات"}else{
                                val dir=store.downloadChapter(chapter.id,chapter.title,found,"MangaZZ/0.2 Android"){done,size->completed=done;total=size}
                                onDownloadComplete(chapter.title,store.getLocalPageFiles(chapter.id))
                                motion.show(ZZMotionAssets.downloadCompleted,"اكتمل تنزيل الفصل")
                                cbzToShare=store.exportCbz(chapter.id)
                                status="تم تنزيل ${store.getLocalPageFiles(chapter.id).size} صفحة؛ تصدير CBZ متاح أدناه"
                            }
                        }catch(e:CancellationException){status="تم إيقاف التنزيل";throw e}
                        catch(e:Exception){status="فشل التنزيل: ${e.localizedMessage}";motion.show(ZZMotionAssets.failed,"فشل تنزيل الفصل")}finally{busy=false}}
                },Modifier.weight(1f))
            }
        }
        if(cbzToShare!=null) V2Action("تصدير آخر فصل CBZ",{picker.launch("manga-chapter.cbz")},Modifier.fillMaxWidth(),selected=true)
    }
}
