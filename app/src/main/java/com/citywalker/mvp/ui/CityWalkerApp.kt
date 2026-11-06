package com.citywalker.mvp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citywalker.mvp.AppScreen
import com.citywalker.mvp.BuildConfig
import com.citywalker.mvp.CityWalkerUiState
import com.citywalker.mvp.CityWalkerViewModel
import com.citywalker.mvp.LocationStatus
import com.citywalker.mvp.MessageActionType
import com.citywalker.mvp.core.camera.CameraCapture
import com.citywalker.mvp.core.camera.CameraExposureInfo
import com.citywalker.mvp.core.camera.PhotoRepository
import com.citywalker.mvp.core.camera.surfaceRotationForDeviceOrientation
import com.citywalker.mvp.core.location.distanceBetweenMeters
import com.citywalker.mvp.core.location.formatDistance
import com.citywalker.mvp.core.map.TianDiTuProjection
import com.citywalker.mvp.core.navigation.NavigationLauncher
import com.citywalker.mvp.core.photo.PhotoBitmapLoader
import com.citywalker.mvp.core.share.ShareHelper
import com.citywalker.mvp.domain.Badge
import com.citywalker.mvp.domain.CaptureMode
import com.citywalker.mvp.domain.CityStats
import com.citywalker.mvp.domain.Dungeon
import com.citywalker.mvp.domain.DungeonProgress
import com.citywalker.mvp.domain.DungeonStatus
import com.citywalker.mvp.domain.GeoPoint
import com.citywalker.mvp.domain.ProgressEngine
import com.citywalker.mvp.domain.Viewpoint
import com.citywalker.mvp.domain.SurveyCapture
import com.citywalker.mvp.domain.SurveyPointType
import com.citywalker.mvp.domain.SurveySamplingState
import com.citywalker.mvp.domain.SurveyViewpointDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CityWalkerApp(viewModel: CityWalkerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.locate() }

    fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) viewModel.locate()
        else locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    CityWalkerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.content == null) {
                    LoadingScreen(state.message)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        if (state.screen != AppScreen.CAMERA && state.screen != AppScreen.MAP) {
                            AppTopBar(state, viewModel)
                        }
                        when (state.screen) {
                            AppScreen.MAP -> MapScreen(state, viewModel, ::requestLocation)
                            AppScreen.DETAIL -> DetailScreen(state, viewModel)
                            AppScreen.VIEWPOINT -> ViewpointScreen(state, viewModel)
                            AppScreen.CAMERA -> CameraScreen(state, viewModel)
                            AppScreen.RESULT -> ResultScreen(state, viewModel)
                            AppScreen.COLLECTION -> CollectionScreen(state, viewModel)
                            AppScreen.POSTCARD -> PostcardScreen(state, viewModel)
                            AppScreen.FIELD_SURVEY -> FieldSurveyScreen(state, viewModel, ::requestLocation)
                        }
                    }
                }

                state.message?.let { message ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        MessageBanner(
                            message = message,
                            actionLabel = state.messageAction?.label,
                            onAction = {
                                when (state.messageAction?.type) {
                                    MessageActionType.NAVIGATE_TO_SELECTED_DUNGEON -> viewModel.selectedDungeon()?.let { dungeon ->
                                        NavigationLauncher.open(context, dungeon, "amap")
                                    }
                                    null -> Unit
                                }
                                viewModel.clearMessage()
                            },
                            onDismiss = viewModel::clearMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(message: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("CITY WALKER", color = Orange, fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            CircularProgressIndicator(color = Orange, strokeWidth = 3.dp)
            Text(message ?: "正在打开长沙城市地图…", color = Sand)
        }
    }
}

@Composable
private fun AppTopBar(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val title = when (state.screen) {
        AppScreen.MAP -> "长沙城市地图"
        AppScreen.DETAIL -> viewModel.selectedDungeon()?.name ?: "副本详情"
        AppScreen.VIEWPOINT -> viewModel.selectedViewpoint()?.name ?: "经典机位"
        AppScreen.COLLECTION -> "我的图鉴"
        AppScreen.RESULT -> "现场记录"
        AppScreen.POSTCARD -> "城市明信片"
        AppScreen.CAMERA -> "拍摄"
        AppScreen.FIELD_SURVEY -> "Debug / 踩点模式"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.screen != AppScreen.MAP) {
            IconButton(onClick = viewModel::navigateBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Paper)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CITY WALKER", color = Orange, fontSize = 10.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                    Text(title, color = Paper, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Slate,
                    fontSize = 10.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.openFieldSurvey() })
                    }
                )
            }
        }
        if (state.screen != AppScreen.COLLECTION) {
            IconButton(onClick = viewModel::openCollection) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = "图鉴", tint = Seafoam)
            }
        }
    }
}

