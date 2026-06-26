package com.qiuh0330.gdtools

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 系统状态栏/导航栏与应用配色一致
        window.statusBarColor = AndroidColor.parseColor("#F5F7FA")
        window.navigationBarColor = AndroidColor.WHITE
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            RocoTheme {
                RocoApp()
            }
        }
    }
}

@Composable
fun RocoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Primary,
            secondary = Primary,
            background = Color(0xFFF5F7FA),
            surface = Color.White,
        ),
        content = content,
    )
}

/** 蛋形线条图标（material 图标库没有蛋，用 Canvas 画椭圆） */
@Composable
private fun EggIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        drawOval(
            color = tint,
            topLeft = Offset(size.width * 0.24f, size.height * 0.12f),
            size = Size(size.width * 0.52f, size.height * 0.74f),
            style = Stroke(width = size.width * 0.075f),
        )
    }
}

/** 空心五角星线条图标（Outlined.Star 实际是实心的，自己画） */
@Composable
private fun StarOutlineIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.54f
        val outer = size.width * 0.44f
        val inner = outer * 0.42f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val angle = (-90.0 + i * 36.0) * PI / 180.0
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = tint, style = Stroke(width = size.width * 0.07f, join = StrokeJoin.Round))
    }
}

private val TABS: List<Pair<String, @Composable (Color) -> Unit>> = listOf(
    "图鉴" to { tint -> Icon(Icons.Outlined.Home, contentDescription = "图鉴", tint = tint) },
    "任务" to { tint -> Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "任务", tint = tint) },
    "蛋组" to { tint -> EggIcon(tint) },
    "收集" to { tint -> StarOutlineIcon(tint) },
    "设置" to { tint -> Icon(Icons.Outlined.Settings, contentDescription = "设置", tint = tint) },
)

@Composable
fun RocoApp() {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(Repo.isLoaded) }

    LaunchedEffect(Unit) {
        if (!Repo.isLoaded) {
            withContext(Dispatchers.IO) {
                Repo.load(context.applicationContext)
                Store.init(context.applicationContext)
            }
        }
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailPetId by rememberSaveable { mutableStateOf<Int?>(null) }

    val pokedexGridState = remember { LazyGridState() }
    val tasksListState = remember { LazyListState() }
    val eggListState = remember { LazyListState() }
    val collectionListState = remember { LazyListState() }
    val scope = rememberCoroutineScope()

    // 图鉴卡片点击：有进化分支时提示，否则跳到最终形态
    val onPetCardClick: (Pet) -> Unit = { pet ->
        if (pet.finalFormIds.size > 1) {
            Toast.makeText(context, "该精灵有进化分支，请点击具体精灵查看详情", Toast.LENGTH_SHORT).show()
        } else {
            detailPetId = pet.finalFormIds.firstOrNull() ?: pet.id
        }
    }
    val onShowPet: (Int) -> Unit = { detailPetId = it }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                TABS.forEachIndexed { index, (label, icon) ->
                    val tint = if (tab == index) Color.Black else Color(0xFF999999)
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            if (tab == index) {
                                scope.launch {
                                    when (index) {
                                        0 -> pokedexGridState.animateScrollToItem(0)
                                        1 -> tasksListState.animateScrollToItem(0)
                                        2 -> eggListState.animateScrollToItem(0)
                                        3 -> collectionListState.animateScrollToItem(0)
                                    }
                                }
                            } else {
                                tab = index
                            }
                        },
                        icon = { icon(tint) },
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color.Black,
                            unselectedIconColor = Color(0xFF999999),
                            unselectedTextColor = Color(0xFF999999),
                            indicatorColor = Color(0xFFEFEFEF),
                        ),
                    )
                }
            }
        },
        containerColor = Color(0xFFF5F7FA),
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> PokedexScreen(onPetCardClick, pokedexGridState)
                1 -> TasksScreen(tasksListState)
                2 -> EggScreen(onShowPet, eggListState)
                3 -> CollectionScreen(onShowPet, collectionListState)
                4 -> SettingsScreen()
            }
        }
    }

    // 详情整页覆盖在标签页之上（不卸载标签页内容，返回时保留滚动位置）
    detailPetId?.let { id ->
        PetDetailScreen(petId = id, onClose = { detailPetId = null })
    }
    } // Box

    // 启动时静默检查更新，有新版本才提示
    var autoUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        val info = withContext(Dispatchers.IO) { fetchUpdateInfo() }.getOrNull()
        if (info != null && info.versionCode > currentVersionCode(context)) {
            autoUpdate = info
        }
    }
    autoUpdate?.let { info ->
        UpdateDialog(info = info, onDismiss = { autoUpdate = null })
    }
}
