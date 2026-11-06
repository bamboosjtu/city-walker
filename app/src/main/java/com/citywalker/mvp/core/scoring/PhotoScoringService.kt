package com.citywalker.mvp.core.scoring

import com.citywalker.mvp.domain.PhotoRecord
import com.citywalker.mvp.domain.PhotoScore

interface PhotoScoringService {
    suspend fun score(dungeonId: String, viewpointId: String, photo: PhotoRecord): PhotoScore
}