@Composable
private fun MapScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel, requestLocation: () -> Unit) {
    val content = state.content ?: return
    val stats = ProgressEngine.cityStats(content, state.progress)
    val focusedDungeon = (state.mapFocusedDungeonId ?: content.dungeons.firstOrNull()?.id)
        ?.let { id -> content.dungeons.firstOrNull { it.id == id } }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        ChangshaMap(
            dungeons = content.dungeons,
            state = state,
            mapCenter = GeoPoint(content.city.latitude, content.city.longitude),
            focusedDungeonId = focusedDungeon?.id,
            modifier = Modifier.fillMaxSize(),
            onDungeonClick = viewModel::focusDungeonOnMap
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Ink.copy(alpha = .76f), Color.Transparent, Ink.copy(alpha = .92f))
                    )
                )
                .background(Color(0xFF071A1D).copy(alpha = .28f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("长沙", color = Paper, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${stats.completedCount}/${stats.totalCount} 副本  ·  全城探索 ${stats.explorationPercent}%",
                    color = Sand,
                    style = MaterialTheme.typography.labelLarge
                )
                Text("天地图卫星 · 拖动探索 · 双指缩放 · 双击复位", color = Slate, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = viewModel::openCollection, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Seafoam, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("图鉴", color = Seafoam)
                }
                MapLocationButton(state, requestLocation)
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Slate,
                    fontSize = 10.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.openFieldSurvey() })
                    }
                )
            }
        }
        focusedDungeon?.let { dungeon ->
            MapDungeonSheet(
                dungeon = dungeon,
                progress = ProgressEngine.progressFor(state.progress, dungeon.id),
                distanceMeters = state.currentLocation?.let { location -> distanceBetweenMeters(location, dungeon.checkpoint.point) },
                onExplore = { viewModel.openDungeon(dungeon.id) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MapLocationButton(state: CityWalkerUiState, requestLocation: () -> Unit) {
    val label = when (state.locationStatus) {
        LocationStatus.READY, LocationStatus.SIMULATED -> "已定位"
        LocationStatus.LOCATING -> "定位中"
        else -> "定位"
    }
    Surface(
        modifier = Modifier.clickable(enabled = state.locationStatus != LocationStatus.LOCATING, onClick = requestLocation),
        shape = RoundedCornerShape(50),
        color = Ink.copy(alpha = .82f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .12f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = if (state.locationStatus == LocationStatus.ERROR) Coral else Orange, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = Paper, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MapDungeonSheet(
    dungeon: Dungeon,
    progress: DungeonProgress,
    distanceMeters: Float?,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF102C2F),
        shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(dungeon.name, color = Paper, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("${dungeon.category}  ·  ${dungeon.subtitle}", color = Sand, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusPill(progress.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallInfo("距离目的地", distanceMeters?.let(::formatDistance) ?: "未定位")
                SmallInfo("副本探索", "${progress.explorationPercent}%")
                SmallInfo("任务", "${dungeon.viewpoints.size + 1} 个")
            }
            Button(
                onClick = onExplore,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
            ) {
                Text("去探索")
                Spacer(Modifier.width(7.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun CityStatsStrip(stats: CityStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile("主线完成", "${stats.completedCount}/${stats.totalCount}", Modifier.weight(1f))
        StatTile("城市探索", "${stats.explorationPercent}%", Modifier.weight(1f))
        StatTile("状态", if (stats.cityComplete) "已通关" else "进行中", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = DeepTeal) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, color = Slate, style = MaterialTheme.typography.labelSmall)
            Text(value, color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LocationPanel(state: CityWalkerUiState, requestLocation: () -> Unit) {
    val statusText = when (state.locationStatus) {
        LocationStatus.IDLE -> "现场模式未开启"
        LocationStatus.LOCATING -> "正在寻找你的位置…"
        LocationStatus.READY -> "已连接现场定位"
        LocationStatus.SIMULATED -> "测试模式：已模拟到达"
        LocationStatus.ERROR -> state.locationError ?: "定位不可用"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF143235),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B5B58))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Orange.copy(alpha = .16f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Orange)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("现场定位", color = Paper, fontWeight = FontWeight.Bold)
                Text(statusText, color = if (state.locationStatus == LocationStatus.ERROR) Coral else Seafoam, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = requestLocation,
                enabled = state.locationStatus != LocationStatus.LOCATING,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Orange)
            ) {
                Text(if (state.locationStatus == LocationStatus.READY) "刷新" else "开启", color = Orange)
            }
        }
    }
}

private data class MapViewportState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

private fun clampMapViewport(viewport: MapViewportState, width: Float, height: Float): MapViewportState {
    if (width <= 0f || height <= 0f) return viewport
    val scaledWidth = width * viewport.scale
    val scaledHeight = height * viewport.scale
    val overscanX = width * .55f
    val overscanY = height * .55f
    val baseMinX = minOf(0f, width - scaledWidth)
    val baseMaxX = maxOf(0f, width - scaledWidth)
    val baseMinY = minOf(0f, height - scaledHeight)
    val baseMaxY = maxOf(0f, height - scaledHeight)
    return viewport.copy(
        offsetX = viewport.offsetX.coerceIn(baseMinX - overscanX, baseMaxX + overscanX),
        offsetY = viewport.offsetY.coerceIn(baseMinY - overscanY, baseMaxY + overscanY)
    )
}

@Composable
private fun ChangshaMap(
    dungeons: List<Dungeon>,
    state: CityWalkerUiState,
    mapCenter: GeoPoint,
    focusedDungeonId: String?,
    modifier: Modifier = Modifier,
    onDungeonClick: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.background(Color(0xFF102C2F))
    ) {
        val density = LocalDensity.current
        val mapWidthPx = with(density) { maxWidth.toPx() }
        val mapHeightPx = with(density) { maxHeight.toPx() }
        var viewport by remember { mutableStateOf(MapViewportState()) }

        LaunchedEffect(focusedDungeonId, maxWidth, maxHeight, mapCenter) {
            val focused = dungeons.firstOrNull { it.id == focusedDungeonId } ?: return@LaunchedEffect
            val focusedPoint = TianDiTuProjection.projectToMap(
                latitude = focused.checkpoint.latitude,
                longitude = focused.checkpoint.longitude,
                centerLatitude = mapCenter.latitude,
                centerLongitude = mapCenter.longitude,
                zoom = CHANGSHA_MAP_ZOOM,
                viewportWidthPx = mapWidthPx,
                viewportHeightPx = mapHeightPx
            )
            val targetX = mapWidthPx * .5f
            val targetY = mapHeightPx * .31f
            viewport = clampMapViewport(
                viewport.copy(
                    offsetX = targetX - focusedPoint.x * viewport.scale,
                    offsetY = targetY - focusedPoint.y * viewport.scale
                ),
                mapWidthPx,
                mapHeightPx
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mapWidthPx, mapHeightPx) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val current = viewport
                        val nextScale = (current.scale * zoom).coerceIn(.8f, 2.5f)
                        val scaleChange = nextScale / current.scale
                        viewport = clampMapViewport(
                            MapViewportState(
                                scale = nextScale,
                                offsetX = current.offsetX + pan.x + centroid.x * (1f - scaleChange),
                                offsetY = current.offsetY + pan.y + centroid.y * (1f - scaleChange)
                            ),
                            mapWidthPx,
                            mapHeightPx
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { viewport = MapViewportState() })
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = viewport.scale
                        scaleY = viewport.scale
                        translationX = viewport.offsetX
                        translationY = viewport.offsetY
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                TianDiTuRasterLayer(
                    centerLatitude = mapCenter.latitude,
                    centerLongitude = mapCenter.longitude,
                    viewportWidthPx = mapWidthPx,
                    viewportHeightPx = mapHeightPx,
                    scale = viewport.scale,
                    offsetX = viewport.offsetX,
                    offsetY = viewport.offsetY,
                    modifier = Modifier.fillMaxSize()
                )

                state.currentLocation?.let { location ->
                    val locationPoint = TianDiTuProjection.projectToMap(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        centerLatitude = mapCenter.latitude,
                        centerLongitude = mapCenter.longitude,
                        zoom = CHANGSHA_MAP_ZOOM,
                        viewportWidthPx = mapWidthPx,
                        viewportHeightPx = mapHeightPx
                    )
                    val locationHalfPx = with(density) { 11.dp.roundToPx() }
                    val locationLabelX = with(density) { 5.dp.roundToPx() }
                    val locationLabelY = with(density) { 14.dp.roundToPx() }
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(locationPoint.x.roundToInt() - locationHalfPx, locationPoint.y.roundToInt() - locationHalfPx)
                            }
                            .size(22.dp),
                        shape = CircleShape,
                        color = Coral,
                        border = androidx.compose.foundation.BorderStroke(3.dp, Paper)
                    ) { }
                    Text(
                        "你",
                        color = Paper,
                        fontSize = 10.sp,
                        modifier = Modifier.offset {
                            IntOffset(locationPoint.x.roundToInt() - locationLabelX, locationPoint.y.roundToInt() + locationLabelY)
                        }
                    )
                }

                dungeons.forEach { dungeon ->
                    val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
                    val focused = dungeon.id == focusedDungeonId
                    val markerColor = when (progress.status) {
                        DungeonStatus.COMPLETED_100 -> Seafoam
                        DungeonStatus.MAIN_COMPLETED, DungeonStatus.EXPLORING -> Orange
                        DungeonStatus.ARRIVED -> Coral
                        DungeonStatus.LOCKED -> Slate
                    }
                    val markerSize = if (focused) 44.dp else 34.dp
                    val markerPoint = TianDiTuProjection.projectToMap(
                        latitude = dungeon.checkpoint.latitude,
                        longitude = dungeon.checkpoint.longitude,
                        centerLatitude = mapCenter.latitude,
                        centerLongitude = mapCenter.longitude,
                        zoom = CHANGSHA_MAP_ZOOM,
                        viewportWidthPx = mapWidthPx,
                        viewportHeightPx = mapHeightPx
                    )
                    val markerHalfPx = with(density) { markerSize.roundToPx() / 2 }
                    val markerLiftPx = with(density) { 7.dp.roundToPx() }
                    Column(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    markerPoint.x.roundToInt() - markerHalfPx,
                                    markerPoint.y.roundToInt() - markerHalfPx - markerLiftPx
                                )
                            }
                            .zIndex(if (focused) 2f else 1f)
                            .clickable { onDungeonClick(dungeon.id) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(markerSize).shadow(if (focused) 12.dp else 7.dp, CircleShape),
                            shape = CircleShape,
                            color = if (focused) Orange else markerColor,
                            border = androidx.compose.foundation.BorderStroke(if (focused) 4.dp else 3.dp, if (focused) Paper else Ink.copy(alpha = .8f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (progress.status == DungeonStatus.LOCKED) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Ink, modifier = Modifier.size(if (focused) 18.dp else 15.dp))
                                } else {
                                    Text("✦", color = Ink, fontSize = if (focused) 20.sp else 17.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            dungeon.name,
                            color = if (focused) Paper else Sand,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Text("湘 江", color = Seafoam.copy(alpha = .72f), fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
        Text("CITY WALKER / 长沙探索区", color = Paper.copy(alpha = .58f), fontSize = 10.sp, letterSpacing = 1.3.sp, modifier = Modifier.padding(18.dp))
        Text(
            "天地图 · 多时相影像优先",
            color = Paper.copy(alpha = .48f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 194.dp)
        )
    }
}

@Composable
private fun DungeonListItem(
    dungeon: Dungeon,
    progress: DungeonProgress,
    distanceMeters: Float?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = DeepTeal
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ImageThumb(dungeon.coverImage, Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dungeon.name, color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    StatusPill(progress.status)
                }
                Text(dungeon.subtitle, color = Sand, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("副本探索 ${progress.explorationPercent}%", color = Orange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    distanceMeters?.let { Text(formatDistance(it), color = Slate, style = MaterialTheme.typography.labelSmall) }
                }
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Slate, modifier = Modifier.padding(start = 7.dp).size(18.dp))
        }
    }
}

@Composable
private fun DetailScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val dungeon = viewModel.selectedDungeon() ?: return
    val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
    val context = LocalContext.current
    val mainCompleted = ProgressEngine.isMainCompleted(progress)
    val distanceMeters = state.currentLocation?.let { location ->
        distanceBetweenMeters(location, dungeon.checkpoint.point)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            DungeonHero(dungeon, progress)
            DungeonProgressRail(dungeon, progress)
            MainTaskCard(
                dungeon = dungeon,
                progress = progress,
                currentlyAtCheckpoint = viewModel.isCurrentlyAtCheckpoint(dungeon.id),
                currentAccuracyMeters = state.currentLocation?.accuracyMeters,
                distanceMeters = distanceMeters,
                onCapture = viewModel::startMainCapture,
                onSimulateArrival = viewModel::simulateArrival
            )
            FreeCaptureSection(
                progress = progress,
                onCapture = viewModel::startFreeCapture
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { NavigationLauncher.open(context, dungeon, "amap") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("去这里")
                }
                OutlinedButton(
                    onClick = { NavigationLauncher.copyLocation(context, dungeon) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制坐标")
                }
            }
            SectionHeader("经典机位", "横向滑动选择关卡 · 完成主线后解锁")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                dungeon.viewpoints.forEachIndexed { index, viewpoint ->
                    ViewpointStageCard(
                        index = index + 2,
                        total = dungeon.viewpoints.size + 1,
                        viewpoint = viewpoint,
                        progress = progress.viewpointProgress[viewpoint.id],
                        enabled = mainCompleted,
                        onClick = { viewModel.openViewpoint(dungeon.id, viewpoint.id) }
                    )
                }
            }
            SectionHeader("副本印章", "完成关卡后收入城市图鉴")
            BadgeShelf(dungeon.badges, progress.unlockedBadges)
        }
    }
}

@Composable
private fun FreeCaptureSection(
    progress: DungeonProgress,
    onCapture: () -> Unit
) {
    val mainCompleted = ProgressEngine.isMainCompleted(progress)
    val captures = progress.freeCaptures
    var selectedPath by remember(captures.size) { mutableStateOf(captures.lastOrNull()?.filePath) }
    Surface(shape = RoundedCornerShape(22.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("自由探索", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("记录我的发现 · 不计入官方探索", color = Slate, style = MaterialTheme.typography.bodySmall)
                }
                Text("${captures.size} 处", color = Orange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallInfo("官方探索", "${progress.explorationPercent}%", Modifier.weight(1f))
                SmallInfo("我的发现", "${captures.size} 处", Modifier.weight(1f))
            }
            if (captures.isNotEmpty()) {
                selectedPath?.let { path ->
                    LocalPhotoThumb(path, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    captures.forEach { capture ->
                        LocalPhotoThumb(
                            path = capture.filePath,
                            modifier = Modifier
                                .size(68.dp, 82.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .border(1.dp, if (capture.filePath == selectedPath) Orange else Color.Transparent, RoundedCornerShape(11.dp))
                                .clickable { selectedPath = capture.filePath }
                        )
                    }
                }
            }
            Button(
                onClick = onCapture,
                enabled = mainCompleted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Ink,
                    disabledContainerColor = Color(0xFF39504B),
                    disabledContentColor = Slate
                )
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (mainCompleted) "记录我的发现" else "完成主线后解锁自由探索")
            }
        }
    }
}

@Composable
private fun DungeonHero(dungeon: Dungeon, progress: DungeonProgress) {
    val playerPhotoPath = progress.mainPhoto?.filePath
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(218.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        if (playerPhotoPath.isNullOrBlank()) {
            ImageThumb(dungeon.coverImage, Modifier.fillMaxSize())
        } else {
            LocalPhotoOrAssetThumb(playerPhotoPath, dungeon.coverImage, Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .94f))))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(progress.status)
                Text(dungeon.category, color = Paper.copy(alpha = .82f), style = MaterialTheme.typography.labelMedium)
            }
            Text(dungeon.name, color = Paper, style = MaterialTheme.typography.headlineLarge)
            Text("本副本探索度 ${progress.explorationPercent}%  ·  ${dungeon.difficulty}", color = Sand, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DungeonProgressRail(dungeon: Dungeon, progress: DungeonProgress) {
    val labels = listOf("主线") + dungeon.viewpoints.map { it.name }
    val mainCompleted = ProgressEngine.isMainCompleted(progress)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            val completed = if (index == 0) {
                mainCompleted
            } else {
                progress.viewpointProgress[dungeon.viewpoints[index - 1].id]?.completedAt != null
            }
            val unlocked = index == 0 || mainCompleted
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(if (completed || mainCompleted) Seafoam else Color(0xFF385652))
                        )
                    }
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = when {
                            completed -> Seafoam
                            unlocked -> Orange
                            else -> Color(0xFF385652)
                        },
                        border = androidx.compose.foundation.BorderStroke(2.dp, Ink)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (completed) Text("✓", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            else if (!unlocked) Icon(Icons.Outlined.Lock, contentDescription = null, tint = Slate, modifier = Modifier.size(12.dp))
                            else Text("○", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (index < labels.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(if (completed) Seafoam else Color(0xFF385652))
                        )
                    }
                }
                Text(
                    label,
                    color = when {
                        completed -> Seafoam
                        unlocked -> Paper
                        else -> Slate
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 3.dp, end = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MainTaskCard(
    dungeon: Dungeon,
    progress: DungeonProgress,
    currentlyAtCheckpoint: Boolean,
    currentAccuracyMeters: Float?,
    distanceMeters: Float?,
    onCapture: () -> Unit,
    onSimulateArrival: () -> Unit
) {
    val discovered = progress.arrivedAt != null
    val completed = ProgressEngine.isMainCompleted(progress)
    val accuracyLimit = minOf(50f, dungeon.checkpoint.radiusMeters / 2f)
    val withinRadius = distanceMeters?.let { it <= dungeon.checkpoint.radiusMeters } == true
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF173D3D), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("01", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(9.dp))
                Text("主线任务", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (completed) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam)
            }
            Text(dungeon.checkpoint.mainTaskTitle, color = Paper, style = MaterialTheme.typography.titleLarge)
            Text(dungeon.checkpoint.mainTaskDescription, color = Sand, style = MaterialTheme.typography.bodyMedium)
            Divider(color = Color.White.copy(alpha = .1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = if (currentlyAtCheckpoint) Seafoam else Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(dungeon.checkpoint.name, color = Paper, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            currentlyAtCheckpoint -> "当前在 GPS 打卡范围内"
                            withinRadius && currentAccuracyMeters == null -> "已在范围内 · 等待可用 GPS 精度"
                            withinRadius -> "已在范围内 · GPS 精度不足，暂不能确认到达"
                            discovered -> "已发现 · 当前不在 GPS 打卡范围内"
                            else -> "必须抵达现场才能完成主线"
                        },
                        color = if (currentlyAtCheckpoint) Seafoam else Slate,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (!currentlyAtCheckpoint) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF102C2F), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Orange, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when {
                                distanceMeters == null -> "需要定位 · 抵达 ${dungeon.checkpoint.radiusMeters}m 内解锁"
                                !withinRadius -> "距离目的地 ${formatDistance(distanceMeters)} · 抵达 ${dungeon.checkpoint.radiusMeters}m 内解锁"
                                currentAccuracyMeters == null -> "已在范围内 · GPS 精度不可用，需要 ≤${accuracyLimit.toInt()}m"
                                else -> "已在范围内 · GPS 精度 ±${currentAccuracyMeters.toInt()}m，需要 ≤${accuracyLimit.toInt()}m"
                            },
                            color = Sand,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = onSimulateArrival,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Orange.copy(alpha = .72f))
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("模拟抵达", color = Orange)
                    }
                }
            }
            Button(
                onClick = onCapture,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentlyAtCheckpoint) Orange else Color(0xFF244C4C),
                    contentColor = if (currentlyAtCheckpoint) Ink else Sand
                )
            ) {
                if (!currentlyAtCheckpoint) Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                else Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (currentlyAtCheckpoint) {
                    if (completed) "重新拍摄主线照片" else "拍摄主线照片"
                } else {
                    if (completed) "抵达后重新拍摄主线照片" else "抵达后拍摄主线照片"
                })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Paper, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Slate, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ViewpointListItem(
    viewpoint: Viewpoint,
    progress: com.citywalker.mvp.domain.ViewpointProgress?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val completed = progress?.completedAt != null
    val playerPhotoPath = progress?.photo?.filePath
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) DeepTeal else Color(0xFF13292B),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (completed) Seafoam.copy(alpha = .55f) else Color.White.copy(alpha = .05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (playerPhotoPath.isNullOrBlank()) {
                ImageThumb(viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)))
            } else {
                LocalPhotoOrAssetThumb(playerPhotoPath, viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(viewpoint.name, color = if (enabled) Paper else Slate, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (completed) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam, modifier = Modifier.size(18.dp))
                }
                Text(viewpoint.subtitle, color = if (enabled) Sand else Slate, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (completed) "已完成 · ${progress?.score?.totalScore ?: 0} 分" else "匹配线 ${viewpoint.matchThreshold} · ${viewpoint.recommendedOrientation}", color = if (completed) Seafoam else Orange, style = MaterialTheme.typography.labelSmall)
            }
            if (!enabled) Icon(Icons.Outlined.Lock, contentDescription = null, tint = Slate, modifier = Modifier.padding(start = 7.dp).size(17.dp))
            else Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Slate, modifier = Modifier.padding(start = 7.dp).size(18.dp))
        }
    }
}

