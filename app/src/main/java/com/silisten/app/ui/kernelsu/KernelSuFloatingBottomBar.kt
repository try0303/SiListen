package com.silisten.app.ui.kernelsu

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.silisten.app.ui.kernelsu.animation.DampedDragAnimation
import com.silisten.app.ui.kernelsu.animation.InteractiveHighlight
import com.silisten.app.ui.kernelsu.liquid.InnerShadow
import com.silisten.app.ui.kernelsu.liquid.innerShadow
import com.silisten.app.ui.kernelsu.liquid.lens
import com.silisten.app.ui.kernelsu.liquid.rememberCombinedBackdrop
import com.silisten.app.ui.kernelsu.liquid.vibrancy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val LocalKernelSuFloatingBottomBarTabScale = staticCompositionLocalOf { { 1f } }
val LocalKernelSuFloatingBottomBarContentTint = staticCompositionLocalOf<Color?> { null }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f
        ),
        dualPeak = true
    )
)

private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f

@Composable
private fun rememberGravityRotatedHighlight(
    base: Highlight,
    extraDegrees: Float = 0f
): Highlight {
    val baseStyle = base.style as BloomStroke
    val tilt by rememberDeviceTilt()
    val rotatedPrimary = remember(tilt, baseStyle.primaryLight, extraDegrees) {
        val basePrimary = baseStyle.primaryLight
        val gx = tilt.gravityX
        val gy = tilt.gravityY
        val gMagSq = gx * gx + gy * gy
        val (lx0, ly0) = if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
            val invMag = 1f / sqrt(gMagSq)
            (gx * invMag) to (gy * invMag)
        } else {
            0f to -1f
        }
        val rad = extraDegrees * PI / 180.0
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val lx = c * lx0 - s * ly0
        val ly = s * lx0 + c * ly0
        basePrimary.copy(
            position = LightPosition(
                x = LIGHT_REF_X + lx,
                y = LIGHT_REF_Y + ly,
                z = basePrimary.position.z
            )
        )
    }
    return remember(base, rotatedPrimary) {
        base.copy(style = baseStyle.copy(primaryLight = rotatedPrimary))
    }
}

