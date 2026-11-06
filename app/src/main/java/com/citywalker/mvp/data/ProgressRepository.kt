package com.citywalker.mvp.data

import android.content.Context
import com.citywalker.mvp.domain.CaptureMode
import com.citywalker.mvp.domain.DungeonProgress
import com.citywalker.mvp.domain.GeoPoint
import com.citywalker.mvp.domain.PhotoRecord
import com.citywalker.mvp.domain.PhotoScore
import com.citywalker.mvp.domain.PlayerProgress
import com.citywalker.mvp.domain.ViewpointProgress
import org.json.JSONArray
import org.json.JSONObject

class ProgressRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PlayerProgress {
        val raw = preferences.getString(KEY_PROGRESS, null) ?: return PlayerProgress()
        return runCatching {
            val root = JSONObject(raw)
            val dungeons = mutableMapOf<String, DungeonProgress>()
            val dungeonJson = root.optJSONObject("dungeons") ?: JSONObject()
            dungeonJson.keys().forEach { dungeonId ->
                dungeons[dungeonId] = dungeonJson.getJSONObject(dungeonId).toDungeonProgress()
            }
            PlayerProgress(
                dungeons = dungeons,
                cityBadges = root.optJSONArray("cityBadges").toStringSet(),
                updatedAt = root.optLong("updatedAt").takeIf { it > 0L }
            )
        }.getOrDefault(PlayerProgress())
    }

    fun save(progress: PlayerProgress) {
        val dungeons = JSONObject()
        progress.dungeons.forEach { (dungeonId, dungeonProgress) ->
            dungeons.put(dungeonId, dungeonProgress.toJson())
        }
        val root = JSONObject()
            .put("dungeons", dungeons)
            .put("cityBadges", JSONArray(progress.cityBadges.toList()))
            .put("updatedAt", System.currentTimeMillis())
        preferences.edit().putString(KEY_PROGRESS, root.toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_PROGRESS).apply()
    }

    private fun JSONObject.toDungeonProgress(): DungeonProgress {
        val viewpointJson = optJSONObject("viewpointProgress") ?: JSONObject()
        val viewpoints = mutableMapOf<String, ViewpointProgress>()
        viewpointJson.keys().forEach { viewpointId ->
            viewpoints[viewpointId] = viewpointJson.getJSONObject(viewpointId).toViewpointProgress()
        }
        val status = runCatching {
            com.citywalker.mvp.domain.DungeonStatus.valueOf(optString("status", "LOCKED"))
        }.getOrDefault(com.citywalker.mvp.domain.DungeonStatus.LOCKED)
        return DungeonProgress(
            status = status,
            arrivedAt = optLong("arrivedAt").takeIf { it > 0L },
            arrivedLocation = optJSONObject("arrivedLocation")?.toGeoPoint(),
            mainCompletedAt = optLong("mainCompletedAt").takeIf { it > 0L },
            mainPhoto = optJSONObject("mainPhoto")?.toPhotoRecord(),
            viewpointProgress = viewpoints,
            freeCaptures = optJSONArray("freeCaptures").toPhotoRecords(),
            explorationPercent = optInt("explorationPercent", 0),
            unlockedBadges = optJSONArray("unlockedBadges").toStringSet()
        )
    }

    private fun DungeonProgress.toJson(): JSONObject = JSONObject()
        .put("status", status.name)
        .put("arrivedAt", arrivedAt ?: JSONObject.NULL)
        .put("arrivedLocation", arrivedLocation?.toJson() ?: JSONObject.NULL)
        .put("mainCompletedAt", mainCompletedAt ?: JSONObject.NULL)
        .put("mainPhoto", mainPhoto?.toJson() ?: JSONObject.NULL)
        .put("viewpointProgress", JSONObject().also { root ->
            viewpointProgress.forEach { (id, progress) -> root.put(id, progress.toJson()) }
        })
        .put("freeCaptures", JSONArray(freeCaptures.map { it.toJson() }))
        .put("explorationPercent", explorationPercent)
        .put("unlockedBadges", JSONArray(unlockedBadges.toList()))

    private fun JSONObject.toViewpointProgress(): ViewpointProgress = ViewpointProgress(
        lastAttemptAt = optLong("lastAttemptAt").takeIf { it > 0L },
        completedAt = optLong("completedAt").takeIf { it > 0L },
        photo = optJSONObject("photo")?.toPhotoRecord(),
        score = optJSONObject("score")?.toPhotoScore()
    )

    private fun ViewpointProgress.toJson(): JSONObject = JSONObject()
        .put("lastAttemptAt", lastAttemptAt ?: JSONObject.NULL)
        .put("completedAt", completedAt ?: JSONObject.NULL)
        .put("photo", photo?.toJson() ?: JSONObject.NULL)
        .put("score", score?.toJson() ?: JSONObject.NULL)

    private fun JSONObject.toGeoPoint(): GeoPoint = GeoPoint(
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
        accuracyMeters = optDouble("accuracyMeters", Double.NaN)
            .takeIf { !it.isNaN() }
            ?.toFloat(),
        simulated = optBoolean("simulated", false)
    )

    private fun GeoPoint.toJson(): JSONObject = JSONObject()
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("accuracyMeters", accuracyMeters ?: JSONObject.NULL)
        .put("simulated", simulated)

    private fun JSONObject.toPhotoRecord(): PhotoRecord {
        val mode = runCatching { CaptureMode.valueOf(optString("mode", "MAIN")) }.getOrDefault(CaptureMode.MAIN)
        return PhotoRecord(
            filePath = getString("filePath"),
            capturedAt = optLong("capturedAt"),
            dungeonId = getString("dungeonId"),
            viewpointId = optString("viewpointId").takeIf { it.isNotBlank() },
            mode = mode,
            location = getJSONObject("location").toGeoPoint(),
            note = optString("note").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun PhotoRecord.toJson(): JSONObject = JSONObject()
        .put("filePath", filePath)
        .put("capturedAt", capturedAt)
        .put("dungeonId", dungeonId)
        .put("viewpointId", viewpointId ?: JSONObject.NULL)
        .put("mode", mode.name)
        .put("location", location.toJson())
        .put("note", note ?: JSONObject.NULL)

    private fun JSONArray?.toPhotoRecords(): List<PhotoRecord> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toPhotoRecord())
            }
        }
    }

    private fun JSONObject.toPhotoScore(): PhotoScore = PhotoScore(
        totalScore = optInt("totalScore"),
        similarity = optInt("similarity"),
        sharpness = optInt("sharpness"),
        exposure = optInt("exposure"),
        matched = optBoolean("matched"),
        provider = optString("provider", "mock-local"),
        scoredAt = optLong("scoredAt")
    )

    private fun PhotoScore.toJson(): JSONObject = JSONObject()
        .put("totalScore", totalScore)
        .put("similarity", similarity)
        .put("sharpness", sharpness)
        .put("exposure", exposure)
        .put("matched", matched)
        .put("provider", provider)
        .put("scoredAt", scoredAt)

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet { for (index in 0 until length()) add(getString(index)) }
    }

    private companion object {
        const val PREFERENCES_NAME = "city_walker_progress"
        const val KEY_PROGRESS = "player_progress_v1"
    }
}
