# AGENTS.md — SiListen 项目 AI Agent 指南

## 项目概述

SiListen 是一个 Kotlin 原生 Android 音乐播放器，使用 Jetpack Compose UI + Media3/ExoPlayer 播放内核 + 网易云音乐端内直连（自实现 WeAPI/EaPI 加密，不依赖外部 Node.js 服务）。界面参考 iOS Apple Music 全屏播放器质感和 Spotify 深色音乐氛围。

## 技术栈

- **语言**：Kotlin（无 Java 代码）
- **UI**：Jetpack Compose（Material3 + 自研液态玻璃组件）
- **播放**：Media3 ExoPlayer 1.5.1 + MediaSession
- **网络**：OkHttp 4.12 + 自实现网易云 WEAPI/EAPI 加密
- **图片**：Coil Compose 2.7
- **取色**：AndroidX Palette 1.0
- **模糊/玻璃**：backdrop-android 2.0 + miuix-blur 0.9.2 + liquidglass-core 1.0.5 (AAR)
- **二维码**：ZXing Core 3.5.3
- **minSdk**：26，**compileSdk/targetSdk**：37

## 构建

```powershell
$env:JAVA_HOME = 'E:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

APK 位于 `app\build\outputs\apk\debug\app-debug.apk`。

## 项目结构

```
app/src/main/java/com/silisten/app/
├── MainActivity.kt                      # ComposeView 宿主，通知 intent 处理
├── SiListenViewModel.kt                 # 单一 AndroidViewModel，所有 UI 状态
├── data/
│   ├── model/Song.kt                    # Song, MusicPlaylist, LyricLine, PlaybackQuality 等
│   ├── source/
│   │   ├── MusicSource.kt              # 统一音源接口
│   │   ├── MusicSourceRegistry.kt      # 音源注册表
│   │   ├── NeteaseMusicSource.kt       # 网易云音源（搜索/播放/歌词/歌单/评论/红心）
│   │   ├── NeteaseApiClient.kt         # 上层封装（直连优先+网关回退+登录）
│   │   ├── NeteaseDirectApiClient.kt   # 端内直连 music.163.com（WeAPI 加密）
│   │   ├── DemoMusicSource.kt          # 公开示例音源
│   │   └── LocalMusicSource.kt         # 本地 MediaStore 音源
│   └── repository/
│       ├── MusicRepository.kt          # 音源门面
│       └── AccountRepository.kt        # 登录门面
├── playback/
│   ├── PlayerController.kt             # ExoPlayer 队列+MediaSession+通知
│   ├── PlaybackNotificationController.kt # 媒体通知（爱心/上一首/播放/下一首/关闭）
│   └── PlaybackNotificationReceiver.kt # 通知 Action BroadcastReceiver
└── ui/
    ├── SiListenApp.kt                  # 应用壳：Scaffold + HorizontalPager + ModalBottomSheet
    ├── HomeAccountScreens.kt           # 首页/搜索/音源/账号（含登录）
    ├── PlayerScreens.kt                # MiniPlayer + FullPlayer + 歌词面板 + 队列 + 评论
    ├── BottomChrome.kt                 # 底部导航栏 + MiniPlayer 容器
    ├── SettingsScreens.kt              # 设置页（主题/播放/音源/捐赠）
    ├── PlaylistScreens.kt             # 歌单详情页
    ├── CommonUi.kt                     # 公共组件（按钮/卡片/搜索框/跑马灯等）
    ├── theme/SiListenTheme.kt          # 主题（双 UI 模式/5 强调色/明暗/缩放）
    └── kernelsu/                       # 液态玻璃组件库
        ├── KernelSuFloatingBottomBar.kt
        ├── liquid/  (CombinedBackdrop, Vibrancy, Lens, InnerShadow)
        └── animation/  (InteractiveHighlight, DampedDragAnimation, InspectDragGestures)
