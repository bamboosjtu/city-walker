package com.citywalker.mvp.core.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.citywalker.mvp.domain.Dungeon
import com.citywalker.mvp.domain.GeoPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val AXIS = 6378245.0
private const val EE = 0.00669342162296594323
private const val PI = Math.PI

object CoordinateConverter {
    fun wgs84ToGcj02(point: GeoPoint): GeoPoint {
        if (outOfChina(point.latitude, point.longitude)) return point
        val dLat = transformLatitude(point.longitude - 105.0, point.latitude - 35.0)
        val dLon = transformLongitude(point.longitude - 105.0, point.latitude - 35.0)
        val radLat = point.latitude / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        // transformLatitude/Longitude return meter-like deltas. Convert those
        // deltas to degrees first, then add them to the WGS-84 coordinate.
        // The previous implementation subtracted a value derived from the
        // original latitude/longitude, which is not the WGS-84 -> GCJ-02
        // transform and can send map navigation to the wrong place.
        val deltaLat = dLat * 180.0 / ((AXIS * (1 - EE)) / (magic * sqrtMagic) * PI)
        val deltaLon = dLon * 180.0 / (AXIS / sqrtMagic * cos(radLat) * PI)
        return point.copy(latitude = point.latitude + deltaLat, longitude = point.longitude + deltaLon)
    }

    fun gcj02ToBd09(point: GeoPoint): GeoPoint {
        val z = sqrt(point.longitude * point.longitude + point.latitude * point.latitude) + 0.00002 * sin(point.latitude * PI * 3000.0 / 180.0)
        val theta = atan2(point.latitude, point.longitude) + 0.000003 * cos(point.longitude * PI * 3000.0 / 180.0)
        return point.copy(latitude = z * sin(theta) + 0.006, longitude = z * cos(theta) + 0.0065)
    }

    private fun outOfChina(latitude: Double, longitude: Double): Boolean = longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271

    private fun transformLatitude(x: Double, y: Double): Double {
        var ret = -100 + 2 * x + 3 * y + .2 * y * y + .1 * x * y + .2 * sqrt(abs(x))
        ret += (20 * sin(6 * x * PI) + 20 * sin(2 * x * PI)) * 2 / 3
        ret += (20 * sin(y * PI) + 40 * sin(y / 3 * PI)) * 2 / 3
        ret += (160 * sin(y / 12 * PI) + 320 * sin(y / 30 * PI)) * 2 / 3
        return ret
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var ret = 300 + x + 2 * y + .1 * x * x + .1 * x * y + .1 * sqrt(abs(x))
        ret += (20 * sin(6 * x * PI) + 20 * sin(2 * x * PI)) * 2 / 3
        ret += (20 * sin(x * PI) + 40 * sin(x / 3 * PI)) * 2 / 3
        ret += (150 * sin(x / 12 * PI) + 300 * sin(x / 30 * PI)) * 2 / 3
        return ret
    }
}

object NavigationLauncher {
    fun open(context: Context, dungeon: Dungeon, provider: String): Boolean {
        val wgs = dungeon.checkpoint.point
        val target = if (provider == "baidu") CoordinateConverter.gcj02ToBd09(CoordinateConverter.wgs84ToGcj02(wgs)) else CoordinateConverter.wgs84ToGcj02(wgs)
        val intentUri = if (provider == "baidu") {
            Uri.parse("baidumap://map/marker?location=${target.latitude},${target.longitude}&title=${Uri.encode(dungeon.name)}&content=${Uri.encode(dungeon.checkpoint.name)}&coord_type=bd09ll")
        } else {
            Uri.parse("androidamap://viewMap?sourceApplication=CityWalker&poiname=${Uri.encode(dungeon.name)}&lat=${target.latitude}&lon=${target.longitude}&dev=0")
        }
        return runCatching {
            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val generic = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${wgs.latitude},${wgs.longitude}?q=${Uri.encode(dungeon.name)}"))
                context.startActivity(Intent.createChooser(generic, "选择地图应用"))
            }
            true
        }.getOrDefault(false)
    }

    fun copyLocation(context: Context, dungeon: Dungeon) {
        val text = "${dungeon.name} · ${dungeon.checkpoint.name}\n${dungeon.checkpoint.latitude}, ${dungeon.checkpoint.longitude}"
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("City Walker location", text))
    }
}
