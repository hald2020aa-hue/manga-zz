package com.mangazz.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.mangazz.app.data.*
import com.mangazz.app.ui.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.launch

@Composable fun ReaderScreen(book:Manga,pages:List<Page>,reading:String,accent:Color,onBack:()->Unit,onImport:()->Unit,onMode:(String)->Unit,onProgress:(Int)->Unit){
    val ctx=LocalContext.current
    var ocr by remember { mutableStateOf(false) }; var translated by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf("日本語") }
    var recognized by remember{mutableStateOf("")};var translateResult by remember{mutableStateOf("")};var error by remember{mutableStateOf("")}
    var current by remember(book.id,pages.size){ mutableIntStateOf(book.page.coerceIn(0,(pages.size-1).coerceAtLeast(0))) }
    val translator=remember { Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.JAPANESE).setTargetLanguage(TranslateLanguage.ARABIC).build()) }
    DisposableEffect(translator){onDispose{translator.close()}}
    fun scan(){
        val page=pages.getOrNull(current)?:return
        if(page.imageUri.startsWith("https://")){
            error="التعرف على النص يحتاج تنزيل الصفحة أولًا، وبعدها افتحها محليًا"
            return
        }
        recognized="جارٍ التعرف على النص...";translateResult="";error=""
        try{
            val image=InputImage.fromFilePath(ctx,Uri.parse(page.imageUri))
            val detector=when(lang){
                "한국어"->TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                "中文"->TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                "English"->TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                else->TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            }
            detector.process(image).addOnSuccessListener { r->
                recognized=r.text.ifBlank{"لم يتم العثور على نص"}
                if(translated && r.text.isNotBlank()){
                    val source=when(lang){"한국어"->TranslateLanguage.KOREAN;"中文"->TranslateLanguage.CHINESE;"English"->TranslateLanguage.ENGLISH;else->TranslateLanguage.JAPANESE}
                    val temp=Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(TranslateLanguage.ARABIC).build())
                    temp.downloadModelIfNeeded().addOnSuccessListener{
                        temp.translate(r.text).addOnSuccessListener{translateResult=it}.addOnFailureListener{error="تعذر ترجمة النص"}.addOnCompleteListener{temp.close()}
                    }.addOnFailureListener {error="تعذر تنزيل نموذج الترجمة. تحقق من اتصال الإنترنت.";temp.close()}
                }
            }.addOnFailureListener{error="تعذر قراءة الصورة: ${it.localizedMessage}"}.addOnCompleteListener{detector.close()}
        }catch(e:Exception){error=e.localizedMessage?:"تعذر قراءة الصورة"}
    }
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null,tint=TextBright)}
            Column(Modifier.weight(1f)){Text(book.title,color=TextBright,maxLines=1);Text("صفحة ${if(pages.isEmpty())0 else current+1} / ${pages.size}",color=TextDim,fontSize=11.sp)}
            IconButton(onClick={onMode(if(reading=="عمودي")"أفقي" else "عمودي")}){Icon(if(reading=="عمودي") Icons.Default.ViewColumn else Icons.Default.ViewStream,"تغيير اتجاه القراءة",tint=accent)}
            IconButton(onClick=onImport){Icon(Icons.Default.AddPhotoAlternate,"إضافة صور",tint=accent)}
        }
        if(pages.isEmpty()){
            BlankState("استورد صور صفحات قانونية من جهازك أولًا")
            NeonButton("اختيار صور",accent,onImport,Modifier.fillMaxWidth())
        }else{
            if(reading=="عمودي"){
                val scroll= androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=current)
                LaunchedEffect(scroll.firstVisibleItemIndex){ val i=scroll.firstVisibleItemIndex;current=i;onProgress(i+1) }
                LazyColumn(Modifier.weight(1f),state=scroll){itemsIndexed(pages,key={_,p->p.position}){i,p-> ZoomPage(p.imageUri,Modifier.fillMaxWidth().heightIn(min=320.dp))}}
            }else{
                val scroll= androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=current)
                LaunchedEffect(scroll.firstVisibleItemIndex){ val i=scroll.firstVisibleItemIndex;current=i;onProgress(i+1) }
                LazyRow(Modifier.weight(1f),state=scroll){itemsIndexed(pages,key={_,p->p.position}){_,p->ZoomPage(p.imageUri,Modifier.fillParentMaxWidth().fillMaxHeight())}}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NeonButton(if(ocr)"OCR ●" else "OCR",accent,{ocr=!ocr; if(ocr)scan()},selected=ocr)
                NeonButton(if(translated)"ترجمة العربية ●" else "ترجمة العربية",accent,{translated=!translated;if(translated){ocr=true;scan()}},selected=translated)
                listOf("日本語","한국어","中文","English").forEach{l->NeonButton(l,accent,{lang=l;if(ocr)scan()},selected=lang==l)}
            }
            if(ocr) Glass(Modifier.fillMaxWidth().heightIn(max=200.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text("النص المكتشف (وليس استبدال الفقاعات)",color=TextBright,modifier=Modifier.weight(1f),fontSize=12.sp); TextButton(onClick=::scan){Text("إعادة فحص")}}
                Column(Modifier.verticalScroll(rememberScrollState())){
                    if(translated && translateResult.isNotBlank()) Text(translateResult,color=accent)
                    Text(recognized,color=TextDim,fontSize=13.sp)
                    if(error.isNotBlank()) Text(error,color=Color.Red,fontSize=12.sp)
                }
            }
        }
    }
}

@Composable private fun ZoomPage(uri:String,modifier:Modifier){
    var scale by remember(uri){mutableFloatStateOf(1f)}
    var x by remember(uri){mutableFloatStateOf(0f)}
    var y by remember(uri){mutableFloatStateOf(0f)}
    Box(modifier.clipToBounds().pointerInput(uri){detectTransformGestures { _, pan, zoom, _->
        scale=(scale*zoom).coerceIn(1f,4f);x=if(scale>1f)x+pan.x else 0f;y=if(scale>1f)y+pan.y else 0f
    }},contentAlignment=Alignment.Center){
        AsyncImage(model=uri,contentDescription="صفحة مانجا",modifier=Modifier.fillMaxWidth().graphicsLayer(scaleX=scale,scaleY=scale,translationX=x,translationY=y))
    }
}
