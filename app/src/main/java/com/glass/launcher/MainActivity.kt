package com.glass.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap)
data class WallpaperItem(val name: String, val category: String, val colors: List<Color>)
data class ControlStyle(val name: String, val bg: Brush, val btn: Color, val text: Color)

val WALLPAPERS = listOf(
    WallpaperItem("Aurora Blue", "Abstract", listOf(Color(0xFF5B8DFF), Color(0xFF8B6BFF))),
    WallpaperItem("Crystal Night", "Abstract", listOf(Color(0xFF1C1C3A), Color(0xFF5B8DFF))),
    WallpaperItem("Deep Amoled", "Abstract", listOf(Color(0xFF000000), Color(0xFF1A1A2E))),
    WallpaperItem("Mint Fog", "Abstract", listOf(Color(0xFF33E0C0), Color(0xFF5B8DFF))),
    WallpaperItem("Forest Mist", "Nature", listOf(Color(0xFF0F4C2E), Color(0xFF4C9A5B))),
    WallpaperItem("Ocean Breeze", "Nature", listOf(Color(0xFF134E5E), Color(0xFF71B280))),
    WallpaperItem("Golden Sunset", "Nature", listOf(Color(0xFFFF9A3C), Color(0xFFFF3C6A))),
    WallpaperItem("Earthy Canyon", "Nature", listOf(Color(0xFF8D5524), Color(0xFFC68642))),
    WallpaperItem("Sakura Dream", "Anime", listOf(Color(0xFFFF6B81), Color(0xFF5352ED))),
    WallpaperItem("Neo Tokyo", "Anime", listOf(Color(0xFF341F97), Color(0xFFFF6B6B))),
    WallpaperItem("Cyber Pink", "Anime", listOf(Color(0xFFFF9FF3), Color(0xFFFECA57))),
    WallpaperItem("Spirit Glow", "Anime", listOf(Color(0xFF1DD1A1), Color(0xFF341F97)))
)

val CONTROL_STYLES = listOf(
    ControlStyle("Glass UI", Brush.verticalGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.05f))), Color.White.copy(0.14f), Color.White),
    ControlStyle("Flat", Brush.verticalGradient(listOf(Color(0xFF2B2F3F), Color(0xFF1C1F2B))), Color(0xFF2B2F3F), Color.White),
    ControlStyle("Neon", Brush.verticalGradient(listOf(Color(0xFF1A0033), Color(0xFF00081A))), Color(0x33B300FF), Color.White),
    ControlStyle("Minimal", Brush.verticalGradient(listOf(Color(0xFFF0F0F0), Color(0xFFE0E0E0))), Color(0x11000000), Color.Black)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen() }
    }
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(128, 128).asImageBitmap()) }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}

fun openApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
}

fun openSettings(ctx: Context, action: String) {
    ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun applyWallpaper(ctx: Context, colors: List<Color>) {
    val width = ctx.resources.displayMetrics.widthPixels
    val height = ctx.resources.displayMetrics.heightPixels
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        colors.map { it.toArgb() }.toIntArray(),
        null, Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    WallpaperManager.getInstance(ctx).setBitmap(bitmap)
}

fun Modifier.glass(radius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))))
        .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
}

@Composable
fun AppIcon(app: AppItem, showLabel: Boolean = true) {
    val ctx = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { openApp(ctx, app.pkg) }.padding(6.dp)) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(15.dp)))
        if (showLabel) {
            Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun Tile(emoji: String, label: String, action: String, btnColor: Color, textColor: Color) {
    val ctx = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(btnColor).clickable { openSettings(ctx, action) }) {
        Text(emoji, fontSize = 24.sp)
        Text(label, color = textColor, fontSize = 11.sp)
    }
}

fun Modifier.swipeUpDown(onUp: () -> Unit, onDown: () -> Unit): Modifier = this.pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = { if (total < -120f) onUp() else if (total > 120f) onDown() },
        onDragCancel = {},
        onVerticalDrag = { _, delta -> total += delta }
    )
}

