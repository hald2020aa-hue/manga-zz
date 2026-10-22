package com.mangazz.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.mangazz.app.data.*
import com.mangazz.app.ui.*

@Composable private fun TitleBar(label:String,subtitle:String=""){
    Column(Modifier.fillMaxWidth().padding(bottom=12.dp)) {
        Headline(label)
        if(subtitle.isNotEmpty()) Text(subtitle,color=TextDim,fontSize=12.sp)
    }
}

@Composable fun HomeScreen(manga:List<Manga>,sources:List<Source>,accent:Color,onSources:()->Unit,onLibrary:()->Unit,onDetail:(Manga)->Unit,onBrowser:(String)->Unit){
    LazyColumn(contentPadding=PaddingValues(horizontal=15.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            NeonButton("المصادر",accent,onSources,Modifier.weight(1f))
            NeonButton("مكتبتي",accent,onLibrary,Modifier.weight(1f))
            NeonButton("الجديد",accent,onSources,Modifier.weight(1f))
        } }
        item { Glass(Modifier.fillMaxWidth()) {
            Headline("تابع القراءة")
            val current=manga.firstOrNull{it.lastRead>0}?:manga.firstOrNull()
            if(current==null) Text("احفظ أول عمل من المواقع أو المكتبة لتبدأ القراءة.",color=TextDim)
            else Column {Text(current.title,color=TextBright,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
                if(current.pages>0) LinearProgressIndicator(progress={current.page.toFloat()/current.pages.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp),color=accent)
                NeonButton("التفاصيل والمتابعة ←",accent,{onDetail(current)},selected=true)
            }
        } }
        item { Headline("المانجا المحفوظة") }
        if(manga.isEmpty()) item { BlankState("مكتبتك فارغة. احفظ عملًا أو أضف صورًا من جهازك.") }
        items(manga.take(16),key={it.id}) { book->MangaLine(book,accent,{onDetail(book)},null) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Headline("المصادر المضافة");TextButton(onClick=onSources){Text("عرض الكل")}} }
        if(sources.isEmpty()) item { BlankState("أضف موقع مانجا مسموحًا بتصفحه") }
        items(sources.take(8),key={"source-${it.id}"}){source->Glass(Modifier.fillMaxWidth().clickable{onBrowser(source.url)}) {
            Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Public,null,tint=accent);Spacer(Modifier.width(12.dp)); Column {Text(source.name,color=TextBright);Text(source.url,color=TextDim,fontSize=11.sp)} }
        }}
    }
}

@Composable private fun MangaLine(book:Manga,accent:Color,onClick:()->Unit,onFavorite:(()->Unit)?){
    Glass(Modifier.fillMaxWidth().clickable(onClick=onClick)) {
        Row(verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(width=75.dp,height=98.dp).background(Color(0xFF10243C),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){
                if(book.cover.isNotBlank()) AsyncImage(model=book.cover,contentDescription=book.title,modifier=Modifier.fillMaxSize())
                else Icon(Icons.Default.AutoStories,null,tint=accent,modifier=Modifier.size(34.dp))
            }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){
                Text(book.title,color=TextBright,fontSize=17.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text("صفحة ${book.page} من ${book.pages}",color=TextDim,fontSize=12.sp)
                if(book.pages>0){ Spacer(Modifier.height(5.dp));LinearProgressIndicator(progress={book.page.toFloat()/book.pages.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth(),color=accent) }
                Spacer(Modifier.height(6.dp)); Text(book.folder,color=accent,fontSize=12.sp)
            }
            if(onFavorite!=null) IconButton(onClick=onFavorite){Icon(if(book.favorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(book.favorite)Color.Red else TextDim)}
        }
    }
}

@Composable fun SourcesScreen(sources:List<Source>,accent:Color,onOpen:(String)->Unit,onAdd:(String,String)->Unit,onUpdate:(Source)->Unit,onDelete:(Source)->Unit){
    var query by remember {mutableStateOf("")}; var add by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var url by remember{mutableStateOf("")};var editing by remember{mutableStateOf<Source?>(null)}
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        TitleBar("المصادر","إدارة المواقع التي تختارها. حالة كل موقع غير مضمونة.")
        ZZTextField(query,{query=it},"ابحث عن موقع",Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton("+ إضافة موقع HTTPS",accent,{editing=null;name="";url="";add=true},Modifier.fillMaxWidth(),selected=true)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(sources.filter{it.name.contains(query,true)||it.url.contains(query,true)},key={it.id}){ s->
                Glass(Modifier.fillMaxWidth()) { Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Language,null,tint=accent)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f).clickable{onOpen(s.url)}){
                        Text(s.name,color=TextBright,fontWeight=FontWeight.Bold);Text(s.url,color=TextDim,fontSize=11.sp,maxLines=2)
                    }
                    IconButton(onClick={editing=s;name=s.name;url=s.url;add=true}){Icon(Icons.Default.Edit,"تعديل",tint=accent)}
                    IconButton(onClick={onDelete(s)}){Icon(Icons.Default.Delete,"حذف",tint=Color(0xFFFF6177))}
                }}
            }
            if(sources.isEmpty()) item{BlankState("ما في مصادر بعد. أضف موقعًا للبدء.")}
        }
    }
    if(add) AlertDialog(onDismissRequest={add=false},title={Text(if(editing==null)"إضافة مصدر" else "تعديل المصدر")},text={Column {
        ZZTextField(name,{name=it},"اسم الموقع",Modifier.fillMaxWidth())
        ZZTextField(url,{url=it},"https://example.org",Modifier.fillMaxWidth())
        Text("المواقع التي تحظر التصفح المدمج قد لا تعمل.",fontSize=11.sp,color=TextDim)
    }},confirmButton={TextButton(onClick={
        val valid = name.isNotBlank() && runCatching { safeWebUrl(url) }.isSuccess
        if(valid){
            val old=editing
            if(old!=null) onUpdate(old.copy(name=name.trim(),url=safeWebUrl(url)))
            else onAdd(name.trim(),url.trim())
            add=false
        }
    }) {Text("حفظ")}},dismissButton={TextButton(onClick={add=false}){Text("إلغاء")}})
}