```

## 架构要点

### 状态管理
- `SiListenViewModel` 是唯一的 ViewModel，持有 `uiState: SiListenUiState`（Compose `mutableStateOf`）
- 播放状态通过 `PlayerController.state: PlaybackState` 暴露
- 账号状态：`accountState: AccountUiState`
- 无 DI 框架——手动在 ViewModel 构造函数中创建依赖

### 网易云直连
- `NeteaseDirectApiClient` 自实现 `NeteaseCrypto.weApi()` 和 `eApi()` 加密
- 请求优先走直连 `music.163.com`，失败回退到本地 NeteaseCloudMusicApi 网关
- Cookie 通过 `PersistentCookieJar` 存储在 SharedPreferences
- 设备指纹 cookies 在 `NeteaseDirectApiClient.init` 时自动注入

### 播放器
- `PlayerController.playQueue()` 先解析首曲 streamUrl 并立即播放，然后并发解析后续 24 首
- `playSession` 版本号防止异步竞态
- 自定义 HTTP 头（Referer/Origin）绕过网易云 CDN 限制
- 通知使用 `MediaStyleNotificationHelper` + `MediaSession`

### 歌词
- `NeteaseMusicSource.lyrics()` 调用 `/weapi/song/lyric/v1`，解析 `lrc`/`tlyric`/`romalrc`
- `LyricLine(timeMs, text)` 存储时间轴歌词
- `GlassLyricsPanel` 使用 `withFrameNanos` 帧级插值实现 60fps 歌词进度
- `AppleMusicLyricLineText` 使用双层 Text + `drawWithContent` + `clipRect` 逐行裁剪实现逐字填充

## UI 文件分工

- `SiListenApp.kt` 只保留应用壳、全局状态和页面编排，不再继续堆积具体页面实现
- `BottomChrome.kt`：底部悬浮栏和迷你播放器
- `PlayerScreens.kt`：播放页、歌词、播放队列、评论、歌曲详情
- `PlaylistScreens.kt`：歌单详情和歌单评论
- `SettingsScreens.kt`：设置页、主题、外观、音源配置
- `HomeAccountScreens.kt`：首页、搜索、音乐库、账号、登录、二维码、本地音乐
- `CommonUi.kt`：通用视觉组件、颜色、玻璃容器、共享小组件

## 工作规则

- 每次修改前都必须先查看 `git status --short`
- 未提交的现有改动默认视为用户改动，除非明确是本轮任务产生的内容
- 未经明确允许，不得回退、删除或覆盖用户已有改动
- 优先采用小步、可验证、可回退的修改方式
- Kotlin 代码变更后必须运行 `.\gradlew.bat assembleDebug`
- 涉及 UI、播放、通知、模糊、导航、返回手势等高风险改动后，需要安装到设备并检查崩溃日志
- 不得擅自推送到远端
- 只有在本人实际检测并确认无误后，才允许推送到远端

## 设计方向

- 以 Apple Music 作为主要产品参考，追求简洁、沉浸、克制的视觉体验
- 所有文字必须在浅色和深色模式下都清晰可读
- 玻璃、模糊、透明和动效必须服务于可读性，不能把内容糊成马赛克
- 液态底栏应尽量贴近 KernelSU/Miuix 的液态玻璃行为和反馈
- 不得为了修复无关问题而直接删除液态底栏、迷你播放器或歌词入口
- 用户可见文案需要面向普通用户，不能暴露内部实现名

## 约定——不要做

- 不要引入 Hilt/Koin 等 DI 框架——当前手动依赖注入是有意为之
- 不要引入 Navigation Compose——当前用 HorizontalPager + 手动状态管理
- 不要引入 Room 数据库——当前只用 SharedPreferences 和内存缓存
- 不要拆分 ViewModel——当前单 ViewModel 是设计选择
- 不要更改 `NeteaseDirectApiClient` 的加密实现，除非网易云接口变动
- 不要同时绘制完整歌词和逐字歌词造成重影
- 不要为了动画效果牺牲流畅性和可读性
- 不要用普通文本通知冒充媒体控制器
- 不要在界面中显示内部 API 名称或 debug 信息

## 验证清单

- 查看 `git status --short`
- Kotlin 变更后运行 `.\gradlew.bat assembleDebug`
- 高风险变更后安装到设备并启动应用
- 检查是否有新的 `logcat -b crash` 崩溃记录
- 验证底部栏点击、拖动、页面滑动都能准确切页
- 验证播放歌曲后迷你播放器出现
- 验证点击迷你播放器可以进入播放页或歌词页
- 验证播放页的歌词、队列、评论、详情入口正常
- 验证浅色和深色模式下主要标题、列表、按钮、设置项都可读

## 参考资料

- `.reference/bujuan/` — Flutter 网易云播放器参考（bujuan_music_api 在 GitHub: 2697a/bujuan_music_api）
- `.reference/Mineradio/` — Electron 音乐播放器参考
- `.reference/KernelSU/` — 液态玻璃 UI 组件参考
- `.services/api-enhanced/` — NeteaseCloudMusicApi Enhanced（Node.js 网关，调试备用）
