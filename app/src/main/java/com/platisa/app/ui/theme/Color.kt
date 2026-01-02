package com.platisa.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==============================================================================
// 1. Primitive Colors (The core palette)
// ==============================================================================

// Neutrals
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Transparent = Color(0x00000000)

// Dark Theme Neutrals
val VoidBackground = Color(0xFF080B16)
val CardSurface = Color(0xFF111625)
val CardBorder = Color(0xFF232946)
val TextSecondaryDark = Color(0xFF8F9BB3)
val TextLabelDark = Color(0xFF58668B)

// Light Theme Neutrals (Solarpunk)
val SolarBackground = Color(0xFFF0F2F5)     // Soft Gray-White
val SolarSurface = Color(0xFFFAF9F6)        // Paper-like Off-White
val SolarSurfaceVariant = Color(0xFFE8EAED) // Slightly darker for headers
val SolarBorderBrown = Color(0xFFA1887F)    // Light Brown Frame
val SolarTextPrimary = Color(0xFF1A1C1E)    // Near Black
val SolarTextSecondary = Color(0xFF42474E)  // Dark Gray
val SolarTextLabel = Color(0xFF72777F)      // Medium Gray

// Neon Accents (Cyberpunk)
val CyberCyan = Color(0xFF00F0FF)
val DeepCyan = Color(0xFF0099FF)
val NeonPurple = Color(0xFFD946EF)
val ElectricPurple = Color(0xFFBC13FE)
val DeepPurple = Color(0xFF9C27B0)
val ElectricYellow = Color(0xFFFFD700)
val DeepOrange = Color(0xFFFFA000)
val MatrixGreen = Color(0xFF00FF9D)
val DeepGreen = Color(0xFF00C853)
val AlertRed = Color(0xFFFF3D00)

// Light Theme Accents (Deep/Readable)
val DeepTeal = Color(0xFF00796B)           // Readable version of CyberCyan
val DeepPurpleAccent = Color(0xFF7B1FA2)   // Readable version of NeonPurple
val DeepOrangeAccent = Color(0xFFE65100)   // Readable version of DeepOrange
val DeepGreenAccent = Color(0xFF2E7D32)    // Readable version of MatrixGreen
val DeepRedAccent = Color(0xFFC62828)      // Readable version of AlertRed

// Status Colors (Shared/Specific)
val StatusPaidStart = Color(0xFF00FF00) // Neon Green
val StatusProcessingStart = Color(0xFFFF00D9) // Neon Magenta
val StatusUnpaidStart = Color(0xFF00EAFF) // Neon Cyan

val SolarStatusPaid = Color(0xFF81C784)
val SolarStatusProcessing = Color(0xFFFF80AB)
val SolarStatusUnpaid = Color(0xFF64B5F6)

// ==============================================================================
// 2. Gradients
// ==============================================================================
val GradientCyan = Brush.verticalGradient(listOf(CyberCyan, DeepCyan))
val GradientPurple = Brush.verticalGradient(listOf(NeonPurple, DeepPurple))
val GradientYellow = Brush.verticalGradient(listOf(ElectricYellow, DeepOrange))
val GradientGreen = Brush.verticalGradient(listOf(MatrixGreen, DeepGreen))
val GradientSurface = Brush.verticalGradient(listOf(CardSurface.copy(alpha = 0.9f), CardSurface.copy(alpha = 0.7f)))

val SolarGradientSurface = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FA)))

// ==============================================================================
// 3. Subscription & Specific UI Colors
// ==============================================================================
val SubscriptionCardDark = Color(0xFF1A1D24)

// Home Screen Icons
// Light
val IconBellLight = Color(0xFF03A9F4)   // Light Blue
val IconThemeLight = Color(0xFF0D47A1)  // Dark Blue
val IconHelpLight = Color(0xFF4CAF50)   // Normal Green
// Dark
val IconBellDark = Color(0xFF00E5FF)    // Cyan
val IconThemeDark = Color(0xFFFFD700)   // Yellow
val IconHelpDark = Color(0xFF00E676)    // Bright Green

// Charts (Cyberpunk)
val CyberChartElectricity = Color(0xFF25D1F4) // Cyan
val CyberChartInfoStan = Color(0xFFEC4899)    // Pink
val CyberChartPhone = Color(0xFFFACC15)       // Yellow
val CyberChartWater = Color(0xFFA7F3D0)       // Green

// ==============================================================================
// 4. Legacy Aliases (To be deprecated/consolidated)
// ==============================================================================
val NeonCyanHome = StatusUnpaidStart
val NeonCyanLight = Color(0xFF00FFFF)
val NeonMagentaHome = StatusProcessingStart
val NeonGreenHome = Color(0xFF39FF14)
val NeonLimeHome = Color(0xFFAAFF00)

val HtmlNeonGreen = NeonGreenHome
val HtmlNeonPink = Color(0xFFFF1493)
val HtmlNeonCyan = NeonCyanHome
val HtmlNeonYellow = Color(0xFFFFFF00)

val DeepVoidBlue = VoidBackground
val DarkGlass = CardSurface
val ElectricCyan = CyberCyan
val NeonCyan = CyberCyan
val NeonPurpleLegacy = NeonPurple
val MagentaNeon = NeonPurple
val OrangeGlow = DeepOrange
val BrightBlue = DeepCyan
val GlassWhite = Color(0x1AFFFFFF)
val ErrorRed = AlertRed
val TextSecondary = TextSecondaryDark
val TextLabel = TextLabelDark

// Statistics / Cyber Legacy Aliases
val CyberPrimary = CyberCyan
val CyberSecondary = NeonPurple
val CyberBackgroundDark = VoidBackground
val CyberCardBg = CardSurface
val LightStatsBackground = SolarBackground
val LightStatsTextMain = SolarTextPrimary
val LightStatsTextMuted = SolarTextSecondary
val LightStatsPrimary = DeepTeal
val LightStatsSecondary = DeepPurpleAccent
val LightStatsBorder = SolarBorderBrown

// Gradients List
val CyanBlueGradient = listOf(CyberCyan, DeepCyan)
val PurpleMagentaGradient = listOf(DeepPurple, NeonPurple)
val OrangeRedGradient = listOf(DeepOrange, AlertRed)
val FullSpectrumGradient = listOf(CyberCyan, DeepCyan, DeepPurple, NeonPurple)

