package com.silisten.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.composed
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.qmdeve.liquidglass.widget.LiquidGlassView
import com.silisten.app.AppTab
import com.silisten.app.LyricDisplayMode
import com.silisten.app.PlaybackSettingsState
import com.silisten.app.PlayerSheetPanel
import com.silisten.app.QrLoginUiState
import com.silisten.app.SettingsRoute
import com.silisten.app.SiListenUiState
import com.silisten.app.SiListenViewModel
import com.silisten.app.ThemeAccentOption
import com.silisten.app.ThemeColorSpecOption
import com.silisten.app.ThemeModeOption
import com.silisten.app.ThemePaletteStyleOption
import com.silisten.app.ThemeSettingsState
import com.silisten.app.ThemeUiModeOption
import com.silisten.app.data.model.LyricLine
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.PlaylistKind
import com.silisten.app.data.model.PlaylistRoute
import com.silisten.app.data.model.Song
import com.silisten.app.playback.PlaybackState
import com.silisten.app.ui.kernelsu.KernelSuFloatingBottomBar
import com.silisten.app.ui.kernelsu.LocalKernelSuFloatingBottomBarContentTint
import com.silisten.app.ui.kernelsu.KernelSuFloatingBottomBarItem
import com.silisten.app.ui.theme.accentColor
import com.silisten.app.ui.theme.appBackgroundBrush
import com.silisten.app.ui.theme.LocalSiListenAppearance
import com.silisten.app.ui.theme.onAccentColor
import com.silisten.app.ui.theme.resolveDarkTheme
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop as MiuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

private sealed interface SearchChromeState {
    data object Closed : SearchChromeState
    data class DockExpanded(val origin: AppTab) : SearchChromeState
    data class SearchPage(val origin: AppTab) : SearchChromeState
}

private fun SearchChromeState.originOrNull(): AppTab? = when (this) {
    SearchChromeState.Closed -> null
    is SearchChromeState.DockExpanded -> origin
    is SearchChromeState.SearchPage -> origin
}

private val SearchChromeStateSaver = listSaver<SearchChromeState, String>(
    save = { state ->
        when (state) {
            SearchChromeState.Closed -> listOf("closed", AppTab.Home.name)
            is SearchChromeState.DockExpanded -> listOf("dock", state.origin.name)
            is SearchChromeState.SearchPage -> listOf("page", state.origin.name)
        }
    },
    restore = { values ->
        val origin = values.getOrNull(1)
            ?.let { name -> runCatching { AppTab.valueOf(name) }.getOrNull() }
            ?: AppTab.Home
        when (values.firstOrNull()) {
            "dock" -> SearchChromeState.DockExpanded(origin)
            "page" -> SearchChromeState.SearchPage(origin)
            else -> SearchChromeState.Closed
        }
    }
)

