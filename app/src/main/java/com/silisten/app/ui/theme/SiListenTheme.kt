package com.silisten.app.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.silisten.app.ThemeAccentOption
import com.silisten.app.ThemeColorSpecOption
import com.silisten.app.ThemeModeOption
import com.silisten.app.ThemePaletteStyleOption
import com.silisten.app.ThemeSettingsState
import com.silisten.app.ThemeUiModeOption

data class SiListenAppearance(
    val uiMode: ThemeUiModeOption,
    val dark: Boolean,
    val blurEnabled: Boolean,
    val floatingBottomBarEnabled: Boolean,
    val floatingBottomBarBlurEnabled: Boolean,
    val predictiveBackEnabled: Boolean,
    val accent: Color
)

private data class SiListenPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color
)

val LocalSiListenAppearance = staticCompositionLocalOf {
    SiListenAppearance(
        uiMode = ThemeUiModeOption.Miuix,
        dark = false,
        blurEnabled = true,
        floatingBottomBarEnabled = true,
        floatingBottomBarBlurEnabled = true,
        predictiveBackEnabled = false,
        accent = Color(0xFF1ED760)
    )
}

@Composable
fun SiListenTheme(
    themeSettings: ThemeSettingsState = ThemeSettingsState(),
    content: @Composable () -> Unit
) {
    val darkTheme = themeSettings.resolveDarkTheme()
    val palette = remember(themeSettings, darkTheme) {
        themeSettings.toPalette(darkTheme)
    }
    val appearance = remember(themeSettings, darkTheme, palette.primary) {
        themeSettings.toAppearance(darkTheme, palette.primary)
    }
    val currentDensity = LocalDensity.current
    val scaledDensity = remember(currentDensity, themeSettings.uiScale) {
        Density(
            density = currentDensity.density * themeSettings.uiScale,
            fontScale = currentDensity.fontScale
        )
    }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceContainer = palette.surfaceContainer,
            onPrimary = onColorFor(palette.primary),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            surface = palette.surface,
            surfaceContainer = palette.surfaceContainer,
            onPrimary = onColorFor(palette.primary),
            onBackground = Color(0xFF111111),
            onSurface = Color(0xFF111111)
        )
    }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalSiListenAppearance provides appearance
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

fun ThemeSettingsState.toAppearance(darkTheme: Boolean, accent: Color = accentColor()): SiListenAppearance =
    SiListenAppearance(
        uiMode = uiMode,
        dark = darkTheme,
        blurEnabled = blurEnabled,
        floatingBottomBarEnabled = floatingBottomBarEnabled,
        floatingBottomBarBlurEnabled = floatingBottomBarBlurEnabled,
        predictiveBackEnabled = predictiveBackEnabled,
        accent = accent
    )

@Composable
fun ThemeSettingsState.resolveDarkTheme(): Boolean = when (mode) {
    ThemeModeOption.System -> isSystemInDarkTheme()
    ThemeModeOption.Light -> false
    ThemeModeOption.Dark -> true
}

fun ThemeSettingsState.accentColor(): Color {
    val keyColor = when (accent) {
        ThemeAccentOption.Emerald -> Color(0xFF1ED760)
        ThemeAccentOption.Rose -> Color(0xFFFF5C7C)
        ThemeAccentOption.Sky -> Color(0xFF4C9FFF)
        ThemeAccentOption.Amber -> Color(0xFFFFB020)
        ThemeAccentOption.Violet -> Color(0xFF8F6BFF)
    }
    return keyColor
}

fun ThemeSettingsState.onAccentColor(): Color {
    return onColorFor(accentColor())
}

fun appBackgroundBrush(themeSettings: ThemeSettingsState, darkTheme: Boolean): Brush {
    val palette = themeSettings.toPalette(darkTheme)
    return if (darkTheme) {
        Brush.verticalGradient(
            listOf(
                palette.background,
                palette.primary.copy(alpha = 0.18f),
                palette.tertiary.copy(alpha = 0.10f).compositeOver(palette.surface),
                palette.background
            )
        )
    } else {
        val accentWash = palette.primary.copy(alpha = if (themeSettings.monetEnabled) 0.18f else 0.12f)
        Brush.verticalGradient(
            listOf(
                palette.background,
                accentWash.compositeOver(palette.background),
                palette.surfaceContainer,
                palette.background
            )
        )
    }
}

