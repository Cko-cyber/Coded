package com.example.coded.ui.theme

import androidx.compose.ui.graphics.Color

// Oasis Brand Colors - EXACT MATCH to Website Tailwind Config
// Primary Color Palette (from tailwind.config.js)
val OasisGreen = Color(0xFF1F4F46)      // oasisGreen - Main brand color
val OasisTeal = Color(0xFF2FAF8F)       // oasisTeal - Secondary/accent
val OasisMint = Color(0xFF6FD3B3)       // oasisMint - Light accent
val OasisDark = Color(0xFF0F2E2B)       // oasisDark - Deep dark
val OasisGray = Color(0xFFE6ECEB)       // oasisGray - Light backgrounds

val OasisGold = Color(0xFFFFD700)
val OasisGoldDark = Color(0xFFFFC107)

// Derived Colors for UI consistency
val OasisGreenLight = Color(0xFF2F6F66)     // Lighter version of green
val OasisGreenDark = Color(0xFF153F36)      // Darker version of green
val OasisTealLight = Color(0xFF4FCFAF)      // Lighter teal
val OasisTealDark = Color(0xFF258F7F)       // Darker teal

// Neutral Colors
val OasisWhite = Color(0xFFFFFFFF)
val OasisBlack = Color(0xFF000000)
val OasisGrayLight = Color(0xFFF5F5F5)
val OasisGrayMedium = Color(0xFF9E9E9E)
val OasisGrayDark = Color(0xFF616161)

// Status Colors (keeping standard for UX)
val OasisSuccess = Color(0xFF2FAF8F)        // Using oasisTeal
val OasisWarning = Color(0xFFFF9800)
val OasisError = Color(0xFFF44336)
val OasisInfo = Color(0xFF2196F3)

// Background Colors
val OasisBackgroundLight = Color(0xFFFAFAFA)
val OasisBackgroundDark = Color(0xFF0F2E2B)  // Using oasisDark

// Card/Surface Colors
val OasisSurfaceLight = Color(0xFFFFFFFF)
val OasisSurfaceDark = Color(0xFF1F4F46)     // Using oasisGreen

// Text Colors
val OasisTextPrimary = Color(0xFF0F2E2B)     // oasisDark for text
val OasisTextSecondary = Color(0xFF616161)
val OasisTextOnPrimary = Color(0xFFFFFFFF)
val OasisTextOnDark = Color(0xFFE6ECEB)      // oasisGray on dark

// Gradient Colors (for backgrounds, cards, CTAs)
val OasisGradientStart = OasisGreen          // #1F4F46
val OasisGradientMiddle = OasisTeal          // #2FAF8F
val OasisGradientEnd = OasisMint             // #6FD3B3

// Service Type Colors (customized to match Oasis brand)
val ServiceGrassCutting = Color(0xFF2FAF8F)  // OasisTeal
val ServiceYardClearing = Color(0xFF1F4F46)  // OasisGreen
val ServiceGardening = Color(0xFF6FD3B3)     // OasisMint
val ServiceTreeFelling = Color(0xFF0F2E2B)   // OasisDark
val ServiceCleaning = Color(0xFF2196F3)      // Blue
val ServicePlumbing = Color(0xFFFF9800)      // Orange
val ServiceElectrical = Color(0xFF9C27B0)    // Purple
val ServiceDSTV = Color(0xFF3F51B5)          // Indigo
val ServiceMaintenance = Color(0xFF00BCD4)   // Cyan

// Legacy colors (deprecated - keeping for backward compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Accent Colors for CTAs and highlights
val OasisAccent = OasisTeal                  // Primary CTA color
val OasisAccentHover = OasisTealDark         // Hover state
val borderColor = Color(0xFF1F4F46)
val borderColorHover = Color(0xFF2FAF8F)