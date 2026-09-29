package com.glass.launcher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        setContent { LauncherApp() }
    }
}

data class AppItem(val label: String, val packageName: String, val icon: ImageBitmap)
data class WallSpec(val name: String, val style: Int, val hue: Float, val dark: Boolean)
data class GlassTheme(val name: String, val emoji: String, val accent: Color, val wall: Int, val card: Int)
data class GlassColors(val fill: Color, val border: Color)
class Pal(val bg: Color, val mid: Color, val glow: Color, val rim: Color)

val Ink = Color(0xFF0F1B33)
val InkSoft = Color(0xFF5B6B86)
val InkFixed = Color(0xFF0E1A32)

val LocalInk = compositionLocalOf { Color(0xFF0F1B33) }
val LocalInkSoft = compositionLocalOf { Color(0xFF5B6B86) }
val LocalGlass = compositionLocalOf {
    GlassColors(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.95f))
}

fun hsvColor(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hh, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
}

val wallStyleNames = listOf("Orb", "Lens", "Waves", "Aura", "Eclipse")
val allWalls: List<WallSpec> = List(100) { n ->
    val style = n % 5
    val p = n / 5
    WallSpec("${wallStyleNames[style]} ${p + 1}", style, (p * 18f + style * 7f) % 360f, p % 2 == 0)
}

val themeAdj = listOf("Midnight", "Frost", "Ember", "Ocean", "Neon", "Velvet", "Coral", "Lunar", "Solar", "Misty")
val themeNoun = listOf("Glass", "Bloom", "Pulse", "Drift", "Aura", "Echo", "Prism", "Haze", "Wave", "Glow")
val themeEmoji = listOf("💠", "🌸", "💓", "🌫️", "✨", "🔔", "🔷", "☁️", "🌊", "🌟")

val themes: List<GlassTheme> = List(100) { i ->
    val wIdx = (i * 7) % 100
    val sp = allWalls[wIdx]
    GlassTheme(
        "${themeAdj[i % 10]} ${themeNoun[(i / 10) % 10]}",
        themeEmoji[(i / 10) % 10],
        hsvColor(sp.hue + 20f, 0.75f, if (sp.dark) 1f else 0.8f),
        wIdx, i % 3
    )
}

fun palette(spec: WallSpec): Pal {
    val h = spec.hue
    return if (spec.dark) Pal(
        hsvColor(h, 0.90f, 0.07f), hsvColor(h, 0.85f, 0.50f),
        hsvColor(h + 25f, 0.55f, 1f), hsvColor(h + 40f, 0.25f, 1f)
    ) else Pal(
        hsvColor(h, 0.08f, 0.98f), hsvColor(h, 0.30f, 0.92f),
        hsvColor(h + 25f, 0.55f, 0.85f), Color.White
    )
}

fun DrawScope.drawWall(spec: WallSpec) {
    val w = size.width
    val h = size.height
    val p = palette(spec)
    val hue = spec.hue
    val dark = spec.dark
    when (spec.style) {
        0 -> {
            drawRect(Brush.verticalGradient(listOf(p.mid, p.bg, p.bg)))
            val c = Offset(w / 2f, h * 0.5f)
            val r = w * 0.46f
            val inner = if (dark) p.bg else hsvColor(hue, 0.35f, 0.75f)
            drawCircle(Brush.radialGradient(0f to inner, 1f to p.glow, center = c, radius = r), r, c)
            drawCircle(p.rim.copy(alpha = 0.7f), r, c, Stroke(w * 0.005f))
        }
        1 -> {
            drawRect(Brush.linearGradient(listOf(p.mid, p.bg)))
            val a = Offset(-w * 0.05f, h * 0.55f)
            val b = Offset(w * 1.0f, h * 0.40f)
            drawCircle(Brush.radialGradient(0f to p.bg, 1f to p.glow, center = a, radius = w * 0.75f), w * 0.75f, a)
            drawCircle(Brush.radialGradient(0f to p.glow, 1f to p.bg, center = b, radius = w * 0.70f), w * 0.70f, b)
        }
        else -> {
            drawRect(Brush.verticalGradient(listOf(p.mid, p.bg)))
            val path = Path().apply {
                moveTo(0f, h * 0.5f)
                cubicTo(w * 0.3f, h * 0.3f, w * 0.7f, h * 0.7f, w, h * 0.5f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(p.mid, p.bg)))
        }
    }
}

