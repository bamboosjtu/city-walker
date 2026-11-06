package com.citywalker.mvp.core.camera

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraOrientationTest {
    @Test
    fun mapsPortraitAndLandscapeDeviceAnglesToSurfaceRotations() {
        assertEquals(Surface.ROTATION_0, surfaceRotationForDeviceOrientation(0))
        assertEquals(Surface.ROTATION_270, surfaceRotationForDeviceOrientation(90))
        assertEquals(Surface.ROTATION_180, surfaceRotationForDeviceOrientation(180))
        assertEquals(Surface.ROTATION_90, surfaceRotationForDeviceOrientation(270))
    }

    @Test
    fun usesStableBoundariesForAllFourOrientations() {
        assertEquals(Surface.ROTATION_0, surfaceRotationForDeviceOrientation(44))
        assertEquals(Surface.ROTATION_270, surfaceRotationForDeviceOrientation(45))
        assertEquals(Surface.ROTATION_270, surfaceRotationForDeviceOrientation(134))
        assertEquals(Surface.ROTATION_180, surfaceRotationForDeviceOrientation(135))
        assertEquals(Surface.ROTATION_180, surfaceRotationForDeviceOrientation(224))
        assertEquals(Surface.ROTATION_90, surfaceRotationForDeviceOrientation(225))
        assertEquals(Surface.ROTATION_90, surfaceRotationForDeviceOrientation(314))
        assertEquals(Surface.ROTATION_0, surfaceRotationForDeviceOrientation(315))
    }

    @Test
    fun ignoresUnknownOrientationEvents() {
        assertNull(surfaceRotationForDeviceOrientation(-1))
    }
}
