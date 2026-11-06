package com.citywalker.mvp.core.navigation

import com.citywalker.mvp.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CoordinateConverterTest {
    @Test
    fun changshaWgs84CoordinateUsesStandardGcj02Delta() {
        val source = GeoPoint(latitude = 28.1812, longitude = 112.9601)
        val converted = CoordinateConverter.wgs84ToGcj02(source)

        assertEquals(28.1777367891, converted.latitude, 0.0000001)
        assertEquals(112.9656016003, converted.longitude, 0.0000001)
    }

    @Test
    fun coordinateOutsideChinaIsNotModified() {
        val source = GeoPoint(latitude = 40.7128, longitude = -74.0060, accuracyMeters = 7.8f)
        val converted = CoordinateConverter.wgs84ToGcj02(source)

        assertSame(source, converted)
    }
}
