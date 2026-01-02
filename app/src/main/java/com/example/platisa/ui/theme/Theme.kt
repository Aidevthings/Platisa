package com.example.platisa.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = NeonPurple,
    tertiary = DeepCyan,
    background = VoidBackground,
    surface = CardSurface,
    onPrimary = VoidBackground,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = AlertRed
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = DeepTeal,
    secondary = DeepPurpleAccent,
    tertiary = DeepOrangeAccent,
    background = SolarBackground,
    surface = SolarSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SolarTextPrimary,
    onSurface = SolarTextPrimary,
    error = DeepRedAccent
)

@Composable
fun PlatisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our brand
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
    
    // Define Custom Colors (Extension) based on theme
    val customColors = if (darkTheme) {
        PlatisaCustomColors(
            neonCyan = NeonCyanHome,    // Bright Neon
            neonPurple = NeonMagentaHome, // Bright Neon
            neonGreen = NeonGreenHome,  // Bright Neon
            cardBackground = CardSurface,
            cardBorder = CardBorder,
            gradientSurface = GradientSurface,
            textSecondary = TextSecondaryDark,
            textLabel = TextLabelDark,
            isDark = true,
            statusPaid = StatusPaidStart,
            statusUnpaid = StatusUnpaidStart,
            statusProcessing = StatusProcessingStart,
            navHome = Color(0xFFFFD700),      // Neon Yellow
            navMarket = Color(0xFF00FF9D),    // Matrix Green
            navAnalytics = Color(0xFFD946EF), // Neon Purple/Pink
            navSettings = Color(0xFF00F0FF),   // Cyber Cyan
            
            // New Semantic Mapping (Dark)
            textPrimary = Color.White,
            textOnPrimary = Color.Black,
            error = AlertRed,
            onError = Color.White,
            success = MatrixGreen,
            onSuccess = Color.Black,
            warning = ElectricYellow,
            surfaceContainer = CardSurface,
            surfaceContainerHigh = SubscriptionCardDark
        )
    } else {
        PlatisaCustomColors(
            neonCyan = DeepTeal,        // Deep Readable Teal
            neonPurple = DeepPurpleAccent, // Deep Purple
            neonGreen = DeepGreenAccent,   // Deep Green
            cardBackground = SolarSurfaceVariant,
            cardBorder = Color(0xFFD1D5DB), // Light Gray Border
            gradientSurface = SolarGradientSurface,
            textSecondary = SolarTextSecondary,
            textLabel = SolarTextLabel,
            isDark = false,
            statusPaid = SolarStatusPaid,
            statusUnpaid = SolarStatusUnpaid,
            statusProcessing = SolarStatusProcessing,
            navHome = Color(0xFF1976D2),      // Blue
            navMarket = Color(0xFF2E7D32),    // Dark Green
            navAnalytics = Color(0xFFAD1457), // Dark Magenta
            navSettings = Color(0xFF00838F),   // Dark Cyan
            
            // New Semantic Mapping (Light)
            textPrimary = SolarTextPrimary,
            textOnPrimary = Color.White,
            error = DeepRedAccent,
            onError = Color.White,
            success = DeepGreenAccent,
            onSuccess = Color.White,
            warning = DeepOrangeAccent,
            surfaceContainer = SolarSurface,
            surfaceContainerHigh = SolarSurfaceVariant
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Limit Font Scale to max 1.3x prevents layout breakage on devices with extreme accessibility settings
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val maxFontScale = 1.3f
    val fixedFontScale = currentDensity.fontScale.coerceAtMost(maxFontScale)
    
    // 2. Cap Display Density (Display Size)
    // Ensure screen width is at least 411dp (Standard Large Phone) to prevent UI from being "too huge"
    val configuration = LocalContext.current.resources.configuration
    val screenWidthPx = configuration.screenWidthDp * currentDensity.density
    val minScreenWidthDp = 411f // Increased from 360f to force smaller UI scaling on Max settings
    
    // Calculate max allowed density to maintain minScreenWidthDp
    // density = pixels / dp  =>  maxDensity = pixels / minWidthDp
    val maxDensity = screenWidthPx / minScreenWidthDp
    
    // If current density is higher (meaning screen is "zoomed in" more), clamp it
    val fixedDensityValue = currentDensity.density.coerceAtMost(maxDensity)

    val fixedDensity = androidx.compose.ui.unit.Density(
        density = fixedDensityValue,
        fontScale = fixedFontScale
    )

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides fixedDensity,
        LocalPlatisaColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Custom Theme Extension System
data class PlatisaCustomColors(
    val neonCyan: androidx.compose.ui.graphics.Color,
    val neonPurple: androidx.compose.ui.graphics.Color,
    val neonGreen: androidx.compose.ui.graphics.Color,
    val cardBackground: androidx.compose.ui.graphics.Color,
    val cardBorder: androidx.compose.ui.graphics.Color,
    val gradientSurface: androidx.compose.ui.graphics.Brush,
    val textSecondary: androidx.compose.ui.graphics.Color,
    val textLabel: androidx.compose.ui.graphics.Color,
    val isDark: Boolean,
    val statusPaid: androidx.compose.ui.graphics.Color,
    val statusUnpaid: androidx.compose.ui.graphics.Color,
    val statusProcessing: androidx.compose.ui.graphics.Color,
    // Navigation Colors
    val navHome: androidx.compose.ui.graphics.Color,
    val navMarket: androidx.compose.ui.graphics.Color,
    val navAnalytics: androidx.compose.ui.graphics.Color,
    val navSettings: androidx.compose.ui.graphics.Color,
    // Background color for tests
    val background: androidx.compose.ui.graphics.Color = if (isDark) VoidBackground else SolarBackground,
    
    // New Semantic Extensions
    val textPrimary: androidx.compose.ui.graphics.Color,
    val textOnPrimary: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
    val onError: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val onSuccess: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val surfaceContainer: androidx.compose.ui.graphics.Color, // For popups/dialogs
    val surfaceContainerHigh: androidx.compose.ui.graphics.Color // For cards on top of cards
)

val LocalPlatisaColors = androidx.compose.runtime.staticCompositionLocalOf {
    PlatisaCustomColors(
        neonCyan = androidx.compose.ui.graphics.Color.Unspecified,
        neonPurple = androidx.compose.ui.graphics.Color.Unspecified,
        neonGreen = androidx.compose.ui.graphics.Color.Unspecified,
        cardBackground = androidx.compose.ui.graphics.Color.Unspecified,
        cardBorder = androidx.compose.ui.graphics.Color.Unspecified,
        gradientSurface = androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent)),
        textSecondary = androidx.compose.ui.graphics.Color.Unspecified,
        textLabel = androidx.compose.ui.graphics.Color.Unspecified,
        isDark = true,
        statusPaid = androidx.compose.ui.graphics.Color.Green,
        statusUnpaid = androidx.compose.ui.graphics.Color.Magenta,
        statusProcessing = androidx.compose.ui.graphics.Color.Yellow,
        navHome = androidx.compose.ui.graphics.Color.Yellow,
        navMarket = androidx.compose.ui.graphics.Color.Green,
        navAnalytics = androidx.compose.ui.graphics.Color.Magenta,
        navSettings = androidx.compose.ui.graphics.Color.Cyan,
        background = VoidBackground,
        textPrimary = androidx.compose.ui.graphics.Color.White,
        textOnPrimary = androidx.compose.ui.graphics.Color.Black,
        error = androidx.compose.ui.graphics.Color.Red,
        onError = androidx.compose.ui.graphics.Color.White,
        success = androidx.compose.ui.graphics.Color.Green,
        onSuccess = androidx.compose.ui.graphics.Color.Black,
        warning = androidx.compose.ui.graphics.Color.Yellow,
        surfaceContainer = androidx.compose.ui.graphics.Color.Gray,
        surfaceContainerHigh = androidx.compose.ui.graphics.Color.DarkGray
    )
}

// Easy Access Object
object PlatisaTheme {
    val colors: PlatisaCustomColors
        @Composable
        get() = LocalPlatisaColors.current
}
