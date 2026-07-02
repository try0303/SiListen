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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
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
import com.silisten.app.data.model.BuiltInCommentPlatforms
import com.silisten.app.data.model.BuiltInSearchPlatforms
import com.silisten.app.data.model.CustomPlaybackSourceType
import com.silisten.app.data.model.CustomSourceConfig
import com.silisten.app.data.model.PlaybackQuality
import com.silisten.app.data.model.PlaylistComment
import com.silisten.app.data.model.PlaylistCommentSort
import com.silisten.app.data.model.PlaylistRoute
import com.silisten.app.data.model.SourceSettingsState
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
fun SettingsScreen(
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
            onLyricDisplayModeChange = viewModel::selectLyricDisplayMode,
            onStatusBarLyricChange = viewModel::setStatusBarLyricEnabled
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
    onLyricDisplayModeChange: (LyricDisplayMode) -> Unit,
    onStatusBarLyricChange: (Boolean) -> Unit
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
                ThemeToggleRow(
                    title = "状态栏歌词",
                    subtitle = "开启后，音乐通知副标题会显示当前歌词；关闭后显示歌手名。",
                    mark = "词",
                    checked = playbackSettings.statusBarLyricEnabled,
                    onCheckedChange = onStatusBarLyricChange,
                    containerColor = panelColor,
                    textColor = titleColor,
                    mutedColor = mutedColor
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
    val settings = uiState.sourceSettings
    val dark = uiState.themeSettings.resolveDarkTheme()
    val pageColor = if (dark) Color(0xFF050805) else Color(0xFFF6F6F8)
    val panelColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    var editingSource by remember { mutableStateOf<CustomSourceConfig?>(null) }
    var showSourceManager by remember { mutableStateOf(false) }
    var showOnlineImport by remember { mutableStateOf(false) }

    editingSource?.let { source ->
        CustomSourceEditorSheet(
            initial = source,
            dark = dark,
            onDismiss = { editingSource = null },
            onSave = {
                viewModel.saveCustomSource(it)
                editingSource = null
            },
            onTest = viewModel::testCustomSource
        )
    }
    if (showSourceManager) {
        CustomSourceManagerDialog(
            settings = settings,
            dark = dark,
            titleColor = titleColor,
            mutedColor = mutedColor,
            onDismiss = { showSourceManager = false },
            onLocalImport = viewModel::importCustomSourceFromScript,
            onOnlineImport = { showOnlineImport = true },
            onEdit = { editingSource = it },
            onDelete = viewModel::deleteCustomSource,
            onEnabledChange = viewModel::setCustomSourceEnabled,
            onAllowUpdateAlertChange = viewModel::setCustomSourceAllowUpdateAlert
        )
    }
    if (showOnlineImport) {
        OnlineSourceImportDialog(
            dark = dark,
            titleColor = titleColor,
            mutedColor = mutedColor,
            onDismiss = { showOnlineImport = false },
            onImport = { url ->
                viewModel.importCustomSourceFromUrl(url)
                showOnlineImport = false
            }
        )
    }

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
                text = "自定义音源",
                color = titleColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "网易云继续负责评论、红心和歌单；播放地址优先交给你添加的脚本或接口解析。",
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            SourceSettingsCard(
                title = "自定义源优先",
                subtitle = "启用后播放会先请求下方自定义音源，全部失败才回退网易云直链",
                iconText = "换",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                SettingSwitchRow(
                    title = "优先使用自定义音源播放",
                    subtitle = "只替换播放 URL，歌曲身份仍保留网易云，评论、红心和歌单不受影响",
                    checked = settings.autoSourceFallbackEnabled,
                    titleColor = titleColor,
                    mutedColor = mutedColor,
                    onCheckedChange = viewModel::setAutoSourceFallbackEnabled
                )
            }
        }

        item {
            SourceSettingsCard(
                title = "平台搜索模块",
                subtitle = "和 LX 一样，单曲搜索可以由多个平台模块共同参与；自定义音源只负责解析播放地址。",
                iconText = "搜",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BuiltInSearchPlatforms.forEach { platform ->
                        SettingSwitchRow(
                            title = "${platform.label}（${platform.lxId}）",
                            subtitle = platform.description,
                            checked = platform.id in settings.enabledSearchPlatformIds,
                            titleColor = titleColor,
                            mutedColor = mutedColor,
                            onCheckedChange = { enabled ->
                                viewModel.setSearchPlatformEnabled(platform.id, enabled)
                            }
                        )
                    }
                }
            }
        }

        item {
            SourceSettingsCard(
                title = "平台评论模块",
                subtitle = "播放页评论会优先按歌曲自身平台读取，不再把所有歌曲都硬套到网易云评论。",
                iconText = "评",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BuiltInCommentPlatforms.forEach { platform ->
                        SettingSwitchRow(
                            title = "${platform.label}（${platform.lxId}）",
                            subtitle = if (platform.id == "netease") {
                                "网易云歌曲、红心、歌单收藏和账号歌单仍使用网易云身份。"
                            } else {
                                "${platform.label}歌曲会读取对应平台评论；红心和加入网易云歌单不会被强制绑定。"
                            },
                            checked = platform.id in settings.enabledCommentPlatformIds,
                            titleColor = titleColor,
                            mutedColor = mutedColor,
                            onCheckedChange = { enabled ->
                                viewModel.setCommentPlatformEnabled(platform.id, enabled)
                            }
                        )
                    }
                }
            }
        }

        item {
            SourceSettingsCard(
                title = "自定义源管理（实验性）",
                subtitle = "像 LX Music 一样导入脚本源，并显示版本、作者和更新提醒设置",
                iconText = "API",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.customSources.isEmpty()) {
                        Text(
                            "还没有自定义源。导入 LX 脚本后会优先参与播放地址解析。",
                            color = mutedColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            settings.customSources.joinToString("、") { source ->
                                source.displaySummaryName()
                            },
                            color = titleColor,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showSourceManager = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("自定义源管理")
                        }
                        Button(
                            onClick = {
                                editingSource = CustomSourceConfig(
                                    id = "",
                                    name = "直接播放接口",
                                    endpoint = "",
                                    enabled = true,
                                    type = CustomPlaybackSourceType.DirectHttp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE9EAEE)
                            )
                        ) {
                            Text("高级编辑", color = titleColor)
                        }
                    }
                }
            }
        }

        item {
            SourceSettingsCard(
                title = "网易云账号能力",
                subtitle = "评论、红心和加入歌单都继续走网易云账号体系",
                iconText = "易",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeteaseAbilityRow("评论", "优先使用网易云 ID 展示歌曲评论，换源后仍保留评论入口", titleColor, mutedColor)
                    NeteaseAbilityRow("喜欢与歌单", "红心、加入歌单、歌单收藏继续同步网易云", titleColor, mutedColor)
                    NeteaseAbilityRow("身份桥接", "网易云 VIP 或灰色歌曲换源播放时，仍使用原网易云歌曲身份", titleColor, mutedColor)
                }
            }
        }
    }
}

