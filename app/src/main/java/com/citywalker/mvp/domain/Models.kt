package com.citywalker.mvp.domain

enum class DungeonStatus {
    LOCKED,
    ARRIVED,
    MAIN_COMPLETED,
    EXPLORING,
    COMPLETED_100
}

enum class BadgeType {
    MAIN,
    PHOTO,
    EXPLORE,
    CITY
}

enum class CaptureMode {
    MAIN,
    VIEWPOINT,
    SURVEY_VIEWPOINT,
    FREE
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val simulated: Boolean = false
)

data class CityConfig(
    val id: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val coverImage: String,
    val dungeonIds: List<String>
)

data class Checkpoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val mainTaskTitle: String,
    val mainTaskDescription: String
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

data class Viewpoint(
    val id: String,
    val dungeonId: String,
    val name: String,
    val subtitle: String,
    val hint: String,
    val referenceImages: List<String>,
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val status: String,
    val recommendedOrientation: String,
    val matchThreshold: Int,
    val badgeId: String,
    val requiredForCompletion: Boolean = true
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val type: BadgeType,
    val condition: String
)

data class PostcardDefinition(
    val title: String,
    val subtitle: String,
    val locationText: String,
    val templateId: String
)

data class Dungeon(
    val id: String,
    val cityId: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val coverImage: String,
    val mapX: Float,
    val mapY: Float,
    val checkpoint: Checkpoint,
    val viewpoints: List<Viewpoint>,
    val badges: List<Badge>,
    val postcard: PostcardDefinition,
    val creatorType: String,
    val contentStatus: String
)

data class ContentBundle(
    val city: CityConfig,
    val dungeons: List<Dungeon>,
    val cityBadges: List<Badge>
)

data class PhotoRecord(
    val filePath: String,
    val capturedAt: Long,
    val dungeonId: String,
    val viewpointId: String? = null,
    val mode: CaptureMode,
    val location: GeoPoint,
    val note: String? = null
)

data class PhotoScore(
    val totalScore: Int,
    val similarity: Int,
    val sharpness: Int,
    val exposure: Int,
    val matched: Boolean,
    val provider: String,
    val scoredAt: Long
)

data class ViewpointProgress(
    val lastAttemptAt: Long? = null,
    val completedAt: Long? = null,
    val photo: PhotoRecord? = null,
    val score: PhotoScore? = null
)

data class DungeonProgress(
    val status: DungeonStatus = DungeonStatus.LOCKED,
    val arrivedAt: Long? = null,
    val arrivedLocation: GeoPoint? = null,
    val mainCompletedAt: Long? = null,
    val mainPhoto: PhotoRecord? = null,
    val viewpointProgress: Map<String, ViewpointProgress> = emptyMap(),
    val freeCaptures: List<PhotoRecord> = emptyList(),
    val explorationPercent: Int = 0,
    val unlockedBadges: Set<String> = emptySet()
)

data class PlayerProgress(
    val dungeons: Map<String, DungeonProgress> = emptyMap(),
    val cityBadges: Set<String> = emptySet(),
    val updatedAt: Long? = null
)

data class CityStats(
    val completedCount: Int,
    val totalCount: Int,
    val explorationPercent: Int,
    val cityComplete: Boolean
)

data class CaptureResult(
    val mode: CaptureMode,
    val dungeonId: String,
    val viewpointId: String? = null,
    val photo: PhotoRecord,
    val score: PhotoScore? = null,
    val isScoring: Boolean = false
)
