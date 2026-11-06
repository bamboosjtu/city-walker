package com.citywalker.mvp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SurveyModelsTest {
    @Test
    fun newViewpointDraftStartsAsCandidate() {
        assertEquals("candidate", SurveyViewpointDraft("vp-01", "经典机位").status)
    }

    @Test
    fun activationNeedsLocationPhotoAndHint() {
        val point = SurveyPoint(
            dungeonId = "orange-island",
            type = SurveyPointType.VIEWPOINT,
            viewpointId = "vp-01",
            latitude = 28.181234,
            longitude = 112.960456,
            accuracyMeters = 7.8f,
            capturedAt = 1L
        )
        val draft = SurveyViewpointDraft("vp-01", "经典机位")
        assertFalse(draft.isReadyForActivation())
        assertFalse(draft.copy(capture = SurveyCapture(point)).isReadyForActivation())
        assertFalse(draft.copy(hint = "从江岸向北取景").isReadyForActivation())
        assertTrue(
            draft.copy(
                capture = SurveyCapture(point.copy(photoPath = "/photos/viewpoint.jpg")),
                hint = "从江岸向北取景"
            ).isReadyForActivation()
        )
    }

    @Test
    fun legacyCheckpointIsPromotedToHistoryAndRemainsEffective() {
        val first = SurveyCapture(
            SurveyPoint("dungeon", SurveyPointType.CHECKPOINT, latitude = 28.1, longitude = 112.9, accuracyMeters = 6f, capturedAt = 1L)
        )
        val survey = DungeonSurvey(dungeonId = "dungeon", checkpoint = first)

        assertEquals(listOf(first), survey.checkpointHistory)
        assertEquals(first, survey.effectiveCheckpoint)
    }

    @Test
    fun selectedCheckpointCanPointToAnOlderCaptureWithoutDeletingNewest() {
        val first = SurveyCapture(
            SurveyPoint("dungeon", SurveyPointType.CHECKPOINT, latitude = 28.1, longitude = 112.9, accuracyMeters = 6f, capturedAt = 1L)
        )
        val second = SurveyCapture(
            SurveyPoint("dungeon", SurveyPointType.CHECKPOINT, latitude = 28.2, longitude = 113.0, accuracyMeters = 5f, capturedAt = 2L)
        )
        val survey = DungeonSurvey(
            dungeonId = "dungeon",
            checkpoint = second,
            checkpointCaptures = listOf(first, second),
            selectedCheckpointCaptureId = first.captureId
        )

        assertEquals(2, survey.checkpointHistory.size)
        assertEquals(first, survey.effectiveCheckpoint)
    }
}
