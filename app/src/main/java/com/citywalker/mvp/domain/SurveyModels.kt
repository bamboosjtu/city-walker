package com.citywalker.mvp.domain

enum class SurveyPointType {
    CHECKPOINT,
    VIEWPOINT
}

/** The smallest unit exported for downstream content editing. */
data class SurveyPoint(
    val dungeonId: String,
    val type: SurveyPointType,
    val viewpointId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long,
    val photoPath: String? = null,
    val note: String? = null
)

data class SurveyLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long
)

data class SurveyCapture(
    val point: SurveyPoint,
    val samples: List<SurveyLocationSample> = emptyList(),
    val captureId: String = "${point.capturedAt}:${point.latitude}:${point.longitude}"
)

data class SurveyViewpointDraft(
    val viewpointId: String,
    val name: String,
    val capture: SurveyCapture? = null,
    val hint: String = "",
    val verified: Boolean = false,
    val status: String = "candidate",
    val note: String? = null
)

fun SurveyViewpointDraft.isReadyForActivation(): Boolean {
    val photoPath = capture?.point?.photoPath
    return capture != null && !photoPath.isNullOrBlank() && hint.isNotBlank()
}

data class DungeonSurvey(
    val dungeonId: String,
    val checkpoint: SurveyCapture? = null,
    val checkpointCaptures: List<SurveyCapture> = emptyList(),
    val selectedCheckpointCaptureId: String? = null,
    val viewpoints: List<SurveyViewpointDraft> = emptyList(),
    val note: String? = null
) {
    /** New files keep every sample run; old files are promoted into history on load. */
    val checkpointHistory: List<SurveyCapture>
        get() = checkpointCaptures.ifEmpty { listOfNotNull(checkpoint) }

    /** The explicitly selected point wins; otherwise the newest sample run is effective. */
    val effectiveCheckpoint: SurveyCapture?
        get() = checkpointHistory.firstOrNull { it.captureId == selectedCheckpointCaptureId }
            ?: checkpointHistory.lastOrNull()
}

data class SurveyBook(
    val cityId: String = "changsha",
    val dungeons: Map<String, DungeonSurvey> = emptyMap(),
    val updatedAt: Long? = null
)

data class SurveySamplingState(
    val active: Boolean = false,
    val type: SurveyPointType? = null,
    val viewpointId: String? = null,
    val sampleCount: Int = 0,
    val elapsedSeconds: Int = 0
)

object SurveyMath {
    fun stableLocation(samples: List<SurveyLocationSample>): GeoPoint? {
        if (samples.isEmpty()) return null
        return GeoPoint(
            latitude = median(samples.map { it.latitude }),
            longitude = median(samples.map { it.longitude }),
            accuracyMeters = median(samples.map { it.accuracyMeters.toDouble() }).toFloat(),
            simulated = false
        )
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}
