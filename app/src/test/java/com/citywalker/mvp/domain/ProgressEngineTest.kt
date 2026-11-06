package com.citywalker.mvp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressEngineTest {
    @Test
    fun viewpointDiscoveryUsesSimilarityInsteadOfTotalScore() {
        val dungeon = testDungeon()
        val arrived = DungeonProgress(
            arrivedAt = 1L,
            arrivedLocation = GeoPoint(28.18, 112.96)
        )
        val main = ProgressEngine.completeMain(dungeon, arrived, photo(CaptureMode.MAIN, null, 2L))
        val result = ProgressEngine.recordViewpointScore(
            dungeon = dungeon,
            progress = main,
            viewpoint = dungeon.viewpoints.single(),
            photo = photo(CaptureMode.VIEWPOINT, "viewpoint-1", 3L),
            score = PhotoScore(60, similarity = 75, sharpness = 40, exposure = 40, matched = false, provider = "test", scoredAt = 3L)
        )

        assertTrue(result.viewpointProgress.getValue("viewpoint-1").completedAt != null)
        assertTrue(result.viewpointProgress.getValue("viewpoint-1").score?.matched == true)
        assertFalse("badge-photo" in result.unlockedBadges)
    }

    @Test
    fun photographyBadgeUsesTotalScoreIndependently() {
        val dungeon = testDungeon()
        val arrived = DungeonProgress(arrivedAt = 1L, arrivedLocation = GeoPoint(28.18, 112.96))
        val main = ProgressEngine.completeMain(dungeon, arrived, photo(CaptureMode.MAIN, null, 2L))
        val result = ProgressEngine.recordViewpointScore(
            dungeon,
            main,
            dungeon.viewpoints.single(),
            photo(CaptureMode.VIEWPOINT, "viewpoint-1", 3L),
            PhotoScore(85, similarity = 40, sharpness = 95, exposure = 95, matched = true, provider = "test", scoredAt = 3L)
        )

        assertFalse(result.viewpointProgress.getValue("viewpoint-1").completedAt != null)
        assertTrue("badge-photo" in result.unlockedBadges)
    }

    @Test
    fun mainOnlyDungeonIsOfficiallyComplete() {
        val dungeon = testDungeon(viewpoints = emptyList())
        val main = completedMain(dungeon)

        assertEquals(100, main.explorationPercent)
    }

    @Test
    fun oneRequiredViewpointSplitsTheRemainingFiftyPercent() {
        val dungeon = testDungeon(viewpoints = listOf(testViewpoint("required", required = true)))
        val main = completedMain(dungeon)
        val incomplete = ProgressEngine.normalize(dungeon, main)
        val complete = ProgressEngine.recordViewpointScore(
            dungeon,
            main,
            dungeon.viewpoints.single(),
            photo(CaptureMode.VIEWPOINT, "required", 3L),
            matchingScore()
        )

        assertEquals(50, incomplete.explorationPercent)
        assertEquals(100, complete.explorationPercent)
    }

    @Test
    fun twoRequiredViewpointsSplitTheRemainingFiftyPercentEvenly() {
        val viewpoints = listOf(
            testViewpoint("required-1", required = true),
            testViewpoint("required-2", required = true)
        )
        val dungeon = testDungeon(viewpoints = viewpoints)
        val main = completedMain(dungeon)
        val oneComplete = ProgressEngine.recordViewpointScore(
            dungeon,
            main,
            viewpoints.first(),
            photo(CaptureMode.VIEWPOINT, "required-1", 3L),
            matchingScore()
        )

        assertEquals(50, main.explorationPercent)
        assertEquals(75, oneComplete.explorationPercent)
    }

    @Test
    fun bonusViewpointsDoNotLowerAnExistingHundredPercent() {
        val required = testViewpoint("required", required = true)
        val bonus = testViewpoint("bonus", required = false)
        val dungeon = testDungeon(viewpoints = listOf(required, bonus))
        val main = completedMain(dungeon)
        val requiredComplete = ProgressEngine.recordViewpointScore(
            dungeon,
            main,
            required,
            photo(CaptureMode.VIEWPOINT, "required", 3L),
            matchingScore()
        )
        val withBonus = ProgressEngine.recordViewpointScore(
            dungeon,
            requiredComplete,
            bonus,
            photo(CaptureMode.VIEWPOINT, "bonus", 4L),
            matchingScore()
        )

        assertEquals(100, requiredComplete.explorationPercent)
        assertEquals(100, withBonus.explorationPercent)
    }

    private fun completedMain(dungeon: Dungeon): DungeonProgress =
        ProgressEngine.completeMain(
            dungeon,
            DungeonProgress(arrivedAt = 1L, arrivedLocation = GeoPoint(28.18, 112.96)),
            photo(CaptureMode.MAIN, null, 2L)
        )

    private fun matchingScore(): PhotoScore = PhotoScore(
        totalScore = 70,
        similarity = 85,
        sharpness = 60,
        exposure = 60,
        matched = false,
        provider = "test",
        scoredAt = 3L
    )

    private fun testDungeon(viewpoints: List<Viewpoint> = listOf(testViewpoint("viewpoint-1"))): Dungeon {
        return Dungeon(
            id = "dungeon-1",
            cityId = "changsha",
            name = "测试副本",
            subtitle = "测试",
            description = "测试",
            category = "测试",
            difficulty = "测试",
            coverImage = "",
            mapX = 50f,
            mapY = 50f,
            checkpoint = Checkpoint("checkpoint", "测试点", 28.18, 112.96, 100, "测试", "测试"),
            viewpoints = viewpoints,
            badges = listOf(Badge("badge-photo", "摄影徽章", "测试", "◎", BadgeType.PHOTO, "total_score_80")),
            postcard = PostcardDefinition("测试", "测试", "长沙", "test"),
            creatorType = "test",
            contentStatus = "active"
        )
    }

    private fun testViewpoint(id: String, required: Boolean = true): Viewpoint = Viewpoint(
            id = id,
            dungeonId = "dungeon-1",
            name = "测试机位",
            subtitle = "测试",
            hint = "测试",
            referenceImages = emptyList(),
            latitude = 28.18,
            longitude = 112.96,
            source = "test",
            status = "active",
            recommendedOrientation = "横构图",
            matchThreshold = 70,
            badgeId = "badge-photo",
            requiredForCompletion = required
        )

    private fun photo(mode: CaptureMode, viewpointId: String?, capturedAt: Long): PhotoRecord = PhotoRecord(
        filePath = "/tmp/$capturedAt.jpg",
        capturedAt = capturedAt,
        dungeonId = "dungeon-1",
        viewpointId = viewpointId,
        mode = mode,
        location = GeoPoint(28.18, 112.96)
    )
}