@Composable
fun RowScope.KernelSuFloatingBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalKernelSuFloatingBottomBarTabScale.current
    Column(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val targetScale = scale()
                scaleX = targetScale
                scaleY = targetScale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun KernelSuFloatingBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    selectedPosition: Float? = null,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    darkTheme: Boolean,
    accentColor: Color,
    containerColor: Color,
    isBlurEnabled: Boolean = true,
    useLiquidGlassFallback: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val pillShape = remember { CircleShape }
    val surfaceColor = when {
        isBlurEnabled -> containerColor.copy(alpha = 0.4f)
        useLiquidGlassFallback -> containerColor.copy(alpha = if (darkTheme) 0.54f else 0.68f)
        else -> containerColor
    }
    val glassBaseBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (darkTheme) 0.20f else 0.74f),
            surfaceColor,
            Color.Black.copy(alpha = if (darkTheme) 0.16f else 0.04f)
        )
    )
    val glassIndicatorBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (darkTheme) 0.24f else 0.78f),
            accentColor.copy(alpha = if (darkTheme) 0.20f else 0.16f),
            Color.Black.copy(alpha = if (darkTheme) 0.12f else 0.03f)
        )
    )
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    var isDockDragging by remember { mutableStateOf(false) }
    var externalFollowLockedToIndex by remember { mutableStateOf<Int?>(null) }

    val panelOffset = 0f

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }
    val holder = remember { DampedDragAnimationHolder() }
    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).coerceAtLeast(0).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f || totalWidthPx == 0f) return@DampedDragAnimation false
                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx
                val padding = with(density) { 4.dp.toPx() }
                val globalTouchX = if (isLtr) {
                    padding + indicatorX + offset.x
                } else {
                    totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {
                isDockDragging = true
                externalFollowLockedToIndex = null
            },
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                isDockDragging = false
                externalFollowLockedToIndex = targetIndex
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                }
            }
        ).also { holder.instance = it }
    }

    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
        if (selectedPosition == null) {
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }
    LaunchedEffect(selectedPosition) {
        selectedPosition?.let { position ->
            val lockedIndex = externalFollowLockedToIndex
            if (isDockDragging) return@LaunchedEffect
            if (lockedIndex != null && abs(position - lockedIndex) > 0.03f) {
                return@LaunchedEffect
            }
            externalFollowLockedToIndex = null
            dampedDragAnimation.updateValue(position.fastCoerceIn(0f, (tabsCount - 1).toFloat()))
        }
    }
    LaunchedEffect(dampedDragAnimation, tabsCount) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            dampedDragAnimation.animateToValue(index.toFloat())
            onSelected(index)
        }
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx, isLtr) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f
                )
            }
        )
    }
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val baseHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = 90f)

    Box(
        modifier = modifier.width(304.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .graphicsLayer { translationX = panelOffset }
                .shadow(
                    elevation = 10.dp,
                    shape = pillShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (darkTheme) 0.2f else 0.1f),
                    spotColor = Color.Black.copy(alpha = if (darkTheme) 0.2f else 0.1f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .then(
                    if (isBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx()
                                )
                            },
                            highlight = { baseHighlight.copy(alpha = 0.75f) },
                            layerBlock = {
                                val width = size.width.coerceAtLeast(1f)
                                val scale = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(surfaceColor) }
                        )
                    } else {
                        Modifier
                            .clip(pillShape)
                            .background(if (useLiquidGlassFallback) glassBaseBrush else Brush.linearGradient(listOf(surfaceColor, surfaceColor)), pillShape)
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = if (useLiquidGlassFallback) 9.dp else 0.dp,
                                    color = Color.White.copy(alpha = if (darkTheme) 0.10f else 0.34f),
                                    alpha = if (useLiquidGlassFallback) 1f else 0f
                                )
                            }
                    }
                )
                .then(if (isBlurEnabled) interactiveHighlight.modifier else Modifier)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        if (isBlurEnabled) {
            CompositionLocalProvider(
                LocalKernelSuFloatingBottomBarTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                },
                LocalKernelSuFloatingBottomBarContentTint provides accentColor
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx()
                                )
                            },
                            onDrawSurface = { drawRect(surfaceColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            if (isBlurEnabled) {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .then(interactiveHighlight.gestureModifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { pillShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    refractionHeight = 10.dp.toPx() * progress,
                                    refractionAmount = 14.dp.toPx() * progress,
                                    depthEffect = true,
                                    chromaticAberration = 0.5f
                                )
                            },
                            highlight = { pillHighlight.copy(alpha = dampedDragAnimation.pressProgress) },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = if (!darkTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f),
                                    alpha = 1f - progress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress
                            )
                        }
                        .height(56.dp)
                        .width(tabWidthDp)
                )
            } else {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            if (useLiquidGlassFallback) {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                            }
                        }
                        .then(if (useLiquidGlassFallback) interactiveHighlight.gestureModifier else Modifier)
                        .then(dampedDragAnimation.modifier)
                        .shadow(
                            elevation = if (useLiquidGlassFallback) 8.dp else 0.dp,
                            shape = pillShape,
                            clip = false,
                            ambientColor = accentColor.copy(alpha = if (darkTheme) 0.18f else 0.12f),
                            spotColor = accentColor.copy(alpha = if (darkTheme) 0.18f else 0.12f)
                        )
                        .background(
                            if (useLiquidGlassFallback) glassIndicatorBrush else Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.15f),
                                    accentColor.copy(alpha = 0.15f)
                                )
                            ),
                            pillShape
                        )
                        .clip(pillShape)
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = if (darkTheme) 0.18f else 0.10f),
                                alpha = if (useLiquidGlassFallback) 0.75f else 0f
                            )
                        }
                        .height(56.dp)
                        .width(tabWidthDp)
                )
            }
        }
    }
}
