package com.citywalker.mvp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentOverlayTest {
    @Test
    fun verifiedSurveyCheckpointAndOfficialViewpointOverrideSeedContent() {
        val base = testContent()
        val checkpoint = SurveyCapture(
            SurveyPoint(
                dungeonId = "dungeon",
                type = SurveyPointType.CHECKPOINT,
                latitude = 28.2001,
                longitude = 112.9001,
                accuracyMeters = 5f,
                capturedAt = 1L
            )
        )
        val officialCapture = SurveyCapture(
            SurveyPoint(
                dungeonId = "dungeon",
                type = SurveyPointType.VIEWPOINT,
                viewpointId = "official",
                latitude = 28.2002,
                longitude = 112.9002,
                accuracyMeters = 5f,
                capturedAt = 2L,
                photoPath = "/tmp/official-reference.jpg"
            )
        )
        val effective = base.withSurveyOverlay(
            SurveyBook(
                dungeons = mapOf(
                    "dungeon" to DungeonSurvey(
                        dungeonId = "dungeon",
                        checkpoint = checkpoint,
                        viewpoints = listOf(
                            SurveyViewpointDraft(
                                viewpointId = "official",
                                name = "官方机位",
                                capture = officialCapture,
                                hint = "现场 hint",
                                verified = true,
                                status = "active"
                            )
                        )
                    )
                )
            )
        )
        val dungeon = effective.dungeons.single()

        assertEquals(28.2001, dungeon.checkpoint.latitude, 0.000001)
        assertEquals(112.9001, dungeon.checkpoint.longitude, 0.000001)
        assertEquals(28.2002, dungeon.viewpoints.single().latitude, 0.000001)
        assertEquals(112.9002, dungeon.viewpoints.single().longitude, 0.000001)
        assertEquals(listOf("/tmp/official-reference.jpg"), dungeon.viewpoints.single().referenceImages)
        assertEquals("现场 hint", dungeon.viewpoints.single().hint)
        assertEquals("survey-verified", dungeon.viewpoints.single().source)
    }

    @Test
    fun candidateSurveyViewpointDoesNotOverrideSeedViewpoint() {
        val base = testContent()
        val seed = base.dungeons.single().viewpoints.single()
        val candidate = SurveyViewpointDraft(
            viewpointId = "official",
            name = "官方机位",
            capture = SurveyCapture(
                SurveyPoint(
                    dungeonId = "dungeon",
                    type = SurveyPointType.VIEWPOINT,
                    viewpointId = "official",
                    latitude = 28.2002,
                    longitude = 112.9002,
                    accuracyMeters = 5f,
                    capturedAt = 2L,
                    photoPath = "/tmp/candidate.jpg"
                )
            ),
            hint = "候选 hint",
            verified = false,
            status = "candidate"
        )
        val effective = base.withSurveyOverlay(
            SurveyBook(dungeons = mapOf("dungeon" to DungeonSurvey("dungeon", viewpoints = listOf(candidate))))
        ).dungeons.single().viewpoints.single()

        assertEquals(seed.latitude, effective.latitude, 0.000001)
        assertEquals(seed.longitude, effective.longitude, 0.000001)
        assertEquals(seed.referenceImages, effective.referenceImages)
        assertNotEquals("survey-verified", effective.source)
    }

    private fun testContent(): ContentBundle {
        val viewpoint = Viewpoint(
            id = "official",
            dungeonId = "dungeon",
            name = "官方机位",
            subtitle = "测试机位",
            hint = "种子 hint",
            referenceImages = listOf("seed-reference"),
            latitude = 28.1,
            longitude = 112.8,
            source = "official",
            status = "active",
            recommendedOrientation = "横构图",
            matchThreshold = 55,
            badgeId = "badge"
        )
        val dungeon = Dungeon(
            id = "dungeon",
            cityId = "changsha",
            name = "测试副本",
            subtitle = "测试",
            description = "测试",
            category = "测试",
            difficulty = "测试",
            coverImage = "seed-cover",
            mapX = 50f,
            mapY = 50f,
            checkpoint = Checkpoint("checkpoint", "测试点", 28.0, 112.7, 100, "测试", "测试"),
            viewpoints = listOf(viewpoint),
            badges = emptyList(),
            postcard = PostcardDefinition("测试", "测试", "长沙", "test"),
            creatorType = "official",
            contentStatus = "active"
        )
        return ContentBundle(
            city = CityConfig("changsha", "长沙", "测试", 28.0, 112.7, 12.0, "cover", listOf("dungeon")),
            dungeons = listOf(dungeon),
            cityBadges = emptyList()
        )
    }
}