@Composable
private fun ViewpointStageCard(
    index: Int,
    total: Int,
    viewpoint: Viewpoint,
    progress: com.citywalker.mvp.domain.ViewpointProgress?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val completed = progress?.completedAt != null
    val playerPhotoPath = progress?.photo?.filePath
    Surface(
        modifier = Modifier
            .width(236.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) DeepTeal else Color(0xFF13292B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                completed -> Seafoam.copy(alpha = .55f)
                enabled -> Orange.copy(alpha = .4f)
                else -> Color.White.copy(alpha = .06f)
            }
        )
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                if (playerPhotoPath.isNullOrBlank()) {
                    ImageThumb(viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.fillMaxSize())
                } else {
                    LocalPhotoOrAssetThumb(playerPhotoPath, viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .86f)))))
                Text("${index.toString().padStart(2, '0')} / ${total.toString().padStart(2, '0')}", color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(11.dp))
                if (!enabled) {
                    Surface(shape = CircleShape, color = Ink.copy(alpha = .72f), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(27.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Slate, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(viewpoint.name, color = if (enabled) Paper else Slate, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(viewpoint.subtitle, color = if (enabled) Sand else Slate, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        completed -> "已完成 · ${progress?.score?.totalScore ?: 0} 分"
                        !enabled -> "完成主线后解锁"
                        else -> "匹配线 ${viewpoint.matchThreshold} · ${viewpoint.recommendedOrientation}"
                    },
                    color = if (completed) Seafoam else if (enabled) Orange else Slate,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BadgeShelf(badges: List<Badge>, unlocked: Set<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        badges.forEach { badge ->
            val active = badge.id in unlocked
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(17.dp),
                color = if (active) Orange.copy(alpha = .14f) else DeepTeal,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Orange.copy(alpha = .65f) else Color.White.copy(alpha = .05f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (active) badge.icon else "○", color = if (active) Orange else Slate, fontSize = 22.sp)
                    Text(badge.name, color = if (active) Paper else Slate, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ViewpointScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val dungeon = viewModel.selectedDungeon() ?: return
    val viewpoint = viewModel.selectedViewpoint() ?: return
    val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
    val viewpointProgress = progress.viewpointProgress[viewpoint.id]
    val context = LocalContext.current
    val playerPhotoPath = viewpointProgress?.photo?.filePath
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Spacer(Modifier.height(3.dp))
        Box(modifier = Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(28.dp))) {
            if (playerPhotoPath.isNullOrBlank()) {
                ImageThumb(viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.fillMaxSize())
            } else {
                LocalPhotoOrAssetThumb(playerPhotoPath, viewpoint.referenceImages.firstOrNull().orEmpty(), Modifier.fillMaxSize())
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .9f)))))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(19.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (playerPhotoPath.isNullOrBlank()) "REFERENCE FRAME / ${viewpoint.source.uppercase()}" else "YOUR FRAME / 已完成", color = Orange, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Text(viewpoint.name, color = Paper, style = MaterialTheme.typography.headlineLarge)
                Text(viewpoint.subtitle, color = Sand, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(shape = RoundedCornerShape(22.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("机位提示", color = Orange, style = MaterialTheme.typography.labelLarge)
                Text(viewpoint.hint, color = Paper, style = MaterialTheme.typography.bodyLarge)
                Divider(color = Color.White.copy(alpha = .09f))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallInfo("方向", viewpoint.recommendedOrientation, Modifier.weight(1f))
                    SmallInfo("匹配线", "${viewpoint.matchThreshold} 分", Modifier.weight(1f))
                    SmallInfo("来源", "官方", Modifier.weight(1f))
                }
            }
        }
        if (viewpointProgress?.completedAt != null) {
            Surface(shape = RoundedCornerShape(18.dp), color = Seafoam.copy(alpha = .12f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam)
                    Spacer(Modifier.width(9.dp))
                    Text("已完成这个经典机位 · ${viewpointProgress.score?.totalScore ?: 0} 分", color = Seafoam, fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(
            onClick = viewModel::startViewpointCapture,
            enabled = ProgressEngine.isMainCompleted(progress),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink, disabledContainerColor = Color(0xFF39504B), disabledContentColor = Slate)
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("拍摄并匹配这个机位")
        }
        OutlinedButton(onClick = { NavigationLauncher.open(context, dungeon, "amap") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("导航到副本现场")
        }
    }
}

@Composable
private fun SmallInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Slate, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Paper, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CameraScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val dungeon = viewModel.selectedDungeon() ?: return
    val viewpoint = if (state.captureMode == CaptureMode.SURVEY_VIEWPOINT) null else viewModel.selectedViewpoint()
    val surveyViewpoint = if (state.captureMode == CaptureMode.SURVEY_VIEWPOINT) viewModel.selectedSurveyViewpoint() else null
    val mode = state.captureMode ?: CaptureMode.MAIN
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraCapture = remember { CameraCapture(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
    }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var exposureInfo by remember { mutableStateOf<CameraExposureInfo?>(null) }
    var exposureIndex by remember { mutableStateOf(0) }
    val captureScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) cameraFailed = true
    }

    DisposableEffect(imageCapture) {
        var lastRotation = Surface.ROTATION_0
        imageCapture.targetRotation = lastRotation
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                surfaceRotationForDeviceOrientation(orientation)?.let { rotation ->
                    if (rotation != lastRotation) {
                        lastRotation = rotation
                        imageCapture.targetRotation = rotation
                    }
                }
            }
        }
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
        onDispose { orientationListener.disable() }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            cameraCapture.bind(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                imageCapture = imageCapture,
                onReady = { cameraReady = true },
                onError = { cameraFailed = true }
            )
        }
    }
    LaunchedEffect(cameraReady) {
        if (cameraReady) {
            cameraCapture.exposureCompensationInfo()?.let { info ->
                exposureInfo = info
                exposureIndex = info.currentIndex.coerceIn(info.indexRange.first, info.indexRange.last)
            }
        }
    }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(900L)
            focusPoint = null
        }
    }
    DisposableEffect(Unit) {
        onDispose { cameraCapture.unbind() }
    }

    fun deliverPhoto(file: File) {
        captureScope.launch(Dispatchers.IO) {
            runCatching { photoRepository.normalizeOrientation(file) }
            withContext(Dispatchers.Main.immediate) {
                capturing = false
                viewModel.onPhotoCaptured(file.absolutePath)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraReady && !cameraFailed) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cameraReady) {
                        detectTapGestures(onTap = { offset ->
                            cameraCapture.focusAndMetering(previewView, offset.x, offset.y)
                            focusPoint = offset
                        })
                    }
            )
        } else {
            DemoCameraPreview(dungeon.name, surveyViewpoint?.name ?: viewpoint?.name ?: dungeon.checkpoint.mainTaskTitle)
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .58f), Color.Transparent, Color.Black.copy(alpha = .8f)))))
        focusPoint?.let { point ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Orange,
                    center = point,
                    radius = 31.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::navigateBack) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Paper)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(
                        when (mode) {
                            CaptureMode.MAIN -> "主线取景"
                            CaptureMode.VIEWPOINT -> "经典机位匹配"
                            CaptureMode.SURVEY_VIEWPOINT -> "踩点参考图"
                            CaptureMode.FREE -> "自由探索"
                        },
                        color = Orange,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(surveyViewpoint?.name ?: viewpoint?.name ?: dungeon.name, color = Paper, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (cameraReady && !cameraFailed) "LIVE" else "DEMO", color = if (cameraReady && !cameraFailed) Seafoam else Orange, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(250.dp).border(1.dp, Paper.copy(alpha = .75f), RoundedCornerShape(26.dp)))
                Box(modifier = Modifier.size(206.dp).border(1.dp, Paper.copy(alpha = .35f), RoundedCornerShape(18.dp)))
                Text(
                    when (mode) {
                        CaptureMode.MAIN -> "把地点放进框内"
                        CaptureMode.SURVEY_VIEWPOINT -> "记录现场参考构图"
                        CaptureMode.VIEWPOINT -> "对齐参考构图"
                        CaptureMode.FREE -> "记录这处现场发现"
                    },
                    color = Paper.copy(alpha = .86f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                )
            }
            val info = exposureInfo
            if (cameraReady && !cameraFailed && info != null) {
                val minEv = maxOf(-2f, info.indexRange.first * info.step)
                val maxEv = minOf(2f, info.indexRange.last * info.step)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Ink.copy(alpha = .74f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("曝光补偿", color = Paper, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            val currentEv = (exposureIndex * info.step).coerceIn(minEv, maxEv)
                            Text(formatExposure(currentEv), color = Orange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = (exposureIndex * info.step).coerceIn(minEv, maxEv),
                            onValueChange = { value ->
                                val next = (value / info.step).roundToInt().coerceIn(info.indexRange.first, info.indexRange.last)
                                exposureIndex = next
                                cameraCapture.setExposureCompensation(next)
                            },
                            valueRange = minEv..maxEv,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatExposure(minEv), color = Slate, style = MaterialTheme.typography.labelSmall)
                            Text("点击画面可对焦并测光", color = Seafoam, style = MaterialTheme.typography.labelSmall)
                            Text(formatExposure(maxEv), color = Slate, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (cameraFailed) {
                Text("相机权限未开启或设备不可用 · 使用 Demo 快门记录", color = Sand, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 13.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(50.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(76.dp).clickable(enabled = !capturing) {
                            capturing = true
                            val file = photoRepository.newPhotoFile(
                                when (mode) {
                                    CaptureMode.MAIN -> "main"
                                    CaptureMode.VIEWPOINT -> "viewpoint"
                                    CaptureMode.SURVEY_VIEWPOINT -> "survey"
                                    CaptureMode.FREE -> "free"
                                }
                            )
                            val title = surveyViewpoint?.name ?: viewpoint?.name ?: dungeon.name
                            val subtitle = when (mode) {
                                CaptureMode.MAIN -> dungeon.checkpoint.name
                                CaptureMode.VIEWPOINT -> "经典机位现场记录"
                                CaptureMode.SURVEY_VIEWPOINT -> "踩点参考照片"
                                CaptureMode.FREE -> "我的发现"
                            }
                            if (cameraReady && !cameraFailed) {
                                cameraCapture.capture(
                                    imageCapture = imageCapture,
                                    file = file,
                                    executor = ContextCompat.getMainExecutor(context),
                                    onSaved = { deliverPhoto(file) },
                                    onError = {
                                        photoRepository.saveDemoPhoto(file, title, subtitle)
                                        deliverPhoto(file)
                                    }
                                )
                            } else {
                                photoRepository.saveDemoPhoto(file, title, subtitle)
                                deliverPhoto(file)
                            }
                        },
                        shape = CircleShape,
                        color = Paper,
                        border = androidx.compose.foundation.BorderStroke(7.dp, Paper.copy(alpha = .35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (capturing) CircularProgressIndicator(color = Ink, modifier = Modifier.size(29.dp), strokeWidth = 3.dp)
                            else Box(Modifier.size(54.dp).background(Orange, CircleShape))
                        }
                    }
                }
                IconButton(onClick = viewModel::navigateBack, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "重置", tint = Paper.copy(alpha = .75f))
                }
            }
        }
    }
}

@Composable
private fun DemoCameraPreview(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF376E68), Color(0xFF11272D))))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFE9A15B).copy(alpha = .72f), radius = size.minDimension * .19f, center = Offset(size.width * .74f, size.height * .22f))
            val ridge = Path().apply {
                moveTo(0f, size.height * .74f)
                lineTo(size.width * .3f, size.height * .5f)
                lineTo(size.width * .49f, size.height * .68f)
                lineTo(size.width * .7f, size.height * .45f)
                lineTo(size.width, size.height * .67f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(ridge, Color(0xFF0B252A))
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(30.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("DEMO FIELD CAMERA", color = Orange, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Paper, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, color = Sand, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val result = state.captureResult ?: return
    val dungeon = state.content?.dungeons?.firstOrNull { it.id == result.dungeonId } ?: return
    val dungeonProgress = ProgressEngine.progressFor(state.progress, dungeon.id)
    val bitmap = remember(result.photo.filePath) { PhotoBitmapLoader.load(result.photo.filePath)?.asImageBitmap() }
    val score = result.score
    val firstViewpoint = dungeon.viewpoints.firstOrNull()
    var freeNote by remember(result.photo.filePath) { mutableStateOf(result.photo.note.orEmpty()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(3.dp))
        Text(
            when (result.mode) {
                CaptureMode.MAIN -> "主线已记录"
                CaptureMode.FREE -> "自由探索已记录"
                else -> "机位评分完成"
            },
            color = Orange,
            fontSize = 12.sp,
            letterSpacing = 2.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            when (result.mode) {
                CaptureMode.MAIN -> "你已经把这里\n走进自己的图鉴。"
                CaptureMode.FREE -> "这一帧属于你，\n不改变官方探索进度。"
                else -> "参考图与现场\n正在对齐。"
            },
            color = Paper,
            style = MaterialTheme.typography.headlineLarge
        )
        bitmap?.let {
            androidx.compose.foundation.Image(bitmap = it, contentDescription = "现场照片", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(26.dp)))
        } ?: Box(Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(26.dp)).background(DeepTeal), contentAlignment = Alignment.Center) {
            Text("照片已保存", color = Seafoam)
        }
        if (result.isScoring) {
            Surface(shape = RoundedCornerShape(20.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Orange, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("正在评分…", color = Paper, fontWeight = FontWeight.Bold)
                        Text("本地 Demo 会检查相似度、清晰度和曝光", color = Slate, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if (result.mode == CaptureMode.VIEWPOINT && score != null) {
            ScoreCard(score)
        } else if (result.mode == CaptureMode.MAIN) {
            Surface(shape = RoundedCornerShape(20.dp), color = Seafoam.copy(alpha = .13f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("主线完成 · ${dungeon.name}探索度 ${dungeonProgress.explorationPercent}%", color = Paper, fontWeight = FontWeight.Bold)
                        Text("照片和 GPS 记录已保存在本机", color = Seafoam, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if (result.mode == CaptureMode.FREE) {
            Surface(shape = RoundedCornerShape(20.dp), color = Seafoam.copy(alpha = .13f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("已加入我的发现", color = Paper, fontWeight = FontWeight.Bold)
                            Text("${formatCoordinate(result.photo.location.latitude)}, ${formatCoordinate(result.photo.location.longitude)} · 不计入官方探索", color = Seafoam, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    SurveyTextField(
                        value = freeNote,
                        onValueChange = { freeNote = it },
                        placeholder = "现场备注（可选）",
                        minLines = 2
                    )
                    OutlinedButton(
                        onClick = { viewModel.updateFreeCaptureNote(result.photo.filePath, freeNote) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存备注")
                    }
                }
            }
        } else if (result.mode == CaptureMode.VIEWPOINT) {
            Surface(shape = RoundedCornerShape(20.dp), color = Seafoam.copy(alpha = .13f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Seafoam, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("照片已保存", color = Paper, fontWeight = FontWeight.Bold)
                        Text("评分暂不可用，可以稍后重试", color = Seafoam, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Button(onClick = viewModel::generatePostcard, enabled = !result.isScoring, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)) {
            Text("生成城市明信片")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        if (result.mode == CaptureMode.MAIN && firstViewpoint != null) {
            OutlinedButton(onClick = { viewModel.openViewpoint(dungeon.id, firstViewpoint.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("继续探索经典机位")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
        TextButton(onClick = viewModel::navigateBack, modifier = Modifier.fillMaxWidth()) { Text("回到副本详情", color = Seafoam) }
    }
}

@Composable
private fun ScoreCard(score: com.citywalker.mvp.domain.PhotoScore) {
    Surface(shape = RoundedCornerShape(22.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (score.matched) "构图匹配成功" else "还差一点点", color = if (score.matched) Seafoam else Orange, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(ProgressEngine.scoreStars(score.totalScore), color = Orange, fontSize = 23.sp, letterSpacing = 2.sp)
                }
                Text("${score.totalScore}", color = Paper, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            }
            ScoreBar("相似度", score.similarity)
            ScoreBar("清晰度", score.sharpness)
            ScoreBar("曝光", score.exposure)
            Text("评分引擎：${score.provider} · 可替换为真实 AI 服务", color = Slate, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row { Text(label, color = Sand, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); Text("$score", color = Paper, style = MaterialTheme.typography.labelMedium) }
        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Orange, trackColor = Color.White.copy(alpha = .12f))
    }
}

@Composable
private fun CollectionScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val content = state.content ?: return
    val stats = ProgressEngine.cityStats(content, state.progress)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("收集每一次到达、每一张现场照片。", color = Sand, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF173D3D), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("长沙探险者", color = Paper, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${stats.completedCount}/${stats.totalCount} 个副本已留下主线记录", color = Seafoam, style = MaterialTheme.typography.bodySmall)
                }
                Text("${stats.explorationPercent}%", color = Orange, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        content.cityBadges.firstOrNull()?.let { cityBadge ->
            val unlocked = cityBadge.id in state.progress.cityBadges
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (unlocked) Orange.copy(alpha = .14f) else DeepTeal,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (unlocked) Orange.copy(alpha = .55f) else Color.White.copy(alpha = .05f)),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) cityBadge.icon else "○", color = if (unlocked) Orange else Slate, fontSize = 25.sp)
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(cityBadge.name, color = if (unlocked) Paper else Slate, fontWeight = FontWeight.Bold)
                        Text(if (unlocked) cityBadge.description else "完成长沙全部副本主线后解锁", color = if (unlocked) Seafoam else Slate, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (unlocked) "已获得" else "CITY", color = if (unlocked) Orange else Slate, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(content.dungeons, key = { it.id }) { dungeon ->
                val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
                CollectionCard(dungeon, progress) { viewModel.openDungeon(dungeon.id) }
            }
        }
    }
}

@Composable
private fun CollectionCard(dungeon: Dungeon, progress: DungeonProgress, onClick: () -> Unit) {
    val unlocked = progress.unlockedBadges.size
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = DeepTeal) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                ImageThumb(dungeon.coverImage, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .8f)))))
                Text("副本 ${progress.explorationPercent}%", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(dungeon.name, color = Paper, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ProgressEngine.statusLabel(progress.status), color = if (progress.status == DungeonStatus.LOCKED) Slate else Orange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("$unlocked 印章", color = Slate, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PostcardScreen(state: CityWalkerUiState, viewModel: CityWalkerViewModel) {
    val context = LocalContext.current
    val path = state.postcardPath
    val bitmap = remember(path) { path?.let { PhotoBitmapLoader.load(it)?.asImageBitmap() } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(3.dp))
        Text("POSTCARD / 现场纪念", color = Orange, fontSize = 12.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
        Text("把今天寄给\n未来的自己。", color = Paper, style = MaterialTheme.typography.headlineLarge)
        if (state.isGeneratingPostcard) {
            Box(modifier = Modifier.fillMaxWidth().height(480.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = Orange)
                    Text("正在排版你的城市明信片…", color = Sand)
                }
            }
        } else if (bitmap != null) {
            androidx.compose.foundation.Image(bitmap = bitmap, contentDescription = "城市明信片", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)))
            Button(onClick = { path?.let { file -> if (File(file).exists()) ShareHelper.shareImage(context, File(file), viewModel.selectedDungeon()?.name ?: "长沙城市探险") } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("调用系统分享")
            }
        } else {
            Text("明信片暂时还没有生成完成。", color = Sand)
        }
        Text("图片保存在应用本机目录，分享时通过 Android 系统分享面板选择微信、相册或其他应用。", color = Slate, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FieldSurveyScreen(
    state: CityWalkerUiState,
    viewModel: CityWalkerViewModel,
    requestLocation: () -> Unit
) {
    val content = state.content ?: return
    val dungeon = viewModel.selectedSurveyDungeon() ?: return
    val survey = viewModel.selectedSurvey()
    val draft = viewModel.selectedSurveyViewpoint()
    val surveyViewpoints = survey?.viewpoints.orEmpty()
    val context = LocalContext.current
    val distance = state.currentLocation?.let { location -> distanceBetweenMeters(location, dungeon.checkpoint.point) }
    var dungeonMenuExpanded by remember { mutableStateOf(false) }
    var name by remember(draft?.viewpointId) { mutableStateOf(draft?.name.orEmpty()) }
    var hint by remember(draft?.viewpointId) { mutableStateOf(draft?.hint.orEmpty()) }
    var note by remember(dungeon.id, draft?.viewpointId) { mutableStateOf(draft?.note ?: survey?.note.orEmpty()) }
    var pendingReset by remember { mutableStateOf<ProgressResetTarget?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 38.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(3.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("DEBUG / FIELD SURVEY", color = Orange, fontSize = 12.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                Text("把现场看到的\n直接记录成内容。", color = Paper, style = MaterialTheme.typography.headlineLarge)
                Text("连续采样、照片和备注会独立保存，不会修改正式玩家进度。", color = Sand, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("当前副本", color = Slate, style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { dungeonMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(dungeon.name, color = Paper, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            Text("⌄", color = Orange, fontSize = 20.sp)
                        }
                        DropdownMenu(expanded = dungeonMenuExpanded, onDismissRequest = { dungeonMenuExpanded = false }) {
                            content.dungeons.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.name) },
                                    onClick = {
                                        dungeonMenuExpanded = false
                                        viewModel.selectSurveyDungeon(candidate.id)
                                    }
                                )
                            }
                        }
                    }
                    Text(dungeon.subtitle, color = Sand, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SurveyGpsCard(state, requestLocation)
        }
        item {
            SurveyCheckpointCard(
                dungeon = dungeon,
                capture = survey?.effectiveCheckpoint,
                historyCount = survey?.checkpointHistory?.size ?: 0,
                currentLocation = state.currentLocation,
                distanceMeters = distance,
                sampling = state.surveySampling,
                onRecord = viewModel::recordSurveyCheckpoint
            )
        }
        item {
            if (dungeon.viewpoints.isNotEmpty()) {
                SurveyOfficialViewpointPicker(
                    viewpoints = dungeon.viewpoints,
                    recorded = surveyViewpoints,
                    selectedId = state.surveyViewpointId,
                    onSelect = viewModel::selectSurveyOfficialViewpoint
                )
            }
        }
        item {
            SectionHeader("新增机位", "发现官方列表之外的点位，生成新的 survey ID")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.createSurveyViewpoint() },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
                ) {
                    Text("新增机位")
                }
                OutlinedButton(
                    onClick = viewModel::recordSurveyViewpoint,
                    enabled = !state.surveySampling.active,
                    modifier = Modifier.weight(1.25f),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(if (state.surveySampling.type == SurveyPointType.VIEWPOINT && state.surveySampling.active) "采集中…" else "记录当前位置为 viewpoint")
                }
            }
        }
        if (surveyViewpoints.isNotEmpty()) {
            item {
                SurveyViewpointPicker(
                    viewpoints = surveyViewpoints,
                    officialViewpointIds = dungeon.viewpoints.map { it.id }.toSet(),
                    selectedId = state.surveyViewpointId,
                    onSelect = viewModel::selectSurveyViewpoint
                )
            }
        }
        item {
            if (draft == null) {
                EmptySurveyDraftCard()
            } else {
                SurveyViewpointEditor(
                    draft = draft,
                    name = name,
                    hint = hint,
                    note = note,
                    sampling = state.surveySampling,
                    onNameChange = {
                        name = it
                        viewModel.updateSurveyViewpointName(it)
                    },
                    onHintChange = {
                        hint = it
                        viewModel.updateSurveyHint(it)
                    },
                    onNoteChange = {
                        note = it
                        viewModel.updateSurveyNote(it)
                    },
                    onRecord = viewModel::recordSurveyViewpoint,
                    onCapture = viewModel::startSurveyReferenceCapture,
                    onConfirm = viewModel::confirmSurveyViewpoint
                )
            }
        }
        item {
            if (draft == null) {
                SurveyNoteEditor(
                    label = "现场备注",
                    value = note,
                    placeholder = "记录开放时间、光线、施工、游客量等现场信息…",
                    onValueChange = {
                        note = it
                        viewModel.updateSurveyNote(it)
                    }
                )
            }
        }
        item {
            Surface(shape = RoundedCornerShape(21.dp), color = Color(0xFF173D3D), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("导出", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("输出 changsha_survey_YYYY-MM-DD.zip，内含 survey.json 和全部参考照片。", color = Sand, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = viewModel::exportSurvey,
                        enabled = !state.surveySampling.active,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
                    ) {
                        Text("导出踩点数据")
                    }
                    state.lastSurveyExportPath?.let { path ->
                        Text("已生成：${File(path).name}", color = Seafoam, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        OutlinedButton(onClick = { ShareHelper.shareZip(context, File(path)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("通过 Android 系统分享 ZIP")
                        }
                    }
                }
            }
        }
        if (BuildConfig.DEBUG) {
            item {
                DebugProgressTools(
                    dungeonName = dungeon.name,
                    onClearCurrent = { pendingReset = ProgressResetTarget.CURRENT },
                    onClearAll = { pendingReset = ProgressResetTarget.ALL }
                )
            }
        }
    }

    pendingReset?.let { target ->
        val clearAll = target == ProgressResetTarget.ALL
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(if (clearAll) "清空全部玩家进度？" else "清空当前副本进度？") },
            text = {
                Text(
                    if (clearAll) {
                        "只会清除本机 PlayerProgress。已保存的踩点记录、survey_state.json 和照片不会删除。"
                    } else {
                        "只会清除“${dungeon.name}”的玩家进度。踩点记录、survey_state.json 和照片不会删除。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (clearAll) viewModel.clearAllPlayerProgress() else viewModel.clearCurrentDungeonProgress(dungeon.id)
                        pendingReset = null
                    }
                ) {
                    Text("确认清空", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReset = null }) {
                    Text("取消", color = MutedInk)
                }
            }
        )
    }
}

private enum class ProgressResetTarget {
    CURRENT,
    ALL
}

@Composable
private fun DebugProgressTools(
    dungeonName: String,
    onClearCurrent: () -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(21.dp),
        color = Color(0xFF173D3D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = .42f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Debug / 玩家进度", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("仅清除 PlayerProgress；踩点数据和照片保留。用于重新验证真实 GPS 打卡。", color = Sand, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onClearCurrent,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = .72f))
                ) {
                    Text("清空 $dungeonName", color = Coral, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Coral.copy(alpha = .72f))
                ) {
                    Text("清空全部", color = Coral)
                }
            }
        }
    }
}

@Composable
private fun SurveyGpsCard(state: CityWalkerUiState, requestLocation: () -> Unit) {
    val location = state.currentLocation
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF143235), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("GPS", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (state.locationStatus) {
                            LocationStatus.READY -> "已连接现场定位"
                            LocationStatus.SIMULATED -> "测试位置，不可用于正式踩点"
                            LocationStatus.LOCATING -> "正在寻找位置…"
                            LocationStatus.ERROR -> state.locationError ?: "定位不可用"
                            LocationStatus.IDLE -> "需要真实 GPS 才能记录现场点"
                        },
                        color = if (state.locationStatus == LocationStatus.ERROR) Coral else Seafoam,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = requestLocation, enabled = state.locationStatus != LocationStatus.LOCATING, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("刷新")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GpsValue("纬度", formatCoordinate(location?.latitude), Modifier.weight(1f))
                GpsValue("经度", formatCoordinate(location?.longitude), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GpsValue("精度", location?.accuracyMeters?.let { "±${formatOneDecimal(it)}m" } ?: "--", Modifier.weight(1f))
                GpsValue("位置来源", if (location?.simulated == true) "模拟" else if (location != null) "设备 GPS" else "--", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GpsValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Slate, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Paper, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SurveyCheckpointCard(
    dungeon: Dungeon,
    capture: SurveyCapture?,
    historyCount: Int,
    currentLocation: com.citywalker.mvp.domain.GeoPoint?,
    distanceMeters: Float?,
    sampling: SurveySamplingState,
    onRecord: () -> Unit
) {
    Surface(shape = RoundedCornerShape(22.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("主线 checkpoint", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${dungeon.checkpoint.radiusMeters}m 半径", color = Orange, style = MaterialTheme.typography.labelLarge)
            }
            Text("配置坐标", color = Slate, style = MaterialTheme.typography.labelSmall)
            Text("${formatCoordinate(dungeon.checkpoint.latitude)}, ${formatCoordinate(dungeon.checkpoint.longitude)}", color = Paper, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GpsValue("当前距离", distanceMeters?.let { formatDistance(it) } ?: "--", Modifier.weight(1f))
                GpsValue("当前 radius", "${dungeon.checkpoint.radiusMeters}m", Modifier.weight(1f))
            }
            currentLocation?.let {
                Text("当前设备点：${formatCoordinate(it.latitude)}, ${formatCoordinate(it.longitude)}", color = Slate, style = MaterialTheme.typography.labelSmall)
            }
            capture?.let {
                Surface(shape = RoundedCornerShape(14.dp), color = Seafoam.copy(alpha = .1f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("当前有效锚点 · ${it.samples.size} 个样本 · 历史 ${historyCount} 次", color = Seafoam, fontWeight = FontWeight.Bold)
                        Text("${formatCoordinate(it.point.latitude)}, ${formatCoordinate(it.point.longitude)} · 精度 ±${formatOneDecimal(it.point.accuracyMeters)}m", color = Sand, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(
                onClick = onRecord,
                enabled = !sampling.active,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
            ) {
                if (sampling.type == SurveyPointType.CHECKPOINT && sampling.active) {
                    Text("连续采样 ${sampling.sampleCount} 个 · ${sampling.elapsedSeconds}s")
                } else {
                    Text(if (capture == null) "设当前位置为主线锚点" else "重新采样主线锚点")
                }
            }
        }
    }
}

@Composable
private fun SurveyViewpointPicker(
    viewpoints: List<SurveyViewpointDraft>,
    officialViewpointIds: Set<String>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("本次踩点记录", color = Slate, style = MaterialTheme.typography.labelMedium)
        viewpoints.forEach { draft ->
            val selected = draft.viewpointId == selectedId
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(draft.viewpointId) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) Orange.copy(alpha = .16f) else DeepTeal,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Orange.copy(alpha = .65f) else Color.White.copy(alpha = .05f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(draft.name.ifBlank { draft.viewpointId }, color = if (selected) Paper else Sand, fontWeight = FontWeight.Bold)
                        Text(
                            (if (draft.verified && draft.status == "active") "active" else "candidate") + " · " +
                                (if (draft.viewpointId in officialViewpointIds) "验证官方机位" else "新增机位") + " · " + when {
                                draft.capture?.point?.photoPath != null -> "已定位 · 已绑定参考图"
                                draft.capture != null -> "已定位 · 待拍参考图"
                                else -> "待记录位置"
                            },
                            color = if (selected) Orange else Slate,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (draft.verified && draft.status == "active") {
                        Text("verified", color = Seafoam, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyOfficialViewpointPicker(
    viewpoints: List<Viewpoint>,
    recorded: List<SurveyViewpointDraft>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("验证已有机位", color = Paper, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("沿用官方 ID，现场坐标和参考图会回填到同一个机位；未完成前仍是 candidate。", color = Slate, style = MaterialTheme.typography.bodySmall)
        viewpoints.forEach { viewpoint ->
            val draft = recorded.firstOrNull { it.viewpointId == viewpoint.id }
            val selected = viewpoint.id == selectedId
            val active = draft?.verified == true && draft.status == "active"
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(viewpoint.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) Orange.copy(alpha = .16f) else Color(0xFF143235),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) Orange.copy(alpha = .72f) else Color.White.copy(alpha = .06f)
                )
            ) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(viewpoint.name, color = if (selected) Paper else Sand, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                active -> "已验证 · verified=true · active · ${viewpoint.id}"
                                draft != null -> "已有候选记录 · ${viewpoint.id}"
                                else -> "待现场验证 · ${viewpoint.id}"
                            },
                            color = if (active) Seafoam else if (selected) Orange else Slate,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(if (active) "已完成" else "选择", color = if (active) Seafoam else Orange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SurveyViewpointEditor(
    draft: SurveyViewpointDraft,
    name: String,
    hint: String,
    note: String,
    sampling: SurveySamplingState,
    onNameChange: (String) -> Unit,
    onHintChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onRecord: () -> Unit,
    onCapture: () -> Unit,
    onConfirm: () -> Unit
) {
    val active = draft.verified && draft.status == "active"
    Surface(shape = RoundedCornerShape(22.dp), color = DeepTeal, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前机位", color = Paper, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(draft.viewpointId, color = Slate, style = MaterialTheme.typography.labelSmall)
            }
            SurveyTextField(name, onNameChange, "机位名称，例如：洲头江岸线", singleLine = true)
            draft.capture?.let { capture ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF244C4C), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("viewpoint 坐标已记录", color = Seafoam, fontWeight = FontWeight.Bold)
                        Text("${formatCoordinate(capture.point.latitude)}, ${formatCoordinate(capture.point.longitude)}", color = Paper, style = MaterialTheme.typography.bodyMedium)
                        Text("精度 ±${formatOneDecimal(capture.point.accuracyMeters)}m · ${capture.samples.size} 个样本", color = Sand, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } ?: Text("还没有现场坐标，建议先点击“记录当前位置为 viewpoint”。", color = Slate, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRecord,
                    enabled = !sampling.active,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(if (sampling.type == SurveyPointType.VIEWPOINT && sampling.active) "采集中…" else "记录位置")
                }
                Button(
                    onClick = onCapture,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (draft.capture?.point?.photoPath == null) "拍参考图" else "重拍参考图")
                }
            }
            SurveyTextField(hint, onHintChange, "机位 hint：从哪里走、看什么、怎么构图…", minLines = 3)
            SurveyNoteEditor("现场备注", note, "记录现场光线、游客量、遮挡等…", onNoteChange)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (active) Seafoam.copy(alpha = .12f) else Color(0xFF244C4C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (active) "已确认 · verified = true · status = active" else "候选机位 · status = candidate",
                        color = if (active) Seafoam else Orange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (active) "已作为正式参考机位发布到踩点导出数据。"
                        else "完成坐标、参考图和 hint 后确认，才会作为正式机位导出。",
                        color = Sand,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (!active) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("确认机位并设为 active")
                }
            }
            draft.capture?.point?.photoPath?.let { path ->
                Text("参考照片已绑定：${File(path).name}", color = Seafoam, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LocalPhotoThumb(path, Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)))
            }
        }
    }
}

@Composable
private fun EmptySurveyDraftCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF13292B), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Slate)
            Spacer(Modifier.width(10.dp))
            Text("点击“新增机位”创建一个现场草稿，然后记录坐标、拍参考图和填写 hint。", color = Slate, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SurveyNoteEditor(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Paper, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        SurveyTextField(value, onValueChange, placeholder, minLines = 3)
    }
}

@Composable
private fun SurveyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Slate) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F292C),
            unfocusedContainerColor = Color(0xFF0F292C),
            disabledContainerColor = Color(0xFF0F292C),
            focusedTextColor = Paper,
            unfocusedTextColor = Paper,
            cursorColor = Orange,
            focusedIndicatorColor = Orange,
            unfocusedIndicatorColor = Color(0xFF45625E)
        )
    )
}

@Composable
private fun LocalPhotoThumb(path: String, modifier: Modifier = Modifier) {
    LocalPhotoOrAssetThumb(path, null, modifier)
}

@Composable
private fun LocalPhotoOrAssetThumb(path: String, fallbackAsset: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { PhotoBitmapLoader.load(path)?.asImageBitmap() }
    if (bitmap == null) {
        if (fallbackAsset.isNullOrBlank()) {
            Box(modifier = modifier.background(Color(0xFF21494A)), contentAlignment = Alignment.Center) {
                Text("PHOTO", color = Slate, fontSize = 12.sp, letterSpacing = 2.sp)
            }
        } else {
            ImageThumb(fallbackAsset, modifier)
        }
    } else {
        androidx.compose.foundation.Image(bitmap = bitmap, contentDescription = "现场照片", contentScale = ContentScale.Crop, modifier = modifier)
    }
}

private fun formatExposure(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    val number = if (rounded % 1f == 0f) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
    return if (rounded > 0f) "+$number EV" else "$number EV"
}

private fun formatCoordinate(value: Double?): String = value?.let { String.format(Locale.US, "%.6f", it) } ?: "--"

private fun formatOneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)

@Composable
private fun StatusPill(status: DungeonStatus) {
    val color = when (status) {
        DungeonStatus.COMPLETED_100 -> Seafoam
        DungeonStatus.MAIN_COMPLETED, DungeonStatus.EXPLORING -> Orange
        DungeonStatus.ARRIVED -> Coral
        DungeonStatus.LOCKED -> Slate
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = .15f)) {
        Text(ProgressEngine.statusLabel(status), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun ImageThumb(url: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF21494A)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            if (File(url).isFile) {
                LocalPhotoThumb(url, Modifier.fillMaxSize())
            } else {
                coil.compose.AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Text("CITY", color = Seafoam.copy(alpha = .7f), fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun MessageBanner(
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(17.dp),
        color = Paper,
        shadowElevation = 12.dp
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = Ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            actionLabel?.let { label ->
                TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(label, color = Coral, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "关闭提示", tint = MutedInk, modifier = Modifier.size(17.dp))
            }
        }
    }
}
