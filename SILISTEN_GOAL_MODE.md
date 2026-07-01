# SiListen Goal 模式执行文档

> 本文档为 Codex/Claude 自主执行模式设计。每个 Goal 独立、可验证、可回退。

## 项目基线

- 纯 Kotlin + Jetpack Compose Android 项目，包名 `com.silisten.app`
- 构建命令：`$env:JAVA_HOME='E:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebug`
- APK 输出：`app\build\outputs\apk\debug\app-debug.apk`
- 当前文件已拆分：`SiListenApp.kt`（壳）、`HomeAccountScreens.kt`、`PlayerScreens.kt`、`BottomChrome.kt`、`SettingsScreens.kt`、`PlaylistScreens.kt`、`CommonUi.kt`
- ViewModel：`SiListenViewModel.kt`（单一大 ViewModel，Compose State 驱动）
- 数据层：`NeteaseDirectApiClient.kt`（端内直连 WeAPI/EaPI 加密）、`NeteaseApiClient.kt`（封装+网关回退）、`NeteaseMusicSource.kt`
- 播放层：`PlayerController.kt`（Media3/ExoPlayer）、`PlaybackNotificationController.kt`

## 禁止事项（全局）

- 不允许未经 `assembleDebug` 构建验证就提交
- 不允许一次性重写超过 300 行的核心函数——拆成多个小步
- 不允许删除已工作的功能来规避 bug
- 不允许把多个无关改动塞进同一个提交
- 不允许未经用户确认直接 `git push`

---

## Goal 1：登录页重做——iOS 风格精简登录

### 现状
`HomeAccountScreens.kt` 中 `AccountScreen` 未登录时展示：`AccountLoginHero`（大块网易云品牌卡片）→ `LoginMethodSelector`（QR/SMS 切换按钮）→ `QrLoginCard` 或 `SmsLoginCard`。Hero 卡片占据大量空间，且 QR 登录受网易云风控限制（code 8821），实际只有短信登录可用。

### 目标
去掉 `AccountLoginHero` 大块品牌卡片，改为 iOS 风格简洁登录页：顶部 Logo + 标题，中间直接显示短信登录表单（优先），底部小字提供 QR 登录备选入口。

### 执行步骤
1. **删除 `AccountLoginHero` composable 的调用**——在 `AccountScreen` 的未登录分支中移除 `AccountLoginHero(...)` item。
2. **改写未登录分支布局**——顶部：居中 app icon（48dp）+ "SiListen" 标题 + "登录网易云音乐" 副标题；中间：直接展示 `SmsLoginCard`（不再需要 `LoginMethodSelector` 切换）；底部：一行小字 "使用二维码登录" 可点击展开 `QrLoginCard`。
3. **调整 `SmsLoginCard`**——手机号输入框改为圆角 iOS 风格（`RoundedCornerShape(14.dp)`），验证码输入用分离的数字输入格式（类似 Pinput），"获取验证码" 和 "登录" 按钮改为全宽胶囊按钮。
4. **保留 `loginMethod` 状态**——默认 `Sms`，点击底部 "二维码登录" 时切换。
5. **状态消息**——`state.loginState.message` 改为显示在表单下方的小字提示，不再用大卡片包裹。

### 涉及文件
- `HomeAccountScreens.kt`：`AccountScreen`、`AccountLoginHero`（删除或大幅简化）、`SmsLoginCard`、`LoginMethodSelector`
- 不改 ViewModel 和数据层

### 验收
- 未登录时页面顶部是 Logo+标题，中间是手机号+验证码表单
- 没有大块 "网易云" 品牌卡片
- 登录成功后正常跳转到已登录状态
- `assembleDebug` 通过

---

## Goal 2：歌词逐字填充平滑 + 两行回闪修复

### 现状
`AppleMusicLyricLineText` 使用 `drawWithContent` + `clipRect` 逐行裁剪实现填充。当一句歌词占两行时，`progress` 从第一行末尾跳到第二行开头会产生 <1s 的回闪——因为 `fillOffset = visibleEnd * p` 是线性的，但字符在两行间的 x 坐标不连续。

### 目标
消除两行歌词的回闪；确保最后一个字也被完整填充。