@Composable
private fun SourceSettingsCard(
    title: String,
    subtitle: String,
    iconText: String,
    panelColor: Color,
    titleColor: Color,
    mutedColor: Color,
    dark: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        color = panelColor,
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFECECEF)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.28f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        iconText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = titleColor, fontWeight = FontWeight.Black)
                    Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    titleColor: Color,
    mutedColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold)
            Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomSourceManagerDialog(
    settings: SourceSettingsState,
    dark: Boolean,
    titleColor: Color,
    mutedColor: Color,
    onDismiss: () -> Unit,
    onLocalImport: (String, String) -> Unit,
    onOnlineImport: () -> Unit,
    onEdit: (CustomSourceConfig) -> Unit,
    onDelete: (String) -> Unit,
    onEnabledChange: (String, Boolean) -> Unit,
    onAllowUpdateAlertChange: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var importMenuExpanded by remember { mutableStateOf(false) }
    val localImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = uri.lastPathSegment.orEmpty().substringAfterLast('/')
        val script = runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
        }.getOrDefault("")
        onLocalImport(fileName, script)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Color(0xFF101510) else Color.White,
        titleContentColor = titleColor,
        textContentColor = titleColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "自定义源管理（实验性）",
                    color = titleColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = mutedColor)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (settings.customSources.isEmpty()) {
                    Text(
                        "这里还没有自定义源。导入 LX 音源脚本后，会显示名称、版本、作者和可用播放源。",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(settings.customSources, key = { it.id }) { source ->
                            CustomSourceManagerRow(
                                source = source,
                                dark = dark,
                                titleColor = titleColor,
                                mutedColor = mutedColor,
                                onEdit = { onEdit(source) },
                                onDelete = { onDelete(source.id) },
                                onEnabledChange = { enabled -> onEnabledChange(source.id, enabled) },
                                onAllowUpdateAlertChange = { enabled ->
                                    onAllowUpdateAlertChange(source.id, enabled)
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("源编写说明：", color = titleColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "LX 自定义源协议",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "提示：脚本运行环境已尽量隔离，但导入来源不明的脚本仍可能带来风险，请只导入你信任的源。",
                    color = mutedColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) Color.White.copy(alpha = 0.10f) else Color(0xFFEAF4EF)
                )
            ) {
                Text("关闭", color = if (dark) Color(0xFFE8FFF0) else Color(0xFF23875B))
            }
        },
        confirmButton = {
            Box {
                Button(onClick = { importMenuExpanded = true }) {
                    Text("导入")
                }
                DropdownMenu(
                    expanded = importMenuExpanded,
                    onDismissRequest = { importMenuExpanded = false },
                    containerColor = if (dark) Color(0xFF1A211B) else Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text("本地导入", color = titleColor) },
                        leadingIcon = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
                        onClick = {
                            importMenuExpanded = false
                            localImportLauncher.launch(arrayOf("application/javascript", "text/javascript", "text/plain", "*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("在线导入", color = titleColor) },
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                        onClick = {
                            importMenuExpanded = false
                            onOnlineImport()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun CustomSourceManagerRow(
    source: CustomSourceConfig,
    dark: Boolean,
    titleColor: Color,
    mutedColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAllowUpdateAlertChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (dark) Color(0xFF1B261E) else Color(0xFFEAF8F1))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    source.displaySummaryName(),
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                val meta = listOf(source.version.withVersionPrefix(), source.author)
                    .filter { it.isNotBlank() }
                    .joinToString("   ")
                if (meta.isNotBlank()) {
                    Text(meta, color = mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
            Switch(checked = source.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = mutedColor)
            }
        }
        if (source.description.isNotBlank()) {
            Text(source.description, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        if (source.supportedSources.isNotEmpty()) {
            Text(
                "可用播放源：${source.supportedSources.joinToString("、")}",
                color = mutedColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = source.allowShowUpdateAlert,
                onCheckedChange = onAllowUpdateAlertChange
            )
            Text(
                "允许显示更新弹窗",
                color = titleColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEdit) {
                Text("编辑")
            }
        }
    }
}

@Composable
private fun OnlineSourceImportDialog(
    dark: Boolean,
    titleColor: Color,
    mutedColor: Color,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var url by remember {
        mutableStateOf("https://ghproxy.net/raw.githubusercontent.com/pdone/lx-music-source/main/lx/latest.js")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Color(0xFF101510) else Color.White,
        titleContentColor = titleColor,
        textContentColor = titleColor,
        title = { Text("在线导入", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "粘贴 LX 自定义源脚本链接，SiListen 会下载脚本、识别源信息并加入管理列表。",
                    color = mutedColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("脚本链接") },
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            Button(onClick = { onImport(url) }) {
                Text("导入")
            }
        }
    )
}

private fun CustomSourceConfig.displaySummaryName(): String {
    val versionLabel = version.withVersionPrefix()
    return if (versionLabel.isBlank()) name else "$name  $versionLabel"
}

private fun String.withVersionPrefix(): String {
    val clean = trim()
    return when {
        clean.isBlank() -> ""
        clean.firstOrNull()?.isDigit() == true -> "v$clean"
        else -> clean
    }
}

@Composable
private fun CustomSourceRow(
    source: CustomSourceConfig,
    titleColor: Color,
    mutedColor: Color,
    dark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) Color.White.copy(alpha = 0.05f) else Color(0xFFF4F5F7))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(source.name, color = titleColor, fontWeight = FontWeight.Bold)
                Text(source.endpoint, color = mutedColor, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = source.enabled, onCheckedChange = onEnabledChange)
        }
        Text(
            text = source.type.label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                Text("编辑")
            }
            Button(onClick = onDelete, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                Text("删除")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSourceEditorSheet(
    initial: CustomSourceConfig,
    dark: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomSourceConfig) -> Unit,
    onTest: (CustomSourceConfig) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var endpoint by remember(initial.id) { mutableStateOf(initial.endpoint) }
    var enabled by remember(initial.id) { mutableStateOf(initial.enabled) }
    var type by remember(initial.id) { mutableStateOf(initial.type) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedColor = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72)
    val current = initial.copy(
        name = name,
        endpoint = endpoint,
        enabled = enabled,
        type = type
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (dark) Color(0xFF101510) else Color.White,
        contentColor = titleColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial.id.isBlank()) "新增自定义音源" else "编辑自定义音源",
                color = titleColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "LX 脚本按洛雪自定义源协议运行；直接播放接口会收到歌曲信息并返回 URL。",
                color = mutedColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text("音源类型", color = titleColor, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CustomPlaybackSourceType.values().forEach { option ->
                    SourceTypeChip(
                        type = option,
                        selected = option == type,
                        dark = dark,
                        onClick = { type = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("音源名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text(if (type == CustomPlaybackSourceType.LxScript) "LX 脚本地址" else "接口地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            SettingSwitchRow(
                title = "启用音源",
                subtitle = "关闭后暂不参与自动换源",
                checked = enabled,
                titleColor = titleColor,
                mutedColor = mutedColor,
                onCheckedChange = { enabled = it }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onTest(current) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFFE9EAEE))
                ) {
                    Text("测试配置", color = titleColor)
                }
                Button(
                    onClick = { onSave(current) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SourceTypeChip(
    type: CustomPlaybackSourceType,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = when {
            selected -> accent.copy(alpha = if (dark) 0.24f else 0.14f)
            dark -> Color.White.copy(alpha = 0.06f)
            else -> Color(0xFFF2F3F6)
        },
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.75f) else Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.noRippleClick(shape = RoundedCornerShape(18.dp), onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                type.label,
                color = if (selected) accent else if (dark) Color(0xFFF3FFF5) else Color(0xFF111111),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                type.description,
                color = if (dark) Color(0xFFAAC0B0) else Color(0xFF6A6D72),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NeteaseAbilityRow(
    title: String,
    subtitle: String,
    titleColor: Color,
    mutedColor: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold)
            Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
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
