# 《城市探险家》MVP需求文档

>  version: v0.1

## 1. 项目概述

游戏名（暂定）：城市探险家

定位：这是一个 Android 端 LBS 城市探索小游戏，通过游戏化方式鼓励用户走出门，探索真实城市。

玩法：玩家在地图上发现官方精选的城市副本，前往现实地点，通过 GPS 解锁副本并现场拍摄照片。

完成主线后获得：

- 城市图鉴记录
- 副本徽章
- 一张属于自己的旅行照片明信片

副本还包含 1–3 个经典摄影机位。玩家可以根据参考照片和提示自行寻找机位，完成额外摄影挑战，提高副本探索度并获得额外徽章。

核心原则：**GPS负责让玩家到达，眼睛负责寻找机位，AI负责增加摄影反馈。**

要求：AI 不得成为完成副本主线的前置条件。

---

## 2. MVP功能需求

### 2.1 核心目标

MVP只验证一个假设：**精选城市地标 + LBS真实移动 + 副本进度 + 摄影收藏，是否能够产生主动探索城市的动力。**

本阶段不验证社区、UGC或者复杂社交。

---

### 2.2 技术选型

仅支持：**Android**

不开发：

- iOS
- Web玩家端
- 微信小程序

推荐技术基线：

- Kotlin
- Jetpack Compose
- Android原生定位能力
- 天地图作为游戏浏览地图
- 本地持久化
- 本地小模型AI评分（或者免费的HTTP API）

开发优先级：功能跑通 > 架构优雅 > UI动画 > 性能极致。

---

### 2.3 首发城市

城市：**长沙**

首发副本：

1. 岳麓山
2. 橘子洲
3. 黄兴路步行街
4. 湖南省植物园
5. 湖南博物院
6. 洋湖湿地
7. 天心阁
8. 长沙图书馆
9. 开福寺
10. 德思勤城市广场

城市数据不得硬编码进UI。

所有副本必须通过配置数据加载。

---

### 2.4 核心玩法循环

```
长沙地图  
→ 浏览副本  
→ 点击副本  
→ 查看详情  
→ 调用外部地图导航  
→ 到达现实地点  
→ GPS进入Checkpoint范围  
→ 副本状态变为“已到达”  
→ App内现场拍照  
→ 主线完成  
→ 城市图鉴+1  
→ 解锁基础徽章  
→ 生成照片明信片  
→ 玩家可继续挑战经典机位  
→ AI评分  
→ 提升探索进度  
→ 解锁额外徽章
```

---

### 2.5 副本状态模型

统一状态：

```
`LOCKED`，尚未到达。

↓

`ARRIVED`，GPS 已进入主线 Checkpoint 半径。

↓

`MAIN_COMPLETED`，玩家已在现场完成至少一次 App 内拍照。该状态即代表：**副本已经完成，计入城市图鉴。**

↓

`EXPLORING`，正在继续完成经典机位。

↓

`COMPLETED_100`，所有经典机位已经完成。
```

重要规则：主线完成与100%探索必须分离。

---

### 2.6 探索进度规则

主线固定占：**50%**

经典机位共同占：**50%**

- 如果存在 1 个经典机位：主线：50% 、机位：50%

- 如果存在 2 个：主线：50% 、机位1：25% 、机位2：25%

- 如果存在 3 个：主线：50% 、每个机位：约16.67%

规则：进度计算应由程序动态生成，不在每个副本配置人工百分比。

---

### 2.7 主线判定规则

一个副本主线完成必须同时满足：

1. 用户GPS进入该副本Checkpoint半径；
2. 用户使用App现场拍摄至少一张照片。



AI不参与主线是否成功的判断。

照片内容不影响主线成功。

不得因为：

- 照片难看
- AI服务异常
- 模型判断错误
- 网络中断

导致用户现场打卡失败。

---

### 2.8 GPS规则

每个副本必须拥有：

- `checkpoint.latitude`

- `checkpoint.longitude`

- `checkpoint.radiusMeters`

不同副本半径允许不同。建议范围：50–200m。



禁止将整个景区作为一个巨大打卡区域。大型区域必须选择一个代表性主线锚点。

例如：

- 岳麓山：东方红广场

- 橘子洲：青年毛泽东艺术雕塑附近

---

### 2.9 经典机位 Viewpoint

每个副本：**1–3个**

每个机位至少包含：

- `id`

- `dungeonId`

- `name`

- `subtitle`

- `hint`

- `referenceImages`
- `latitude`

- `longitude`

- `source`

- `status`

- `recommendedOrientation`

- `scoreThreshold`

说明：

MVP：`source = official`，未来允许：`community`，但本阶段不得实现UGC。

机位GPS主要用于内容管理和未来功能。