### 执行步骤
1. **改进 fillOffset 计算**——当前用 `visibleEnd * progress` 做全局线性映射。改为先算出当前行号和行内进度：对每行累计字符数，找到 progress 对应的行，再在该行内做局部线性插值。这样第一行结束时 clipX 就是 lineRight，第二行开始时 clipX 从 lineLeft 起步，不会有 x 坐标的跳跃。
2. **最后字符处理**——当 `progress >= 0.98f` 时，直接将所有行的 clipX 设为 lineRight。
3. **帧级插值**——`GlassLyricsPanel` 已有 `withFrameNanos` 帧插值（`interpolatedMs`），确保 `karaokeProgress` 用的是 `interpolatedMs` 而非 `playbackPositionMs`。

### 涉及文件
- `PlayerScreens.kt`：`AppleMusicLyricLineText`、`GlassLyricsPanel` 的 `karaokeProgress` 计算

### 验收
- 播放两行长歌词时，填充色从第一行扫到第二行无闪烁
- 最后一个字被完整填充
- `assembleDebug` 通过

---

## Goal 3：歌词翻译 + 罗马音显示

### 现状
`NeteaseMusicSource.lyrics()` 已升级到 v1 API，获取 `lrc`/`tlyric`/`romalrc` 三个字段。但 `LyricLine` 只有 `timeMs` + `text`，没有存翻译和罗马音。当前 `tlyric`/`romalrc` 只作为 fallback 使用——主歌词有的话它们就被丢弃了。

### 目标
粤语/英语/日语歌曲的歌词下方显示中文翻译和/或罗马音（如果 API 返回了的话）。

### 执行步骤
1. **扩展 `LyricLine`**——加 `translation: String? = null` 和 `romanization: String? = null` 字段。
2. **改写 `NeteaseMusicSource.lyrics()`**——同时解析 `lrc`、`tlyric`、`romalrc` 三份 LRC 文本，按 `timeMs` 合并到同一个 `LyricLine` 对象。合并逻辑：对每个主歌词行，找翻译和罗马音中 `timeMs` 最接近的行（误差 <500ms）配对。
3. **改写 `GlassLyricsPanel` 的歌词行渲染**——在 `AppleMusicLyricLineText` 下方，如果 `line.translation` 不为空则显示翻译文字（较小字号、半透明）；如果 `line.romanization` 不为空则显示罗马音（更小字号、更浅）。
4. **只在活跃行和 distance=1 的行显示翻译/罗马音**——更远的行只显示主歌词，避免信息过载。

### 涉及文件
- `Song.kt`：`LyricLine` data class
- `NeteaseMusicSource.kt`：`lyrics()` 方法、`parseLrc()` 方法
- `PlayerScreens.kt`：`GlassLyricsPanel` 歌词行渲染

### 验收
- 播放英文/粤语/日语歌曲时，歌词下方显示中文翻译
- 日语歌曲有罗马音时显示在翻译下方
- 纯中文歌曲不显示多余行
- `assembleDebug` 通过

---

## Goal 4：播放器详情页作为默认主页 + 封面点击跳歌词

### 现状
`FullPlayer` 用 `HorizontalPager` 展示 4 页：Detail（封面+控件）→ Lyrics → Queue → Comments。当前 Detail 页是大封面+标题+进度条+控件。歌词页有独立的小封面 header。

### 目标
Detail 页作为点击歌曲后的默认页面（入口主页）。点击封面图片时滑动到歌词页。歌词页顶部 header 改为与 Detail 页一致的沉浸式风格。

### 执行步骤
1. **封面可点击**——在 `PlayerDetailPage` 的封面 `AsyncImage` 上加 `noRippleClick`，点击时 `pagerScope.launch { pagerState.animateScrollToPage(1) }` 跳到歌词页。需要将 `pagerState` 和 `pagerScope` 传入 `PlayerDetailPage`。
2. **歌词 header 沉浸化**——`GlassLyricsPanel` 顶部的小封面+歌名+控件行，改为半透明背景融入动态取色背景，去掉分离感。用 `Color.Transparent` 背景 + 更大的 padding，与 Detail 页视觉语言一致。
3. **默认打开 Detail 页**——`openPlayerSheet` 默认 panel 改为 `PlayerSheetPanel.Detail`（当前默认是 `Lyrics`）。

### 涉及文件
- `PlayerScreens.kt`：`PlayerDetailPage`（加封面点击）、`GlassLyricsPanel`（header 样式）、`FullPlayer`（传参）
- `SiListenViewModel.kt`：`openPlayerSheet` 默认参数

### 验收
- 点击歌曲后打开的是 Detail 页（大封面）
- 点击封面滑动到歌词页
- 歌词页 header 视觉上与 Detail 页融为一体
- `assembleDebug` 通过

