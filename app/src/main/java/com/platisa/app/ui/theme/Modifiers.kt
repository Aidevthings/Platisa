package com.platisa.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 8.dp,
    alpha: Float = 0.6f
): Modifier {
    // Removed shadow to prevent dark overlay - now just uses colored border for glow effect
    return this
        .border(
            width = 2.dp, // Slightly thicker for glow effect
            color = color.copy(alpha = alpha),
            shape = RoundedCornerShape(radius)
        )
}

fun Modifier.glassBackground(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    return this
        .background(
            color = DarkGlass.copy(alpha = 0.7f),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
}

fun Modifier.cyberpunkGradient(): Modifier {
    return this.background(
        brush = Brush.horizontalGradient(
            colors = listOf(
                ElectricCyan.copy(alpha = 0.2f),
                NeonPurple.copy(alpha = 0.2f)
            )
        )
    )
}

