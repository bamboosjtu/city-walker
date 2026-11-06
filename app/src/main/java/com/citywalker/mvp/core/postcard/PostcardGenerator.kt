package com.citywalker.mvp.core.postcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.citywalker.mvp.core.photo.PhotoBitmapLoader
import com.citywalker.mvp.domain.CityConfig
import com.citywalker.mvp.domain.CityStats
import com.citywalker.mvp.domain.CaptureMode
import com.citywalker.mvp.domain.Dungeon
import com.citywalker.mvp.domain.PhotoRecord
import com.citywalker.mvp.domain.PhotoScore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostcardGenerator(private val context: Context) {
    fun generate(
        city: CityConfig,
        dungeon: Dungeon,
        photo: PhotoRecord,
        score: PhotoScore?,
        stats: CityStats
    ): File {
        val width = 1080
        val height = 1440
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paper = Color.rgb(244, 240, 232)
        val ink = Color.rgb(11, 31, 34)
        val orange = Color.rgb(233, 161, 91)
        val muted = Color.rgb(101, 111, 108)

        canvas.drawColor(paper)
        val imageRect = RectF(48f, 48f, width - 48f, 750f)
        drawPhoto(canvas, photo.filePath, imageRect, orange)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = orange }
        canvas.drawRoundRect(RectF(48f, 790f, 210f, 802f), 6f, 6f, accentPaint)

        drawText(canvas, "CITY WALKER", 48f, 870f, 30f, muted, bold = true, letterSpacing = 0.18f)
        drawText(canvas, dungeon.name, 48f, 948f, 66f, ink, bold = true)
        drawText(canvas, dungeon.subtitle, 48f, 1000f, 30f, muted)
        drawText(canvas, "${city.name}  ·  ${dungeon.category}", 48f, 1060f, 28f, ink, bold = true)

        val detail = when {
            score != null -> "经典机位 ${score.totalScore} 分  ${if (score.matched) "已匹配" else "再试一次"}"
            photo.mode == CaptureMode.FREE -> "自由探索记录  ·  ${stats.completedCount}/${stats.totalCount} 副本已通关"
            else -> "主线任务完成  ·  ${stats.completedCount}/${stats.totalCount} 副本已通关"
        }
        drawText(canvas, detail, 48f, 1120f, 28f, muted)
        drawText(canvas, "长沙图鉴 ${stats.completedCount}/${stats.totalCount}  ·  城市探索 ${stats.explorationPercent}%", 48f, 1170f, 24f, muted)

        val date = SimpleDateFormat("yyyy.MM.dd  HH:mm", Locale.CHINA).format(Date(photo.capturedAt))
        drawText(canvas, date, 48f, 1300f, 24f, muted)
        drawText(canvas, "把城市走成自己的地图", 48f, 1360f, 26f, ink, bold = true)

        val file = File(File(context.filesDir, "postcards").apply { mkdirs() }, "${dungeon.id}_${photo.capturedAt}.jpg")
        FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        output.recycle()
        return file
    }

    private fun drawPhoto(canvas: Canvas, path: String, target: RectF, fallback: Int) {
        val photo = PhotoBitmapLoader.load(path)
        if (photo == null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    target.left,
                    target.top,
                    target.right,
                    target.bottom,
                    fallback,
                    Color.rgb(19, 57, 58),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(target, 26f, 26f, paint)
            drawText(canvas, "YOUR CITY MOMENT", target.left + 34f, target.centerY(), 30f, Color.WHITE, bold = true)
            return
        }

        val sourceRatio = photo.width.toFloat() / photo.height.toFloat()
        val targetRatio = target.width() / target.height()
        val source = if (sourceRatio > targetRatio) {
            val cropWidth = (photo.height * targetRatio).toInt()
            val left = (photo.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, photo.height)
        } else {
            val cropHeight = (photo.width / targetRatio).toInt()
            val top = (photo.height - cropHeight) / 2
            Rect(0, top, photo.width, top + cropHeight)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(photo, source, target, paint)
        photo.recycle()

        val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                target.left,
                target.top,
                target.left,
                target.bottom,
                Color.TRANSPARENT,
                Color.argb(180, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(target, 26f, 26f, overlay)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        letterSpacing: Float = 0f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            this.letterSpacing = letterSpacing
        }
        canvas.drawText(text, x, y, paint)
    }
}
