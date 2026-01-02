package com.example.platisa.ui.screens.home

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platisa.ui.theme.CyberCyan
import com.example.platisa.R
import androidx.compose.ui.text.font.Font
import java.math.BigDecimal

import com.example.platisa.ui.theme.neonGlow
import coil.ImageLoader
import coil.decode.GifDecoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import android.os.Build
import coil.decode.ImageDecoderDecoder
import android.util.Log

import com.example.platisa.ui.theme.LocalPlatisaColors

// --- TEXT STYLES ---
private val CyberpunkFontFamily = FontFamily(
    Font(R.font.audiowide, FontWeight.Normal)
)

private fun getShadow(color: Color) = Shadow(
    color = color.copy(alpha = 0.6f),
    offset = Offset(1f, 1f),
    blurRadius = 4f
)

private val GlobalTextShadowSubtle = Shadow(
    color = Color.Black.copy(alpha = 0.5f),
    offset = Offset(1f, 1f),
    blurRadius = 2f
)

@Composable
fun GaugePanel(
    totalAmount: BigDecimal,
    totalPaid: BigDecimal,
    totalUnpaid: BigDecimal,
    currency: String,
    celebrationImagePath: String?,
    modifier: Modifier = Modifier
) {
    val customColors = LocalPlatisaColors.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Calculate if all bills are paid
    val total = totalPaid.add(totalUnpaid)
    val isFullyPaid = total.compareTo(java.math.BigDecimal.ZERO) > 0 && 
                      totalUnpaid.compareTo(java.math.BigDecimal.ZERO) <= 0
    
    // Delayed celebration state - waits for gauge animation (1500ms) + 2 seconds
    var showCelebration by remember { mutableStateOf(false) }
    
    LaunchedEffect(isFullyPaid) {
        if (isFullyPaid) {
            // Wait for gauge animation (1500ms) + extra delay (2000ms)
            kotlinx.coroutines.delay(3500L)
            showCelebration = true
        } else {
            // Reset immediately when not fully paid
            showCelebration = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) // Let parent (SummaryGrid) handle background
    ) {
        // Show celebration only after the delay
        if (showCelebration) {
            val context = LocalContext.current
            val imageLoader = remember(context) {
                ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .crossfade(true)
                    .build()
            }
            
            val model = when {
                celebrationImagePath?.startsWith("custom:") == true -> {
                    java.io.File(celebrationImagePath.removePrefix("custom:"))
                }
                celebrationImagePath?.startsWith("predefined:") == true -> {
                    val resName = celebrationImagePath.removePrefix("predefined:")
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId != 0) resId else R.drawable.celebration_dance
                }
                else -> R.drawable.celebration_dance
            }
            
            coil.compose.AsyncImage(
                model = model,
                imageLoader = imageLoader,
                contentDescription = "Celebration Dance",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.9f), // Almost fully opaque for the dancing man
                contentScale = androidx.compose.ui.layout.ContentScale.Fit // Fit the dancer in the frame
            )
        } else {
            // Currency Indicators
            // Left: RSD
            CurrencyIndicator(
                text = "RSD",
                isActive = currency == "RSD",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
            )

            // Right: EUR
            CurrencyIndicator(
                text = "€",
                isActive = currency == "EUR",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 12.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize().padding(top = 10.dp)
            ) {
                // 1. The Gauge
                AccountBalanceGauge(
                    totalPaid = totalPaid,
                    totalUnpaid = totalUnpaid,
                    modifier = Modifier
                        .size(width = 124.dp, height = 62.dp)
                )

                // 2. Main Amount Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        com.example.platisa.ui.components.DynamicSizeText(
                            text = com.example.platisa.core.common.Formatters.formatCurrencySimple(totalAmount),
                            color = onSurfaceColor,
                            fontFamily = CyberpunkFontFamily,
                            fontWeight = FontWeight.Bold,
                            minFontSize = 14.sp,
                            maxFontSize = 28.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(bottom = 8.dp) // Add internal breathing room
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountBalanceGauge(
    totalPaid: BigDecimal,
    totalUnpaid: BigDecimal,
    modifier: Modifier = Modifier
) {
    val total = totalPaid.add(totalUnpaid)
    val offsetAngle = 10f // Minimal offset for futuristic look
    
    val percentagePaid = if (total.compareTo(java.math.BigDecimal.ZERO) == 0) {
        1f
    } else {
        totalPaid.divide(total, 4, java.math.RoundingMode.HALF_UP).toFloat()
    }
    
    // Animation for the progress/needle
    val animatedPercentage by animateFloatAsState(
        targetValue = percentagePaid,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
    )
    
    val fullSweep = 180f - (offsetAngle * 2)
    // Needle starts at LEFT (-80°) and moves RIGHT (+80°) as percentage increases
    val targetAngle = -(90f - offsetAngle) + (fullSweep * animatedPercentage)

    val customColors = LocalPlatisaColors.current
    val isDark = customColors.isDark
    
    // Theme Colors for Canvas
    val trackColor = if (isDark) Color(0xFF151515) else Color(0xFFE0E0E0)
    val trackShadowColor = if (isDark) Color.Black else Color.Gray
    val needleColor = if (isDark) Color.White else Color.DarkGray
    val needleGradientStart = if (isDark) Color.White else Color.LightGray
    val needleGradientEnd = if (isDark) Color(0xFF008ba3) else customColors.neonCyan
    
    // Gauge Gradient
    val gaugeStart = customColors.statusPaid // Green-ish
    val gaugeMid = customColors.statusProcessing
    val gaugeEnd = customColors.neonCyan // Or Orange? Using Platform Logic: Green -> Yellow -> Cyan/Teal?
    // Original was NeonGreen -> GaugeYellow -> GaugeOrange
    // Let's stick to standard traffic light but using theme tokens IF available, else literals.
    // Since customColors has specific semantic meanings, let's use literals for the *gauge* specifically to maintain the "Good -> Bad" or "Paid -> Unpaid" logic visually?
    // Actually, "Percentage Paid" means 100% is GOOD. So Green is Good. 
    // The gauge fills up as you pay. So Empty = Bad (Red/Orange), Full = Good (Green)? 
    // Original code: Green -> Yellow -> Orange. Wait. If percentagePaid increases, it fills with this gradient.
    // If it fills from Left to Right, and start is Green... it implies the *beginning* is Green.
    // Usually gauges go Red -> Yellow -> Green.
    // Let's trust the original intent: Green -> Yellow -> Orange.
    
    val cGreen = customColors.statusPaid
    val cYellow = customColors.statusProcessing
    val cOrange = Color(0xFFFF3D00) // Keep vibrant orange for consistency
    

    Canvas(modifier = modifier) {
        val width = size.width
        val barCount = 45
        val barGap = 1.5f // degrees
        val barSweep = (fullSweep / barCount) - barGap
        
        val outerRadius = width / 2
        val innerRadius = outerRadius - 12.dp.toPx()
        val arcCenterY = size.height // Geometric center of the semi-circle arc
        
        // 1. Draw Arcs (Continuous)
        val arcTopLeft = Offset(outerRadius - innerRadius, outerRadius - innerRadius)
        val arcSize = Size(innerRadius * 2, innerRadius * 2)
        val strokeWidth = outerRadius - innerRadius

        // 1. Drop Shadow (Ambient) using native canvas for better blur
        drawIntoCanvas {
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                alpha = if (isDark) 100 else 40
            }
            it.nativeCanvas.drawArc(
                android.graphics.RectF(
                    arcTopLeft.x, arcTopLeft.y,
                    arcTopLeft.x + arcSize.width, arcTopLeft.y + arcSize.height
                ),
                180f + offsetAngle,
                fullSweep,
                false,
                paint
            )
        }

        // 2. Background Track (Recessed 3D Look)
        drawArc(
            color = trackColor, 
            startAngle = 180f + offsetAngle,
            sweepAngle = fullSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Inner shadow/Bevel for depth on track
        drawArc(
            brush = Brush.verticalGradient(
                colors = listOf(trackShadowColor.copy(alpha = 0.8f), Color.Transparent),
                startY = arcTopLeft.y,
                endY = arcTopLeft.y + strokeWidth
            ),
            startAngle = 180f + offsetAngle,
            sweepAngle = fullSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Progress Arc (Lit + 3D Tube Effect)
        if (animatedPercentage > 0f) {
            val sweepAngle = fullSweep * animatedPercentage
            
            // Gradient Brush
            val progressBrush = Brush.horizontalGradient(
                colors = listOf(cGreen, cYellow, cOrange),
                startX = 0f,
                endX = width
            )
            
            // Base Progress
            drawArc(
                brush = progressBrush,
                startAngle = 180f + offsetAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3D Highlight (Top reflection for tube look)
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = 180f + offsetAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(arcTopLeft.x + strokeWidth * 0.2f, arcTopLeft.y + strokeWidth * 0.2f),
                size = Size(arcSize.width - strokeWidth * 0.4f, arcSize.height - strokeWidth * 0.4f),
                style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
            )

            // Bottom Shadow for tube roundedness
            drawArc(
                color = Color.Black.copy(alpha = 0.3f),
                startAngle = 180f + offsetAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(arcTopLeft.x - strokeWidth * 0.1f, arcTopLeft.y - strokeWidth * 0.1f),
                size = Size(arcSize.width + strokeWidth * 0.2f, arcSize.height + strokeWidth * 0.2f),
                style = Stroke(width = strokeWidth * 0.2f, cap = StrokeCap.Round)
            )

            // Outer Glow (Neon effect) - Only in Dark Mode or very subtle in Light
            if (isDark) {
                drawArc(
                    color = cOrange.copy(alpha = 0.3f), 
                    startAngle = 180f + offsetAngle - 2f,
                    sweepAngle = sweepAngle + 4f,
                    useCenter = false,
                    topLeft = Offset(arcTopLeft.x - 4f, arcTopLeft.y - 4f),
                    size = Size(arcSize.width + 8f, arcSize.height + 8f),
                    style = Stroke(width = strokeWidth + 8f, cap = StrokeCap.Round),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // 4. Glass/Gloss Overlay (Reflections)
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(width / 2, 0f),
                end = Offset(width / 2, size.height / 2)
            ),
            startAngle = 180f + offsetAngle,
            sweepAngle = fullSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Sleek Needle
        rotate(degrees = targetAngle, pivot = Offset(width / 2, arcCenterY)) {
            val needleLength = outerRadius - 12.dp.toPx() // Visible gap from the rim

            // Tapered Needle Shape (Path)
            val needlePath = Path().apply {
                val centerX = width / 2
                val centerY = arcCenterY
                val baseHalfWidth = 6.dp.toPx() // Wide base
                
                moveTo(centerX - baseHalfWidth, centerY) // Bottom-Left
                lineTo(centerX, centerY - needleLength)  // Top-Center (Tip)
                lineTo(centerX + baseHalfWidth, centerY) // Bottom-Right
                close()
            }

            // 1. Needle Shadow (Drop Shadow for 3D Lift)
            drawPath(
                path = needlePath,
                color = Color.Black.copy(alpha = 0.4f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Fill shadow
            drawPath(
                path = needlePath,
                color = Color.Black.copy(alpha = 0.2f)
            )

            // 2. 3D Gradient Needle Body
            val needleBrush = Brush.horizontalGradient(
                colors = listOf(
                    needleGradientStart,       
                    customColors.neonCyan,          
                    needleGradientEnd
                ),
                startX = width / 2 - 6.dp.toPx(),
                endX = width / 2 + 6.dp.toPx()
            )

            // Draw the 3D tapered needle
            drawPath(
                path = needlePath,
                brush = needleBrush
            )

            // 2b. The "Tip Lights"
            drawCircle(
                color = customColors.neonCyan,
                radius = 1.5.dp.toPx(),
                center = Offset(width / 2, arcCenterY - needleLength)
            )
        }
        
        // 3. Pivot Point
        drawCircle(
            color = if (isDark) Color.Black else Color.DarkGray,
            radius = 8.dp.toPx(),
            center = Offset(width / 2, arcCenterY)
        )
    }
}

@Composable
private fun CurrencyIndicator(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val customColors = LocalPlatisaColors.current
    val color = if (isActive) customColors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val shadow = if (isActive) getShadow(customColors.neonCyan) else null
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = CyberpunkFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color,
                shadow = shadow
            )
        )
    }
}
