package com.selfproxy.vpn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SelfProxyBlueDark80,
    onPrimary = TextPrimaryDark,
    primaryContainer = SelfProxyBlueDark60,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SelfProxyAccentDark,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SelfProxyAccentDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = ConnectedGreenDark,
    onTertiary = TextPrimaryDark,
    error = ErrorRedDark,
    onError = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DisconnectedGrayDark
)

private val LightColorScheme = lightColorScheme(
    primary = SelfProxyBlue,
    onPrimary = Color.White,
    primaryContainer = SelfProxyBlueLight,
    onPrimaryContainer = SelfProxyBlueDark,
    secondary = SelfProxyAccent,
    onSecondary = Color.White,
    secondaryContainer = SelfProxyAccentLight,
    onSecondaryContainer = SelfProxyBlueDark,
    tertiary = ConnectedGreen,
    onTertiary = Color.White,
    error = ErrorRed,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    outline = DisconnectedGray
)

@Composable
fun SelfProxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to use SelfProxy brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
