package com.platisa.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.platisa.app.R

import androidx.compose.ui.graphics.luminance

@Composable
fun AppBackground() {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Or better, check LocalPlatisaColors.isDark if available, or just rely on colorScheme.
    // Since AppBackground is low-level, let's use isSystemInDarkTheme() logic or passed param?
    // But we have custom theme toggle. MaterialTheme.colorScheme.background IS updated by our theme.
    // So if background is dark, we show image. If background is light, we show background color.
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        if (isDark) {
            Image(
                painter = painterResource(id = R.drawable.pozadina),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            // Light Theme Background
            Image(
                painter = painterResource(id = R.drawable.light_background),
                contentDescription = "Light Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f // Watermark / Washed out effect
            )
            // Optional: Add a subtle overlay if needed to ensure text readability, 
            // but user asked to "use this as a background picture", so we start clean.
        }
    }
}

