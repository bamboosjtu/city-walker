package com.citywalker.mvp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.citywalker.mvp.core.camera.PhotoRepository
import com.citywalker.mvp.core.location.LocationService
import com.citywalker.mvp.core.location.distanceBetweenMeters
import com.citywalker.mvp.core.location.formatDistance
import com.citywalker.mvp.core.postcard.PostcardGenerator
import com.citywalker.mvp.core.scoring.MockPhotoScoringService
import com.citywalker.mvp.core.scoring.PhotoScoringService
import com.citywalker.mvp.data.ContentRepository
import com.citywalker.mvp.data.ProgressRepository
import com.citywalker.mvp.data.SurveyRepository
import com.citywalker.mvp.domain.CaptureMode
import com.citywalker.mvp.domain.CaptureResult
import com.citywalker.mvp.domain.ContentBundle
import com.citywalker.mvp.domain.DungeonSurvey
import com.citywalker.mvp.domain.Dungeon
import com.citywalker.mvp.domain.DungeonProgress
import com.citywalker.mvp.domain.GeoPoint
import com.citywalker.mvp.domain.PhotoRecord
import com.citywalker.mvp.domain.PlayerProgress
import com.citywalker.mvp.domain.ProgressEngine
import com.citywalker.mvp.domain.SurveyBook
import com.citywalker.mvp.domain.SurveyCapture
import com.citywalker.mvp.domain.SurveyLocationSample
import com.citywalker.mvp.domain.SurveyMath
import com.citywalker.mvp.domain.SurveyPoint
import com.citywalker.mvp.domain.SurveyPointType
import com.citywalker.mvp.domain.SurveySamplingState
import com.citywalker.mvp.domain.SurveyViewpointDraft
import com.citywalker.mvp.domain.isReadyForActivation
import com.citywalker.mvp.domain.nearestDungeon
import com.citywalker.mvp.domain.withSurveyOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AppScreen {
    MAP,
    DETAIL,
    VIEWPOINT,
    CAMERA,
    RESULT,
    COLLECTION,
    POSTCARD,
    FIELD_SURVEY
}

enum class LocationStatus {
    IDLE,
    LOCATING,
    READY,
    SIMULATED,
    ERROR
}

enum class MessageActionType {
    NAVIGATE_TO_SELECTED_DUNGEON
}

data class UiMessageAction(
    val label: String,
    val type: MessageActionType
)

data class CityWalkerUiState(
    val content: ContentBundle? = null,
    val progress: PlayerProgress = PlayerProgress(),
    val screen: AppScreen = AppScreen.MAP,
    val selectedDungeonId: String? = null,
    val selectedViewpointId: String? = null,
    val mapFocusedDungeonId: String? = null,
    val currentLocation: GeoPoint? = null,
    val locationStatus: LocationStatus = LocationStatus.IDLE,
    val locationError: String? = null,
    val captureMode: CaptureMode? = null,
    val captureResult: CaptureResult? = null,
    val postcardPath: String? = null,
    val isGeneratingPostcard: Boolean = false,
    val surveyBook: SurveyBook = SurveyBook(),
    val surveyDungeonId: String? = null,
    val surveyViewpointId: String? = null,
    val surveySampling: SurveySamplingState = SurveySamplingState(),
    val lastSurveyExportPath: String? = null,
    val surveyReturnScreen: AppScreen = AppScreen.FIELD_SURVEY,
    val message: String? = null,
    val messageAction: UiMessageAction? = null
)

class CityWalkerViewModel(application: Application) : AndroidViewModel(application) {
    private val contentRepository = ContentRepository(application)
    private val progressRepository = ProgressRepository(application)
    private val surveyRepository = SurveyRepository(application)
    private val locationService = LocationService(application)
    private val photoRepository = PhotoRepository(application)
    private val postcardGenerator = PostcardGenerator(application)
    private val scoringService: PhotoScoringService = MockPhotoScoringService()

