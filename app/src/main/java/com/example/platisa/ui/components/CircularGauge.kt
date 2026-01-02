package com.example.platisa.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular gauge component with glowing gradient ring and centered value display.
 * Matches the Cyberpunk reference style with radial progress indicators.
 *
 * @param value Current value to display
 * @param maxValue Maximum value for progress calculation
 * @param unit Unit label (e.g., "kWh", "RSD")
 * @param label Description label below value
 * @param size Diameter of the gauge
 * @param ringColors Gradient colors for the progress ring
 * @param showTicks Whether to show measurement tick marks
 * @param animate Whether to animate the progress
 */
@Composable
fun CircularGauge(
    value: Float,
    maxValue: Float,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    ringColors: List<Color> = listOf(Color(0xFF00F0FF), Color(0xFF2979FF)),
    glowColor: Color = ringColors.first(),
    backgroundColor: Color = Color(0x1A8F9BB3),
    showTicks: Boolean = true,
    animate: Boolean = true,
    strokeWidth: Dp = 12.dp
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (animate) progress else progress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "gauge_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Canvas for rings and ticks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            val radius = (canvasSize.width / 2f) * 0.7f
            val strokeWidthPx = strokeWidth.toPx()

            // Background ring (dim)
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress ring with gradient
            val safeRingColors = if (ringColors.size < 2) {
                listOf(ringColors.firstOrNull() ?: Color.Cyan, ringColors.firstOrNull() ?: Color.Blue)
            } else {
                ringColors
            }

            val brush = Brush.sweepGradient(
                colors = safeRingColors,
                center = center
            )
            
            drawArc(
                brush = brush,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                blendMode = BlendMode.Plus // Glow effect
            )

            // Outer glow layer (bloom effect)
            drawArc(
                color = glowColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx * 2f, cap = StrokeCap.Round),
                alpha = 0.2f
            )

            // Tick marks
            if (showTicks) {
                val tickCount = 12
                val tickRadius = radius + strokeWidthPx
                val tickLength = 8.dp.toPx()
                
                for (i in 0 until tickCount) {
                    val angle = (i * 360f / tickCount) - 90f
                    val angleRad = Math.toRadians(angle.toDouble())
                    
                    val startX = center.x + tickRadius * cos(angleRad).toFloat()
                    val startY = center.y + tickRadius * sin(angleRad).toFloat()
                    val endX = center.x + (tickRadius + tickLength) * cos(angleRad).toFloat()
                    val endY = center.y + (tickRadius + tickLength) * sin(angleRad).toFloat()
                    
                    drawLine(
                        color = backgroundColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%.0f", value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value / 5).sp,
                    letterSpacing = (-1).sp
                ),
                color = Color.White
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Radial progress meter - simplified arc indicator
 */
@Composable
fun RadialProgressMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    colors: List<Color> = listOf(Color(0xFFBC13FE), Color(0xFFD946EF)),
    strokeWidth: Dp = 10.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val radius = (canvasSize.width / 2f) * 0.8f

        val brush = Brush.sweepGradient(colors, center = center)
        
        drawArc(
            brush = brush,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}