@Composable
fun WallCanvas(spec: WallSpec, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawWall(spec) }
}

fun loadApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppItem(
                it.loadLabel(pm).toString(),
                it.activityInfo.packageName,
                it.loadIcon(pm).toBitmap(120, 120).asImageBitmap()
            )
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, pkg: String) {
    context.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}

fun openSettings(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
}

fun readBattery(context: Context): Int {
    val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (level >= 0 && scale > 0) level * 100 / scale else 0
}

fun Modifier.glass(radius: Dp = 24.dp): Modifier = composed {
    val shape = RoundedCornerShape(radius)
    val g = LocalGlass.current
    this.shadow(6.dp, shape).clip(shape).background(g.fill).border(1.dp, g.border, shape)
}

@Composable
fun T(text: String, size: Int = 14, color: Color = Ink, bold: Boolean = false, align: TextAlign = TextAlign.Start, maxLines: Int = Int.MAX_VALUE) {
    BasicText(
        text = text, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = when (color) { Ink -> LocalInk.current; InkSoft -> LocalInkSoft.current; else -> color },
            fontSize = size.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, textAlign = align
        )
    )
}

@Composable
fun FeatureCard(emoji: String, title: String, sub: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier = modifier.glass(22.dp).clickable { onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(0.16f)), contentAlignment = Alignment.Center) { T(emoji, 22) }
        Spacer(Modifier.width(10.dp))
        Column { T(title, 15, Ink, true, maxLines = 1); T(sub, 11, InkSoft, maxLines = 1) }
    }
}

@Composable
fun ControlTile(emoji: String, label: String, action: String) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp).glass(20.dp).clickable { openSettings(context, action) }, contentAlignment = Alignment.Center) { T(emoji, 22) }
        Spacer(Modifier.height(6.dp))
        T(label, 11, InkSoft, align = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun ScreenHeader(title: String, sub: String) {
    Column(Modifier.padding(20.dp, 20.dp, 20.dp, 12.dp)) { T(title, 28, Ink, true); T(sub, 14, InkSoft) }
}

@Composable
fun LauncherApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("glass", Context.MODE_PRIVATE) }
    var themeIdx by remember { mutableStateOf(prefs.getInt("theme", 0).coerceIn(0, themes.size - 1)) }
    var wallIdx by remember { mutableStateOf(prefs.getInt("wall", 0).coerceIn(0, allWalls.size - 1)) }
    var tab by remember { mutableStateOf(0) }
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }

    LaunchedEffect(Unit) { apps = withContext(Dispatchers.Default) { loadApps(context) } }
    BackHandler(enabled = tab != 0) { tab = 0 }

    val accent by animateColorAsState(themes[themeIdx].accent)
    val spec = allWalls[wallIdx]
    val dark = spec.dark
    val ink = if (dark) Color(0xFFF2F6FF) else Color(0xFF0F1B33)
    val inkSoft = if (dark) Color(0xFFB4C0D8) else Color(0xFF5B6B86)
    val glassColors = when (themes[themeIdx].card) {
        1 -> if (dark) GlassColors(Color(0xFF1B2333).copy(0.92f), Color.White.copy(0.18f)) else GlassColors(Color.White.copy(0.92f), Color.White)
        2 -> GlassColors(accent.copy(if (dark) 0.22f else 0.16f), accent.copy(0.45f))
        else -> if (dark) GlassColors(Color.White.copy(0.12f), Color.White.copy(0.28f)) else GlassColors(Color.White.copy(0.60f), Color.White.copy(0.95f))
    }

    CompositionLocalProvider(LocalInk provides ink, LocalInkSoft provides inkSoft, LocalGlass provides glassColors) {
        Box(Modifier.fillMaxSize()) {
            WallCanvas(spec, Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Crossfade(targetState = tab) { t ->
                        when (t) {
                            0 -> HomeTab(apps, accent) { tab = it }
                            1 -> AppsTab(apps)
                            2 -> ThemesTab(themeIdx, accent) { i -> themeIdx = i; wallIdx = themes[i].wall; prefs.edit().putInt("theme", i).putInt("wall", wallIdx).apply() }
                            3 -> WallpapersTab(wallIdx, accent) { i -> wallIdx = i; prefs.edit().putInt("wall", i).apply() }
                            else -> ControlCenterTab(accent)
                        }
                    }
                }
                BottomBar(tab, accent) { tab = it }
            }
        }
    }
}

