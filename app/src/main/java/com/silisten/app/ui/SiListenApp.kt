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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
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

private fun Modifier.noRippleClick(
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: () -> Unit
): Modifier = composed {
    clip(shape).clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun LiquidGlassPane(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    dark: Boolean,
    shape: Shape,
    cornerRadius: Dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    blurRadius: Dp = 26.dp,
    tintAlpha: Float = if (dark) 0.10f else 0.14f,
    refractionHeight: Float = 18f,
    refractionOffset: Float = 0.12f,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val hostView = LocalView.current
    val latestContent = androidx.compose.runtime.rememberUpdatedState(content)
    val useNativeLiquidGlass = enabled && supportsNativeLiquidGlass()
    val fallbackBrush = if (enabled) {
        liquidGlassBrush(dark, accent, if (dark) 0.92f else 0.84f)
    } else {
        Brush.linearGradient(
            listOf(
                if (dark) Color(0xFF121612) else Color.White.copy(alpha = 0.96f),
                if (dark) Color(0xFF0C100D) else Color(0xFFF6F7F9)
            )
        )
    }
    Box(
        modifier = modifier
            .clip(shape)
    ) {
        if (useNativeLiquidGlass) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { context ->
                    LiquidGlassView(context).apply {
                        addView(
                            ComposeView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        )
                    }
                },
                update = { glassView ->
                    val target = ((hostView.context as? ComponentActivity)
                        ?.findViewById(android.R.id.content) as? ViewGroup)
                        ?: (hostView.rootView.findViewById(android.R.id.content) as? ViewGroup)
                        ?: (hostView.rootView as? ViewGroup)
                    if (target != null) {
                        glassView.bind(target)
                    }
                    with(density) {
                        glassView.setCornerRadius(cornerRadius.toPx())
                        glassView.setBlurRadius(blurRadius.toPx())
                        glassView.setRefractionHeight(refractionHeight.dp.toPx())
                        glassView.setRefractionOffset((refractionOffset * 100f).dp.toPx())
                    }
                    glassView.setTintAlpha(tintAlpha)
                    glassView.setTintColorRed(accent.red)
                    glassView.setTintColorGreen(accent.green)
                    glassView.setTintColorBlue(accent.blue)
                    glassView.setDispersion(if (dark) 0.24f else 0.34f)
                    glassView.setDraggableEnabled(false)
                    glassView.setElasticEnabled(false)
                    glassView.setTouchEffectEnabled(false)
                    (glassView.getChildAt(0) as? ComposeView)?.setContent {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (dark) 0.16f else 0.28f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = if (dark) 0.08f else 0.03f)
                                        )
                                    )
                                ),
                            content = latestContent.value
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(fallbackBrush)
                    .then(
                        if (enabled) Modifier.blur(blurRadius * 0.16f) else Modifier
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (dark) 0.20f else 0.58f),
                                Color.White.copy(alpha = if (dark) 0.05f else 0.10f),
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
                                Color.Transparent,
                                Color.Black.copy(alpha = if (dark) 0.06f else 0.03f)
                            )
                        )
                    ),
                content = content
            )
        }
    }
}

private fun supportsNativeLiquidGlass(): Boolean {
    return false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

private data class NativeGlassSpec(
    val visible: Boolean,
    val widthPx: Int,
    val heightPx: Int,
    val offsetXPx: Int,
    val offsetYPx: Int,
    val cornerRadiusPx: Float,
    val accent: Color,
    val tintAlpha: Float,
    val blurRadiusPx: Float,
    val refractionHeightPx: Float,
    val refractionOffsetPx: Float
)

private fun attachOrUpdateNativeGlassHost(
    activity: ComponentActivity,
    hostTag: String,
    spec: NativeGlassSpec
) {
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
    val existing = root.findViewWithTag<View>(hostTag)
    if (!spec.visible) {
        if (existing != null) root.removeView(existing)
        return
    }
    val container = (existing as? FrameLayout) ?: FrameLayout(activity).apply {
        tag = hostTag
        clipChildren = false
        clipToPadding = false
        isClickable = false
        isFocusable = false
        root.addView(this)
    }
    val glassView = (container.getChildAt(0) as? LiquidGlassView) ?: LiquidGlassView(activity).apply {
        setDraggableEnabled(false)
        setElasticEnabled(false)
        setTouchEffectEnabled(false)
        bind(root)
        container.addView(
            this,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }
    container.layoutParams = FrameLayout.LayoutParams(
        spec.widthPx.coerceAtLeast(1),
        spec.heightPx.coerceAtLeast(1)
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        leftMargin = spec.offsetXPx
        bottomMargin = spec.offsetYPx
    }
    glassView.setCornerRadius(spec.cornerRadiusPx)
    glassView.setBlurRadius(spec.blurRadiusPx)
    glassView.setRefractionHeight(spec.refractionHeightPx)
    glassView.setRefractionOffset(spec.refractionOffsetPx)
    glassView.setTintAlpha(spec.tintAlpha)
    glassView.setTintColorRed(spec.accent.red)
    glassView.setTintColorGreen(spec.accent.green)
    glassView.setTintColorBlue(spec.accent.blue)
    glassView.setDispersion(0.26f)
    container.bringToFront()
}

private fun removeNativeGlassHost(activity: ComponentActivity, hostTag: String) {
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
    root.findViewWithTag<View>(hostTag)?.let(root::removeView)
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
    val mainTabs = remember { listOf(AppTab.Home, AppTab.Search, AppTab.Sources, AppTab.Account) }
    val selectedMainTab = if (uiState.selectedTab in mainTabs) uiState.selectedTab else AppTab.Account
    val selectedPage = mainTabs.indexOf(selectedMainTab).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { mainTabs.size })
    val pagerScope = rememberCoroutineScope()
    val pagerPosition by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .fastCoerceIn(0f, (mainTabs.size - 1).toFloat())
        }
    }

    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    val latestSettingsRoute = androidx.compose.runtime.rememberUpdatedState(uiState.settingsRoute)
    val latestSelectedTab = androidx.compose.runtime.rememberUpdatedState(uiState.selectedTab)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (latestSettingsRoute.value == SettingsRoute.Main) {
                val targetTab = mainTabs.getOrNull(page) ?: AppTab.Home
                if (latestSelectedTab.value != targetTab) {
                    viewModel.selectTab(targetTab)
                }
            }
        }
    }

    BackHandler(
        enabled = uiState.settingsRoute != SettingsRoute.Main
    ) {
        viewModel.closeThemeSettings()
    }

    BackHandler(
        enabled = uiState.settingsRoute == SettingsRoute.Main &&
            selectedPlaylist == null &&
            selectedMainTab != AppTab.Home
    ) {
        viewModel.selectTab(AppTab.Home)
        pagerScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = selectedPlaylist != null) {
        if (uiState.selectedPlaylistRoute == PlaylistRoute.Comments) {
            viewModel.showPlaylistOverview()
        } else {
            viewModel.closePlaylist()
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
                                    AppTab.Search -> SearchScreen(uiState, viewModel, padding)
                                    AppTab.Sources -> SourcesScreen(uiState, viewModel, padding)
                                    AppTab.Account -> AccountScreen(viewModel, padding)
                                    AppTab.Settings -> AccountScreen(viewModel, padding)
                                }
                            }
                        }

                        if (selectedPlaylist != null) {
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
                                dark = resolvedDark,
                                glassy = appearance.blurEnabled,
                                onBack = {
                                    if (uiState.selectedPlaylistRoute == PlaylistRoute.Comments) {
                                        viewModel.showPlaylistOverview()
                                    } else {
                                        viewModel.closePlaylist()
                                    }
                                },
                                onPlayAll = { viewModel.playSelectedPlaylist() },
                                onSongClick = { song -> viewModel.playSong(song) },
                                isSongLiked = viewModel::isSongLiked,
                                isSongLikeLoading = viewModel::isSongLikeLoading,
                                onToggleSongLike = viewModel::toggleSongLike,
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
                        selected = selectedMainTab,
                        selectedPosition = if (uiState.settingsRoute == SettingsRoute.Main) pagerPosition else null,
                        onSelect = { tab ->
                            val index = mainTabs.indexOf(tab)
                            viewModel.selectTab(tab)
                            if (index >= 0) {
                                pagerScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        }
                    )
                }
            }

            if (viewModel.isPlayerSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = viewModel::closePlayerSheet,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF101312)
                ) {
                    FullPlayer(
                        playback = viewModel.playbackState,
                        lyrics = uiState.lyrics,
                        isLyricLoading = uiState.isLyricLoading,
                        playerComments = uiState.playerComments,
                        playerCommentSort = uiState.playerCommentSort,
                        playerCommentCount = uiState.playerCommentCount,
                        isPlayerCommentsLoading = uiState.isPlayerCommentsLoading,
                        playerCommentsMessage = uiState.playerCommentsMessage,
                        themeSettings = uiState.themeSettings,
                        lyricDisplayMode = uiState.playbackSettings.lyricDisplayMode,
                        initialPanel = viewModel.playerSheetPanel,
                        isLiked = viewModel.playbackState.currentSong?.let(viewModel::isSongLiked) == true,
                        isLikeLoading = viewModel.playbackState.currentSong?.let(viewModel::isSongLikeLoading) == true,
                        onToggle = viewModel::togglePlayback,
                        onNext = viewModel::next,
                        onPrevious = viewModel::previous,
                        onPlayQueueIndex = viewModel::playQueueIndex,
                        onSeek = viewModel::seekTo,
                        onToggleLike = viewModel::toggleSongLike,
                        onPlayerCommentSortChange = viewModel::selectPlayerCommentSort,
                        onRefreshPlayerComments = viewModel::refreshPlayerComments
                    )
                }
            }

    }
}

