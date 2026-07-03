package com.silisten.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.silisten.app.R
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
            onStatusBarLyricChange = viewModel::setStatusBarLyricEnabled,
            onDesktopLyricChange = viewModel::setDesktopLyricEnabled,
            onStatusBarLyricOffsetChange = viewModel::setStatusBarLyricOffsetDp,
            onStatusBarLyricHorizontalChange = viewModel::setStatusBarLyricHorizontalPercent,
            onStatusBarLyricWidthChange = viewModel::setStatusBarLyricWidthPercent,
            onStatusBarLyricColorChange = viewModel::setStatusBarLyricColorArgb
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
                subtitle = "管理搜索模块、自定义源和播放解析策略",
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
                subtitle = "如果 SiListen 对你有帮助，可以在这里支持后续维护",
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
                subtitle = "账号曲库与自定义接口协同工作的音乐客户端",
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
                            selectedLabel = state.paletteStyle.label,
                            mark = "S",
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            dark = dark,
                            options = ThemePaletteStyleOption.entries.toList(),
                            selected = state.paletteStyle,
                            optionLabel = { it.label },
                            onSelect = onPaletteStyleChange
                        )
                        ThemeDivider(dividerColor)
                        KernelSuDropdownPreference(
                            title = "色彩规范",
                            selectedLabel = state.colorSpec.label,
                            mark = "25",
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            dark = dark,
                            options = ThemeColorSpecOption.entries.toList(),
                            selected = state.colorSpec,
                            optionLabel = { it.label },
                            onSelect = onColorSpecChange
                        )
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
    val context = LocalContext.current
    val rewardBitmap: Bitmap? = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.mm_reward_qrcode)
    }
    val rewardImage = remember(rewardBitmap) { rewardBitmap?.asImageBitmap() }
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
                text = "你的支持会用于 SiListen 的持续维护、功能优化和后续体验打磨。",
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
                            .size(246.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rewardImage != null && rewardBitmap != null) {
                            Image(
                                bitmap = rewardImage,
                                contentDescription = "微信赞赏二维码",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .pointerInput(rewardBitmap) {
                                        detectTapGestures(
                                            onLongPress = {
                                                val saved = saveDonationBitmapToPictures(context, rewardBitmap)
                                                Toast.makeText(
                                                    context,
                                                    if (saved) "赞赏码已保存到图片/相册" else "赞赏码保存失败，请稍后再试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("赞赏码加载失败", color = Color(0xFF111111), fontWeight = FontWeight.Black)
                                Text("请稍后再试", color = Color(0xFF6A6D72), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text(
                        text = "长按二维码可保存到相册，也可以使用微信扫码赞赏。感谢每一份鼓励与支持。",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            ThemeSettingsGroup(containerColor = panelColor) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("说明", color = titleColor, fontWeight = FontWeight.Bold)
                    Text(
                        "赞赏完全自愿，不影响应用的任何功能使用。你的反馈和使用本身同样是对项目很重要的支持。",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

    }
}

private fun saveDonationBitmapToPictures(context: Context, bitmap: Bitmap): Boolean {
    val filename = "SiListen_Reward_${
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
private fun PlaybackSettingsScreen(
    playbackSettings: PlaybackSettingsState,
    themeSettings: ThemeSettingsState,
    onBack: () -> Unit,
    onQualityChange: (PlaybackQuality) -> Unit,
    onLyricDisplayModeChange: (LyricDisplayMode) -> Unit,
    onStatusBarLyricChange: (Boolean) -> Unit,
    onDesktopLyricChange: (Boolean) -> Unit,
    onStatusBarLyricOffsetChange: (Int) -> Unit,
    onStatusBarLyricHorizontalChange: (Float) -> Unit,
    onStatusBarLyricWidthChange: (Float) -> Unit,
    onStatusBarLyricColorChange: (Long) -> Unit
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
                    subtitle = "在屏幕顶部显示一条透明歌词，需要悬浮窗权限。",
                    mark = "顶",
                    checked = playbackSettings.statusBarLyricEnabled,
                    onCheckedChange = onStatusBarLyricChange,
                    containerColor = panelColor,
                    textColor = titleColor,
                    mutedColor = mutedColor
                )
                AnimatedVisibility(visible = playbackSettings.statusBarLyricEnabled) {
                    Column {
                        ThemeDivider(dividerColor)
                        StatusLyricSliderItem(
                            title = "上下位置调节",
                            subtitle = "距离屏幕顶部 ${playbackSettings.statusBarLyricOffsetDp}dp",
                            value = playbackSettings.statusBarLyricOffsetDp.toFloat(),
                            valueRange = 0f..120f,
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            onValueChange = { onStatusBarLyricOffsetChange(it.toInt()) }
                        )
                        ThemeDivider(dividerColor)
                        StatusLyricSliderItem(
                            title = "左右位置调节",
                            subtitle = statusLyricHorizontalLabel(playbackSettings.statusBarLyricHorizontalPercent),
                            value = playbackSettings.statusBarLyricHorizontalPercent,
                            valueRange = -1f..1f,
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            onValueChange = onStatusBarLyricHorizontalChange
                        )
                        ThemeDivider(dividerColor)
                        StatusLyricSliderItem(
                            title = "宽度调节",
                            subtitle = "${(playbackSettings.statusBarLyricWidthPercent * 100).toInt()}% 屏幕宽度，拖动时顶部会显示胶囊边框预览",
                            value = playbackSettings.statusBarLyricWidthPercent,
                            valueRange = 0.35f..1f,
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            onValueChange = onStatusBarLyricWidthChange
                        )
                        ThemeDivider(dividerColor)
                        StatusLyricColorItem(
                            selectedColor = playbackSettings.statusBarLyricColorArgb,
                            textColor = titleColor,
                            mutedColor = mutedColor,
                            onColorChange = onStatusBarLyricColorChange
                        )
                    }
                }
                ThemeDivider(dividerColor)
                ThemeToggleRow(
                    title = "桌面歌词",
                    subtitle = "在桌面和其他应用上方显示可拖动歌词胶囊，需要悬浮窗权限。",
                    mark = "桌",
                    checked = playbackSettings.desktopLyricEnabled,
                    onCheckedChange = onDesktopLyricChange,
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
        }

        item {
            SourceSettingsCard(
                title = "自定义源优先",
                subtitle = "启用后播放会优先请求已启用的自定义源，全部失败后再回退默认解析",
                iconText = "换",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                SettingSwitchRow(
                    title = "优先使用自定义源播放",
                    subtitle = "只替换播放地址，歌曲身份、评论和收藏能力保持不变",
                    checked = settings.autoSourceFallbackEnabled,
                    titleColor = titleColor,
                    mutedColor = mutedColor,
                    onCheckedChange = viewModel::setAutoSourceFallbackEnabled
                )
            }
        }

        item {
            SourceSettingsCard(
                title = "曲库搜索模块",
                subtitle = "可按需开启多个内置搜索模块；自定义源只负责解析播放地址。",
                iconText = "搜",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BuiltInSearchPlatforms.forEach { platform ->
                        SettingSwitchRow(
                            title = platform.label,
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
                title = "评论模块",
                subtitle = "播放页评论会按歌曲身份读取对应模块。",
                iconText = "评",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BuiltInCommentPlatforms.forEach { platform ->
                        SettingSwitchRow(
                            title = platform.label,
                            subtitle = "开启后，具备对应身份的歌曲可读取该模块评论。",
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
                title = "自定义源管理",
                subtitle = "导入兼容 LX 协议的脚本源，管理版本、作者和更新提醒",
                iconText = "源",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.customSources.isEmpty()) {
                        Text(
                            "暂无自定义源。导入兼容 LX 协议的脚本后，可用于播放地址解析。",
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
                    Button(
                        onClick = { showSourceManager = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("管理自定义源")
                    }
                }
            }
        }

        item {
            SourceSettingsCard(
                title = "账号能力",
                subtitle = "评论、红心和加入歌单继续走账号体系",
                iconText = "账",
                panelColor = panelColor,
                titleColor = titleColor,
                mutedColor = mutedColor,
                dark = dark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeteaseAbilityRow("评论", "优先使用账号歌曲身份展示评论，换源后仍保留评论入口", titleColor, mutedColor)
                    NeteaseAbilityRow("喜欢与歌单", "红心、加入歌单和歌单收藏继续同步到账号", titleColor, mutedColor)
                    NeteaseAbilityRow("身份桥接", "需要换源播放时，仍尽量保留原歌曲身份", titleColor, mutedColor)
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
                    "自定义源管理",
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
                        "暂无自定义源。导入后会显示名称、版本、作者和可用模块。",
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
                        "LX自定义源协议",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.noRippleClick(RoundedCornerShape(6.dp)) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://lxmusic.toside.cn/mobile/custom-source")
                                )
                            )
                        }
                    )
                }
                Text(
                    "请仅导入可信来源的脚本。自定义源仅用于解析播放地址，请遵守所在地相关法律法规。",
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
                    "粘贴兼容 LX 协议的自定义源脚本链接，应用会识别源信息并加入管理列表。",
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
            "平滑式保留现在的 Apple Music 风格滚动，逐字式强调一个字一个字高亮，普通模式只显示干净歌词。",
            color = mutedColor,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LyricModeChip(
                label = "平滑式",
                subtitle = "顺滑滚动",
                selected = selectedMode == LyricDisplayMode.Glass,
                dark = dark,
                onClick = { onModeChange(LyricDisplayMode.Glass) },
                modifier = Modifier.weight(1f)
            )
            LyricModeChip(
                label = "逐字式",
                subtitle = "逐字高亮",
                selected = selectedMode == LyricDisplayMode.Word || selectedMode == LyricDisplayMode.Particles,
                dark = dark,
                onClick = { onModeChange(LyricDisplayMode.Word) },
                modifier = Modifier.weight(1f)
            )
            LyricModeChip(
                label = "普通",
                subtitle = "纯净歌词",
                selected = selectedMode == LyricDisplayMode.Plain,
                dark = dark,
                onClick = { onModeChange(LyricDisplayMode.Plain) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun statusLyricHorizontalLabel(value: Float): String {
    val percent = kotlin.math.abs(value * 100).toInt()
    return when {
        value < -0.04f -> "向左偏移 $percent%"
        value > 0.04f -> "向右偏移 $percent%"
        else -> "居中显示"
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
private fun StatusLyricSliderItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    textColor: Color,
    mutedColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(title, color = textColor, fontWeight = FontWeight.Bold)
            Text(subtitle, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = mutedColor.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun StatusLyricColorItem(
    selectedColor: Long,
    textColor: Color,
    mutedColor: Color,
    onColorChange: (Long) -> Unit
) {
    val options = listOf(
        0xFFFFFFFFL to "白",
        0xFF50C8FFL to "蓝",
        0xFF7CFFB2L to "绿",
        0xFFFF7FA4L to "粉",
        0xFFFFD166L to "黄"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("状态栏歌词颜色", color = textColor, fontWeight = FontWeight.Bold)
            Text("选择顶部歌词的文字颜色。", color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { (argb, label) ->
                val selected = selectedColor == argb
                val color = Color(argb)
                Surface(
                    color = if (selected) color.copy(alpha = 0.20f) else mutedColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (selected) color else Color.Transparent),
                    modifier = Modifier.noRippleClick(RoundedCornerShape(999.dp)) {
                        onColorChange(argb)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            label,
                            color = if (selected) color else textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
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
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ThemeDropdownAnchor(
            title = "关键色",
            selectedLabel = state.accent.label,
            mark = "●",
            textColor = titleColor,
            mutedColor = mutedColor,
            dark = dark,
            leading = {
                Box(Modifier.size(15.dp).clip(CircleShape).background(state.accent.color))
            },
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (dark) Color(0xFF1A211B) else Color.White
        ) {
            ThemeAccentOption.entries.forEach { accent ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(accent.color))
                            Text(accent.label, color = titleColor, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    trailingIcon = {
                        if (accent == state.accent) {
                            Text("已选", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        expanded = false
                        onAccentChange(accent)
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> KernelSuDropdownPreference(
    title: String,
    selectedLabel: String,
    mark: String,
    textColor: Color,
    mutedColor: Color,
    dark: Boolean,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ThemeDropdownAnchor(
            title = title,
            selectedLabel = selectedLabel,
            mark = mark,
            textColor = textColor,
            mutedColor = mutedColor,
            dark = dark,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (dark) Color(0xFF1A211B) else Color.White
        ) {
            options.forEach { option ->
                val active = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(option),
                            color = textColor,
                            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold
                        )
                    },
                    trailingIcon = {
                        if (active) {
                            Text("已选", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeDropdownAnchor(
    title: String,
    selectedLabel: String,
    mark: String,
    textColor: Color,
    mutedColor: Color,
    dark: Boolean,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.05f) else Color(0xFFF4F5F7),
        border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFE7E8EC)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClick(RoundedCornerShape(18.dp), onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeRowMark(mark)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = textColor, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    leading?.invoke()
                    Text(selectedLabel, color = mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = mutedColor)
        }
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
