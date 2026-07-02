package com.silisten.app

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.PlaylistKind
import com.silisten.app.data.model.PlaylistRoute
import com.silisten.app.data.model.Song
import com.silisten.app.data.model.CustomPlaybackSourceType
import com.silisten.app.data.model.CustomSourceConfig
import com.silisten.app.data.model.inferCustomSourceType
import com.silisten.app.data.model.normalizeCustomSourceType
import com.silisten.app.data.model.SourcePlatformIds
import com.silisten.app.data.model.SourceSettingsState
import com.silisten.app.data.model.neteaseIdentityId
import com.silisten.app.data.model.normalizeBuiltInPlatformId
import com.silisten.app.data.repository.AccountRepository
import com.silisten.app.data.repository.MusicRepository
import com.silisten.app.data.repository.RecentPlaybackStore
import com.silisten.app.data.repository.SourceSettingsStore
import com.silisten.app.data.source.MusicSourceRegistry
import com.silisten.app.data.source.CustomPlaybackSourceClient
import com.silisten.app.data.source.NeteaseApiClient
import com.silisten.app.data.source.NeteaseLoginState
import com.silisten.app.playback.PlaybackState
import com.silisten.app.playback.PlaybackCenter
import com.silisten.app.playback.PlaybackNotificationBridge
import com.silisten.app.playback.PlaybackStreamResolver
import com.silisten.app.playback.PlaybackMode
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppTab { Home, Search, Sources, Account, Settings }

enum class SearchResultKind { Songs, Playlists, Albums, Artists }

enum class ArtistPageTab { Detail, Hot, Songs, Albums }

enum class SettingsRoute { Main, Theme, Playback, Source, Donation }

enum class ThemeModeOption { System, Light, Dark }

enum class ThemeUiModeOption { Material3, Miuix }

enum class ThemePaletteStyleOption { TonalSpot, Vibrant, Expressive, Fidelity, Content, Monochrome }

enum class ThemeColorSpecOption { Default, Spec2021, Spec2025 }

enum class ThemeAccentOption { Emerald, Rose, Sky, Amber, Violet }

enum class LyricDisplayMode { Glass, Particles }

enum class PlayerSheetPanel { Detail, Queue, Lyrics, Comments }

data class ThemeSettingsState(
    val uiMode: ThemeUiModeOption = ThemeUiModeOption.Miuix,
    val mode: ThemeModeOption = ThemeModeOption.Light,
    val accent: ThemeAccentOption = ThemeAccentOption.Emerald,
    val paletteStyle: ThemePaletteStyleOption = ThemePaletteStyleOption.TonalSpot,
    val colorSpec: ThemeColorSpecOption = ThemeColorSpecOption.Default,
    val monetEnabled: Boolean = false,
    val blurEnabled: Boolean = true,
    val floatingBottomBarEnabled: Boolean = true,
    val floatingBottomBarBlurEnabled: Boolean = true,
    val predictiveBackEnabled: Boolean = false,
    val uiScale: Float = 1f
)

data class PlaybackSettingsState(
    val quality: PlaybackQuality = PlaybackQuality.ExHigh,
    val lyricDisplayMode: LyricDisplayMode = LyricDisplayMode.Glass,
    val playbackMode: PlaybackMode = PlaybackMode.Order,
    val statusBarLyricEnabled: Boolean = false
)

data class SleepTimerState(
    val active: Boolean = false,
    val durationMinutes: Int = 0,
    val remainingMs: Long = 0L,
    val waitUntilSongEnds: Boolean = false,
    val pendingStopAfterCurrentSong: Boolean = false
)

