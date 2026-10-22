package com.mangazz.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.airbnb.lottie.compose.*
import com.mangazz.app.R

val Background = Color(0xFF040C18)
val SurfaceGlass = Color(0xFF0A192E)
val TextBright = Color(0xFFF2F7FF)
val TextDim = Color(0xFFA6BCE0)
val Edge = Color(0xFF1C568E)
fun themeAccent(name: String) = when (name) {
    "red" -> Color(0xFFFF465F)
    "green" -> Color(0xFF26E5A1)
    "purple" -> Color(0xFFAD69FF)
    "gold" -> Color(0xFFFFC357)
    else -> Color(0xFF20BCFF)
}
@Composable fun Glass(modifier: Modifier=Modifier, body: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Brush.verticalGradient(listOf(Color(0xFF112B4A), SurfaceGlass)), RoundedCornerShape(18.dp))
        .border(1.dp, Edge, RoundedCornerShape(18.dp)).padding(13.dp), content=body)
}
@Composable fun Headline(text:String) { Text(text, color=TextBright, fontSize=21.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(vertical=9.dp)) }
@Composable fun ZZTextField(value:String,onChange:(String)->Unit,label:String,modifier:Modifier=Modifier) {
    OutlinedTextField(value,onChange,modifier=modifier,label={ Text(label) },singleLine=true,colors=OutlinedTextFieldDefaults.colors(
        focusedTextColor=TextBright,unfocusedTextColor=TextBright,focusedLabelColor=TextDim,unfocusedLabelColor=TextDim,
        focusedBorderColor=Color(0xFF158BDF),unfocusedBorderColor=Edge,cursorColor=Color.Cyan),shape=RoundedCornerShape(16.dp))
}
@Composable fun NeonButton(text:String,accent:Color, onClick:()->Unit,modifier:Modifier=Modifier, selected:Boolean=false, enabled:Boolean=true) {
    val target=if(selected)accent.copy(alpha=.3f) else Color(0xFF10253F)
    val bg by animateColorAsState(target,label="button_color")
    Box(modifier.background(bg,RoundedCornerShape(15.dp)).border(1.dp,if(selected)accent else Edge,RoundedCornerShape(15.dp))
        .clickable(enabled=enabled,onClick=onClick).padding(horizontal=14.dp,vertical=11.dp),contentAlignment=Alignment.Center) {
        Text(text,color=if(enabled)TextBright else TextDim,fontSize=13.sp,maxLines=2)
    }
}
@Composable fun ZZMotion(res:Int,modifier:Modifier=Modifier,repeat:Boolean=false) {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
    val progress by animateLottieCompositionAsState(comp,iterations=if(repeat)LottieConstants.IterateForever else 1)
    LottieAnimation(comp,progress=progress,modifier=modifier)
}
@Composable fun BlankState(msg:String) { Glass(Modifier.fillMaxWidth()) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        ZZMotion(R.raw.xw_empty_library,Modifier.size(68.dp))
        Spacer(Modifier.width(8.dp))
        Text(msg,color=TextDim,fontSize=13.sp)
    }
} }
