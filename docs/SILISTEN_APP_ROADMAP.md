# SiListen App 规划文档

## 1. 产品方向

SiListen 的目标不是做一个功能堆砌型播放器，而是做一个简洁、沉浸、带有 Apple Music 气质的 Android 音乐 App。

核心关键词：

- 简洁
- 沉浸
- 顺滑
- 留白
- 毛玻璃
- 细腻动画
- 可切换的歌词舞台

## 2. 设计原则

### 2.1 整体视觉

- 以 Apple Music 为主要参考，但不直接复刻 iOS 组件。
- 整体保持简洁，避免过多装饰型渐变、过度发光、信息堆积。
- 页面优先保证层级清晰：背景、内容卡片、播放控件、歌词区要一眼区分。
- 颜色以专辑封面和主题强调色为辅，默认基调保持干净克制。

### 2.2 动画原则

- 动画必须服务于状态切换，不做无意义抖动和炫技。
- 动效节奏参考 Apple Music：轻、柔、连续。
- 优先使用位移、透明度、缩放、模糊强弱变化，不使用廉价弹跳。
- 粒子模式只在歌词舞台里承担氛围作用，不能压过歌词本体。

### 2.3 歌词体验

提供两种歌词模式：

1. `Glass`
   参考 Apple Music 的毛玻璃歌词体验。
   特征：背景来自封面模糊、歌词区是半透明玻璃层、当前行突出、整体安静克制。

2. `Particles`
   参考 Mineradio 的粒子化舞台气质。
   特征：背景存在漂浮粒子、歌词更像舞台中央主角、强调氛围与演出感。

无论哪种模式，都要遵守“简洁优先”，不要把页面做得嘈杂。

## 3. 当前项目现状

已经具备的能力：

- Jetpack Compose 主界面
- 全屏播放器雏形
- 毛玻璃容器基础能力
- 网易云歌词拉取与同步
- 主题设置、播放设置、音质设置

当前主要问题：

- 全屏播放器视觉还不够统一，Apple Music 方向不够明确。
- 歌词区只有单一实现，没有成为正式可配置能力。
- 粒子歌词只是背景氛围雏形，还没有独立模式化。
- 设置页还缺少“歌词显示风格”入口。
- 动效和信息层级还需要进一步收敛，减少杂讯。

## 4. 分阶段实施路线

## Phase 1：播放器与歌词架构定型

目标：

- 建立 Apple Music 风格的全屏播放器基础版
- 正式支持双歌词模式
- 把歌词显示风格接入设置、状态和持久化

任务：

- 新增歌词显示模式枚举与持久化
- 在播放设置中加入歌词风格切换
- 重构全屏播放器顶部信息区、封面区、歌词区、控制区层级
- 实现 `Glass` 毛玻璃歌词模式
- 实现 `Particles` 粒子歌词模式

验收标准：

- 可以在设置中切换歌词模式
- 重启 App 后模式保持不丢失
- 两种模式视觉差异明确，但都保持简洁

## Phase 2：Apple Music 化细节完善

目标：

- 提升播放器整体高级感和一致性

任务：

- 让封面、标题、控制条、底部信息的间距更统一
- 优化拖动进度条的触感和视觉反馈
- 增强背景与封面色彩联动
- 收敛过重的阴影与不必要的文字标签
- 调整 mini player 与全屏播放器之间的过渡感

## Phase 3：歌词高级能力

目标：

- 让歌词从“能显示”升级到“可沉浸使用”

任务：

- 歌词自动滚动与当前行对焦
- 更自然的行间过渡
- 长句断行与多语言兼容
- 预留逐字歌词和翻译歌词结构
- 增加歌词空状态、加载状态、异常状态的精修

## Phase 4：整体产品收口

目标：

- 把首页、搜索、播放页、设置页统一到同一套产品语言

任务：

- 首页推荐卡片风格收敛
- 搜索结果和歌单详情样式统一
- 主题预览与真实播放器风格一致
- 调整文案口吻，减少“开发中感”

## 5. 技术建议

