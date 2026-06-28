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
            item {
                SectionTitle("今日播放", dark = dark)
            }
            val songs = uiState.dailyDiscovery?.songs?.ifEmpty { null }
                ?: uiState.featured.firstOrNull()?.songs.orEmpty()
            items(songs) { song ->
                SongRow(
                    song = song,
                    liked = viewModel.isSongLiked(song),
                    likeLoading = viewModel.isSongLikeLoading(song),
                    onClick = { viewModel.playSong(song) },
                    onLikeClick = if (song.sourceId == "netease") ({ viewModel.toggleSongLike(song) }) else null
                )
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
}

@Composable
fun SearchScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    padding: PaddingValues
) {
    val dark = uiState.themeSettings.resolveDarkTheme()
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
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
            PageHeroCard(
                title = "搜索",
                subtitle = "歌曲、歌手、专辑和歌单入口都会从这里收进同一套搜索体验。",
                dark = dark,
                trailing = {
                    SearchFloatingGlyph(dark = dark)
                }
            )
        }
        item {
            Surface(
                color = cardColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("歌曲、歌手或专辑") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                        SearchActionButton(
                            dark = dark,
                            loading = uiState.isSearching,
                            onClick = viewModel::runSearch
                        )
                }
            }
        }
        item {
            if (uiState.isSearching) {
                StatusMessageCard(
                    text = "正在搜索网易云...",
                    dark = dark
                )
            }
        }
        items(uiState.searchResults) { song ->
            Surface(
                color = cardColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFECECEF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SongRow(
                    song = song,
                    liked = viewModel.isSongLiked(song),
                    likeLoading = viewModel.isSongLikeLoading(song),
                    onClick = { viewModel.playSong(song) },
                    onLikeClick = if (song.sourceId == "netease") ({ viewModel.toggleSongLike(song) }) else null
                )
            }
        }
    }
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
            PageHeroCard(
                title = "音乐库",
                subtitle = "这里先专注本地音乐和你的媒体库入口，音源切换移到设置里统一管理。",
                dark = dark
            )
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
                            Text("扫描设备中的音频文件并加入 SiListen。", color = mutedText, style = MaterialTheme.typography.bodySmall)
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
                                        songs = uiState.localSongs
                                    )
                                )
                            }
                        )
                        uiState.localSongs.take(5).forEach { song ->
                            SongRow(song = song, liked = false, likeLoading = false, onClick = { viewModel.playSong(song) })
                        }
                    }
                }
            }
        }
    }
}

private enum class AccountLoginMethod { Qr, Sms }

@Composable
fun AccountScreen(viewModel: SiListenViewModel, padding: PaddingValues) {
    val state = viewModel.accountState
    val uiState = viewModel.uiState
    val user = state.loginState.user
    val context = LocalContext.current
    var loginMethod by remember { mutableStateOf(AccountLoginMethod.Qr) }
    val dark = uiState.themeSettings.resolveDarkTheme()
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val warningText = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00)
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageTopTitle("资料库", dark = dark)
        }
        item {
            AccountLocalMusicCard(
                uiState = uiState,
                dark = dark,
                onScan = { localPermissionLauncher.launch(audioPermission) },
                onOpenAll = {
                    viewModel.openPlaylist(
                        MusicPlaylist(
                            id = "local-library",
                            title = "本地音乐",
                            subtitle = uiState.localMusicMessage,
                            coverUrl = "",
                            songs = uiState.localSongs
                        )
                    )
                },
                onPlaySong = viewModel::playSong
            )
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
                    onSearch = { viewModel.selectTab(AppTab.Search) }
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
                SectionBulletTitle("喜欢的音乐", dark = dark)
            }
            item {
                val liked = uiState.likedSongs
                if (liked != null) {
                    AccountLibraryRow(
                        playlist = liked,
                        subtitle = if (liked.songs.isEmpty()) liked.subtitle else "${liked.songs.size} 首",
                        dark = dark,
                        onClick = { viewModel.openPlaylist(liked) }
                    )
                } else {
                    AccountPlaceholderRow(
                        title = "喜欢的音乐",
                        subtitle = if (uiState.isLibraryLoading) "正在同步喜欢列表..." else "点击刷新后读取喜欢列表",
                        dark = dark
                    )
                }
            }
            item {
                SectionBulletTitle("我的歌单", dark = dark)
            }
            if (uiState.userPlaylists.isEmpty()) {
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
                                onClick = viewModel::refreshLibrary
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
                AccountLoginHero(
                    dark = dark,
                    accent = MaterialTheme.colorScheme.primary,
                    accentOn = MaterialTheme.colorScheme.onPrimary,
                    statusText = state.loginState.message,
                    warningText = warningText,
                    onOpenNeteaseApp = { openNeteaseCloudMusic(context) }
                )
            }
            item {
                LoginMethodSelector(
                    selected = loginMethod,
                    dark = dark,
                    onSelect = { loginMethod = it }
                )
            }
            item {
                when (loginMethod) {
                    AccountLoginMethod.Qr -> QrLoginCard(
                        state = state.qrLogin,
                        dark = dark,
                        onCreate = viewModel::createQrLogin,
                        onOpenNeteaseApp = { openNeteaseCloudMusic(context) }
                    )
                    AccountLoginMethod.Sms -> SmsLoginCard(
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
            }
        }
    }
}

