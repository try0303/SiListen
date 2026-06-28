# SiListen

SiListen 是一个使用 Kotlin 原生 Android 技术栈实现的音乐播放器原型，界面使用 Jetpack Compose，播放内核使用 Media3/ExoPlayer。

界面方向参考了 iOS 的全屏播放器质感和 Spotify 的深色音乐氛围：包含首页推荐、搜索、音源切换、底部迷你播放器、全屏播放页、队列控制、上一首/下一首和进度拖动。

## 当前内容

- `MusicSource`：统一音源协议，负责推荐、搜索和播放地址解析。
- `NeteaseApiClient`：网易云接口客户端，负责短信验证码、手机号登录、Cookie 保存、登录状态刷新和退出登录。
- `NeteaseMusicSource`：默认网易云音乐音源，复用登录态 Cookie 进行搜索和播放地址解析。
- `DemoMusicSource`：公开示例音频音源，用于验证播放队列，也可作为接入其他 API 的模板。
- `PlayerController`：基于 Media3/ExoPlayer 的播放队列、播放暂停、上一首、下一首、拖动进度和状态同步；播放前会过滤无法解析播放地址的歌曲。
- `MainActivity`：首页、搜索、音源页、账号登录页、迷你播放器和全屏播放器的 Compose 界面。

## 网易云登录

账号页提供手机号验证码登录：

1. 输入手机号。
2. 点击“获取验证码”。
3. 输入短信验证码。
4. 点击“登录”。

登录成功后会保存网易云 Cookie，后续默认网易云音源会带登录态请求 `/song/url/v1` 解析播放地址。

## 接入其他音源

新增一个类实现 `MusicSource`，再注册到 `MusicSourceRegistry.create()` 即可。

```kotlin
class MyMusicSource : MusicSource {
    override val info = MusicSourceInfo(...)
    override suspend fun featured(): List<MusicPlaylist> = ...
    override suspend fun search(keyword: String): List<Song> = ...
    override suspend fun streamUrl(song: Song): String = ...
}
```

## 构建

在 Android Studio 打开 `D:\SiListen`，或在命令行运行：

```powershell
$env:JAVA_HOME='E:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

已验证的调试包位置：

```text
D:\SiListen\app\build\outputs\apk\debug\app-debug.apk
```