---

## Goal 5：底部导航栏搜索按钮独立化

### 现状
`SiListenNav` 有 4 个 tab：首页/搜索/音乐库/账号。搜索是和其他 tab 一样的普通按钮，与 `HorizontalPager` 联动。

### 目标
参考 iOS Apple Music——搜索不是一个 tab 页，而是一个独立的浮动按钮。底部导航栏只保留 3 个 tab（首页/音乐库/账号），搜索按钮放在底部栏右上角或独立位置，点击后弹出搜索 overlay。

### 执行步骤
1. **从 tabs 中移除 Search**——`SiListenNav` 的 tabs 改为 `Home/Sources/Account` 三个。`HorizontalPager` 的 `pageCount` 改为 3。
2. **新增搜索浮动按钮**——在 `SiListenBottomChrome` 区域加一个圆形搜索图标按钮，位置在迷你播放器右侧或底部栏上方。
3. **搜索 overlay**——点击搜索按钮后，搜索页以 `AnimatedVisibility`（从下滑入）覆盖当前内容。顶部搜索栏 + 结果列表。点击取消或空白区域关闭。
4. **保持 `SearchScreen` 逻辑**——搜索功能不变，只是入口从 tab 变成 overlay。
5. **更新 `AppTab` enum**——移除 `Search` 或保留但不在底部栏显示。

### 涉及文件
- `SiListenViewModel.kt`：`AppTab` enum 调整
- `SiListenApp.kt`：`HorizontalPager` pageCount、搜索 overlay 逻辑
- `BottomChrome.kt`：`SiListenNav` tabs 列表、搜索按钮
- `HomeAccountScreens.kt`：`SearchScreen` 保持不变

### 验收
- 底部栏只有首页/音乐库/账号 3 个 tab
- 搜索按钮明显可见，点击弹出搜索界面
- 搜索界面可以正常搜索和播放歌曲
- 返回后底部栏状态正常
- `assembleDebug` 通过

---

## Goal 6：通知栏状态栏歌词开关

### 现状
`PlaybackNotificationController` 已有爱心（喜欢）、上一首、播放/暂停、下一首、关闭 5 个 action。没有状态栏歌词功能。

### 目标
在设置中增加"状态栏歌词"开关。开启后，通知的 `setContentText` 显示当前歌词行文本而不是歌手名。

### 执行步骤
1. **新增设置项**——在 `PlaybackSettingsState` 中加 `statusBarLyricEnabled: Boolean = false`。在 `PlaybackSettingsScreen` 中加对应开关 UI。在 `SiListenViewModel` 中加 `setStatusBarLyricEnabled()` 方法和持久化。
2. **传递当前歌词到通知**——`syncNotification()` 时，如果 `statusBarLyricEnabled` 为 true 且有歌词，取当前 `activeLyricIndex` 对应的歌词文本作为 `contentText`。
3. **ViewModel 向 PlayerController 传递歌词**——`syncLyricsWithPlayback` 中，将当前歌词行文本存到一个 `currentLyricText: String?` 状态，`PlayerController.syncNotification` 读取它。

### 涉及文件
- `SiListenViewModel.kt`：新增设置项 + 当前歌词文本
- `PlaybackNotificationController.kt`：`show()` 方法读取歌词文本
- `SettingsScreens.kt`：`PlaybackSettingsScreen` 加开关
- `data/model/Song.kt`：`PlaybackSettingsState` 扩展

### 验收
- 设置中有 "状态栏歌词" 开关
- 开启后通知栏副标题显示当前歌词
- 关闭后显示歌手名（原行为）
- `assembleDebug` 通过

---

## 推荐执行顺序

1. **Goal 2**（歌词填充平滑）——影响面最小、改动集中在一个函数
2. **Goal 3**（歌词翻译+罗马音）——在 Goal 2 基础上扩展
3. **Goal 1**（登录页重做）——纯 UI 改动
4. **Goal 4**（Detail 页主页 + 封面跳歌词）——播放器交互调整
5. **Goal 5**（搜索按钮独立化）——底部栏重构，风险较高
6. **Goal 6**（通知栏歌词开关）——独立功能，随时可做

## 每轮执行输出格式

每轮完成后用中文输出：
- 本轮完成了什么
- 修改了哪些核心文件
- 运行了哪些验证
- 是否有未解决风险
- 是否已经提交
- 是否等待本人检测后再推送