MVP不要求玩家精确进入机位坐标。

玩家看到：

- 参考图
- 名称
- 提示
- 大致“就在附近”

而不是看到精确坐标。

---

### 2.10 摄影评分

#### 评分规则

第一阶段评分建议：`totalScore = similarity * 0.6 + sharpness * 0.2 + exposure * 0.2`

其中：

- `similarity`，用户照片和经典机位参考照片的视觉相似度。
- `sharpness`，清晰度。
- `exposure`，曝光合理程度。

具体模型允许后续替换。

UI只依赖统一返回接口，返回结构示例：

```json
{
  "totalScore": 87,
  "similarity": 86,
  "sharpness": 92,
  "exposure": 83,
  "matched": true
}
```

建议：`matched >= 55`，视为经典机位完成。

建议：`totalScore >= 80`，获得摄影徽章。

要求：网络、模型或者服务器异常时，不得影响已经完成的主线。经典机位评分应允许稍后重试。

#### AI模型

Android端不得直接依赖某一个具体模型实现，必须可替换。

定义统一接口：

```text
PhotoScoringService

score(
  dungeonId,
  viewpointId,
  photo
) -> PhotoScore
```

MVP允许：

A. 后端模型服务

B. 本地假数据Mock

C. 后续端侧模型

开发初期首先使用 Mock 实现跑通完整用户流程。

AI模型不得阻塞地图、打卡、图鉴和明信片功能开发。

---

## 3. 核心领域对象

总体：

```text
City
 └─ Dungeon
     ├─ Checkpoint
     ├─ Viewpoint[]
     ├─ Badge[]
     └─ PostcardDefinition
```

用户数据：

```text
PlayerProgress
 └─ DungeonProgress
      ├─ arrivedAt
      ├─ mainCompletedAt
      ├─ mainPhoto
      ├─ viewpointProgress[]
      ├─ explorationPercent
      └─ unlockedBadges[]
```

---

### 3.1 City

字段：

```text
id
name
subtitle
latitude
longitude
zoom
coverImage
dungeonIds[]
```

MVP仅：`changsha`，但程序结构不得假设永远只有一个城市。

---

### 3.2 Dungeon

建议字段：

```text
id
cityId
name
subtitle
description
category
difficulty
coverImage
checkpoint
viewpoints[]
badges[]
postcard
creatorType
status
```

MVP：

```text
creatorType = official
status = active
```

未来为用户自制副本保留扩展空间，但不得开发对应UI和后台。

---

### 3.3 Checkpoint

```text
id
name
latitude
longitude
radiusMeters
mainTaskTitle
mainTaskDescription
```

---

### 3.4 Viewpoint

```text
id
dungeonId
name
subtitle
hint
latitude
longitude
referenceImages[]
orientation
source
status
scoreThreshold
badgeId
```

---

### 3.5 Badge

建议支持三类：

- `MAIN`，完成副本主线。
- `PHOTO`，照片达到指定摄影评分。
- `EXPLORE`，副本100%。

字段：

```text
id
name
description
icon
type
condition
```

---

### 3.6 Postcard

每个副本定义：

```text
title
subtitle
locationText
templateId
```

生成时加入：

- 用户照片
- 副本名称
- 副标题
- 拍摄日期
- 摄影评分，可选
- 长沙城市探索进度
- App品牌标识

主线完成必须可以生成明信片。

优先照片：用户选择或者默认主线照片。

后期允许用户选择最高分经典机位照片。

---

## 4. 主要页面和组件

### 4.1 城市地图页

展示：

- 天地图
- 玩家当前位置
- 10个副本Marker
- 城市探索进度

Marker至少支持：未完成 、主线完成 、100%完成

点击Marker：打开Dungeon详情。

---

### 4.2 副本详情页

展示：

- 副本封面
- 名称
- 副标题
- 简介
- 距离
- 难度
- 当前探索度
- 主线任务
- 经典机位列表
- 已获得徽章

按钮：

- `去这里`，调用外部地图。
- `开始拍照`，只有GPS进入Checkpoint范围后启用。

---

### 4.3 外部导航

App本身不实现路线规划。

支持尝试唤起：

- 高德地图
- 百度地图

如果目标App不存在：允许复制位置或者打开通用地图URI。

不同地图坐标系必须通过独立：`CoordinateConverter`处理。

不得直接假设所有地图使用同一坐标。

---

### 4.4 相机页

MVP优先使用Android成熟相机能力。

要求：

- 只能通过游戏内发起拍照
- 保存本次照片
- 记录拍摄时间
- 记录当前副本ID
- 记录当前位置

MVP不允许从图库选择照片完成主线。

不实现：

- Root检测
- GPS模拟检测
- EXIF防作弊
- 高级反作弊

