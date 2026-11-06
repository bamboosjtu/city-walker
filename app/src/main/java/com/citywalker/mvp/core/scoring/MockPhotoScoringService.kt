package com.citywalker.mvp.core.scoring

import com.citywalker.mvp.domain.PhotoRecord
import com.citywalker.mvp.domain.PhotoScore
import kotlinx.coroutines.delay
import kotlin.math.abs

class MockPhotoScoringService : PhotoScoringService {
    override suspend fun score(dungeonId: String, viewpointId: String, photo: PhotoRecord): PhotoScore {
        delay(850L)
        val seed = abs("$dungeonId:$viewpointId:${photo.capturedAt}".hashCode())
        val similarity = 72 + seed % 21
        val sharpness = 78 + seed.times(3) % 20
        val exposure = 74 + seed.times(5) % 22
        val totalScore = (similarity * .6 + sharpness * .2 + exposure * .2).toInt()
        return PhotoScore(
            totalScore = totalScore,
            similarity = similarity,
            sharpness = sharpness,
            exposure = exposure,
            // The ViewModel applies the content-specific matchThreshold;
            // this fallback keeps the mock score semantically composition-first.
            matched = similarity >= 55,
            provider = "mock-local",
            scoredAt = System.currentTimeMillis()
        )
    }
}
