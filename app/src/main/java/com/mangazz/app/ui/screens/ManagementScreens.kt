package com.mangazz.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mangazz.app.download.DownloadRepository
import com.mangazz.app.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DownloadsScreen(accent:Color,initialUrl:String=""){
    val ctx=LocalContext.current
    val motion=LocalZZMotionController.current
    val repo=remember { DownloadRepository(ctx) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val seenStates=remember { mutableStateMapOf<Long,String>() }
    LaunchedEffect(Unit){ while(isActive){ refresh++;delay(1500) } }
    val records=remember(refresh){ runCatching{repo.list()}.getOrElse{error=it.localizedMessage?:"تعذر قراءة سجل التنزيلات"; emptyList()} }
    LaunchedEffect(records) {
        for(entry in records){
            val old=seenStates.put(entry.id,entry.state)
            if(old!=null && old!=entry.state){
                when(entry.state){
                    "مكتمل"->motion.show(ZZMotionAssets.downloadCompleted,"اكتمل تنزيل ${entry.name}")
                    "فشل"->motion.show(ZZMotionAssets.failed,"فشل تنزيل ${entry.name}")
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Headline("التنزيلات")
        Text("تنزيل ملفات HTTPS المباشرة التي تسمح بها مواقعها. لا يتم استخراج فصول محمية أو تجاوز أذونات المواقع.",color=TextDim,fontSize=12.sp)
        Spacer(Modifier.height(8.dp))
        Glass(Modifier.fillMaxWidth()){
            ZZTextField(url,{url=it},"رابط ملف مباشر HTTPS",Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            ZZIconAction("بدء التنزيل",com.mangazz.app.R.drawable.ic_zz_download,{
                try{repo.enqueue(url);motion.show(ZZMotionAssets.downloadStarted,"دخل الملف قائمة التنزيل");url="";error="";refresh++}
                catch(e:Exception){error=e.localizedMessage?:"تعذر تنزيل هذا الرابط";motion.show(ZZMotionAssets.failed,"تعذّر بدء التنزيل")}
            },Modifier.fillMaxWidth(),selected=true)
            if(error.isNotBlank())Text(error,color=Color(0xFFFF7488),fontSize=12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("الملفات تُحفظ في مجلد تنزيلات التطبيق الخاص",color=TextDim,fontSize=11.sp)
        if(records.isEmpty()) {Spacer(Modifier.height(10.dp));BlankState("ما في تنزيلات حاليًا")}
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=12.dp)){
            items(records,key={it.id}){entry->Glass(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Download,null,tint=accent)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)){
                        Text(entry.name,color=TextBright,fontWeight=FontWeight.SemiBold)
                        Text("${entry.state} • ${entry.bytes.coerceAtLeast(0)/1024} كيلوبايت / ${if(entry.total>0) "${entry.total/1024} كيلوبايت" else "غير معروف"}",color=TextDim,fontSize=11.sp)
                    }
                    IconButton(onClick={repo.remove(entry.id);refresh++}){Icon(Icons.Default.Delete,"حذف سجل التنزيل والملف",tint=Color(0xFFFF6680))}
                }
                if(entry.state=="جارٍ التنزيل") ZZMotion(com.mangazz.app.R.raw.xw_download_progress,Modifier.size(40.dp),repeat=true)
                if(entry.total>0)LinearProgressIndicator(progress={entry.percent/100f},modifier=Modifier.fillMaxWidth().padding(top=6.dp),color=accent)
                if(entry.state=="فشل")Text("رمز الخطأ: ${entry.reason}",color=TextDim,fontSize=11.sp)
            }}
        }
    }
}

@Composable
fun SettingsScreen(theme:String,reading:String,accent:Color,onTheme:(String)->Unit,onReading:(String)->Unit,onBackup:()->Unit,onDownloads:()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
        Headline("الإعدادات والمظهر")
        Text("اختر ألوان الواجهة وتجربة القراءة",color=TextDim,fontSize=13.sp)
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Text("ألوان الثيم — خمسة خيارات",color=TextBright,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("blue" to "الأزرق", "red" to "الأحمر", "green" to "الأخضر", "purple" to "البنفسجي", "gold" to "الذهبي").forEach{(id,label)->
                    NeonButton(label,themeAccent(id),{onTheme(id)},selected=theme==id)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Text("اتجاه القراءة",color=TextBright,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NeonButton("عمودي",accent,{onReading("عمودي")},Modifier.weight(1f),selected=reading=="عمودي")
                NeonButton("أفقي",accent,{onReading("أفقي")},Modifier.weight(1f),selected=reading=="أفقي")
            }
        }
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Headline("النسخ الاحتياطي والاستعادة")
            Text("احفظ مكتبتك ومصادرك في ملف JSON محلي",color=TextDim,fontSize=12.sp)
            NeonButton("إدارة النسخ الاحتياطية",accent,onBackup,Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Headline("التنزيلات")
            NeonButton("فتح مدير التنزيلات",accent,onDownloads,Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Text("Manga ZZ — إصدار تطوير 0.2.0 • التشغيل داخل WebView يخضع لقيود الموقع الأصلي",color=TextDim,fontSize=12.sp)
    }
}

@Composable
fun BackupScreen(sourceCount:Int,mangaCount:Int,accent:Color,onExport:()->Unit,onRestore:()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
        Headline("النسخ الاحتياطي والاستعادة")
        Glass(Modifier.fillMaxWidth()){
            Text("بيانات التطبيق المحلية",color=TextBright,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("مصادر محفوظة: $sourceCount",color=TextDim)
            Text("أعمال المكتبة: $mangaCount",color=TextDim)
            Text("النسخة JSON تحتوي أسماء وروابط وتقدم القراءة، ولا تتضمن ملفات صور المانجا أو صلاحياتها بين الأجهزة.",color=TextDim,fontSize=12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.FileUpload,null,tint=accent);Spacer(Modifier.width(8.dp));Headline("تصدير JSON") }
            ZZIconAction("تصدير مكتبتك ومصادرك",com.mangazz.app.R.drawable.ic_zz_backup,onExport,Modifier.fillMaxWidth(),selected=true)
        }
        Spacer(Modifier.height(12.dp))
        Glass(Modifier.fillMaxWidth()){
            Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Restore,null,tint=accent);Spacer(Modifier.width(8.dp));Headline("استيراد JSON") }
            Text("تنبيه: الاستيراد يستبدل البيانات الموجودة، ولا يعيد تنزيل صور القراءة.",color=TextDim,fontSize=12.sp)
            Spacer(Modifier.height(9.dp))
            ZZIconAction("اختيار نسخة للاستعادة",com.mangazz.app.R.drawable.ic_zz_backup,onRestore,Modifier.fillMaxWidth())
        }
    }
}
