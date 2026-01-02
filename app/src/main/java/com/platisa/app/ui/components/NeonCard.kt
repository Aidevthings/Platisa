package com.platisa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.platisa.app.ui.theme.CardBorder
import com.platisa.app.ui.theme.CardSurface
import com.platisa.app.ui.theme.GradientSurface
import com.platisa.app.ui.theme.neonGlow

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = CardBorder,
    backgroundColor: Color = CardSurface.copy(alpha = 0.9f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neonGlow(glowColor, radius = 12.dp, alpha = 0.3f)
            .clip(shape)
            .background(CardSurface.copy(alpha = 0.95f))
            .border(1.dp, glowColor.copy(alpha = 0.5f), shape)
            .padding(16.dp)
    ) {
        content()
    }
}

