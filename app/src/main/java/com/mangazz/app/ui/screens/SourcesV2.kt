package com.mangazz.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.mangazz.app.data.Source
import com.mangazz.app.data.safeWebUrl
import com.mangazz.app.ui.*
import com.mangazz.integration.SourceRulesStore
import androidx.compose.ui.platform.LocalContext

/** Explicit save feedback, open/edit/delete and opt-in chapter selectors. */
@Composable fun SourcesScreenV2(sources:List<Source>,accent:Color,onOpen:(String)->Unit,
    onAdd:(String,String)->Unit,onUpdate:(Source)->Unit,onDelete:(Source)->Unit){
    val p=LocalZzPalette.current
    val motion=LocalZZMotionController.current
    val context=LocalContext.current
    val rules=remember(context) { SourceRulesStore(context) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Source?>(null) }
    var modal by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var ruleSource by remember { mutableStateOf<Source?>(null) }
    var chapters by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    var ruleError by remember { mutableStateOf("") }
    var confirmDelete by remember{mutableStateOf<Source?>(null)}
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("المصادر",color=p.text,fontSize=25.sp)
        Text("إضافة وحفظ المواقع مع إعدادات فصل اختيارية",color=p.secondary,fontSize=12.sp)
        ZZTextField(query,{query=it},"بحث عن موقع",Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        V2Action("＋ إضافة مصدر",{editing=null;title="";url="https://";error="";modal=true},Modifier.fillMaxWidth(),selected=true,iconRes=com.mangazz.app.R.drawable.ic_zz_sources)
        Spacer(Modifier.height(10.dp))
        if(sources.isEmpty())V2Card(Modifier.fillMaxWidth()){Text("ما في مصادر محفوظة. اضغط إضافة مصدر بالأعلى.",color=p.secondary)}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
            items(sources.filter{it.name.contains(query,true)||it.url.contains(query,true)},key={it.id}){source->
                V2Card(Modifier.fillMaxWidth()){
                    Text(source.name,color=p.text,fontSize=17.sp)
                    Text(source.url,color=p.secondary,fontSize=11.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
                        V2Action("فتح",{onOpen(source.url)},Modifier.weight(1f),selected=true)
                        V2Action("تعديل",{editing=source;title=source.name;url=source.url;error="";modal=true},Modifier.weight(1f))
                        V2Action("الفصول",{ruleSource=source;val old=rules.get(source.url);chapters=old?.chapterLinks?:"";pages=old?.pageImages?:"";ruleError=""},Modifier.weight(1f))
                        V2Action("حذف",{confirmDelete=source},Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if(modal)AlertDialog(onDismissRequest={modal=false},title={Text(if(editing==null)"إضافة المصدر" else "تعديل المصدر")},text={Column{
        ZZTextField(title,{title=it;error=""},"اسم الموقع",Modifier.fillMaxWidth())
        ZZTextField(url,{url=it;error=""},"عنوان HTTPS",Modifier.fillMaxWidth())
        if(error.isNotBlank())Text(error,color=Color(0xFFFB7588))
    }},confirmButton={TextButton(onClick={
        val valid=runCatching{safeWebUrl(url)}.getOrNull()
        when {
            title.isBlank()->error="اكتب اسم المصدر أولًا"
            valid==null->error="الرابط لازم يكون HTTPS صحيحًا"
            sources.any{it.url==valid && it.id!=editing?.id}->error="المصدر موجود بالفعل"
            else->{val old=editing;if(old!=null)onUpdate(old.copy(name=title.trim(),url=valid))else onAdd(title.trim(),valid);modal=false}
        }
    }){Text("حفظ المصدر")}},dismissButton={TextButton(onClick={modal=false}){Text("إلغاء")}})
    ruleSource?.let { s->AlertDialog(onDismissRequest={ruleSource=null},title={Text("إعداد فصول ${s.name}")},text={Column{
        Text("للمصادر التي تسمح بقراءة HTML الثابت فقط. ضع محدد CSS لرابط الفصل ولمحتوى الصورة.",fontSize=12.sp)
        ZZTextField(chapters,{chapters=it;ruleError=""},"محدد الفصل مثل a.chapter-link",Modifier.fillMaxWidth())
        ZZTextField(pages,{pages=it;ruleError=""},"محدد الصور مثل .reader img",Modifier.fillMaxWidth())
        if(ruleError.isNotBlank())Text(ruleError,color=Color(0xFFFB7588))
    }},confirmButton={TextButton(onClick={try{rules.save(s.url,chapters.trim(),pages.trim());ruleSource=null;motion.show(ZZMotionAssets.sourceSaved,"حُفظت قواعد الفصول")}catch(e:Exception){ruleError=e.message?:"خطأ في قواعد الموقع";motion.show(ZZMotionAssets.failed,"تعذّر حفظ قواعد الموقع")}}){Text("حفظ القواعد")}},dismissButton={TextButton(onClick={ruleSource=null}){Text("إلغاء")}}) }
    confirmDelete?.let {s->AlertDialog(onDismissRequest={confirmDelete=null},title={Text("حذف المصدر؟")},text={Text(s.name)},confirmButton={TextButton(onClick={onDelete(s);confirmDelete=null}){Text("حذف")}},dismissButton={TextButton(onClick={confirmDelete=null}){Text("إلغاء")}})}
}