---

### 4.5 拍摄结果页

#### 主线普通照片

展示：“副本主线完成”，获得基础徽章。

提供：

`生成明信片`

#### 经典机位照片

额外显示：

- 机位匹配
- 清晰度
- 曝光
- 综合分
- 星级
- 是否解锁徽章

---

### 4.6 城市图鉴页

长沙探索：`x / 10`

每个副本卡片：

- 封面或者用户照片
- 副本名称
- 副标题
- 主线是否完成
- 探索度
- 徽章

未探索：灰色卡片。

完成全部10个副本，解锁城市级徽章：**长沙旅行家**

---

### 4.7 分享

MVP不接入完整社交SDK。

只实现：

生成明信片图片 →  Android系统分享

目标用户自行选择：微信 、小红书 、其他App

不实现：

- App内社区
- 动态流
- 评论
- 点赞
- 好友

---

## 5. 数据持久化

MVP必须支持退出App后保留：

- 已完成副本
- 主线照片
- 经典机位完成情况
- 照片分数
- 徽章
- 城市探索度

首版全部保存在本地。

不要求：

- 账号登录 
- 云同步 
- 跨设备同步

推荐实现：Repository层屏蔽具体存储方式。未来可以切换到服务器。

### Repository建议

```text
ContentRepository
ProgressRepository
PhotoRepository
LocationRepository
ScoringRepository
```

UI不得直接操作文件、数据库或者HTTP。

---

## 6. 内容配置

长沙副本数据应放在独立JSON或者资源文件中。

例如：

```text
assets/content/changsha.json
```

图片单独管理。

程序启动后：

```
ContentRepository  
→ 读取City  
→ 读取Dungeon  
→ 展示地图
```

禁止：在Compose页面里手写十个地点。

---

## 7. 目录结构

```text
app/

core/
  location/
  camera/
  map/
  navigation/
  sharing/
  scoring/
  storage/

domain/
  model/
  repository/
  usecase/

data/
  content/
  local/
  remote/

feature/
  map/
  dungeon/
  camera/
  scoring/
  collection/
  postcard/

ui/
  components/
  theme/
```

核心要求不是严格按照上述目录，而是：地图、定位、相机、评分、内容数据必须解耦。

---

## 8. 后续拓展

### MVP明确不做

以下功能全部属于Post-MVP：

- 好友

- 排行榜

- 第N位打卡者

- 账号系统

- UGC副本

- 自由机位投稿

- 社区App

- 点赞

- 评论

- 优质机位晋升官方机位

- 用户自制副本

- AR

- 路线规划

- 城市推荐算法

- 经验等级

- 虚拟货币

- 签到

- 商城

- 任务树

- 反作弊体系

- iOS

- 多城市运营后台


---

### Post-MVP预留方向

未来内容飞轮：

```
官方副本  
→ 用户探索  
→ 自由机位投稿  
→ 社区分享  
→ 点赞/评价  
→ 高质量机位成为推荐机位  
→ 官方机位
```

进一步：

```
用户创建副本  
→ 社区试玩  
→ 社区评价  
→ 精品副本  
→ 城市正式内容
```

当前只需要保证：`Viewpoint.source`和`Dungeon.creatorType`能够承载未来扩展。

不得提前实现对应业务。

---

## 9. MVP验收标准

MVP完成必须能够真实完成以下全过程：

- 用户在Android手机打开App；

- 看到长沙地图和10个官方副本；

- 点击橘子洲；

- 看到副本信息；

- 调用外部地图导航；

- 现实中抵达橘子洲；

- App识别用户进入打卡半径；

- 现场拍一张照片；

- 副本主线完成；

- 长沙图鉴从0/10变为1/10；

- 获得基础徽章；

- 生成自己的橘子洲明信片；

- 打开经典机位；

- 查看参考照片；

- 寻找相应位置；

- 现场再次拍摄；

- 得到摄影评分；

- 经典机位完成；

- 副本探索度增加；

- 退出App；

- 重新打开；

- 所有进度仍然存在。

- 只要上述流程真实可运行：


---

# 33. 优先级约束

如果实现过程中出现范围冲突，遵循：

第一优先级：真实世界完整主循环能够运行。

第二优先级：数据和状态正确保存。

第三优先级：现场GPS体验可靠。

第四优先级：照片采集与图鉴。

第五优先级：AI摄影反馈。

第六优先级：视觉效果。

任何非核心功能都不得牺牲前四项进度。

AI失效时，游戏仍然必须能够完成副本主线。

网络失效时，除AI评分外，核心探索流程尽可能保持可用。

最终交付目标不是展示一组页面，而是：**拿一台Android手机真正走到长沙街头，完成一次完整城市副本。**
