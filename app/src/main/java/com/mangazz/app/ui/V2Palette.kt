package com.mangazz.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

/** One central, consistent palette: surfaces, borders, gradients, actions and selection all adapt. */
data class ZzPalette(
    val background: Color, val surface: Color, val card: Color, val border: Color,
    val accent: Color, val accentSoft: Color, val text: Color, val secondary: Color
)
fun v2Palette(id: String): ZzPalette {
    val glow = when(id){
        "red"->Color(0xFFFF506D)
        "green"->Color(0xFF3FE7B1)
        "purple"->Color(0xFFC086FF)
        "gold"->Color(0xFFFFCB69)
        else->Color(0xFF2BCBFF)
    }
    val back=when(id){
        "red"->Color(0xFF130912)
        "green"->Color(0xFF051613)
        "purple"->Color(0xFF0C0A20)
        "gold"->Color(0xFF17130B)
        else->Color(0xFF050D19)
    }
    val surface=when(id){
        "red"->Color(0xFF2B101E)
        "green"->Color(0xFF0C2B24)
        "purple"->Color(0xFF23133A)
        "gold"->Color(0xFF2B2415)
        else->Color(0xFF102844)
    }
    return ZzPalette(back,surface,surface.copy(alpha=.90f),glow.copy(alpha=.38f),glow,glow.copy(alpha=.18f),Color(0xFFF5F8FF),Color(0xFFAFC0DC))
}
val LocalZzPalette = staticCompositionLocalOf { v2Palette("blue") }

@Composable fun V2Card(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    val p=LocalZzPalette.current
    Column(modifier.background(Brush.verticalGradient(listOf(p.surface,p.background)),RoundedCornerShape(18.dp))
        .border(BorderStroke(1.dp,p.border),RoundedCornerShape(18.dp)).padding(14.dp),content=content)
}
@Composable fun V2Action(title:String,onClick:()->Unit,modifier:Modifier=Modifier,selected:Boolean=false){
    val p=LocalZzPalette.current
    Box(modifier.background(if(selected)p.accentSoft else p.surface,RoundedCornerShape(13.dp))
        .border(1.dp,if(selected)p.accent else p.border,RoundedCornerShape(13.dp))
        .clickable(onClick=onClick).padding(horizontal=13.dp, vertical=12.dp)){
        Text(title,color=p.text,fontSize=13.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
    }
}
