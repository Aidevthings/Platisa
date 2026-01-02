package com.example.platisa.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platisa.core.domain.model.BillCategory
import com.example.platisa.ui.theme.*
import java.math.BigDecimal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Composable
fun CategoryBreakdownChart(
    spendingByCategory: Map<BillCategory, BigDecimal>,
    totalSpending: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color.White else SolarTextPrimary
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else SolarTextSecondary

    Column(modifier = modifier) {
        // Header
        Text(
            text = "PO KATEGORIJAMA",
            style = MaterialTheme.typography.titleMedium,
            color = primaryTextColor, // Dark Text
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (spendingByCategory.isEmpty()) {
            Text(
                text = "Nema podataka",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor, // Dark Gray
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // Pie Chart
            val sortedCategories = spendingByCategory.entries.sortedByDescending { it.value }
            val total = totalSpending.toFloat()

            if (total > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
                        var startAngle = -90f
                        sortedCategories.forEach { (category, amount) ->
                            val sweepAngle = (amount.toFloat() / total) * 360f
                            drawArc(
                                color = category.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true // Pie chart style
                            )
                            startAngle += sweepAngle
                        }
                        
                        // Inner circle for Donut effect (Optional, keeps it cleaner/modern)
                        drawCircle(
                            color = Color(0xFFD7CCC8), // Matches card background
                            radius = size.minDimension / 2 * 0.5f 
                        )
                    }
                }
            }

            // List
            sortedCategories.forEach { (category, amount) ->
                val percentage = if (totalSpending > BigDecimal.ZERO) {
                    (amount.toDouble() / totalSpending.toDouble() * 100).toFloat()
                } else 0f
                
                CategoryBar(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    currency = currency,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(
    category: BillCategory,
    amount: BigDecimal,
    percentage: Float,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryTextColor = if (isDark) Color.White else SolarTextPrimary
    
    // Animated width
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_animation"
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... (rest of Row content up to Amount) ...
        // Category label with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(100.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = category.color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = primaryTextColor, // Dark Text
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Bar container
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Animated bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedPercentage / 100f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                category.color.copy(alpha = 0.8f),
                                category.color
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .neonGlow(category.color, radius = 4.dp, alpha = 0.5f)
            )
            
            // Percentage text
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = primaryTextColor, // Dark Text
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Amount
        Text(
            text = com.example.platisa.core.common.Formatters.formatCurrencyWithSuffix(amount, currency),
            style = MaterialTheme.typography.bodySmall,
            color = category.color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )
    }
}
