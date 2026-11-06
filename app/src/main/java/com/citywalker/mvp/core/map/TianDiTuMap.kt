package com.citywalker.mvp.core.map

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

/** A tile coordinate in the Web Mercator pyramid used by TianDiTu. */
data class MapTile(
    val zoom: Int,
    val x: Int,
    val y: Int
)

data class MapPoint(
    val x: Float,
    val y: Float
)

data class MapTileRange(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
)

/**
 * Pure Web Mercator math shared by the raster layer and its unit tests.
 *
 * TianDiTu's XYZ satellite tiles use a 256px Web Mercator tile scheme.  The
 * x coordinate is deliberately allowed to be unwrapped so a viewport can be
 * panned without a discontinuity at the antimeridian; only URL generation
 * wraps it back into the valid tile range.
 */
object TianDiTuProjection {
    const val TILE_SIZE_PX = 256
    const val MAX_MERCATOR_LATITUDE = 85.05112878

    fun worldSizePx(zoom: Int): Double {
        require(zoom >= 0) { "zoom must be non-negative" }
        return TILE_SIZE_PX * 2.0.pow(zoom)
    }

    fun worldPixelX(longitude: Double, zoom: Int): Double {
        val normalizedLongitude = longitude.coerceIn(-180.0, 180.0)
        return ((normalizedLongitude + 180.0) / 360.0) * worldSizePx(zoom)
    }

    fun worldPixelY(latitude: Double, zoom: Int): Double {
        val clampedLatitude = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
        val latitudeRadians = Math.toRadians(clampedLatitude)
        val mercator = ln(tan(Math.PI / 4.0 + latitudeRadians / 2.0))
        return (0.5 - mercator / (2.0 * Math.PI)) * worldSizePx(zoom)
    }

    fun tileFor(latitude: Double, longitude: Double, zoom: Int): MapTile {
        val worldSize = worldSizePx(zoom)
        val x = floor(worldPixelX(longitude, zoom) / TILE_SIZE_PX).toInt()
        val y = floor(worldPixelY(latitude, zoom) / TILE_SIZE_PX)
            .toInt()
            .coerceIn(0, (worldSize / TILE_SIZE_PX).toInt() - 1)
        return MapTile(zoom = zoom, x = wrapTileX(x, zoom), y = y)
    }

    fun wrapTileX(tileX: Int, zoom: Int): Int {
        val tileCount = 1 shl zoom
        return ((tileX % tileCount) + tileCount) % tileCount
    }

    fun projectToMap(
        latitude: Double,
        longitude: Double,
        centerLatitude: Double,
        centerLongitude: Double,
        zoom: Int,
        viewportWidthPx: Float,
        viewportHeightPx: Float
    ): MapPoint {
        val centerX = worldPixelX(centerLongitude, zoom)
        val centerY = worldPixelY(centerLatitude, zoom)
        val pointX = worldPixelX(longitude, zoom)
        val pointY = worldPixelY(latitude, zoom)
        return MapPoint(
            x = (viewportWidthPx / 2f + pointX - centerX).toFloat(),
            y = (viewportHeightPx / 2f + pointY - centerY).toFloat()
        )
    }

    /**
     * Returns the XYZ tiles needed to cover the transformed viewport, plus a
     * small buffer so a one-finger pan does not reveal a blank edge.
     */
    fun tileRangeForViewport(
        centerLatitude: Double,
        centerLongitude: Double,
        zoom: Int,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        paddingTiles: Int = 1
    ): MapTileRange {
        val safeScale = scale.coerceAtLeast(0.01f)
        val centerX = worldPixelX(centerLongitude, zoom)
        val centerY = worldPixelY(centerLatitude, zoom)
        val minBaseX = -offsetX / safeScale
        val maxBaseX = (viewportWidthPx - offsetX) / safeScale
        val minBaseY = -offsetY / safeScale
        val maxBaseY = (viewportHeightPx - offsetY) / safeScale
        val minWorldX = centerX + minBaseX - viewportWidthPx / 2f
        val maxWorldX = centerX + maxBaseX - viewportWidthPx / 2f
        val minWorldY = centerY + minBaseY - viewportHeightPx / 2f
        val maxWorldY = centerY + maxBaseY - viewportHeightPx / 2f
        val worldTileCount = 1 shl zoom
        val maxTileY = worldTileCount - 1
        return MapTileRange(
            minX = floor(minWorldX / TILE_SIZE_PX).toInt() - paddingTiles,
            maxX = floor(maxWorldX / TILE_SIZE_PX).toInt() + paddingTiles,
            minY = max(0, floor(minWorldY / TILE_SIZE_PX).toInt() - paddingTiles),
            maxY = min(maxTileY, floor(maxWorldY / TILE_SIZE_PX).toInt() + paddingTiles)
        )
    }
}

/**
 * URL builders are kept separate from Compose so endpoint changes can be
 * verified without an Android device.  The temporal source is attempted
 * first; the national satellite source is the runtime fallback.
 */
object TianDiTuTileSource {
    private const val NATIONAL_HOST_COUNT = 8
    private const val MULTI_TEMPORAL_SERVICE =
        "https://hunan.tianditu.gov.cn/iserver/services/map-mongodb/wmts100"
    private const val MULTI_TEMPORAL_LAYER = "dsx"
    private const val MULTI_TEMPORAL_MATRIX_SET = "dsx_Matrix_0"

    fun multiTemporalHunan(tile: MapTile, apiKey: String, time: String): String {
        return buildString {
            append(MULTI_TEMPORAL_SERVICE)
            append("?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0")
            append("&LAYER=").append(MULTI_TEMPORAL_LAYER)
            append("&STYLE=default&FORMAT=image/png")
            append("&TILEMATRIXSET=").append(MULTI_TEMPORAL_MATRIX_SET)
            append("&TILEMATRIX=").append(tile.zoom)
            append("&TILEROW=").append(tile.y)
            append("&TILECOL=").append(tile.x)
            append("&TIME=").append(encodeQueryValue(time))
            append("&USERECENT=true&tk=").append(encodeQueryValue(apiKey))
        }
    }

    fun currentSatellite(tile: MapTile, apiKey: String): String =
        nationalUrl(tile, layer = "img_w", apiKey = apiKey)

    fun satelliteLabels(tile: MapTile, apiKey: String): String =
        nationalUrl(tile, layer = "cia_w", apiKey = apiKey)

    private fun nationalUrl(tile: MapTile, layer: String, apiKey: String): String {
        val hostIndex = Math.floorMod(tile.x + tile.y, NATIONAL_HOST_COUNT)
        val x = TianDiTuProjection.wrapTileX(tile.x, tile.zoom)
        return "https://t$hostIndex.tianditu.gov.cn/DataServer?T=$layer&x=$x&y=${tile.y}&l=${tile.zoom}&tk=${encodeQueryValue(apiKey)}"
    }

    private fun encodeQueryValue(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                ' ' -> append("%20")
                '&' -> append("%26")
                '?' -> append("%3F")
                '=' -> append("%3D")
                '#' -> append("%23")
                '%' -> append("%25")
                else -> append(character)
            }
        }
    }
}
