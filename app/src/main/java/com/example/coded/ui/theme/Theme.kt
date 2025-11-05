package com.example.coded.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

/**
 * 🎨 HERDMAT MODERN COLOR SYSTEM
 * Based on professional design principles for 2025
 */

// Primary Brand Colors
val HerdmatDeepGreen = Color(0xFF013B33) // Main brand color
val HerdmatGoldLegacy = Color(0xFFFFD700) // Premium accent
val HerdmatTeallegacy = Color(0xFF00A896) // Interactive elements

// Semantic Colors
val SuccessGreen = Color(0xFF4CAF50) // Success states, active indicators
val WarningOrange = Color(0xFFFF6F00) // Warnings, important actions
val ErrorRed = Color(0xFFF44336) // Errors, destructive actions
val InfoBlue = Color(0xFF2196F3) // Information, links

// Neutral Palette (Modern grayscale)
val NeutralWhite = Color(0xFFFFFFFF)
val NeutralGray50 = Color(0xFFFAFAFA) // Background
val NeutralGray100 = Color(0xFFF5F5F5) // Secondary background
val NeutralGray200 = Color(0xFFEEEEEE) // Dividers
val NeutralGray300 = Color(0xFFE0E0E0) // Borders
val NeutralGray400 = Color(0xFFBDBDBD) // Disabled
val NeutralGray500 = Color(0xFF9E9E9E) // Placeholders
val NeutralGray600 = Color(0xFF757575) // Secondary text
val NeutralGray700 = Color(0xFF616161) // Body text
val NeutralGray800 = Color(0xFF424242) // Headings
val NeutralGray900 = Color(0xFF212121) // Dark mode background
val NeutralBlack = Color(0xFF000000)

// Overlay Colors
val OverlayLight = Color(0x1A000000) // 10% black for light overlays
val OverlayMedium = Color(0x4D000000) // 30% black for medium overlays
val OverlayDark = Color(0x80000000) // 50% black for dark overlays

// Gradient Colors
val GradientStart = Color(0xFF013B33)
val GradientEnd = Color(0xFF00A896)

// Status Colors with transparency
val ActiveGreenBg = Color(0x1A4CAF50) // 10% green for active badges
val PendingOrangeBg = Color(0x1AFF6F00) // 10% orange for pending badges
val InactiveGrayBg = Color(0x1A9E9E9E) // 10% gray for inactive badges

// Listing Tier Colors
val TierFree = SuccessGreen
val TierBasic = InfoBlue
val TierBulk = WarningOrange
val TierPremium = HerdmatGold

// Message Colors
val MessageSent = HerdmatDeepGreen
val MessageReceived = NeutralWhite
val MessageTimestamp = NeutralGray600
val MessageRead = SuccessGreen

// Interactive States
val Pressed = Color(0x1A000000) // 10% black overlay
val Focused = Color(0x1F000000) // 12% black overlay
val Hovered = Color(0x0A000000) // 4% black overlay
val Dragged = Color(0x14000000) // 8% black overlay