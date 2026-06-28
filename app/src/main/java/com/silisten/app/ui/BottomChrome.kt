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
fun SiListenBottomChromeReserve(
    hasPlaybackChrome: Boolean,
    hideNavDock: Boolean
) {
    val appearance = LocalSiListenAppearance.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasPlaybackChrome) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (appearance.floatingBottomBarEnabled) 92.dp else 84.dp)
            )
        }
        if (!hideNavDock) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (appearance.floatingBottomBarEnabled) 88.dp else 72.dp)
            )
        } else if (hasPlaybackChrome) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SiListenBottomChrome(
    viewModel: SiListenViewModel,
    hideNavDock: Boolean = false,
    backdrop: LayerBackdrop? = null,
    miuixBackdrop: MiuixLayerBackdrop? = null,
    selected: AppTab = viewModel.uiState.selectedTab,
    selectedPosition: Float? = null,
    onSelect: (AppTab) -> Unit = viewModel::selectTab
) {
    val uiState = viewModel.uiState
    val playback = viewModel.playbackState
    val appearance = LocalSiListenAppearance.current
    val resolvedDark = appearance.dark
    val barShape = if (appearance.floatingBottomBarEnabled) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp)
    val barAlpha = when {
        appearance.blurEnabled -> 0.88f
        else -> 1f
    }
    val primaryAccent = appearance.accent
    val barContainer = MaterialTheme.colorScheme.surfaceContainer
    val barContent = if (resolvedDark) Color(0xFFF3FFF5) else Color(0xFF121212)
    var miniPlayerSize by remember { mutableStateOf(IntSize.Zero) }
    var navSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (playback.currentSong != null || playback.errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (appearance.floatingBottomBarEnabled) 12.dp else 0.dp,
                        end = if (appearance.floatingBottomBarEnabled) 12.dp else 0.dp,
                        bottom = if (hideNavDock) 8.dp else 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                playback.errorMessage?.let { message ->
                    PlaybackErrorBar(
                        message = message,
                        darkTheme = resolvedDark,
                        glassy = appearance.blurEnabled
                    )
                }
                MiniPlayer(
                    playback = playback,
                    onOpenLyrics = { viewModel.openPlayerSheet(PlayerSheetPanel.Lyrics) },
                    onOpenQueue = { viewModel.openPlayerSheet(PlayerSheetPanel.Queue) },
                    onToggle = viewModel::togglePlayback,
                    onNext = viewModel::next,
                    darkTheme = resolvedDark,
                    glassy = appearance.blurEnabled,
                    alwaysVisible = false,
                    onMeasured = { miniPlayerSize = it }
                )
            }
        } else {
            Spacer(
                modifier = Modifier
                    .onSizeChanged { miniPlayerSize = it }
            )
        }

        if (!hideNavDock) {
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = if (appearance.floatingBottomBarEnabled) 12.dp else 0.dp,
                        vertical = if (appearance.floatingBottomBarEnabled) 8.dp else 0.dp
                    )
            ) {
                SiListenNav(
                    selected = selected,
                    onSelect = onSelect,
                    darkTheme = resolvedDark,
                    barShape = barShape,
                    containerColor = barContainer,
                    contentColor = barContent,
                    floating = appearance.floatingBottomBarEnabled,
                    blurEnabled = appearance.blurEnabled,
                    floatingBottomBarBlurEnabled = appearance.floatingBottomBarBlurEnabled,
                    backdrop = miuixBackdrop,
                    miuixBackdrop = miuixBackdrop,
                    selectedPosition = selectedPosition,
                    onMeasured = { navSize = it }
                )
            }
        } else {
            Spacer(
                modifier = Modifier
                    .onSizeChanged { navSize = it }
            )
        }
    }
}

@Composable
private fun SiListenNav(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    selectedPosition: Float?,
    darkTheme: Boolean,
    barShape: Shape,
    containerColor: Color,
    contentColor: Color,
    floating: Boolean,
    blurEnabled: Boolean,
    floatingBottomBarBlurEnabled: Boolean,
    backdrop: MiuixLayerBackdrop?,
    miuixBackdrop: MiuixLayerBackdrop?,
    onMeasured: (IntSize) -> Unit
) {
    val tabs = listOf(
        AppTab.Home to "首页",
        AppTab.Search to "搜索",
        AppTab.Sources to "音乐库",
        AppTab.Account to "账号"
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val barColor = if (darkTheme) Color.Black.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.72f)
    if (backdrop != null) {
        KernelSuFloatingBottomBar(
            selectedIndex = selectedIndex,
            selectedPosition = selectedPosition,
            onSelected = { index -> onSelect(tabs[index].first) },
            backdrop = backdrop,
            tabsCount = tabs.size,
            darkTheme = darkTheme,
            accentColor = MaterialTheme.colorScheme.primary,
            containerColor = if (containerColor != Color.Unspecified) containerColor else barColor,
            isBlurEnabled = floating && blurEnabled && floatingBottomBarBlurEnabled,
            useLiquidGlassFallback = false,
            modifier = Modifier.onSizeChanged(onMeasured)
        ) {
            tabs.forEachIndexed { index, (tab, label) ->
                KernelSuFloatingBottomBarItem(
                    index = index,
                    onClick = { onSelect(tab) }
                ) {
                    Icon(
                        imageVector = when (tab) {
                            AppTab.Home -> Icons.Rounded.Home
                            AppTab.Search -> Icons.Rounded.Search
                            AppTab.Sources -> Icons.Rounded.LibraryMusic
                            AppTab.Account -> Icons.Rounded.AccountCircle
                            AppTab.Settings -> Icons.Rounded.Settings
                        },
                        contentDescription = label,
                        tint = if (index == selectedIndex) contentColor else contentColor.copy(alpha = 0.68f)
                    )
                    Text(
                        text = label,
                        color = if (index == selectedIndex) contentColor else contentColor.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == selectedIndex) FontWeight.Black else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    } else {
        Surface(
            color = if (containerColor != Color.Unspecified) containerColor else barColor,
            shape = barShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .onSizeChanged(onMeasured)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, (tab, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .noRippleClick(shape = CircleShape) { onSelect(tab) },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                AppTab.Home -> Icons.Rounded.Home
                                AppTab.Search -> Icons.Rounded.Search
                                AppTab.Sources -> Icons.Rounded.LibraryMusic
                                AppTab.Account -> Icons.Rounded.AccountCircle
                                AppTab.Settings -> Icons.Rounded.Settings
                            },
                            contentDescription = label,
                            tint = if (index == selectedIndex) contentColor else contentColor.copy(alpha = 0.64f)
                        )
                        Text(
                            text = label,
                            color = if (index == selectedIndex) contentColor else contentColor.copy(alpha = 0.64f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == selectedIndex) FontWeight.Black else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}