private fun ThemeSettingsState.toPalette(darkTheme: Boolean): SiListenPalette {
    val base = accentColor()
    val primary = when (paletteStyle) {
        ThemePaletteStyleOption.TonalSpot -> base.shiftColor(saturation = 0.82f, value = if (darkTheme) 1.08f else 0.95f)
        ThemePaletteStyleOption.Vibrant -> base.shiftColor(saturation = 1.32f, value = 1.08f)
        ThemePaletteStyleOption.Expressive -> base.shiftColor(hue = 28f, saturation = 1.18f, value = 1.04f)
        ThemePaletteStyleOption.Fidelity -> base
        ThemePaletteStyleOption.Content -> base.shiftColor(hue = -18f, saturation = 1.05f, value = if (darkTheme) 1.10f else 0.98f)
        ThemePaletteStyleOption.Monochrome -> if (darkTheme) Color(0xFFE7E7EA) else Color(0xFF202124)
    }
    val secondary = when (paletteStyle) {
        ThemePaletteStyleOption.TonalSpot -> base.shiftColor(hue = 42f, saturation = 0.74f, value = 1.03f)
        ThemePaletteStyleOption.Vibrant -> base.shiftColor(hue = 52f, saturation = 1.22f, value = 1.12f)
        ThemePaletteStyleOption.Expressive -> base.shiftColor(hue = 118f, saturation = 1.05f, value = 1.05f)
        ThemePaletteStyleOption.Fidelity -> base.shiftColor(hue = 18f, saturation = 0.92f, value = 1.02f)
        ThemePaletteStyleOption.Content -> base.shiftColor(hue = -64f, saturation = 0.96f, value = 1.04f)
        ThemePaletteStyleOption.Monochrome -> if (darkTheme) Color(0xFFBFC2C7) else Color(0xFF6E7278)
    }
    val tertiary = when (paletteStyle) {
        ThemePaletteStyleOption.TonalSpot -> base.shiftColor(hue = -46f, saturation = 0.72f, value = 1.08f)
        ThemePaletteStyleOption.Vibrant -> base.shiftColor(hue = -92f, saturation = 1.16f, value = 1.10f)
        ThemePaletteStyleOption.Expressive -> base.shiftColor(hue = -128f, saturation = 1.12f, value = 1.08f)
        ThemePaletteStyleOption.Fidelity -> base.shiftColor(hue = -22f, saturation = 0.88f, value = 1.04f)
        ThemePaletteStyleOption.Content -> base.shiftColor(hue = 76f, saturation = 0.98f, value = 1.03f)
        ThemePaletteStyleOption.Monochrome -> if (darkTheme) Color(0xFF92969C) else Color(0xFF8E939A)
    }
    val uiMiuix = uiMode == ThemeUiModeOption.Miuix
    val background = when {
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> Color(0xFF020403)
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> Color(0xFF111311)
        darkTheme && uiMiuix -> Color(0xFF050505)
        darkTheme -> Color(0xFF101114)
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> primary.copy(alpha = 0.08f).compositeOver(Color(0xFFFFFFFF))
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> primary.copy(alpha = 0.05f).compositeOver(Color(0xFFFBFAF7))
        uiMiuix -> Color(0xFFF7F7F9)
        else -> Color(0xFFFDFBFF)
    }
    val surface = when {
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> primary.copy(alpha = 0.10f).compositeOver(Color(0xFF090B0A))
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> Color(0xFF171A17)
        darkTheme && uiMiuix -> Color(0xFF0F0F0F)
        darkTheme -> Color(0xFF1B1C20)
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> primary.copy(alpha = 0.05f).compositeOver(Color.White)
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> Color(0xFFFFFCF7)
        uiMiuix -> Color(0xFFFDFDFE)
        else -> Color(0xFFFFFBFE)
    }
    val surfaceContainer = when {
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> primary.copy(alpha = 0.16f).compositeOver(Color(0xFF111311))
        darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> Color(0xFF242721)
        darkTheme && uiMiuix -> Color(0xFF1C1C1E)
        darkTheme -> Color(0xFF25262A)
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2025 -> primary.copy(alpha = 0.11f).compositeOver(Color.White)
        !darkTheme && colorSpec == ThemeColorSpecOption.Spec2021 -> primary.copy(alpha = 0.07f).compositeOver(Color(0xFFFFFCF7))
        uiMiuix -> Color(0xFFF0F0F3)
        else -> Color(0xFFF3EDF7)
    }
    return SiListenPalette(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        surface = surface,
        surfaceContainer = surfaceContainer
    )
}

private fun Color.shiftColor(
    hue: Float = 0f,
    saturation: Float = 1f,
    value: Float = 1f
): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    hsv[0] = (hsv[0] + hue).floorMod360()
    hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * value).coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor((alpha * 255f).toInt().coerceIn(0, 255), hsv))
}

private fun Float.floorMod360(): Float {
    val mod = this % 360f
    return if (mod < 0f) mod + 360f else mod
}

private fun onColorFor(color: Color): Color =
    if (color.luminance() > 0.58f) {
        Color(0xFF081109)
    } else {
        Color.White
    }