@Composable
fun BottomBar(selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    val items = listOf("🏠" to "Home", "🔲" to "Apps", "🎨" to "Themes", "🖼️" to "Wallpapers")
    Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp).glass(28.dp).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { i, (emoji, label) ->
            val on = i == selected
            Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (on) accent else Color.Transparent).clickable { onSelect(i) }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                T(emoji, 16)
                if (on) { Spacer(Modifier.width(4.dp)); T(label, 11, Color.White, true, maxLines = 1) }
            }
        }
    }
}

@Composable
fun HomeTab(apps: List<AppItem>, accent: Color, goTab: (Int) -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var battery by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) { while (true) { now = Date(); battery = readBattery(context); delay(10_000) } }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) { in 5..11 -> "Good Morning"; in 12..16 -> "Good Afternoon"; in 17..20 -> "Good Evening"; else -> "Good Night" }
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row { T("Glass ", 26, Ink, true); T("Launcher", 26, accent, true) }
        T("Customize • Personalize • Enjoy", 13, InkSoft)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().glass(28.dp).padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                T(greeting, 15, InkSoft)
                T(SimpleDateFormat("hh:mm", Locale.getDefault()).format(now), 56, Ink, true)
                T(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now), 14, InkSoft)
            }
            Column(horizontalAlignment = Alignment.End) { T("🔋 $battery%", 16, Ink, true) }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("🎨", "Themes", "Stylish themes", accent, Modifier.weight(1f)) { goTab(2) }
            FeatureCard("🖼️", "Wallpapers", "Clean & bright", accent, Modifier.weight(1f)) { goTab(3) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("🔲", "All Apps", "${apps.size} apps", accent, Modifier.weight(1f)) { goTab(1) }
            FeatureCard("🎛️", "Control Center", "Quick settings", accent, Modifier.weight(1f)) { goTab(4) }
        }
        Spacer(Modifier.height(20.dp))
        T("Quick Controls", 16, Ink, true)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlTile("📶", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS)
            ControlTile("🔵", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)
            ControlTile("✈️", "Airplane", Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            ControlTile("📍", "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ControlTile("🔆", "Display", Settings.ACTION_DISPLAY_SETTINGS)
            ControlTile("🔊", "Sound", Settings.ACTION_SOUND_SETTINGS)
        }
    }
}

@Composable
fun AppsTab(apps: List<AppItem>) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("All Apps", "${apps.size} apps installed")
        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(10.dp, 4.dp), modifier = Modifier.fillMaxSize()) {
            items(apps) { app ->
                Column(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable { launchApp(context, app.packageName) }.padding(8.dp, 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(4.dp))
                    T(app.label, 11, Ink, align = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ThemesTab(selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Themes", "100 themes available")
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(themes.size) { i ->
                val t = themes[i]
                Column(modifier = Modifier.glass(22.dp).then(if (i == selected) Modifier.border(2.dp, accent, RoundedCornerShape(22.dp)) else Modifier).clickable { onPick(i) }.padding(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp))) {
                        WallCanvas(allWalls[t.wall], Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(8.dp))
                    T(t.name, 14, Ink, true)
                }
            }
        }
    }
}

@Composable
fun WallpapersTab(selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Wallpapers", "100 wallpapers available")
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(allWalls.size) { i ->
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.62f).clip(RoundedCornerShape(18.dp)).border(if (i == selected) 3.dp else 1.dp, if (i == selected) accent else Color.White, RoundedCornerShape(18.dp)).clickable { onPick(i) }) {
                    WallCanvas(allWalls[i], Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun ControlCenterTab(accent: Color) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Control Center", "Quick access toggles")
        Column(modifier = Modifier.fillMaxWidth().glass(28.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ControlTile("📶", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS)
                ControlTile("🔵", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)
                ControlTile("🔆", "Display", Settings.ACTION_DISPLAY_SETTINGS)
                ControlTile("🔊", "Sound", Settings.ACTION_SOUND_SETTINGS)
            }
        }
    }
}
