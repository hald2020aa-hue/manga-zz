package com.mangazz.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangazz.app.R
import kotlinx.coroutines.delay

/** Only emit success after the corresponding operation actually succeeds. */
data class ZZFeedback(val nonce: Long, val lottie: Int, val message: String)

@Stable
class ZZMotionController {
    private var nonce = 0L
    var event by mutableStateOf<ZZFeedback?>(null)
        private set
    fun show(lottie: Int, message: String) {
        event = ZZFeedback(++nonce, lottie, message)
    }
}

val LocalZZMotionController = staticCompositionLocalOf<ZZMotionController> {
    error("Manga ZZ motion controller not provided")
}

/** Finite event-driven motion, removed after completion. */
@Composable
fun ZZEventFeedback(event: ZZFeedback?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<ZZFeedback?>(null) }
    LaunchedEffect(event?.nonce) {
        shown = event
        if(event != null) {
            delay(1650)
            if (shown?.nonce == event.nonce) shown = null
        }
    }
    AnimatedVisibility(visible = shown != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val current = shown
        val palette = LocalZzPalette.current
        if(current != null) key(current.nonce) {
            V2Card(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZZMotion(current.lottie, Modifier.size(46.dp), repeat = false)
                    Spacer(Modifier.width(9.dp))
                    Text(current.message, color = palette.text, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Icons are vectors tinted with the active theme rather than raster screenshots. */
@Composable
fun ZZIconAction(label: String, iconRes: Int, onClick: () -> Unit,
    modifier: Modifier = Modifier, selected: Boolean = false) {
    val colors = LocalZzPalette.current
    V2Card(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(iconRes), contentDescription = null,
                tint = if (selected) colors.accent else colors.secondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            V2Action(label, onClick, Modifier.weight(1f), selected)
        }
    }
}

object ZZMotionAssets {
    val sourceSaved = R.raw.xw_source_added
    val favorite = R.raw.xw_favorite_heart
    val bookmark = R.raw.xw_bookmark_saved
    val downloadStarted = R.raw.xw_download_start
    val downloadCompleted = R.raw.xw_download_done
    val translationCompleted = R.raw.xw_translation_bubble
    val backupCompleted = R.raw.xw_cloud_sync
    val failed = R.raw.xw_network_error
    val pageFlip = R.raw.xw_page_flip
    val tabSwitch = R.raw.xw_tab_switch
    val themeChanged = R.raw.xw_moon_theme
}
