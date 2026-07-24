package com.silisten.app.ui.kernelsu.animation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Press highlight for the floating bottom bar.
 * RuntimeShader path needs API 33+; older devices use a radial-gradient fallback
 * so the app does not crash with NoSuchMethodError on RuntimeShader.<init>.
 */
class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var startPosition = Offset.Zero

    private val runtimeShader: RuntimeShader? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(
                """
                uniform float2 size;
                layout(color) uniform half4 color;
                uniform float radius;
                uniform float2 position;

                half4 main(float2 coord) {
                    float dist = distance(coord, position);
                    float intensity = smoothstep(radius, radius * 0.5, dist);
                    return color * intensity;
                }
                """.trimIndent()
            )
        } else {
            null
        }

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(Color.White.copy(alpha = 0.06f * progress), blendMode = BlendMode.Plus)
            val highlightPosition = position(size, positionAnimation.value)
            val safeX = highlightPosition.x.fastCoerceIn(0f, size.width)
            val safeY = highlightPosition.y.fastCoerceIn(0f, size.height)
            val radius = size.minDimension * 1.2f
            val highlightColor = Color.White.copy(alpha = 0.12f * progress)
            val shader = runtimeShader
            if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color", highlightColor.toArgb())
                shader.setFloatUniform("radius", radius)
                shader.setFloatUniform("position", safeX, safeY)
                drawRect(ShaderBrush(shader), blendMode = BlendMode.Plus)
            } else {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(highlightColor, Color.Transparent),
                        center = Offset(safeX, safeY),
                        radius = radius.coerceAtLeast(1f)
                    ),
                    blendMode = BlendMode.Plus
                )
            }
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}
