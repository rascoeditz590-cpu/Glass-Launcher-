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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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

// ---------- Data ----------

data class AppItem(val label: String, val packageName: String, val icon: ImageBitmap)
data class Wall(val name: String, val colors: List<Color>)
data class GlassTheme(val name: String, val emoji: String, val accent: Color, val wall: Int)

val Ink = Color(0xFF0F1B33)
val InkSoft = Color(0xFF5B6B86)

val wallpapers = listOf(
    Wall("Sky", listOf(Color(0xFFE9F3FF), Color(0xFFCFE2FF), Color(0xFFB9D0FF))),
    Wall("Aurora", listOf(Color(0xFFE6FAF3), Color(0xFFCDEFF4), Color(0xFFD6D4FF))),
    Wall("Blossom", listOf(Color(0xFFFFF1F6), Color(0xFFFFD8E8), Color(0xFFE4D8FF))),
    Wall("Ocean", listOf(Color(0xFFE3F6FF), Color(0xFFBFE6F5), Color(0xFF9FD0F0))),
    Wall("Sunset", listOf(Color(0xFFFFF1E0), Color(0xFFFFD3B5), Color(0xFFFFB8C8))),
    Wall("Minimal", listOf(Color(0xFFFAFAFB), Color(0xFFEEF0F4), Color(0xFFE0E4EB))),
    Wall("Lavender", listOf(Color(0xFFF1EBFF), Color(0xFFDCD0FF), Color(0xFFC6D4FF))),
    Wall("Sand", listOf(Color(0xFFFFF8EC), Color(0xFFF6E7CF), Color(0xFFEBD5B8)))
)

val themes = listOf(
    GlassTheme("Glass Blue", "💠", Color(0xFF2F6BFF), 0),
    GlassTheme("Aurora", "🌌", Color(0xFF14A38B), 1),
    GlassTheme("Blossom", "🌸", Color(0xFFE0457B), 2),
    GlassTheme("Ocean", "🌊", Color(0xFF0A8FD0), 3),
    GlassTheme("Sunset", "🌇", Color(0xFFF26B3A), 4),
    GlassTheme("Minimal", "⚪", Color(0xFF1F2937), 5)
)

fun loadApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppItem(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm).toBitmap(120, 120).asImageBitmap()
            )
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}

fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
    }
}

fun readBattery(context: Context): Int {
    val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (level >= 0 && scale > 0) level * 100 / scale else 0
}

// ---------- Small building blocks ----------

fun Modifier.glass(radius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(6.dp, shape, ambientColor = Color(0x22224488), spotColor = Color(0x33224488))
        .clip(shape)
        .background(Color.White.copy(alpha = 0.60f))
        .border(1.dp, Color.White.copy(alpha = 0.95f), shape)
}

@Composable
fun T(
    text: String,
    size: Int = 14,
    color: Color = Ink,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = align
        )
    )
}

@Composable
fun FeatureCard(
    emoji: String,
    title: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .glass(22.dp)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { T(emoji, 22) }
        Spacer(Modifier.width(10.dp))
        Column {
            T(title, 15, Ink, true, maxLines = 1)
            T(sub, 11, InkSoft, maxLines = 1)
        }
    }
}

@Composable
fun ControlTile(emoji: String, label: String, action: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .glass(20.dp)
                .clickable { openSettings(context, action) },
            contentAlignment = Alignment.Center
        ) { T(emoji, 22) }
        Spacer(Modifier.height(6.dp))
        T(label, 11, InkSoft, align = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun ScreenHeader(title: String, sub: String) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
        T(title, 28, Ink, true)
        T(sub, 14, InkSoft)
    }
}

// ---------- Root ----------

