package com.citywalker.mvp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SurveyMathTest {
    @Test
    fun stableLocationUsesMedianForPositionAndAccuracy() {
        val stable = SurveyMath.stableLocation(
            listOf(
                SurveyLocationSample(28.1000, 112.9000, 12f, 1L),
                SurveyLocationSample(28.1004, 112.9004, 8f, 2L),
                SurveyLocationSample(28.1002, 112.9002, 6f, 3L),
                SurveyLocationSample(28.1001, 112.9001, 7f, 4L),
                SurveyLocationSample(28.0999, 112.8999, 6f, 5L)
            )
        )

        requireNotNull(stable)
        assertEquals(28.1001, stable.latitude, 0.0000001)
        assertEquals(112.9001, stable.longitude, 0.0000001)
        assertEquals(7f, stable.accuracyMeters ?: 0f, 0.001f)
    }
}
