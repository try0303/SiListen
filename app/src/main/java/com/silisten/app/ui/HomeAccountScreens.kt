package com.silisten.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.composed
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.silisten.app.SearchResultKind
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop as MiuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop as rememberMiuixLayerBackdrop
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    padding: PaddingValues
) {
    val dark = uiState.themeSettings.resolveDarkTheme()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isRefreshing = uiState.isLoading || uiState.isLibraryLoading
    var actionSong by remember { mutableStateOf<Song?>(null) }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    var pullDistancePx by remember { mutableFloatStateOf(0f) }
    val refreshThresholdPx = with(density) { 92.dp.toPx() }
    val pullConnection = remember(isRefreshing, isAtTop, refreshThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullDistancePx > 0f) {
                    val consumed = minOf(pullDistancePx, -available.y)
                    pullDistancePx -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && isAtTop && !isRefreshing) {
                    pullDistancePx = (pullDistancePx + available.y * 0.55f).coerceAtMost(refreshThresholdPx * 1.35f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistancePx >= refreshThresholdPx && !isRefreshing) {
                    viewModel.refreshHome()
                }
                pullDistancePx = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 156.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                PageTopTitle("首页", dark = dark)
            }
            item {
                uiState.message?.let {
                    Text(it, color = Color(0xFFFFD166), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SectionTitle("推荐歌单", dark = dark)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val playlists = uiState.recommendedPlaylists.ifEmpty { uiState.featured }
                    items(playlists) { playlist ->
                        PlaylistTile(playlist = playlist, onClick = { viewModel.openPlaylist(playlist) })
                    }
                }
            }
            uiState.dailyDiscovery?.let { playlist ->
                item {
                    FeaturePlaylistCard(
                        playlist = playlist,
                        icon = Icons.Rounded.Radio,
                        accent = Color(0xFF1ED760),
                        dark = uiState.themeSettings.resolveDarkTheme(),
                        onClick = { viewModel.openPlaylist(playlist) }
                    )
                }
            }
            val songs = uiState.dailyDiscovery?.songs?.ifEmpty { null }
                ?: uiState.featured.firstOrNull()?.songs?.ifEmpty { null }
                ?: uiState.recentPlayedSongs
            if (songs.isNotEmpty()) {
                item {
                    SectionTitle(
                        if (uiState.dailyDiscovery?.songs?.isNotEmpty() == true ||
                            uiState.featured.firstOrNull()?.songs?.isNotEmpty() == true
                        ) {
                            "今日播放"
                        } else {
                            "最近播放"
                        },
                        dark = dark
                    )
                }
                items(songs) { song ->
                    SongRow(
                        song = song,
                        liked = viewModel.isSongLiked(song),
                        likeLoading = viewModel.isSongLikeLoading(song),
                        onClick = { viewModel.playSong(song) },
                        onLikeClick = if (song.sourceId != "local") ({ viewModel.toggleSongLike(song) }) else null,
                        onMoreClick = { actionSong = song }
                    )
                }
            }
        }

        val indicatorVisible = pullDistancePx > 1f || isRefreshing
        if (indicatorVisible) {
            val offsetY = with(density) {
                (if (isRefreshing) 42.dp else (pullDistancePx / 3f).toDp()).coerceAtMost(54.dp)
            }
            Surface(
                color = if (dark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.88f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = offsetY)
                    .size(42.dp)
                    .shadow(16.dp, CircleShape, clip = false)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    actionSong?.let { song ->
        SongActionSheetModal(
            song = song,
            dark = dark,
            onDismiss = { actionSong = null },
            onPlayNext = {
                actionSong = null
                viewModel.playSongNext(song)
            },
            onAddToPlaylist = {
                actionSong = null
                viewModel.openAddToPlaylistChooser(song)
            },
            onShowComments = {
                actionSong = null
                viewModel.playSongAndOpenComments(song)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    padding: PaddingValues,
    autoFocus: Boolean = false,
    onClose: (() -> Unit)? = null,
    onOpenPlaylist: ((MusicPlaylist) -> Unit)? = null
) {
    val dark = uiState.themeSettings.resolveDarkTheme()
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tabs = SearchResultTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val songsListState = rememberLazyListState()
    val playlistsListState = rememberLazyListState()
    val albumsListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()
    val selectedTab = tabs[pagerState.currentPage.coerceIn(0, tabs.lastIndex)]
    val selectedListState = when (selectedTab) {
        SearchResultTab.Songs -> songsListState
        SearchResultTab.Playlists -> playlistsListState
        SearchResultTab.Albums -> albumsListState
        SearchResultTab.Artists -> artistsListState
    }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var dismissDragOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 128.dp.toPx() }
    val dragSlopPx = with(density) { 12.dp.toPx() }
    val query = uiState.searchQuery.trim()
    val songs = uiState.searchResults
    val playlists = uiState.searchPlaylists
    val albumResults = uiState.searchAlbums
    val artistResults = uiState.searchArtists
    val hasAnyResults = songs.isNotEmpty() ||
        playlists.isNotEmpty() ||
        albumResults.isNotEmpty() ||
        artistResults.isNotEmpty()
    val selectedHasMore = when (selectedTab) {
        SearchResultTab.Songs -> uiState.searchHasMoreSongs
        SearchResultTab.Playlists -> uiState.searchHasMorePlaylists
        SearchResultTab.Albums -> uiState.searchHasMoreAlbums
        SearchResultTab.Artists -> uiState.searchHasMoreArtists
    }
    val selectedCount = when (selectedTab) {
        SearchResultTab.Songs -> songs.size
        SearchResultTab.Playlists -> playlists.size
        SearchResultTab.Albums -> albumResults.size
        SearchResultTab.Artists -> artistResults.size
    }
    val closeSearchPage: () -> Unit = {
        viewModel.closeSearchPage()
        onClose?.invoke()
    }
    val openPlaylistFromSearch = onOpenPlaylist?.let { open ->
        { playlist: MusicPlaylist ->
            viewModel.closeSearchPage()
            open(playlist)
        }
    } ?: { playlist: MusicPlaylist ->
        closeSearchPage()
        viewModel.openPlaylist(playlist)
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(220)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            pagerState.scrollToPage(0)
        }
        songsListState.scrollToItem(0)
        playlistsListState.scrollToItem(0)
        albumsListState.scrollToItem(0)
        artistsListState.scrollToItem(0)
    }
    val collectionArtworkUrls = remember(playlists, albumResults, artistResults) {
        (playlists + albumResults + artistResults)
            .map { it.coverUrl }
            .filter { it.isNotBlank() }
            .distinct()
            .take(18)
    }
    LaunchedEffect(collectionArtworkUrls) {
        if (collectionArtworkUrls.isEmpty()) return@LaunchedEffect
        val imageLoader = ImageLoader(context)
        collectionArtworkUrls.forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(160)
                .build()
            runCatching { imageLoader.execute(request) }
        }
    }
    LaunchedEffect(
        selectedListState,
        selectedTab,
        query,
        selectedCount,
        selectedHasMore,
        uiState.isSearching,
        uiState.isLoadingMoreSearch
    ) {
        snapshotFlow {
            val layoutInfo = selectedListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            layoutInfo.totalItemsCount > 0 && lastVisible >= layoutInfo.totalItemsCount - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest {
                if (query.isNotBlank() && selectedHasMore && !uiState.isSearching && !uiState.isLoadingMoreSearch) {
                    viewModel.loadMoreSearchResults(selectedTab.toKind())
                }
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .graphicsLayer {
                translationY = dismissDragOffsetPx
                alpha = 1f - (dismissDragOffsetPx / (dismissThresholdPx * 2f)).coerceIn(0f, 0.18f)
            }
            .pointerInput(onClose, selectedTab, selectedListState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var pointerId = down.id
                    var totalX = 0f
                    var totalY = 0f
                    var isDismissDrag = false
                    var passedSlop = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()?.also { pointerId = it.id }
                            ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        val listAtTop = selectedListState.firstVisibleItemIndex == 0 &&
                            selectedListState.firstVisibleItemScrollOffset == 0
                        if (!passedSlop && abs(totalY) > dragSlopPx && abs(totalY) > abs(totalX) * 1.2f) {
                            passedSlop = true
                            isDismissDrag = onClose != null && listAtTop && totalY > 0f
                        }
                        if (isDismissDrag) {
                            dismissDragOffsetPx = (dismissDragOffsetPx + delta.y).coerceAtLeast(0f)
                            change.consume()
                        }
                    }
                    if (isDismissDrag && dismissDragOffsetPx >= dismissThresholdPx) {
                        closeSearchPage()
                    }
                    dismissDragOffsetPx = 0f
                }
            }
            .padding(start = 18.dp, end = 18.dp, top = 12.dp)
    ) {
        SearchPageHeader(
            dark = dark,
            onClose = if (onClose != null) closeSearchPage else null
        )
        Spacer(Modifier.height(16.dp))
        IosSearchField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            onSearch = viewModel::submitCurrentSearch,
            dark = dark,
            loading = uiState.isSearching,
            modifier = Modifier.focusRequester(searchFocusRequester)
        )
        Spacer(Modifier.height(16.dp))
        if (query.isBlank() && !hasAnyResults) {
            SearchHistorySection(
                history = uiState.searchHistory,
                dark = dark,
                onHistoryClick = viewModel::selectSearchHistory,
                onHistoryLongClick = viewModel::removeSearchHistory,
                onClearHistory = viewModel::clearSearchHistory
            )
        } else {
            SearchCategoryTabs(
                selected = selectedTab,
                onSelect = { tab ->
                    scope.launch {
                        pagerState.animateScrollToPage(tabs.indexOf(tab).coerceAtLeast(0))
                    }
                },
                dark = dark
            )
            Spacer(Modifier.height(10.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (tabs[page]) {
                    SearchResultTab.Songs -> SearchResultsPage(
                        listState = songsListState,
                        padding = padding,
                        dark = dark,
                        isSearching = uiState.isSearching,
                        isLoadingMore = uiState.isLoadingMoreSearch,
                        hasMore = uiState.searchHasMoreSongs,
                        loadingText = "继续加载单曲..."
                    ) {
                        if (uiState.isSearching && songs.isEmpty()) {
                            item { SearchLoadingRow(dark = dark) }
                        } else if (songs.isEmpty()) {
                            item { SearchResultEmptyRow("没有找到单曲", dark) }
                        } else {
                            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                SearchSongResultRow(
                                    index = index + 1,
                                    song = song,
                                    dark = dark,
                                    liked = viewModel.isSongLiked(song),
                                    likeLoading = viewModel.isSongLikeLoading(song),
                                    onClick = { viewModel.playSong(song) },
                                    onLikeClick = if (song.sourceId != "local") ({ viewModel.toggleSongLike(song) }) else null,
                                    onMoreClick = { actionSong = song }
                                )
                            }
                        }
                    }
                    SearchResultTab.Playlists -> SearchResultsPage(
                        listState = playlistsListState,
                        padding = padding,
                        dark = dark,
                        isLoadingMore = uiState.isLoadingMoreSearch,
                        hasMore = uiState.searchHasMorePlaylists,
                        loadingText = "继续加载歌单..."
                    ) {
                        if (playlists.isEmpty()) {
                            item { SearchResultEmptyRow("没有找到歌单", dark) }
                        } else {
                            items(playlists, key = { it.id }) { playlist ->
                                SearchCollectionResultRow(
                                    playlist = playlist,
                                    dark = dark,
                                    subtitle = playlist.subtitle.ifBlank { "${playlist.songs.size} 单曲" },
                                    artworkShape = CircleShape,
                                    fallbackIcon = Icons.Rounded.LibraryMusic,
                                    onClick = { openPlaylistFromSearch(playlist) }
                                )
                            }
                        }
                    }
                    SearchResultTab.Albums -> SearchResultsPage(
                        listState = albumsListState,
                        padding = padding,
                        dark = dark,
                        isLoadingMore = uiState.isLoadingMoreSearch,
                        hasMore = uiState.searchHasMoreAlbums,
                        loadingText = "继续加载专辑..."
                    ) {
                        if (albumResults.isEmpty()) {
                            item { SearchResultEmptyRow("没有找到专辑", dark) }
                        } else {
                            items(albumResults, key = { it.id }) { album ->
                                SearchCollectionResultRow(
                                    playlist = album,
                                    dark = dark,
                                    subtitle = album.subtitle,
                                    artworkShape = RoundedCornerShape(14.dp),
                                    fallbackIcon = Icons.Rounded.Album,
                                    onClick = { openPlaylistFromSearch(album) }
                                )
                            }
                        }
                    }
                    SearchResultTab.Artists -> SearchResultsPage(
                        listState = artistsListState,
                        padding = padding,
                        dark = dark,
                        isLoadingMore = uiState.isLoadingMoreSearch,
                        hasMore = uiState.searchHasMoreArtists,
                        loadingText = "继续加载歌手..."
                    ) {
                        if (artistResults.isEmpty()) {
                            item { SearchResultEmptyRow("没有找到歌手", dark) }
                        } else {
                            items(artistResults, key = { it.id }) { artist ->
                                SearchCollectionResultRow(
                                    playlist = artist,
                                    dark = dark,
                                    subtitle = artist.searchArtistSubtitle(),
                                    artworkShape = CircleShape,
                                    fallbackIcon = Icons.Rounded.AccountCircle,
                                    onClick = { openPlaylistFromSearch(artist) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    actionSong?.let { song ->
        SongActionSheetModal(
            song = song,
            dark = dark,
            onDismiss = { actionSong = null },
            onPlayNext = {
                actionSong = null
                viewModel.playSongNext(song)
            },
            onAddToPlaylist = {
                actionSong = null
                viewModel.openAddToPlaylistChooser(song)
            },
            onShowComments = {
                actionSong = null
                closeSearchPage()
                viewModel.playSongAndOpenComments(song)
            }
        )
    }
}

@Composable
private fun SearchResultsPage(
    listState: LazyListState,
    padding: PaddingValues,
    dark: Boolean,
    isSearching: Boolean = false,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    loadingText: String,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = padding.calculateBottomPadding().coerceAtLeast(156.dp)
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
        if (!isSearching && (hasMore || isLoadingMore)) {
            item { SearchLoadingRow(dark = dark, text = loadingText) }
        }
    }
}

private enum class SearchResultTab(val label: String) {
    Songs("单曲"),
    Playlists("歌单"),
    Albums("专辑"),
    Artists("歌手")
}

private fun SearchResultTab.toKind(): SearchResultKind = when (this) {
    SearchResultTab.Songs -> SearchResultKind.Songs
    SearchResultTab.Playlists -> SearchResultKind.Playlists
    SearchResultTab.Albums -> SearchResultKind.Albums
    SearchResultTab.Artists -> SearchResultKind.Artists
}

@Composable
private fun SearchPageHeader(
    dark: Boolean,
    onClose: (() -> Unit)?
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "搜索",
            color = titleColor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭搜索",
                    tint = titleColor,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun IosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    dark: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF151515)
    val mutedText = if (dark) Color.White.copy(alpha = 0.48f) else Color(0xFF7B7D82)
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(999.dp),
        shadowElevation = if (dark) 0.dp else 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                onValueChange(it.text)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = modifier.fillMaxSize(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.84f),
                        modifier = Modifier.size(29.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                text = "歌曲、歌手、专辑或歌单",
                                color = mutedText,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (loading) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchCategoryTabs(
    selected: SearchResultTab,
    onSelect: (SearchResultTab) -> Unit,
    dark: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    val normal = if (dark) Color.White.copy(alpha = 0.68f) else Color(0xFF4B4D52)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchResultTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .noRippleClick(shape = RoundedCornerShape(16.dp)) { onSelect(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) accent else normal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 28.dp else 0.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) accent else Color.Transparent)
                )
            }
        }
    }
}

private fun MusicPlaylist.searchArtistSubtitle(): String {
    val total = songCount.coerceAtLeast(songs.size)
    val fallback = subtitle.takeIf {
        it.isNotBlank() && it != "歌手"
    }
    return listOfNotNull(
        total.takeIf { it > 0 }?.let { "$it 首单曲" },
        albumCount.takeIf { it > 0 }?.let { "$it 张专辑" },
        fallback
    ).joinToString(" · ").ifBlank { "歌手" }
}

@Composable
private fun SearchSongResultRow(
    index: Int,
    song: Song,
    dark: Boolean,
    liked: Boolean,
    likeLoading: Boolean,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)?,
    onMoreClick: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color.White.copy(alpha = 0.54f) else Color(0xFF676A70)
    val rankLabel = if (index > 99) "99+" else index.toString()
    val rankStyle = if (index > 99) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge
    val heartTint by animateColorAsState(
        targetValue = if (liked) Color(0xFFFF5C7C) else mutedText,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "search-song-heart"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rankLabel,
            color = if (index > 99) mutedText else titleColor,
            style = rankStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(38.dp)
        )
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF252525))
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.artist,
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onLikeClick != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .noRippleClick(shape = CircleShape, onClick = onLikeClick),
                contentAlignment = Alignment.Center
            ) {
                if (likeLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 1.8.dp,
                        color = heartTint
                    )
                } else {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "喜欢",
                        tint = heartTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "更多",
                tint = mutedText,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun SearchCollectionResultRow(
    playlist: MusicPlaylist,
    dark: Boolean,
    subtitle: String,
    artworkShape: Shape,
    fallbackIcon: ImageVector,
    onClick: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color.White.copy(alpha = 0.54f) else Color(0xFF676A70)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchArtwork(
            url = playlist.coverUrl,
            title = playlist.title,
            shape = artworkShape,
            fallbackIcon = fallbackIcon
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = mutedText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SearchArtwork(
    url: String,
    title: String,
    shape: Shape,
    fallbackIcon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            fallbackIcon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(28.dp)
        )
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    dark: Boolean,
    onHistoryClick: (String) -> Unit,
    onHistoryLongClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    if (history.isEmpty()) return
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val chipColor = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f)
    val chipBorder = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
    var showClearConfirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                color = titleColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "清空搜索历史",
                    tint = if (dark) Color.White.copy(alpha = 0.62f) else Color(0xFF696B70),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history, key = { it }) { keyword ->
                Surface(
                    color = chipColor,
                    border = BorderStroke(1.dp, chipBorder),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.pointerInput(keyword) {
                        detectTapGestures(
                            onTap = { onHistoryClick(keyword) },
                            onLongPress = { onHistoryLongClick(keyword) }
                        )
                    }
                ) {
                    Text(
                        text = keyword,
                        color = if (dark) Color.White.copy(alpha = 0.78f) else Color(0xFF3F4248),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空历史记录？") },
            text = { Text("确定要清空全部搜索历史记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearHistory()
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SearchLoadingRow(
    dark: Boolean,
    text: String = "正在搜索..."
) {
    val mutedText = if (dark) Color.White.copy(alpha = 0.60f) else Color(0xFF676A70)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = mutedText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchResultEmptyRow(
    text: String,
    dark: Boolean
) {
    val mutedText = if (dark) Color.White.copy(alpha = 0.54f) else Color(0xFF676A70)
    Text(
        text = text,
        color = mutedText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp)
    )
}

@Composable
fun SourcesScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    padding: PaddingValues
) {
    val dark = uiState.themeSettings.resolveDarkTheme()
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val recentMusicSongs = remember(uiState.recentPlayedSongs) {
        uiState.recentPlayedSongs
            .distinctBy { "${it.sourceId}:${it.id}:${it.streamHint.orEmpty()}" }
            .take(50)
    }
    val recentMusicPlaylist = remember(recentMusicSongs) {
        recentMusicSongs.takeIf { it.isNotEmpty() }?.let { songs ->
            MusicPlaylist(
                id = "local-recent-music",
                title = "最近音乐",
                subtitle = "最近播放 · ${songs.size} 首",
                coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
                songs = songs,
                kind = PlaylistKind.Playlist,
                songCount = songs.size
            )
        }
    }
    val visibleLocalPlaylists = remember(recentMusicPlaylist, uiState.localPlaylists) {
        listOfNotNull(recentMusicPlaylist) + uiState.localPlaylists
    }
    val localPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.scanLocalMusic()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 156.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageTopTitle("音乐库", dark = dark)
        }
        item {
            Surface(
                color = cardColor,
                border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Color(0xFF8BD3FF))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "本地音乐",
                                color = titleColor,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        PrimaryActionButton(
                            text = "扫描",
                            onClick = { localPermissionLauncher.launch(audioPermission) },
                            containerColor = Color(0xFF8BD3FF),
                            contentColor = Color(0xFF081018)
                        )
                    }
                    StatusMessageCard(
                        text = uiState.localMusicMessage,
                        dark = dark
                    )
                    if (uiState.localSongs.isNotEmpty()) {
                        SecondaryActionButton(
                            text = "查看全部本地歌曲",
                            dark = dark,
                            onClick = {
                                viewModel.openPlaylist(
                                    MusicPlaylist(
                                        id = "local-library",
                                        title = "本地音乐",
                                        subtitle = uiState.localMusicMessage,
                                        coverUrl = "",
                                        songs = uiState.localSongs,
                                        kind = PlaylistKind.LocalMusic
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            Surface(
                color = cardColor,
                border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    LocalPlaylistLibrarySection(
                        playlists = visibleLocalPlaylists,
                        dark = dark,
                        onCreate = viewModel::createLocalPlaylist,
                        onOpenPlaylist = viewModel::openPlaylist,
                        onDeletePlaylist = viewModel::deleteLocalPlaylist
                    )
                }
            }
        }
    }
}

private enum class AccountLoginMethod { Qr, Sms }

@Composable
fun AccountScreen(
    viewModel: SiListenViewModel,
    padding: PaddingValues,
    onSearch: () -> Unit = { viewModel.selectTab(AppTab.Search) }
) {
    val state = viewModel.accountState
    val uiState = viewModel.uiState
    val user = state.loginState.user
    val context = LocalContext.current
    var loginMethod by remember { mutableStateOf(AccountLoginMethod.Sms) }
    val dark = uiState.themeSettings.resolveDarkTheme()
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val warningText = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00)
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val recentPlayedPlaylist = remember(uiState.recentPlayedSongs) {
        uiState.recentPlayedSongs.takeIf { it.isNotEmpty() }?.let { songs ->
            MusicPlaylist(
                id = "local-recent-played",
                title = "最近播放",
                subtitle = "${songs.size} 首最近听过的歌曲",
                coverUrl = songs.firstOrNull()?.coverUrl.orEmpty(),
                songs = songs,
                kind = PlaylistKind.Playlist
            )
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 156.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageTopTitle("资料库", dark = dark)
        }
        item {
            AccountSettingsPanel(
                dark = dark,
                onTheme = viewModel::openThemeSettings,
                onPlayback = viewModel::openPlaybackSettings,
                onSource = viewModel::openSourceSettings,
                onDonation = viewModel::openDonationSettings
            )
        }
        if (state.loginState.loggedIn && user != null) {
            item {
                AccountProfileHeader(
                    nickname = user.nickname,
                    avatarUrl = user.avatarUrl,
                    loading = uiState.isLibraryLoading,
                    dark = dark,
                    accent = MaterialTheme.colorScheme.primary,
                    onSearch = onSearch
                )
            }
            item {
                AccountShortcutStrip(
                    dark = dark,
                    onDaily = { uiState.dailyDiscovery?.let(viewModel::openPlaylist) ?: viewModel.refreshLibrary() },
                    onFm = { uiState.personalFm?.let(viewModel::openPlaylist) ?: viewModel.refreshLibrary() },
                    onPodcast = { uiState.podcasts?.let(viewModel::openPlaylist) ?: viewModel.refreshLibrary() },
                    onCloud = { uiState.cloudDrive?.let(viewModel::openPlaylist) ?: viewModel.refreshLibrary() }
                )
            }
            item {
                SectionBulletTitle("我的歌单", dark = dark)
            }
            recentPlayedPlaylist?.let { playlist ->
                item(key = "recent-played") {
                    AccountLibraryRow(
                        playlist = playlist,
                        subtitle = playlist.subtitle,
                        dark = dark,
                        onClick = { viewModel.openPlaylist(playlist) }
                    )
                }
            }
            if (uiState.userPlaylists.isEmpty() && recentPlayedPlaylist == null) {
                item {
                    AccountPlaceholderRow(
                        title = "还没有读到歌单",
                        subtitle = if (uiState.isLibraryLoading) "正在同步歌单..." else "登录后会在这里展示你的歌单",
                        dark = dark
                    )
                }
            } else {
                items(uiState.userPlaylists) { playlist ->
                    AccountLibraryRow(
                        playlist = playlist,
                        subtitle = playlist.subtitle,
                        dark = dark,
                        onClick = { viewModel.openPlaylist(playlist) }
                    )
                }
            }
            item {
                Surface(
                    color = cardColor,
                    border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = state.loginState.message,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryActionButton(
                                text = "同步内容",
                                dark = dark,
                                onClick = viewModel::syncAccountContent
                            )
                            PrimaryActionButton(
                                text = "退出登录",
                                onClick = viewModel::logoutNetease,
                                containerColor = Color(0xFFFF6B6B),
                                contentColor = Color(0xFF170505)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                AccountLoginHeader(
                    dark = dark,
                    accent = MaterialTheme.colorScheme.primary,
                    onOpenNeteaseApp = { openNeteaseCloudMusic(context) }
                )
            }
            item {
                SmsLoginCard(
                    phone = state.phone,
                    captcha = state.captcha,
                    sendingCode = state.sendingCode,
                    loggingIn = state.loggingIn,
                    cooldownSeconds = state.smsCooldownSeconds,
                    dark = dark,
                    mutedText = mutedText,
                    onPhoneChange = viewModel::updatePhone,
                    onCaptchaChange = viewModel::updateCaptcha,
                    onSendCode = viewModel::sendSmsCode,
                    onLogin = viewModel::loginNetease
                )
            }
            item {
                LoginStatusText(
                    text = state.loginState.message,
                    color = warningText,
                    dark = dark
                )
            }
            item {
                QrLoginDisclosure(
                    expanded = loginMethod == AccountLoginMethod.Qr,
                    dark = dark,
                    onToggle = {
                        loginMethod = if (loginMethod == AccountLoginMethod.Qr) {
                            AccountLoginMethod.Sms
                        } else {
                            AccountLoginMethod.Qr
                        }
                    }
                )
            }
            item {
                AnimatedVisibility(visible = loginMethod == AccountLoginMethod.Qr) {
                    QrLoginCard(
                        state = state.qrLogin,
                        dark = dark,
                        onCreate = viewModel::createQrLogin,
                        onOpenNeteaseApp = { openNeteaseCloudMusic(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalPlaylistLibrarySection(
    playlists: List<MusicPlaylist>,
    dark: Boolean,
    onCreate: (String) -> Unit,
    onOpenPlaylist: (MusicPlaylist) -> Unit,
    onDeletePlaylist: (MusicPlaylist) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var pendingDeletePlaylist by remember { mutableStateOf<MusicPlaylist?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionBulletTitle("本地歌单库", dark = dark)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                placeholder = { Text("新建本地歌单") },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = "创建",
                enabled = title.isNotBlank(),
                onClick = {
                    onCreate(title)
                    title = ""
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
        if (playlists.isEmpty()) {
            EmptyStateCard(
                title = "还没有本地歌单",
                subtitle = "创建后可以从歌曲菜单里加入本地歌单，不需要登录网易云。",
                dark = dark
            )
        } else {
            playlists.forEach { playlist ->
                AccountLibraryRow(
                    playlist = playlist,
                    subtitle = playlist.subtitle.ifBlank { "本地歌单 · ${playlist.songs.size} 首" },
                    dark = dark,
                    onClick = { onOpenPlaylist(playlist) },
                    onLongClick = {
                        if (playlist.kind == PlaylistKind.LocalPlaylist) {
                            pendingDeletePlaylist = playlist
                        }
                    }
                )
            }
        }
    }
    pendingDeletePlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = null },
            title = { Text("删除本地歌单？") },
            text = { Text("将删除「${playlist.title}」，歌单里的歌曲文件不会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeletePlaylist = null
                        onDeletePlaylist(playlist)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylist = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AccountSettingsPanel(
    dark: Boolean,
    onTheme: () -> Unit,
    onPlayback: () -> Unit,
    onSource: () -> Unit,
    onDonation: () -> Unit
) {
    val settingsItems = listOf(
        AccountSettingsAction("外观", Icons.Rounded.Settings, onTheme),
        AccountSettingsAction("播放", Icons.Rounded.MusicNote, onPlayback),
        AccountSettingsAction("音源", Icons.Rounded.Cloud, onSource),
        AccountSettingsAction("支持", Icons.Rounded.Favorite, onDonation)
    )
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionBulletTitle("设置", dark = dark)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                settingsItems.forEach { item ->
                    AccountSettingsTile(
                        item = item,
                        dark = dark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class AccountSettingsAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun AccountSettingsTile(
    item: AccountSettingsAction,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFF5F5F7),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.noRippleClick(shape = RoundedCornerShape(22.dp), onClick = item.onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = item.label,
                color = titleColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccountLoginHeader(
    dark: Boolean,
    accent: Color,
    onOpenNeteaseApp: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent,
                            accent.copy(alpha = 0.72f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "SiListen",
            color = titleColor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "登录网易云音乐，同步你的歌单和推荐",
            color = mutedText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "打开网易云音乐 App",
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(top = 12.dp)
                .noRippleClick(shape = RoundedCornerShape(999.dp), onClick = onOpenNeteaseApp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LoginStatusText(
    text: String,
    color: Color,
    dark: Boolean
) {
    if (text.isBlank()) return
    val container = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.80f)
    Surface(
        color = container,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFE9E9ED)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun QrLoginDisclosure(
    expanded: Boolean,
    dark: Boolean,
    onToggle: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6E7176)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFE9E9ED)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(22.dp), onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("QR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (expanded) "收起二维码登录" else "使用二维码登录",
                    color = titleColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "如果短信不方便，可以用官方 App 扫码确认",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = mutedText,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (expanded) 90f else 0f
                }
            )
        }
    }
}

@Composable
private fun SmsLoginCard(
    phone: String,
    captcha: String,
    sendingCode: Boolean,
    loggingIn: Boolean,
    cooldownSeconds: Int,
    dark: Boolean,
    mutedText: Color,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onLogin: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentOn = MaterialTheme.colorScheme.onPrimary
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val formContainer = if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.96f)
    Surface(
        color = formContainer,
        border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "手机号验证码登录",
                color = titleColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "先发送验证码，再用手机号和验证码登录网易云账号。",
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                placeholder = { Text("手机号") },
                shape = RoundedCornerShape(14.dp)
            )
            SmsCodeInput(
                value = captcha,
                dark = dark,
                onValueChange = onCaptchaChange
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(
                    text = when {
                        sendingCode -> "发送中"
                        cooldownSeconds > 0 -> "${cooldownSeconds}s 后重新获取"
                        else -> "获取验证码"
                    },
                    dark = dark,
                    onClick = onSendCode,
                    enabled = !sendingCode && cooldownSeconds == 0,
                    loading = sendingCode,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryActionButton(
                    text = if (loggingIn) "登录中" else "登录",
                    onClick = onLogin,
                    containerColor = accent,
                    contentColor = accentOn,
                    enabled = !loggingIn && !sendingCode,
                    loading = loggingIn,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SmsCodeInput(
    value: String,
    dark: Boolean,
    onValueChange: (String) -> Unit
) {
    val minVisibleSlots = 4
    val maxCodeLength = 6
    val code = value.filter { it.isDigit() }.take(maxCodeLength)
    val visibleSlots = if (code.length >= minVisibleSlots) maxCodeLength else minVisibleSlots
    val textColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFF9FA8A0) else Color(0xFF7A7D83)
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(code, selection = TextRange(code.length)))
    }
    LaunchedEffect(code) {
        if (code != fieldValue.text) {
            fieldValue = TextFieldValue(code, selection = TextRange(code.length))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "短信验证码（支持 4-6 位）",
            color = mutedText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        BasicTextField(
            value = fieldValue,
            onValueChange = { next ->
                val sanitized = next.text.filter { it.isDigit() }.take(maxCodeLength)
                fieldValue = TextFieldValue(sanitized, selection = TextRange(sanitized.length))
                if (sanitized != code) {
                    onValueChange(sanitized)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(visibleSlots) { index ->
                            val digit = code.getOrNull(index)?.toString().orEmpty()
                            val active = index == code.length && code.length < maxCodeLength
                            Surface(
                                color = if (dark) Color.White.copy(alpha = 0.07f) else Color(0xFFF8F8FA),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                    } else if (dark) {
                                        Color.White.copy(alpha = 0.10f)
                                    } else {
                                        Color(0xFFE2E3E7)
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = digit,
                                        color = textColor,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0f)
                    ) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun AccountProfileHeader(
    nickname: String,
    avatarUrl: String,
    loading: Boolean,
    dark: Boolean,
    accent: Color,
    onSearch: () -> Unit
) {
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF7A7A7A)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (avatarUrl.isBlank()) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = accent)
            }
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi", color = mutedText, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$nickname~",
                    color = accent,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = if (loading) "正在同步每日、FM、播客和歌单..." else "你的网易云主页",
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onSearch) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "搜索",
                tint = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
            )
        }
    }
}

@Composable
private fun AccountShortcutStrip(
    dark: Boolean,
    onDaily: () -> Unit,
    onFm: () -> Unit,
    onPodcast: () -> Unit,
    onCloud: () -> Unit
) {
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccountShortcutItem("每日", Icons.Rounded.Home, dark, onDaily)
            AccountShortcutItem("FM", Icons.Rounded.Radio, dark, onFm)
            AccountShortcutItem("播客", Icons.Rounded.Podcasts, dark, onPodcast)
            AccountShortcutItem("云盘", Icons.Rounded.Cloud, dark, onCloud)
        }
    }
}

@Composable
private fun RowScope.AccountShortcutItem(
    label: String,
    icon: ImageVector,
    dark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    Column(
        modifier = Modifier
            .weight(1f)
            .noRippleClick(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionBulletTitle(
    title: String,
    dark: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0xFFFF7C8F) else Color(0xFFFF5C7C))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun AccountLibraryRow(
    playlist: MusicPlaylist,
    subtitle: String,
    dark: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val container = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val muted = if (dark) Color(0xFFB8C1B9) else Color(0xFF6E7176)
    val shape = RoundedCornerShape(24.dp)
    val longClick = onLongClick
    val clickModifier = if (longClick == null) {
        Modifier.noRippleClick(shape = shape, onClick = onClick)
    } else {
        Modifier.pointerInput(onClick, longClick) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { longClick() }
            )
        }
    }
    Surface(
        color = container,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFECECEF)),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = playlist.coverUrl.ifBlank { null },
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "查看歌单", tint = muted)
        }
    }
}

@Composable
private fun AccountPlaceholderRow(
    title: String,
    subtitle: String,
    dark: Boolean
) {
    EmptyStateCard(
        title = title,
        subtitle = subtitle,
        dark = dark
    )
}

@Composable
private fun QrLoginCard(
    state: QrLoginUiState,
    dark: Boolean,
    onCreate: () -> Unit,
    onOpenNeteaseApp: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val accentOn = MaterialTheme.colorScheme.onPrimary
    val cardColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.White
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val pendingText = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00)
    val badgeTextColor = if (dark) Color(0xFF081109) else Color(0xFF111111)
    Surface(
        color = cardColor,
        border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.92f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("QR", color = if (dark) MaterialTheme.colorScheme.onPrimary else badgeTextColor, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "二维码登录",
                        color = titleColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text("使用网易云音乐 App 扫码确认", color = mutedText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (!state.qrImg.isNullOrBlank()) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    QrCodeImage(
                        dataUrl = state.qrImg,
                        onLongPress = { bitmap ->
                            val saved = saveBitmapToPictures(context, bitmap)
                            Toast.makeText(
                                context,
                                if (saved) "二维码已保存到图片/相册" else "二维码保存失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(184.dp)
                            .padding(14.dp)
                    )
                }
                Text(
                    text = "长按二维码保存到相册",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Text(
                text = state.message,
                color = if (state.polling) accent else pendingText,
                style = MaterialTheme.typography.bodyMedium
            )
            PrimaryActionButton(
                text = when {
                    state.loading -> "生成中"
                    state.qrImg.isNullOrBlank() -> "生成二维码"
                    else -> "刷新二维码"
                },
                onClick = onCreate,
                enabled = !state.loading,
                containerColor = accent,
                contentColor = accentOn
            )
            SecondaryActionButton(
                text = "打开网易云音乐 App 扫码确认",
                dark = dark,
                onClick = onOpenNeteaseApp
            )
            Text(
                text = "打开官方 App 后，请回到 SiListen 使用二维码或短信验证码完成登录。",
                color = mutedText.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun QrCodeImage(
    dataUrl: String?,
    modifier: Modifier = Modifier,
    onLongPress: (Bitmap) -> Unit = {}
) {
    val bitmap = remember(dataUrl) { decodeQrBitmap(dataUrl) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    if (imageBitmap == null || bitmap == null) {
        EmptyStateCard(
            title = "二维码加载失败",
            subtitle = "重新生成二维码后再试一次。",
            dark = false,
            modifier = modifier
        )
    } else {
        Image(
            bitmap = imageBitmap,
            contentDescription = "网易云登录二维码",
            modifier = modifier.pointerInput(bitmap) {
                detectTapGestures(
                    onLongPress = { onLongPress(bitmap) }
                )
            }
        )
    }
}

private fun decodeQrBitmap(dataUrl: String?): Bitmap? {
    return dataUrl?.substringAfter("base64,", missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }
        ?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
}

private fun saveBitmapToPictures(context: android.content.Context, bitmap: Bitmap): Boolean {
    val filename = "SiListen_QR_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SiListen")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    return runCatching {
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)
}

private fun openNeteaseCloudMusic(context: android.content.Context) {
    val packageName = "com.netease.cloudmusic"
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent == null) {
        Toast.makeText(context, "未检测到网易云音乐 App，请先安装或更新后再试", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(
            launchIntent
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onSuccess {
        Toast.makeText(context, "已打开网易云音乐，请回到 SiListen 完成登录", Toast.LENGTH_SHORT).show()
    }.onFailure { error ->
        val message = if (error is ActivityNotFoundException) {
            "无法打开网易云音乐 App，请确认已安装官方最新版"
        } else {
            "打开网易云音乐失败，请手动打开 App 后再回来登录"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
