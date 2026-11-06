package com.citywalker.mvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.citywalker.mvp.BuildConfig
import com.citywalker.mvp.core.map.MapTile
import com.citywalker.mvp.core.map.TianDiTuProjection
import com.citywalker.mvp.core.map.TianDiTuTileSource
import kotlin.math.roundToInt

internal const val CHANGSHA_MAP_ZOOM = 13
private const val TILE_PADDING = 1
private const val MULTI_TEMPORAL_TIME = "2022-12-31 00:00:00"

/**
 * Native raster tile layer. It is intentionally a composable rather than a
 * WebView or a map SDK so the game viewport can transform the imagery and
 * HUD markers together.
 */
@Composable
internal fun TianDiTuRasterLayer(
    centerLatitude: Double,
    centerLongitude: Double,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    val apiKey = BuildConfig.TIANDITU_API_KEY
    val density = LocalDensity.current
    val tileSizeDp = with(density) { TianDiTuProjection.TILE_SIZE_PX.toDp() }
    var temporalSourceEnabled by remember(apiKey) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF102C2F))
    ) {
        if (apiKey.isNotBlank()) {
            val tileRange = TianDiTuProjection.tileRangeForViewport(
                centerLatitude = centerLatitude,
                centerLongitude = centerLongitude,
                zoom = CHANGSHA_MAP_ZOOM,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                paddingTiles = TILE_PADDING
            )
            val centerWorldX = TianDiTuProjection.worldPixelX(centerLongitude, CHANGSHA_MAP_ZOOM)
            val centerWorldY = TianDiTuProjection.worldPixelY(centerLatitude, CHANGSHA_MAP_ZOOM)

            for (tileX in tileRange.minX..tileRange.maxX) {
                for (tileY in tileRange.minY..tileRange.maxY) {
                    val tile = MapTile(zoom = CHANGSHA_MAP_ZOOM, x = tileX, y = tileY)
                    val x = (viewportWidthPx / 2f + tileX * TianDiTuProjection.TILE_SIZE_PX - centerWorldX)
                        .roundToInt()
                    val y = (viewportHeightPx / 2f + tileY * TianDiTuProjection.TILE_SIZE_PX - centerWorldY)
                        .roundToInt()
                    TianDiTuTile(
                        tile = tile,
                        apiKey = apiKey,
                        useTemporalSource = temporalSourceEnabled,
                        onTemporalSourceError = { temporalSourceEnabled = false },
                        tileSize = tileSizeDp,
                        x = x,
                        y = y
                    )
                }
            }
        }
    }
}

@Composable
private fun TianDiTuTile(
    tile: MapTile,
    apiKey: String,
    useTemporalSource: Boolean,
    onTemporalSourceError: () -> Unit,
    tileSize: androidx.compose.ui.unit.Dp,
    x: Int,
    y: Int
) {
    val imageModifier = Modifier
        .offset { IntOffset(x, y) }
        .size(tileSize)

    Box(modifier = imageModifier) {
        if (useTemporalSource) {
            AsyncImage(
                model = TianDiTuTileSource.multiTemporalHunan(tile, apiKey, MULTI_TEMPORAL_TIME),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                onError = {
                    // The Hunan temporal WMTS is an optional source. Some keys
                    // or service environments do not grant it, so switch the
                    // whole map to national satellite tiles after the first
                    // definitive tile failure.
                    onTemporalSourceError()
                }
            )
        } else {
            AsyncImage(
                model = TianDiTuTileSource.currentSatellite(tile, apiKey),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        AsyncImage(
            model = TianDiTuTileSource.satelliteLabels(tile, apiKey),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = .78f
        )
    }
}
