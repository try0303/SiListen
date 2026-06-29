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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
fun MiniPlayer(
    playback: PlaybackState,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    darkTheme: Boolean,
    glassy: Boolean,
    alwaysVisible: Boolean = false,
    onMeasured: (IntSize) -> Unit = {}
) {
    val song = playback.currentSong
    if (song == null && !alwaysVisible) return
    val accent = MaterialTheme.colorScheme.primary
    val container = if (darkTheme) Color(0xB8141519) else Color.White.copy(alpha = if (glassy) 0.70f else 0.96f)
    val primaryText = if (darkTheme) Color(0xFFF5F5F6) else Color(0xFF141414)
    val secondaryText = if (darkTheme) Color(0xFFABAFB6) else Color(0xFF666A70)
    val openInteraction = remember { MutableInteractionSource() }
    val openPressed by openInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (openPressed) 0.988f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 540f),
        label = "mini-player-scale"
    )
    val artScale by animateFloatAsState(
        targetValue = if (openPressed) 0.976f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 520f),
        label = "mini-player-art-scale"
    )
    val duration = playback.durationMs.coerceAtLeast(1L).toFloat()
    val progress = playback.positionMs.coerceIn(0L, playback.durationMs.coerceAtLeast(1L)).toFloat() / duration
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "mini-player-progress"
    )
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (darkTheme) 0.12f else 0.58f)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 4.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(26.dp))
            .zIndex(1f)
            .clickable(
                interactionSource = openInteraction,
                indication = null,
                onClick = {
                    if (song != null) onOpenLyrics()
                }
            )
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .onSizeChanged { onMeasured(it) }
    ) {
        LiquidGlassPane(
            enabled = glassy,
            dark = darkTheme,
            shape = RoundedCornerShape(26.dp),
            cornerRadius = 26.dp,
            accent = accent,
            blurRadius = if (glassy) 22.dp else 18.dp,
            tintAlpha = if (darkTheme) 0.055f else 0.07f,
            modifier = if (glassy) {
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(container, RoundedCornerShape(26.dp))
            }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (darkTheme) 0.08f else 0.46f),
                                container,
                                Color.Black.copy(alpha = if (darkTheme) 0.12f else 0.025f)
                            )
                        )
                    )
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer {
                            scaleX = artScale
                            scaleY = artScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song?.coverUrl.isNullOrBlank()) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = primaryText.copy(alpha = 0.78f),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        song?.title ?: "点击任意歌曲开始播放",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Black,
                        color = primaryText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (playback.isPreparing) {
                            "正在准备播放"
                        } else {
                            listOf(song?.artist.orEmpty(), song?.album.orEmpty())
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifBlank { "播放器入口" }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(
                    onClick = { if (song != null) onToggle() else onOpenLyrics() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (darkTheme) {
                                Color.White.copy(alpha = 0.04f)
                            } else {
                                Color.White.copy(alpha = 0.28f)
                            }
                        )
                ) {
                    if (playback.isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = primaryText
                        )
                    } else {
                        Icon(
                            if (song != null && playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "播放或暂停",
                            tint = primaryText,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { if (song != null) onNext() else onOpenQueue() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = primaryText.copy(alpha = if (song != null) 0.86f else 0.38f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = 0.96f),
                                    accent.copy(alpha = 0.62f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PlaybackErrorBar(
    message: String,
    darkTheme: Boolean,
    glassy: Boolean
) {
    Surface(
        color = if (darkTheme) Color(0xFF2A1717).copy(alpha = if (glassy) 0.78f else 1f) else Color(0xFFFFF0F0),
        border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.42f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = message,
            color = if (darkTheme) Color(0xFFFFB4B4) else Color(0xFF9B1C1C),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

private enum class PlayerPage { Detail, Lyrics, Queue, Comments }

private val playerPages = PlayerPage.entries.toList()

private fun PlayerSheetPanel.toPlayerPage(): PlayerPage = when (this) {
    PlayerSheetPanel.Detail -> PlayerPage.Detail
    PlayerSheetPanel.Queue -> PlayerPage.Queue
    PlayerSheetPanel.Lyrics -> PlayerPage.Lyrics
    PlayerSheetPanel.Comments -> PlayerPage.Comments
}

@Composable
private fun rememberDynamicPalette(coverUrl: String?): Pair<Color, Color> {
    val context = LocalContext.current
    var dominant by remember { mutableStateOf(Color(0xFF1A1A2E)) }
    var vibrant by remember { mutableStateOf(Color(0xFF16213E)) }
    LaunchedEffect(coverUrl) {
        if (coverUrl.isNullOrBlank()) return@LaunchedEffect
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
            .size(200)
            .build()
        val result = runCatching { loader.execute(request) }.getOrNull()
        val bitmap = (result as? SuccessResult)?.drawable?.let { it as? BitmapDrawable }?.bitmap
            ?: return@LaunchedEffect
        val palette = Palette.from(bitmap).generate()
        palette.dominantSwatch?.rgb?.let { dominant = Color(it) }
        (palette.vibrantSwatch ?: palette.mutedSwatch)?.rgb?.let { vibrant = Color(it) }
    }
    val animDominant by animateColorAsState(
        targetValue = dominant,
        animationSpec = tween(800),
        label = "palette-dominant"
    )
    val animVibrant by animateColorAsState(
        targetValue = vibrant,
        animationSpec = tween(800),
        label = "palette-vibrant"
    )
    return animDominant to animVibrant
}

@Composable
private fun DynamicPlayerBackground(
    coverUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val (dominant, vibrant) = rememberDynamicPalette(coverUrl)
    Box(modifier = modifier) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(56.dp)
                .graphicsLayer {
                    scaleX = 1.3f
                    scaleY = 1.3f
                    alpha = 0.48f
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            dominant.copy(alpha = 0.92f),
                            vibrant.copy(alpha = 0.60f),
                            accent.copy(alpha = 0.30f),
                            Color(0xFF0A0A0A).copy(alpha = 0.96f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlayerGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 10.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.48f))
    )
}

@Composable
private fun PagerDotIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val dotAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0.36f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
                label = "dot-alpha"
            )
            val dotWidth by animateFloatAsState(
                targetValue = if (selected) 20f else 7f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                label = "dot-width"
            )
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(dotWidth.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .graphicsLayer { alpha = dotAlpha }
                    .background(Color.White)
            )
        }
    }
}

private data class PlayerTabItem(
    val page: PlayerPage,
    val label: String,
    val icon: ImageVector
)

private val playerTabItems = listOf(
    PlayerTabItem(PlayerPage.Detail, "唱片", Icons.Rounded.Album),
    PlayerTabItem(PlayerPage.Lyrics, "歌词", Icons.Rounded.Subtitles),
    PlayerTabItem(PlayerPage.Queue, "列表", Icons.AutoMirrored.Rounded.QueueMusic),
    PlayerTabItem(PlayerPage.Comments, "评论", Icons.AutoMirrored.Rounded.Comment)
)

@Composable
private fun PlayerTabBar(
    currentPage: Int,
    onPageSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            playerTabItems.forEachIndexed { index, item ->
                val selected = index == currentPage
                val tint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.62f),
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
                    label = "tab-tint"
                )
                val bgAlpha by animateFloatAsState(
                    targetValue = if (selected) 0.14f else 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
                    label = "tab-bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = bgAlpha))
                        .noRippleClick(shape = RoundedCornerShape(22.dp)) { onPageSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            color = tint,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerDetailPage(
    playback: PlaybackState,
    song: Song?,
    accent: Color,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: (Song) -> Unit,
    artworkScale: Float,
    artworkAlpha: Float
) {
    val duration = playback.durationMs.coerceAtLeast(1L)
    val progress = playback.positionMs.coerceIn(0L, duration).toFloat()
    val heartTint by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF5C7C) else Color.White.copy(alpha = 0.82f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "detail-heart-color"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "detail-heart-scale"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.08f))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                    alpha = artworkAlpha
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song?.coverUrl,
                contentDescription = song?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(40.dp, RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF252525))
            )
        }
        Spacer(Modifier.weight(0.06f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "暂无播放",
                    color = Color.White.copy(alpha = 0.94f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song?.artist ?: "未知歌手",
                    color = Color.White.copy(alpha = 0.60f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (song?.sourceId == "netease") {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .noRippleClick(shape = CircleShape) { onToggleLike(song) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLikeLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.8.dp,
                            color = heartTint
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "喜欢",
                            tint = heartTint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Slider(
            value = progress,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.92f),
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatTime(playback.positionMs),
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                formatTime(playback.durationMs),
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(62.dp)) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "上一首",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "播放或暂停",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(42.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(62.dp)) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "下一首",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(Modifier.weight(0.1f))
    }
}

@Composable
fun FullPlayer(
    playback: PlaybackState,
    lyrics: List<LyricLine>,
    isLyricLoading: Boolean,
    playerComments: List<PlaylistComment>,
    playerCommentSort: PlaylistCommentSort,
    playerCommentCount: Int,
    isPlayerCommentsLoading: Boolean,
    playerCommentsMessage: String?,
    themeSettings: ThemeSettingsState,
    lyricDisplayMode: LyricDisplayMode,
    initialPanel: PlayerSheetPanel,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: (Song) -> Unit,
    onPlayerCommentSortChange: (PlaylistCommentSort) -> Unit,
    onRefreshPlayerComments: () -> Unit
) {
    val song = playback.currentSong
    val accent = themeSettings.accentColor()
    val activeLyricIndex = remember(lyrics, playback.positionMs) {
        lyrics.indexOfLast { it.timeMs <= playback.positionMs }.coerceAtLeast(0)
    }
    val duration = playback.durationMs.coerceAtLeast(1L)
    val initialPage = remember(song?.id, initialPanel) {
        playerPages.indexOf(initialPanel.toPlayerPage()).coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { playerPages.size })
    val artworkFraction by remember {
        derivedStateOf {
            if (pagerState.currentPage == 0) {
                pagerState.currentPageOffsetFraction.coerceIn(0f, 1f)
            } else if (pagerState.currentPage == 1 && pagerState.currentPageOffsetFraction < 0f) {
                (1f + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val artworkScale = lerp(1f, 0.5f, artworkFraction)
    val artworkAlpha = lerp(1f, 0f, artworkFraction)
    val pagerScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        DynamicPlayerBackground(
            coverUrl = song?.coverUrl,
            accent = accent,
            modifier = Modifier.fillMaxSize()
        )
        if (lyricDisplayMode == LyricDisplayMode.Particles) {
            LyricStageParticles()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerGrabber()
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (playerPages[page]) {
                    PlayerPage.Detail -> PlayerDetailPage(
                        playback = playback,
                        song = song,
                        accent = accent,
                        isLiked = isLiked,
                        isLikeLoading = isLikeLoading,
                        onToggle = onToggle,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onToggleLike = onToggleLike,
                        artworkScale = artworkScale,
                        artworkAlpha = artworkAlpha
                    )
                    PlayerPage.Lyrics -> when (lyricDisplayMode) {
                        LyricDisplayMode.Glass -> GlassLyricsPanel(
                            lyrics = lyrics,
                            isLoading = isLyricLoading,
                            playbackPositionMs = playback.positionMs,
                            durationMs = duration,
                            coverUrl = song?.coverUrl,
                            songTitle = song?.title,
                            songArtist = song?.artist,
                            onSeek = onSeek,
                            isPlaying = playback.isPlaying,
                            onToggle = onToggle,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            accent = accent,
                            dark = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        LyricDisplayMode.Particles -> ParticleLyricsPanel(
                            lyrics = lyrics,
                            activeIndex = activeLyricIndex,
                            isLoading = isLyricLoading,
                            playbackPositionMs = playback.positionMs,
                            accent = accent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    PlayerPage.Queue -> PlayerQueuePanel(
                        playback = playback,
                        dark = true,
                        accent = accent,
                        onSongSelect = onPlayQueueIndex
                    )
                    PlayerPage.Comments -> PlayerCommentsPanel(
                        song = song,
                        comments = playerComments,
                        commentSort = playerCommentSort,
                        commentCount = playerCommentCount,
                        isLoading = isPlayerCommentsLoading,
                        message = playerCommentsMessage,
                        dark = true,
                        accent = accent,
                        onSortChange = onPlayerCommentSortChange,
                        onRefresh = onRefreshPlayerComments
                    )
                }
            }
            PlayerTabBar(
                currentPage = pagerState.currentPage,
                onPageSelect = { index ->
                    pagerScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    }
}

@Composable
private fun PlayerQueuePanel(
    playback: PlaybackState,
    dark: Boolean,
    accent: Color,
    onSongSelect: (Int) -> Unit
) {
    val sectionText = if (dark) Color.White.copy(alpha = 0.66f) else Color(0xFF111111).copy(alpha = 0.58f)
    val currentIndex = playback.currentIndex.coerceAtLeast(0)
    val currentQueueSong = playback.queue.getOrNull(currentIndex)
    val currentSong = playback.currentSong ?: currentQueueSong
    val playedSongs = playback.queue
        .mapIndexed { index, song -> index to song }
        .take(currentIndex)
    val upcomingSongs = playback.queue
        .mapIndexed { index, song -> index to song }
        .drop(currentIndex + 1)
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        val playedCount = playedSongs.size
        if (playedCount > 0) {
            listState.animateScrollToItem(playedCount + 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            PlayerPanelHeader(
                title = "播放列表",
                subtitle = if (playback.queue.isEmpty()) "当前没有播放队列" else "${playback.queue.size} 首歌在队列中",
                dark = dark
            )
        }
        if (playedSongs.isNotEmpty()) {
            item {
                PlayerQueueSectionLabel("已播放", sectionText)
            }
            items(playedSongs, key = { "played-${it.first}" }) { (index, item) ->
                PlayerQueueRow(
                    index = index,
                    song = item,
                    active = false,
                    isPreparing = false,
                    playbackPositionMs = playback.positionMs,
                    dark = dark,
                    accent = accent,
                    dimmed = true,
                    onClick = { onSongSelect(index) }
                )
            }
        }
        if (currentSong != null) {
            item {
                PlayerQueueSectionLabel("当前歌曲", sectionText)
            }
            item {
                PlayerQueueRow(
                    index = currentIndex.coerceAtMost(playback.queue.lastIndex.coerceAtLeast(0)),
                    song = currentSong,
                    active = true,
                    isPreparing = playback.isPreparing,
                    playbackPositionMs = playback.positionMs,
                    dark = dark,
                    accent = accent,
                    onClick = { }
                )
            }
        } else {
            item {
                EmptyStateCard(
                    title = "暂无播放",
                    subtitle = "选择一首歌后，这里会显示当前歌曲和接下来要播放的内容。",
                    dark = dark
                )
            }
        }
        item {
            PlayerQueueSectionLabel("接下来", sectionText)
        }
        if (upcomingSongs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "没有更多歌曲",
                    subtitle = "继续点歌后，新的歌曲会排在这里。",
                    dark = dark
                )
            }
        } else {
            items(upcomingSongs, key = { it.first }) { (index, item) ->
                PlayerQueueRow(
                    index = index,
                    song = item,
                    active = false,
                    isPreparing = false,
                    playbackPositionMs = playback.positionMs,
                    dark = dark,
                    accent = accent,
                    onClick = { onSongSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun PlayerQueueSectionLabel(
    text: String,
    color: Color
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PlayerQueueRow(
    index: Int,
    song: Song,
    active: Boolean,
    isPreparing: Boolean,
    playbackPositionMs: Long,
    dark: Boolean,
    accent: Color,
    dimmed: Boolean = false,
    onClick: () -> Unit
) {
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6A6D72)
    val panelStroke = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.988f
            active -> 1.01f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 520f),
        label = "queue-card-scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = when {
            pressed -> 0.95f
            active -> 1f
            dimmed -> 0.50f
            else -> 0.98f
        },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "queue-card-alpha"
    )
    Surface(
        color = when {
            active -> accent.copy(alpha = if (dark) 0.18f else 0.12f)
            pressed -> if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)
            else -> Color.Transparent
        },
        border = BorderStroke(
            1.dp,
            if (active) accent.copy(alpha = 0.36f) else panelStroke
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                color = if (active) accent else secondaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (active) 56.dp else 48.dp)
                    .clip(RoundedCornerShape(if (active) 18.dp else 15.dp))
                    .background(Color.DarkGray)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (active) {
                    Text(
                        text = if (isPreparing) "准备播放" else "正在播放",
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            if (active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    } else {
                        Text(
                            text = formatTime(playbackPositionMs),
                            color = accent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                Text(
                    text = formatTime(song.durationMs),
                    color = secondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlayerCommentsPanel(
    song: Song?,
    comments: List<PlaylistComment>,
    commentSort: PlaylistCommentSort,
    commentCount: Int,
    isLoading: Boolean,
    message: String?,
    dark: Boolean,
    accent: Color,
    onSortChange: (PlaylistCommentSort) -> Unit,
    onRefresh: () -> Unit
) {
    val sortTitle = if (commentSort == PlaylistCommentSort.Hot) "热门评论" else "最新评论"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerPanelHeader(
                    title = if (commentSort == PlaylistCommentSort.Hot) "热评榜" else "最新评论",
                    subtitle = if (commentCount > 0) "${song?.title.orEmpty()} · $commentCount 条评论" else (song?.title ?: "暂无播放"),
                    dark = dark,
                    modifier = Modifier.weight(1f)
                )
                PlaylistActionChip(
                    text = "刷新",
                    dark = dark,
                    selected = false,
                    onClick = onRefresh
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlaylistRouteChip(
                    text = "热门评论",
                    selected = commentSort == PlaylistCommentSort.Hot,
                    dark = dark,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortChange(PlaylistCommentSort.Hot) }
                )
                PlaylistRouteChip(
                    text = "最新评论",
                    selected = commentSort == PlaylistCommentSort.Latest,
                    dark = dark,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortChange(PlaylistCommentSort.Latest) }
                )
            }
        }
        item {
            PlayerCommentSummaryCard(
                song = song,
                commentCount = commentCount,
                sortTitle = sortTitle,
                isLoading = isLoading,
                dark = dark,
                accent = accent
            )
        }
        if (song == null) {
            item {
                EmptyStateCard(
                    title = "暂无播放",
                    subtitle = "先播放一首歌，再来看评论。",
                    dark = dark
                )
            }
            return@LazyColumn
        }
        if (isLoading) {
            item {
                LoadingStateCard(
                    text = "正在加载歌曲评论...",
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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (dark) Color(0x33FFD166) else Color(0x1AFFD166)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        if (!isLoading && comments.isEmpty() && message == null) {
            item {
                EmptyStateCard(
                    title = "暂时还没有评论",
                    subtitle = "这首歌现在没有可展示的评论内容。",
                    dark = dark
                )
            }
        }
        items(comments) { comment ->
            PlayerCommentFlowRow(
                comment = comment,
                dark = dark,
                accent = accent
            )
        }
    }
}

@Composable
private fun PlayerCommentFlowRow(
    comment: PlaylistComment,
    dark: Boolean,
    accent: Color
) {
    val titleColor = if (dark) Color.White.copy(alpha = 0.94f) else Color(0xFF171717)
    val mutedText = if (dark) Color.White.copy(alpha = 0.48f) else Color(0xFF83878D)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.authorAvatarUrl.ifBlank { null },
            contentDescription = comment.authorName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (comment.timeLabel.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = comment.timeLabel,
                        color = mutedText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = comment.content,
                color = titleColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.08f
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comment.replyCount > 0) {
                    Text(
                        text = "${comment.replyCount} 条回复 >",
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                if (comment.likedCount > 0) {
                    Text(
                        text = "${comment.likedCount} 赞",
                        color = mutedText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCommentSummaryCard(
    song: Song?,
    commentCount: Int,
    sortTitle: String,
    isLoading: Boolean,
    dark: Boolean,
    accent: Color
) {
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6A6D72)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song?.coverUrl,
                contentDescription = song?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.14f))
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song?.title ?: "暂无播放",
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(song?.artist.orEmpty(), sortTitle)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    color = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = accent.copy(alpha = if (dark) 0.20f else 0.13f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    }
                    Text(
                        text = if (commentCount > 0) "$commentCount" else "0",
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerPanelHeader(
    title: String,
    subtitle: String,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6A6D72)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = subtitle,
            color = secondaryText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassLyricsPanel(
    lyrics: List<LyricLine>,
    isLoading: Boolean,
    playbackPositionMs: Long,
    durationMs: Long,
    coverUrl: String?,
    songTitle: String?,
    songArtist: String?,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    accent: Color,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val lyricItems = remember(lyrics, isLoading) {
        if (isLoading) {
            listOf(LyricLine(0L, "歌词加载中..."))
        } else {
            lyrics.ifEmpty { listOf(LyricLine(0L, "暂时没有歌词")) }
        }
    }

    var interpolatedMs by remember { mutableStateOf(playbackPositionMs) }
    var lastKnownMs by remember { mutableStateOf(playbackPositionMs) }
    var lastKnownNanos by remember { mutableStateOf(0L) }

    LaunchedEffect(playbackPositionMs) {
        lastKnownMs = playbackPositionMs
        lastKnownNanos = System.nanoTime()
        interpolatedMs = playbackPositionMs
    }

    LaunchedEffect(isPlaying, lastKnownMs) {
        if (!isPlaying) {
            interpolatedMs = lastKnownMs
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastKnownNanos > 0L) {
                    val elapsedMs = (frameNanos - lastKnownNanos) / 1_000_000L
                    interpolatedMs = (lastKnownMs + elapsedMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                }
            }
        }
    }

    val activeIndex = remember(lyricItems, interpolatedMs) {
        lyricItems.indexOfLast { it.timeMs <= interpolatedMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex, lyricItems.size) {
        if (lyricItems.isEmpty()) return@LaunchedEffect
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val focusOffset = if (viewportHeight > 0) {
            -((viewportHeight * 0.35f) - 60f).toInt()
        } else {
            -200
        }
        listState.animateScrollToItem(
            index = activeIndex.coerceIn(0, lyricItems.lastIndex),
            scrollOffset = focusOffset
        )
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                .zIndex(2f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    MarqueeText(
                        text = songTitle.orEmpty().ifBlank { "未知歌曲" },
                        color = Color.White.copy(alpha = 0.94f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    MarqueeText(
                        text = songArtist.orEmpty().ifBlank { "未知歌手" },
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "播放或暂停",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "apple-lyrics-top") {
                Spacer(Modifier.height(80.dp))
            }
            itemsIndexed(
                items = lyricItems,
                key = { index, line -> "${line.timeMs}-$index-${line.text}" }
            ) { index, line ->
                val isActive = index == activeIndex || lyricItems.size == 1
                val distance = kotlin.math.abs(index - activeIndex)
                val nextTime = lyricItems.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 3_800L)
                val karaokeProgress = if (isActive && nextTime > line.timeMs) {
                    ((interpolatedMs - line.timeMs).toFloat() / (nextTime - line.timeMs).toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val targetAlpha = when {
                    isActive -> 1f
                    distance == 1 -> 0.52f
                    distance == 2 -> 0.26f
                    distance == 3 -> 0.12f
                    else -> 0.05f
                }
                val lineAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
                    label = "apple-lyric-alpha"
                )
                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.0f else 0.94f - (distance.coerceAtMost(4) * 0.025f),
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 260f),
                    label = "apple-lyric-scale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (isActive) 20.dp else 12.dp)
                        .graphicsLayer {
                            alpha = lineAlpha
                            scaleX = lineScale
                            scaleY = lineScale
                        }
                        .blur(if (isActive) 0.dp else (distance.coerceAtMost(4) * 1.2f).dp)
                        .noRippleClick(shape = RoundedCornerShape(18.dp)) {
                            onSeek(line.timeMs)
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    AppleMusicLyricLineText(
                        text = line.text.trim().ifBlank { " " },
                        active = isActive,
                        progress = karaokeProgress,
                        accent = accent
                    )
                }
            }
            item(key = "apple-lyrics-bottom") {
                Spacer(Modifier.height(200.dp))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f))
                    )
                )
        )
    }
}

@Composable
private fun AppleMusicLyricLineText(
    text: String,
    active: Boolean,
    progress: Float,
    accent: Color,
    activeTextColor: Color = Color.White,
    inactiveTextColor: Color = Color.White.copy(alpha = 0.58f),
    unsungTextColor: Color = Color.White.copy(alpha = 0.44f),
    activeTextStyle: androidx.compose.ui.text.TextStyle? = null,
    inactiveTextStyle: androidx.compose.ui.text.TextStyle? = null,
    textAlign: TextAlign = TextAlign.Start,
    activeMaxLines: Int = 4,
    inactiveMaxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    val baseStyle = if (active) {
        activeTextStyle ?: MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 37.sp)
    } else {
        inactiveTextStyle ?: MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp)
    }
    val fontWeight = if (active) FontWeight.Black else FontWeight.Bold
    val maxLines = if (active) activeMaxLines else inactiveMaxLines
    if (!active) {
        Text(
            text = text,
            color = inactiveTextColor,
            style = baseStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    var textLayout by remember(text, maxLines, textAlign) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = unsungTextColor,
            style = baseStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayout = it },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = text,
            color = activeTextColor,
            style = baseStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    val layout = textLayout ?: return@drawWithContent
                    val p = progress.coerceIn(0f, 1f)
                    if (p <= 0f || layout.lineCount == 0) return@drawWithContent
                    val visibleEnd = (0 until layout.lineCount)
                        .maxOf { line -> layout.getLineEnd(line, visibleEnd = true) }
                        .coerceIn(0, text.length)
                    if (visibleEnd <= 0) return@drawWithContent

                    val fillOffset = visibleEnd * p
                    val fillBaseOffset = floor(fillOffset).toInt().coerceIn(0, visibleEnd)
                    val fillFraction = (fillOffset - fillBaseOffset).coerceIn(0f, 1f)

                    for (line in 0 until layout.lineCount) {
                        val lineStart = layout.getLineStart(line).coerceIn(0, visibleEnd)
                        val lineEnd = layout.getLineEnd(line, visibleEnd = true).coerceIn(lineStart, visibleEnd)
                        if (lineEnd <= lineStart) continue

                        val lineLeft = layout.getLineLeft(line).coerceIn(0f, size.width)
                        val lineRight = layout.getLineRight(line).coerceIn(lineLeft, size.width)
                        val right = when {
                            fillOffset >= lineEnd -> lineRight
                            fillOffset <= lineStart -> lineLeft
                            else -> {
                                val startOffset = fillBaseOffset.coerceIn(lineStart, lineEnd)
                                val endOffset = (startOffset + 1).coerceAtMost(lineEnd)
                                val startX = layout.getHorizontalPosition(startOffset, true)
                                val endX = layout.getHorizontalPosition(endOffset, true)
                                lerp(startX, endX, fillFraction).coerceIn(lineLeft, lineRight)
                            }
                        }
                        if (right > lineLeft) {
                            clipRect(
                                left = lineLeft,
                                top = layout.getLineTop(line).coerceAtLeast(0f),
                                right = right,
                                bottom = layout.getLineBottom(line).coerceAtMost(size.height)
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun ParticleLyricsPanel(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    isLoading: Boolean,
    playbackPositionMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            ImmersiveLyrics(
                lyrics = lyrics,
                activeIndex = activeIndex,
                isLoading = isLoading,
                playbackPositionMs = playbackPositionMs,
                center = true,
                dark = true,
                activeTextStyle = MaterialTheme.typography.displaySmall,
                inactiveTextStyle = MaterialTheme.typography.headlineSmall,
                activeScale = 1.14f,
                inactiveScale = 0.96f,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ImmersiveLyrics(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    isLoading: Boolean,
    playbackPositionMs: Long,
    center: Boolean,
    dark: Boolean,
    activeTextStyle: androidx.compose.ui.text.TextStyle,
    inactiveTextStyle: androidx.compose.ui.text.TextStyle,
    activeScale: Float,
    inactiveScale: Float,
    modifier: Modifier = Modifier
) {
    val activeTextColor = if (dark) Color.White else Color(0xFF111111)
    val inactiveTextColor = if (dark) Color.White.copy(alpha = 0.72f) else Color(0xFF111111).copy(alpha = 0.56f)
    val glowColor = if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
    val lyricItems = remember(lyrics, isLoading) {
        if (isLoading) {
            listOf(LyricLine(0L, "歌词加载中..."))
        } else {
            lyrics.ifEmpty { listOf(LyricLine(0L, "暂时没有歌词")) }
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex, lyricItems.size, isLoading) {
        if (isLoading || lyricItems.isEmpty()) return@LaunchedEffect
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val estimatedItemHeight = if (center) 124 else 112
        val centerOffset = if (viewportHeight > 0) {
            -((viewportHeight / 2f) - estimatedItemHeight).toInt()
        } else {
            0
        }
        listState.animateScrollToItem(
            index = activeIndex.coerceIn(0, lyricItems.lastIndex),
            scrollOffset = centerOffset
        )
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        item(key = "lyrics-top-spacer") {
            Spacer(Modifier.height(if (center) 164.dp else 176.dp))
        }
        itemsIndexed(
            items = lyricItems,
            key = { index, line -> "${line.timeMs}-$index-${line.text}" }
        ) { index, line ->
            val lineText = line.text.trim()
            val distance = (index - activeIndex).let { kotlin.math.abs(it) }
            val isActive = index == activeIndex || lyricItems.size == 1
            val previousLine = lyricItems.getOrNull(index - 1)?.text?.trim().orEmpty()
            val nextLineText = lyricItems.getOrNull(index + 1)?.text?.trim().orEmpty()
            val isLongLine = lineText.length >= if (center) 18 else 22
            val containsWideGlyphs = lineText.any { it.code > 255 }
            val segmentCount = lineText
                .split(" / ", " · ", "，", ",", "。", "！", "？", ":", "：", "…")
                .map { it.trim() }
                .count { it.isNotBlank() }
            val hasSentenceContinuation = lineText.endsWith("，") ||
                lineText.endsWith(",") ||
                lineText.endsWith("、") ||
                lineText.endsWith("：") ||
                lineText.endsWith(":") ||
                lineText.endsWith("…")
            val nextLineLooksConnected = nextLineText.isNotBlank() &&
                nextLineText.firstOrNull()?.let { it.isLowerCase() || it.code > 255 } == true
            val aroundTextFeelsDense = previousLine.length >= 10 || nextLineText.length >= 10
            val isSegmentHeavy = segmentCount >= 3
            val shouldUseExpandedReading = isLongLine || containsWideGlyphs || (hasSentenceContinuation && nextLineLooksConnected)
            val shouldHoldFocusLonger = shouldUseExpandedReading || aroundTextFeelsDense || isSegmentHeavy
            val nextTimeMs = lyricItems.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 4_000L)
            val activeProgress = if (isActive && nextTimeMs > line.timeMs) {
                ((playbackPositionMs - line.timeMs).toFloat() / (nextTimeMs - line.timeMs).toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val targetAlpha = when {
                isActive -> 1f
                distance == 1 -> 0.72f
                distance == 2 -> 0.42f
                distance == 3 -> 0.24f
                else -> 0.14f
            }
            val targetScale = when {
                isActive && shouldHoldFocusLonger -> activeScale + 0.015f
                isActive -> activeScale
                distance == 1 && shouldHoldFocusLonger -> (inactiveScale + 0.07f).coerceAtMost(1.02f)
                distance == 1 -> (inactiveScale + 0.05f).coerceAtMost(1f)
                distance == 2 -> (inactiveScale + 0.02f).coerceAtMost(1f)
                else -> inactiveScale - 0.02f
            }
            val targetTranslationY = when {
                isActive -> 0f
                distance == 1 && index < activeIndex -> -7f
                distance == 1 -> 7f
                index < activeIndex -> -14f
                else -> 14f
            }
            val targetGlowAlpha = when {
                isActive && shouldHoldFocusLonger -> 0.24f
                isActive -> 0.20f
                distance == 1 -> 0.10f
                distance == 2 -> 0.03f
                else -> 0f
            }
            val targetPaddingVertical = when {
                isActive && shouldHoldFocusLonger -> 24.dp
                isActive && shouldUseExpandedReading -> 22.dp
                isActive -> 20.dp
                distance == 1 && shouldHoldFocusLonger -> 14.dp
                distance == 1 -> 13.dp
                else -> 10.dp
            }
            val lineAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
                label = "lyric-alpha"
            )
            val lineScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = 0.74f, stiffness = 260f),
                label = "lyric-scale"
            )
            val lineTranslationY by animateFloatAsState(
                targetValue = targetTranslationY,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = 240f),
                label = "lyric-translation"
            )
            val lineGlowAlpha by animateFloatAsState(
                targetValue = targetGlowAlpha,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
                label = "lyric-glow"
            )
            val linePaddingVertical by animateDpAsState(
                targetValue = targetPaddingVertical,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
                label = "lyric-padding"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = linePaddingVertical)
                    .graphicsLayer {
                        alpha = lineAlpha
                        scaleX = lineScale
                        scaleY = lineScale
                        translationY = lineTranslationY
                    }
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f)
                    ),
                horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
            ) {
                AppleMusicLyricLineText(
                    text = lineText,
                    active = isActive,
                    progress = activeProgress,
                    accent = MaterialTheme.colorScheme.primary,
                    activeTextColor = activeTextColor,
                    inactiveTextColor = inactiveTextColor,
                    unsungTextColor = activeTextColor.copy(alpha = if (dark) 0.38f else 0.30f),
                    activeTextStyle = activeTextStyle,
                    inactiveTextStyle = inactiveTextStyle,
                    textAlign = if (center) TextAlign.Center else TextAlign.Start,
                    activeMaxLines = when {
                        isActive && shouldHoldFocusLonger -> 9
                        isActive && shouldUseExpandedReading -> 8
                        isActive -> 6
                        else -> 2
                    },
                    inactiveMaxLines = if (shouldUseExpandedReading) 3 else 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (lineGlowAlpha > 0f) {
                                glowColor.copy(alpha = lineGlowAlpha * if (dark) 0.72f else 0.38f)
                            } else {
                                Color.Transparent
                            },
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = if (isActive) 10.dp else 6.dp)
                )
            }
        }
        item(key = "lyrics-bottom-spacer") {
            Spacer(Modifier.height(if (center) 248.dp else 260.dp))
        }
    }
}