@Composable fun LibraryScreen(manga:List<Manga>,folders:List<Folder>,accent:Color,onSelect:(Manga)->Unit,onFavorite:(Manga)->Unit,onAdd:(String,String)->Unit,onFolder:(String)->Unit,onDelete:(Manga)->Unit){
    var query by remember{mutableStateOf("")};var filter by remember{mutableStateOf("الكل")};var add by remember{mutableStateOf(false)};var newFolder by remember{mutableStateOf(false)};var title by remember{mutableStateOf("")};var url by remember{mutableStateOf("")};var folderName by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(14.dp)){
        TitleBar("المكتبة","كل أعمالك في مكان واحد — ${manga.size} عمل")
        ZZTextField(query,{query=it},"ابحث في المكتبة",Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            NeonButton("+ عمل",accent,{add=true},Modifier.weight(1f),selected=true)
            NeonButton("+ مجلد",accent,{newFolder=true},Modifier.weight(1f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            (listOf("الكل","المفضلة")+folders.map{it.name}).distinct().forEach{f->NeonButton(f,accent,{filter=f},selected=filter==f)}
        }
        val entries=manga.filter{it.title.contains(query,true)&&(filter=="الكل"||(filter=="المفضلة"&&it.favorite)||it.folder==filter)}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(entries,key={it.id}){book->
                MangaLine(book,accent,{onSelect(book)},{onFavorite(book)})
            }
            if(entries.isEmpty()) item{BlankState("لا توجد أعمال بهذا التصنيف")}
        }
    }
    if(add) AlertDialog(onDismissRequest={add=false},title={Text("إضافة عمل للمكتبة")},text={Column{
        ZZTextField(title,{title=it},"اسم المانجا",Modifier.fillMaxWidth())
        ZZTextField(url,{url=it},"الرابط HTTPS",Modifier.fillMaxWidth())
        Text("بعد الإضافة افتح التفاصيل وأدخل صور الصفحات من جهازك.",fontSize=11.sp,color=TextDim)
    }},confirmButton={TextButton(onClick={if(title.isNotBlank()&&runCatching{safeWebUrl(url)}.isSuccess){onAdd(title,url);add=false}}){Text("إضافة")}},dismissButton={TextButton(onClick={add=false}){Text("إلغاء")}})
    if(newFolder) AlertDialog(onDismissRequest={newFolder=false},title={Text("مجلد جديد")},text={ZZTextField(folderName,{folderName=it},"اسم المجلد")},confirmButton={TextButton(onClick={if(folderName.isNotBlank()){onFolder(folderName.trim());newFolder=false;folderName=""}}){Text("إنشاء")}},dismissButton={TextButton(onClick={newFolder=false}){Text("إلغاء")}})
}

@Composable fun DetailScreen(book:Manga,pageCount:Int,accent:Color,onBack:()->Unit,onRead:()->Unit,onOpen:()->Unit,onPick:()->Unit,onFavorite:()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null,tint=accent)};Headline("تفاصيل المانجا")}
        Glass(Modifier.fillMaxWidth()){
            Text(book.title,fontSize=24.sp,color=TextBright,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Text("المصدر: ${book.url}",color=TextDim,fontSize=12.sp)
            Text("صفحات مستوردة: $pageCount",color=TextDim,fontSize=12.sp)
            if(pageCount>0) LinearProgressIndicator(progress={book.page.toFloat()/pageCount.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp),color=accent)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NeonButton("ابدأ القراءة",accent,onRead,Modifier.weight(1f),selected=true)
                NeonButton(if(book.favorite)"♥ محفوظ" else "♡ المفضلة",accent,onFavorite,Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp));NeonButton("إضافة صور الصفحات",accent,onPick,Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp));NeonButton("فتح الموقع الداخلي",accent,onOpen,Modifier.fillMaxWidth())
        }
        Headline("الفصول والصفحات")
        BlankState("لا يتم تخمين الفصول من أي موقع. القراءة المحلية تتطلب صورًا تختارها بنفسك.")
    }
}
