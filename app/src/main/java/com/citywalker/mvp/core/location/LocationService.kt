package com.citywalker.mvp.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.citywalker.mvp.domain.GeoPoint
import com.citywalker.mvp.domain.LocationMath
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    fun hasPermission(): Boolean = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun current(onLocation: (GeoPoint) -> Unit, onError: (Throwable) -> Unit) {
        if (!hasPermission()) {
            onError(IllegalStateException("定位权限未开启"))
            return
        }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) onLocation(location.toGeoPoint())
                else client.lastLocation.addOnSuccessListener { last ->
                    if (last != null) onLocation(last.toGeoPoint()) else onError(IllegalStateException("暂时无法获取当前位置"))
                }.addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    @SuppressLint("MissingPermission")
    suspend fun currentOnce(): GeoPoint = suspendCancellableCoroutine { continuation ->
        if (!hasPermission()) {
            continuation.resumeWithException(IllegalStateException("定位权限未开启"))
            return@suspendCancellableCoroutine
        }

        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    if (continuation.isActive) continuation.resume(location.toGeoPoint())
                } else {
                    client.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (continuation.isActive) {
                                if (lastLocation != null) continuation.resume(lastLocation.toGeoPoint())
                                else continuation.resumeWithException(IllegalStateException("暂时无法获取当前位置"))
                            }
                        }
                        .addOnFailureListener { error ->
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                }
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        continuation.invokeOnCancellation { cancellation.cancel() }
    }

    @SuppressLint("MissingPermission")
    fun startWatching(onLocation: (GeoPoint) -> Unit, onError: (Throwable) -> Unit) {
        if (!hasPermission() || callback != null) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setWaitForAccurateLocation(false)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it.toGeoPoint()) }
            }
        }
        client.requestLocationUpdates(request, callback!!, context.mainLooper).addOnFailureListener(onError)
    }

    fun stopWatching() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        simulated = false
    )
}

fun distanceBetweenMeters(from: GeoPoint, to: GeoPoint): Float {
    return LocationMath.distanceMeters(from, to)
}

fun formatDistance(meters: Float?): String = when {
    meters == null -> "等待定位"
    meters < 1000f -> "${meters.toInt()} m"
    else -> "%.1f km".format(meters / 1000f)
}