- 歌词模式优先做成枚举驱动，避免后续用多个布尔值叠加。
- 粒子模式使用低成本 Compose 动画即可，先保证稳定和帧率。
- 毛玻璃模式优先依赖现有 `LiquidGlassPane` 与模糊背景能力，不急着引入更重的图形方案。
- 后续若要做逐字歌词，再单独扩展歌词数据模型。

## 6. 本轮优先级

本轮先完成：

- 规划文档落地
- 歌词模式设置项
- 全屏播放器双歌词模式

本轮暂不做：

- 逐字歌词
- 翻译歌词
- 复杂粒子系统
- 全局页面大规模重写

## 7. 当前进度记录

已完成：

- `Phase 1` 的核心骨架已经落地
- 播放设置页已支持歌词风格切换
- 歌词风格已支持持久化保存
- 全屏播放器已拆分为 `Glass` / `Particles` 两种歌词模式
- 已开始推进 `Phase 2`，完成播放器顶部层级、封面舞台、进度区与底部信息的第一轮收口
- 已启动 `Phase 3`，歌词面板升级为围绕当前行自动聚焦的滚动式歌词舞台
- 已补充 mini player 进度反馈，并增强歌词长句展示、上下留白与当前行视觉锚点
- 已开始统一首页推荐区、歌单卡片与歌单详情操作区的卡片语言，向播放器视觉收敛
- 已继续收拢搜索页、音乐库页与账号页的头部和主卡片层级，减少原型感割裂
- 已开始提炼共享页面头卡与状态提示卡，并顺手清理一批过时图标用法
- 已补充共享空状态卡与加载状态卡，并继续细化歌词当前行切换的阅读节奏
- 已继续把零散提示并入共享状态卡，并优化歌词相邻行层次与长句阅读节奏
- 已把本地音乐扫描状态提升为更完整的状态区块，并继续增强歌词当前句停留感
- 已开始统一共享主次操作按钮，并让账号登录、本地扫描等入口的动作样式继续向简洁播放器语言收敛
- 已为歌词当前行补充更平滑的进行中进度表达，让 Glass / Particles 两种模式都更接近沉浸式阅读节奏
- 已继续收口 mini player 与 full player 的视觉语言，让底部播放器更像完整播放器的压缩态而不是独立模块
- 已补充 mini player 的模式标签、时间反馈与更柔和的进度过渡，强化播放器各状态之间的连续感
- 已接入通知栏音乐播放控制器，支持从系统通知直接执行上一首、播放/暂停与下一首
- 已补充 Android 13+ 通知权限请求，并把通知控制器和现有播放器状态保持同步联动
- 已开始系统修正深浅色模式下的标题与正文对比度，优先处理账号、音乐库、歌单和设置页的可读性问题
- 已把音源与扩展入口从音乐库页迁入设置页统一管理，并把搜索入口收成更接近 Apple Music 的悬浮小图标语言
- 已把普通按钮通知重做为真正的媒体样式音乐通知，并让点按通知可直接回到完整播放器
- 已继续加强 mini player 到完整播放器/歌词页的显式入口，减少用户找不到歌词入口的问题
- 已修正通知栏点按回流逻辑，确保从媒体通知返回时能可靠打开完整播放器而不是只回到普通页面
- 已把底部 mini player 从信息卡片重新收回更紧凑的音乐控制条样式，保留底部悬浮控制器的使用方式
- 已继续修正底部 chrome 层对 mini player 的承载方式，避免出现点歌后控制条不在底部正常显示的问题
- 已把底部 mini 控制条并回主界面 Compose 树渲染，并改成仅在存在当前播放歌曲时显示，减少独立覆盖层导致的不稳定显示问题
- 已补充歌单详情页的底部显示规则：进入歌单后隐藏底部导航 dock，但继续保留底部 mini 控制器，避免遮挡同时保持播放可控
- 已把完整播放器里的评论面板从占位卡片接成真实歌曲评论，支持网易云热门/最新评论切换、刷新、加载态、空状态和错误提示
- 已继续补齐完整播放器的播放列表面板交互，支持在队列里直接点歌切换当前播放项，并补上时长/当前项状态反馈
- 已继续打磨 mini player 到 full player 的连续感，让迷你态补上与完整播放器一致的模式信息，并把完整播放器底部面板切换改成更柔和的滑入淡入过渡
- 已继续统一完整播放器里唱片信息 / 播放列表 / 评论面板的头部层级与卡片描边语言，减少不同面板像多个模块拼接的割裂感
- 已继续细化完整播放器底部面板的切换方向感，让列表 / 歌词 / 评论之间按面板顺序滑动切换，而不是同方向硬切
- 已继续加强歌词面板的阅读节奏：补上更明显的顶部/底部留白、远近行衰减层次和当前行聚焦，让歌词页更像沉浸阅读舞台
- 已继续增强歌词长句和混合语言行的显示容错，让长句在当前行和邻近行阶段都获得更宽松的展示空间，减少沉浸阅读时的截断感
- 已继续补齐评论卡和播放列表卡片的按下反馈、当前态强调与时间信息层级，让它们更像播放器内部的可操作内容而不是静态列表
- 已继续增强歌词当前行的停留窗口判断：对连续句、长句和文本密度更高的段落给予更稳的聚焦与留白，让沉浸阅读不再过早收紧
- 已继续细化评论与播放列表卡片的信息层级：当前播放项补上“正在播放”状态，评论提示和回复/喜欢信息收进更清晰的卡片底部结构
- 已继续增强歌词对分段密集文本的识别：当一行更像翻译式断句或多段混排时，会给当前行更长的停留窗口并补上更明确的阅读聚焦提示
- 已继续细化评论卡和播放列表卡片的动画连续性：按下与当前态不只缩放，还加入更柔和的透明度变化，减少状态切换时的生硬感
- 已修正 mini player 在玻璃模式下可能没有明确尺寸导致完全不显示的问题，给悬浮胶囊条和内部玻璃容器补上稳定高度
- 已把系统媒体通知和 MediaSession 点击入口统一为直达歌词面板，并让 Activity 在 Compose 创建后使用同一个 ViewModel 处理回流
- 已进一步修正 mini player 的触发时机：点歌后会先把所选歌曲写入当前播放状态并显示底部控制条，流地址解析期间展示“正在准备播放”和加载反馈，避免网络准备阶段看起来完全没出现
- 已把完整播放器的列表面板重构为“当前歌曲 / 接下来”的明确结构，当前项独立展示，后续歌曲按真实队列顺序展示并保留点选切歌能力
- 已修正完整播放器 Glass 歌词页在浅色模式下的可读性，歌词文字、当前行光晕和进度轨道会根据深浅色主题切换
- 已继续完善完整播放器评论页，在热门评论 / 最新评论列表前补上当前歌曲封面、歌曲信息、评论分类、加载状态和评论总数摘要
- 已把设置入口和本地音乐入口收进账号页，账号页现在可以直接扫描/查看本地音乐，并进入外观、播放、音源和支持设置
- 已按 Apple Music 参考图重做底部 chrome：mini player 收成上方细长玻璃胶囊，底部 dock 收成首页/广播/资料库三段胶囊，并把搜索做成右侧独立圆形按钮
- 已为底部三段 dock 补上按住左右滑动交互，选中液态胶囊会跟随手指横向移动，释放后吸附到最近的 tab，并带轻微拉伸反馈