private fun paddingForSearchOverlay(
    hasPlayback: Boolean,
    hasDock: Boolean,
    floating: Boolean
): Dp {
    var bottom = 24.dp
    if (hasPlayback) {
        bottom += if (floating) 100.dp else 92.dp
    }
    if (hasDock) {
        bottom += if (floating) 104.dp else 84.dp
    }
    return bottom.coerceAtLeast(156.dp)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SiListenApp(viewModel: SiListenViewModel) {
    val uiState = viewModel.uiState
    val selectedPlaylist = uiState.selectedPlaylist
    val resolvedDark = uiState.themeSettings.resolveDarkTheme()
    val background = appBackgroundBrush(uiState.themeSettings, resolvedDark)
    val hasActivePlayback = viewModel.playbackState.currentSong != null
    val appBackdrop = rememberLayerBackdrop()
    val miuixSurfaceColor = MaterialTheme.colorScheme.surface
    val miuixAppBackdrop = rememberMiuixLayerBackdrop {
        drawRect(miuixSurfaceColor)
        drawContent()
    }
    val appearance = LocalSiListenAppearance.current
    val mainTabs = remember { listOf(AppTab.Home, AppTab.Sources, AppTab.Account) }
    var currentMainTab by rememberSaveable {
        mutableStateOf(
            uiState.selectedTab.takeIf { it in mainTabs } ?: AppTab.Home
        )
    }
    var searchChromeState by rememberSaveable(stateSaver = SearchChromeStateSaver) {
        mutableStateOf<SearchChromeState>(SearchChromeState.Closed)
    }
    var returnToSearchOrigin by rememberSaveable { mutableStateOf<AppTab?>(null) }
    val searchOriginTab = searchChromeState.originOrNull()
    val isSearchDockExpanded = searchChromeState is SearchChromeState.DockExpanded
    val isSearchPageVisible = searchChromeState is SearchChromeState.SearchPage
    val bottomSelectedTab = searchOriginTab ?: currentMainTab
    val selectedPage = mainTabs.indexOf(currentMainTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { mainTabs.size })
    val pagerScope = rememberCoroutineScope()
    var lockedMainTab by remember { mutableStateOf<AppTab?>(null) }
    val pagerPosition by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .fastCoerceIn(0f, (mainTabs.size - 1).toFloat())
        }
    }
    val visibleMainTab by remember {
        derivedStateOf {
            val index = pagerPosition.roundToInt().coerceIn(0, mainTabs.lastIndex)
            mainTabs[index]
        }
    }
    fun captureSearchOrigin(): AppTab {
        val currentPageTab = mainTabs.getOrNull(pagerState.currentPage)
        val settledPageTab = mainTabs.getOrNull(pagerState.settledPage)
        return when {
            pagerState.isScrollInProgress -> visibleMainTab
            currentMainTab in mainTabs -> currentMainTab
            currentPageTab != null -> currentPageTab
            settledPageTab != null -> settledPageTab
            else -> AppTab.Home
        }
    }
    fun settleMainTab(tab: AppTab) {
        val index = mainTabs.indexOf(tab)
        if (index < 0) return
        lockedMainTab = tab
        pagerScope.launch {
            if (pagerState.currentPage != index) {
                pagerState.animateScrollToPage(index)
            }
            snapshotFlow { pagerState.settledPage }
                .filter { it == index }
                .first()
            if (lockedMainTab == tab) {
                lockedMainTab = null
            }
        }
    }

    fun openPlaylistFromSearch(playlist: MusicPlaylist) {
        val origin = searchChromeState.originOrNull() ?: captureSearchOrigin()
        returnToSearchOrigin = origin
        searchChromeState = SearchChromeState.Closed
        currentMainTab = origin
        viewModel.selectTab(origin)
        settleMainTab(origin)
        viewModel.openPlaylist(playlist)
    }

    fun closePlaylistWithReturn() {
        val hadPlaylistBackStack = uiState.playlistBackStack.isNotEmpty()
        viewModel.closePlaylist()
        if (!hadPlaylistBackStack) {
            returnToSearchOrigin?.let { origin ->
                searchChromeState = SearchChromeState.SearchPage(origin)
                currentMainTab = origin
                viewModel.selectTab(origin)
                settleMainTab(origin)
                returnToSearchOrigin = null
            }
        }
    }

    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            lockedMainTab = currentMainTab
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    LaunchedEffect(uiState.selectedTab, searchChromeState) {
        if (
            searchChromeState is SearchChromeState.Closed &&
            uiState.selectedTab in mainTabs &&
            uiState.selectedTab != currentMainTab
        ) {
            currentMainTab = uiState.selectedTab
        }
    }

    val latestSettingsRoute = androidx.compose.runtime.rememberUpdatedState(uiState.settingsRoute)
    val latestSelectedTab = androidx.compose.runtime.rememberUpdatedState(uiState.selectedTab)
    val latestLockedMainTab = androidx.compose.runtime.rememberUpdatedState(lockedMainTab)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (latestSettingsRoute.value == SettingsRoute.Main) {
                val targetTab = mainTabs.getOrNull(page) ?: AppTab.Home
                val lockedTab = latestLockedMainTab.value
                if (lockedTab != null && targetTab != lockedTab) {
                    return@collectLatest
                }
                if (lockedTab == targetTab) {
                    lockedMainTab = null
                }
                currentMainTab = targetTab
                if (latestSelectedTab.value != targetTab) {
                    viewModel.selectTab(targetTab)
                }
            }
        }
    }

    BackHandler(
        enabled = isSearchPageVisible || isSearchDockExpanded
    ) {
        viewModel.closeSearchPage()
        returnToSearchOrigin = null
        val origin = searchChromeState.originOrNull() ?: currentMainTab
        searchChromeState = SearchChromeState.Closed
        currentMainTab = origin
        viewModel.selectTab(origin)
        settleMainTab(origin)
    }

    BackHandler(
        enabled = uiState.settingsRoute != SettingsRoute.Main
    ) {
        viewModel.closeThemeSettings()
    }

    BackHandler(
        enabled = uiState.settingsRoute == SettingsRoute.Main &&
            selectedPlaylist == null &&
            searchChromeState is SearchChromeState.Closed &&
            currentMainTab != AppTab.Home
    ) {
        currentMainTab = AppTab.Home
        viewModel.selectTab(AppTab.Home)
        settleMainTab(AppTab.Home)
    }

    BackHandler(enabled = selectedPlaylist != null) {
        if (uiState.selectedPlaylistRoute == PlaylistRoute.Comments) {
            viewModel.showPlaylistOverview()
        } else {
            closePlaylistWithReturn()
        }
    }

    Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(appBackdrop)
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        SiListenBottomChromeReserve(
                            hasPlaybackChrome = viewModel.playbackState.currentSong != null ||
                                viewModel.playbackState.errorMessage != null,
                            hideNavDock = selectedPlaylist != null
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (uiState.settingsRoute != SettingsRoute.Main) {
                            SettingsScreen(uiState, viewModel, padding)
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (appearance.floatingBottomBarEnabled &&
                                            appearance.blurEnabled &&
                                            appearance.floatingBottomBarBlurEnabled
                                        ) {
                                            Modifier.miuixLayerBackdrop(miuixAppBackdrop)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) { page ->
                                when (mainTabs[page]) {
                                    AppTab.Home -> HomeScreen(uiState, viewModel, padding)
                                    AppTab.Sources -> SourcesScreen(uiState, viewModel, padding)
                                    AppTab.Account -> AccountScreen(
                                        viewModel = viewModel,
                                        padding = padding,
                                        onSearch = {
                                            searchChromeState = SearchChromeState.SearchPage(captureSearchOrigin())
                                            returnToSearchOrigin = null
                                        }
                                    )
                                    AppTab.Search,
                                    AppTab.Settings -> HomeScreen(uiState, viewModel, padding)
                                }
                            }
                        }

                        if (selectedPlaylist != null) {
                            if (selectedPlaylist.kind == PlaylistKind.Artist) {
                                ArtistDetailScreen(
                                    artist = selectedPlaylist,
                                    isLoading = uiState.isPlaylistDetailLoading,
                                    message = uiState.playlistDetailMessage,
                                    selectedPageTab = uiState.selectedArtistTab,
                                    artistSongsPage = uiState.artistSongsPage,
                                    dark = resolvedDark,
                                    glassy = appearance.blurEnabled,
                                    onBack = ::closePlaylistWithReturn,
                                    onPlayAll = { viewModel.playSelectedArtistTab() },
                                    onTabSelected = viewModel::selectArtistTab,
                                    onLoadMoreArtistSongs = { viewModel.loadMoreArtistSongs() },
                                    onSongClick = { song -> viewModel.playSong(song) },
                                    isSongLiked = viewModel::isSongLiked,
                                    isSongLikeLoading = viewModel::isSongLikeLoading,
                                    onToggleSongLike = viewModel::toggleSongLike,
                                    onPlaySongNext = viewModel::playSongNext,
                                    onAddSongToPlaylist = viewModel::openAddToPlaylistChooser,
                                    onShowSongComments = viewModel::playSongAndOpenComments,
                                    onAlbumClick = viewModel::openPlaylist,
                                    reserveMiniPlayerSpace = hasActivePlayback
                                )
                            } else {
                                PlaylistDetailScreen(
                                    playlist = selectedPlaylist,
                                    route = uiState.selectedPlaylistRoute,
                                    songSearchQuery = uiState.playlistSongSearchQuery,
                                    isLoading = uiState.isPlaylistDetailLoading,
                                    message = uiState.playlistDetailMessage,
                                    commentSort = uiState.playlistCommentSort,
                                    comments = uiState.playlistComments,
                                    commentCount = uiState.playlistCommentCount,
                                    isCommentsLoading = uiState.isPlaylistCommentsLoading,
                                    commentsMessage = uiState.playlistCommentsMessage,
                                    isSubscribed = viewModel.isSelectedPlaylistSubscribed(),
                                    isSubscriptionLoading = uiState.isPlaylistSubscriptionLoading,
                                    canShowComments = viewModel.canSelectedPlaylistShowComments(),
                                    canShowSubscriptionAction = viewModel.canSelectedPlaylistShowSubscriptionAction(),
                                    isSubscriptionLocked = viewModel.isSelectedPlaylistSubscriptionLocked(),
                                    dark = resolvedDark,
                                    glassy = appearance.blurEnabled,
                                    onBack = {
                                        if (uiState.selectedPlaylistRoute == PlaylistRoute.Comments) {
                                            viewModel.showPlaylistOverview()
                                        } else {
                                            closePlaylistWithReturn()
                                        }
                                    },
                                    onPlayAll = { viewModel.playSelectedPlaylist() },
                                    onSongClick = { song -> viewModel.playSong(song) },
                                    isSongLiked = viewModel::isSongLiked,
                                    isSongLikeLoading = viewModel::isSongLikeLoading,
                                    onToggleSongLike = viewModel::toggleSongLike,
                                    onPlaySongNext = viewModel::playSongNext,
                                    onAddSongToPlaylist = viewModel::openAddToPlaylistChooser,
                                    onShowSongComments = viewModel::playSongAndOpenComments,
                                    onToggleSubscription = viewModel::toggleSelectedPlaylistSubscription,
                                    onShowSongs = viewModel::showPlaylistOverview,
                                    onShowComments = viewModel::showPlaylistComments,
                                    onRefreshComments = viewModel::refreshPlaylistComments,
                                    onSongSearchQueryChange = viewModel::updatePlaylistSongSearchQuery,
                                    onCommentSortChange = viewModel::selectPlaylistCommentSort,
                                    reserveMiniPlayerSpace = hasActivePlayback
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(20f)
                ) {
                    SiListenBottomChrome(
                        viewModel = viewModel,
                        hideNavDock = selectedPlaylist != null,
                        backdrop = appBackdrop,
                        miuixBackdrop = miuixAppBackdrop,
                        selected = bottomSelectedTab,
                        selectedPosition = if (uiState.settingsRoute == SettingsRoute.Main) pagerPosition else null,
                        onSelect = { tab ->
                            if (isSearchPageVisible || isSearchDockExpanded) {
                                viewModel.closeSearchPage()
                            }
                            searchChromeState = SearchChromeState.Closed
                            returnToSearchOrigin = null
                            currentMainTab = tab
                            viewModel.selectTab(tab)
                            settleMainTab(tab)
                        },
                        searchExpanded = isSearchDockExpanded,
                        onSearchExpand = {
                            val origin = captureSearchOrigin()
                            returnToSearchOrigin = null
                            searchChromeState = SearchChromeState.DockExpanded(origin)
                        },
                        onSearchSubmit = {
                            val origin = searchChromeState.originOrNull() ?: captureSearchOrigin()
                            searchChromeState = SearchChromeState.SearchPage(origin)
                            returnToSearchOrigin = null
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isSearchPageVisible,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 430f),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(animationSpec = tween(120)),
                exit = slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { fullHeight -> fullHeight }
                ) + fadeOut(animationSpec = tween(120)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(18f)
            ) {
                val origin = searchChromeState.originOrNull() ?: captureSearchOrigin()
                SearchOverlay(
                    uiState = uiState,
                    viewModel = viewModel,
                    dark = resolvedDark,
                    autoFocus = isSearchPageVisible,
                    bottomPadding = paddingForSearchOverlay(
                        hasPlayback = viewModel.playbackState.currentSong != null ||
                            viewModel.playbackState.errorMessage != null,
                        hasDock = selectedPlaylist == null,
                        floating = appearance.floatingBottomBarEnabled
                    ),
                    onClose = {
                        searchChromeState = SearchChromeState.Closed
                        returnToSearchOrigin = null
                        currentMainTab = origin
                        viewModel.selectTab(origin)
                        settleMainTab(origin)
                    },
                    onOpenPlaylist = ::openPlaylistFromSearch
                )
            }

            if (isSearchPageVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(29f)
                ) {
                    SiListenBottomChrome(
                        viewModel = viewModel,
                        hideNavDock = selectedPlaylist != null,
                        backdrop = appBackdrop,
                        miuixBackdrop = miuixAppBackdrop,
                        selected = bottomSelectedTab,
                        selectedPosition = if (uiState.settingsRoute == SettingsRoute.Main) pagerPosition else null,
                        onSelect = { tab ->
                            viewModel.closeSearchPage()
                            searchChromeState = SearchChromeState.Closed
                            returnToSearchOrigin = null
                            currentMainTab = tab
                            viewModel.selectTab(tab)
                            settleMainTab(tab)
                        },
                        searchExpanded = false,
                        onSearchExpand = {
                            val origin = searchChromeState.originOrNull() ?: captureSearchOrigin()
                            returnToSearchOrigin = null
                            searchChromeState = SearchChromeState.SearchPage(origin)
                        },
                        onSearchSubmit = {
                            val origin = searchChromeState.originOrNull() ?: captureSearchOrigin()
                            searchChromeState = SearchChromeState.SearchPage(origin)
                            returnToSearchOrigin = null
                        }
                    )
                }
            }

            if (!viewModel.isPlayerSheetVisible) {
                LikeAddedPrompt(
                    prompt = uiState.likePrompt,
                    accent = uiState.themeSettings.accentColor(),
                    onAddToPlaylist = viewModel::openAddToPlaylistChooser,
                    onDismiss = viewModel::dismissLikePrompt,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 18.dp, start = 18.dp, end = 18.dp)
                        .zIndex(30f)
                )
            }

            if (viewModel.isPlayerSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = viewModel::closePlayerSheet,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF0A0A0A),
                    scrimColor = Color.Black.copy(alpha = 0.42f),
                    dragHandle = null
                ) {
                    FullPlayer(
                        playback = viewModel.playbackState,
                        lyrics = uiState.lyrics,
                        isLyricLoading = uiState.isLyricLoading,
                        playerComments = uiState.playerComments,
                        playerCommentSort = uiState.playerCommentSort,
                        playerCommentCount = uiState.playerCommentCount,
                        playerCommentsHasMore = uiState.playerCommentsHasMore,
                        isPlayerCommentsLoading = uiState.isPlayerCommentsLoading,
                        isLoadingMorePlayerComments = uiState.isLoadingMorePlayerComments,
                        playerCommentsMessage = uiState.playerCommentsMessage,
                        themeSettings = uiState.themeSettings,
                        lyricDisplayMode = uiState.playbackSettings.lyricDisplayMode,
                        initialPanel = viewModel.playerSheetPanel,
                        isLiked = viewModel.playbackState.currentSong?.let(viewModel::isSongLiked) == true,
                        isLikeLoading = viewModel.playbackState.currentSong?.let(viewModel::isSongLikeLoading) == true,
                        likePrompt = uiState.likePrompt,
                        sleepTimer = uiState.sleepTimer,
                        onToggle = viewModel::togglePlayback,
                        onNext = viewModel::next,
                        onPrevious = viewModel::previous,
                        onPlayQueueIndex = viewModel::playQueueIndex,
                        onSeek = viewModel::seekTo,
                        onRefreshLyrics = viewModel::refreshCurrentLyrics,
                        onToggleLike = viewModel::toggleSongLike,
                        onArtistClick = viewModel::openArtistFromSong,
                        onPlaybackModeChange = viewModel::selectPlaybackMode,
                        onStartSleepTimer = viewModel::startSleepTimer,
                        onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                        onLikePromptAddToPlaylist = viewModel::openAddToPlaylistChooser,
                        onDismissLikePrompt = viewModel::dismissLikePrompt,
                        onPlayerCommentSortChange = viewModel::selectPlayerCommentSort,
                        onRefreshPlayerComments = viewModel::refreshPlayerComments,
                        onLoadMorePlayerComments = viewModel::loadMorePlayerComments
                    )
                }
            }

            uiState.playlistChooserSong?.let { song ->
                ModalBottomSheet(
                    onDismissRequest = viewModel::closeAddToPlaylistChooser,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = if (resolvedDark) Color(0xFF111111) else Color(0xFFF7F7F8),
                    scrimColor = Color.Black.copy(alpha = 0.36f),
                    dragHandle = null
                ) {
                    AddToPlaylistSheet(
                        song = song,
                        playlists = uiState.localPlaylists + uiState.userPlaylists.filter { playlist ->
                            playlist.kind == PlaylistKind.UserPlaylist &&
                                playlist.title != "我喜欢的音乐"
                        },
                        dark = resolvedDark,
                        isLoading = viewModel::isPlaylistAddLoading,
                        onPlaylistClick = viewModel::addPlaylistChooserSongToPlaylist
                    )
                }
            }

    }
}