    private val _uiState = MutableStateFlow(CityWalkerUiState())
    val uiState: StateFlow<CityWalkerUiState> = _uiState.asStateFlow()
    private var surveySamplingJob: Job? = null
    private val surveyWriteMutex = Mutex()
    private var baseContent: ContentBundle? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contentRepository.load() }
                .onSuccess { sourceContent ->
                    baseContent = sourceContent
                    val surveyBook = surveyRepository.load()
                    val content = if (BuildConfig.DEBUG) sourceContent.withSurveyOverlay(surveyBook) else sourceContent
                    val restored = normalizeProgress(content, progressRepository.load())
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update {
                            it.copy(
                                content = content,
                                progress = restored,
                                surveyBook = surveyBook,
                                surveyDungeonId = defaultSurveyDungeonId(content),
                                mapFocusedDungeonId = defaultMapDungeonId(content)
                            )
                        }
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { it.copy(message = error.message ?: "内容加载失败") }
                    }
                }
        }
    }

    fun openDungeon(dungeonId: String) {
        if (findDungeon(dungeonId) == null) return
        _uiState.update {
            it.copy(
                screen = AppScreen.DETAIL,
                selectedDungeonId = dungeonId,
                selectedViewpointId = null,
                mapFocusedDungeonId = dungeonId,
                captureResult = null,
                postcardPath = null,
                message = null,
                messageAction = null
            )
        }
    }

    fun focusDungeonOnMap(dungeonId: String) {
        if (findDungeon(dungeonId) == null) return
        _uiState.update {
            it.copy(mapFocusedDungeonId = dungeonId, message = null, messageAction = null)
        }
    }

    fun openViewpoint(dungeonId: String, viewpointId: String) {
        val dungeon = findDungeon(dungeonId) ?: return
        if (dungeon.viewpoints.none { it.id == viewpointId }) return
        _uiState.update {
            it.copy(
                screen = AppScreen.VIEWPOINT,
                selectedDungeonId = dungeonId,
                selectedViewpointId = viewpointId,
                message = null,
                messageAction = null
            )
        }
    }

    fun openCollection() {
        _uiState.update { it.copy(screen = AppScreen.COLLECTION, message = null, messageAction = null) }
    }

    fun openFieldSurvey() {
        if (!BuildConfig.DEBUG) return
        val content = _uiState.value.content ?: return
        val dungeonId = _uiState.value.surveyDungeonId
            ?.takeIf { id -> content.dungeons.any { it.id == id } }
            ?: defaultSurveyDungeonId(content)
            ?: return
        val viewpointId = _uiState.value.surveyBook.dungeons[dungeonId]?.viewpoints?.firstOrNull()?.viewpointId
        _uiState.update {
            it.copy(
                screen = AppScreen.FIELD_SURVEY,
                selectedDungeonId = dungeonId,
                selectedViewpointId = null,
                surveyDungeonId = dungeonId,
                surveyViewpointId = viewpointId,
                surveySampling = SurveySamplingState(),
                message = null,
                messageAction = null
            )
        }
    }

    fun selectSurveyDungeon(dungeonId: String) {
        if (findDungeon(dungeonId) == null) return
        val viewpointId = _uiState.value.surveyBook.dungeons[dungeonId]?.viewpoints?.firstOrNull()?.viewpointId
        _uiState.update {
            it.copy(selectedDungeonId = dungeonId, selectedViewpointId = null, surveyDungeonId = dungeonId, surveyViewpointId = viewpointId, message = null, messageAction = null)
        }
    }

    fun selectSurveyViewpoint(viewpointId: String) {
        if (selectedSurveyDungeon()?.let { dungeon -> surveyFor(dungeon.id)?.viewpoints?.any { it.viewpointId == viewpointId } } != true) return
        _uiState.update { it.copy(surveyViewpointId = viewpointId, message = null, messageAction = null) }
    }

    /**
     * Starts a field-survey record for an official viewpoint without changing
     * its content ID. This lets field verification update the same point that
     * the player-facing content already references.
     */
    fun selectSurveyOfficialViewpoint(viewpointId: String) {
        val dungeon = selectedSurveyDungeon() ?: return
        val official = dungeon.viewpoints.firstOrNull { it.id == viewpointId } ?: return
        val survey = surveyFor(dungeon.id) ?: DungeonSurvey(dungeon.id)
        val existing = survey.viewpoints.firstOrNull { it.viewpointId == official.id }
        if (existing == null) {
            persistSurvey(
                updateSurveyDungeon(
                    dungeon.id,
                    survey.copy(
                        viewpoints = survey.viewpoints + SurveyViewpointDraft(
                            viewpointId = official.id,
                            name = official.name,
                            status = "candidate"
                        )
                    )
                )
            )
        }
        _uiState.update {
            it.copy(
                surveyViewpointId = official.id,
                message = if (existing == null) {
                    "已建立官方机位候选记录：${official.name} · ID 保持 ${official.id}"
                } else {
                    "已选择官方机位：${official.name} · 继续补采或复核"
                },
                messageAction = null
            )
        }
    }

    fun createSurveyViewpoint(): String? {
        val dungeon = selectedSurveyDungeon() ?: return null
        val survey = surveyFor(dungeon.id) ?: DungeonSurvey(dungeon.id)
        val sequence = survey.viewpoints.size + 1
        val viewpointId = "${dungeon.id}-survey-${sequence.toString().padStart(2, '0')}"
        val draft = SurveyViewpointDraft(
            viewpointId = viewpointId,
            name = "经典机位 ${sequence.toString().padStart(2, '0')}",
            status = "candidate"
        )
        val next = survey.copy(viewpoints = survey.viewpoints + draft)
        persistSurvey(updateSurveyDungeon(dungeon.id, next))
        _uiState.update { it.copy(surveyViewpointId = viewpointId, message = "已新增候选机位，请记录位置、拍参考图并补充提示", messageAction = null) }
        return viewpointId
    }

    fun recordSurveyCheckpoint() {
        startSurveySampling(SurveyPointType.CHECKPOINT, null)
    }

    fun recordSurveyViewpoint() {
        val viewpointId = _uiState.value.surveyViewpointId ?: createSurveyViewpoint()
        if (viewpointId == null) {
            showMessage("请先选择一个副本")
            return
        }
        startSurveySampling(SurveyPointType.VIEWPOINT, viewpointId)
    }

    fun startSurveyReferenceCapture() {
        val viewpointId = _uiState.value.surveyViewpointId
        if (viewpointId == null || selectedSurveyViewpoint() == null) {
            showMessage("请先新增或选择一个机位")
            return
        }
        val draft = selectedSurveyViewpoint()
        if (draft?.capture == null && _uiState.value.currentLocation == null) {
            showMessage("请先刷新 GPS 或记录当前位置为 viewpoint，再拍参考图")
            return
        }
        _uiState.update {
            it.copy(
                screen = AppScreen.CAMERA,
                captureMode = CaptureMode.SURVEY_VIEWPOINT,
                surveyReturnScreen = AppScreen.FIELD_SURVEY,
                message = null,
                messageAction = null
            )
        }
    }

    fun updateSurveyViewpointName(name: String) {
        updateCurrentSurveyViewpoint { it.copy(name = name) }
    }

    fun updateSurveyHint(hint: String) {
        updateCurrentSurveyViewpoint { it.copy(hint = hint, verified = false, status = "candidate") }
    }

    fun updateSurveyVerified(verified: Boolean) {
        if (verified) confirmSurveyViewpoint() else updateCurrentSurveyViewpoint { it.copy(verified = false, status = "candidate") }
    }

    fun updateSurveyStatus(status: String) {
        if (status == "active") confirmSurveyViewpoint()
        else updateCurrentSurveyViewpoint { it.copy(verified = false, status = "candidate") }
    }

    fun confirmSurveyViewpoint() {
        val draft = selectedSurveyViewpoint()
        if (draft == null) {
            showMessage("请先新增或选择一个机位")
            return
        }
        val capture = draft.capture
        val missing = when {
            capture == null -> "请先记录 viewpoint 坐标"
            capture.point.photoPath.isNullOrBlank() -> "请先拍参考图"
            draft.hint.isBlank() -> "请先填写机位 hint"
            else -> null
        }
        if (missing != null || !draft.isReadyForActivation()) {
            showMessage(missing ?: "请先完成机位资料")
            return
        }
        updateCurrentSurveyViewpoint { it.copy(verified = true, status = "active") }
        showMessage("机位已确认 · verified = true · status = active")
    }

    fun updateSurveyNote(note: String) {
        val state = _uiState.value
        val dungeon = selectedSurveyDungeon() ?: return
        val survey = surveyFor(dungeon.id) ?: DungeonSurvey(dungeon.id)
        val viewpointId = state.surveyViewpointId
        val next = if (viewpointId == null) {
            survey.copy(note = note.takeIf { it.isNotBlank() })
        } else {
            val viewpoints = survey.viewpoints.map { draft ->
                if (draft.viewpointId == viewpointId) draft.copy(note = note.takeIf { it.isNotBlank() }) else draft
            }
            survey.copy(viewpoints = viewpoints)
        }
        persistSurvey(updateSurveyDungeon(dungeon.id, next))
    }

    fun exportSurvey() {
        val state = _uiState.value
        val content = state.content ?: return
        _uiState.update { it.copy(message = "正在生成踩点 ZIP…", messageAction = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { surveyRepository.export(content, state.surveyBook) }
                .onSuccess { file ->
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update {
                            it.copy(lastSurveyExportPath = file.absolutePath, message = "已导出 ${file.name}", messageAction = null)
                        }
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { it.copy(message = error.message ?: "踩点数据导出失败", messageAction = null) }
                    }
                }
        }
    }

    fun navigateBack() {
        val state = _uiState.value
        when (state.screen) {
            AppScreen.MAP -> Unit
            AppScreen.DETAIL, AppScreen.COLLECTION, AppScreen.FIELD_SURVEY -> _uiState.update { it.copy(screen = AppScreen.MAP, message = null, messageAction = null) }
            AppScreen.VIEWPOINT -> _uiState.update { it.copy(screen = AppScreen.DETAIL, message = null, messageAction = null) }
            AppScreen.CAMERA -> _uiState.update { it.copy(screen = if (state.captureMode == CaptureMode.SURVEY_VIEWPOINT) state.surveyReturnScreen else previousContentScreen(state), captureMode = null, message = null, messageAction = null) }
            AppScreen.RESULT -> _uiState.update { it.copy(screen = AppScreen.DETAIL, captureResult = null, message = null, messageAction = null) }
            AppScreen.POSTCARD -> _uiState.update { it.copy(screen = AppScreen.RESULT, message = null, messageAction = null) }
        }
    }

    fun startMainCapture() {
        val state = _uiState.value
        val dungeon = selectedDungeon() ?: return
        if (!isCurrentlyAtCheckpoint(dungeon, state.currentLocation)) {
            val distance = state.currentLocation?.let { location ->
                distanceBetweenMeters(location, dungeon.checkpoint.point)
            }
            val distanceText = distance?.let(::formatDistance)
            val accuracyLimit = checkpointAccuracyLimit(dungeon)
            val accuracy = state.currentLocation?.accuracyMeters
            showMessage(
                if (distance != null && distance <= dungeon.checkpoint.radiusMeters) {
                    "已在打卡范围内，但 GPS 精度${accuracy?.let { " ±${it.toInt()}m" } ?: "不可用"}，需要 ≤${accuracyLimit.toInt()}m 才能拍摄"
                } else if (distanceText != null) {
                    "还差 $distanceText，抵达后即可拍摄"
                } else {
                    "先开启定位并抵达现场，抵达后即可拍摄"
                },
                UiMessageAction("去这里", MessageActionType.NAVIGATE_TO_SELECTED_DUNGEON)
            )
            return
        }
        _uiState.update {
            it.copy(screen = AppScreen.CAMERA, captureMode = CaptureMode.MAIN, captureResult = null, message = null, messageAction = null)
        }
    }

    fun startViewpointCapture() {
        val dungeon = selectedDungeon() ?: return
        if (!ProgressEngine.isMainCompleted(ProgressEngine.progressFor(_uiState.value.progress, dungeon.id))) {
            showMessage("完成副本主线后才能挑战经典机位")
            return
        }
        _uiState.update {
            it.copy(screen = AppScreen.CAMERA, captureMode = CaptureMode.VIEWPOINT, captureResult = null, message = null, messageAction = null)
        }
    }

    fun startFreeCapture() {
        val state = _uiState.value
        val dungeon = selectedDungeon() ?: return
        val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
        if (!ProgressEngine.isMainCompleted(progress)) {
            showMessage("完成副本主线后才能记录自由发现")
            return
        }
        if (state.currentLocation == null) {
            showMessage("自由探索需要先开启定位，以保存这张照片的经纬度")
            return
        }
        _uiState.update {
            it.copy(screen = AppScreen.CAMERA, captureMode = CaptureMode.FREE, captureResult = null, message = null, messageAction = null)
        }
    }

    fun onPhotoCaptured(filePath: String) {
        val state = _uiState.value
        val mode = state.captureMode ?: return
        if (mode == CaptureMode.SURVEY_VIEWPOINT) {
            attachSurveyPhoto(filePath)
            _uiState.update {
                it.copy(screen = AppScreen.FIELD_SURVEY, captureMode = null, message = "参考照片已绑定到当前机位")
            }
            return
        }
        val dungeon = selectedDungeon() ?: return
        val viewpoint = state.selectedViewpointId?.let { id -> dungeon.viewpoints.firstOrNull { it.id == id } }
        val dungeonProgress = ProgressEngine.progressFor(state.progress, dungeon.id)
        if (mode == CaptureMode.FREE && state.currentLocation == null) {
            showMessage("自由探索需要有效 GPS，照片未记录")
            return
        }
        val location = state.currentLocation
            ?: dungeonProgress.arrivedLocation
            ?: dungeon.checkpoint.point.copy(simulated = true)
        val photo = PhotoRecord(
            filePath = filePath,
            capturedAt = System.currentTimeMillis(),
            dungeonId = dungeon.id,
            viewpointId = viewpoint?.id,
            mode = mode,
            location = location
        )

        when (mode) {
            CaptureMode.MAIN -> {
                if (!isCurrentlyAtCheckpoint(dungeon, state.currentLocation)) {
                    showMessage("拍摄时已不在 ${dungeon.checkpoint.name} 的有效打卡范围内，请返回现场后重拍")
                    return
                }
                val next = runCatching { ProgressEngine.completeMain(dungeon, dungeonProgress, photo) }
                    .getOrElse {
                        showMessage(it.message ?: "主线还不能完成")
                        return
                    }
                val nextPlayer = state.progress.copy(dungeons = state.progress.dungeons + (dungeon.id to next), updatedAt = System.currentTimeMillis())
                persist(nextPlayer)
                _uiState.update {
                    it.copy(
                        screen = AppScreen.RESULT,
                        captureMode = null,
                        captureResult = CaptureResult(mode, dungeon.id, photo = photo),
                        message = "主线完成，城市图鉴新增一枚印章"
                    )
                }
            }

            CaptureMode.VIEWPOINT -> {
                if (viewpoint == null) {
                    showMessage("没有找到这个经典机位")
                    return
                }
                val result = CaptureResult(mode, dungeon.id, viewpoint.id, photo, isScoring = true)
                _uiState.update { it.copy(screen = AppScreen.RESULT, captureMode = null, captureResult = result, message = "正在用本地 Demo 评分…") }
                viewModelScope.launch {
                    runCatching { scoringService.score(dungeon.id, viewpoint.id, photo) }
                        .onSuccess { rawScore ->
                            val score = rawScore.copy(
                                matched = rawScore.similarity >= viewpoint.matchThreshold
                            )
                            val latestState = _uiState.value
                            val latestProgress = ProgressEngine.progressFor(latestState.progress, dungeon.id)
                            val next = ProgressEngine.recordViewpointScore(dungeon, latestProgress, viewpoint, photo, score)
                            val nextPlayer = latestState.progress.copy(
                                dungeons = latestState.progress.dungeons + (dungeon.id to next),
                                updatedAt = System.currentTimeMillis()
                            )
                            persist(nextPlayer)
                            _uiState.update {
                                it.copy(
                                    captureResult = result.copy(score = score, isScoring = false),
                                    message = if (score.matched) "机位匹配成功，探索进度已更新" else "这次构图还可以再靠近参考图"
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    captureResult = result.copy(isScoring = false),
                                    message = error.message ?: "评分失败，可以稍后重试"
                                )
                            }
                        }
                }
            }

            CaptureMode.SURVEY_VIEWPOINT -> Unit

            CaptureMode.FREE -> {
                val next = dungeonProgress.copy(freeCaptures = dungeonProgress.freeCaptures + photo)
                val nextPlayer = state.progress.copy(
                    dungeons = state.progress.dungeons + (dungeon.id to next),
                    updatedAt = System.currentTimeMillis()
                )
                persist(nextPlayer)
                _uiState.update {
                    it.copy(
                        screen = AppScreen.RESULT,
                        captureMode = null,
                        captureResult = CaptureResult(mode, dungeon.id, photo = photo),
                        message = "已记录到“我的发现” · 不影响官方探索进度"
                    )
                }
            }
        }
    }

    fun updateFreeCaptureNote(photoPath: String, note: String) {
        val state = _uiState.value
        val result = state.captureResult?.takeIf { it.mode == CaptureMode.FREE && it.photo.filePath == photoPath } ?: return
        val dungeon = state.content?.dungeons?.firstOrNull { it.id == result.dungeonId } ?: return
        val progress = ProgressEngine.progressFor(state.progress, dungeon.id)
        if (progress.freeCaptures.none { it.filePath == photoPath }) return
        val updatedPhoto = result.photo.copy(note = note.takeIf { it.isNotBlank() })
        val updatedProgress = progress.copy(
            freeCaptures = progress.freeCaptures.map { photo ->
                if (photo.filePath == photoPath) updatedPhoto else photo
            }
        )
        persist(
            state.progress.copy(
                dungeons = state.progress.dungeons + (dungeon.id to updatedProgress),
                updatedAt = System.currentTimeMillis()
            )
        )
        _uiState.update { it.copy(captureResult = result.copy(photo = updatedPhoto)) }
    }

    fun generatePostcard() {
        val state = _uiState.value
        val content = state.content ?: return
        val result = state.captureResult ?: return
        val dungeon = content.dungeons.firstOrNull { it.id == result.dungeonId } ?: return
        if (result.isScoring) return
        _uiState.update { it.copy(screen = AppScreen.POSTCARD, isGeneratingPostcard = true, postcardPath = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                postcardGenerator.generate(
                    city = content.city,
                    dungeon = dungeon,
                    photo = result.photo,
                    score = result.score,
                    stats = ProgressEngine.cityStats(content, _uiState.value.progress)
                )
            }.onSuccess { file ->
                withContext(Dispatchers.Main.immediate) {
                    _uiState.update { it.copy(isGeneratingPostcard = false, postcardPath = file.absolutePath) }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main.immediate) {
                    _uiState.update { it.copy(isGeneratingPostcard = false, screen = AppScreen.RESULT, message = error.message ?: "明信片生成失败") }
                }
            }
        }
    }

    fun locate() {
        if (!locationService.hasPermission()) {
            _uiState.update { it.copy(locationStatus = LocationStatus.ERROR, locationError = "定位权限未开启", message = "请允许定位权限后再试") }
            return
        }
        _uiState.update { it.copy(locationStatus = LocationStatus.LOCATING, locationError = null, message = "正在寻找你的位置…") }
        locationService.current({ point -> onLocation(point, autoFocusNearest = true) }, ::onLocationError)
        locationService.startWatching({ point -> onLocation(point) }, ::onLocationError)
    }

    fun simulateArrival() {
        if (!BuildConfig.DEBUG) return
        val dungeon = selectedDungeon() ?: return
        onLocation(dungeon.checkpoint.point.copy(simulated = true))
        _uiState.update {
            it.copy(
                locationStatus = LocationStatus.SIMULATED,
                message = "已模拟到达 ${dungeon.checkpoint.name}，可以开始主线",
                messageAction = null
            )
        }
    }

    fun clearAllPlayerProgress() {
        if (!BuildConfig.DEBUG) return
        progressRepository.clear()
        _uiState.update {
            it.copy(
                progress = PlayerProgress(),
                currentLocation = null,
                locationStatus = LocationStatus.IDLE,
                locationError = null,
                message = "已清空全部玩家进度 · 踩点数据保留",
                messageAction = null
            )
        }
    }

    fun clearCurrentDungeonProgress(dungeonId: String) {
        if (!BuildConfig.DEBUG) return
        val state = _uiState.value
        val content = state.content ?: return
        val dungeon = content.dungeons.firstOrNull { it.id == dungeonId } ?: return
        val cityBadgeIds = content.cityBadges.map { it.id }.toSet()
        persist(
            state.progress.copy(
                dungeons = state.progress.dungeons - dungeon.id,
                cityBadges = state.progress.cityBadges - cityBadgeIds,
                updatedAt = System.currentTimeMillis()
            )
        )
        _uiState.update {
            it.copy(
                currentLocation = null,
                locationStatus = LocationStatus.IDLE,
                locationError = null,
                message = "已清空 ${dungeon.name} 玩家进度 · 踩点数据保留",
                messageAction = null
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageAction = null) }
    }

    fun progressFor(dungeonId: String): DungeonProgress =
        ProgressEngine.progressFor(_uiState.value.progress, dungeonId)

    fun isCurrentlyAtCheckpoint(dungeonId: String): Boolean =
        findDungeon(dungeonId)?.let { dungeon ->
            isCurrentlyAtCheckpoint(dungeon, _uiState.value.currentLocation)
        } == true

    fun selectedDungeon(): Dungeon? = selectedDungeonId()?.let { id -> findDungeon(id) }

    fun selectedViewpoint(): com.citywalker.mvp.domain.Viewpoint? {
        val dungeon = selectedDungeon() ?: return null
        return _uiState.value.selectedViewpointId?.let { id -> dungeon.viewpoints.firstOrNull { it.id == id } }
    }

    fun selectedDistanceMeters(): Float? {
        val location = _uiState.value.currentLocation ?: return null
        return selectedDungeon()?.let { distanceBetweenMeters(location, it.checkpoint.point) }
    }

    fun createDemoPhoto(prefix: String, title: String, subtitle: String): String {
        val file = photoRepository.newPhotoFile(prefix)
        return photoRepository.saveDemoPhoto(file, title, subtitle)
    }

    fun selectedSurveyDungeon(): Dungeon? = _uiState.value.surveyDungeonId?.let { findDungeon(it) }

    fun selectedSurvey(): DungeonSurvey? = selectedSurveyDungeon()?.let { surveyFor(it.id) }

    fun selectedSurveyViewpoint(): SurveyViewpointDraft? {
        val viewpointId = _uiState.value.surveyViewpointId ?: return null
        return selectedSurvey()?.viewpoints?.firstOrNull { it.viewpointId == viewpointId }
    }

    private fun startSurveySampling(type: SurveyPointType, viewpointId: String?) {
        if (surveySamplingJob?.isActive == true) {
            showMessage("正在采样，请稍候…")
            return
        }
        if (!locationService.hasPermission()) {
            showMessage("请先开启定位权限，踩点需要真实 GPS")
            return
        }
        val dungeon = selectedSurveyDungeon() ?: return
        if (type == SurveyPointType.VIEWPOINT && surveyFor(dungeon.id)?.viewpoints?.none { it.viewpointId == viewpointId } == true) {
            showMessage("请先新增这个机位草稿")
            return
        }

        val startedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                surveySampling = SurveySamplingState(active = true, type = type, viewpointId = viewpointId),
                message = "正在连续采样约 8 秒，请保持手机稳定…"
            )
        }
        surveySamplingJob = viewModelScope.launch {
            val samples = mutableListOf<SurveyLocationSample>()
            val targetEnd = startedAt + 8_000L
            val hardEnd = startedAt + 10_000L
            while (System.currentTimeMillis() < hardEnd && (System.currentTimeMillis() < targetEnd || samples.size < 5)) {
                val location = try {
                    locationService.currentOnce()
                } catch (error: CancellationException) {
                    return@launch
                } catch (error: Throwable) {
                    _uiState.update {
                        it.copy(surveySampling = SurveySamplingState(), message = error.message ?: "GPS 采样失败")
                    }
                    return@launch
                }
                val sample = SurveyLocationSample(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracyMeters?.coerceAtLeast(0f) ?: 999f,
                    capturedAt = System.currentTimeMillis()
                )
                samples += sample
                val elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1_000L).toInt()
                _uiState.update {
                    it.copy(
                        currentLocation = location,
                        locationStatus = LocationStatus.READY,
                        surveySampling = SurveySamplingState(
                            active = true,
                            type = type,
                            viewpointId = viewpointId,
                            sampleCount = samples.size,
                            elapsedSeconds = elapsedSeconds
                        ),
                        message = "已采集 ${samples.size} 个 GPS 样本…"
                    )
                }
                if (System.currentTimeMillis() < hardEnd) delay(1_000L)
            }

            val stable = SurveyMath.stableLocation(samples)
            if (stable == null) {
                _uiState.update { it.copy(surveySampling = SurveySamplingState(), message = "没有采到有效 GPS，请重试") }
                return@launch
            }
            val accuracy = stable.accuracyMeters ?: 999f
            val point = SurveyPoint(
                dungeonId = dungeon.id,
                type = type,
                viewpointId = viewpointId,
                latitude = stable.latitude,
                longitude = stable.longitude,
                accuracyMeters = accuracy,
                capturedAt = samples.lastOrNull()?.capturedAt ?: System.currentTimeMillis()
            )
            val capture = SurveyCapture(point = point, samples = samples)
            val latestSurvey = surveyFor(dungeon.id) ?: DungeonSurvey(dungeon.id)
            val nextSurvey = if (type == SurveyPointType.CHECKPOINT) {
                latestSurvey.copy(
                    checkpoint = capture,
                    checkpointCaptures = latestSurvey.checkpointHistory + capture,
                    selectedCheckpointCaptureId = capture.captureId
                )
            } else {
                latestSurvey.copy(
                    viewpoints = latestSurvey.viewpoints.map { draft ->
                        if (draft.viewpointId == viewpointId) {
                            draft.copy(capture = capture, verified = false, status = "candidate")
                        } else {
                            draft
                        }
                    }
                )
            }
            persistSurvey(updateSurveyDungeon(dungeon.id, nextSurvey))
            _uiState.update {
                it.copy(
                    currentLocation = stable,
                    locationStatus = LocationStatus.READY,
                    surveySampling = SurveySamplingState(),
                    message = "已保存稳定点：${"%.6f".format(stable.latitude)}, ${"%.6f".format(stable.longitude)} · ${samples.size} 个样本 · 精度约 ±${accuracy.toInt()}m"
                )
            }
        }
    }

    private fun attachSurveyPhoto(filePath: String) {
        val dungeon = selectedSurveyDungeon() ?: return
        val viewpointId = _uiState.value.surveyViewpointId ?: return
        val survey = surveyFor(dungeon.id) ?: DungeonSurvey(dungeon.id)
        val draft = survey.viewpoints.firstOrNull { it.viewpointId == viewpointId } ?: return
        val existingPoint = draft.capture?.point
        val location = _uiState.value.currentLocation
            ?: existingPoint?.let { GeoPoint(it.latitude, it.longitude, it.accuracyMeters) }
            ?: dungeon.checkpoint.point.copy(simulated = true)
        val point = existingPoint?.copy(
            photoPath = filePath,
            note = draft.note ?: existingPoint.note
        ) ?: SurveyPoint(
            dungeonId = dungeon.id,
            type = SurveyPointType.VIEWPOINT,
            viewpointId = viewpointId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters ?: 999f,
            capturedAt = System.currentTimeMillis(),
            photoPath = filePath,
            note = draft.note
        )
        val samples = draft.capture?.samples ?: listOf(
            SurveyLocationSample(
                latitude = point.latitude,
                longitude = point.longitude,
                accuracyMeters = point.accuracyMeters,
                capturedAt = point.capturedAt
            )
        )
        val nextSurvey = survey.copy(
            viewpoints = survey.viewpoints.map { item ->
                if (item.viewpointId == viewpointId) {
                    item.copy(capture = SurveyCapture(point, samples), verified = false, status = "candidate")
                } else {
                    item
                }
            }
        )
        persistSurvey(updateSurveyDungeon(dungeon.id, nextSurvey))
    }

    private fun updateCurrentSurveyViewpoint(transform: (SurveyViewpointDraft) -> SurveyViewpointDraft) {
        val dungeon = selectedSurveyDungeon() ?: return
        val viewpointId = _uiState.value.surveyViewpointId ?: return
        val survey = surveyFor(dungeon.id) ?: return
        val nextSurvey = survey.copy(
            viewpoints = survey.viewpoints.map { draft ->
                if (draft.viewpointId == viewpointId) transform(draft) else draft
            }
        )
        persistSurvey(updateSurveyDungeon(dungeon.id, nextSurvey))
    }

    private fun surveyFor(dungeonId: String): DungeonSurvey? = _uiState.value.surveyBook.dungeons[dungeonId]

    private fun updateSurveyDungeon(dungeonId: String, survey: DungeonSurvey): SurveyBook =
        _uiState.value.surveyBook.copy(
            dungeons = _uiState.value.surveyBook.dungeons + (dungeonId to survey),
            updatedAt = System.currentTimeMillis()
        )

    private fun persistSurvey(book: SurveyBook) {
        val stamped = book.copy(updatedAt = System.currentTimeMillis())
        val effectiveContent = if (BuildConfig.DEBUG) {
            baseContent?.withSurveyOverlay(stamped)
        } else {
            baseContent
        }
        _uiState.update {
            it.copy(
                content = effectiveContent ?: it.content,
                surveyBook = stamped
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            surveyWriteMutex.withLock { surveyRepository.save(stamped) }
        }
    }

    override fun onCleared() {
        surveySamplingJob?.cancel()
        locationService.stopWatching()
        super.onCleared()
    }

    private fun onLocation(point: GeoPoint, autoFocusNearest: Boolean = false) {
        val content = _uiState.value.content ?: return
        val nearest = if (autoFocusNearest) nearestDungeon(point, content.dungeons) else null
        var nextProgress = _uiState.value.progress
        val arrivedNames = mutableListOf<String>()
        val accuracyBlockedNames = mutableListOf<String>()
        var accuracyBlockedLimit: Float? = null
        content.dungeons.forEach { dungeon ->
            val distance = distanceBetweenMeters(point, dungeon.checkpoint.point)
            val before = ProgressEngine.progressFor(nextProgress, dungeon.id)
            if (distance <= dungeon.checkpoint.radiusMeters) {
                if (before.arrivedAt == null && !hasArrivalAccuracy(dungeon, point)) {
                    accuracyBlockedNames += dungeon.name
                    accuracyBlockedLimit = accuracyBlockedLimit ?: checkpointAccuracyLimit(dungeon)
                    return@forEach
                }
                val after = ProgressEngine.markArrived(dungeon, before, point)
                if (after.arrivedAt != before.arrivedAt) arrivedNames += dungeon.name
                nextProgress = nextProgress.copy(dungeons = nextProgress.dungeons + (dungeon.id to after))
            }
        }
        if (nextProgress != _uiState.value.progress) persist(nextProgress)
        _uiState.update {
            it.copy(
                currentLocation = point,
                progress = nextProgress,
                mapFocusedDungeonId = nearest?.id ?: it.mapFocusedDungeonId,
                locationStatus = if (point.simulated) LocationStatus.SIMULATED else LocationStatus.READY,
                locationError = null,
                message = when {
                    arrivedNames.isNotEmpty() -> "已进入：${arrivedNames.joinToString("、")}"
                    accuracyBlockedNames.isNotEmpty() -> "已在 ${accuracyBlockedNames.joinToString("、")} 附近，但 GPS 精度不足（需 ≤${accuracyBlockedLimit?.toInt() ?: 50}m）"
                    nearest != null -> "已定位 · 最近副本：${nearest.name}"
                    it.message == "正在寻找你的位置…" -> "定位成功，可以查看附近副本"
                    else -> it.message
                },
                messageAction = when {
                    arrivedNames.isNotEmpty() || accuracyBlockedNames.isNotEmpty() || it.message == "正在寻找你的位置…" -> null
                    else -> it.messageAction
                }
            )
        }
    }

    private fun onLocationError(error: Throwable) {
        _uiState.update {
            it.copy(
                locationStatus = LocationStatus.ERROR,
                locationError = error.message ?: "定位失败",
                message = error.message ?: "定位失败，请检查系统定位开关",
                messageAction = null
            )
        }
    }

    private fun persist(progress: PlayerProgress) {
        val normalized = _uiState.value.content?.let { normalizeProgress(it, progress) } ?: progress
        _uiState.update { it.copy(progress = normalized) }
        viewModelScope.launch(Dispatchers.IO) { progressRepository.save(normalized) }
    }

    private fun normalizeProgress(content: ContentBundle, progress: PlayerProgress): PlayerProgress {
        val normalized = content.dungeons.associate { dungeon ->
            dungeon.id to ProgressEngine.normalize(dungeon, ProgressEngine.progressFor(progress, dungeon.id))
        }
        val cityComplete = content.dungeons.isNotEmpty() && content.dungeons.all { dungeon ->
            ProgressEngine.isMainCompleted(normalized[dungeon.id] ?: DungeonProgress())
        }
        val cityBadges = if (cityComplete) {
            progress.cityBadges + content.cityBadges.map { it.id }
        } else {
            progress.cityBadges
        }
        return progress.copy(dungeons = normalized, cityBadges = cityBadges)
    }

    private fun findDungeon(id: String): Dungeon? = _uiState.value.content?.dungeons?.firstOrNull { it.id == id }

    private fun defaultSurveyDungeonId(content: ContentBundle): String? =
        content.dungeons.firstOrNull { it.id == "orange-island" }?.id ?: content.dungeons.firstOrNull()?.id

    private fun defaultMapDungeonId(content: ContentBundle): String? =
        content.dungeons.firstOrNull { it.id == "orange-island" }?.id ?: content.dungeons.firstOrNull()?.id

    private fun selectedDungeonId(): String? = _uiState.value.selectedDungeonId

    private fun previousContentScreen(state: CityWalkerUiState): AppScreen =
        if (state.selectedViewpointId != null) AppScreen.VIEWPOINT else AppScreen.DETAIL

    private fun checkpointAccuracyLimit(dungeon: Dungeon): Float =
        minOf(50f, dungeon.checkpoint.radiusMeters / 2f)

    private fun hasArrivalAccuracy(dungeon: Dungeon, point: GeoPoint): Boolean =
        point.simulated || point.accuracyMeters?.let { it <= checkpointAccuracyLimit(dungeon) } == true

    private fun isCurrentlyAtCheckpoint(dungeon: Dungeon, point: GeoPoint?): Boolean =
        point != null &&
            distanceBetweenMeters(point, dungeon.checkpoint.point) <= dungeon.checkpoint.radiusMeters &&
            hasArrivalAccuracy(dungeon, point)

    private fun showMessage(message: String, action: UiMessageAction? = null) {
        _uiState.update { it.copy(message = message, messageAction = action) }
    }
}