下一步优先：
- 歌词页的显示歌词有问题将进行优化，继续真机核对 mini player 在首页、歌单详情和不同主题下的可见性
- 继续优化歌词更长文本、翻译式断句和多语言混排时的沉浸阅读节奏
- 继续细化评论面板和播放列表面板内部卡片的更多状态反馈与层次表达
- 收敛首页、歌单详情与播放器之间的视觉语言
- 每次写完就得写进这个文档里

## 8. 2026-06-27 本轮继续完善

已完成：

- 修正 mini player 标题/歌手区域被圆角点击层裁剪的问题，文字区不再单独裁切，整条 mini player 仍可点击进入歌词/完整播放器。
- 强化 mini player 的半透明毛玻璃感，补上更稳定的高度、柔和高光、底部播放进度和更接近 iOS 的悬浮胶囊质感。
- 重排完整播放详情页：顶部改为紧凑歌曲信息卡，包含封面、歌曲名、歌手/专辑、喜欢按钮和播放按钮，减少原来信息块拼接感。
- 将完整播放器的四个入口调整为顶部下方的玻璃图标栏，顺序保持为唱片/详情、播放列表、歌词、评论，更接近参考图的操作结构。
- 将 Glass 歌词页改为更大的居中沉浸式歌词，弱化卡片边界，提升 Apple Music 风格的歌词阅读感。
- 将播放器评论页从重卡片列表调整为更轻的评论流，保留热门评论/最新评论切换、评论总数、头像、时间、点赞和回复数。
- 为底部三段 dock 增加拖动滞后光斑、高光层和选中胶囊拉伸，使按住左右滑动时更接近液态玻璃的牵引感。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。

