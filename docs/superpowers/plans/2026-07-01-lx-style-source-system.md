# 洛雪式音源系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 SiListen 的“音源与扩展”从简单内置音源列表升级为类似 LX Music 的在线平台、搜索范围、自动换源、播放接口和自定义源编辑入口。

**Architecture:** 第一阶段只落地配置层和 UI 层：新增可持久化的 `SourceSettingsState` 与 `CustomSourceConfig`，由 `SiListenViewModel` 暴露读写操作，`SettingsScreens.kt` 展示在线平台、默认搜索源、自动换源优先级、自定义接口源编辑。播放解析、跨源匹配和网易云身份桥接在后续任务接入，避免一次性改动播放链路导致回归。

**Tech Stack:** Kotlin、Jetpack Compose、SharedPreferences、org.json、现有单 ViewModel 架构。

## Global Constraints

- 不修改 `app/src/main/java/com/silisten/app/ui/BottomChrome.kt`。
- 不推送远端，除非用户测试无误并明确要求。
- 本地音乐不作为在线主内容源，只保留在音乐库/本地音乐入口。
- 网易云账号、评论、喜欢和歌单操作继续由网易云模块负责。
- 本阶段完成后运行 `./gradlew.bat :app:assembleDebug --console=plain`。

---

### Task 1: 音源设置模型与持久化

**Files:**
- Create: `D:/SiListen/app/src/main/java/com/silisten/app/data/model/SourceSettings.kt`
- Create: `D:/SiListen/app/src/main/java/com/silisten/app/data/repository/SourceSettingsStore.kt`

**Interfaces:**
- Produces: `SourceSettingsState`, `OnlinePlatformConfig`, `CustomSourceConfig`, `CustomSourceCapability`, `SourceSettingsStore.load()`, `SourceSettingsStore.save(state)`。
- Consumes: Android `Context` and SharedPreferences。

- [ ] 新增在线平台配置，内置 `netease/kuwo/qq/kugou/migu`。
- [ ] 新增搜索默认源、全部音源搜索开关、自动换源开关、播放优先级。
- [ ] 新增自定义源配置：名称、接口地址、启用状态、能力列表。
- [ ] 用 JSON 持久化自定义源，兼容旧配置缺失时自动回落默认值。

### Task 2: ViewModel 接入

**Files:**
- Modify: `D:/SiListen/app/src/main/java/com/silisten/app/SiListenViewModel.kt`

**Interfaces:**
- Consumes: `SourceSettingsStore.load/save`。
- Produces: `uiState.sourceSettings` and操作方法：`setOnlinePlatformEnabled`, `setDefaultSearchSource`, `setAllSourcesSearchEnabled`, `setAutoSourceFallbackEnabled`, `movePlaybackSource`, `saveCustomSource`, `deleteCustomSource`, `testCustomSource`。

- [ ] 初始化加载音源设置。
- [ ] 每个操作更新 `uiState.sourceSettings` 并立即保存。
- [ ] 测试自定义源本阶段只做配置校验：名称非空、URL 以 `http://` 或 `https://` 开头。

### Task 3: 音源与扩展页 UI

**Files:**
- Modify: `D:/SiListen/app/src/main/java/com/silisten/app/ui/SettingsScreens.kt`

**Interfaces:**
- Consumes: `uiState.sourceSettings` and ViewModel 操作方法。
- Produces: 用户可操作的设置页。

- [ ] 将页面文案改为“在线平台 / 搜索设置 / 自动换源播放 / 自定义播放接口 / 网易云账号能力”。
- [ ] 在线平台显示网易云、酷我、QQ、酷狗、咪咕开关。
- [ ] 默认搜索源显示为横向 chip：网易云、酷我、QQ、酷狗、咪咕、全部音源。
- [ ] 自动换源播放显示开关和播放优先级列表，上移/下移调整顺序。
- [ ] 自定义播放接口支持新增、编辑、删除、启用/禁用、测试。
- [ ] 不出现“本地音乐”作为主内容源。

### Task 4: 构建验证

**Files:**
- No source changes beyond Tasks 1-3.

- [ ] 运行 `D:/SiListen/gradlew.bat :app:assembleDebug --console=plain`。
- [ ] 确认无 Kotlin 编译错误。
- [ ] 汇报修改范围和未接入播放链路的边界。