@Composable
fun LauncherApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("glass", Context.MODE_PRIVATE) }
    var themeIdx by remember { mutableStateOf(prefs.getInt("theme", 0).coerceIn(0, themes.size - 1)) }
    var wallIdx by remember { mutableStateOf(prefs.getInt("wall", 0).coerceIn(0, wallpapers.size - 1)) }
    var tab by remember { mutableStateOf(0) }
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { loadApps(context) }
    }

    BackHandler(enabled = tab != 0) { tab = 0 }

    val accent by animateColorAsState(themes[themeIdx].accent)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(wallpapers[wallIdx].colors))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = tab) { t ->
                    when (t) {
                        0 -> HomeTab(apps, accent) { tab = it }
                        1 -> AppsTab(apps)
                        2 -> ThemesTab(themeIdx, accent) { i ->
                            themeIdx = i
                            wallIdx = themes[i].wall
                            prefs.edit().putInt("theme", i).putInt("wall", wallIdx).apply()
                        }
                        3 -> WallpapersTab(wallIdx, accent) { i ->
                            wallIdx = i
                            prefs.edit().putInt("wall", i).apply()
                        }
                        else -> ControlCenterTab(accent)
                    }
                }
            }
            BottomBar(tab, accent) { tab = it }
        }
    }
}

@Composable
fun BottomBar(selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    val items = listOf("🏠" to "Home", "🔲" to "Apps", "🎨" to "Themes", "🖼️" to "Wallpapers")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .glass(28.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, (emoji, label) ->
            val on = i == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (on) accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                T(emoji, 16)
                if (on) {
                    Spacer(Modifier.width(4.dp))
                    T(label, 11, Color.White, true, maxLines = 1)
                }
            }
        }
    }
}

// ---------- Home ----------

@Composable
fun HomeTab(apps: List<AppItem>, accent: Color, goTab: (Int) -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var battery by remember { mutableStateOf(readBattery(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            battery = readBattery(context)
            delay(10_000)
        }
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    val time = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row {
                T("Glass ", 26, Ink, true)
                T("Launcher", 26, accent, true)
            }
            T("Customize • Personalize • Enjoy", 13, InkSoft)
            Spacer(Modifier.height(16.dp))

            // Clock + battery widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(28.dp)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    T(greeting, 15, InkSoft)
                    T(time, 56, Ink, true)
                    T(date, 14, InkSoft)
                }
                Column(horizontalAlignment = Alignment.End) {
                    T("🔋 $battery%", 16, Ink, true)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(battery.coerceIn(0, 100) / 100f)
                                .fillMaxHeight()
                                .background(accent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard("🎨", "Themes", "Stylish themes", accent, Modifier.weight(1f)) { goTab(2) }
                FeatureCard("🖼️", "Wallpapers", "Clean & bright", accent, Modifier.weight(1f)) { goTab(3) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard("🔲", "All Apps", "${apps.size} apps", accent, Modifier.weight(1f)) { goTab(1) }
                FeatureCard("🎛️", "Control Center", "Quick settings", accent, Modifier.weight(1f)) {
                    goTab(4)
                }
            }

            Spacer(Modifier.height(20.dp))
            T("Quick Controls", 16, Ink, true)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlTile("📶", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS)
                ControlTile("🔵", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)
                ControlTile("✈️", "Airplane", Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                ControlTile("📍", "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                ControlTile("🔆", "Display", Settings.ACTION_DISPLAY_SETTINGS)
                ControlTile("🔊", "Sound", Settings.ACTION_SOUND_SETTINGS)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glass(28.dp)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            apps.take(5).forEach { app ->
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { launchApp(context, app.packageName) }
                )
            }
        }
    }
}

// ---------- Apps ----------

@Composable
fun AppsTab(apps: List<AppItem>) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("All Apps", "${apps.size} apps installed")
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps) { app ->
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launchApp(context, app.packageName) }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    T(app.label, 11, Ink, align = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

// ---------- Themes ----------

@Composable
fun ThemesTab(selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Themes", "Change the look of your phone")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(themes.size) { i ->
                val t = themes[i]
                Column(
                    modifier = Modifier
                        .glass(22.dp)
                        .then(
                            if (i == selected) Modifier.border(2.dp, accent, RoundedCornerShape(22.dp))
                            else Modifier
                        )
                        .clickable { onPick(i) }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(wallpapers[t.wall].colors))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(t.accent),
                            contentAlignment = Alignment.Center
                        ) { T(t.emoji, 13) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        T(t.name, 14, Ink, true)
                        if (i == selected) T("  ✓", 14, t.accent, true)
                    }
                }
            }
        }
    }
}

// ---------- Wallpapers ----------

@Composable
fun WallpapersTab(selected: Int, accent: Color, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Wallpapers", "Clean & bright backgrounds")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement
