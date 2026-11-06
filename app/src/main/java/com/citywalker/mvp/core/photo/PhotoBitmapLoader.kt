package com.citywalker.mvp.core.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

/**
 * Loads a photo with the orientation described by its JPEG EXIF metadata applied.
 *
 * Camera images often keep their pixels in the sensor's landscape orientation and
 * store the real display direction in EXIF. BitmapFactory does not apply that
 * metadata, so every local-photo consumer should come through this loader.
 */
object PhotoBitmapLoader {
    fun load(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null

        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        val needsTransform = when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                true
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
                true
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
                true
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
                true
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
                true
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
                true
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(-90f)
                true
            }

            else -> false
        }

        if (!needsTransform) return bitmap

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { corrected ->
                if (corrected !== bitmap) bitmap.recycle()
            }
        }.getOrElse { bitmap }
    }
}
