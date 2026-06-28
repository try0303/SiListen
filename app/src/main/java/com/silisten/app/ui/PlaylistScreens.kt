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
    dark: Boolean,
    glassy: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    isSongLiked: (Song) -> Boolean,
    isSongLikeLoading: (Song) -> Boolean,
    onToggleSongLike: (Song) -> Unit,
    onToggleSubscription: () -> Unit,
    onShowSongs: () -> Unit,
    onShowComments: () -> Unit,
    onRefreshComments: () -> Unit,
    onSongSearchQueryChange: (String) -> Unit,
    onCommentSortChange: (PlaylistCommentSort) -> Unit,
    reserveMiniPlayerSpace: Boolean
) {
    var showInlineSearch by remember(playlist.id) { mutableStateOf(songSearchQuery.isNotBlank()) }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.animateContentSize(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)
                                )
                            ) {
                                Spacer(Modifier.size(46.dp))
                                Spacer(Modifier.weight(1f))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedVisibility(
                                        visible = route == PlaylistRoute.Overview && showInlineSearch,
                                        enter = expandHorizontally(
                                            expandFrom = Alignment.End,
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)
                                        ) + fadeIn(animationSpec = tween(180)),
                                        exit = shrinkHorizontally(
                                            shrinkTowards = Alignment.End,
                                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 560f)
                                        ) + fadeOut(animationSpec = tween(140))
                                    ) {
                                        LiquidGlassPane(
                                            enabled = glassy,
                                            dark = dark,
                                            shape = RoundedCornerShape(999.dp),
                                            cornerRadius = 32.dp,
                                            modifier = Modifier.width(196.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = songSearchQuery,
                                                onValueChange = onSongSearchQueryChange,
                                                singleLine = true,
                                                placeholder = { Text("搜索歌单") },
                                                leadingIcon = {
                                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Rounded.ChevronRight,
                                                        contentDescription = "收起搜索",
                                                        modifier = Modifier.noRippleClick(shape = CircleShape) {
                                                            showInlineSearch = false
                                                            onSongSearchQueryChange("")
                                                        }
                                                    )
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (route == PlaylistRoute.Overview && !showInlineSearch) {
                                        LiquidGlassPane(
                                            enabled = glassy,
                                            dark = dark,
                                            shape = CircleShape,
                                            cornerRadius = 26.dp,
                                            modifier = Modifier.noRippleClick(shape = CircleShape) {
                                                showInlineSearch = true
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Search,
                                                    contentDescription = "展开搜索",
                                                    tint = if (dark) Color.White else Color(0xFF111111)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                                            text = if (route == PlaylistRoute.Comments) "评论区" else "歌单详情"
                                        )
                                        PlaylistHeaderBadge(text = "${playlist.songs.size} 首")
                                        if (commentCount > 0) {
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
                                    PlaylistActionChip(
                                        text = when {
                                            isSubscriptionLoading -> "处理中"
                                            isSubscribed -> "已收藏"
                                            else -> "收藏"
                                        },
                                        dark = dark,
                                        selected = isSubscribed,
                                        onClick = onToggleSubscription,
                                        leading = {
                                            Icon(
                                                Icons.Rounded.Favorite,
                                                contentDescription = null,
                                                tint = if (isSubscribed) Color(0xFFFF5C7C) else titleColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                    PlaylistActionChip(
                                        text = if (commentCount > 0) "评论 $commentCount" else "评论",
                                        dark = dark,
                                        selected = route == PlaylistRoute.Comments,
                                        onClick = onShowComments
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    PlaylistRouteChip(
                                        text = "歌曲 ${playlist.songs.size}",
                                        selected = route == PlaylistRoute.Overview,
                                        dark = dark,
                                        modifier = Modifier.weight(1f),
                                        onClick = onShowSongs
                                    )
                                    PlaylistRouteChip(
                                        text = if (commentCount > 0) "评论 $commentCount" else "评论",
                                        selected = route == PlaylistRoute.Comments,
                                        dark = dark,
                                        modifier = Modifier.weight(1f),
                                        onClick = onShowComments
                                    )
                                }
                            }
                        }
                    }
                }
                if (route == PlaylistRoute.Overview) {
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
                                onLikeClick = if (song.sourceId == "netease") ({ onToggleSongLike(song) }) else null
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
        modifier = Modifier.noRippleClick(shape = RoundedCornerShape(999.dp), onClick = onClick)
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
