package com.example.coded.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Oasis Light Color Scheme - Matching Website Exactly
private val OasisLightColorScheme = lightColorScheme(
    // Primary colors - using OasisGreen (#1F4F46)
    primary = OasisGreen,
    onPrimary = OasisWhite,
    primaryContainer = OasisMint,
    onPrimaryContainer = OasisDark,

    // Secondary colors - using OasisTeal (#2FAF8F)
    secondary = OasisTeal,
    onSecondary = OasisWhite,
    secondaryContainer = OasisMint,
    onSecondaryContainer = OasisDark,

    // Tertiary colors - using OasisMint for accents
    tertiary = OasisMint,
    onTertiary = OasisDark,
    tertiaryContainer = OasisGray,
    onTertiaryContainer = OasisDark,

    // Error colors
    error = OasisError,
    onError = OasisWhite,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),

    // Background - using light neutrals
    background = OasisBackgroundLight,
    onBackground = OasisTextPrimary,

    // Surface - cards, dialogs
    surface = OasisSurfaceLight,
    onSurface = OasisTextPrimary,
    surfaceVariant = OasisGray,
    onSurfaceVariant = OasisTextSecondary,

    // Outline
    outline = OasisGrayMedium,
    outlineVariant = OasisGrayLight,

    scrim = Color(0x80000000)
)

// Oasis Dark Color Scheme
private val OasisDarkColorScheme = darkColorScheme(
    // Primary colors - lighter versions for dark mode
    primary = OasisTeal,
    onPrimary = OasisDark,
    primaryContainer = OasisGreenDark,
    onPrimaryContainer = OasisMint,

    // Secondary colors
    secondary = OasisMint,
    onSecondary = OasisDark,
    secondaryContainer = OasisTeal,
    onSecondaryContainer = OasisWhite,

    // Tertiary
    tertiary = OasisMint,
    onTertiary = OasisDark,
    tertiaryContainer = OasisGreen,
    onTertiaryContainer = OasisGray,

    // Error
    error = Color(0xFFEF5350),
    onError = OasisDark,
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),

    // Background - using OasisDark (#0F2E2B)
    background = OasisBackgroundDark,
    onBackground = OasisWhite,

    // Surface
    surface = OasisSurfaceDark,
    onSurface = OasisTextOnDark,
    surfaceVariant = OasisGreen,
    onSurfaceVariant = OasisGray,

    // Outline
    outline = OasisGrayMedium,
    outlineVariant = OasisGreenDark,

    scrim = Color(0x80000000)
)

@Composable
fun CodedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to maintain exact brand consistency with website
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OasisDarkColorScheme else OasisLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OasisTypography,
        content = content
    )
}