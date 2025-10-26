package com.example.coded.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HerdmatGold,
    secondary = HerdmatTeal,
    tertiary = CodedCircuitGreen,
    background = HerdmatGreen,
    surface = HerdmatGrayDark,
    onPrimary = HerdmatGreen,
    onSecondary = HerdmatGrayLight,
    onTertiary = HerdmatWhite,
    onBackground = HerdmatGrayLight,
    onSurface = HerdmatGrayLight,
)

private val LightColorScheme = lightColorScheme(
    primary = HerdmatGreen,
    secondary = HerdmatGold,
    tertiary = HerdmatTeal,
    background = HerdmatGrayLight,
    surface = HerdmatWhite,
    onPrimary = HerdmatWhite,
    onSecondary = HerdmatWhite,
    onBackground = HerdmatGreen,
    onSurface = HerdmatGrayDark,
)

@Composable
fun CodedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
