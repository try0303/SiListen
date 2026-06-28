package com.silisten.app.ui.theme

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.silisten.app.ThemeAccentOption
import com.silisten.app.ThemeModeOption
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
    val accent = themeSettings.accentColor()
    val appearance = remember(themeSettings, darkTheme) {
        themeSettings.toAppearance(darkTheme)
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
            primary = accent,
            secondary = Color(0xFFFFC857),
            tertiary = Color(0xFF8BD3FF),
            background = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFF050505) else Color(0xFF101114),
            surface = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFF0F0F0F) else Color(0xFF1B1C20),
            surfaceContainer = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFF1C1C1E) else Color(0xFF25262A),
            onPrimary = themeSettings.onAccentColor(),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = Color(0xFFFFC857),
            tertiary = Color(0xFF006D77),
            background = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFFF7F7F9) else Color(0xFFFDFBFF),
            surface = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFFFDFDFE) else Color(0xFFFFFBFE),
            surfaceContainer = if (themeSettings.uiMode == ThemeUiModeOption.Miuix) Color(0xFFF0F0F3) else Color(0xFFF3EDF7),
            onPrimary = themeSettings.onAccentColor(),
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

fun ThemeSettingsState.toAppearance(darkTheme: Boolean): SiListenAppearance =
    SiListenAppearance(
        uiMode = uiMode,
        dark = darkTheme,
        blurEnabled = blurEnabled,
        floatingBottomBarEnabled = floatingBottomBarEnabled,
        floatingBottomBarBlurEnabled = floatingBottomBarBlurEnabled,
        predictiveBackEnabled = predictiveBackEnabled,
        accent = accentColor()
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
    return if (accentColor().luminance() > 0.58f) {
        Color(0xFF081109)
    } else {
        Color.White
    }
}

fun appBackgroundBrush(themeSettings: ThemeSettingsState, darkTheme: Boolean): Brush {
    return if (darkTheme) {
        val accent = themeSettings.accentColor()
        Brush.verticalGradient(
            listOf(
                Color(0xFF050505),
                accent.copy(alpha = 0.18f),
                accent.copy(alpha = 0.10f).compositeOver(Color(0xFF111216)),
                Color(0xFF050505)
            )
        )
    } else {
        val accentWash = themeSettings.accentColor().copy(alpha = if (themeSettings.monetEnabled) 0.18f else 0.12f)
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                accentWash.compositeOver(Color.White),
                Color(0xFFF5F6F8),
                Color(0xFFFFFFFF)
            )
        )
    }
}