下一步优先：

- 在真机上重点核对 mini player 是否始终显示在 dock 上方，以及歌词页入口是否符合预期。
- 继续对底部 dock 引入更深层的 backdrop/liquid glass 实现，必要时进一步参考 Kyant0/AndroidLiquidGlass 的 backdrop 分层方案。
- 根据真机截图微调完整播放器顶部间距、评论页文字大小和浅/深色模式对比度。

## 9. 2026-06-27 Dock 真液态玻璃修正

已完成：

- 根据用户再次强调的要求，停止使用“半透明圆角 + 光斑”的模拟方案，底部三段 dock 主体改为基于 Kyant0/AndroidLiquidGlass 思路的真实 backdrop 渲染。
- 在 App 根背景层加入 `LayerBackdrop` 导出，使 dock 能对背后页面内容进行真实取样，而不是只画一层半透明颜色。
- 新增 `KyantLiquidDockTabs`：外层玻璃使用 `drawBackdrop`，并启用 `vibrancy()`、`blur()`、`lens()`，选中胶囊使用 `rememberCombinedBackdrop` 叠加隐藏 tint 层。
- 选中胶囊已启用 `chromaticAberration = true` 的 lens 折射，并带按压进度、速度形变、内阴影和高光，拖动时会有真实液态牵引感。
- 保留旧版 dock 作为非液态玻璃/无 backdrop 场景下的 fallback，避免设置关闭或设备异常时底部导航不可用。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。

下一步优先：

- 真机核对 dock 在首页、歌单页返回、浅色/深色模式下的折射强度和文字可读性。
- 如果用户仍觉得不像 iOS，需要继续把搜索圆按钮和 mini player 也迁到同一套 Kyant backdrop 渲染链，避免底部 chrome 质感不一致。

## 10. 2026-06-27 启动闪退修复

问题原因：

- 接入 Kyant backdrop 后，最外层根 `Box` 被挂上了 `layerBackdrop(appBackdrop)`。
- 底部 dock 又在同一棵根树内部通过 `drawBackdrop` 读取这个 backdrop，导致 dock 间接把自己也记录进 backdrop。
- 真机上表现为 native 崩溃，crash buffer 里栈位于 `libhwui.so RenderNode::prepareTreeImpl`，属于渲染树递归/爆栈，不是普通 Kotlin `FATAL EXCEPTION`。

已修复：

- 将 `layerBackdrop(appBackdrop)` 从最外层根容器移到只包含主页面 `Scaffold` 的内容层。
- 底部 chrome、mini player、dock 不再被记录进它们自己要读取的 backdrop，避免自引用渲染。
- 保留 dock 对页面内容的真实取样能力，仍然使用 Kyant `drawBackdrop` / `lens` / `blur` / `vibrancy` 路线。
- 已重新编译、安装并通过 adb 启动验证，启动后进程保持存活，crash buffer 未再出现 `com.silisten.app` 的 `libhwui RenderNode` 崩溃。

## 11. 2026-06-27 Dock 点击与跟手修正

问题原因：

- Kyant 版 dock 的选中胶囊视觉层叠在按钮之上，同时挂了拖拽 `pointerInput`，导致点击事件被胶囊层吞掉，只剩滑动能触发。
- 拖动过程使用 `animateTo` 追目标值，每一帧都走弹簧动画，所以看起来过快、不跟手。

已修复：

- 将拖拽手势从选中胶囊层移到 dock 外层容器，选中胶囊只负责绘制和折射，不再拦截点击。
- tab 按钮改为直接 `clickable(indication = null)`，点击首页/广播/资料库可以正常切换。
- 拖动过程改为 `snapToValue`，手指移动时胶囊直接跟随；松手后再 `settleToValue` 吸附到最近 tab。
- 降低按压形变比例，避免滑动时胶囊被拉得过猛。
- 已重新编译、安装并通过 adb 启动验证，进程保持存活且 crash buffer 无新增崩溃。

