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
import com.silisten.app.data.repository.AccountRepository
import com.silisten.app.data.repository.MusicRepository
import com.silisten.app.data.repository.RecentPlaybackStore
import com.silisten.app.data.source.MusicSourceRegistry
import com.silisten.app.data.source.NeteaseApiClient
import com.silisten.app.data.source.NeteaseLoginState
import com.silisten.app.playback.PlaybackState
import com.silisten.app.playback.PlaybackCenter
import com.silisten.app.playback.PlaybackNotificationBridge
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
    val statusBarLyricEnabled: Boolean = false
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
    private val themePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in themePreferenceKeys) {
            val next = loadThemeSettings()
            if (next != uiState.themeSettings) {
                uiState = uiState.copy(themeSettings = next)
            }
        }
    }
    private val musicRepository = MusicRepository(
        MusicSourceRegistry.create(neteaseApiClient) { currentPlaybackQuality() }
    )
    val registry: MusicSourceRegistry = musicRepository.registry
    private val player = PlaybackCenter.controller(application)

    var uiState by mutableStateOf(
        SiListenUiState(
            selectedSourceId = registry.default().info.id,
            themeSettings = loadThemeSettings(),
            playbackSettings = loadPlaybackSettings(),
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
    private var loadedLyricSongId: String? = null
    private var loadedPlayerCommentSongId: String? = null
    private var lastRecentSongId: String? = null
    private var lastLibraryRefreshAt = 0L
    private val artistTabMemory = mutableMapOf<String, ArtistPageTab>()
    private val notificationLikeHandler = { toggleCurrentSongLike() }
    private val notificationLikeStateProvider: (Song) -> Boolean = { song -> isSongLiked(song) }

    val playbackState: PlaybackState
        get() = player.state

    init {
        themePreferences.registerOnSharedPreferenceChangeListener(themePreferenceListener)
        PlaybackNotificationBridge.attachLikeHandler(notificationLikeHandler)
        PlaybackNotificationBridge.attachLikeStateProvider(notificationLikeStateProvider)
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
                player.playQueue(page.songs, startIndex.coerceIn(0, page.songs.lastIndex), registry)
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
        val sourceId = uiState.selectedSourceId
        val results = coroutineScope {
            val songs = async {
                runCatching {
                    musicRepository.searchSongs(sourceId, query, SEARCH_SONG_PAGE_SIZE, 0)
                }.getOrDefault(emptyList())
            }
            val playlists = async {
                runCatching {
                    musicRepository.searchPlaylists(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                }.getOrDefault(emptyList())
            }
            val albums = async {
                runCatching {
                    musicRepository.searchAlbums(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                }.getOrDefault(emptyList())
            }
            val artists = async {
                runCatching {
                    musicRepository.searchArtists(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, 0)
                }.getOrDefault(emptyList())
            }
            SearchInitialResults(
                songs = songs.await(),
                playlists = playlists.await(),
                albums = albums.await(),
                artists = artists.await()
            )
        }
        if (sourceId == uiState.selectedSourceId && query == uiState.searchQuery.trim()) {
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
        val sourceId = uiState.selectedSourceId
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
                        musicRepository.searchSongs(sourceId, query, SEARCH_SONG_PAGE_SIZE, offset)
                    }.getOrDefault(emptyList())
                    nextPlaylists = emptyList()
                }
                SearchResultKind.Playlists -> {
                    nextSongs = emptyList()
                    nextPlaylists = runCatching {
                        musicRepository.searchPlaylists(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                    }.getOrDefault(emptyList())
                }
                SearchResultKind.Albums -> {
                    nextSongs = emptyList()
                    nextPlaylists = runCatching {
                        musicRepository.searchAlbums(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                    }.getOrDefault(emptyList())
                }
                SearchResultKind.Artists -> {
                    nextSongs = emptyList()
                    nextPlaylists = runCatching {
                        musicRepository.searchArtists(sourceId, query, SEARCH_COLLECTION_PAGE_SIZE, offset)
                    }.getOrDefault(emptyList())
                }
            }
            if (sourceId != uiState.selectedSourceId || query != uiState.searchQuery.trim()) {
                uiState = uiState.copy(isLoadingMoreSearch = false)
                return@launch
            }
            uiState = when (kind) {
                SearchResultKind.Songs -> {
                    val combined = (uiState.searchResults + nextSongs).distinctBy { it.id }
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
        if (song.sourceId != "netease") {
            uiState = uiState.copy(
                playerComments = emptyList(),
                playerCommentCount = 0,
                isPlayerCommentsLoading = false,
                playerCommentsMessage = "当前只支持网易云歌曲评论"
            )
            return
        }
        if (!force && loadedPlayerCommentSongId == song.id && uiState.playerComments.isNotEmpty()) return
        playerCommentsJob?.cancel()
        playerCommentsJob = viewModelScope.launch {
            val sort = uiState.playerCommentSort
            loadedPlayerCommentSongId = song.id
            uiState = uiState.copy(
                isPlayerCommentsLoading = true,
                playerComments = if (force) emptyList() else uiState.playerComments,
                playerCommentsMessage = null
            )
            val bundle = runCatching {
                musicRepository.neteaseSongComments(song, sort)
            }.getOrElse {
                if (playbackState.currentSong?.id == song.id) {
                    uiState = uiState.copy(
                        isPlayerCommentsLoading = false,
                        playerCommentsMessage = "歌曲评论加载失败：${it.message.orEmpty()}"
                    )
                }
                return@launch
            }
            if (playbackState.currentSong?.id == song.id) {
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
        if (loadedPlayerCommentSongId != song.id && playerCommentsJob?.isActive != true) {
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
        return PlaybackSettingsState(
            quality = quality,
            lyricDisplayMode = lyricDisplayMode,
            statusBarLyricEnabled = playbackPreferences.getBoolean("status_bar_lyric", false)
        )
    }

    private fun savePlaybackSettings(settings: PlaybackSettingsState) {
        playbackPreferences.edit()
            .putString("quality", settings.quality.name)
            .putString("lyric_display_mode", settings.lyricDisplayMode.name)
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
            player.playQueue(playable.songs, startIndex, registry)
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
            player.playQueue(queue, index, registry)
        }
    }

    fun playSongNext(song: Song) {
        viewModelScope.launch {
            val queued = player.queueNext(song, registry)
            uiState = uiState.copy(
                message = if (queued) "已添加到下一首播放" else "暂时无法添加到下一首播放"
            )
        }
    }

    fun playSongAndOpenComments(song: Song) {
        val queue = playbackQueueFor(song)
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        viewModelScope.launch {
            player.playQueue(queue, index, registry)
            playerSheetPanel = PlayerSheetPanel.Comments
            isPlayerSheetVisible = true
        }
    }

    fun isSongLiked(song: Song): Boolean = song.id in uiState.likedSongIds

    fun isSongLikeLoading(song: Song): Boolean = song.id in uiState.songLikeLoadingIds

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
            uiState = uiState.copy(message = "当前没有可加入歌单的歌曲")
            return
        }
        if (target.sourceId != "netease") {
            uiState = uiState.copy(message = "当前歌曲暂不支持加入网易云歌单")
            return
        }
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "请先登录网易云账号后再加入歌单")
            return
        }
        uiState = uiState.copy(playlistChooserSong = target)
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
        val song = uiState.playlistChooserSong ?: playbackState.currentSong ?: run {
            uiState = uiState.copy(message = "当前没有可加入歌单的歌曲")
            return
        }
        if (song.sourceId != "netease" || playlist.kind != PlaylistKind.UserPlaylist) {
            uiState = uiState.copy(message = "当前只支持加入网易云自建歌单")
            return
        }
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "请先登录网易云账号后再加入歌单")
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
                    message = "加入歌单失败：${it.message.orEmpty()}"
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
        if (song.sourceId != "netease") {
            uiState = uiState.copy(message = "当前只支持网易云歌曲红心")
            return
        }
        if (!accountState.loginState.loggedIn) {
            uiState = uiState.copy(message = "请先登录网易云账号后再收藏歌曲")
            return
        }
        if (isSongLikeLoading(song)) return
        val loadingIds = uiState.songLikeLoadingIds + song.id
        uiState = uiState.copy(songLikeLoadingIds = loadingIds)
        viewModelScope.launch {
            val result = runCatching {
                musicRepository.neteaseToggleSongLike(song, shouldLike)
            }.getOrElse {
                uiState = uiState.copy(
                    songLikeLoadingIds = uiState.songLikeLoadingIds - song.id,
                    message = "歌曲红心操作失败：${it.message.orEmpty()}"
                )
                return@launch
            }
            val nextLikedIds = uiState.likedSongIds.toMutableSet().apply {
                if (result.success && shouldLike) add(song.id)
                if (result.success && !shouldLike) remove(song.id)
            }
            val nextLikedSongs = uiState.likedSongs?.let { liked ->
                val nextSongs = liked.songs.toMutableList().apply {
                    removeAll { it.id == song.id }
                    if (result.success && shouldLike) add(0, song)
                }.distinctBy { it.id }
                liked.copy(
                    songs = nextSongs,
                    coverUrl = nextSongs.firstOrNull()?.coverUrl ?: liked.coverUrl,
                    subtitle = if (nextSongs.isEmpty()) liked.subtitle else "${nextSongs.size} 首喜欢的歌"
                )
            }
            uiState = uiState.copy(
                likedSongIds = nextLikedIds,
                likedSongs = nextLikedSongs,
                songLikeLoadingIds = uiState.songLikeLoadingIds - song.id,
                likePrompt = if (result.success && shouldLike) {
                    LikePromptState(song, "已添加到我喜欢的音乐", showAddToPlaylistAction = true)
                } else if (result.success && !shouldLike) {
                    LikePromptState(song, "已从我喜欢的音乐移除", showAddToPlaylistAction = false)
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
        super.onCleared()
    }
}
