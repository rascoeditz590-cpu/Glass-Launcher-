package com.glass.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen() }
    }
}

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
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

val glassFill = Color.White.copy(alpha = 0.15f)
val glassBorder = Color.White.copy(alpha = 0.30f)

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var wallpaperIndex by remember { mutableStateOf(0) }

    val wallpapers = remember {
        listOf(
            Brush.verticalGradient(listOf(Color(0xFF1B2A49), Color(0xFF6A4C93))),
            Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF2C5364))),
            Brush.verticalGradient(listOf(Color(0xFF41295A), Color(0xFF2F0743))),
            Brush.verticalGradient(listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B))),
            Brush.verticalGradient(listOf(Color(0xFF134E5E), Color(0xFF71B280)))
        )
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { loadApps(context) }
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    val dragState = rememberDraggableState { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpapers[wallpaperIndex])
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (velocity < -800f) drawerOpen = true
                    else if (velocity > 800f) drawerOpen = false
                }
            )
    ) {
        if (!drawerOpen) {
            HomeScreen(
                apps = apps,
                onOpenDrawer = { drawerOpen = true },
                onChangeWallpaper = {
                    wallpaperIndex = (wallpaperIndex + 1) % wallpapers.size
                }
            )
        } else {
            DrawerScreen(apps = apps)
        }
    }
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(glassFill)
            .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = TextStyle(color = Color.White, fontSize = 14.sp))
    }
}

@Composable
fun HomeScreen(
    apps: List<AppItem>,
    onOpenDrawer: () -> Unit,
    onChangeWallpaper: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.height(24.dp))
            BasicText(
                "Glass Launcher",
                style = TextStyle(color = Color.White, fontSize = 28.sp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                GlassButton("Wallpaper") { onChangeWallpaper() }
                GlassButton("All apps") { onOpenDrawer() }
            }

            Box(modifier = Modifier.height(16.dp))

            // Dock: first 4 apps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(glassFill)
                    .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                apps.take(4).forEach { app ->
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { launchApp(context, app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerScreen(apps: List<AppItem>) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(apps) { app ->
                Column(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launchApp(context, app.packageName) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(52.dp)
                    )
                    BasicText(
                        app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