@Composable
fun LauncherScreen() {
    val ctx = LocalContext.current
    val apps = remember { loadApps(ctx) }
    var drawer by remember { mutableStateOf(false) }
    var control by remember { mutableStateOf(false) }
    var themeSheet by remember { mutableStateOf(false) }
    var themeTab by remember { mutableStateOf(0) }
    var wallCat by remember { mutableStateOf("Abstract") }
    var selectedControl by remember { mutableStateOf(CONTROL_STYLES[0]) }
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            time = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
            date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    BackHandler(enabled = drawer || control || themeSheet) {
        drawer = false; control = false; themeSheet = false
    }

    Box(modifier = Modifier.fillMaxSize().swipeUpDown(
        onUp = { if (control) control = false else drawer = true },
        onDown = { if (drawer) drawer = false else control = true }
    )) {

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.size(38.dp).glass(14.dp).clickable { themeSheet = true }, contentAlignment = Alignment.Center) {
                    Text("\u2728", fontSize = 16.sp)
                }
            }
            Text(time, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 10.dp))
            Text(date, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
            Box(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth().glass(30.dp).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                apps.take(4).forEach { AppIcon(it, showLabel = false) }
            }
            Text("Upar: Apps  |  Neeche: Control  |  \u2728: Themes", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        }

        AnimatedVisibility(visible = drawer,
            enter = slideInVertically(tween(400)) { it / 2 } + fadeIn(tween(400)) + scaleIn(tween(400), 0.92f),
            exit = slideOutVertically(tween(300)) { it / 2 } + fadeOut(tween(300)) + scaleOut(tween(300), 0.92f)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
                LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(16.dp, 60.dp, 16.dp, 24.dp), modifier = Modifier.fillMaxSize()) {
                    items(apps) { AppIcon(it) }
                }
            }
        }

        AnimatedVisibility(visible = control,
            enter = slideInVertically(tween(400)) { -it } + fadeIn(tween(400)),
            exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(20.dp, 56.dp, 20.dp, 20.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(selectedControl.bg).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Control Centre", color = selectedControl.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Tile("\uD83D\uDCF6", "Wi-Fi", android.provider.Settings.ACTION_WIFI_SETTINGS, selectedControl.btn, selectedControl.text)
                        Tile("\uD83D\uDD35", "Bluetooth", android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, selectedControl.btn, selectedControl.text)
                        Tile("\u2600\uFE0F", "Display", android.provider.Settings.ACTION_DISPLAY_SETTINGS, selectedControl.btn, selectedControl.text)
                        Tile("\uD83D\uDD0A", "Sound", android.provider.Settings.ACTION_SOUND_SETTINGS, selectedControl.btn, selectedControl.text)
                    }
                }
            }
        }

        AnimatedVisibility(visible = themeSheet,
            enter = slideInVertically(tween(400)) { it } + fadeIn(tween(400)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().glass(28.dp).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Themes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("Band karein", color = Color.White.copy(0.6f), fontSize = 13.sp, modifier = Modifier.clickable { themeSheet = false })
                    }
                    Row(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)) {
                        listOf("Wallpapers", "Control").forEachIndexed { i, label ->
                            Text(label, color = if (themeTab == i) Color.White else Color.White.copy(0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 20.dp).clickable { themeTab = i })
                        }
                    }
                    if (themeTab == 0) {
                        LazyRow(modifier = Modifier.padding(bottom = 12.dp)) {
                            items(listOf("Abstract", "Nature", "Anime")) { cat ->
                                Text(cat, color = if (wallCat == cat) Color.White else Color.White.copy(0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 16.dp).clickable { wallCat = cat })
                            }
                        }
                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(260.dp)) {
                            items(WALLPAPERS.filter { it.category == wallCat }) { w ->
                                Column(modifier = Modifier.padding(6.dp).clickable { applyWallpaper(ctx, w.colors); themeSheet = false }) {
                                    Box(modifier = Modifier.fillMaxWidth().size(120.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(w.colors)))
                                    Text(w.name, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    } else {
                        LazyRow {
                            items(CONTROL_STYLES) { style ->
                                Column(modifier = Modifier.padding(6.dp).clickable { selectedControl = style }) {
                                    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(18.dp)).background(style.bg).border(if (selectedControl.name == style.name) 2.dp else 0.dp, Color.White, RoundedCornerShape(18.dp)))
                                    Text(style.name, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
