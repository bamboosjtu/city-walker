package com.citywalker.mvp.data

import android.content.Context
import com.citywalker.mvp.domain.Badge
import com.citywalker.mvp.domain.BadgeType
import com.citywalker.mvp.domain.Checkpoint
import com.citywalker.mvp.domain.CityConfig
import com.citywalker.mvp.domain.ContentBundle
import com.citywalker.mvp.domain.Dungeon
import com.citywalker.mvp.domain.PostcardDefinition
import com.citywalker.mvp.domain.Viewpoint
import org.json.JSONArray
import org.json.JSONObject

class ContentRepository(private val context: Context) {
    fun load(): ContentBundle {
        val text = context.assets.open("content/changsha.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val city = root.getJSONObject("city").toCity()
        val dungeons = root.getJSONArray("dungeons").let { array ->
            buildList(array.length()) { for (index in 0 until array.length()) add(array.getJSONObject(index).toDungeon()) }
        }
        val cityBadges = root.optJSONArray("cityBadges").toBadges()
        return ContentBundle(city = city, dungeons = dungeons, cityBadges = cityBadges)
    }

    private fun JSONObject.toCity(): CityConfig = CityConfig(
        id = getString("id"),
        name = getString("name"),
        subtitle = getString("subtitle"),
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
        zoom = getDouble("zoom"),
        coverImage = optString("coverImage"),
        dungeonIds = getJSONArray("dungeonIds").toStringList()
    )

    private fun JSONObject.toDungeon(): Dungeon {
        val checkpoint = getJSONObject("checkpoint").let { item ->
            Checkpoint(
                id = item.getString("id"),
                name = item.getString("name"),
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
                radiusMeters = item.getInt("radiusMeters"),
                mainTaskTitle = item.getString("mainTaskTitle"),
                mainTaskDescription = item.getString("mainTaskDescription")
            )
        }
        val mapPosition = optJSONObject("mapPosition")
        val viewpoints = optJSONArray("viewpoints").toViewpoints()
        return Dungeon(
            id = getString("id"),
            cityId = getString("cityId"),
            name = getString("name"),
            subtitle = getString("subtitle"),
            description = getString("description"),
            category = getString("category"),
            difficulty = getString("difficulty"),
            coverImage = optString("coverImage"),
            mapX = mapPosition?.optDouble("x", 50.0)?.toFloat() ?: 50f,
            mapY = mapPosition?.optDouble("y", 50.0)?.toFloat() ?: 50f,
            checkpoint = checkpoint,
            viewpoints = viewpoints,
            badges = optJSONArray("badges").toBadges(),
            postcard = getJSONObject("postcard").let { item ->
                PostcardDefinition(
                    title = item.getString("title"),
                    subtitle = item.getString("subtitle"),
                    locationText = item.getString("locationText"),
                    templateId = item.getString("templateId")
                )
            },
            creatorType = optString("creatorType", "official"),
            contentStatus = optString("status", "active")
        )
    }

    private fun JSONArray?.toViewpoints(): List<Viewpoint> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                add(
                    Viewpoint(
                        id = item.getString("id"),
                        dungeonId = item.getString("dungeonId"),
                        name = item.getString("name"),
                        subtitle = item.getString("subtitle"),
                        hint = item.getString("hint"),
                        referenceImages = item.getJSONArray("referenceImages").toStringList(),
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        source = item.optString("source", "official"),
                        status = item.optString("status", "active"),
                        recommendedOrientation = item.optString("recommendedOrientation"),
                        // Keep scoreThreshold as a backwards-compatible content key;
                        // semantically this value controls viewpoint discovery.
                        matchThreshold = item.optInt("matchThreshold", item.optInt("scoreThreshold", 55)),
                        badgeId = item.optString("badgeId"),
                        requiredForCompletion = item.optBoolean("requiredForCompletion", true)
                    )
                )
            }
        }
    }

    private fun JSONArray?.toBadges(): List<Badge> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                val type = runCatching { BadgeType.valueOf(item.optString("type", "MAIN").uppercase()) }.getOrDefault(BadgeType.MAIN)
                add(
                    Badge(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        description = item.getString("description"),
                        icon = item.optString("icon", "✦"),
                        type = type,
                        condition = item.optString("condition")
                    )
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList(length()) {
        for (index in 0 until length()) add(getString(index))
    }
}
