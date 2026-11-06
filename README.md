# 城市探险家 · Android MVP

这是一个原生 Android 小游戏，不是 Web 页面。项目使用 Kotlin、Jetpack Compose、CameraX、Fused Location Provider 和 SharedPreferences，当前默认城市为长沙。

## 已实现的 MVP 闭环

- 长沙城市地图与 10 个官方副本入口
- 首页使用天地图 Web Mercator 真实卫星瓦片；湖南多时相影像优先请求，服务或权限不可用时按瓦片回退到全国卫星影像，游戏 HUD、Marker 和暗色遮罩保持在底图之上
- 玩家首页以全屏地图为游戏世界：点击 Marker 先选中底部副本卡，再从副本卡进入；完整副本列表放在城市图鉴
- 副本详情采用单屏任务轨道，经典机位使用横向卡片切换
- 副本状态：未到达、已到达、主线完成、探索中、100% 完成
- GPS 打卡半径判断；Debug 详情页提供明显的“模拟抵达”按钮以便没有真机定位时跑通流程
- 主线照片：原生 CameraX 相机权限、预览和本地 JPEG 保存；相机不可用时使用本地 Demo 快门
- 经典机位：参考图、拍摄提示、方向与分数线
- 本地 Demo 评分：相似度、清晰度、曝光、总分和匹配结果，评分服务可替换为真实 AI
- 本地进度、照片、分数和印章持久化
- 自由探索：完成副本主线后，可无限记录带 GPS/时间/备注的“我的发现”，不影响官方探索度或完成判定
- 官方经典机位数量按内容动态计算；`requiredForCompletion=false` 的奖励机位不会改变既有 100% 规则
- 城市图鉴与副本印章
- Android 原生明信片生成、FileProvider 和系统分享面板
- 高德/百度地图 URI 导航与坐标复制
- Debug / Field Survey Mode：现场 GPS、精度、checkpoint 距离与半径、连续采样锚点、验证已有官方 viewpoint 或新增 viewpoint、参考图绑定、hint、verified、active、备注和 ZIP 导出
- 相机现场控制：点击预览进行自动对焦/测光，并提供设备支持的 -2～+2 EV 曝光补偿；App 保持竖屏 UI，但会监听设备物理方向更新 CameraX `targetRotation`，照片保存后再把 EXIF 方向归一化为真实像素方向
- 长沙地点封面与经典机位参考图已改为 APK 内本地素材；它们是内容采集前的地点定向视觉底稿，现场照片可通过踩点模式覆盖

## 打开与运行

用 Android Studio 打开本目录，安装 Android SDK 35，并让 Android Studio 同步 Gradle 8.9。连接 Android 设备或创建 API 26+ 模拟器后运行 `app` 配置。

构建地图前，在项目根目录 `.env` 提供 `TIANDITU_APIKEY=...`。Gradle 会在构建时注入 `BuildConfig`；`.env` 已被 Git 忽略，不应提交到仓库。地图运行时会先请求湖南天地图多时相 WMTS，单瓦片失败则自动使用 `img_w` 卫星影像，并叠加 `cia_w` 地名注记。

本机已使用 Gradle 8.9、Android SDK Platform 35 完成 `:app:testDebugUnitTest` 和 `:app:assembleDebug` 验证；当前没有连接 Android 真机或模拟器，因此尚未执行安装后的现场交互验收。首次在其他机器打开工程时，Android Studio 会负责下载缺失依赖并完成构建。

## 关键目录

- `app/src/main/assets/content/changsha.json`：长沙城市、副本、机位和印章配置；德思勤现场 checkpoint 半径为 200m
- `app/src/main/java/com/citywalker/mvp/domain`：状态机、进度与评分领域模型
- `app/src/main/java/com/citywalker/mvp/core`：定位、导航、相机、照片、评分和明信片能力
- `app/src/main/java/com/citywalker/mvp/ui`：原生小游戏页面与视觉主题

Debug 构建启动时会把 `survey_state.json` 中当前有效的 checkpoint，以及已完成 `verified=true/status=active` 的官方 viewpoint 覆盖到内存中的有效内容；Release 构建不会启用该 overlay。候选机位不会覆盖正式内容，原始 Survey 数据仍保留。

## 真机验收路径

1. 打开应用，进入地图，点击任意副本。
2. Debug 详情页点击“模拟抵达”，再点击“拍摄主线照片”；普通构建则需要真实 GPS 进入 checkpoint 半径。
3. 若授予相机权限，使用 CameraX 取景；否则点击快门会写入 Demo 现场照片。
4. 主线完成后打开任意经典机位，拍摄并等待本地 Demo 评分。
5. 从结果页生成明信片，在明信片页调用 Android 系统分享。
6. 返回图鉴，确认副本进度和印章已经保留。

## 踩点模式

在地图页长按顶部版本号 `v0.1.0`，进入 `Debug / 踩点模式`。选择副本后，可以查看设备 GPS、accuracy、配置 checkpoint 坐标、当前距离和 radius。普通模式主动点击“定位”后，会自动选中并聚焦到最近 checkpoint 对应的副本；之后的连续 GPS 更新不会抢走手工选中的 Marker。

点击“设当前位置为主线锚点”或“记录当前位置为 viewpoint”后，应用会连续采样约 8 秒，最多延长到 10 秒以尽量取得至少 5 个样本，并用纬度、经度和 accuracy 的中位数保存稳定点。checkpoint 每次重新采样都会追加历史记录，并通过 `selectedCheckpointCaptureId` 标记当前有效点；旧样本不会丢失。

验证官方机位时，从“验证已有机位”选择器进入，导出会沿用原内容 ID（例如 `desiqin-geometry`）；“新增机位”才会生成 `*-survey-XX`。新记录在坐标、参考图和 hint 完整并点击确认前始终保持 `candidate`。

Debug 踩点页还提供“清空当前副本”和“清空全部”玩家进度工具。它们只操作 `PlayerProgress`，不会删除 `survey_state.json`、踩点照片或导出数据。普通模式只有在当前位置仍处于 checkpoint 半径内时才允许主线相机；首次标记到达还要求 GPS 精度达到 `min(50m, radius/2)`，以避免低精度位置误触发。

踩点数据保存在应用内部的 `files/surveys/survey_state.json`。点击“导出踩点数据”会生成 `changsha_survey_YYYY-MM-DD.zip`，压缩包内包含 `survey.json` 与 `photos/<dungeonId>/...` 下的全部参考照片；JSON 使用压缩包内的相对路径，再用 Android 系统分享整个 ZIP 传回电脑或内容编辑流程。checkpoint 历史与当前有效采样点都会导出。
