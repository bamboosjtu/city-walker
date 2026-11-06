package com.citywalker.mvp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationMathTest {
    @Test
    fun nearestDungeonUsesCheckpointDistance() {
        val far = testDungeon("far", latitude = 28.20, longitude = 112.98)
        val near = testDungeon("near", latitude = 28.1812, longitude = 112.9602)

        assertEquals("near", nearestDungeon(GeoPoint(28.1812, 112.9600), listOf(far, near))?.id)
    }

    @Test
    fun nearestDungeonReturnsNullForEmptyContent() {
        assertNull(nearestDungeon(GeoPoint(28.1812, 112.9600), emptyList()))
    }

    private fun testDungeon(id: String, latitude: Double, longitude: Double): Dungeon = Dungeon(
        id = id,
        cityId = "changsha",
        name = id,
        subtitle = "测试",
        description = "测试",
        category = "测试",
        difficulty = "测试",
        coverImage = "",
        mapX = 50f,
        mapY = 50f,
        checkpoint = Checkpoint("$id-checkpoint", id, latitude, longitude, 100, "测试", "测试"),
        viewpoints = emptyList(),
        badges = emptyList(),
        postcard = PostcardDefinition("测试", "测试", "长沙", "test"),
        creatorType = "test",
        contentStatus = "active"
    )
}
