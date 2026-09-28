package com.glass.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
        .map {
            AppItem(
                it.loadLabel(pm).toString(),
                it.activityInfo.packageName,
                it.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
            )
        }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}

fun openApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
}

fun openSettings(ctx: Context, action: String) {
    ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Modifier.glass(radius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
}

@Composable
fun AppIcon(app: AppItem, showLabel: Boolean = true) {
    val ctx = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { openApp(ctx, app.pkg) }
            .padding(6.dp)
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
        )
        if (showLabel) {
            Text(
                app.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun Tile(emoji: String, label: String, action: String) {
    val ctx = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(76.dp)
            .glass(22.dp)
            .clickable { openSettings(ctx, action) }
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun LauncherScreen() {
    val ctx = LocalContext.current
    val apps = remember { loadApps(ctx) }
    var drawer by remember { mutableStateOf(false) }
    var control by remember { mutableStateOf(false) }
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

    BackHandler(enabled = drawer || control) {
        drawer = false
        control = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -120f) {
                            if (control) control = false else drawer = true
                        } else if (total > 120f) {
                            if (drawer) drawer = false else control = true
                        }
                    },
                    onDragCancel = {},
                    onVerticalDrag = { _, delta -> total += delta }
                )
            }
    ) {
        // HOME
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                time,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 60.dp)
            )
            Text(date, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)

            Box(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(30.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                apps.take(4).forEach { AppIcon(it, showLabel = false) }
            }
            Text(
                "Upar swipe: Apps  |  Neeche swipe: Control Centre",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // APP DRAWER (swipe up)
        AnimatedVisibility(
            visible = drawer,
            enter = slideInVertically(animationSpec = tween(400)) { it / 2 } +
                fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f),
            exit = slideOutVertically(animationSpec = tween(300)) { it / 2 } +
                fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp, 60.dp, 16.dp, 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps) { AppIcon(it) }
                }
            }
        }

        // CONTROL CENTRE (swipe down)
        AnimatedVisibility(
            visible = control,
            enter = slideInVertically(animationSpec = tween(400)) { -it } + fadeIn(tween(400)),
            exit = slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut(tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
                Column(
                    modifier = Modifier
                        .padding(20.dp, 56.dp, 20.dp, 20.dp)
                        .fillMaxWidth()
                        .glass(32.dp)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Control Centre", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Tile("📶", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS)
                        Tile("🔵", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)
                        Tile("☀️", "Display", Settings.ACTION_DISPLAY_SETTINGS)
                        Tile("🔊", "Sound", Settings.ACTION_SOUND_SETTINGS)
                    }
                }
            }
        }
    }
}