## 12. 2026-06-27 Dock 点击二次修正

问题原因：

- 上一次虽然把拖拽手势从选中胶囊层移走了，但隐藏的 tint/backdrop 导出层仍然复用了 `KyantLiquidDockButton`。
- 该隐藏层虽然 `alpha(0f)`，但仍然带 `clickable`，并且 `onClick = {}`，所以它透明盖在可见按钮上继续吞点击。

已修复：

- 将隐藏 tint/backdrop 层的按钮替换为纯视觉组件 `KyantLiquidDockVisual`，不再带任何 `clickable`。
- 外层 dock 手势新增 `onTap`，轻点时直接根据点击 x 坐标计算目标 tab 并切换，不再只依赖子按钮点击。
- 拖动仍保持 `snapToValue` 跟手，松手再吸附。
- 已重新编译、安装，并用 adb 模拟点击 dock 三个区域，进程保持存活且 crash buffer 无新增崩溃。

## 13. 2026-06-27 Dock 拖动跟手修正

问题原因：

- 拖动期间仍然混入 `panelOffset` 滞后位移、press 形变和 velocity 拉伸，视觉位置不是纯粹由手指位置决定。
- `Animatable.snapTo` 仍通过 coroutine 调度，快速滑动时会有一帧到数帧的排队延迟。

已修复：

- 移除拖动期间的 `panelOffset` 滞后位移，dock 胶囊位置只由当前拖动值计算。
- 将拖动值从 `Animatable.value` 改为同步 Compose state `dragValue`，拖动回调中直接更新，手指移动立即反映到胶囊位置。
- 拖动期间禁用 velocity 拉伸和过强按压形变，避免视觉“飞过去”。
- 松手时再把 `Animatable` 同步到当前 `dragValue` 并执行吸附动画，避免松手跳变。
- 已重新编译、安装，并用 adb 做横向 swipe 验证，进程保持存活且 crash buffer 无新增崩溃。

## 14. 2026-06-27 17:19 播放页与 Dock 跟手再修正

已完成：

- 将 Kyant 液态 Dock 的拖动从 `dragAmount` 增量累加改为按手指绝对 `position.x / tabWidth` 实时计算，选中胶囊位置由当前触点直接驱动，减少拖动延迟和速度失真。
- 参考 KernelSU 的顶层外观状态做法，新增 `SiListenAppearance` 与 `LocalSiListenAppearance`，在 `SiListenApp` 顶层统一下发深浅色、模糊、悬浮底栏、液态玻璃、预测返回和强调色状态。
- 底部 chrome 已改为消费统一 Appearance 状态，避免 Dock、mini player、页面各自重复读取主题字段导致样式漂移。
- 重构完整播放器 Detail 页：大封面、暖色沉浸渐变、居中歌名歌手、喜欢/上一首/播放/下一首控制、波形式进度、底部四图标模式栏，整体更接近用户参考图的 iOS 播放详情结构。
- 重构非 Detail 面板顶部：歌词、队列、评论页使用小封面歌曲行 + 四图标模式栏 + 大内容区，不再套重卡片，让歌词和评论更像沉浸播放页内部视图。
- 将四图标模式栏改成图标主导的玻璃切换条，选中项使用亮度、缩放和底部小胶囊表达，弱化 Android 标签栏感。
- 保留现有 ViewModel 播放、喜欢、队列、歌词、评论数据流，不改业务回调，降低本轮 UI 重构对播放功能的风险。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。
- 已通过 `./gradlew.bat :app:installDebug --console=plain --no-daemon` 安装到真机 `24129PN74C - 16`。
- 已通过 adb 清理 crash buffer、强停、启动、检查进程与 crash 日志；启动后 `com.silisten.app` 进程存在，crash buffer 无新增崩溃。

下一步优先：

