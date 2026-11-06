package com.citywalker.mvp.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Deterministic geographic helpers that do not depend on Android services. */
object LocationMath {
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Float {
        val earthRadiusMeters = 6_371_000.0
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val haversine = sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)
        val centralAngle = 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
        return (earthRadiusMeters * centralAngle).toFloat()
    }
}

/** Returns the dungeon whose checkpoint is closest to the supplied location. */
fun nearestDungeon(point: GeoPoint, dungeons: List<Dungeon>): Dungeon? =
    dungeons.minByOrNull { dungeon -> LocationMath.distanceMeters(point, dungeon.checkpoint.point) }
