package com.citywalker.mvp.core.camera

import android.view.Surface

/**
 * Converts OrientationEventListener's clockwise device angle into the
 * corresponding CameraX/Surface rotation. The UI can stay portrait-locked;
 * this only describes how the captured pixels should be oriented.
 */
fun surfaceRotationForDeviceOrientation(orientationDegrees: Int): Int? {
    if (orientationDegrees < 0) return null
    return when (orientationDegrees) {
        in 45..134 -> Surface.ROTATION_270
        in 135..224 -> Surface.ROTATION_180
        in 225..314 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}