- 真机手动核对 Dock 拖动是否完全跟手，特别是慢拖、快速横滑、轻点三种手势。
- 根据真机截图微调播放器详情页封面尺寸、顶部安全区、歌词字号和评论页行距。
- 继续把普通页面的浅色/深色文字语义收敛到统一颜色系统，避免标题和正文在模式切换后对比度不稳定。

## 15. 2026-06-27 KernelSU 主题设置链路学习与收敛

已完成：

- 已克隆并阅读 `tiann/KernelSU` 的设置与主题相关源码：`SettingPager`、`ColorPaletteScreen`、`SettingsViewModel`、`SettingsRepositoryImpl`、`MainActivityViewModel`、`ThemeController`、`KernelSUTheme`。
- 明确 KernelSU 的主题入口不是散落在各页面，而是在设置页提供“主题”入口，进入 `ColorPaletteScreen` 后统一调整主题模式、Monet/种子色、调色板风格、模糊、悬浮底栏、玻璃底栏、预测返回和页面缩放。
- 明确 KernelSU 的核心架构：设置页写入 SharedPreferences；MainActivityViewModel 监听主题相关 preference key；顶层 `KernelSUTheme` 根据 `ThemeController.getAppSettings()` 生成 Material/Miuix 主题；`CompositionLocalProvider` 下发 `LocalColorMode`、`LocalEnableBlur`、`LocalEnableFloatingBottomBar`、`LocalEnableFloatingBottomBarBlur` 等全局外观开关。
- 将 SiListen 临时放在 `SiListenApp.kt` 的 `SiListenAppearance` / `LocalSiListenAppearance` 移入 `ui.theme.SiListenTheme`，让外观状态成为主题层能力，而不是页面私有状态。
- `SiListenTheme` 现在会在顶层同时 provide `LocalDensity` 和 `LocalSiListenAppearance`，底部 chrome / dock / mini player 直接消费主题层 Local，和 KernelSU 的全局外观开关思路保持一致。
- 为 `SiListenViewModel` 补充 `SharedPreferences.OnSharedPreferenceChangeListener`，监听主题偏好 key 后重新读取 `ThemeSettingsState` 并同步到 `uiState`，避免主题设置只依赖手动 setter 的临时状态。
- 主题页中“液态玻璃”开关改为只在“悬浮底栏”启用后显示，贴近 KernelSU 里 `enableFloatingBottomBar && Android 13+` 才展示玻璃开关的逻辑。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。

下一步优先：

- 继续把 SiListen 的主题设置页拆成更清晰的“预览 / 模式 / 颜色 / 显示效果 / 交互与缩放”结构，进一步贴近 KernelSU 的 ColorPaletteScreen 信息架构。
- 普通页面继续减少手写颜色，优先从 `MaterialTheme.colorScheme` 与 `LocalSiListenAppearance` 派生文字和容器语义。

## 16. 2026-06-27 KernelSU FloatingBottomBar 交互复刻修正

已完成：

- 重新阅读 KernelSU 的底栏实现：`component/FloatingBottomBar.kt`、`component/bottombar/BottomBar.kt`、`BottomBarMiuix.kt`、`ui/util/BlurExt.kt`，确认它的核心不是外层整条 dock 拖动，而是选中 indicator 自身接管长按拖动。
- 对照 KernelSU 的 `DampedDragAnimation`：新增 `canDrag` 限制，拖动只在当前选中胶囊范围内有效，避免长按非选中区域时胶囊瞬间跳到手指位置。
- 将 SiListen 的 Kyant dock 手势从外层容器移回选中胶囊层，外层仅保留高光视觉层，按钮点击继续由真实 tab item 处理。
- 拖动逻辑从绝对坐标跳转改回 KernelSU 风格的 `targetValue + dragAmount / tabWidth` 增量更新，松手按 `targetValue.roundToInt()` 吸附到最近 tab。
- 为本地 `DampedDragAnimation` 补齐 `canDrag`、`updateValue`、`animateToValue`，使拖动、点击和外部 tab 变化走统一的动画入口。
- 主题页里的“液态玻璃”开关现在只在“悬浮底栏”启用且 Android 13+ 时展示，说明也改成 backdrop 取样、模糊和镜头折射，贴近 KernelSU 的 `enableFloatingBottomBar && enableFloatingBottomBarBlur` 关系。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。
- 已通过 `./gradlew.bat :app:installDebug --console=plain --no-daemon` 安装真机，并用 adb 启动检查：`com.silisten.app` 进程存在，crash buffer 无新增崩溃。

