package com.mangazz.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.mangazz.app.data.*
import com.mangazz.app.ui.*

/** Responsive layout: 2+ columns on phones and more on unfolded foldables. */
@Composable fun LibraryScreenV2(books:List<Manga>,folders:List<Folder>,accent:Color,
    onSelect:(Manga)->Unit,onFavorite:(Manga)->Unit,onAdd:(String,String)->Unit,
    onFolder:(String)->Unit,onDelete:(Manga)->Unit,onMove:(Manga,String)->Unit){
    val pal=LocalZzPalette.current
    var search by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("الكل") }
    var sort by remember { mutableStateOf("الأحدث") }
    var showAdd by remember{mutableStateOf(false)}
    var showFolder by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")}
    var url by remember{mutableStateOf("https://")}
    var folderName by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    var moving by remember{mutableStateOf<Manga?>(null)}
    var deleting by remember{mutableStateOf<Manga?>(null)}
    val filtered=books.filter { b->b.title.contains(search,true)&&when(type){
        "الكل"->true;"المفضلة"->b.favorite;"مكتمل"->b.pages>0&&b.page>=b.pages
        "مستمرة"->b.page>0 && (b.pages==0||b.page<b.pages);"غير مقروءة"->b.page==0
        else->b.folder==type
    } }
    val shown=when(sort){"الاسم"->filtered.sortedBy{it.title.lowercase()};"التقدم"->filtered.sortedByDescending{it.page.toFloat()/it.pages.coerceAtLeast(1)};else->filtered.sortedWith(compareByDescending<Manga>{it.lastRead}.thenBy{it.title})}
    Column(Modifier.fillMaxSize().padding(12.dp)){
        Text("المكتبة",color=pal.text,fontSize=25.sp)
        Text("${books.size} عمل محفوظ • ${books.count{it.favorite}} مفضلة",color=pal.secondary,fontSize=12.sp)
        ZZTextField(search,{search=it},"ابحث في مكتبتك",Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.padding(vertical=7.dp)){
            V2Action("＋ إضافة عمل",{showAdd=true},Modifier.weight(1f),selected=true)
            V2Action("＋ مجلد",{showFolder=true},Modifier.weight(1f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            (listOf("الكل","المفضلة","مكتمل","مستمرة","غير مقروءة")+folders.map{it.name}).distinct().forEach{label->V2Action(label,{type=label},selected=type==label)}
        }
        Row(Modifier.padding(vertical=6.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf("الأحدث","الاسم","التقدم").forEach{label->V2Action("ترتيب: $label",{sort=label},selected=sort==label)}
        }
        if(shown.isEmpty())V2Card(Modifier.fillMaxWidth()){Text("لا توجد أعمال بهذا التصنيف",color=pal.secondary)}
        LazyVerticalGrid(columns=GridCells.Adaptive(165.dp),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            items(shown,key={it.id}) {b-> V2Card(Modifier.fillMaxWidth()){
                Box(Modifier.fillMaxWidth().height(140.dp).clickable{onSelect(b)},contentAlignment=Alignment.Center){
                    if(b.cover.isNotBlank())AsyncImage(model=b.cover,contentDescription=b.title,modifier=Modifier.fillMaxSize())
                    else Text("📖",fontSize=40.sp)
                }
                Text(b.title,color=pal.text,maxLines=2,fontSize=14.sp)
                Text("${b.page} من ${b.pages} صفحة",color=pal.secondary,fontSize=11.sp)
                if(b.pages>0)LinearProgressIndicator(progress={b.page.toFloat()/b.pages.coerceAtLeast(1)},color=pal.accent,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    V2Action(if(b.favorite)"♥" else "♡",{onFavorite(b)},Modifier.weight(1f))
                    V2Action("مجلد",{moving=b},Modifier.weight(1f))
                    V2Action("حذف",{deleting=b},Modifier.weight(1f))
                }
            }}
        }
    }
    if(showAdd)AlertDialog(onDismissRequest={showAdd=false},title={Text("إضافة عمل")},text={Column{
        ZZTextField(title,{title=it},"العنوان",Modifier.fillMaxWidth())
        ZZTextField(url,{url=it},"رابط HTTPS",Modifier.fillMaxWidth())
        if(error.isNotBlank())Text(error,color=Color.Red)
    }},confirmButton={TextButton(onClick={val v=runCatching{safeWebUrl(url)}.getOrNull();if(title.isBlank()||v==null)error="أدخل اسمًا ورابط HTTPS" else {onAdd(title.trim(),v);showAdd=false}}){Text("حفظ")}},dismissButton={TextButton(onClick={showAdd=false}){Text("إلغاء")}})
    if(showFolder)AlertDialog(onDismissRequest={showFolder=false},title={Text("مجلد جديد")},text={ZZTextField(folderName,{folderName=it},"اسم المجلد",Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={if(folderName.isNotBlank()){onFolder(folderName.trim());showFolder=false;folderName=""}}){Text("إضافة")}},dismissButton={TextButton(onClick={showFolder=false}){Text("إلغاء")}})
    moving?.let{b->AlertDialog(onDismissRequest={moving=null},title={Text("نقل العمل للمجلد")},text={Column(Modifier.heightIn(max=240.dp).verticalScroll(rememberScrollState())){
        (listOf("الكل")+folders.map{it.name}).distinct().forEach{f->TextButton(onClick={onMove(b,f);moving=null}){Text(f)}}
    }},confirmButton={TextButton(onClick={moving=null}){Text("إلغاء")}})}
    deleting?.let{b->AlertDialog(onDismissRequest={deleting=null},title={Text("حذف العمل؟")},text={Text(b.title)},confirmButton={TextButton(onClick={onDelete(b);deleting=null}){Text("حذف")}},dismissButton={TextButton(onClick={deleting=null}){Text("إلغاء")}})}
}
