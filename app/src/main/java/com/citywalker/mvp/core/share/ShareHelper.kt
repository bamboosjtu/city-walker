package com.citywalker.mvp.core.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.citywalker.mvp.R
import java.io.File

object ShareHelper {
    fun shareImage(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            context.getString(R.string.file_provider_authority),
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "我在城市探险家完成了：$title")
            clipData = ClipData.newRawUri("城市明信片", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享城市明信片"))
    }

    fun shareZip(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.getString(R.string.file_provider_authority),
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "城市探险家踩点数据包：${file.name}")
            clipData = ClipData.newRawUri("踩点 ZIP", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享踩点 ZIP"))
    }
}
