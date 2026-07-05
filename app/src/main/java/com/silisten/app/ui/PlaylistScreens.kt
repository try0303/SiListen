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
import androidx.compose.foundation.text.BasicTextField
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
import com.silisten.app.ArtistPageTab
import com.silisten.app.ArtistSongsPageState
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
fun PlaylistDetailScreen(
    playlist: MusicPlaylist,
    route: PlaylistRoute,
    songSearchQuery: String,
    isLoading: Boolean,
    message: String?,
    commentSort: PlaylistCommentSort,
    comments: List<PlaylistComment>,
    commentCount: Int,
    isCommentsLoading: Boolean,
    commentsMessage: String?,
    isSubscribed: Boolean,
    isSubscriptionLoading: Boolean,
    canShowComments: Boolean,
    canShowSubscriptionAction: Boolean,
    isSubscriptionLocked: Boolean,
    dark: Boolean,
    glassy: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    isSongLiked: (Song) -> Boolean,
    isSongLikeLoading: (Song) -> Boolean,
    onToggleSongLike: (Song) -> Unit,
    onPlaySongNext: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onShowSongComments: (Song) -> Unit,
    onToggleSubscription: () -> Unit,
    onShowSongs: () -> Unit,
    onShowComments: () -> Unit,
    onRefreshComments: () -> Unit,
    onSongSearchQueryChange: (String) -> Unit,
    onCommentSortChange: (PlaylistCommentSort) -> Unit,
    reserveMiniPlayerSpace: Boolean
) {
    var showInlineSearch by remember(playlist.id) { mutableStateOf(songSearchQuery.isNotBlank()) }
    var actionSong by remember(playlist.id) { mutableStateOf<Song?>(null) }
    var pendingCancelSubscription by remember(playlist.id) { mutableStateOf(false) }
    val displayRoute = if (canShowComments) route else PlaylistRoute.Overview
    val pageColor = if (dark) Color(0xFF071008) else Color(0xFFF5F6F8)
    val cardColor = if (dark) Color(0xFF111A12) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val borderColor = if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)
    val filteredSongs = remember(playlist.songs, songSearchQuery) {
        val query = songSearchQuery.trim()
        if (query.isBlank()) {
            playlist.songs
        } else {
            playlist.songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        }
    }

    Surface(
        color = pageColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = if (reserveMiniPlayerSpace) 118.dp else 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(304.dp)
                    ) {
                        AsyncImage(
                            model = playlist.coverUrl.ifBlank { null },
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141414))
                                .blur(8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.30f),
                                            Color.Black.copy(alpha = 0.08f),
                                            pageColor.copy(alpha = 0.92f),
                                            pageColor
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Spacer(Modifier.height(46.dp))
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                AsyncImage(
                                    model = playlist.coverUrl.ifBlank { null },
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(Color(0xFF1ED760).copy(alpha = 0.16f))
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = playlist.subtitle,
                                        color = Color.White.copy(alpha = 0.76f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlaylistHeaderBadge(
                                            text = if (displayRoute == PlaylistRoute.Comments) "评论区" else "歌单详情"
                                        )
                                        PlaylistHeaderBadge(text = "${playlist.songs.size} 首")
                                        if (canShowComments && commentCount > 0) {
                                            PlaylistHeaderBadge(text = "$commentCount 条评论")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = cardColor,
                            border = BorderStroke(1.dp, borderColor),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = onPlayAll,
                                        enabled = playlist.songs.isNotEmpty() && !isLoading,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("播放全部")
                                    }
                                    if (canShowSubscriptionAction) {
                                        PlaylistActionChip(
                                            text = when {
                                                isSubscriptionLoading -> "处理中"
                                                isSubscriptionLocked -> "已收藏"
                                                isSubscribed -> "已收藏"
                                                else -> "收藏"
                                            },
                                            dark = dark,
                                            selected = isSubscribed || isSubscriptionLocked,
                                            enabled = !isSubscriptionLoading && !isSubscriptionLocked,
                                            onClick = {
                                                if (isSubscribed) {
                                                    pendingCancelSubscription = true
                                                } else {
                                                    onToggleSubscription()
                                                }
                                            },
                                            leading = {
                                                Icon(
                                                    Icons.Rounded.Favorite,
                                                    contentDescription = null,
                                                    tint = if (isSubscribed || isSubscriptionLocked) Color(0xFFFF5C7C) else titleColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                    if (canShowComments) {
                                        PlaylistActionChip(
                                            text = if (commentCount > 0) "评论 $commentCount" else "评论",
                                            dark = dark,
                                            selected = displayRoute == PlaylistRoute.Comments,
                                            onClick = onShowComments
                                        )
                                    }
                                }
                                if (canShowComments) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        PlaylistRouteChip(
                                            text = "歌曲 ${playlist.songs.size}",
                                            selected = displayRoute == PlaylistRoute.Overview,
                                            dark = dark,
                                            modifier = Modifier.weight(1f),
                                            onClick = onShowSongs
                                        )
                                        PlaylistRouteChip(
                                            text = if (commentCount > 0) "评论 $commentCount" else "评论",
                                            selected = displayRoute == PlaylistRoute.Comments,
                                            dark = dark,
                                            modifier = Modifier.weight(1f),
                                            onClick = onShowComments
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (displayRoute == PlaylistRoute.Overview) {
                    if (isLoading) {
                        item {
                            LoadingStateCard(
                                text = "正在加载歌单详情...",
                                dark = dark,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    message?.let {
                        item {
                            Text(
                                text = it,
                                color = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    if (!isLoading && playlist.songs.isEmpty() && message == null) {
                        item {
                            EmptyStateCard(
                                title = "暂时没有可展示的歌曲",
                                subtitle = "这个歌单现在还没有可以展示的内容。",
                                dark = dark,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    if (!isLoading && filteredSongs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "没有找到匹配的歌曲",
                                subtitle = "换个关键词试试，或者先清空筛选。",
                                dark = dark,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    items(filteredSongs) { song ->
                        Surface(
                            color = cardColor,
                            border = BorderStroke(1.dp, borderColor),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            SongRow(
                                song = song,
                                liked = isSongLiked(song),
                                likeLoading = isSongLikeLoading(song),
                                onClick = { onSongClick(song) },
                                onLikeClick = if (song.sourceId != "local") ({ onToggleSongLike(song) }) else null,
                                onMoreClick = { actionSong = song }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (commentSort == PlaylistCommentSort.Hot) "热门评论" else "最新评论",
                                color = titleColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            PlaylistActionChip(
                                text = "刷新",
                                dark = dark,
                                selected = false,
                                onClick = onRefreshComments
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlaylistRouteChip(
                                text = "热门评论",
                                selected = commentSort == PlaylistCommentSort.Hot,
                                dark = dark,
                                modifier = Modifier.weight(1f),
                                onClick = { onCommentSortChange(PlaylistCommentSort.Hot) }
                            )
                            PlaylistRouteChip(
                                text = "最新评论",
                                selected = commentSort == PlaylistCommentSort.Latest,
                                dark = dark,
                                modifier = Modifier.weight(1f),
                                onClick = { onCommentSortChange(PlaylistCommentSort.Latest) }
                            )
                        }
                    }
                    if (isCommentsLoading) {
                        item {
                            LoadingStateCard(
                                text = "正在加载评论...",
                                dark = dark,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    commentsMessage?.let {
                        item {
                            Text(
                                text = it,
                                color = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    }
                    items(comments) { comment ->
                        PlaylistCommentCard(
                            comment = comment,
                            dark = dark,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
            }

            PlaylistFloatingBackButton(
                dark = dark,
                glassy = glassy,
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            )
            PlaylistFloatingSearchControl(
                query = songSearchQuery,
                expanded = showInlineSearch,
                dark = dark,
                glassy = glassy,
                enabled = playlist.songs.isNotEmpty(),
                onExpandedChange = { expanded ->
                    if (expanded && displayRoute != PlaylistRoute.Overview) {
                        onShowSongs()
                    }
                    showInlineSearch = expanded
                    if (!expanded) onSongSearchQueryChange("")
                },
                onQueryChange = onSongSearchQueryChange,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
    if (pendingCancelSubscription) {
        AlertDialog(
            onDismissRequest = { pendingCancelSubscription = false },
            title = { Text("取消收藏歌单？") },
            text = { Text("取消后，这个歌单会从账号收藏中移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCancelSubscription = false
                        onToggleSubscription()
                    }
                ) {
                    Text("取消收藏")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelSubscription = false }) {
                    Text("再想想")
                }
            }
        )
    }
    actionSong?.let { song ->
        SongActionSheetModal(
            song = song,
            dark = dark,
            onDismiss = { actionSong = null },
            onPlayNext = {
                actionSong = null
                onPlaySongNext(song)
            },
            onAddToPlaylist = {
                actionSong = null
                onAddSongToPlaylist(song)
            },
            onShowComments = {
                actionSong = null
                onShowSongComments(song)
            }
        )
    }
}

private enum class ArtistDetailTab(val pageTab: ArtistPageTab, val label: String) {
    Detail(ArtistPageTab.Detail, "详情"),
    Hot(ArtistPageTab.Hot, "热门"),
    Songs(ArtistPageTab.Songs, "单曲"),
    Albums(ArtistPageTab.Albums, "专辑")
}

private fun ArtistPageTab.toArtistDetailTab(): ArtistDetailTab =
    ArtistDetailTab.entries.firstOrNull { it.pageTab == this } ?: ArtistDetailTab.Detail

@Composable
fun ArtistDetailScreen(
    artist: MusicPlaylist,
    isLoading: Boolean,
    message: String?,
    selectedPageTab: ArtistPageTab,
    artistSongsPage: ArtistSongsPageState,
    dark: Boolean,
    glassy: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onTabSelected: (ArtistPageTab) -> Unit,
    onLoadMoreArtistSongs: () -> Unit,
    onSongClick: (Song) -> Unit,
    isSongLiked: (Song) -> Boolean,
    isSongLikeLoading: (Song) -> Boolean,
    onToggleSongLike: (Song) -> Unit,
    onPlaySongNext: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onShowSongComments: (Song) -> Unit,
    onAlbumClick: (MusicPlaylist) -> Unit,
    reserveMiniPlayerSpace: Boolean
) {
    val artistTabs = ArtistDetailTab.entries
    val initialPage = artistTabs.indexOf(selectedPageTab.toArtistDetailTab()).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { artistTabs.size })
    val scope = rememberCoroutineScope()
    val selectedTab = artistTabs[pagerState.currentPage.coerceIn(0, artistTabs.lastIndex)]
    val detailListState = rememberLazyListState()
    val hotListState = rememberLazyListState()
    val artistSongListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    var actionSong by remember(artist.id) { mutableStateOf<Song?>(null) }
    val pageColor = if (dark) Color(0xFF080B0C) else Color(0xFFF6F6F7)
    val cardColor = if (dark) Color(0xFF141718) else Color.White
    val titleColor = if (dark) Color(0xFFF7F8F8) else Color(0xFF141414)
    val mutedText = if (dark) Color.White.copy(alpha = 0.62f) else Color(0xFF666A70)
    val borderColor = if (dark) Color.White.copy(alpha = 0.11f) else Color.Black.copy(alpha = 0.05f)
    val hotSongs = remember(artist.songs) { artist.songs }
    val artistSongs = if (artistSongsPage.artistId == artist.id) artistSongsPage.songs else emptyList()
    var showArtistSongSearch by remember(artist.id) { mutableStateOf(false) }
    var artistSongSearchQuery by remember(artist.id) { mutableStateOf("") }
    val filteredArtistSongs = remember(artistSongs, artistSongSearchQuery) {
        val query = artistSongSearchQuery.trim()
        if (query.isBlank()) {
            artistSongs
        } else {
            artistSongs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        }
    }
    val description = artist.description.ifBlank {
        "暂时没有歌手简介。可以先查看热门歌曲和专辑，后续同步到更完整的网易云资料。"
    }
    LaunchedEffect(selectedPageTab, artist.id) {
        val targetPage = artistTabs.indexOf(selectedPageTab.toArtistDetailTab()).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState, artist.id) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                artistTabs.getOrNull(page)?.pageTab?.let(onTabSelected)
            }
    }
    LaunchedEffect(selectedPageTab, artist.id) {
        if (selectedPageTab == ArtistPageTab.Songs) {
            onLoadMoreArtistSongs()
        }
    }
    LaunchedEffect(
        artistSongListState,
        selectedPageTab,
        artistSongs.size,
        artistSongsPage.hasMore,
        artistSongsPage.isLoading
    ) {
        snapshotFlow {
            val layoutInfo = artistSongListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            layoutInfo.totalItemsCount > 0 && lastVisible >= layoutInfo.totalItemsCount - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest {
                if (selectedPageTab == ArtistPageTab.Songs && artistSongsPage.hasMore && !artistSongsPage.isLoading) {
                    onLoadMoreArtistSongs()
                }
            }
    }

    Surface(color = pageColor, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            ArtistDetailTopBar(
                title = artist.title,
                dark = dark,
                onBack = onBack,
                showSearch = selectedTab.pageTab == ArtistPageTab.Songs,
                searchQuery = artistSongSearchQuery,
                searchExpanded = showArtistSongSearch,
                onSearchExpandedChange = { expanded ->
                    showArtistSongSearch = expanded
                    if (!expanded) artistSongSearchQuery = ""
                },
                onSearchQueryChange = { artistSongSearchQuery = it }
            )
            Spacer(Modifier.height(12.dp))
            ArtistDetailTabs(
                selected = selectedTab,
                dark = dark,
                onSelect = { tab ->
                    scope.launch {
                        pagerState.animateScrollToPage(artistTabs.indexOf(tab).coerceAtLeast(0))
                        onTabSelected(tab.pageTab)
                    }
                }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val currentTab = artistTabs[page]
                val currentListState = when (currentTab) {
                    ArtistDetailTab.Detail -> detailListState
                    ArtistDetailTab.Hot -> hotListState
                    ArtistDetailTab.Songs -> artistSongListState
                    ArtistDetailTab.Albums -> albumListState
                }
                LazyColumn(
                    state = currentListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = if (reserveMiniPlayerSpace) 126.dp else 42.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ArtistHeroCard(
                            artist = artist,
                            dark = dark,
                            glassy = glassy,
                            titleColor = titleColor,
                            mutedText = mutedText,
                            cardColor = cardColor,
                            borderColor = borderColor
                        )
                    }
                    if (isLoading) {
                        item {
                            LoadingStateCard(
                                text = "正在加载歌手详情...",
                                dark = dark
                            )
                        }
                    }
                    message?.let {
                        item {
                            Text(
                                text = it,
                                color = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    when (currentTab) {
                        ArtistDetailTab.Detail -> {
                            item {
                                ArtistDescriptionCard(
                                    description = description,
                                    dark = dark,
                                    titleColor = titleColor,
                                    mutedText = mutedText,
                                    cardColor = cardColor,
                                    borderColor = borderColor
                                )
                            }
                            if (hotSongs.isNotEmpty()) {
                                item {
                                    ArtistSectionHeader(
                                        title = "热门歌曲",
                                        action = "播放",
                                        dark = dark,
                                        onAction = onPlayAll
                                    )
                                }
                                itemsIndexed(hotSongs.take(5), key = { _, song -> song.id }) { index, song ->
                                    ArtistSongSurface(
                                        index = index + 1,
                                        song = song,
                                        dark = dark,
                                        cardColor = cardColor,
                                        borderColor = borderColor,
                                        liked = isSongLiked(song),
                                        likeLoading = isSongLikeLoading(song),
                                        onClick = { onSongClick(song) },
                                        onLikeClick = if (song.sourceId != "local") ({ onToggleSongLike(song) }) else null,
                                        onMoreClick = { actionSong = song }
                                    )
                                }
                            }
                        }
                        ArtistDetailTab.Hot -> {
                            item {
                                ArtistSectionHeader(
                                    title = "热门歌曲",
                                    action = "播放全部",
                                    dark = dark,
                                    onAction = onPlayAll
                                )
                            }
                            if (!isLoading && hotSongs.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "暂时没有热门歌曲",
                                        subtitle = "网易云暂时没有返回这个歌手的热门歌曲。",
                                        dark = dark
                                    )
                                }
                            }
                            itemsIndexed(hotSongs, key = { _, song -> song.id }) { index, song ->
                                ArtistSongSurface(
                                    index = index + 1,
                                    song = song,
                                    dark = dark,
                                    cardColor = cardColor,
                                    borderColor = borderColor,
                                    liked = isSongLiked(song),
                                    likeLoading = isSongLikeLoading(song),
                                    onClick = { onSongClick(song) },
                                    onLikeClick = if (song.sourceId != "local") ({ onToggleSongLike(song) }) else null,
                                    onMoreClick = { actionSong = song }
                                )
                            }
                        }
                        ArtistDetailTab.Songs -> {
                            item {
                                ArtistSectionHeader(
                                    title = "单曲",
                                    action = "播放全部",
                                    dark = dark,
                                    onAction = onPlayAll
                                )
                            }
                            artistSongsPage.message?.let { pageMessage ->
                                item {
                                    Text(
                                        text = pageMessage,
                                        color = if (dark) Color(0xFFFFD166) else Color(0xFF8A5A00),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (!artistSongsPage.isLoading && filteredArtistSongs.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "暂时没有单曲",
                                        subtitle = "这里会展示网易云返回的歌手全部单曲。",
                                        dark = dark
                                    )
                                }
                            }
                            itemsIndexed(filteredArtistSongs, key = { _, song -> song.id }) { index, song ->
                                ArtistSongSurface(
                                    index = index + 1,
                                    song = song,
                                    dark = dark,
                                    cardColor = cardColor,
                                    borderColor = borderColor,
                                    liked = isSongLiked(song),
                                    likeLoading = isSongLikeLoading(song),
                                    onClick = { onSongClick(song) },
                                    onLikeClick = if (song.sourceId != "local") ({ onToggleSongLike(song) }) else null,
                                    onMoreClick = { actionSong = song }
                                )
                            }
                            if (artistSongsPage.isLoading || artistSongsPage.hasMore) {
                                item {
                                    LoadingStateCard(
                                        text = if (artistSongsPage.isLoading) "正在加载更多单曲..." else "上滑继续加载更多单曲",
                                        dark = dark
                                    )
                                }
                            }
                        }
                        ArtistDetailTab.Albums -> {
                            item {
                                ArtistSectionHeader(
                                    title = "专辑",
                                    action = null,
                                    dark = dark,
                                    onAction = {}
                                )
                            }
                            if (!isLoading && artist.albums.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "暂时没有专辑",
                                        subtitle = "网易云暂时没有返回这个歌手的专辑列表。",
                                        dark = dark
                                    )
                                }
                            }
                            items(artist.albums, key = { it.id }) { album ->
                                ArtistAlbumRow(
                                    album = album,
                                    dark = dark,
                                    cardColor = cardColor,
                                    borderColor = borderColor,
                                    onClick = { onAlbumClick(album) }
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
                onPlaySongNext(song)
            },
            onAddToPlaylist = {
                actionSong = null
                onAddSongToPlaylist(song)
            },
            onShowComments = {
                actionSong = null
                onShowSongComments(song)
            }
        )
    }
}

@Composable
private fun ArtistDetailTopBar(
    title: String,
    dark: Boolean,
    onBack: () -> Unit,
    showSearch: Boolean = false,
    searchQuery: String = "",
    searchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    val titleColor = if (dark) Color(0xFFF7F8F8) else Color(0xFF141414)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f))
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = titleColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showSearch) {
            Spacer(Modifier.width(10.dp))
            ArtistSongSearchControl(
                query = searchQuery,
                expanded = searchExpanded,
                dark = dark,
                onExpandedChange = onSearchExpandedChange,
                onQueryChange = onSearchQueryChange
            )
        }
    }
}

@Composable
private fun ArtistSongSearchControl(
    query: String,
    expanded: Boolean,
    dark: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit
) {
    val width by animateDpAsState(
        targetValue = if (expanded) 184.dp else 46.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "artist-song-search-width"
    )
    val fieldAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = if (expanded) 180 else 90),
        label = "artist-song-search-alpha"
    )
    val iconTint = if (dark) Color.White else Color(0xFF111111)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .width(width)
            .height(46.dp)
            .shadow(if (dark) 0.dp else 14.dp, RoundedCornerShape(999.dp), clip = false)
            .noRippleClick(shape = RoundedCornerShape(999.dp)) {
                if (!expanded) onExpandedChange(true)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "搜索歌手单曲",
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .noRippleClick(shape = CircleShape) {
                        onExpandedChange(!expanded)
                    }
            )
            if (expanded) {
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).alpha(fieldAlpha)) {
                    if (query.isBlank()) {
                        Text(
                            text = "搜索单曲",
                            color = iconTint.copy(alpha = 0.42f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = iconTint,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailTabs(
    selected: ArtistDetailTab,
    dark: Boolean,
    onSelect: (ArtistDetailTab) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val normal = if (dark) Color.White.copy(alpha = 0.62f) else Color(0xFF545860)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistDetailTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .noRippleClick(shape = RoundedCornerShape(16.dp)) { onSelect(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab.label,
                    color = if (active) accent else normal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (active) FontWeight.Black else FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(if (active) 30.dp else 0.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) accent else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun ArtistHeroCard(
    artist: MusicPlaylist,
    dark: Boolean,
    glassy: Boolean,
    titleColor: Color,
    mutedText: Color,
    cardColor: Color,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 62.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LiquidGlassPane(
            enabled = glassy,
            dark = dark,
            shape = RoundedCornerShape(30.dp),
            cornerRadius = 30.dp,
            tintAlpha = if (dark) 0.12f else 0.10f,
            blurRadius = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 58.dp)
        ) {
            Surface(
                color = cardColor.copy(alpha = if (dark) 0.78f else 0.86f),
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 88.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = artist.title,
                        color = titleColor,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = artist.artistPlatformSummary(),
                        color = mutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ArtistStatBlock("${artist.albumCount.coerceAtLeast(artist.albums.size)}", "专辑", titleColor, mutedText)
                        ArtistStatBlock("${artist.totalArtistSongCount()}", "单曲", titleColor, mutedText)
                        ArtistStatBlock("${artist.mvCount}", "MV", titleColor, mutedText)
                    }
                }
            }
        }
        AsyncImage(
            model = artist.coverUrl.ifBlank { null },
            contentDescription = artist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222))
                .shadow(18.dp, CircleShape, clip = false)
        )
    }
}

private fun MusicPlaylist.artistPlatformSummary(): String {
    val count = totalArtistSongCount()
    return listOfNotNull(
        count.takeIf { it > 0 }?.let { "$it 首单曲" }
    ).joinToString(" · ").ifBlank { "歌手" }
}

private fun MusicPlaylist.totalArtistSongCount(): Int =
    songCount.coerceAtLeast(songs.size)

@Composable
private fun ArtistStatBlock(
    value: String,
    label: String,
    titleColor: Color,
    mutedText: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = titleColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = mutedText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ArtistDescriptionCard(
    description: String,
    dark: Boolean,
    titleColor: Color,
    mutedText: Color,
    cardColor: Color,
    borderColor: Color
) {
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "歌手简介",
                color = titleColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = description,
                color = if (dark) mutedText.copy(alpha = 0.92f) else Color(0xFF4E5259),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun ArtistSectionHeader(
    title: String,
    action: String?,
    dark: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (dark) Color(0xFFF7F8F8) else Color(0xFF141414),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            PlaylistActionChip(
                text = action,
                dark = dark,
                selected = false,
                onClick = onAction
            )
        }
    }
}

@Composable
private fun ArtistSongSurface(
    index: Int,
    song: Song,
    dark: Boolean,
    cardColor: Color,
    borderColor: Color,
    liked: Boolean,
    likeLoading: Boolean,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)?,
    onMoreClick: () -> Unit
) {
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = index.toString(),
                color = if (dark) Color.White.copy(alpha = 0.72f) else Color(0xFF737780),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(42.dp)
            )
            Box(Modifier.weight(1f)) {
                SongRow(
                    song = song,
                    liked = liked,
                    likeLoading = likeLoading,
                    onClick = onClick,
                    onLikeClick = onLikeClick,
                    onMoreClick = onMoreClick
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumRow(
    album: MusicPlaylist,
    dark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF7F8F8) else Color(0xFF141414)
    val mutedText = if (dark) Color.White.copy(alpha = 0.58f) else Color(0xFF6B6F75)
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(22.dp), onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (dark) Color(0xFF252525) else Color(0xFFE9EAEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = null,
                    tint = if (dark) Color.White.copy(alpha = 0.66f) else Color(0xFF757981),
                    modifier = Modifier.size(30.dp)
                )
                if (album.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = album.subtitle.ifBlank { "专辑" },
                    color = mutedText,
                    style = MaterialTheme.typography.bodyMedium,
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
}

@Composable
private fun PlaylistFloatingSearchControl(
    query: String,
    expanded: Boolean,
    dark: Boolean,
    glassy: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val controlWidth by animateDpAsState(
        targetValue = if (expanded) 248.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "playlist-search-width"
    )
    val fieldAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = if (expanded) 180 else 90),
        label = "playlist-search-field-alpha"
    )
    val iconTint = if (dark) Color.White else Color(0xFF111111)
    val containerColor = if (dark) Color(0xF0222528) else Color.White.copy(alpha = 0.96f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)
    val disabledAlpha = if (enabled) 1f else 0.42f

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(end = 18.dp, top = 10.dp)
            .zIndex(24f),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .width(controlWidth)
                .shadow(if (glassy) 10.dp else 4.dp, RoundedCornerShape(999.dp), clip = false)
                .alpha(disabledAlpha)
                .then(
                    if (expanded || !enabled) {
                        Modifier
                    } else {
                        Modifier.noRippleClick(shape = RoundedCornerShape(999.dp)) {
                            onExpandedChange(true)
                        }
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "搜索当前歌单",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                if (expanded) {
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = iconTint,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .alpha(fieldAlpha),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isBlank()) {
                                    Text(
                                        text = "搜索歌单",
                                        color = iconTint.copy(alpha = 0.46f),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "收起搜索",
                        tint = iconTint.copy(alpha = 0.72f),
                        modifier = Modifier
                            .size(32.dp)
                            .noRippleClick(shape = CircleShape) {
                                onExpandedChange(false)
                            }
                            .padding(5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistFloatingBackButton(
    dark: Boolean,
    glassy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 18.dp, top = 10.dp)
            .zIndex(4f)
    ) {
        Surface(
            color = if (dark) Color(0xF0222528) else Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)),
            shape = CircleShape,
            modifier = Modifier
                .shadow(if (glassy) 10.dp else 4.dp, CircleShape, clip = false)
                .noRippleClick(shape = CircleShape, onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = if (dark) Color.White else Color(0xFF111111)
                )
            }
        }
    }
}

@Composable
fun PlaylistActionChip(
    text: String,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.22f else 0.16f)
        dark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White
    }
    val border = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        dark -> Color.White.copy(alpha = 0.12f)
        else -> Color(0xFFE7E7EA)
    }
    Surface(
        color = container,
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(999.dp),
        modifier = if (enabled) {
            Modifier.noRippleClick(shape = RoundedCornerShape(999.dp), onClick = onClick)
        } else {
            Modifier.alpha(0.56f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leading?.invoke()
            Text(
                text = text,
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlaylistHeaderBadge(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PlaylistRouteChip(
    text: String,
    selected: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.20f else 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "playlist-route-chip"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "playlist-route-chip-scale"
    )
    Surface(
        color = container,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            else if (dark) Color.White.copy(alpha = 0.12f)
            else Color(0xFFE7E7EA)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlaylistCommentCard(
    comment: PlaylistComment,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColor = if (dark) Color(0xFF111A12) else Color.White
    val borderColor = if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6B6F75)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 520f),
        label = "comment-card-scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 520f),
        label = "comment-card-alpha"
    )
    Surface(
        color = if (pressed) cardColor.copy(alpha = if (dark) 0.92f else 0.98f) else cardColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.authorAvatarUrl.ifBlank { null },
                    contentDescription = comment.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = comment.authorName,
                        color = titleColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = comment.timeLabel,
                        color = mutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF6B7D),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = comment.likedCount.toString(),
                        color = mutedText,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                text = comment.content,
                color = titleColor,
                style = MaterialTheme.typography.bodyMedium
            )
            if (comment.replyCount > 0 || comment.likedCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (comment.replyCount > 0) {
                        Text(
                            text = "${comment.replyCount} 条回复",
                            color = mutedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (comment.likedCount > 0) {
                        Text(
                            text = "喜欢 ${comment.likedCount}",
                            color = mutedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
