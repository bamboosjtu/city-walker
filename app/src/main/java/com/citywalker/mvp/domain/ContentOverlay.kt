package com.citywalker.mvp.domain

/**
 * Applies only field-survey data that is safe to use as game content. The
 * caller is responsible for enabling this in Debug builds only.
 */
fun ContentBundle.withSurveyOverlay(book: SurveyBook): ContentBundle = copy(
    dungeons = dungeons.map { dungeon ->
        val survey = book.dungeons[dungeon.id] ?: return@map dungeon
        val checkpoint = survey.effectiveCheckpoint?.point?.let { point ->
            dungeon.checkpoint.copy(latitude = point.latitude, longitude = point.longitude)
        } ?: dungeon.checkpoint
        val viewpoints = dungeon.viewpoints.map { viewpoint ->
            val draft = survey.viewpoints.firstOrNull { it.viewpointId == viewpoint.id }
            val capturePoint = draft?.capture?.point
            if (draft != null && draft.verified && draft.status == "active" && draft.isReadyForActivation() && capturePoint != null) {
                viewpoint.copy(
                    latitude = capturePoint.latitude,
                    longitude = capturePoint.longitude,
                    hint = draft.hint.ifBlank { viewpoint.hint },
                    referenceImages = capturePoint.photoPath?.let { listOf(it) } ?: viewpoint.referenceImages,
                    source = "survey-verified"
                )
            } else {
                viewpoint
            }
        }
        dungeon.copy(checkpoint = checkpoint, viewpoints = viewpoints)
    }
)