@Composable
private fun SiListenBottomChromeReserve(
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
private fun HomeScreen(
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
private fun SearchScreen(
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
private fun SourcesScreen(
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
private fun AccountScreen(viewModel: SiListenViewModel, padding: PaddingValues) {
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

@Composable
private fun PlaylistDetailScreen(
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
private fun PlaylistActionChip(
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
private fun PlaylistRouteChip(
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

@Composable
private fun SettingsScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    padding: PaddingValues
) {
    when (uiState.settingsRoute) {
        SettingsRoute.Main -> SettingsHomeScreen(
            viewModel = viewModel,
            themeSettings = uiState.themeSettings,
            padding = padding
        )
        SettingsRoute.Theme -> ThemeSettingsScreen(
            state = uiState.themeSettings,
            onBack = viewModel::closeThemeSettings,
            onUiModeChange = viewModel::selectThemeUiMode,
            onModeChange = viewModel::selectThemeMode,
            onAccentChange = viewModel::selectThemeAccent,
            onPaletteStyleChange = viewModel::selectThemePaletteStyle,
            onColorSpecChange = viewModel::selectThemeColorSpec,
            onMonetChange = viewModel::setMonetEnabled,
            onBlurChange = viewModel::setBlurEnabled,
            onFloatingBottomBarChange = viewModel::setFloatingBottomBarEnabled,
            onFloatingBottomBarBlurChange = viewModel::setFloatingBottomBarBlurEnabled,
            onPredictiveBackChange = viewModel::setPredictiveBackEnabled,
            onUiScaleChange = viewModel::setUiScale
        )
        SettingsRoute.Playback -> PlaybackSettingsScreen(
            playbackSettings = uiState.playbackSettings,
            themeSettings = uiState.themeSettings,
            onBack = viewModel::closeThemeSettings,
            onQualityChange = viewModel::selectPlaybackQuality,
            onLyricDisplayModeChange = viewModel::selectLyricDisplayMode
        )
        SettingsRoute.Source -> SourceSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = viewModel::closeThemeSettings
        )
        SettingsRoute.Donation -> DonationSettingsScreen(
            themeSettings = uiState.themeSettings,
            onBack = viewModel::closeThemeSettings
        )
    }
}

@Composable
private fun SettingsHomeScreen(
    viewModel: SiListenViewModel,
    themeSettings: ThemeSettingsState,
    padding: PaddingValues
) {
    val dark = themeSettings.resolveDarkTheme()
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
            Text(
                "设置",
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "管理界面、播放体验和账号偏好。",
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            ThemeSettingsGroup(containerColor = cardColor) {
                KernelSuUiModePreference(
                    selected = themeSettings.uiMode,
                    dark = dark,
                    onSelect = viewModel::selectThemeUiMode
                )
                ThemeDivider(if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFEFF2))
                SettingsNavigationRowContent(
                    title = "主题",
                    subtitle = "主题模式、动态取色、关键色、模糊和页面缩放",
                    mark = "Aa",
                    markColor = Color(0xFF1ED760),
                    mutedText = mutedText,
                    dark = dark,
                    onClick = viewModel::openThemeSettings
                )
            }
        }
        item {
            SettingsNavigationRow(
                title = "播放设置",
                subtitle = "音质、缓存和播放队列偏好",
                mark = "i",
                markColor = Color(0xFFFFC857),
                cardColor = cardColor,
                mutedText = mutedText,
                dark = dark,
                onClick = viewModel::openPlaybackSettings
            )
        }
        item {
            SettingsNavigationRow(
                title = "音源与扩展",
                subtitle = "切换默认音源，查看后续可扩展的音乐来源",
                mark = "云",
                markColor = Color(0xFF8BD3FF),
                cardColor = cardColor,
                mutedText = mutedText,
                dark = dark,
                onClick = viewModel::openSourceSettings
            )
        }
        item {
            SettingsNavigationRow(
                title = "赞赏开发者",
                subtitle = "赞赏入口还没有设置，稍后可以在这里展示支持方式",
                mark = "￥",
                markColor = Color(0xFFFFC857),
                cardColor = cardColor,
                mutedText = mutedText,
                dark = dark,
                onClick = viewModel::openDonationSettings
            )
        }
        item {
            SettingsNavigationRow(
                title = "关于 SiListen",
                subtitle = "网易云默认音源，可继续扩展其他 API",
                mark = "i",
                markColor = Color(0xFF8BD3FF),
                cardColor = cardColor,
                mutedText = mutedText,
                dark = dark,
                onClick = {}
            )
        }
    }
}

@Composable
private fun PageTopTitle(title: String, dark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = if (dark) Color.White else Color(0xFF111111),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
private fun SectionTitle(title: String, dark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
private fun SectionBulletTitle(title: String, dark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = "• $title",
        color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun PageHeroCard(
    title: String,
    subtitle: String,
    dark: Boolean,
    trailing: @Composable (() -> Unit)? = null
) {
    val cardColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val borderColor = if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)
    Surface(
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    color = if (dark) Color.White else Color(0xFF111111),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SearchFloatingGlyph(dark: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFF2F4F7),
        shape = CircleShape,
        modifier = modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = if (dark) Color.White else Color(0xFF111111)
            )
        }
    }
}

@Composable
private fun SearchActionButton(
    dark: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    PrimaryActionButton(
        text = if (loading) "搜索中" else "搜索",
        onClick = onClick,
        enabled = !loading,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
private fun StatusMessageCard(text: String, dark: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor.copy(alpha = if (enabled) 1f else 0.55f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.noRippleClick(shape = RoundedCornerShape(999.dp)) {
            if (enabled && !loading) onClick()
        }
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Surface(
        color = if (dark) Color.White.copy(alpha = if (enabled) 0.08f else 0.04f) else Color.White.copy(alpha = if (enabled) 1f else 0.55f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.noRippleClick(shape = RoundedCornerShape(999.dp)) {
            if (enabled && !loading) onClick()
        }
    ) {
        Text(
            text = text,
            color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String = "",
    subtitle: String = "",
    dark: Boolean,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    StatusMessageCard(
        text = text ?: "$title\n$subtitle",
        dark = dark,
        modifier = modifier
    )
}

@Composable
private fun LoadingStateCard(
    title: String = "",
    subtitle: String = "",
    dark: Boolean,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    StatusMessageCard(
        text = text ?: "$title\n$subtitle",
        dark = dark,
        modifier = modifier
    )
}

@Composable
private fun SourceBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun LyricStageParticles() {
    Box(modifier = Modifier.fillMaxSize())
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

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    mark: String,
    markColor: Color,
    cardColor: Color,
    mutedText: Color,
    dark: Boolean,
    onClick: () -> Unit
) {
    val markTextColor = if (markColor.luminance() < 0.45f) Color.White else Color(0xFF111111)
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(20.dp), onClick = onClick)
    ) {
        SettingsNavigationRowContent(
            title = title,
            subtitle = subtitle,
            mark = mark,
            markColor = markColor,
            mutedText = mutedText,
            dark = dark,
            onClick = onClick
        )
    }
}

@Composable
private fun SettingsNavigationRowContent(
    title: String,
    subtitle: String,
    mark: String,
    markColor: Color,
    mutedText: Color,
    dark: Boolean,
    onClick: () -> Unit
) {
    val markTextColor = if (markColor.luminance() < 0.45f) Color.White else Color(0xFF111111)
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(markColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Text(mark, color = markTextColor, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold)
            Text(subtitle, color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = mutedText)
    }
}

@Composable
private fun KernelSuUiModePreference(
    selected: ThemeUiModeOption,
    dark: Boolean,
    onSelect: (ThemeUiModeOption) -> Unit
) {
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF5B8CFF).copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "界面模式",
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                fontWeight = FontWeight.Bold
            )
            Text("选择更喜欢的界面质感", color = mutedText, style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier
                .width(154.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFE9EAEE))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            KernelSuUiModeSegment("Miuix", ThemeUiModeOption.Miuix, selected, dark, onSelect, Modifier.weight(1f))
            KernelSuUiModeSegment("M3", ThemeUiModeOption.Material3, selected, dark, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun KernelSuUiModeSegment(
    label: String,
    value: ThemeUiModeOption,
    selected: ThemeUiModeOption,
    dark: Boolean,
    onSelect: (ThemeUiModeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = value == selected
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (active) {
                    if (dark) MaterialTheme.colorScheme.primary else Color.White
                } else {
                    Color.Transparent
                }
            )
            .noRippleClick(RoundedCornerShape(13.dp)) { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                active && dark -> MaterialTheme.colorScheme.onPrimary
                active -> MaterialTheme.colorScheme.primary
                dark -> Color.White.copy(alpha = 0.82f)
                else -> Color(0xFF111111).copy(alpha = 0.72f)
            },
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ThemeSettingsScreen(
    state: ThemeSettingsState,
    onBack: () -> Unit,
    onUiModeChange: (ThemeUiModeOption) -> Unit,
    onModeChange: (ThemeModeOption) -> Unit,
    onAccentChange: (ThemeAccentOption) -> Unit,
    onPaletteStyleChange: (ThemePaletteStyleOption) -> Unit,
    onColorSpecChange: (ThemeColorSpecOption) -> Unit,
    onMonetChange: (Boolean) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    onFloatingBottomBarChange: (Boolean) -> Unit,
    onFloatingBottomBarBlurChange: (Boolean) -> Unit,
    onPredictiveBackChange: (Boolean) -> Unit,
    onUiScaleChange: (Float) -> Unit
) {
    val dark = state.resolveDarkTheme()
    val pageColor = if (dark) Color(0xFF050805) else Color(0xFFF6F6F8)
    val panelColor = if (dark) Color(0xFF121A14) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF7A7A7A)
    val dividerColor = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFEFF2)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = titleColor)
            }
            Text(
                text = "主题设置",
                color = titleColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        item {
            ThemePreviewCard(state)
        }
        item {
            KernelSuThemeModeTabs(selected = state.mode, dark = dark, onSelect = onModeChange)
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                ThemeToggleItem(
                    title = "动态取色",
                    subtitle = if (state.uiMode == ThemeUiModeOption.Miuix) {
                        "开启后会根据关键色生成更统一的界面配色"
                    } else {
                        "开启后会使用关键色调整按钮、卡片和强调色"
                    },
                    mark = "?",
                    checked = state.monetEnabled,
                    onCheckedChange = onMonetChange,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
                AnimatedVisibility(visible = state.monetEnabled) {
                    Column {
                        ThemeDivider(dividerColor)
                        KernelSuKeyColorPreference(
                            state = state,
                            dark = dark,
                            onAccentChange = onAccentChange
                        )
                        ThemeDivider(dividerColor)
                        KernelSuDropdownPreference(
                            title = "色彩风格",
                            subtitle = state.paletteStyle.label,
                            mark = "S",
                            textColor = titleColor,
                            mutedColor = mutedColor
                        ) {
                            ThemePaletteStyleOption.entries.forEach { style ->
                                ThemeOptionChip(
                                    text = style.label,
                                    selected = state.paletteStyle == style,
                                    dark = dark,
                                    onClick = { onPaletteStyleChange(style) }
                                )
                            }
                        }
                        ThemeDivider(dividerColor)
                        KernelSuDropdownPreference(
                            title = "色彩规范",
                            subtitle = state.colorSpec.label,
                            mark = "25",
                            textColor = titleColor,
                            mutedColor = mutedColor
                        ) {
                            ThemeColorSpecOption.entries.forEach { spec ->
                                ThemeOptionChip(
                                    text = spec.label,
                                    selected = state.colorSpec == spec,
                                    dark = dark,
                                    onClick = { onColorSpecChange(spec) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                ThemeToggleItem(
                    title = "启用模糊",
                    subtitle = "让卡片和底部区域呈现柔和的半透明质感",
                    mark = "?",
                    checked = state.blurEnabled,
                    onCheckedChange = onBlurChange,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
                ThemeDivider(dividerColor)
                ThemeToggleItem(
                    title = "悬浮底栏",
                    subtitle = "使用更轻盈的悬浮式底部导航",
                    mark = "▔",
                    checked = state.floatingBottomBarEnabled,
                    onCheckedChange = onFloatingBottomBarChange,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
                AnimatedVisibility(
                    visible = state.floatingBottomBarEnabled &&
                        state.uiMode == ThemeUiModeOption.Miuix &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    Column {
                        ThemeDivider(dividerColor)
                        ThemeToggleItem(
                            title = "底栏玻璃",
                            subtitle = "为底部导航加入玻璃模糊效果",
                            mark = "?",
                            checked = state.floatingBottomBarBlurEnabled,
                            onCheckedChange = onFloatingBottomBarBlurChange,
                            textColor = titleColor,
                            mutedColor = mutedColor
                        )
                    }
                }
            }
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                ThemeToggleItem(
                    title = "预测性返回手势",
                    subtitle = "启用对预测性返回手势的支持",
                    mark = "≡",
                    checked = state.predictiveBackEnabled,
                    onCheckedChange = onPredictiveBackChange,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
                ThemeDivider(dividerColor)
                UiScaleItem(
                    value = state.uiScale,
                    onValueChange = onUiScaleChange,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
            }
        }

    }
}

@Composable
private fun DonationSettingsScreen(
    themeSettings: ThemeSettingsState,
    onBack: () -> Unit
) {
    val dark = themeSettings.resolveDarkTheme()
    val pageColor = if (dark) Color(0xFF050805) else Color(0xFFF6F6F8)
    val panelColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = titleColor)
            }
            Text(
                text = "赞赏开发者",
                color = titleColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "用于后续展示微信或支付宝收款码。",
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Surface(
                color = panelColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
                shadowElevation = if (themeSettings.blurEnabled) 18.dp else 4.dp,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1ED760).copy(alpha = if (dark) 0.75f else 0.22f),
                                        Color(0xFFFFC857).copy(alpha = if (dark) 0.46f else 0.22f),
                                        Color(0xFF8BD3FF).copy(alpha = if (dark) 0.42f else 0.24f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("暂未设置", color = titleColor, fontWeight = FontWeight.Black)
                            Text("设置后会显示赞赏二维码", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = "把真实收款二维码放到资源目录后，这里会改成可直接展示的赞赏卡片。",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                ThemeToggleItem(
                    title = "赞赏入口",
                    subtitle = "设置页已提供入口，等待接入真实收款码素材",
                    mark = "￥",
                    checked = false,
                    onCheckedChange = {},
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
            }
        }

    }
}

@Composable
private fun PlaybackSettingsScreen(
    playbackSettings: PlaybackSettingsState,
    themeSettings: ThemeSettingsState,
    onBack: () -> Unit,
    onQualityChange: (PlaybackQuality) -> Unit,
    onLyricDisplayModeChange: (LyricDisplayMode) -> Unit
) {
    val dark = themeSettings.resolveDarkTheme()
    val pageColor = if (dark) Color(0xFF050805) else Color(0xFFF6F6F8)
    val panelColor = if (dark) Color(0xFF121A14) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    val dividerColor = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFEFF2)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = titleColor)
            }
            Text(
                text = "播放设置",
                color = titleColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "音质设置会影响新获取的网易云播放地址。",
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                LyricDisplayModeItem(
                    selectedMode = playbackSettings.lyricDisplayMode,
                    onModeChange = onLyricDisplayModeChange,
                    textColor = titleColor,
                    mutedColor = mutedColor,
                    dark = dark
                )
            }
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                PlaybackQuality.values().forEachIndexed { index, quality ->
                    PlaybackQualityItem(
                        quality = quality,
                        selected = quality == playbackSettings.quality,
                        textColor = titleColor,
                        mutedColor = mutedColor,
                        onClick = { onQualityChange(quality) }
                    )
                    if (index != PlaybackQuality.values().lastIndex) {
                        ThemeDivider(dividerColor)
                    }
                }
            }
        }
        item {
            Surface(
                color = panelColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "当前：${playbackSettings.quality.label}。切换后会用于下一次解析播放地址；已经缓存的旧地址会按音质区分，不会互相覆盖。",
                    color = mutedColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceSettingsScreen(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    onBack: () -> Unit
) {
    val dark = uiState.themeSettings.resolveDarkTheme()
    val pageColor = if (dark) Color(0xFF050805) else Color(0xFFF6F6F8)
    val panelColor = if (dark) Color(0xFF121A14) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = titleColor)
            }
            Text(
                text = "音源与扩展",
                color = titleColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "把默认音源和未来扩展入口统一收进设置，避免音乐库页信息过杂。",
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(viewModel.registry.all()) { source ->
            Surface(
                color = panelColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFECECEF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SourceRow(
                    title = source.info.name,
                    subtitle = source.info.description,
                    badge = source.info.badge,
                    accent = Color(source.info.accentHex.toInt()),
                    selected = source.info.id == uiState.selectedSourceId,
                    titleColor = titleColor,
                    subtitleColor = mutedColor,
                    onClick = { viewModel.selectSource(source.info.id) }
                )
            }
        }
        item {
            Surface(
                color = panelColor,
                border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFE7E7EA)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Cloud, contentDescription = null, tint = Color(0xFF8BD3FF))
                        Spacer(Modifier.width(8.dp))
                        Text("扩展音源", color = titleColor, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "接入 QQ 音乐、酷狗或自建网关时，新建一个音源实现推荐、搜索和播放地址解析，再注册到音源列表。",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricDisplayModeItem(
    selectedMode: LyricDisplayMode,
    onModeChange: (LyricDisplayMode) -> Unit,
    textColor: Color,
    mutedColor: Color,
    dark: Boolean
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("歌词风格", color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Glass 参考 Apple Music 毛玻璃歌词，Particles 参考 Mineradio 的粒子化舞台氛围。",
            color = mutedColor,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LyricModeChip(
                label = "Glass",
                subtitle = "简洁毛玻璃",
                selected = selectedMode == LyricDisplayMode.Glass,
                dark = dark,
                onClick = { onModeChange(LyricDisplayMode.Glass) },
                modifier = Modifier.weight(1f)
            )
            LyricModeChip(
                label = "Particles",
                subtitle = "粒子舞台",
                selected = selectedMode == LyricDisplayMode.Particles,
                dark = dark,
                onClick = { onModeChange(LyricDisplayMode.Particles) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LyricModeChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = when {
            selected && dark -> accent.copy(alpha = 0.22f)
            selected -> accent.copy(alpha = 0.14f)
            dark -> Color.White.copy(alpha = 0.05f)
            else -> Color(0xFFF4F5F7)
        },
        border = BorderStroke(1.dp, if (selected) accent else Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = label,
                color = if (selected) accent else if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PlaybackQualityItem(
    quality: PlaybackQuality,
    selected: Boolean,
    textColor: Color,
    mutedColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else mutedColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (selected) "?" else "${quality.bitrate}",
                color = if (selected) MaterialTheme.colorScheme.onPrimary else mutedColor,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(quality.label, color = textColor, fontWeight = FontWeight.Bold)
            Text(quality.description, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        if (selected) {
            SourceBadge("当前", MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ThemePreviewCard(state: ThemeSettingsState) {
    val dark = state.resolveDarkTheme()
    val accent = state.accentColor()
    val accentOn = state.onAccentColor()
    val previewBackground = if (dark) Color(0xFF101510) else Color.White
    val previewText = if (dark) Color.White else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB6C8B8) else Color(0xFF7A7A7A)
    val miuixMode = state.uiMode == ThemeUiModeOption.Miuix
    val glassAlpha = when {
        state.blurEnabled -> 0.84f
        else -> 1f
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = previewBackground,
            border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0x22000000)),
            shadowElevation = if (miuixMode && state.blurEnabled) 18.dp else 6.dp,
            shape = RoundedCornerShape(if (miuixMode) 38.dp else 24.dp),
            modifier = Modifier
                .width(218.dp)
                .height(346.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SiListen", color = previewText, fontWeight = FontWeight.Black)
                        Text(
                            if (miuixMode) "Miuix" else "Material 3",
                            color = mutedText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
                Surface(
                    color = if (dark) Color.White.copy(alpha = if (miuixMode) 0.10f else 0.07f) else Color(0xFFECEEF1),
                    shape = RoundedCornerShape(if (miuixMode) 20.dp else 14.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("今日推荐", color = previewText, fontWeight = FontWeight.Bold)
                        Text("iOS 质感播放页", color = mutedText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accent, Color(0xFF8BD3FF), Color(0xFFFFC857))
                                    )
                                )
                        )
                    }
                }
                repeat(3) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(listOf(accent, Color(0xFFFF6B6B), Color(0xFF8BD3FF))[index])
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 0) 0.86f else 0.66f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(previewText.copy(alpha = 0.18f))
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(mutedText.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = Color.Transparent,
                    border = if (miuixMode && state.blurEnabled) BorderStroke(1.dp, Color.White.copy(alpha = if (dark) 0.18f else 0.58f)) else null,
                    shadowElevation = if (miuixMode && state.floatingBottomBarEnabled) 12.dp else 0.dp,
                    shape = RoundedCornerShape(if (state.floatingBottomBarEnabled) 999.dp else 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (miuixMode && state.blurEnabled) {
                                    liquidGlassBrush(dark, accent, glassAlpha)
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            (if (dark) Color(0xFF101510) else Color.White).copy(alpha = glassAlpha),
                                            (if (dark) Color(0xFF101510) else Color.White).copy(alpha = glassAlpha)
                                        )
                                    )
                                },
                                RoundedCornerShape(if (state.floatingBottomBarEnabled) 999.dp else 8.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(if (miuixMode && state.blurEnabled) liquidGlassHighlightBrush(dark) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(if (state.floatingBottomBarEnabled) 10.dp else 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("?", "?", "?").forEachIndexed { index, label ->
                                Text(
                                    label,
                                    color = if (index == 1) accentOn else previewText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = if (index == 1) {
                                        Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(accent)
                                            .padding(horizontal = 14.dp, vertical = 4.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeUiModeSelector(
    selected: ThemeUiModeOption,
    dark: Boolean,
    onSelect: (ThemeUiModeOption) -> Unit
) {
    Surface(
        color = if (dark) Color(0xFF121A14) else Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "界面模式",
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                fontWeight = FontWeight.Black
            )
            Text(
                "选择适合你的界面风格，预览会同步展示主要颜色和底部栏效果。",
                color = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72),
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFE8E8ED))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeUiModeSegment("Material 3", ThemeUiModeOption.Material3, selected, dark, onSelect, Modifier.weight(1f))
                ThemeUiModeSegment("Miuix", ThemeUiModeOption.Miuix, selected, dark, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeUiModeSegment(
    label: String,
    value: ThemeUiModeOption,
    selected: ThemeUiModeOption,
    dark: Boolean,
    onSelect: (ThemeUiModeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selected == value
    val bg by animateColorAsState(
        targetValue = if (isSelected) {
            if (dark) MaterialTheme.colorScheme.primary else Color.White
        } else {
            Color.Transparent
        },
        label = "theme-ui-mode-bg"
    )
    val fg by animateColorAsState(
        targetValue = when {
            isSelected && dark -> MaterialTheme.colorScheme.onPrimary
            isSelected -> MaterialTheme.colorScheme.primary
            dark -> Color(0xFFE9F7EC)
            else -> Color(0xFF111111)
        },
        label = "theme-ui-mode-fg"
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .noRippleClick(RoundedCornerShape(14.dp)) { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold)
    }
}

@Composable
private fun KernelSuThemeModeTabs(
    selected: ThemeModeOption,
    dark: Boolean,
    onSelect: (ThemeModeOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) Color(0xFF121A14) else Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeModeSegment("跟随系统", ThemeModeOption.System, selected, dark, onSelect)
        ThemeModeSegment("浅色", ThemeModeOption.Light, selected, dark, onSelect)
        ThemeModeSegment("深色", ThemeModeOption.Dark, selected, dark, onSelect)
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeModeOption,
    dark: Boolean,
    onSelect: (ThemeModeOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) Color(0xFF1B241D) else Color(0xFFE8E8ED))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ThemeModeSegment("跟随系统", ThemeModeOption.System, selected, dark, onSelect)
        ThemeModeSegment("浅色", ThemeModeOption.Light, selected, dark, onSelect)
        ThemeModeSegment("深色", ThemeModeOption.Dark, selected, dark, onSelect)
    }
}

@Composable
private fun KernelSuKeyColorPreference(
    state: ThemeSettingsState,
    dark: Boolean,
    onAccentChange: (ThemeAccentOption) -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemeRowMark("●")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("关键色", color = titleColor, fontWeight = FontWeight.Bold)
                Text(state.accent.label, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ThemeAccentOption.entries) { accent ->
                val selected = accent == state.accent
                val color = accent.color
                Surface(
                    color = if (selected) color.copy(alpha = 0.18f) else if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFF2F3F6),
                    border = BorderStroke(1.dp, if (selected) color else Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.noRippleClick(RoundedCornerShape(16.dp)) { onAccentChange(accent) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                        Text(
                            accent.label,
                            color = titleColor,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KernelSuDropdownPreference(
    title: String,
    subtitle: String,
    mark: String,
    textColor: Color,
    mutedColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemeRowMark(mark)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = textColor, fontWeight = FontWeight.Bold)
                Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = mutedColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ThemeAccentSelector(
    state: ThemeSettingsState,
    dark: Boolean,
    onSelect: (ThemeAccentOption) -> Unit
) {
    val options = remember {
        listOf(
            ThemeAccentOption.Emerald to "绿黑",
            ThemeAccentOption.Rose to "玫红",
            ThemeAccentOption.Sky to "冰蓝",
            ThemeAccentOption.Amber to "琥珀",
            ThemeAccentOption.Violet to "紫雾"
        )
    }
    Surface(
        color = if (dark) Color(0xFF121A14) else Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "主题强调色",
                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                fontWeight = FontWeight.Black
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(options) { option ->
                    val selected = option.first == state.accent
                    val color = when (option.first) {
                        ThemeAccentOption.Emerald -> Color(0xFF1ED760)
                        ThemeAccentOption.Rose -> Color(0xFFFF5C7C)
                        ThemeAccentOption.Sky -> Color(0xFF4C9FFF)
                        ThemeAccentOption.Amber -> Color(0xFFFFB020)
                        ThemeAccentOption.Violet -> Color(0xFF8F6BFF)
                    }
                    Surface(
                        color = if (selected) color.copy(alpha = 0.16f) else if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFFF4F5F7),
                        border = BorderStroke(1.dp, if (selected) color else Color.Transparent),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable { onSelect(option.first) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                option.second,
                                color = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private val ThemeAccentOption.label: String
    get() = when (this) {
        ThemeAccentOption.Emerald -> "默认绿"
        ThemeAccentOption.Rose -> "玫瑰红"
        ThemeAccentOption.Sky -> "天空蓝"
        ThemeAccentOption.Amber -> "琥珀黄"
        ThemeAccentOption.Violet -> "紫罗兰"
    }

private val ThemeAccentOption.color: Color
    get() = when (this) {
        ThemeAccentOption.Emerald -> Color(0xFF1ED760)
        ThemeAccentOption.Rose -> Color(0xFFFF5C7C)
        ThemeAccentOption.Sky -> Color(0xFF4C9FFF)
        ThemeAccentOption.Amber -> Color(0xFFFFB020)
        ThemeAccentOption.Violet -> Color(0xFF8F6BFF)
    }

@Composable
private fun ThemeAdvancedColorOptions(
    state: ThemeSettingsState,
    dark: Boolean,
    onPaletteStyleChange: (ThemePaletteStyleOption) -> Unit,
    onColorSpecChange: (ThemeColorSpecOption) -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    Surface(
        color = if (dark) Color(0xFF121A14) else Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("动态色算法", color = titleColor, fontWeight = FontWeight.Black)
            Text(
                "调整关键色生成主题时的取色倾向，让界面更贴近你的偏好。",
                color = mutedColor,
                style = MaterialTheme.typography.bodySmall
            )
            Text("色彩风格", color = mutedColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemePaletteStyleOption.entries) { style ->
                    ThemeOptionChip(
                        text = style.label,
                        selected = state.paletteStyle == style,
                        dark = dark,
                        onClick = { onPaletteStyleChange(style) }
                    )
                }
            }
            Text("色彩规范", color = mutedColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeColorSpecOption.entries.forEach { spec ->
                    ThemeOptionChip(
                        text = spec.label,
                        selected = state.colorSpec == spec,
                        dark = dark,
                        onClick = { onColorSpecChange(spec) }
                    )
                }
            }
        }
    }
}

private val ThemePaletteStyleOption.label: String
    get() = when (this) {
        ThemePaletteStyleOption.TonalSpot -> "柔和色调"
        ThemePaletteStyleOption.Vibrant -> "鲜明"
        ThemePaletteStyleOption.Expressive -> "表现力"
        ThemePaletteStyleOption.Fidelity -> "忠实"
        ThemePaletteStyleOption.Content -> "内容取色"
        ThemePaletteStyleOption.Monochrome -> "单色"
    }

private val ThemeColorSpecOption.label: String
    get() = when (this) {
        ThemeColorSpecOption.Default -> "默认"
        ThemeColorSpecOption.Spec2021 -> "2021 规范"
        ThemeColorSpecOption.Spec2025 -> "2025 规范"
    }

@Composable
private fun ThemeOptionChip(
    text: String,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF0F1F4),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else if (dark) Color(0xFFE9F7EC) else Color(0xFF111111),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.noRippleClick(RoundedCornerShape(999.dp), onClick)
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RowScope.ThemeModeSegment(
    label: String,
    value: ThemeModeOption,
    selected: ThemeModeOption,
    dark: Boolean,
    onSelect: (ThemeModeOption) -> Unit
) {
    val isSelected = value == selected
    Box(
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) {
                    if (dark) MaterialTheme.colorScheme.primary else Color.White
                } else {
                    Color.Transparent
                }
            )
            .clickable { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected && dark) {
                MaterialTheme.colorScheme.onPrimary
            } else if (dark) {
                Color(0xFFE9F7EC)
            } else {
                Color(0xFF111111)
            },
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ThemeToggleRow(
    title: String,
    subtitle: String,
    mark: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    containerColor: Color,
    textColor: Color,
    mutedColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ThemeToggleItem(
            title = title,
            subtitle = subtitle,
            mark = mark,
            checked = checked,
            onCheckedChange = onCheckedChange,
            textColor = textColor,
            mutedColor = mutedColor
        )
    }
}

@Composable
private fun ThemeSettingsGroup(
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun ThemeToggleItem(
    title: String,
    subtitle: String,
    mark: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeRowMark(mark)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontWeight = FontWeight.Bold)
            Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeDivider(color: Color) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 64.dp)
            .background(color)
    )
}

@Composable
private fun UiScaleItem(
    value: Float,
    onValueChange: (Float) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        ThemeRowMark("?")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("界面缩放", color = textColor, fontWeight = FontWeight.Bold)
                Text("调整全局显示比例", color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
            Text("${(value * 100).toInt()}%", color = mutedColor, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = mutedColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.85f..1.15f
        )
    }
}

@Composable
private fun ThemeRowMark(mark: String) {
    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Text(mark, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
    }
}

private fun liquidGlassBrush(
    dark: Boolean,
    accent: Color,
    alpha: Float = 1f
): Brush = Brush.linearGradient(
    listOf(
        (if (dark) Color(0xFF18221B) else Color.White).copy(alpha = if (dark) 0.82f * alpha else 0.78f * alpha),
        accent.copy(alpha = if (dark) 0.12f * alpha else 0.10f * alpha),
        Color.White.copy(alpha = if (dark) 0.10f * alpha else 0.52f * alpha)
    )
)

private fun liquidGlassHighlightBrush(dark: Boolean): Brush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = if (dark) 0.22f else 0.72f),
        Color.White.copy(alpha = if (dark) 0.08f else 0.18f),
        Color.Transparent
    )
)

@Composable
private fun FeaturePlaylistCard(
    playlist: MusicPlaylist,
    icon: ImageVector,
    accent: Color,
    dark: Boolean,
    onClick: () -> Unit
) {
    val container = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val primary = if (dark) Color(0xFFF5FFF6) else Color(0xFF111111)
    val muted = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val iconTint = if (accent.luminance() < 0.45f) Color.White else Color(0xFF081008)
    Surface(
        color = container,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = if (dark) 0.88f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (dark) iconTint else accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
            Text(playlist.title, color = primary, fontWeight = FontWeight.Black)
                Text(
                    playlist.subtitle,
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (playlist.songs.isNotEmpty()) {
                Text("${playlist.songs.size}", color = if (dark) accent.copy(alpha = 0.92f) else accent, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = muted)
        }
    }
}

@Composable
private fun PlaylistTile(playlist: MusicPlaylist, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (dark) Color(0xFFF5FFF6) else Color(0xFF111111)
    val subtitleColor = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    Surface(
        modifier = Modifier
            .width(168.dp)
            .noRippleClick(shape = RoundedCornerShape(24.dp), onClick = onClick),
        color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFE7E7EA)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Box {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF232323))
                )
                Surface(
                    color = Color(0xFF050505).copy(alpha = 0.48f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "查看",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(playlist.title, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(playlist.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, color = subtitleColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    liked: Boolean,
    likeLoading: Boolean,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (dark) Color(0xFFF5FFF6) else Color(0xFF111111)
    val mutedText = if (dark) Color(0xFFB8C1B9) else Color(0xFF5F6368)
    val heartTint by animateColorAsState(
        targetValue = if (liked) Color(0xFFFF5C7C) else mutedText,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "song-heart-color"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (liked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "song-heart-scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF242424))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = "${song.artist} - ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = mutedText,
                style = MaterialTheme.typography.bodySmall
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
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.8.dp,
                        color = heartTint
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "红心",
                        tint = heartTint,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SourceRow(
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    selected: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rowColor = when {
        selected && dark -> accent.copy(alpha = 0.28f)
        selected -> accent.copy(alpha = 0.18f)
        dark -> Color.White.copy(alpha = 0.10f)
        else -> Color.White.copy(alpha = 0.78f)
    }
    Surface(
        color = rowColor,
        border = if (selected) BorderStroke(1.dp, accent) else null,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val badgeTint = if (accent.luminance() < 0.45f) Color.White else Color(0xFF111111)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = badgeTint)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = titleColor, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = subtitleColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SourceBadge(badge, accent)
        }
    }
}

@Composable
private fun MiniPlayer(
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
    val container = if (darkTheme) Color(0xC91B1C20) else Color.White.copy(alpha = if (glassy) 0.62f else 0.96f)
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
            blurRadius = if (glassy) 34.dp else 22.dp,
            tintAlpha = if (darkTheme) 0.09f else 0.11f,
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
                    .blur(if (glassy) 8.dp else 0.dp)
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
                            model = song?.coverUrl,
                            contentDescription = song?.title ?: "当前播放",
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
private fun PlaybackErrorBar(
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

@Composable
private fun FullPlayer(
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
    val dark = themeSettings.resolveDarkTheme()
    val accent = themeSettings.accentColor()
    var bottomPanel by remember(song?.id, initialPanel) {
        mutableStateOf(initialPanel.toBottomPanel())
    }
    val activeLyricIndex = remember(lyrics, playback.positionMs) {
        lyrics.indexOfLast { it.timeMs <= playback.positionMs }.coerceAtLeast(0)
    }
    val duration = playback.durationMs.coerceAtLeast(1L)
    val heartTint by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF5C7C) else if (dark) Color.White.copy(alpha = 0.82f) else Color(0xFF111111).copy(alpha = 0.82f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "player-heart-color"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "player-heart-scale"
    )
    val pageForeground = Color.White.copy(alpha = 0.92f)
    val pageMuted = Color.White.copy(alpha = 0.58f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(820.dp)
            .navigationBarsPadding()
    ) {
        AsyncImage(
            model = song?.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(46.dp)
                .graphicsLayer { alpha = 0.54f }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFF7A3D).copy(alpha = 0.88f),
                            accent.copy(alpha = 0.34f),
                            Color(0xFF8A5A43).copy(alpha = 0.90f),
                            Color(0xFF18242A).copy(alpha = 0.98f)
                        )
                    )
                )
        )
        if (lyricDisplayMode == LyricDisplayMode.Particles) {
            LyricStageParticles()
        }
        CinematicBars()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bottomPanel == PlayerBottomPanel.Detail) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = pageForeground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    IconButton(
                        onClick = { bottomPanel = PlayerBottomPanel.Playlist },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "播放列表",
                            tint = pageForeground
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                PlayerArtworkStage(song = song, accent = accent, dark = true)
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = song?.title ?: "暂无播放",
                            color = pageForeground,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = song?.artist ?: "未知歌手",
                            color = pageMuted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (song?.sourceId == "netease") {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
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
                Spacer(Modifier.weight(1f))
                PlayerTransportControls(
                    playback = playback,
                    dark = true,
                    onToggle = onToggle,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
                Spacer(Modifier.height(18.dp))
                PlayerWaveformProgressSection(
                    playback = playback,
                    duration = duration,
                    accent = Color.White.copy(alpha = 0.88f),
                    onSeek = onSeek
                )
                Spacer(Modifier.height(16.dp))
                PlayerBottomActionBar(
                    playback = playback,
                    dark = true,
                    lyricDisplayMode = lyricDisplayMode,
                    selectedPanel = bottomPanel,
                    onSelectPanel = { bottomPanel = it }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song?.coverUrl,
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = listOf(song?.title.orEmpty(), song?.artist.orEmpty()).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "暂无播放" },
                        color = pageForeground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "播放或暂停",
                            tint = pageForeground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PlayerBottomActionBar(
                    playback = playback,
                    dark = true,
                    lyricDisplayMode = lyricDisplayMode,
                    selectedPanel = bottomPanel,
                    onSelectPanel = { bottomPanel = it }
                )
                Spacer(Modifier.height(18.dp))
                AnimatedContent(
                    targetState = bottomPanel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    transitionSpec = {
                        val movingForward = targetState.panelOrder >= initialState.panelOrder
                        (slideInHorizontally(
                            animationSpec = tween(240),
                            initialOffsetX = { fullWidth ->
                                if (movingForward) fullWidth / 7 else -fullWidth / 7
                            }
                        ) + fadeIn(animationSpec = tween(180)) + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(220)
                        ))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { fullWidth ->
                                        if (movingForward) -fullWidth / 8 else fullWidth / 8
                                    }
                                ) + fadeOut(animationSpec = tween(160)) + scaleOut(
                                    targetScale = 0.985f,
                                    animationSpec = tween(180)
                                )
                            )
                    },
                    label = "player-panel-content"
                ) { panel ->
                    when (panel) {
                        PlayerBottomPanel.Lyrics -> when (lyricDisplayMode) {
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
                        PlayerBottomPanel.Playlist -> PlayerQueuePanel(
                            playback = playback,
                            dark = true,
                            accent = accent,
                            onSongSelect = onPlayQueueIndex
                        )
                        PlayerBottomPanel.Comments -> PlayerCommentsPanel(
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
                        PlayerBottomPanel.Detail -> PlayerRecordPanel(
                            song = song,
                            dark = true,
                            accent = accent
                        )
                    }
                }
            }
        }
    }
}

private enum class PlayerBottomPanel {
    Detail,
    Playlist,
    Lyrics,
    Comments
}

private fun PlayerSheetPanel.toBottomPanel(): PlayerBottomPanel = when (this) {
    PlayerSheetPanel.Detail -> PlayerBottomPanel.Detail
    PlayerSheetPanel.Queue -> PlayerBottomPanel.Playlist
    PlayerSheetPanel.Lyrics -> PlayerBottomPanel.Lyrics
    PlayerSheetPanel.Comments -> PlayerBottomPanel.Comments
}

private val PlayerBottomPanel.panelOrder: Int
    get() = when (this) {
        PlayerBottomPanel.Detail -> 0
        PlayerBottomPanel.Playlist -> 1
        PlayerBottomPanel.Lyrics -> 2
        PlayerBottomPanel.Comments -> 3
    }

@Composable
private fun PlayerTopBar(
    playback: PlaybackState,
    song: Song?,
    primaryText: Color,
    secondaryText: Color,
    accent: Color,
    heartTint: Color,
    heartScale: Float,
    isLikeLoading: Boolean,
    lyricDisplayMode: LyricDisplayMode,
    activePanelLabel: String,
    dark: Boolean,
    onToggle: () -> Unit,
    onToggleLike: (Song) -> Unit
) {
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.075f) else Color.White.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(30.dp), clip = false)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (dark) 0.05f else 0.34f),
                                accent.copy(alpha = if (dark) 0.10f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song?.coverUrl,
                    contentDescription = song?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.copy(alpha = 0.16f))
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = accent.copy(alpha = if (dark) 0.18f else 0.12f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = activePanelLabel,
                                color = accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Text(
                        text = song?.title ?: "暂无播放",
                        color = primaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(song?.artist.orEmpty(), song?.album.orEmpty())
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { if (lyricDisplayMode == LyricDisplayMode.Particles) "粒子歌词舞台" else "Apple Music 风格歌词" },
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(10.dp))
                if (song?.sourceId == "netease") {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.055f))
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
                                contentDescription = "红心",
                                tint = heartTint,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = heartScale
                                        scaleY = heartScale
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(14.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(primaryText)
                ) {
                    Icon(
                        if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "播放或暂停",
                        tint = if (dark) Color(0xFF111111) else Color.White,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerArtworkStage(
    song: Song?,
    accent: Color,
    dark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .blur(52.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = if (dark) 0.42f else 0.20f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        AsyncImage(
            model = song?.coverUrl,
            contentDescription = song?.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .shadow(34.dp, RoundedCornerShape(38.dp), clip = false)
                .clip(RoundedCornerShape(38.dp))
                .background(Color(0xFF252525))
        )
    }
}

@Composable
private fun PlayerProgressSection(
    playback: PlaybackState,
    duration: Long,
    dark: Boolean,
    accent: Color,
    onSeek: (Long) -> Unit
) {
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6A6D72)
    val progress = playback.positionMs.coerceIn(0L, duration).toFloat()
    Slider(
        value = progress,
        onValueChange = { onSeek(it.toLong()) },
        valueRange = 0f..duration.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
        )
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatTime(playback.positionMs),
            color = secondaryText,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            formatTime(playback.durationMs),
            color = secondaryText,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PlayerWaveformProgressSection(
    playback: PlaybackState,
    duration: Long,
    accent: Color,
    onSeek: (Long) -> Unit
) {
    val progress = playback.positionMs.coerceIn(0L, duration).toFloat()
    val activeFraction = (progress / duration.toFloat()).coerceIn(0f, 1f)
    val barHeights = remember {
        listOf(18, 34, 46, 22, 10, 30, 14, 42, 24, 50, 18, 34, 12, 38, 16, 44, 28, 54, 20, 36, 12, 48, 26, 58, 16, 40)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(playback.positionMs), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(formatTime(playback.durationMs), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEachIndexed { index, height ->
                val active = index <= (barHeights.lastIndex * activeFraction).fastRoundToInt()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) accent else Color.White.copy(alpha = 0.30f))
                )
            }
        }
        Slider(
            value = progress,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .matchParentSize()
                .alpha(0.01f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun PlayerTransportControls(
    playback: PlaybackState,
    dark: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val iconTint = if (dark) Color.White else Color(0xFF111111)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "播放或暂停",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun PlayerBottomActionBar(
    playback: PlaybackState,
    dark: Boolean,
    lyricDisplayMode: LyricDisplayMode,
    selectedPanel: PlayerBottomPanel,
    onSelectPanel: (PlayerBottomPanel) -> Unit
) {
    val items = listOf(
        PlayerActionItem(
            panel = PlayerBottomPanel.Detail,
            label = if (lyricDisplayMode == LyricDisplayMode.Glass) "唱片" else "舞台",
            icon = Icons.Rounded.Album
        ),
        PlayerActionItem(
            panel = PlayerBottomPanel.Playlist,
            label = "列表",
            icon = Icons.AutoMirrored.Rounded.QueueMusic
        ),
        PlayerActionItem(
            panel = PlayerBottomPanel.Lyrics,
            label = "歌词",
            icon = Icons.Rounded.Subtitles
        ),
        PlayerActionItem(
            panel = PlayerBottomPanel.Comments,
            label = "评论",
            icon = Icons.AutoMirrored.Rounded.Comment
        )
    )
    Surface(
        color = Color.White.copy(alpha = if (dark) 0.075f else 0.18f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (dark) 0.12f else 0.28f)),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(16.dp, RoundedCornerShape(30.dp), clip = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.panel == selectedPanel
                val tint by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else if (dark) {
                        Color.White.copy(alpha = 0.70f)
                    } else {
                        Color(0xFF111111).copy(alpha = 0.68f)
                    },
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
                    label = "player-bottom-action-tint"
                )
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.07f else 1f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
                    label = "player-bottom-action-scale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (dark) 0.14f else 0.72f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.18f else 0.10f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .noRippleClick(shape = RoundedCornerShape(22.dp)) { onSelectPanel(item.panel) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(if (selected) 30.dp else 27.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 26.dp else 6.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) tint.copy(alpha = 0.72f) else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

private data class PlayerActionItem(
    val panel: PlayerBottomPanel,
    val label: String,
    val icon: ImageVector
)

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
    val upcomingSongs = playback.queue
        .mapIndexed { index, song -> index to song }
        .drop(currentIndex + 1)
    LazyColumn(
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
private fun PlayerRecordPanel(
    song: Song?,
    dark: Boolean,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlayerPanelHeader(
            title = "唱片信息",
            subtitle = song?.title ?: "暂无播放",
            dark = dark
        )
        PlayerInfoCard(
            label = "歌曲",
            value = song?.title ?: "暂无播放",
            dark = dark,
            accent = accent
        )
        PlayerInfoCard(
            label = "歌手",
            value = song?.artist ?: "未知歌手",
            dark = dark,
            accent = accent
        )
        PlayerInfoCard(
            label = "专辑",
            value = song?.album ?: "未知专辑",
            dark = dark,
            accent = accent
        )
        PlayerInfoCard(
            label = "模式",
            value = "Apple Music 灵感的极简全屏播放器",
            dark = dark,
            accent = accent
        )
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
private fun PlayerInfoCard(
    label: String,
    value: String,
    dark: Boolean,
    accent: Color
) {
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val secondaryText = if (dark) Color(0xFFB8C1B9) else Color(0xFF6A6D72)
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    color = secondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
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
    val activeIndex = remember(lyricItems, playbackPositionMs) {
        lyricItems.indexOfLast { it.timeMs <= playbackPositionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    val duration = durationMs.coerceAtLeast(1L)
    val progress = playbackPositionMs.coerceIn(0L, duration).toFloat()

    LaunchedEffect(activeIndex, lyricItems.size) {
        if (lyricItems.isEmpty()) return@LaunchedEffect
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val centerOffset = if (viewportHeight > 0) {
            -((viewportHeight / 2f) - 132f).toInt()
        } else {
            -220
        }
        listState.animateScrollToItem(
            index = activeIndex.coerceIn(0, lyricItems.lastIndex),
            scrollOffset = centerOffset
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF0C0C0C))
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(56.dp)
                .graphicsLayer {
                    scaleX = 1.16f
                    scaleY = 1.16f
                    alpha = 0.78f
                }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.88f),
                            accent.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.56f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 20.dp, end = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = songTitle.orEmpty().ifBlank { "未知歌曲" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = songArtist.orEmpty().ifBlank { "未知歌手" },
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "?",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "apple-lyrics-top") {
                Spacer(Modifier.height(132.dp))
            }
            itemsIndexed(
                items = lyricItems,
                key = { index, line -> "${line.timeMs}-$index-${line.text}" }
            ) { index, line ->
                val isActive = index == activeIndex || lyricItems.size == 1
                val distance = kotlin.math.abs(index - activeIndex)
                val nextTime = lyricItems.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 3_800L)
                val karaokeProgress = if (isActive && nextTime > line.timeMs) {
                    ((playbackPositionMs - line.timeMs).toFloat() / (nextTime - line.timeMs).toFloat()).coerceIn(0f, 1f)
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
                Spacer(Modifier.height(260.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Slider(
                value = progress,
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(playbackPositionMs), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(54.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(onClick = onToggle, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "播放或暂停",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
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
    val displayText = remember(text, active, progress, accent, activeTextColor, inactiveTextColor, unsungTextColor) {
        if (!active) {
            buildAnnotatedString {
                withStyle(SpanStyle(color = inactiveTextColor)) {
                    append(text)
                }
            }
        } else {
            val splitIndex = (text.length * progress.coerceIn(0f, 1f)).toInt().coerceIn(0, text.length)
            buildAnnotatedString {
                if (splitIndex > 0) {
                    withStyle(SpanStyle(color = activeTextColor)) {
                        append(text.substring(0, splitIndex))
                    }
                }
                if (splitIndex < text.length) {
                    withStyle(SpanStyle(color = unsungTextColor)) {
                        append(text.substring(splitIndex))
                    }
                }
            }
        }
    }
    Text(
        text = displayText,
        style = baseStyle,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = if (active) activeMaxLines else inactiveMaxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth()
    )
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

@Composable
private fun CinematicBars() {
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.74f), Color.Transparent)
                    )
                )
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))
                    )
                )
        )
    }
}

private fun formatTime(valueMs: Long): String {
    val totalSeconds = (valueMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}


