package com.platisa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.platisa.app.ui.theme.*

@Composable
fun GradientBar(
    progress: Float, // 0.0 to 1.0
    gradient: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    backgroundColor: Color = CardSurface
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(gradient)
        )
    }
}

@Composable
fun VerticalGradientBar(
    progress: Float, // 0.0 to 1.0
    gradient: Brush,
    modifier: Modifier = Modifier,
    width: Dp = 12.dp,
    backgroundColor: Color = CardSurface
) {
    Box(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(width / 2))
            .background(backgroundColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress)
                .background(gradient)
        )
    }
}

@Composable
fun TimelineNode(
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isActive) activeColor else TextSecondary,
                shape = CircleShape
            )
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.1f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextLabel,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

