package com.mangazz.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.mangazz.app.data.safeWebUrl
import com.mangazz.app.ui.*
import java.util.UUID

private class ZZTab(val id:String=UUID.randomUUID().toString(), val web:WebView, var url:String)

@SuppressLint("SetJavaScriptEnabled")
@Composable fun BrowserScreen(initialUrl:String,accent:Color,onSave:(String,String)->Unit,onDownload:(String)->Unit={}){
    val ctx=LocalContext.current
    val motion=LocalZZMotionController.current
    val tabs=remember { mutableStateListOf<ZZTab>() }
    var active by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf(initialUrl) }
    var title by remember { mutableStateOf("موقع مانجا") }
    var load by remember { mutableIntStateOf(0) }
    var fullscreen by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    fun addTab(url:String){
        val validated=runCatching{safeWebUrl(url)}.getOrElse{error="الرابط يجب أن يبدأ بـ HTTPS";motion.show(ZZMotionAssets.failed,"الرابط غير صالح");return}
        val web=WebView(ctx).apply {
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.javaScriptCanOpenWindowsAutomatically=false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.cacheMode=WebSettings.LOAD_DEFAULT
            settings.safeBrowsingEnabled=true
            webViewClient=object:WebViewClient(){
                override fun onPageStarted(view:WebView?,url:String?,favicon:Bitmap?){load=5;error="";if(url!=null)input=url}
                override fun onPageFinished(view:WebView?,url:String?){load=100;title=view?.title?:"موقع مانجا";if(url!=null)input=url}
                override fun onReceivedError(view:WebView?,request:WebResourceRequest?,e:WebResourceError?){if(request?.isForMainFrame==true)error="تعذر تحميل الموقع. تأكد من الشبكة وإمكانية عرض الموقع داخل WebView.";motion.show(ZZMotionAssets.failed,"فشل فتح الموقع")}
                override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?):Boolean{
                    val u=request?.url?:return false
                    return u.scheme !in setOf("https")
                }
            }
            webChromeClient=object:WebChromeClient(){override fun onProgressChanged(view:WebView?,newProgress:Int){load=newProgress}}
            loadUrl(validated)
        }
        tabs.add(ZZTab(web=web,url=validated));active=tabs.lastIndex;input=validated;motion.show(ZZMotionAssets.tabSwitch,"تبويب جديد")
    }
    DisposableEffect(Unit){
        onDispose { tabs.forEach { t->t.web.stopLoading();(t.web.parent as? ViewGroup)?.removeView(t.web);t.web.destroy() };tabs.clear() }
    }
    LaunchedEffect(initialUrl){
        if(tabs.isEmpty())addTab(initialUrl)
        else if(tabs.getOrNull(active)?.web?.url!=initialUrl) addTab(initialUrl)
    }
    val tab=tabs.getOrNull(active)
    fun select(index:Int){if(active!=index)motion.show(ZZMotionAssets.tabSwitch,"تم تبديل التبويب");active=index;val selected=tabs.getOrNull(index);input=selected?.web?.url?:selected?.url.orEmpty();title=selected?.web?.title?:"موقع مانجا"}
    BackHandler(enabled=tab?.web?.canGoBack()==true){tab?.web?.goBack()}
    Column(Modifier.fillMaxSize().padding(horizontal=8.dp)){
        if(!fullscreen){
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                tabs.forEachIndexed { i,t -> NeonButton("${i+1} · ${t.web.title?.take(13)?:"تبويب"}",accent,{select(i)},selected=i==active) }
                IconButton(onClick={addTab("https://mangadex.org/")}){Icon(Icons.Default.Add,"تبويب جديد",tint=accent)}
                IconButton(onClick={
                    if(tabs.size>1){val remove=tabs.removeAt(active);remove.web.destroy();select((active-1).coerceAtLeast(0))}
                    else {tab?.web?.loadUrl("about:blank");input=""}
                }){Icon(Icons.Default.Close,"إغلاق التبويب",tint=TextDim)}
            }
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={tab?.web?.goBack()}){Icon(Icons.Default.ArrowBack,null,tint=TextBright)}
                IconButton(onClick={tab?.web?.goForward()}){Icon(Icons.Default.ArrowForward,null,tint=TextBright)}
                OutlinedTextField(input,{input=it},modifier=Modifier.weight(1f),singleLine=true,textStyle=MaterialTheme.typography.bodySmall,
                    colors=OutlinedTextFieldDefaults.colors(focusedTextColor=TextBright,unfocusedTextColor=TextBright),
                    trailingIcon={IconButton(onClick={if(tab!=null){try{tab.web.loadUrl(safeWebUrl(input))}catch(_:Exception){error="رابط غير صالح"}}}){Icon(Icons.Default.ArrowCircleRight,"افتح الرابط",tint=accent)}})
                IconButton(onClick={tab?.web?.reload()}){Icon(Icons.Default.Refresh,"تحديث",tint=accent)}
                IconButton(onClick={onSave(title,tab?.web?.url?:input)}){Icon(Icons.Default.BookmarkAdd,"حفظ في المكتبة",tint=accent)}
                IconButton(onClick={fullscreen=true}){Icon(Icons.Default.Fullscreen,"ملء الشاشة",tint=TextBright)}
            }
        } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){IconButton(onClick={fullscreen=false}){Icon(Icons.Default.FullscreenExit,"إظهار الأدوات",tint=accent)}}
        AnimatedVisibility(load in 1..99){Row(verticalAlignment=Alignment.CenterVertically){
            ZZMotion(com.mangazz.app.R.raw.xw_loading_orbit,Modifier.size(32.dp),repeat=true)
            LinearProgressIndicator(progress={load/100f},modifier=Modifier.weight(1f),color=accent)
        }}
        if(error.isNotBlank()) Text(error,color=Color(0xFFFF788C),fontSize=12.sp,modifier=Modifier.padding(8.dp))
        // Each WebView instance remains alive while other tabs are selected.
        if(tab!=null) key(tab.id) { AndroidView(factory={
            (tab.web.parent as? ViewGroup)?.removeView(tab.web)
            tab.web
        },update={ view->
            if(view!==tab.web){ /* AndroidView is keyed with the tab ID below; no redirect needed. */ }
        },modifier=Modifier.weight(1f).fillMaxWidth().background(Color.White)) }
        if(!fullscreen && tab!=null) Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            ZZIconAction("حفظ في المكتبة",com.mangazz.app.R.drawable.ic_zz_save,{onSave(tab.web.title?:"صفحة مانجا",tab.web.url?:input)},Modifier.weight(1f),selected=true)
            ZZIconAction("تنزيل الرابط",com.mangazz.app.R.drawable.ic_zz_download,{onDownload(tab.web.url?:input)},Modifier.weight(1f))
        }
    }
}
