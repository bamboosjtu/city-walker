package com.citywalker.mvp.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.ExifInterface
import com.citywalker.mvp.core.photo.PhotoBitmapLoader
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoRepository(private val context: Context) {
    private val photoDirectory: File by lazy {
        File(context.filesDir, "photos").apply { mkdirs() }
    }

    fun newPhotoFile(prefix: String): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(photoDirectory, "${prefix}_$stamp.jpg")
    }

    /**
     * Rewrites a camera JPEG so its pixels and EXIF both describe the same
     * orientation. This keeps exported photos safe for consumers that ignore EXIF.
     */
    fun normalizeOrientation(file: File): Boolean {
        if (!file.isFile) return false
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return runCatching {
                ExifInterface(file.absolutePath).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    saveAttributes()
                }
                true
            }.getOrDefault(false)
        }

        val normalized = PhotoBitmapLoader.load(file.absolutePath) ?: return false
        val temporary = File(file.parentFile ?: photoDirectory, "${file.name}.normalized")
        return try {
            FileOutputStream(temporary).use { output ->
                check(normalized.compress(Bitmap.CompressFormat.JPEG, 95, output)) { "无法写入方向已归一化的照片" }
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            normalized.recycle()
            if (temporary.exists()) temporary.delete()
        }
    }

    fun saveDemoPhoto(file: File, title: String, subtitle: String): String {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), Color.rgb(29, 75, 73), Color.rgb(10, 25, 34), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        canvas.drawCircle(800f, 270f, 220f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 161, 92); alpha = 200 })
        val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 42, 46); alpha = 235 }
        val ridge = Path().apply {
            moveTo(0f, 1040f)
            lineTo(280f, 700f)
            lineTo(475f, 910f)
            lineTo(690f, 650f)
            lineTo(width.toFloat(), 1010f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(ridge, ridgePaint)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(246, 234, 215)
            textSize = 58f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(214, 202, 185)
            textSize = 30f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 161, 92)
            textSize = 21f
            letterSpacing = .16f
        }
        canvas.drawText("CITY WALKER / FIELD PHOTO", 72f, 102f, bodyPaint)
        canvas.drawText(title, 72f, 1240f, titlePaint)
        canvas.drawText(subtitle, 74f, 1300f, bodyPaint)
        canvas.drawText("DEMO SHUTTER / ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}", 74f, 1360f, monoPaint)
        saveBitmap(bitmap, file)
        bitmap.recycle()
        return file.absolutePath
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
        }
    }
}
