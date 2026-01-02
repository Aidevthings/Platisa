package com.example.platisa.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platisa.core.common.Formatters
import com.example.platisa.core.domain.model.EpsMonthData
import com.example.platisa.ui.theme.*
import kotlin.math.max

@Composable
fun EpsAnalyticsScreen(
    monthlyData: List<EpsMonthData>
) {
    val latestMonth = monthlyData.firstOrNull()
    val totalVt = latestMonth?.vtConsumption?.toFloat() ?: 0f
    val totalNt = latestMonth?.ntConsumption?.toFloat() ?: 0f
    val totalConsumption = totalVt + totalNt
    val totalCost = latestMonth?.totalAmount?.toFloat() ?: 0f

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.platisa.ui.components.AppBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "EPS STATISTIKA",
                style = MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color.Black,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (monthlyData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema EPS podataka",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                // Total Consumption Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xFFD7CCC8),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = ElectricYellow.copy(alpha=0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "UKUPNA POTROŠNJA",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${String.format("%.0f", totalConsumption)} kWh",
                            style = MaterialTheme.typography.headlineLarge,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // VT/NT Breakdown Bars
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xFFD7CCC8),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "RASPODELA TARIFA",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // VT Bar
                        TariffBar(
                            label = "Viša Tarifa (VT)",
                            value = totalVt,
                            maxValue = totalConsumption,
                            color = ElectricYellow, // Keep accents
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // NT Bar
                        TariffBar(
                            label = "Niža Tarifa (NT)",
                            value = totalNt,
                            maxValue = totalConsumption,
                            color = DeepCyan, // Keep accents
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cost Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = androidx.compose.ui.graphics.Color(0xFFD7CCC8),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "FINANSIJE",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ukupan Iznos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = androidx.compose.ui.graphics.Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = Formatters.formatCurrencyWithSuffix(latestMonth?.totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                color = androidx.compose.ui.graphics.Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TariffBar(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (maxValue > 0f) (value / maxValue * 100f) else 0f
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Black,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${String.format("%.0f", value)} kWh",
                style = MaterialTheme.typography.bodyMedium,
                color = color, // Keep colored value
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.8f),
                                color
                            )
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .neonGlow(color, radius = 4.dp, alpha = 0.5f)
            )
            
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Black, // Ensure visibility inside bar? Wait, if bar is dark/colored?
                // Usually progress bars are bright. `color` is ElectricYellow or DeepCyan.
                // Text might be better white if inside a filled bar, or black if outside.
                // The text is aligned Center.
                // If bar is 100%, it's full color.
                // ElectricYellow is bright, Black text is good. DeepCyan is darkish, White text is good.
                // Let's stick to Black for safety on bright colors, or maybe just offset it?
                // The previous code had `TextWhite`.
                // If background is `ElectricYellow`, `TextWhite` might be hard to read.
                // I will set it to `Color.Black` as it is generally safer on "Neon" colors in a Light Theme.
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}
