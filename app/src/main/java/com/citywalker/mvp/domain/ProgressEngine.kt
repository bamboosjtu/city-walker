package com.citywalker.mvp.domain

import java.util.Locale

object ProgressEngine {
    fun progressFor(all: PlayerProgress, dungeonId: String): DungeonProgress =
        all.dungeons[dungeonId] ?: DungeonProgress()

    fun isMainCompleted(progress: DungeonProgress): Boolean =
        progress.mainCompletedAt != null && progress.mainPhoto != null

    fun completedViewpoints(dungeon: Dungeon, progress: DungeonProgress): Int =
        dungeon.viewpoints.count {
            it.requiredForCompletion && progress.viewpointProgress[it.id]?.completedAt != null
        }

    fun explorationPercent(dungeon: Dungeon, progress: DungeonProgress): Int {
        if (!isMainCompleted(progress)) return 0
        val requiredViewpoints = dungeon.viewpoints.count { it.requiredForCompletion }
        if (requiredViewpoints == 0) return 100
        return (50f + completedViewpoints(dungeon, progress) * (50f / requiredViewpoints)).toInt().coerceAtMost(100)
    }

    fun resolveStatus(dungeon: Dungeon, progress: DungeonProgress): DungeonStatus = when {
        !isMainCompleted(progress) && progress.arrivedAt != null -> DungeonStatus.ARRIVED
        !isMainCompleted(progress) -> DungeonStatus.LOCKED
        explorationPercent(dungeon, progress) >= 100 -> DungeonStatus.COMPLETED_100
        completedViewpoints(dungeon, progress) > 0 -> DungeonStatus.EXPLORING
        else -> DungeonStatus.MAIN_COMPLETED
    }

    fun normalize(dungeon: Dungeon, progress: DungeonProgress): DungeonProgress {
        val next = progress.copy(
            explorationPercent = explorationPercent(dungeon, progress),
            status = resolveStatus(dungeon, progress),
            unlockedBadges = progress.unlockedBadges.toSet()
        )
        return next
    }

    fun markArrived(dungeon: Dungeon, progress: DungeonProgress, location: GeoPoint): DungeonProgress {
        val next = if (progress.arrivedAt == null) {
            progress.copy(arrivedAt = System.currentTimeMillis(), arrivedLocation = location)
        } else {
            progress
        }
        return normalize(dungeon, next)
    }

    fun completeMain(dungeon: Dungeon, progress: DungeonProgress, photo: PhotoRecord): DungeonProgress {
        require(progress.arrivedAt != null) { "尚未进入主线打卡范围" }
        val badge = dungeon.badges.firstOrNull { it.type == BadgeType.MAIN }
        val next = normalize(
            dungeon,
            progress.copy(
                mainCompletedAt = progress.mainCompletedAt ?: System.currentTimeMillis(),
                mainPhoto = photo,
                unlockedBadges = progress.unlockedBadges + listOfNotNull(badge?.id)
            )
        )
        return addExploreBadgeIfComplete(dungeon, next)
    }

    fun recordViewpointScore(
        dungeon: Dungeon,
        progress: DungeonProgress,
        viewpoint: Viewpoint,
        photo: PhotoRecord,
        score: PhotoScore
    ): DungeonProgress {
        require(isMainCompleted(progress)) { "完成副本主线后才能挑战经典机位" }
        val previous = progress.viewpointProgress[viewpoint.id]
        // Discovery is a composition/reference-image decision. Total score is
        // intentionally reserved for photography badges.
        val matched = score.similarity >= viewpoint.matchThreshold
        val normalizedScore = score.copy(matched = matched)
        val nextViewpoint = ViewpointProgress(
            lastAttemptAt = System.currentTimeMillis(),
            completedAt = if (matched) previous?.completedAt ?: System.currentTimeMillis() else previous?.completedAt,
            photo = photo,
            score = normalizedScore
        )
        var badges = progress.unlockedBadges
        if (score.totalScore >= 80) {
            dungeon.badges.firstOrNull { it.type == BadgeType.PHOTO }?.let { badges = badges + it.id }
        }
        val next = progress.copy(
            viewpointProgress = progress.viewpointProgress + (viewpoint.id to nextViewpoint),
            unlockedBadges = badges
        )
        val normalized = normalize(dungeon, next)
        return addExploreBadgeIfComplete(dungeon, normalized)
    }

    fun cityStats(content: ContentBundle, progress: PlayerProgress): CityStats {
        val completed = content.dungeons.count { isMainCompleted(progressFor(progress, it.id)) }
        val totalExploration = content.dungeons.sumOf { explorationPercent(it, progressFor(progress, it.id)) }
        return CityStats(
            completedCount = completed,
            totalCount = content.dungeons.size,
            explorationPercent = if (content.dungeons.isEmpty()) 0 else totalExploration / content.dungeons.size,
            cityComplete = content.dungeons.isNotEmpty() && completed == content.dungeons.size
        )
    }

    fun statusLabel(status: DungeonStatus): String = when (status) {
        DungeonStatus.LOCKED -> "未到达"
        DungeonStatus.ARRIVED -> "已到达"
        DungeonStatus.MAIN_COMPLETED -> "主线完成"
        DungeonStatus.EXPLORING -> "探索中"
        DungeonStatus.COMPLETED_100 -> "100% 完成"
    }

    fun scoreStars(score: Int): String = buildString {
        repeat(5) { append(if (score >= (it + 1) * 20) "★" else "☆") }
    }

    fun percentLabel(value: Int): String = String.format(Locale.CHINA, "%d%%", value)

    private fun addExploreBadgeIfComplete(dungeon: Dungeon, progress: DungeonProgress): DungeonProgress {
        if (progress.explorationPercent < 100) return progress
        val badge = dungeon.badges.firstOrNull { it.type == BadgeType.EXPLORE } ?: return progress
        return normalize(dungeon, progress.copy(unlockedBadges = progress.unlockedBadges + badge.id))
    }
}