@Composable
private fun AccountLocalMusicCard(
    uiState: SiListenUiState,
    dark: Boolean,
    onScan: () -> Unit,
    onOpenAll: () -> Unit,
    onPlaySong: (Song) -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF8BD3FF).copy(alpha = if (dark) 0.22f else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Color(0xFF35A6D8))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "本地音乐",
                        color = titleColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = uiState.localMusicMessage,
                        color = mutedText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PrimaryActionButton(
                    text = "扫描",
                    onClick = onScan,
                    containerColor = Color(0xFF8BD3FF),
                    contentColor = Color(0xFF061018)
                )
            }
            if (uiState.localSongs.isNotEmpty()) {
                SecondaryActionButton(
                    text = "查看全部本地歌曲",
                    dark = dark,
                    onClick = onOpenAll,
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.localSongs.take(3).forEach { song ->
                    SongRow(
                        song = song,
                        liked = false,
                        likeLoading = false,
                        onClick = { onPlaySong(song) }
                    )
                }
            }
        }
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
private fun AccountLoginHero(
    dark: Boolean,
    accent: Color,
    accentOn: Color,
    statusText: String,
    warningText: Color,
    onOpenNeteaseApp: () -> Unit
) {
    val cardColor = if (dark) Color.White.copy(alpha = 0.10f) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(20.dp))
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
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accentOn, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "网易云账号登录",
                        color = titleColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "登录后直接同步每日推荐、FM、云盘和个人歌单。",
                        color = secondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Surface(
                color = accent.copy(alpha = if (dark) 0.16f else 0.10f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "支持二维码和短信验证码两种方式",
                        color = if (dark) Color.White else Color(0xFF1B1B1F),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "流程参考 bujuan，但视觉上收成更正常的原生登录界面。",
                        color = secondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = statusText,
                color = warningText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            PrimaryActionButton(
                text = "打开网易云音乐 App",
                onClick = onOpenNeteaseApp,
                containerColor = accent,
                contentColor = accentOn,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoginMethodSelector(
    selected: AccountLoginMethod,
    dark: Boolean,
    onSelect: (AccountLoginMethod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.92f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            AccountLoginMethod.Qr to "二维码登录",
            AccountLoginMethod.Sms to "短信验证码"
        ).forEach { item ->
            val isSelected = item.first == selected
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .noRippleClick(shape = RoundedCornerShape(16.dp)) { onSelect(item.first) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.second,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                    )
                }
            }
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
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.10f) else Color.White,
        border = if (dark) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else BorderStroke(1.dp, Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = captcha,
                onValueChange = onCaptchaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("短信验证码") },
                shape = RoundedCornerShape(16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(
                    text = when {
                        sendingCode -> "发送中"
                        cooldownSeconds > 0 -> "${cooldownSeconds}s"
                        else -> "获取验证码"
                    },
                    dark = dark,
                    onClick = onSendCode,
                    enabled = !sendingCode && cooldownSeconds == 0,
                    loading = sendingCode,
                    modifier = Modifier.weight(1f)
                )
                PrimaryActionButton(
                    text = if (loggingIn) "登录中" else "登录",
                    onClick = onLogin,
                    containerColor = accent,
                    contentColor = accentOn,
                    enabled = !loggingIn && !sendingCode,
                    loading = loggingIn,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
    onClick: () -> Unit
) {
    val container = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val muted = if (dark) Color(0xFFB8C1B9) else Color(0xFF6E7176)
    Surface(
        color = container,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFECECEF)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(24.dp), onClick = onClick)
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
            Column(horizontalAlignment = Alignment.End) {
                Text("查看歌单", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = "查看歌单", tint = muted)
            }
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
                text = "网易云没有公开稳定的第三方授权回调；这里会打开官方 App，登录态仍通过二维码或短信接口确认。",
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
        Toast.makeText(context, "未检测到网易云音乐 App，请先安装后再扫码", Toast.LENGTH_SHORT).show()
        return
    }
    context.startActivity(
        launchIntent
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