data class ArtistSongsPageState(
    val artistId: String = "",
    val songs: List<Song> = emptyList(),
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

data class SiListenUiState(
    val selectedTab: AppTab = AppTab.Home,
    val settingsRoute: SettingsRoute = SettingsRoute.Main,
    val themeSettings: ThemeSettingsState = ThemeSettingsState(),
    val playbackSettings: PlaybackSettingsState = PlaybackSettingsState(),
    val sourceSettings: SourceSettingsState = SourceSettingsState(),
    val selectedSourceId: String = "netease",
    val featured: List<MusicPlaylist> = emptyList(),
    val dailyDiscovery: MusicPlaylist? = null,
    val recommendedPlaylists: List<MusicPlaylist> = emptyList(),
    val personalFm: MusicPlaylist? = null,
    val podcasts: MusicPlaylist? = null,
    val cloudDrive: MusicPlaylist? = null,
    val likedSongs: MusicPlaylist? = null,
    val userPlaylists: List<MusicPlaylist> = emptyList(),
    val recentPlayedSongs: List<Song> = emptyList(),
    val localSongs: List<Song> = emptyList(),
    val localMusicMessage: String = "点击扫描本地音乐",
    val isLibraryLoading: Boolean = false,
    val selectedPlaylist: MusicPlaylist? = null,
    val playlistBackStack: List<MusicPlaylist> = emptyList(),
    val selectedPlaylistRoute: PlaylistRoute = PlaylistRoute.Overview,
    val playlistSongSearchQuery: String = "",
    val selectedArtistTab: ArtistPageTab = ArtistPageTab.Detail,
    val artistSongsPage: ArtistSongsPageState = ArtistSongsPageState(),
    val isPlaylistDetailLoading: Boolean = false,
    val playlistDetailMessage: String? = null,
    val playlistCommentSort: PlaylistCommentSort = PlaylistCommentSort.Hot,
    val playlistComments: List<PlaylistComment> = emptyList(),
    val playlistCommentCount: Int = 0,
    val isPlaylistCommentsLoading: Boolean = false,
    val playlistCommentsMessage: String? = null,
    val playerCommentSort: PlaylistCommentSort = PlaylistCommentSort.Hot,
    val playerComments: List<PlaylistComment> = emptyList(),
    val playerCommentCount: Int = 0,
    val isPlayerCommentsLoading: Boolean = false,
    val playerCommentsMessage: String? = null,
    val likedSongIds: Set<String> = emptySet(),
    val songLikeLoadingIds: Set<String> = emptySet(),
    val likePrompt: LikePromptState? = null,
    val playlistChooserSong: Song? = null,
    val playlistAddLoadingIds: Set<String> = emptySet(),
    val subscribedPlaylistIds: Set<String> = emptySet(),
    val isPlaylistSubscriptionLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val searchPlaylists: List<MusicPlaylist> = emptyList(),
    val searchAlbums: List<MusicPlaylist> = emptyList(),
    val searchArtists: List<MusicPlaylist> = emptyList(),
    val searchHasMoreSongs: Boolean = false,
    val searchHasMorePlaylists: Boolean = false,
    val searchHasMoreAlbums: Boolean = false,
    val searchHasMoreArtists: Boolean = false,
    val isLoadingMoreSearch: Boolean = false,
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val isLyricLoading: Boolean = false,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val message: String? = null
)

data class LikePromptState(
    val song: Song,
    val message: String = "已添加到我喜欢的音乐",
    val showAddToPlaylistAction: Boolean = true
)

private data class SearchInitialResults(
    val songs: List<Song>,
    val playlists: List<MusicPlaylist>,
    val albums: List<MusicPlaylist>,
    val artists: List<MusicPlaylist>
)

data class AccountUiState(
    val phone: String = "",
    val captcha: String = "",
    val loginState: NeteaseLoginState = NeteaseLoginState(),
    val sendingCode: Boolean = false,
    val loggingIn: Boolean = false,
    val smsCooldownSeconds: Int = 0,
    val qrLogin: QrLoginUiState = QrLoginUiState()
)

data class QrLoginUiState(
    val key: String? = null,
    val qrImg: String? = null,
    val qrUrl: String? = null,
    val message: String = "点击生成网易云二维码",
    val loading: Boolean = false,
    val polling: Boolean = false
)

class SiListenViewModel(application: Application) : AndroidViewModel(application) {
    private val neteaseApiClient = NeteaseApiClient(application)
    private val accountRepository = AccountRepository(neteaseApiClient)
    private val themePreferences = application.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
    private val playbackPreferences = application.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
    private val recentPlaybackStore = RecentPlaybackStore(application)
    private val sourceSettingsStore = SourceSettingsStore(application)
    private val initialSourceSettings = sourceSettingsStore.load()
    private val themePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in themePreferenceKeys) {
            val next = loadThemeSettings()
            if (next != uiState.themeSettings) {
                uiState = uiState.copy(themeSettings = next)
            }
        }
    }
    private val musicRepository = MusicRepository(
        registry = MusicSourceRegistry.create(neteaseApiClient) { currentPlaybackQuality() },
        customPlaybackSourceClient = CustomPlaybackSourceClient(application),
        playbackQualityProvider = { currentPlaybackQuality() }
    )
    val registry: MusicSourceRegistry = musicRepository.registry
    private val player = PlaybackCenter.controller(application)

    var uiState by mutableStateOf(
        SiListenUiState(
            selectedSourceId = SourcePlatformIds.ALL,
            themeSettings = loadThemeSettings(),
            playbackSettings = loadPlaybackSettings(),
            sourceSettings = initialSourceSettings,
            recentPlayedSongs = recentPlaybackStore.load()
        )
    )
        private set

    var accountState by mutableStateOf(AccountUiState())
        private set

    var isPlayerSheetVisible by mutableStateOf(false)
        private set

    var playerSheetPanel by mutableStateOf(PlayerSheetPanel.Detail)
        private set

    private var qrLoginJob: Job? = null
    private var searchJob: Job? = null
    private var smsCooldownJob: Job? = null
    private var lyricJob: Job? = null
    private var libraryJob: Job? = null
    private var playerCommentsJob: Job? = null
    private var artistSongsJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var loadedLyricSongId: String? = null
    private var loadedPlayerCommentSongId: String? = null
    private var lastRecentSongId: String? = null
    private var lastLibraryRefreshAt = 0L
    private val artistTabMemory = mutableMapOf<String, ArtistPageTab>()
    private val notificationLikeHandler = { toggleCurrentSongLike() }
    private val notificationLikeStateProvider: (Song) -> Boolean = { song -> isSongLiked(song) }

    val playbackState: PlaybackState
        get() = player.state

    private fun playbackResolver(): PlaybackStreamResolver = { song ->
        musicRepository.resolvePlayable(song, uiState.sourceSettings)
    }

    init {
        themePreferences.registerOnSharedPreferenceChangeListener(themePreferenceListener)
        PlaybackNotificationBridge.attachLikeHandler(notificationLikeHandler)
        PlaybackNotificationBridge.attachLikeStateProvider(notificationLikeStateProvider)
        player.setPlaybackMode(uiState.playbackSettings.playbackMode)
        viewModelScope.launch {
            loadFeatured()
        }
        viewModelScope.launch {
            delay(1500)
            refreshLoginState()
        }
        viewModelScope.launch {
            delay(200)
            while (true) {
                player.updateProgress()
                syncRecentPlayback()
                syncLyricsWithPlayback()
                syncStatusBarLyricWithPlayback()
                syncSleepTimerWithPlayback()
                delay(100)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        uiState = uiState.copy(
            selectedTab = tab,
            settingsRoute = if (tab == AppTab.Settings) uiState.settingsRoute else SettingsRoute.Main
        )
        if (tab == AppTab.Account && accountState.loginState.loggedIn && shouldRefreshLibrary()) {
            refreshLibrary(force = false)
        }
    }

    fun openThemeSettings() {
        uiState = uiState.copy(settingsRoute = SettingsRoute.Theme)
    }

    fun openPlaybackSettings() {
        uiState = uiState.copy(settingsRoute = SettingsRoute.Playback)
    }

    fun openSourceSettings() {
        uiState = uiState.copy(settingsRoute = SettingsRoute.Source)
    }

    fun openDonationSettings() {
        uiState = uiState.copy(settingsRoute = SettingsRoute.Donation)
    }

    fun closeThemeSettings() {
        uiState = uiState.copy(settingsRoute = SettingsRoute.Main)
    }

    fun selectThemeMode(mode: ThemeModeOption) {
        updateThemeSettings { copy(mode = mode) }
    }

    fun selectThemeUiMode(mode: ThemeUiModeOption) {
        updateThemeSettings { copy(uiMode = mode) }
    }

    fun selectThemeAccent(accent: ThemeAccentOption) {
        updateThemeSettings { copy(accent = accent) }
    }

    fun selectThemePaletteStyle(style: ThemePaletteStyleOption) {
        updateThemeSettings { copy(paletteStyle = style) }
    }

    fun selectThemeColorSpec(spec: ThemeColorSpecOption) {
        updateThemeSettings { copy(colorSpec = spec) }
    }

    fun setMonetEnabled(enabled: Boolean) {
        updateThemeSettings {
            copy(monetEnabled = enabled)
        }
    }

    fun setBlurEnabled(enabled: Boolean) {
        updateThemeSettings { copy(blurEnabled = enabled) }
    }

    fun setFloatingBottomBarEnabled(enabled: Boolean) {
        updateThemeSettings { copy(floatingBottomBarEnabled = enabled) }
    }

    fun setFloatingBottomBarBlurEnabled(enabled: Boolean) {
        updateThemeSettings { copy(floatingBottomBarBlurEnabled = enabled) }
    }

    fun setPredictiveBackEnabled(enabled: Boolean) {
        updateThemeSettings { copy(predictiveBackEnabled = enabled) }
    }

    fun setUiScale(scale: Float) {
        updateThemeSettings { copy(uiScale = scale.coerceIn(0.85f, 1.15f)) }
    }

    private fun updateThemeSettings(update: ThemeSettingsState.() -> ThemeSettingsState) {
        val next = uiState.themeSettings.update()
        uiState = uiState.copy(themeSettings = next)
        saveThemeSettings(next)
    }

    fun selectPlaybackQuality(quality: PlaybackQuality) {
        val next = uiState.playbackSettings.copy(quality = quality)
        uiState = uiState.copy(playbackSettings = next)
        savePlaybackSettings(next)
    }

    fun selectLyricDisplayMode(mode: LyricDisplayMode) {
        val next = uiState.playbackSettings.copy(lyricDisplayMode = mode)
        uiState = uiState.copy(playbackSettings = next)
        savePlaybackSettings(next)
    }

    fun setStatusBarLyricEnabled(enabled: Boolean) {
        val next = uiState.playbackSettings.copy(statusBarLyricEnabled = enabled)
        uiState = uiState.copy(playbackSettings = next)
        savePlaybackSettings(next)
        syncStatusBarLyricWithPlayback()
    }

    fun selectPlaybackMode(mode: PlaybackMode) {
        val next = uiState.playbackSettings.copy(playbackMode = mode)
        uiState = uiState.copy(playbackSettings = next)
        savePlaybackSettings(next)
        player.setPlaybackMode(mode)
    }

    fun selectSource(sourceId: String) {
        searchJob?.cancel()
        uiState = uiState.copy(
            selectedSourceId = sourceId,
            searchQuery = "",
            searchResults = emptyList(),
            searchPlaylists = emptyList(),
            searchAlbums = emptyList(),
            searchArtists = emptyList(),
            searchHasMoreSongs = false,
            searchHasMorePlaylists = false,
            searchHasMoreAlbums = false,
            searchHasMoreArtists = false,
            isLoadingMoreSearch = false
        )
        loadFeatured()
    }

    fun setAutoSourceFallbackEnabled(enabled: Boolean) {
        updateSourceSettings { copy(autoSourceFallbackEnabled = enabled) }
    }

    fun setSearchPlatformEnabled(platformId: String, enabled: Boolean) {
        val normalizedId = normalizeBuiltInPlatformId(platformId)
        updateSourceSettings {
            val next = if (enabled) {
                enabledSearchPlatformIds + normalizedId
            } else {
                enabledSearchPlatformIds - normalizedId
            }
            copy(enabledSearchPlatformIds = next)
        }
        rerunSearchIfNeeded()
    }

    fun setCommentPlatformEnabled(platformId: String, enabled: Boolean) {
        val normalizedId = normalizeBuiltInPlatformId(platformId)
        updateSourceSettings {
            val next = if (enabled) {
                enabledCommentPlatformIds + normalizedId
            } else {
                enabledCommentPlatformIds - normalizedId
            }
            copy(enabledCommentPlatformIds = next)
        }
        loadedPlayerCommentSongId = null
        playbackState.currentSong?.let { loadPlayerComments(it, force = true) }
    }

    fun saveCustomSource(config: CustomSourceConfig) {
        val name = config.name.trim()
        val endpoint = config.endpoint.trim()
        if (name.isBlank()) {
            uiState = uiState.copy(message = "\u8bf7\u586b\u5199\u97f3\u6e90\u540d\u79f0")
            return
        }
        if (config.script.isBlank() && !endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            uiState = uiState.copy(message = "\u63a5\u53e3\u5730\u5740\u9700\u8981\u4ee5 http:// \u6216 https:// \u5f00\u5934")
            return
        }
        val nextConfig = config.copy(
            id = config.id.ifBlank { SourceSettingsStore.newCustomSourceId() },
            name = name,
            endpoint = endpoint,
            type = normalizeCustomSourceType(
                endpoint,
                if (config.endpoint != endpoint) inferCustomSourceType(endpoint) else config.type
            )
        )
        updateSourceSettings {
            val exists = customSources.any { it.id == nextConfig.id }
            copy(
                customSources = if (exists) {
                    customSources.map { if (it.id == nextConfig.id) nextConfig else it }
                } else {
                    customSources + nextConfig
                }
            )
        }
        uiState = uiState.copy(message = "\u97f3\u6e90\u914d\u7f6e\u5df2\u4fdd\u5b58")
    }

    fun importCustomSourceFromUrl(endpoint: String) {
        val url = endpoint.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            uiState = uiState.copy(message = "请输入以 http:// 或 https:// 开头的脚本链接")
            return
        }
        if (uiState.sourceSettings.customSources.size >= 20 &&
            uiState.sourceSettings.customSources.none { it.endpoint.trim() == url }
        ) {
            uiState = uiState.copy(message = "最多只能保留 20 个自定义源，请先删除不常用的源")
            return
        }
        val initialConfig = CustomSourceConfig(
            id = SourceSettingsStore.newCustomSourceId(),
            name = "自定义源",
            endpoint = url,
            enabled = true,
            type = CustomPlaybackSourceType.LxScript
        )
        uiState = uiState.copy(message = "正在导入自定义源...")
        viewModelScope.launch {
            val importResult = runCatching {
                musicRepository.importCustomSourceScript(url)
            }.getOrElse { error ->
                uiState = uiState.copy(message = "自定义源导入失败：${error.message.orEmpty().ifBlank { "脚本无法初始化" }}")
                return@launch
            } ?: run {
                uiState = uiState.copy(message = "自定义源导入失败：无法下载脚本，请检查链接或网络")
                return@launch
            }
            val result = importResult.inspect
            if (!result.ok) {
                uiState = uiState.copy(message = "自定义源导入失败：${result.message}")
                return@launch
            }
            val imported = initialConfig.copy(
                id = uiState.sourceSettings.customSources
                    .firstOrNull { it.endpoint.trim() == url }
                    ?.id
                    ?: initialConfig.id,
                name = result.displayName?.takeIf { it.isNotBlank() }
                    ?: initialConfig.name,
                description = result.description.orEmpty(),
                author = result.author.orEmpty(),
                homepage = result.homepage.orEmpty(),
                version = result.version.orEmpty(),
                supportedSources = result.supportedSources,
                allowShowUpdateAlert = true,
                script = importResult.script
            )
            updateSourceSettings {
                val exists = customSources.any { it.id == imported.id || it.endpoint.trim() == imported.endpoint }
                copy(
                    customSources = if (exists) {
                        customSources.map { source ->
                            if (source.id == imported.id || source.endpoint.trim() == imported.endpoint) imported else source
                        }
                    } else {
                        customSources + imported
                    }
                )
            }
            uiState = uiState.copy(message = "自定义源导入成功：${imported.name}")
        }
    }

    fun importCustomSourceFromScript(fileName: String, script: String) {
        val cleanScript = script.trim()
        if (cleanScript.isBlank()) {
            uiState = uiState.copy(message = "自定义源导入失败：脚本内容为空")
            return
        }
        if (uiState.sourceSettings.customSources.size >= 20) {
            uiState = uiState.copy(message = "最多只能保留 20 个自定义源，请先删除不常用的源")
            return
        }
        uiState = uiState.copy(message = "正在导入本地自定义源...")
        viewModelScope.launch {
            val result = runCatching {
                musicRepository.inspectCustomSourceScript(cleanScript)
            }.getOrElse { error ->
                uiState = uiState.copy(message = "自定义源导入失败：${error.message.orEmpty().ifBlank { "脚本无法初始化" }}")
                return@launch
            }
            if (!result.ok) {
                uiState = uiState.copy(message = "自定义源导入失败：${result.message}")
                return@launch
            }
            val endpoint = "local://${fileName.ifBlank { "custom-source.js" }}"
            val imported = CustomSourceConfig(
                id = SourceSettingsStore.newCustomSourceId(),
                name = result.displayName?.takeIf { it.isNotBlank() } ?: "本地自定义源",
                endpoint = endpoint,
                enabled = true,
                type = CustomPlaybackSourceType.LxScript,
                description = result.description.orEmpty(),
                author = result.author.orEmpty(),
                homepage = result.homepage.orEmpty(),
                version = result.version.orEmpty(),
                supportedSources = result.supportedSources,
                allowShowUpdateAlert = true,
                script = cleanScript
            )
            updateSourceSettings {
                copy(customSources = customSources + imported)
            }
            uiState = uiState.copy(message = "自定义源导入成功：${imported.name}")
        }
    }

    fun setCustomSourceEnabled(sourceId: String, enabled: Boolean) {
        updateSourceSettings {
            copy(
                customSources = customSources.map { source ->
                    if (source.id == sourceId) source.copy(enabled = enabled) else source
                }
            )
        }
    }

    fun setCustomSourceAllowUpdateAlert(sourceId: String, enabled: Boolean) {
        updateSourceSettings {
            copy(
                customSources = customSources.map { source ->
                    if (source.id == sourceId) source.copy(allowShowUpdateAlert = enabled) else source
                }
            )
        }
    }

    fun deleteCustomSource(sourceId: String) {
        updateSourceSettings {
            copy(customSources = customSources.filterNot { it.id == sourceId })
        }
        uiState = uiState.copy(message = "\u5df2\u5220\u9664\u81ea\u5b9a\u4e49\u97f3\u6e90")
    }

    fun testCustomSource(config: CustomSourceConfig) {
        val name = config.name.trim()
        val endpoint = config.endpoint.trim()
        when {
            name.isBlank() -> {
                uiState = uiState.copy(message = "请填写音源名称")
                return
            }
            config.script.isBlank() && !endpoint.startsWith("http://") && !endpoint.startsWith("https://") -> {
                uiState = uiState.copy(message = "音源地址需要以 http:// 或 https:// 开头")
                return
            }
        }
        uiState = uiState.copy(message = "正在测试自定义音源...")
        viewModelScope.launch {
            val result = runCatching {
                musicRepository.inspectCustomSource(
                    config.copy(
                        name = name,
                        endpoint = endpoint,
                        type = normalizeCustomSourceType(
                            endpoint,
                            if (config.endpoint != endpoint) inferCustomSourceType(endpoint) else config.type
                        )
                    )
                )
            }.getOrElse { error ->
                uiState = uiState.copy(message = "音源测试失败：${error.message.orEmpty()}")
                return@launch
            }
            uiState = uiState.copy(
                message = if (result.ok) {
                    result.message
                } else {
                    result.message.ifBlank { "音源测试失败" }
                }
            )
        }
    }

    private fun updateSourceSettings(update: SourceSettingsState.() -> SourceSettingsState) {
        val next = uiState.sourceSettings.update()
        uiState = uiState.copy(sourceSettings = next)
        sourceSettingsStore.save(next)
    }

    fun openPlaylist(playlist: MusicPlaylist) {
        val isArtist = playlist.kind == PlaylistKind.Artist || playlist.id.startsWith("netease-artist-")
        val needsDetail = playlist.songs.isEmpty() &&
            (playlist.id.startsWith("netease-playlist-") ||
                playlist.id.startsWith("netease-album-") ||
                playlist.id.startsWith("netease-artist-"))
        val nextBackStack = uiState.selectedPlaylist
            ?.takeIf { it.id != playlist.id }
            ?.let { uiState.playlistBackStack + it }
            ?: uiState.playlistBackStack
        val nextArtistTab = if (isArtist) {
            artistTabMemory[playlist.id] ?: ArtistPageTab.Detail
        } else {
            uiState.selectedArtistTab
        }
        val nextArtistSongsPage = if (isArtist && uiState.artistSongsPage.artistId != playlist.id) {
            artistSongsJob?.cancel()
            ArtistSongsPageState(artistId = playlist.id, hasMore = true)
        } else {
            uiState.artistSongsPage
        }
        uiState = uiState.copy(
            selectedPlaylist = playlist,
            playlistBackStack = nextBackStack,
            selectedPlaylistRoute = PlaylistRoute.Overview,
            playlistSongSearchQuery = "",
            selectedArtistTab = nextArtistTab,
            artistSongsPage = nextArtistSongsPage,
            isPlaylistDetailLoading = needsDetail,
            playlistDetailMessage = null,
            playlistCommentSort = PlaylistCommentSort.Hot,
            playlistComments = emptyList(),
            playlistCommentCount = 0,
            isPlaylistCommentsLoading = false,
            playlistCommentsMessage = null
        )
        if (!needsDetail) {
            return
        }
        viewModelScope.launch {
            val detail = runCatching { musicRepository.neteasePlaylistDetail(playlist) }.getOrElse {
                uiState = uiState.copy(
                    isPlaylistDetailLoading = false,
                    playlistDetailMessage = "内容详情加载失败：${it.message.orEmpty()}"
                )
                return@launch
            }
            if (uiState.selectedPlaylist?.id != playlist.id) return@launch
            uiState = uiState.copy(
                selectedPlaylist = detail,
                recommendedPlaylists = uiState.recommendedPlaylists.replacePlaylist(detail),
                userPlaylists = uiState.userPlaylists.replacePlaylist(detail),
                featured = uiState.featured.replacePlaylist(detail),
                isPlaylistDetailLoading = false,
                playlistDetailMessage = if (detail.songs.isEmpty()) "这里暂时没有可播放歌曲" else null
            )
        }
    }

    fun closePlaylist() {
        val previousPlaylist = uiState.playlistBackStack.lastOrNull()
        if (previousPlaylist != null) {
            val previousArtistTab = if (previousPlaylist.kind == PlaylistKind.Artist) {
                artistTabMemory[previousPlaylist.id] ?: uiState.selectedArtistTab
            } else {
                uiState.selectedArtistTab
            }
            uiState = uiState.copy(
                selectedPlaylist = previousPlaylist,
                playlistBackStack = uiState.playlistBackStack.dropLast(1),
                selectedPlaylistRoute = PlaylistRoute.Overview,
                playlistSongSearchQuery = "",
                selectedArtistTab = previousArtistTab,
                isPlaylistDetailLoading = false,
                playlistDetailMessage = null,
                playlistCommentSort = PlaylistCommentSort.Hot,
                playlistComments = emptyList(),
                playlistCommentCount = 0,
                isPlaylistCommentsLoading = false,
                playlistCommentsMessage = null,
                isPlaylistSubscriptionLoading = false
            )
            return
        }
        uiState = uiState.copy(
            selectedPlaylist = null,
            playlistBackStack = emptyList(),
            selectedPlaylistRoute = PlaylistRoute.Overview,
            playlistSongSearchQuery = "",
            selectedArtistTab = ArtistPageTab.Detail,
            isPlaylistDetailLoading = false,
            playlistDetailMessage = null,
            playlistCommentSort = PlaylistCommentSort.Hot,
            playlistComments = emptyList(),
            playlistCommentCount = 0,
            isPlaylistCommentsLoading = false,
            playlistCommentsMessage = null,
            isPlaylistSubscriptionLoading = false
        )
    }

    fun playSelectedPlaylist(startIndex: Int = 0) {
        uiState.selectedPlaylist?.let { playPlaylist(it, startIndex) }
    }

    fun playSelectedArtistTab(startIndex: Int = 0) {
        val playlist = uiState.selectedPlaylist ?: return
        val page = uiState.artistSongsPage
        if (
            playlist.kind == PlaylistKind.Artist &&
            uiState.selectedArtistTab == ArtistPageTab.Songs &&
            page.artistId == playlist.id &&
            page.songs.isNotEmpty()
        ) {
            viewModelScope.launch {
                player.playQueue(page.songs, startIndex.coerceIn(0, page.songs.lastIndex), playbackResolver())
            }
            return
        }
        playPlaylist(playlist, startIndex)
    }

    fun selectArtistTab(tab: ArtistPageTab) {
        val artist = uiState.selectedPlaylist?.takeIf { it.kind == PlaylistKind.Artist } ?: return
        artistTabMemory[artist.id] = tab
        uiState = uiState.copy(selectedArtistTab = tab)
        if (tab == ArtistPageTab.Songs) {
            loadMoreArtistSongs(initial = true)
        }
    }

    fun loadMoreArtistSongs(initial: Boolean = false) {
        val artist = uiState.selectedPlaylist?.takeIf { it.kind == PlaylistKind.Artist } ?: return
        val currentPage = uiState.artistSongsPage
        val sameArtist = currentPage.artistId == artist.id
        if (currentPage.isLoading) return
        if (sameArtist && !initial && !currentPage.hasMore) return
        if (sameArtist && initial && currentPage.songs.isNotEmpty()) return

        val existingSongs = if (sameArtist && !initial) currentPage.songs else emptyList()
        val offset = existingSongs.size
        val requestArtistId = artist.id
        artistSongsJob?.cancel()
        artistSongsJob = viewModelScope.launch {
            uiState = uiState.copy(
                artistSongsPage = ArtistSongsPageState(
                    artistId = requestArtistId,
                    songs = existingSongs,
                    hasMore = true,
                    isLoading = true
                )
            )
            val nextSongs = runCatching {
                musicRepository.neteaseArtistSongs(artist, ARTIST_SONG_PAGE_SIZE, offset)
            }.getOrElse {
                if (uiState.selectedPlaylist?.id == requestArtistId) {
                    uiState = uiState.copy(
                        artistSongsPage = ArtistSongsPageState(
                            artistId = requestArtistId,
                            songs = existingSongs,
                            hasMore = existingSongs.isNotEmpty(),
                            isLoading = false,
                            message = "单曲加载失败：${it.message.orEmpty()}"
                        )
                    )
                }
                return@launch
            }
            if (uiState.selectedPlaylist?.id != requestArtistId) return@launch
            val combined = (existingSongs + nextSongs).distinctBy { it.id }
            uiState = uiState.copy(
                artistSongsPage = ArtistSongsPageState(
                    artistId = requestArtistId,
                    songs = combined,
                    hasMore = nextSongs.size >= ARTIST_SONG_PAGE_SIZE,
                    isLoading = false,
                    message = if (combined.isEmpty()) "暂时没有获取到更多单曲" else null
                )
            )
        }
    }

    fun isSelectedPlaylistSubscribed(): Boolean {
        val playlist = uiState.selectedPlaylist ?: return false
        return isPlaylistSubscribed(playlist)
    }

    fun showPlaylistOverview() {
        if (uiState.selectedPlaylist == null) return
        uiState = uiState.copy(selectedPlaylistRoute = PlaylistRoute.Overview)
    }

    fun showPlaylistComments() {
        val playlist = uiState.selectedPlaylist ?: return
        uiState = uiState.copy(selectedPlaylistRoute = PlaylistRoute.Comments)
        if (uiState.playlistComments.isEmpty() && !uiState.isPlaylistCommentsLoading) {
            loadPlaylistComments(playlist, force = true)
        }
    }

    fun updatePlaylistSongSearchQuery(query: String) {
        uiState = uiState.copy(playlistSongSearchQuery = query)
    }

    fun selectPlaylistCommentSort(sort: PlaylistCommentSort) {
        if (uiState.playlistCommentSort == sort && uiState.playlistComments.isNotEmpty()) return
        uiState = uiState.copy(playlistCommentSort = sort)
        uiState.selectedPlaylist?.let { loadPlaylistComments(it, force = true) }
    }

    fun refreshPlaylistComments() {
        uiState.selectedPlaylist?.let { loadPlaylistComments(it, force = true) }
    }

    fun selectPlayerCommentSort(sort: PlaylistCommentSort) {
        if (uiState.playerCommentSort == sort && uiState.playerComments.isNotEmpty()) return
        uiState = uiState.copy(playerCommentSort = sort)
        playbackState.currentSong?.let { loadPlayerComments(it, force = true) }
    }

    fun refreshPlayerComments() {
        playbackState.currentSong?.let { loadPlayerComments(it, force = true) }
    }

    fun toggleSelectedPlaylistSubscription() {
        val playlist = uiState.selectedPlaylist ?: return
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "请先登录网易云账号后再收藏歌单")
            return
        }
        if (uiState.isPlaylistSubscriptionLoading || playlist.kind == PlaylistKind.LikedSongs) return
        val subscribe = !isPlaylistSubscribed(playlist)
        viewModelScope.launch {
            uiState = uiState.copy(isPlaylistSubscriptionLoading = true)
            val result = runCatching {
                musicRepository.neteaseTogglePlaylistSubscription(playlist, subscribe)
            }.getOrElse {
                uiState = uiState.copy(
                    isPlaylistSubscriptionLoading = false,
                    message = "歌单收藏操作失败：${it.message.orEmpty()}"
                )
                return@launch
            }
            val nextSubscriptions = uiState.subscribedPlaylistIds.toMutableSet().apply {
                if (result.success && subscribe) add(playlist.id)
                if (result.success && !subscribe) remove(playlist.id)
            }
            uiState = uiState.copy(
                subscribedPlaylistIds = nextSubscriptions,
                isPlaylistSubscriptionLoading = false,
                message = result.message
            )
            if (result.success) {
                refreshLibrary(force = true)
            }
        }
    }

    fun refreshLibrary(force: Boolean = true) {
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(
                dailyDiscovery = null,
                personalFm = null,
                podcasts = null,
                cloudDrive = null,
                likedSongs = null,
                likedSongIds = emptySet(),
                songLikeLoadingIds = emptySet(),
                userPlaylists = emptyList(),
                subscribedPlaylistIds = emptySet(),
                isLibraryLoading = false
            )
            return
        }
        if (!force && !shouldRefreshLibrary()) return
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            uiState = uiState.copy(isLibraryLoading = true, message = null)
            val userId = accountState.loginState.user?.userId ?: 0L
            val dailyDeferred = async { runCatching { musicRepository.neteaseDailyDiscovery() }.getOrNull() }
            val fmDeferred = async { runCatching { musicRepository.neteasePersonalFm() }.getOrNull() }
            val recommendedDeferred = async { runCatching { musicRepository.neteaseRecommendedPlaylists() }.getOrDefault(emptyList()) }

            val daily = dailyDeferred.await()
            val fm = fmDeferred.await()
            val recommended = recommendedDeferred.await()
            uiState = uiState.copy(
                dailyDiscovery = daily,
                personalFm = fm,
                recommendedPlaylists = recommended
            )

            val podcastsDeferred = async { runCatching { musicRepository.neteasePodcasts() }.getOrNull() }
            val cloudDeferred = async {
                if (userId == 0L) null else runCatching { musicRepository.neteaseCloudSongs() }.getOrNull()
            }
            val likedDeferred = async {
                if (userId == 0L) null else runCatching { musicRepository.neteaseLikedSongs(userId) }.getOrNull()
            }
            val likedSongIdsDeferred = async {
                if (userId == 0L) emptySet() else runCatching { musicRepository.neteaseLikedSongIds(userId) }.getOrDefault(emptySet())
            }
            val userPlaylistsDeferred = async {
                if (userId == 0L) emptyList() else runCatching { musicRepository.neteaseUserPlaylists(userId) }.getOrDefault(emptyList())
            }
            val userPlaylists = userPlaylistsDeferred.await()
            uiState = uiState.copy(
                podcasts = podcastsDeferred.await(),
                cloudDrive = cloudDeferred.await(),
                likedSongs = likedDeferred.await(),
                likedSongIds = likedSongIdsDeferred.await(),
                userPlaylists = userPlaylists,
                subscribedPlaylistIds = userPlaylists.mapTo(linkedSetOf()) { it.id },
                isLibraryLoading = false
            )
            lastLibraryRefreshAt = SystemClock.elapsedRealtime()
        }
    }

    fun refreshHome() {
        loadFeatured()
        refreshLibrary(force = true)
    }

    fun updateSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
        searchJob?.cancel()
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            uiState = uiState.copy(
                searchResults = emptyList(),
                searchPlaylists = emptyList(),
                searchAlbums = emptyList(),
                searchArtists = emptyList(),
                searchHasMoreSongs = false,
                searchHasMorePlaylists = false,
                searchHasMoreAlbums = false,
                searchHasMoreArtists = false,
                isLoadingMoreSearch = false,
                isSearching = false,
                message = null
            )
            return
        }
        searchJob = viewModelScope.launch {
            delay(80)
            performSearch(cleanQuery)
        }
    }

    fun updatePhone(phone: String) {
        accountState = accountState.copy(phone = phone.filter { it.isDigit() }.take(11))
    }

    fun updateCaptcha(captcha: String) {
        accountState = accountState.copy(captcha = captcha.filter { it.isDigit() }.take(6))
    }

    fun sendSmsCode() {
        val phone = accountState.phone
        if (accountState.sendingCode) return
        if (accountState.smsCooldownSeconds > 0) {
            accountState = accountState.copy(
                loginState = accountState.loginState.copy(message = "验证码已发送，${accountState.smsCooldownSeconds} 秒后可重发")
            )
            return
        }
        if (phone.length != 11) {
            accountState = accountState.copy(
                loginState = accountState.loginState.copy(message = "请输入 11 位手机号")
            )
            return
        }
        viewModelScope.launch {
            accountState = accountState.copy(
                sendingCode = true,
                loginState = accountState.loginState.copy(message = "正在发送验证码...")
            )
            val result = runCatching {
                accountRepository.sendNeteaseSmsCode(phone)
            }
            accountState = accountState.copy(
                sendingCode = false,
                loginState = accountState.loginState.copy(
                    message = result.fold(
                        onSuccess = { it.message },
                        onFailure = { neteaseFailureMessage("验证码发送失败", it) }
                    )
                )
            )
            result.getOrNull()?.takeIf { it.success }?.let { startSmsCooldown() }
        }
    }

    fun loginNetease() {
        val phone = accountState.phone
        val captcha = accountState.captcha
        if (phone.length != 11 || captcha.length < 4) {
            accountState = accountState.copy(
                loginState = accountState.loginState.copy(message = "请输入手机号和验证码")
            )
            return
        }
        viewModelScope.launch {
            accountState = accountState.copy(
                loggingIn = true,
                loginState = accountState.loginState.copy(message = "正在登录网易云音乐...")
            )
            val state = runCatching {
                accountRepository.loginNeteaseBySms(phone, captcha)
            }.getOrElse {
                NeteaseLoginState(false, null, neteaseFailureMessage("登录失败", it))
            }
            accountState = accountState.copy(loggingIn = false, loginState = state)
            if (state.loggedIn) {
                loadFeatured()
                refreshLibrary(force = true)
            }
        }
    }

    fun refreshLoginState() {
        viewModelScope.launch {
            val state = runCatching { accountRepository.refreshNeteaseLogin() }.getOrElse {
                NeteaseLoginState(false, null, "尚未登录网易云音乐")
            }
            accountState = accountState.copy(loginState = state)
            if (!state.loggedIn) {
                clearAccountLibrary()
                return@launch
            }
            if (uiState.selectedTab == AppTab.Account || shouldRefreshLibrary()) {
                refreshLibrary(force = false)
            }
        }
    }

    fun logoutNetease() {
        viewModelScope.launch {
            qrLoginJob?.cancel()
            val state = accountRepository.logoutNetease()
            accountState = AccountUiState(loginState = state)
            clearAccountLibrary()
        }
    }

    fun createQrLogin() {
        qrLoginJob?.cancel()
        viewModelScope.launch {
            accountState = accountState.copy(
                qrLogin = accountState.qrLogin.copy(
                    loading = true,
                    polling = false,
                    message = "正在生成二维码"
                )
            )
            val qr = runCatching { accountRepository.createNeteaseQrLogin() }.getOrElse {
                accountState = accountState.copy(
                    qrLogin = QrLoginUiState(message = neteaseFailureMessage("二维码生成失败", it))
                )
                return@launch
            }
            accountState = accountState.copy(
                qrLogin = QrLoginUiState(
                    key = qr.key,
                    qrImg = qr.qrImg,
                    qrUrl = qr.qrUrl,
                    message = qr.message,
                    loading = false,
                    polling = true
                )
            )
            startQrLoginPolling(qr.key)
        }
    }

    private fun startQrLoginPolling(key: String) {
        qrLoginJob?.cancel()
        qrLoginJob = viewModelScope.launch {
            while (accountState.qrLogin.key == key && !accountState.loginState.loggedIn) {
                delay(2000)
                val result = runCatching { accountRepository.checkNeteaseQrLogin(key) }.getOrElse {
                    accountState = accountState.copy(
                        qrLogin = accountState.qrLogin.copy(
                            polling = false,
                            message = neteaseFailureMessage("二维码检查失败", it)
                        )
                    )
                    return@launch
                }
                val loginState = result.loginState
                accountState = accountState.copy(
                    loginState = loginState ?: accountState.loginState,
                    qrLogin = accountState.qrLogin.copy(
                        polling = result.code == 801 || result.code == 802,
                        message = result.message
                    )
                )
                when (result.code) {
                    800, 803 -> return@launch
                    8821 -> {
                        accountState = accountState.copy(
                            qrLogin = accountState.qrLogin.copy(
                                polling = false,
                                message = "网易云安全风控拦截了第三方扫码登录，请使用短信验证码登录"
                            )
                        )
                        return@launch
                    }
                }
                if (loginState?.loggedIn == true) {
                    loadFeatured()
                    refreshLibrary(force = true)
                    return@launch
                }
            }
        }
    }

    private fun neteaseFailureMessage(prefix: String, throwable: Throwable): String {
        val raw = throwable.message.orEmpty().trim()
        val hint = when {
            raw.isBlank() -> "请检查网络后重试，或者切换到另一种登录方式。"
            raw.contains("请切换其他登录方式") || raw.contains("升级新版本") ->
                "网易云暂时不接受当前扫码方式，请更新网易云音乐 App 后重试，或改用短信验证码登录。"
            raw.contains("风控") || raw.contains("安全") || raw.contains("risk", ignoreCase = true) ->
                "网易云触发了安全验证，请稍后重试，或改用短信验证码登录。"
            raw.contains("timeout", ignoreCase = true) || raw.contains("超时") ->
                "网络响应超时，请稍后重试。"
            raw.contains("502") || raw.contains("503") || raw.contains("HTTP", ignoreCase = true) ->
                "网易云服务暂时不可用，请稍后重试，或切换到短信验证码登录。"
            raw.contains("连接失败") || raw.contains("不可用") ->
                "当前网络无法连接网易云服务，请换个网络后重试。"
            else -> raw.take(80)
        }
        return "$prefix：$hint"
    }

    fun runSearch() {
        val query = uiState.searchQuery.trim()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            uiState = uiState.copy(
                searchResults = emptyList(),
                searchPlaylists = emptyList(),
                searchAlbums = emptyList(),
                searchArtists = emptyList(),
                searchHasMoreSongs = false,
                searchHasMorePlaylists = false,
                searchHasMoreAlbums = false,
                searchHasMoreArtists = false,
                isLoadingMoreSearch = false,
                isSearching = false,
                message = null
            )
            return
        }
        uiState = uiState.copy(
            isSearching = true,
            isLoadingMoreSearch = false,
            searchResults = emptyList(),
            searchPlaylists = emptyList(),
            searchAlbums = emptyList(),
            searchArtists = emptyList(),
            searchHasMoreSongs = false,
            searchHasMorePlaylists = false,
            searchHasMoreAlbums = false,
            searchHasMoreArtists = false,
            message = null
        )
        val selectedSourceId = uiState.selectedSourceId
        val sourceSettings = uiState.sourceSettings
        val sourceId = effectiveSearchSourceId(selectedSourceId, sourceSettings)
        val canSearchNeteaseCollections = sourceSettings.enabledSearchPlatformIds
            .map(::normalizeBuiltInPlatformId)
            .contains(SourcePlatformIds.NETEASE) &&
            (sourceId == SourcePlatformIds.ALL || sourceId == SourcePlatformIds.NETEASE)
        val results = coroutineScope {
            val songs = async {
                runCatching {
                    musicRepository.searchSongs(sourceId, query, SEARCH_SONG_PAGE_SIZE, 0, sourceSettings)
                }.getOrDefault(emptyList())
            }
            val playlists = async {
                if (canSearchNeteaseCollections) {
                    runCatching {
                        musicRepository.searchPlaylists(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            val albums = async {
                if (canSearchNeteaseCollections) {
                    runCatching {
                        musicRepository.searchAlbums(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            val artists = async {
                if (canSearchNeteaseCollections) {
                    runCatching {
                        musicRepository.searchArtists(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            SearchInitialResults(
                songs = songs.await(),
                playlists = playlists.await(),
                albums = albums.await(),
                artists = artists.await()
            )
        }
        if (selectedSourceId == uiState.selectedSourceId && query == uiState.searchQuery.trim()) {
            uiState = uiState.copy(
                searchResults = results.songs,
                searchPlaylists = results.playlists,
                searchAlbums = results.albums,
                searchArtists = results.artists,
                searchHasMoreSongs = results.songs.size >= SEARCH_SONG_PAGE_SIZE,
                searchHasMorePlaylists = results.playlists.size >= SEARCH_COLLECTION_PAGE_SIZE,
                searchHasMoreAlbums = results.albums.size >= SEARCH_COLLECTION_PAGE_SIZE,
                searchHasMoreArtists = results.artists.size >= SEARCH_COLLECTION_PAGE_SIZE,
                isSearching = false,
                message = if (
                    results.songs.isEmpty() &&
                    results.playlists.isEmpty() &&
                    results.albums.isEmpty() &&
                    results.artists.isEmpty()
                ) {
                    "没有找到相关内容"
                } else {
                    null
                }
            )
        }
    }

    fun loadMoreSearchResults(kind: SearchResultKind) {
        val query = uiState.searchQuery.trim()
        if (query.isBlank() || uiState.isSearching || uiState.isLoadingMoreSearch) return
        val hasMore = when (kind) {
            SearchResultKind.Songs -> uiState.searchHasMoreSongs
            SearchResultKind.Playlists -> uiState.searchHasMorePlaylists
            SearchResultKind.Albums -> uiState.searchHasMoreAlbums
            SearchResultKind.Artists -> uiState.searchHasMoreArtists
        }
        if (!hasMore) return
        val selectedSourceId = uiState.selectedSourceId
        val sourceSettings = uiState.sourceSettings
        val sourceId = effectiveSearchSourceId(selectedSourceId, sourceSettings)
        val canSearchNeteaseCollections = sourceSettings.enabledSearchPlatformIds
            .map(::normalizeBuiltInPlatformId)
            .contains(SourcePlatformIds.NETEASE) &&
            (sourceId == SourcePlatformIds.ALL || sourceId == SourcePlatformIds.NETEASE)
        val offset = when (kind) {
            SearchResultKind.Songs -> uiState.searchResults.size
            SearchResultKind.Playlists -> uiState.searchPlaylists.size
            SearchResultKind.Albums -> uiState.searchAlbums.size
            SearchResultKind.Artists -> uiState.searchArtists.size
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMoreSearch = true, message = null)
            val nextSongs: List<Song>
            val nextPlaylists: List<MusicPlaylist>
            when (kind) {
                SearchResultKind.Songs -> {
                    nextSongs = runCatching {
                        musicRepository.searchSongs(sourceId, query, SEARCH_SONG_PAGE_SIZE, offset, sourceSettings)
                    }.getOrDefault(emptyList())
                    nextPlaylists = emptyList()
                }
                SearchResultKind.Playlists -> {
                    nextSongs = emptyList()
                    nextPlaylists = if (canSearchNeteaseCollections) {
                        runCatching {
                            musicRepository.searchPlaylists(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                SearchResultKind.Albums -> {
                    nextSongs = emptyList()
                    nextPlaylists = if (canSearchNeteaseCollections) {
                        runCatching {
                            musicRepository.searchAlbums(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                SearchResultKind.Artists -> {
                    nextSongs = emptyList()
                    nextPlaylists = if (canSearchNeteaseCollections) {
                        runCatching {
                            musicRepository.searchArtists(SourcePlatformIds.NETEASE, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
            }
            if (selectedSourceId != uiState.selectedSourceId || query != uiState.searchQuery.trim()) {
                uiState = uiState.copy(isLoadingMoreSearch = false)
                return@launch
            }
            uiState = when (kind) {
                SearchResultKind.Songs -> {
                    val combined = (uiState.searchResults + nextSongs).distinctBy { "${it.sourceId}:${it.id}" }
                    uiState.copy(
                        searchResults = combined,
                        searchHasMoreSongs = nextSongs.size >= SEARCH_SONG_PAGE_SIZE,
                        isLoadingMoreSearch = false
                    )
                }
                SearchResultKind.Playlists -> {
                    val combined = (uiState.searchPlaylists + nextPlaylists).distinctBy { it.id }
                    uiState.copy(
                        searchPlaylists = combined,
                        searchHasMorePlaylists = nextPlaylists.size >= SEARCH_COLLECTION_PAGE_SIZE,
                        isLoadingMoreSearch = false
                    )
                }
                SearchResultKind.Albums -> {
                    val combined = (uiState.searchAlbums + nextPlaylists).distinctBy { it.id }
                    uiState.copy(
                        searchAlbums = combined,
                        searchHasMoreAlbums = nextPlaylists.size >= SEARCH_COLLECTION_PAGE_SIZE,
                        isLoadingMoreSearch = false
                    )
                }
                SearchResultKind.Artists -> {
                    val combined = (uiState.searchArtists + nextPlaylists).distinctBy { it.id }
                    uiState.copy(
                        searchArtists = combined,
                        searchHasMoreArtists = nextPlaylists.size >= SEARCH_COLLECTION_PAGE_SIZE,
                        isLoadingMoreSearch = false
                    )
                }
            }
        }
    }

    private fun loadPlaylistComments(playlist: MusicPlaylist, force: Boolean) {
        if (!force && uiState.playlistComments.isNotEmpty()) return
        viewModelScope.launch {
            val sort = uiState.playlistCommentSort
            uiState = uiState.copy(
                isPlaylistCommentsLoading = true,
                playlistCommentsMessage = null
            )
            val bundle = runCatching {
                musicRepository.neteasePlaylistComments(playlist, sort)
            }.getOrElse {
                if (uiState.selectedPlaylist?.id == playlist.id) {
                    uiState = uiState.copy(
                        isPlaylistCommentsLoading = false,
                        playlistCommentsMessage = "评论加载失败：${it.message.orEmpty()}"
                    )
                }
                return@launch
            }
            if (uiState.selectedPlaylist?.id == playlist.id) {
                uiState = uiState.copy(
                    playlistComments = bundle.comments,
                    playlistCommentCount = bundle.totalCount,
                    isPlaylistCommentsLoading = false,
                    playlistCommentsMessage = if (bundle.comments.isEmpty()) {
                        if (sort == PlaylistCommentSort.Hot) "暂时还没有热门评论" else "暂时还没有最新评论"
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun loadPlayerComments(song: Song, force: Boolean) {
        val commentSongId = song.playerCommentCacheKey()
        if (!force && loadedPlayerCommentSongId == commentSongId && uiState.playerComments.isNotEmpty()) return
        playerCommentsJob?.cancel()
        playerCommentsJob = viewModelScope.launch {
            val sort = uiState.playerCommentSort
            loadedPlayerCommentSongId = commentSongId
            uiState = uiState.copy(
                isPlayerCommentsLoading = true,
                playerComments = if (force) emptyList() else uiState.playerComments,
                playerCommentsMessage = null
            )
            val bundle = runCatching {
                musicRepository.songComments(song, sort, uiState.sourceSettings)
            }.getOrElse {
                if (playbackState.currentSong?.playerCommentCacheKey() == commentSongId) {
                    uiState = uiState.copy(
                        isPlayerCommentsLoading = false,
                        playerCommentsMessage = "歌曲评论加载失败：${it.message.orEmpty()}"
                    )
                }
                return@launch
            } ?: run {
                if (playbackState.currentSong?.playerCommentCacheKey() == commentSongId) {
                    uiState = uiState.copy(
                        playerComments = emptyList(),
                        playerCommentCount = 0,
                        isPlayerCommentsLoading = false,
                        playerCommentsMessage = "当前歌曲没有启用可用的评论来源"
                    )
                }
                return@launch
            }
            if (playbackState.currentSong?.playerCommentCacheKey() == commentSongId) {
                uiState = uiState.copy(
                    playerComments = bundle.comments,
                    playerCommentCount = bundle.totalCount,
                    isPlayerCommentsLoading = false,
                    playerCommentsMessage = if (bundle.comments.isEmpty()) {
                        if (sort == PlaylistCommentSort.Hot) "暂时还没有热门评论" else "暂时还没有最新评论"
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun startSmsCooldown(seconds: Int = 45) {
        smsCooldownJob?.cancel()
        smsCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                accountState = accountState.copy(smsCooldownSeconds = remaining)
                delay(1000)
            }
        }
    }

    private fun syncLyricsWithPlayback() {
        val song = playbackState.currentSong
        if (song == null) {
            loadedLyricSongId = null
            loadedPlayerCommentSongId = null
            lyricJob?.cancel()
            playerCommentsJob?.cancel()
            if (
                uiState.lyrics.isNotEmpty() ||
                uiState.isLyricLoading ||
                uiState.playerComments.isNotEmpty() ||
                uiState.isPlayerCommentsLoading ||
                uiState.playerCommentsMessage != null
            ) {
                uiState = uiState.copy(
                    lyrics = emptyList(),
                    isLyricLoading = false,
                    playerComments = emptyList(),
                    playerCommentCount = 0,
                    isPlayerCommentsLoading = false,
                    playerCommentsMessage = null
                )
            }
            return
        }
        if (loadedLyricSongId != song.id) {
            loadedLyricSongId = song.id
            lyricJob?.cancel()
            uiState = uiState.copy(
                lyrics = emptyList(),
                isLyricLoading = true
            )
            lyricJob = viewModelScope.launch {
                val lines = loadLyricsWithRetry(song)
                if (playbackState.currentSong?.id == song.id) {
                    uiState = uiState.copy(lyrics = lines, isLyricLoading = false)
                }
            }
        }
        if (loadedPlayerCommentSongId != song.playerCommentCacheKey() && playerCommentsJob?.isActive != true) {
            uiState = uiState.copy(
                playerComments = emptyList(),
                playerCommentCount = 0,
                isPlayerCommentsLoading = false,
                playerCommentsMessage = null
            )
            loadPlayerComments(song, force = true)
        }
    }

    private suspend fun loadLyricsWithRetry(song: Song): List<LyricLine> {
        repeat(LYRIC_LOAD_RETRY_COUNT) { attempt ->
            val lines = runCatching { musicRepository.lyrics(song) }.getOrDefault(emptyList())
            if (lines.hasRealLyrics()) return lines
            if (playbackState.currentSong?.id != song.id) return emptyList()
            delay(360L + attempt * 420L)
        }
        return runCatching { musicRepository.lyrics(song) }.getOrDefault(emptyList()).takeIf { it.hasRealLyrics() }
            ?: listOf(LyricLine(0L, "暂时没有歌词"))
    }

    private fun List<LyricLine>.hasRealLyrics(): Boolean =
        any { line ->
            val text = line.text.trim()
            text.isNotBlank() && text != "歌词加载中..." && text != "暂时没有歌词"
        }

    private fun syncStatusBarLyricWithPlayback() {
        if (!uiState.playbackSettings.statusBarLyricEnabled) {
            player.setNotificationLyric(enabled = false, lyricText = null)
            return
        }
        val song = playbackState.currentSong ?: run {
            player.setNotificationLyric(enabled = true, lyricText = null)
            return
        }
        val activeLyric = uiState.lyrics
            .lastOrNull { it.timeMs <= playbackState.positionMs }
            ?.text
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it != "歌词加载中..." && it != "暂时没有歌词" }
        player.setNotificationLyric(
            enabled = true,
            lyricText = activeLyric ?: song.artist.ifBlank { "SiListen" }
        )
    }

    private fun syncSleepTimerWithPlayback() {
        val timer = uiState.sleepTimer
        if (!timer.pendingStopAfterCurrentSong) return
        val current = playbackState.currentSong
        val duration = playbackState.durationMs.takeIf { it > 0L } ?: current?.durationMs ?: 0L
        val reachedSongEnd = duration > 0L &&
            playbackState.positionMs >= (duration - 1_200L).coerceAtLeast(0L) &&
            !playbackState.isPlaying &&
            !playbackState.isPreparing
        if (current == null || reachedSongEnd) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            player.setStopAfterCurrentSongForTimer(false)
            uiState = uiState.copy(
                sleepTimer = SleepTimerState(),
                message = "已在当前歌曲结束后停止播放"
            )
        }
    }

    private fun syncRecentPlayback() {
        val song = playbackState.currentSong ?: run {
            lastRecentSongId = null
            return
        }
        if (song.id == lastRecentSongId) return
        lastRecentSongId = song.id
        addRecentPlayedSong(song)
    }

    private fun addRecentPlayedSong(song: Song) {
        if (song.id.isBlank() || song.title.isBlank()) return
        val nextSongs = buildList {
            add(song)
            addAll(uiState.recentPlayedSongs.filterNot { it.id == song.id })
        }.take(RecentPlaybackStore.MAX_ITEMS)
        if (nextSongs == uiState.recentPlayedSongs) return
        uiState = uiState.copy(recentPlayedSongs = nextSongs)
        recentPlaybackStore.save(nextSongs)
    }

    private fun loadThemeSettings(): ThemeSettingsState {
        val uiMode = runCatching {
            ThemeUiModeOption.valueOf(
                themePreferences.getString("ui_mode", ThemeUiModeOption.Miuix.name).orEmpty()
            )
        }.getOrDefault(ThemeUiModeOption.Miuix)
        val mode = runCatching {
            ThemeModeOption.valueOf(themePreferences.getString("mode", ThemeModeOption.Light.name).orEmpty())
        }.getOrDefault(ThemeModeOption.Light)
        val accent = runCatching {
            ThemeAccentOption.valueOf(
                themePreferences.getString("accent", ThemeAccentOption.Emerald.name).orEmpty()
            )
        }.getOrDefault(ThemeAccentOption.Emerald)
        val paletteStyle = runCatching {
            ThemePaletteStyleOption.valueOf(
                themePreferences.getString("palette_style", ThemePaletteStyleOption.TonalSpot.name).orEmpty()
            )
        }.getOrDefault(ThemePaletteStyleOption.TonalSpot)
        val colorSpec = runCatching {
            ThemeColorSpecOption.valueOf(
                themePreferences.getString("color_spec", ThemeColorSpecOption.Default.name).orEmpty()
            )
        }.getOrDefault(ThemeColorSpecOption.Default)
        return ThemeSettingsState(
            uiMode = uiMode,
            mode = mode,
            accent = accent,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            monetEnabled = themePreferences.getBoolean("monet", false),
            blurEnabled = themePreferences.getBoolean("blur", true),
            floatingBottomBarEnabled = themePreferences.getBoolean("floating_bar", true),
            floatingBottomBarBlurEnabled = themePreferences.getBoolean("floating_bar_blur", true),
            predictiveBackEnabled = themePreferences.getBoolean("predictive_back", false),
            uiScale = themePreferences.getFloat("ui_scale", 1f).coerceIn(0.85f, 1.15f)
        )
    }

    private fun saveThemeSettings(settings: ThemeSettingsState) {
        themePreferences.edit()
            .putString("ui_mode", settings.uiMode.name)
            .putString("mode", settings.mode.name)
            .putString("accent", settings.accent.name)
            .putString("palette_style", settings.paletteStyle.name)
            .putString("color_spec", settings.colorSpec.name)
            .putBoolean("monet", settings.monetEnabled)
            .putBoolean("blur", settings.blurEnabled)
            .putBoolean("floating_bar", settings.floatingBottomBarEnabled)
            .putBoolean("floating_bar_blur", settings.floatingBottomBarBlurEnabled)
            .putBoolean("predictive_back", settings.predictiveBackEnabled)
            .putFloat("ui_scale", settings.uiScale)
            .apply()
    }

    private fun loadPlaybackSettings(): PlaybackSettingsState {
        val quality = runCatching {
            PlaybackQuality.valueOf(playbackPreferences.getString("quality", PlaybackQuality.ExHigh.name).orEmpty())
        }.getOrDefault(PlaybackQuality.ExHigh)
        val lyricDisplayMode = runCatching {
            LyricDisplayMode.valueOf(
                playbackPreferences.getString("lyric_display_mode", LyricDisplayMode.Glass.name).orEmpty()
            )
        }.getOrDefault(LyricDisplayMode.Glass)
        val playbackMode = runCatching {
            PlaybackMode.valueOf(
                playbackPreferences.getString("playback_mode", PlaybackMode.Order.name).orEmpty()
            )
        }.getOrDefault(PlaybackMode.Order)
        return PlaybackSettingsState(
            quality = quality,
            lyricDisplayMode = lyricDisplayMode,
            playbackMode = playbackMode,
            statusBarLyricEnabled = playbackPreferences.getBoolean("status_bar_lyric", false)
        )
    }

    private fun savePlaybackSettings(settings: PlaybackSettingsState) {
        playbackPreferences.edit()
            .putString("quality", settings.quality.name)
            .putString("lyric_display_mode", settings.lyricDisplayMode.name)
            .putString("playback_mode", settings.playbackMode.name)
            .putBoolean("status_bar_lyric", settings.statusBarLyricEnabled)
            .apply()
    }

    private companion object {
        const val SEARCH_SONG_PAGE_SIZE = 30
        const val SEARCH_COLLECTION_PAGE_SIZE = 20
        const val ARTIST_SONG_PAGE_SIZE = 50
        const val LYRIC_LOAD_RETRY_COUNT = 2

        val themePreferenceKeys = setOf(
            "mode",
            "accent",
            "palette_style",
            "color_spec",
            "monet",
            "blur",
            "floating_bar",
            "floating_bar_blur",
            "predictive_back",
            "ui_scale",
            "ui_mode"
        )
    }

    private fun currentPlaybackQuality(): PlaybackQuality = runCatching {
        PlaybackQuality.valueOf(playbackPreferences.getString("quality", uiState.playbackSettings.quality.name).orEmpty())
    }.getOrDefault(PlaybackQuality.ExHigh)

    private fun effectiveSearchSourceId(
        selectedSourceId: String,
        sourceSettings: SourceSettingsState = uiState.sourceSettings
    ): String =
        when {
            selectedSourceId == SourcePlatformIds.LOCAL || selectedSourceId == SourcePlatformIds.DEMO ->
                selectedSourceId
            selectedSourceId == SourcePlatformIds.NETEASE &&
                SourcePlatformIds.NETEASE !in sourceSettings.enabledSearchPlatformIds.map(::normalizeBuiltInPlatformId) ->
                SourcePlatformIds.ALL
            else ->
                selectedSourceId
        }

    private fun rerunSearchIfNeeded() {
        val query = uiState.searchQuery.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(query)
        }
    }

    fun playPlaylist(playlist: MusicPlaylist, startIndex: Int = 0) {
        viewModelScope.launch {
            val needsDetail = playlist.songs.isEmpty() &&
                (playlist.id.startsWith("netease-playlist-") ||
                    playlist.id.startsWith("netease-album-") ||
                    playlist.id.startsWith("netease-artist-"))
            val playable = if (needsDetail) {
                runCatching { musicRepository.neteasePlaylistDetail(playlist) }.getOrDefault(playlist)
            } else {
                playlist
            }
            if (playable.songs.isEmpty()) {
                uiState = uiState.copy(message = "${playable.title} 暂时没有可播放歌曲")
                return@launch
            }
            player.playQueue(playable.songs, startIndex, playbackResolver())
        }
    }

    private fun Song.asNeteaseIdentitySong(): Song? {
        val neteaseId = neteaseIdentityId() ?: return null
        return copy(
            id = neteaseId,
            sourceId = SourcePlatformIds.NETEASE,
            canonicalSourceId = SourcePlatformIds.NETEASE,
            canonicalSongId = neteaseId,
            providerIds = providerIds + (SourcePlatformIds.NETEASE to neteaseId)
        )
    }

    private fun Song.playerCommentCacheKey(): String =
        buildString {
            append(sourceId)
            append(':')
            append(id)
            canonicalSourceId?.let {
                append(":canonical=")
                append(it)
                append(':')
                append(canonicalSongId.orEmpty())
            }
            providerIds.toSortedMap().forEach { (key, value) ->
                append(':')
                append(key)
                append('=')
                append(value)
            }
        }

    private fun playbackQueueFor(song: Song): List<Song> =
        uiState.searchResults.takeIf { results -> results.any { it.id == song.id } }
            ?: uiState.featured.firstOrNull { it.songs.any { item -> item.id == song.id } }?.songs
            ?: uiState.dailyDiscovery?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.personalFm?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.podcasts?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.cloudDrive?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.likedSongs?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.artistSongsPage.songs.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.selectedPlaylist?.songs?.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: uiState.localSongs.takeIf { songs -> songs.any { item -> item.id == song.id } }
            ?: listOf(song)

    fun playSong(song: Song) {
        val queue = playbackQueueFor(song)
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        viewModelScope.launch {
            player.playQueue(queue, index, playbackResolver())
        }
    }

    fun playSongNext(song: Song) {
        viewModelScope.launch {
            val queued = player.queueNext(song, playbackResolver())
            uiState = uiState.copy(
                message = if (queued) "已添加到下一首播放" else "暂时无法添加到下一首播放"
            )
        }
    }

    fun playSongAndOpenComments(song: Song) {
        val queue = playbackQueueFor(song)
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        viewModelScope.launch {
            player.playQueue(queue, index, playbackResolver())
            playerSheetPanel = PlayerSheetPanel.Comments
            isPlayerSheetVisible = true
        }
    }

    fun isSongLiked(song: Song): Boolean = song.neteaseIdentityId()?.let { it in uiState.likedSongIds } == true

    fun isSongLikeLoading(song: Song): Boolean {
        val neteaseId = song.neteaseIdentityId()
        val fallbackKey = "${song.sourceId}:${song.id}"
        return (neteaseId != null && neteaseId in uiState.songLikeLoadingIds) ||
            fallbackKey in uiState.songLikeLoadingIds
    }

    fun toggleSongLike(song: Song) {
        setSongLike(song = song, shouldLike = !isSongLiked(song))
    }

    fun addSongToLiked(song: Song) {
        if (isSongLiked(song)) {
            uiState = uiState.copy(
                likePrompt = LikePromptState(song),
                message = "已添加到我喜欢的音乐"
            )
            return
        }
        setSongLike(song = song, shouldLike = true)
    }

    fun addCurrentSongToLiked() {
        val song = playbackState.currentSong
        if (song == null) {
            uiState = uiState.copy(message = "当前没有正在播放的歌曲")
            return
        }
        addSongToLiked(song)
    }

    fun toggleCurrentSongLike() {
        val song = playbackState.currentSong
        if (song == null) {
            uiState = uiState.copy(message = "当前没有正在播放的歌曲")
            return
        }
        toggleSongLike(song)
    }

    fun dismissLikePrompt(songId: String? = null) {
        val prompt = uiState.likePrompt ?: return
        if (songId == null || prompt.song.id == songId) {
            uiState = uiState.copy(likePrompt = null)
        }
    }

    fun openAddToPlaylistChooser(song: Song? = uiState.likePrompt?.song ?: playbackState.currentSong) {
        val target = song ?: run {
            uiState = uiState.copy(message = "\u5f53\u524d\u6ca1\u6709\u53ef\u52a0\u5165\u6b4c\u5355\u7684\u6b4c\u66f2")
            return
        }
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "\u8bf7\u5148\u767b\u5f55\u7f51\u6613\u4e91\u8d26\u53f7\u540e\u518d\u52a0\u5165\u6b4c\u5355")
            return
        }
        val neteaseSong = target.asNeteaseIdentitySong()
        if (neteaseSong == null) {
            uiState = uiState.copy(message = "正在匹配网易云版本...")
            viewModelScope.launch {
                val matched = runCatching { musicRepository.matchNeteaseIdentity(target)?.asNeteaseIdentitySong() }
                    .getOrNull()
                uiState = if (matched == null) {
                    uiState.copy(message = "\u5f53\u524d\u6b4c\u66f2\u672a\u5339\u914d\u5230\u7f51\u6613\u4e91\u7248\u672c\uff0c\u6682\u65f6\u4e0d\u80fd\u52a0\u5165\u7f51\u6613\u4e91\u6b4c\u5355")
                } else {
                    uiState.copy(playlistChooserSong = matched, message = null)
                }
            }
            return
        }
        uiState = uiState.copy(playlistChooserSong = neteaseSong)
    }

    fun closeAddToPlaylistChooser() {
        uiState = uiState.copy(
            playlistChooserSong = null,
            playlistAddLoadingIds = emptySet()
        )
    }

    fun isPlaylistAddLoading(playlist: MusicPlaylist): Boolean =
        playlist.id in uiState.playlistAddLoadingIds

    fun addPlaylistChooserSongToPlaylist(playlist: MusicPlaylist) {
        val rawSong = uiState.playlistChooserSong ?: playbackState.currentSong ?: run {
            uiState = uiState.copy(message = "\u5f53\u524d\u6ca1\u6709\u53ef\u52a0\u5165\u6b4c\u5355\u7684\u6b4c\u66f2")
            return
        }
        val song = rawSong.asNeteaseIdentitySong()
        if (song == null || playlist.kind != PlaylistKind.UserPlaylist) {
            uiState = uiState.copy(message = "\u5f53\u524d\u53ea\u652f\u6301\u52a0\u5165\u7f51\u6613\u4e91\u81ea\u5efa\u6b4c\u5355")
            return
        }
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "\u8bf7\u5148\u767b\u5f55\u7f51\u6613\u4e91\u8d26\u53f7\u540e\u518d\u52a0\u5165\u6b4c\u5355")
            return
        }
        if (isPlaylistAddLoading(playlist)) return
        uiState = uiState.copy(playlistAddLoadingIds = uiState.playlistAddLoadingIds + playlist.id)
        viewModelScope.launch {
            val result = runCatching {
                musicRepository.neteaseAddSongToPlaylist(song, playlist)
            }.getOrElse {
                uiState = uiState.copy(
                    playlistAddLoadingIds = uiState.playlistAddLoadingIds - playlist.id,
                    message = "\u52a0\u5165\u6b4c\u5355\u5931\u8d25\uff1a${it.message.orEmpty()}"
                )
                return@launch
            }
            if (result.success) {
                uiState = uiState.copy(
                    userPlaylists = uiState.userPlaylists.addSongToLoadedPlaylist(playlist.id, song),
                    recommendedPlaylists = uiState.recommendedPlaylists.addSongToLoadedPlaylist(playlist.id, song),
                    featured = uiState.featured.addSongToLoadedPlaylist(playlist.id, song),
                    selectedPlaylist = uiState.selectedPlaylist?.let {
                        if (it.id == playlist.id) it.withAddedSongIfLoaded(song) else it
                    },
                    playlistChooserSong = null,
                    playlistAddLoadingIds = uiState.playlistAddLoadingIds - playlist.id,
                    message = result.message
                )
            } else {
                uiState = uiState.copy(
                    playlistAddLoadingIds = uiState.playlistAddLoadingIds - playlist.id,
                    message = result.message
                )
            }
        }
    }

    private fun setSongLike(song: Song, shouldLike: Boolean) {
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "\u8bf7\u5148\u767b\u5f55\u7f51\u6613\u4e91\u8d26\u53f7\u540e\u518d\u6536\u85cf\u6b4c\u66f2")
            return
        }
        val initialNeteaseSong = song.asNeteaseIdentitySong()
        val loadingKey = initialNeteaseSong?.id ?: "${song.sourceId}:${song.id}"
        if (loadingKey in uiState.songLikeLoadingIds) return
        uiState = uiState.copy(songLikeLoadingIds = uiState.songLikeLoadingIds + loadingKey)
        viewModelScope.launch {
            val neteaseSong = initialNeteaseSong
                ?: runCatching { musicRepository.matchNeteaseIdentity(song)?.asNeteaseIdentitySong() }.getOrNull()
            if (neteaseSong == null) {
                uiState = uiState.copy(
                    songLikeLoadingIds = uiState.songLikeLoadingIds - loadingKey,
                    message = "\u5f53\u524d\u6b4c\u66f2\u672a\u5339\u914d\u5230\u7f51\u6613\u4e91\u7248\u672c\uff0c\u6682\u65f6\u4e0d\u80fd\u7ea2\u5fc3"
                )
                return@launch
            }
            val songId = neteaseSong.id
            val result = runCatching {
                musicRepository.neteaseToggleSongLike(neteaseSong, shouldLike)
            }.getOrElse {
                uiState = uiState.copy(
                    songLikeLoadingIds = uiState.songLikeLoadingIds - loadingKey - songId,
                    message = "\u6b4c\u66f2\u7ea2\u5fc3\u64cd\u4f5c\u5931\u8d25\uff1a${it.message.orEmpty()}"
                )
                return@launch
            }
            val nextLikedIds = uiState.likedSongIds.toMutableSet().apply {
                if (result.success && shouldLike) add(songId)
                if (result.success && !shouldLike) remove(songId)
            }
            val nextLikedSongs = uiState.likedSongs?.let { liked ->
                val nextSongs = liked.songs.toMutableList().apply {
                    removeAll { it.neteaseIdentityId() == songId || it.id == songId }
                    if (result.success && shouldLike) add(0, neteaseSong)
                }.distinctBy { it.neteaseIdentityId() ?: it.id }
                liked.copy(
                    songs = nextSongs,
                    coverUrl = nextSongs.firstOrNull()?.coverUrl ?: liked.coverUrl,
                    subtitle = if (nextSongs.isEmpty()) liked.subtitle else "${nextSongs.size} \u9996\u559c\u6b22\u7684\u6b4c"
                )
            }
            uiState = uiState.copy(
                likedSongIds = nextLikedIds,
                likedSongs = nextLikedSongs,
                searchResults = uiState.searchResults.replaceSongIdentityInSongs(song, neteaseSong),
                featured = uiState.featured.replaceSongIdentityInPlaylists(song, neteaseSong),
                dailyDiscovery = uiState.dailyDiscovery?.replaceSongIdentityInPlaylist(song, neteaseSong),
                recommendedPlaylists = uiState.recommendedPlaylists.replaceSongIdentityInPlaylists(song, neteaseSong),
                personalFm = uiState.personalFm?.replaceSongIdentityInPlaylist(song, neteaseSong),
                podcasts = uiState.podcasts?.replaceSongIdentityInPlaylist(song, neteaseSong),
                cloudDrive = uiState.cloudDrive?.replaceSongIdentityInPlaylist(song, neteaseSong),
                userPlaylists = uiState.userPlaylists.replaceSongIdentityInPlaylists(song, neteaseSong),
                selectedPlaylist = uiState.selectedPlaylist?.replaceSongIdentityInPlaylist(song, neteaseSong),
                artistSongsPage = uiState.artistSongsPage.copy(
                    songs = uiState.artistSongsPage.songs.replaceSongIdentityInSongs(song, neteaseSong)
                ),
                songLikeLoadingIds = uiState.songLikeLoadingIds - loadingKey - songId,
                likePrompt = if (result.success && shouldLike) {
                    LikePromptState(neteaseSong, "\u5df2\u6dfb\u52a0\u5230\u6211\u559c\u6b22\u7684\u97f3\u4e50", showAddToPlaylistAction = true)
                } else if (result.success && !shouldLike) {
                    LikePromptState(neteaseSong, "\u5df2\u4ece\u6211\u559c\u6b22\u7684\u97f3\u4e50\u79fb\u9664", showAddToPlaylistAction = false)
                } else {
                    uiState.likePrompt
                },
                message = if (result.success) null else result.message
            )
        }
    }

    fun togglePlayback() = player.toggle()

    fun next() = player.next()

    fun previous() = player.previous()

    fun playQueueIndex(index: Int) = player.playAt(index)

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun startSleepTimer(durationMinutes: Int, waitUntilSongEnds: Boolean) {
        val safeMinutes = durationMinutes.coerceIn(1, 360)
        val totalMs = safeMinutes * 60_000L
        sleepTimerJob?.cancel()
        player.setStopAfterCurrentSongForTimer(false)
        uiState = uiState.copy(
            sleepTimer = SleepTimerState(
                active = true,
                durationMinutes = safeMinutes,
                remainingMs = totalMs,
                waitUntilSongEnds = waitUntilSongEnds
            ),
            message = "已设置 ${safeMinutes} 分钟后停止播放"
        )
        sleepTimerJob = viewModelScope.launch {
            val endAt = SystemClock.elapsedRealtime() + totalMs
            while (true) {
                val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                uiState = uiState.copy(
                    sleepTimer = uiState.sleepTimer.copy(remainingMs = remaining)
                )
                if (remaining <= 0L) break
                delay(1_000)
            }
            if (waitUntilSongEnds && playbackState.currentSong != null && playbackState.isPlaying) {
                player.setStopAfterCurrentSongForTimer(true)
                uiState = uiState.copy(
                    sleepTimer = uiState.sleepTimer.copy(
                        active = true,
                        remainingMs = 0L,
                        pendingStopAfterCurrentSong = true
                    ),
                    message = "将在当前歌曲播放完后停止"
                )
            } else {
                player.pause()
                player.setStopAfterCurrentSongForTimer(false)
                uiState = uiState.copy(
                    sleepTimer = SleepTimerState(),
                    message = "已定时停止播放"
                )
            }
        }
    }

    fun cancelSleepTimer(showMessage: Boolean = true) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player.setStopAfterCurrentSongForTimer(false)
        uiState = uiState.copy(
            sleepTimer = SleepTimerState(),
            message = if (showMessage) "已取消定时停止" else uiState.message
        )
    }

    fun openPlayerSheet(panel: PlayerSheetPanel = PlayerSheetPanel.Detail) {
        if (playbackState.currentSong != null) {
            playerSheetPanel = panel
            isPlayerSheetVisible = true
        }
    }

    fun closePlayerSheet() {
        isPlayerSheetVisible = false
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            val sourceId = uiState.selectedSourceId
            uiState = uiState.copy(isLoading = true, message = null)
            val featured = runCatching { musicRepository.featured(sourceId) }.getOrElse {
                uiState = uiState.copy(message = "推荐内容暂时不可用")
                emptyList()
            }
            if (sourceId == uiState.selectedSourceId) {
                uiState = uiState.copy(featured = featured, isLoading = false)
            }
        }
    }

    private fun shouldRefreshLibrary(): Boolean {
        val now = SystemClock.elapsedRealtime()
        return now - lastLibraryRefreshAt > 60_000L ||
            uiState.dailyDiscovery == null ||
            uiState.personalFm == null ||
            uiState.userPlaylists.isEmpty()
    }

    fun scanLocalMusic() {
        val context = getApplication<Application>()
        if (!hasAudioPermission(context)) {
            uiState = uiState.copy(localMusicMessage = "需要授予音频读取权限后才能扫描本地音乐")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLibraryLoading = true, localMusicMessage = "正在扫描本地音乐...")
            val songs = runCatching { queryLocalSongs(context) }.getOrDefault(emptyList())
            uiState = uiState.copy(
                localSongs = songs,
                isLibraryLoading = false,
                localMusicMessage = if (songs.isEmpty()) "没有扫描到本地音乐" else "已扫描 ${songs.size} 首本地歌曲"
            )
        }
    }

    private fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun queryLocalSongs(context: Context): List<Song> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        return buildList {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext() && size < 300) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    add(
                        Song(
                            id = "local-$id",
                            title = cursor.getString(titleColumn).orEmpty().ifBlank { "本地歌曲" },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "未知歌手" },
                            album = cursor.getString(albumColumn).orEmpty().ifBlank { "本地音乐" },
                            coverUrl = "",
                            durationMs = cursor.getLong(durationColumn),
                            sourceId = "local",
                            streamHint = uri.toString()
                        )
                    )
                }
            }
        }
    }

    private fun List<MusicPlaylist>.replacePlaylist(detail: MusicPlaylist): List<MusicPlaylist> =
        map { playlist -> if (playlist.id == detail.id) detail else playlist }

    private fun List<Song>.replaceSongIdentityInSongs(original: Song, resolved: Song): List<Song> =
        map { song ->
            if (song.sourceId == original.sourceId && song.id == original.id) {
                resolved
            } else {
                song
            }
        }

    private fun MusicPlaylist.replaceSongIdentityInPlaylist(original: Song, resolved: Song): MusicPlaylist =
        copy(songs = songs.replaceSongIdentityInSongs(original, resolved))

    private fun List<MusicPlaylist>.replaceSongIdentityInPlaylists(original: Song, resolved: Song): List<MusicPlaylist> =
        map { playlist -> playlist.replaceSongIdentityInPlaylist(original, resolved) }

    private fun List<MusicPlaylist>.addSongToLoadedPlaylist(playlistId: String, song: Song): List<MusicPlaylist> =
        map { playlist -> if (playlist.id == playlistId) playlist.withAddedSongIfLoaded(song) else playlist }

    private fun MusicPlaylist.withAddedSongIfLoaded(song: Song): MusicPlaylist {
        if (songs.isEmpty()) return this
        val nextSongs = buildList {
            add(song)
            addAll(songs.filterNot { it.id == song.id })
        }
        return copy(
            songs = nextSongs,
            coverUrl = coverUrl.ifBlank { song.coverUrl },
            subtitle = "${nextSongs.size} 首歌曲"
        )
    }

    private fun isPlaylistSubscribed(playlist: MusicPlaylist): Boolean {
        return playlist.kind == PlaylistKind.LikedSongs ||
            playlist.kind == PlaylistKind.UserPlaylist ||
            uiState.subscribedPlaylistIds.contains(playlist.id)
    }

    private fun clearAccountLibrary() {
        lastLibraryRefreshAt = 0L
        uiState = uiState.copy(
            cloudDrive = null,
            likedSongs = null,
            likedSongIds = emptySet(),
            songLikeLoadingIds = emptySet(),
            likePrompt = null,
            playlistChooserSong = null,
            playlistAddLoadingIds = emptySet(),
            userPlaylists = emptyList(),
            selectedPlaylist = null,
            playlistBackStack = emptyList(),
            selectedPlaylistRoute = PlaylistRoute.Overview,
            playlistSongSearchQuery = "",
            isPlaylistDetailLoading = false,
            playlistDetailMessage = null,
            playlistCommentSort = PlaylistCommentSort.Hot,
            playlistComments = emptyList(),
            playlistCommentCount = 0,
            isPlaylistCommentsLoading = false,
            playlistCommentsMessage = null,
            subscribedPlaylistIds = emptySet(),
            isPlaylistSubscriptionLoading = false
        )
    }

    override fun onCleared() {
        themePreferences.unregisterOnSharedPreferenceChangeListener(themePreferenceListener)
        PlaybackNotificationBridge.detachLikeHandler(notificationLikeHandler)
        PlaybackNotificationBridge.detachLikeStateProvider(notificationLikeStateProvider)
        qrLoginJob?.cancel()
        searchJob?.cancel()
        smsCooldownJob?.cancel()
        lyricJob?.cancel()
        libraryJob?.cancel()
        playerCommentsJob?.cancel()
        sleepTimerJob?.cancel()
        player.setStopAfterCurrentSongForTimer(false)
        super.onCleared()
    }
}
