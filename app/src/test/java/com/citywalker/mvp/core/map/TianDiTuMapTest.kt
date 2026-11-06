package com.citywalker.mvp.core.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TianDiTuMapTest {
    @Test
    fun equatorAndPrimeMeridianProjectToWorldCenter() {
        val zoom = 12
        val worldCenter = TianDiTuProjection.worldSizePx(zoom) / 2.0

        assertEquals(worldCenter, TianDiTuProjection.worldPixelX(0.0, zoom), 0.0001)
        assertEquals(worldCenter, TianDiTuProjection.worldPixelY(0.0, zoom), 0.0001)
    }

    @Test
    fun latitudeIncreasesUpwardOnMap() {
        val zoom = 12

        assertTrue(
            TianDiTuProjection.worldPixelY(28.2, zoom) <
                TianDiTuProjection.worldPixelY(28.1, zoom)
        )
    }

    @Test
    fun tileCoordinatesStayInsideTheZoomGrid() {
        val zoom = 12
        val tile = TianDiTuProjection.tileFor(28.1152066, 113.00664575, zoom)
        val tileCount = 1 shl zoom

        assertEquals(zoom, tile.zoom)
        assertTrue(tile.x in 0 until tileCount)
        assertTrue(tile.y in 0 until tileCount)
    }

    @Test
    fun tileXWrapsForLongitudesOutsideTheWorld() {
        val zoom = 3
        assertEquals(0, TianDiTuProjection.wrapTileX(8, zoom))
        assertEquals(7, TianDiTuProjection.wrapTileX(-1, zoom))
    }

    @Test
    fun projectedCheckpointUsesViewportCenterAsMapOrigin() {
        val point = TianDiTuProjection.projectToMap(
            latitude = 28.181234,
            longitude = 112.960456,
            centerLatitude = 28.181234,
            centerLongitude = 112.960456,
            zoom = 12,
            viewportWidthPx = 1080f,
            viewportHeightPx = 1920f
        )

        assertEquals(540f, point.x, 0.001f)
        assertEquals(960f, point.y, 0.001f)
    }

    @Test
    fun temporalUrlContainsWMTSRequestAndEncodedTime() {
        val url = TianDiTuTileSource.multiTemporalHunan(
            tile = MapTile(zoom = 12, x = 100, y = 200),
            apiKey = "demo-key",
            time = "2022-12-31 00:00:00"
        )

        assertTrue(url.contains("SERVICE=WMTS"))
        assertTrue(url.contains("REQUEST=GetTile"))
        assertTrue(url.contains("TILEMATRIX=12"))
        assertTrue(url.contains("TILEROW=200"))
        assertTrue(url.contains("TILECOL=100"))
        assertTrue(url.contains("TIME=2022-12-31%2000:00:00"))
        assertTrue(url.endsWith("tk=demo-key"))
    }
}
