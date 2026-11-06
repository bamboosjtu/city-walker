package com.citywalker.mvp.data

import android.content.Context
import com.citywalker.mvp.domain.ContentBundle
import com.citywalker.mvp.domain.DungeonSurvey
import com.citywalker.mvp.domain.SurveyBook
import com.citywalker.mvp.domain.SurveyCapture
import com.citywalker.mvp.domain.SurveyLocationSample
import com.citywalker.mvp.domain.SurveyPoint
import com.citywalker.mvp.domain.SurveyPointType
import com.citywalker.mvp.domain.SurveyViewpointDraft
import com.citywalker.mvp.domain.isReadyForActivation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SurveyRepository(context: Context) {
    private val surveyDirectory = File(context.filesDir, "surveys").apply { mkdirs() }
    private val stateFile = File(surveyDirectory, "survey_state.json")

    fun load(): SurveyBook {
        if (!stateFile.exists()) return SurveyBook()
        return runCatching {
            JSONObject(stateFile.readText()).toSurveyBook()
        }.getOrDefault(SurveyBook())
    }

    fun save(book: SurveyBook) {
        val root = JSONObject()
            .put("schemaVersion", "survey-v2")
            .put("cityId", book.cityId)
            .put("updatedAt", book.updatedAt ?: System.currentTimeMillis())
            .put("dungeons", JSONArray().also { array ->
                book.dungeons.values.sortedBy { it.dungeonId }.forEach { array.put(it.toJson()) }
            })
        stateFile.writeText(root.toString())
    }

    fun export(content: ContentBundle, book: SurveyBook): File {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        val output = File(surveyDirectory, "${content.city.id}_survey_$date.zip")
        val flatPoints = JSONArray()
        val dungeonArray = JSONArray()
        val photoEntries = linkedMapOf<String, File>()

        content.dungeons.forEach { dungeon ->
            val survey = book.dungeons[dungeon.id]
            val checkpointHistory = survey?.checkpointHistory.orEmpty()
            val checkpointArchivePaths = checkpointHistory.mapIndexedNotNull { index, capture ->
                val sourcePath = capture.point.photoPath ?: return@mapIndexedNotNull null
                val archivePath = registerPhoto(
                    point = capture.point,
                    archivePath = "photos/${dungeon.id}/checkpoint_${(index + 1).toString().padStart(2, '0')}.${photoExtension(File(sourcePath))}",
                    photoEntries = photoEntries
                ) ?: return@mapIndexedNotNull null
                capture.captureId to archivePath
            }.toMap()
            checkpointHistory.forEach { capture ->
                flatPoints.put(capture.point.copy(photoPath = checkpointArchivePaths[capture.captureId]).toJson())
            }

            val viewpointArray = JSONArray()
            survey?.viewpoints.orEmpty().forEachIndexed { index, draft ->
                val sourcePoint = draft.capture?.point
                val photoPath = sourcePoint?.photoPath?.let { path ->
                    registerPhoto(
                        point = sourcePoint,
                        archivePath = "photos/${dungeon.id}/viewpoint_${(index + 1).toString().padStart(2, '0')}.${photoExtension(File(path))}",
                        photoEntries = photoEntries
                    )
                }
                val exportStatus = if (draft.verified && draft.status == "active" && draft.isReadyForActivation() && photoPath != null) {
                    "active"
                } else {
                    "candidate"
                }
                val exportVerified = exportStatus == "active"
                val point = sourcePoint?.let { capturePoint ->
                    capturePoint.copy(note = draft.note ?: capturePoint.note, photoPath = photoPath)
                }
                point?.let { flatPoints.put(it.toJson()) }
                viewpointArray.put(
                    JSONObject()
                        .put("id", draft.viewpointId)
                        .put("name", draft.name)
                        .put(
                            "source",
                            if (dungeon.viewpoints.any { it.id == draft.viewpointId }) {
                                "official-verification"
                            } else {
                                "field-survey"
                            }
                        )
                        .put("type", SurveyPointType.VIEWPOINT.name)
                        .put("latitude", point?.latitude ?: JSONObject.NULL)
                        .put("longitude", point?.longitude ?: JSONObject.NULL)
                        .put("accuracyMeters", point?.accuracyMeters ?: JSONObject.NULL)
                        .put("capturedAt", point?.capturedAt ?: JSONObject.NULL)
                        .put("photoPath", photoPath ?: JSONObject.NULL)
                        .put("referenceImages", JSONArray().also { images -> photoPath?.let { images.put(it) } })
                        .put("requiredForCompletion", dungeon.viewpoints.firstOrNull { it.id == draft.viewpointId }?.requiredForCompletion ?: true)
                        .put("hint", draft.hint)
                        .put("verified", exportVerified)
                        .put("status", exportStatus)
                        .put("note", draft.note ?: JSONObject.NULL)
                        .put("samples", draft.capture?.samples.toJsonArray())
                )
            }

            val checkpoint = survey?.effectiveCheckpoint
            val checkpointJson = checkpoint?.let { capture ->
                capture.point.copy(photoPath = checkpointArchivePaths[capture.captureId]).toJson()
                    .put("name", dungeon.checkpoint.name)
                    .put("radiusMeters", dungeon.checkpoint.radiusMeters)
                    .put("captureId", capture.captureId)
                    .put("samples", capture.samples.toJsonArray())
            }
            val checkpointHistoryJson = JSONArray().also { history ->
                checkpointHistory.forEach { capture ->
                    history.put(
                        capture.toExportJson(
                            photoPath = checkpointArchivePaths[capture.captureId],
                            name = dungeon.checkpoint.name,
                            radiusMeters = dungeon.checkpoint.radiusMeters
                        )
                    )
                }
            }
            dungeonArray.put(
                JSONObject()
                    .put("dungeonId", dungeon.id)
                    .put("name", dungeon.name)
                    .put("checkpoint", checkpointJson ?: JSONObject.NULL)
                    .put("checkpointCaptures", checkpointHistoryJson)
                    .put("selectedCheckpointCaptureId", survey?.selectedCheckpointCaptureId ?: checkpoint?.captureId ?: JSONObject.NULL)
                    .put("viewpoints", viewpointArray)
                    .put("note", survey?.note ?: JSONObject.NULL)
            )
        }

        val root = JSONObject()
            .put("schemaVersion", "city-walker-survey-v2")
            .put("cityId", content.city.id)
            .put("cityName", content.city.name)
            .put("exportedAt", System.currentTimeMillis())
            .put("source", "City Walker Field Survey Mode")
            .put("photoCount", photoEntries.size)
            .put("dungeons", dungeonArray)
            .put("surveyPoints", flatPoints)

        ZipOutputStream(FileOutputStream(output).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("survey.json"))
            zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            photoEntries.toSortedMap().forEach { (archivePath, photoFile) ->
                zip.putNextEntry(ZipEntry(archivePath))
                photoFile.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output
    }

    private fun registerPhoto(
        point: SurveyPoint,
        archivePath: String,
        photoEntries: MutableMap<String, File>
    ): String? {
        val source = point.photoPath?.let(::File) ?: return null
        if (!source.isFile) return null
        if (!photoEntries.containsKey(archivePath)) photoEntries[archivePath] = source
        return archivePath
    }

    private fun photoExtension(file: File): String = file.extension
        .lowercase(Locale.US)
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
        ?: "jpg"

    private fun JSONObject.toSurveyBook(): SurveyBook {
        val dungeons = linkedMapOf<String, DungeonSurvey>()
        val array = optJSONArray("dungeons") ?: JSONArray()
        for (index in 0 until array.length()) {
            val survey = array.getJSONObject(index).toDungeonSurvey()
            dungeons[survey.dungeonId] = survey
        }
        return SurveyBook(
            cityId = optString("cityId", "changsha"),
            dungeons = dungeons,
            updatedAt = optLong("updatedAt").takeIf { it > 0L }
        )
    }

    private fun JSONObject.toDungeonSurvey(): DungeonSurvey {
        val viewpoints = mutableListOf<SurveyViewpointDraft>()
        val array = optJSONArray("viewpoints") ?: JSONArray()
        for (index in 0 until array.length()) {
            viewpoints += array.getJSONObject(index).toViewpointDraft()
        }
        val legacyCheckpoint = optJSONObject("checkpoint")?.toSurveyCapture()
        val historyArray = optJSONArray("checkpointCaptures")
        val checkpointHistory = if (historyArray == null) {
            listOfNotNull(legacyCheckpoint)
        } else {
            buildList(historyArray.length()) {
                for (index in 0 until historyArray.length()) {
                    add(historyArray.getJSONObject(index).toSurveyCapture())
                }
            }.ifEmpty { listOfNotNull(legacyCheckpoint) }
        }
        val effectiveId = nullableString("selectedCheckpointCaptureId")
        return DungeonSurvey(
            dungeonId = getString("dungeonId"),
            checkpoint = legacyCheckpoint ?: checkpointHistory.lastOrNull(),
            checkpointCaptures = checkpointHistory,
            selectedCheckpointCaptureId = effectiveId ?: legacyCheckpoint?.captureId ?: checkpointHistory.lastOrNull()?.captureId,
            viewpoints = viewpoints,
            note = nullableString("note")
        )
    }

    private fun JSONObject.toViewpointDraft(): SurveyViewpointDraft {
        val capture = optJSONObject("capture")?.toSurveyCapture()
        val hint = optString("hint")
        val verified = optBoolean("verified", false)
        val active = verified && optString("status", "candidate") == "active" &&
            capture != null && !capture.point.photoPath.isNullOrBlank() && hint.isNotBlank()
        return SurveyViewpointDraft(
            viewpointId = getString("viewpointId"),
            name = optString("name", "经典机位"),
            capture = capture,
            hint = hint,
            verified = active,
            status = if (active) "active" else "candidate",
            note = nullableString("note")
        )
    }

    private fun JSONObject.toSurveyCapture(): SurveyCapture {
        val point = getJSONObject("point").toSurveyPoint()
        return SurveyCapture(
            point = point,
            samples = optJSONArray("samples").toSamples(),
            captureId = optString("captureId").takeIf { it.isNotBlank() && it != "null" }
                ?: "${point.capturedAt}:${point.latitude}:${point.longitude}"
        )
    }

    private fun JSONObject.toSurveyPoint(): SurveyPoint {
        val type = runCatching { SurveyPointType.valueOf(optString("type", "CHECKPOINT")) }
            .getOrDefault(SurveyPointType.CHECKPOINT)
        return SurveyPoint(
            dungeonId = getString("dungeonId"),
            type = type,
            viewpointId = nullableString("viewpointId"),
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
            accuracyMeters = optDouble("accuracyMeters", 999.0).toFloat(),
            capturedAt = optLong("capturedAt"),
            photoPath = nullableString("photoPath"),
            note = nullableString("note")
        )
    }

    private fun JSONArray?.toSamples(): List<SurveyLocationSample> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                add(
                    SurveyLocationSample(
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        accuracyMeters = item.optDouble("accuracyMeters", 999.0).toFloat(),
                        capturedAt = item.optLong("capturedAt")
                    )
                )
            }
        }
    }

    private fun DungeonSurvey.toJson(): JSONObject {
        val effective = effectiveCheckpoint
        return JSONObject()
            .put("dungeonId", dungeonId)
            // Keep the legacy key as the effective value so older tooling can
            // still read files written after the history migration.
            .put("checkpoint", effective?.toJson() ?: JSONObject.NULL)
            .put("checkpointCaptures", JSONArray().also { array -> checkpointHistory.forEach { array.put(it.toJson()) } })
            .put("selectedCheckpointCaptureId", selectedCheckpointCaptureId ?: effective?.captureId ?: JSONObject.NULL)
            .put("viewpoints", JSONArray().also { array -> viewpoints.forEach { array.put(it.toJson()) } })
            .put("note", note ?: JSONObject.NULL)
    }

    private fun SurveyViewpointDraft.toJson(): JSONObject = JSONObject()
        .put("viewpointId", viewpointId)
        .put("name", name)
        .put("capture", capture?.toJson() ?: JSONObject.NULL)
        .put("hint", hint)
        .put("verified", verified)
        .put("status", status)
        .put("note", note ?: JSONObject.NULL)

    private fun SurveyCapture.toJson(): JSONObject = JSONObject()
        .put("captureId", captureId)
        .put("point", point.toJson())
        .put("samples", samples.toJsonArray())

    private fun SurveyCapture.toExportJson(photoPath: String?, name: String, radiusMeters: Int): JSONObject = JSONObject()
        .put("captureId", captureId)
        .put("name", name)
        .put("radiusMeters", radiusMeters)
        .put("point", point.copy(photoPath = photoPath).toJson())
        .put("samples", samples.toJsonArray())

    private fun SurveyPoint.toJson(): JSONObject = JSONObject()
        .put("dungeonId", dungeonId)
        .put("type", type.name)
        .put("viewpointId", viewpointId ?: JSONObject.NULL)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("accuracyMeters", accuracyMeters)
        .put("capturedAt", capturedAt)
        .put("photoPath", photoPath ?: JSONObject.NULL)
        .put("note", note ?: JSONObject.NULL)

    private fun List<SurveyLocationSample>?.toJsonArray(): JSONArray = JSONArray().also { array ->
        this.orEmpty().forEach { sample ->
            array.put(
                JSONObject()
                    .put("latitude", sample.latitude)
                    .put("longitude", sample.longitude)
                    .put("accuracyMeters", sample.accuracyMeters)
                    .put("capturedAt", sample.capturedAt)
            )
        }
    }

    private fun JSONObject.nullableString(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }
}