下一步优先：

- 在真机上手动测试：只有长按当前选中胶囊再拖才应移动；轻点其它 tab 应直接切换；松手应吸附到最近 tab。
- 如果仍想要“整条 dock 任意位置长按拖动”，需要明确这是不同于 KernelSU 的交互，我会单独做一套手势规则而不是继续混用。

## 17. 2026-06-27 Dock 拖动飞边修复

已完成：

- 用户反馈“长按当前选中胶囊随便动一下就到了最左或最右”，定位为移动中的 indicator 自身接收 pointerInput，导致拖动过程中局部坐标系跟着 indicator 一起移动，`dragAmount` 出现反馈放大。
- 将 Dock 拖动手势放回固定外层容器，indicator 只负责视觉移动，避免移动节点坐标系影响拖动计算。
- 保留 KernelSU 风格规则：只有按下起点落在当前选中胶囊区域时才接受拖动；拖动开始后不再用当前胶囊区域持续截断，允许手指跨 tab 平滑拖动。
- `DampedDragAnimation` 新增 `dragAccepted`，在 drag start 时一次性判定是否接受拖动；未接受时不触发 press/release/drag stop，避免非选中区域长按误触发。
- 拖动仍使用 `targetValue + dragAmount / tabWidth` 增量更新，松手按最近 tab 吸附。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。
- 已通过 `./gradlew.bat :app:installDebug --console=plain --no-daemon` 安装真机，并用 adb 启动检查：`com.silisten.app` 进程存在，crash buffer 无新增崩溃。

下一步优先：

- 若真机手感仍不满足，停止继续微调当前 Dock，转向完整复刻 KernelSU 的 Material 3 / Miuix 主题设置与底栏结构，减少自研手势分歧。

## 18. 2026-06-27 KernelSU Material3 / Miuix 主题设置复刻

已完成：

- 根据用户要求停止继续围绕 Dock 手势做零散微调，转向复刻 KernelSU 的 Material 3 / Miuix 主题设置结构。
- 新增 `ThemeUiModeOption`，支持在主题设置中切换 `Material3` 与 `Miuix` 两种界面模式语义。
- 新增 `ThemePaletteStyleOption` 与 `ThemeColorSpecOption`，复刻 KernelSU `Color style` / `Color spec` 设置入口，并持久化到 `theme_settings`。
- `ThemeSettingsState`、加载/保存逻辑、SharedPreferences 监听 key 均已扩展，主题设置改动可被顶层主题状态同步感知。
- `ThemeSettingsScreen` 结构调整为更接近 KernelSU ColorPalette：预览卡、界面模式、主题模式、强调色、动态色算法、Monet、显示效果、交互与缩放。
- 新增 `ThemeUiModeSelector`，以分段按钮复刻 KernelSU 设置页中的 UI mode 入口：Material 3 / Miuix。
- 新增 `ThemeAdvancedColorOptions`，以 chips 形式提供 Color style 和 Color spec 选择，后续可接入真实动态调色库。
- `ThemePreviewCard` 已根据 Material3 / Miuix 模式显示不同文案、圆角、底栏与玻璃预览，用户切换时能看到区别。
- `SiListenTheme` 与 `LocalSiListenAppearance` 已携带 `uiMode`，并根据 Material3 / Miuix 模式微调基础 background/surface 色彩语义。
- 已通过 `./gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon` 编译验证。
- 已通过 `./gradlew.bat :app:installDebug --console=plain --no-daemon` 安装真机，并用 adb 启动检查：`com.silisten.app` 进程存在，crash buffer 无新增崩溃。

下一步优先：

- 如果要做到 KernelSU 级别的 Miuix 原生控件，需要评估是否正式引入 `top.yukonga.miuix.kmp`，否则当前是用 Material3 Compose 复刻设置功能和信息架构。
- 后续页面普通文字颜色与容器颜色应继续从 `MaterialTheme.colorScheme` / `LocalSiListenAppearance.uiMode` 派生，减少手写颜色导致的深浅色问题。
