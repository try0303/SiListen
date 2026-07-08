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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
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

fun Modifier.noRippleClick(
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
        label = "no-ripple-press-scale"
    )
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.08f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 70 else 150),
        label = "no-ripple-press-overlay"
    )
    graphicsLayer {
        scaleX = pressedScale
        scaleY = pressedScale
    }
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (pressedOverlayAlpha > 0f) {
                drawRect(Color.Black.copy(alpha = pressedOverlayAlpha))
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
fun LiquidGlassPane(
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

@Composable
fun PageTopTitle(title: String, dark: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = if (dark) Color.White else Color(0xFF111111),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

@Composable
fun SectionTitle(title: String, dark: Boolean, modifier: Modifier = Modifier) {
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
fun PageHeroCard(
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
fun SearchFloatingGlyph(dark: Boolean, modifier: Modifier = Modifier) {
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
fun SearchActionButton(
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
fun StatusMessageCard(text: String, dark: Boolean, modifier: Modifier = Modifier) {
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
fun PrimaryActionButton(
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
fun SecondaryActionButton(
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
fun EmptyStateCard(
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
fun LoadingStateCard(
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
fun SourceBadge(text: String, color: Color) {
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
fun LyricStageParticles() {
    Box(modifier = Modifier.fillMaxSize())
}

fun liquidGlassBrush(
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

fun liquidGlassHighlightBrush(dark: Boolean): Brush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = if (dark) 0.22f else 0.72f),
        Color.White.copy(alpha = if (dark) 0.08f else 0.18f),
        Color.Transparent
    )
)

@Composable
fun FeaturePlaylistCard(
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
fun PlaylistTile(playlist: MusicPlaylist, onClick: () -> Unit) {
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
fun SongRow(
    song: Song,
    liked: Boolean,
    likeLoading: Boolean,
    onClick: () -> Unit,
    onLikeClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
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
        if (onMoreClick != null) {
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
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheetModal(
    song: Song,
    dark: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowComments: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (dark) Color(0xFF111111) else Color(0xFFF7F7F8),
        scrimColor = Color.Black.copy(alpha = 0.36f),
        dragHandle = null
    ) {
        SongActionSheet(
            song = song,
            dark = dark,
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onShowComments = onShowComments
        )
    }
}

@Composable
fun SongActionSheet(
    song: Song,
    dark: Boolean,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowComments: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val mutedText = if (dark) Color.White.copy(alpha = 0.56f) else Color(0xFF676A70)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SongActionRow(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = "下一首播放",
            dark = dark,
            onClick = onPlayNext
        )
        SongActionRow(
            icon = Icons.Rounded.LibraryMusic,
            title = "加入歌单",
            dark = dark,
            onClick = onAddToPlaylist
        )
        SongActionRow(
            icon = Icons.AutoMirrored.Rounded.Comment,
            title = "查看歌曲评论",
            dark = dark,
            onClick = onShowComments
        )
    }
}

@Composable
private fun SongActionRow(
    icon: ImageVector,
    title: String,
    dark: Boolean,
    onClick: () -> Unit
) {
    val titleColor = if (dark) Color(0xFFF3FFF5) else Color(0xFF111111)
    val iconBackground = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.82f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.76f))
            .noRippleClick(shape = RoundedCornerShape(22.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SourceRow(
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
fun MarqueeText(
    text: String,
    color: Color,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight?,
    modifier: Modifier = Modifier
) {
    var needsScroll by remember { mutableStateOf(false) }
    var textWidth by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val measuredStyle = remember(style, fontWeight) {
        if (fontWeight == null) style else style.merge(TextStyle(fontWeight = fontWeight))
    }
    val scrollOffset = remember(text) { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(text) {
        scrollOffset.snapTo(0f)
    }

    LaunchedEffect(text, measuredStyle, containerWidth) {
        val measured = textMeasurer.measure(
            text = AnnotatedString(text),
            style = measuredStyle,
            maxLines = 1,
            softWrap = false
        ).size.width.toFloat()
        textWidth = measured
        needsScroll = containerWidth > 0f && measured > containerWidth + 1f
    }

    val marqueeGap = 32.dp
    val marqueeGapPx = with(density) { marqueeGap.toPx() }

    LaunchedEffect(needsScroll, textWidth, containerWidth, marqueeGapPx) {
        if (!needsScroll || textWidth <= containerWidth) return@LaunchedEffect
        val distance = textWidth + marqueeGapPx
        while (true) {
            scrollOffset.snapTo(0f)
            delay(1200)
            scrollOffset.animateTo(
                targetValue = -distance,
                animationSpec = tween(
                    durationMillis = (distance * 18).toInt().coerceIn(2600, 12000),
                    easing = LinearEasing
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width.toFloat() }
    ) {
        val textModifier = with(density) {
            if (textWidth > containerWidth && textWidth > 0f) {
                Modifier.requiredWidth(textWidth.toDp())
            } else {
                Modifier.fillMaxWidth()
            }
        }
        if (needsScroll) {
            Text(
                text = text,
                color = color,
                style = measuredStyle,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = textModifier.graphicsLayer {
                    translationX = scrollOffset.value
                }
            )
            Text(
                text = text,
                color = color,
                style = measuredStyle,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = textModifier.graphicsLayer {
                    translationX = scrollOffset.value + textWidth + marqueeGapPx
                }
            )
        } else {
            Text(
                text = text,
                color = color,
                style = measuredStyle,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun formatTime(valueMs: Long): String {
    val totalSeconds = (valueMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
