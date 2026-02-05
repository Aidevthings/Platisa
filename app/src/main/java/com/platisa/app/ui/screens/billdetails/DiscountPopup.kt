package com.platisa.app.ui.screens.billdetails

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.platisa.app.ui.theme.*
import com.platisa.app.core.domain.model.DiscountRow

/**
 * Electric-themed popup that appears when entering Bill Details.
 * Automatically adapts to dark/light theme with appropriate styling.
 */
@Composable
fun DiscountPopup(
    discountTable: List<DiscountRow>? = null,
    infostanDeadline: String? = null,
    epsDiscountThreshold: String? = null,
    onDismiss: () -> Unit,
    onRemind: () -> Unit
) {
    val customColors = LocalPlatisaColors.current
    val isDarkTheme = customColors.background == VoidBackground
    
    // Log for debugging
    android.util.Log.d("BillDetails", "🎨 DiscountPopup rendering, isDarkTheme=$isDarkTheme, rows=${discountTable?.size ?: 0}, infostanDeadline=$infostanDeadline")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        PlatisaTheme(darkTheme = isDarkTheme) {
            // Surface required to apply theme background/content colors correctly if strictly needed,
            // but here we just need the CompositionLocals (Density/Typography).
            if (isDarkTheme) {
                DarkThemePopupContent(
                    discountTable = discountTable, 
                    infostanDeadline = infostanDeadline, 
                    epsDiscountThreshold = epsDiscountThreshold,
                    onDismiss = onDismiss, 
                    onRemind = onRemind
                )
            } else {
                LightThemePopupContent(
                    discountTable = discountTable, 
                    infostanDeadline = infostanDeadline, 
                    epsDiscountThreshold = epsDiscountThreshold,
                    onDismiss = onDismiss, 
                    onRemind = onRemind
                )
            }
        }
    }
}

/**
 * Dark theme version - Electric neon blue glow effects
 */
@Composable
private fun DarkThemePopupContent(
    discountTable: List<DiscountRow>?, 
    infostanDeadline: String?, 
    epsDiscountThreshold: String?,
    onDismiss: () -> Unit, 
    onRemind: () -> Unit
) {
    val customColors = LocalPlatisaColors.current
    // Animated glow pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlphaAnimation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Main popup card
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {}) // Prevent dismiss when clicking card
        ) {
            // Outer glow effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .blur(20.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
            
            // Main card with electric border
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = glowAlpha),
                                DeepCyan.copy(alpha = 0.7f),
                                NeonPurple.copy(alpha = glowAlpha * 0.6f),
                                CyberCyan.copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = CyberCyan.copy(alpha = 0.4f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = VoidBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box {
                    // Close button (X) in top-right
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(
                                color = CardSurface,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = CyberCyan.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zatvori",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Content Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Discount grid with actual data OR Infostan message
                        if (infostanDeadline != null) {
                            InfostanMessageDark(infostanDeadline = infostanDeadline, glowAlpha = glowAlpha)
                        } else {
                            if (epsDiscountThreshold != null) {
                                val message = if (epsDiscountThreshold.contains("Uplatom", ignoreCase = true) || 
                                                 epsDiscountThreshold.contains("Уплатом", ignoreCase = true)) {
                                    epsDiscountThreshold
                                } else {
                                    "Popust važi samo uz uplatu celokupnog duga od $epsDiscountThreshold RSD"
                                }
                                
                                Text(
                                    text = message,
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            DiscountGridDark(discountTable = discountTable, glowAlpha = glowAlpha)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Two buttons stacked vertically
                        ElectricButtonDark(
                            text = "Dodaj podsetnik",
                            onClick = onRemind,
                            glowAlpha = glowAlpha,
                            isPrimary = true
                        )
                        
                        ElectricButtonDark(
                            text = "Zatvori",
                            onClick = onDismiss,
                            glowAlpha = glowAlpha,
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Light theme version - Soft shadows and muted colors
 */
@Composable
private fun LightThemePopupContent(
    discountTable: List<DiscountRow>?, 
    infostanDeadline: String?, 
    epsDiscountThreshold: String?,
    onDismiss: () -> Unit, 
    onRemind: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Main popup card
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {})
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = SolarSurface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box {
                // Close button (X) in top-right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(
                            color = SolarSurfaceVariant,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = DeepTeal.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zatvori",
                        tint = SolarTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Discount Grid or Infostan Message for Light Theme
                    if (infostanDeadline != null) {
                        InfostanMessageLight(infostanDeadline = infostanDeadline)
                    } else {
                        if (epsDiscountThreshold != null) {
                            val message = if (epsDiscountThreshold.contains("Uplatom", ignoreCase = true) || 
                                             epsDiscountThreshold.contains("Уплатом", ignoreCase = true)) {
                                epsDiscountThreshold
                            } else {
                                "Popust važi samo uz uplatu celokupnog duga od $epsDiscountThreshold RSD"
                            }
                            
                            Text(
                                text = message,
                                color = SolarTextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        DiscountGridLight(discountTable = discountTable)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Two buttons stacked vertically
                    LightThemeButton(
                        text = "Dodaj podsetnik",
                        onClick = onRemind,
                        isPrimary = true
                    )
                    
                    LightThemeButton(
                        text = "Zatvori",
                        onClick = onDismiss,
                        isPrimary = false
                    )
                }
            }
        }
    }
}

/**
 * Dark theme discount grid with actual data (3 columns: %, Deadline, Amount)
 */
@Composable
private fun DiscountGridDark(discountTable: List<DiscountRow>?, glowAlpha: Float) {
    val rows = discountTable ?: emptyList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = glowAlpha * 0.6f),
                        NeonPurple.copy(alpha = glowAlpha * 0.4f),
                        CyberCyan.copy(alpha = glowAlpha * 0.6f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscountCellDark(text = "Popust", isHeader = true, glowAlpha = glowAlpha, modifier = Modifier.weight(0.20f))
            DiscountCellDark(text = "Uslov", isHeader = true, glowAlpha = glowAlpha, modifier = Modifier.weight(0.53f))
            DiscountCellDark(text = "Iznos", isHeader = true, glowAlpha = glowAlpha, modifier = Modifier.weight(0.27f))
        }
        
        // Data rows
        if (rows.isEmpty()) {
            Text(
                text = "Nema podataka o popustima",
                color = CyberCyan.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiscountCellDark(text = row.percentage, isHeader = false, glowAlpha = glowAlpha, modifier = Modifier.weight(0.20f))
                    DiscountCellDark(text = row.deadline, isHeader = false, glowAlpha = glowAlpha, modifier = Modifier.weight(0.53f), isUslov = true)
                    DiscountCellDark(text = row.amount, isHeader = false, glowAlpha = glowAlpha, modifier = Modifier.weight(0.27f))
                }
            }
        }
    }
}

@Composable
private fun InfostanMessageDark(infostanDeadline: String, glowAlpha: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = glowAlpha * 0.6f),
                        NeonPurple.copy(alpha = glowAlpha * 0.4f),
                        CyberCyan.copy(alpha = glowAlpha * 0.6f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ovaj račun ima popust ako ga platite do:",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = infostanDeadline,
            color = CyberCyan,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DiscountCellDark(
    text: String, 
    isHeader: Boolean, 
    glowAlpha: Float, 
    modifier: Modifier = Modifier,
    isUslov: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isHeader) CyberCyan.copy(alpha = glowAlpha * 0.2f)
                else CardBorder.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = CyberCyan.copy(alpha = glowAlpha * 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp), // Less horizontal padding
        contentAlignment = Alignment.Center
    ) {
        if (isUslov) {
            SplitConditionText(
                text = text,
                color = if (isHeader) CyberCyan else Color.White,
                isHeader = isHeader
            )
        } else {
            com.platisa.app.ui.components.DynamicSizeText(
                text = text,
                color = if (isHeader) CyberCyan else Color.White,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                minFontSize = 10.sp,
                maxFontSize = if (isHeader) 14.sp else 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Light theme discount grid with actual data
 */
@Composable
private fun DiscountGridLight(discountTable: List<DiscountRow>?) {
    val rows = discountTable ?: emptyList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SolarSurfaceVariant)
            .border(
                width = 2.dp,
                color = DeepTeal.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscountCellLight(text = "Popust", isHeader = true, modifier = Modifier.weight(0.20f))
            DiscountCellLight(text = "Uslov", isHeader = true, modifier = Modifier.weight(0.53f))
            DiscountCellLight(text = "Iznos", isHeader = true, modifier = Modifier.weight(0.27f))
        }
        
        // Data rows
        if (rows.isEmpty()) {
            Text(
                text = "Nema podataka o popustima",
                color = SolarTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiscountCellLight(text = row.percentage, isHeader = false, modifier = Modifier.weight(0.20f))
                    DiscountCellLight(text = row.deadline, isHeader = false, modifier = Modifier.weight(0.53f), isUslov = true)
                    DiscountCellLight(text = row.amount, isHeader = false, modifier = Modifier.weight(0.27f))
                }
            }
        }
    }
}

@Composable
private fun InfostanMessageLight(infostanDeadline: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SolarSurfaceVariant)
            .border(
                width = 2.dp,
                color = DeepTeal.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ovaj račun ima popust ako ga platite do:",
            color = SolarTextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = infostanDeadline,
            color = DeepTeal,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DiscountCellLight(
    text: String, 
    isHeader: Boolean, 
    modifier: Modifier = Modifier,
    isUslov: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isHeader) DeepTeal.copy(alpha = 0.2f)
                else SolarSurface
            )
            .border(
                width = 1.dp,
                color = DeepTeal.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isUslov) {
            SplitConditionText(
                text = text,
                color = if (isHeader) DeepTeal else SolarTextPrimary,
                isHeader = isHeader
            )
        } else {
            com.platisa.app.ui.components.DynamicSizeText(
                text = text,
                color = if (isHeader) DeepTeal else SolarTextPrimary,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                minFontSize = 10.sp,
                maxFontSize = if (isHeader) 14.sp else 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Splits text into two lines: Date and secondary text.
 */
@Composable
private fun SplitConditionText(
    text: String,
    color: Color,
    isHeader: Boolean
) {
    if (isHeader) {
        com.platisa.app.ui.components.DynamicSizeText(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            minFontSize = 10.sp,
            maxFontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        return
    }

    // Attempt to split by date pattern (e.g., 28.01.2026.)
    val dateRegex = Regex("(\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\.?)")
    val match = dateRegex.find(text)
    
    if (match != null) {
        val datePart = match.value
        val restPart = text.replace(datePart, "").trim()
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            com.platisa.app.ui.components.DynamicSizeText(
                text = datePart,
                color = color,
                fontWeight = FontWeight.Normal,
                minFontSize = 11.sp,
                maxFontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (restPart.isNotEmpty()) {
                com.platisa.app.ui.components.DynamicSizeText(
                    text = restPart,
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal,
                    minFontSize = 9.sp,
                    maxFontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Fallback
        com.platisa.app.ui.components.DynamicSizeText(
            text = text,
            color = color,
            fontWeight = FontWeight.Normal,
            minFontSize = 10.sp,
            maxFontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Old dark theme electric grid (kept for reference, now unused)
 */
@Composable
private fun ElectricGridDark(glowAlpha: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f) // Makes it more square/expanded
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = glowAlpha * 0.6f),
                        NeonPurple.copy(alpha = glowAlpha * 0.4f),
                        CyberCyan.copy(alpha = glowAlpha * 0.6f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 3 rows
        repeat(3) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4 columns
                repeat(4) { colIndex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if ((rowIndex + colIndex) % 2 == 0) {
                                    CyberCyan.copy(alpha = glowAlpha * 0.15f)
                                } else {
                                    CardBorder.copy(alpha = 0.4f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = CyberCyan.copy(alpha = glowAlpha * 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Light theme grid table (4 columns x 3 rows)
 */
@Composable
private fun GridTableLight() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(SolarSurfaceVariant)
            .border(
                width = 2.dp,
                color = DeepTeal.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 3 rows
        repeat(3) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4 columns
                repeat(4) { colIndex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if ((rowIndex + colIndex) % 2 == 0) {
                                    DeepTeal.copy(alpha = 0.1f)
                                } else {
                                    SolarBorderBrown.copy(alpha = 0.15f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = DeepTeal.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Dark theme electric button
 */
@Composable
private fun ElectricButtonDark(
    text: String,
    onClick: () -> Unit,
    glowAlpha: Float,
    isPrimary: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (isPrimary) {
            // Glow behind primary button
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(12.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = glowAlpha * 0.4f),
                                DeepCyan.copy(alpha = glowAlpha * 0.3f),
                                CyberCyan.copy(alpha = glowAlpha * 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
        
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isPrimary) {
                            listOf(CyberCyan.copy(alpha = glowAlpha), DeepCyan, CyberCyan.copy(alpha = glowAlpha))
                        } else {
                            listOf(CardBorder.copy(alpha = 0.5f), CardBorder.copy(alpha = 0.5f))
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrimary) CyberCyan.copy(alpha = 0.15f) else CardSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) CyberCyan else TextSecondaryDark
            )
        }
    }
}

/**
 * Light theme button
 */
@Composable
private fun LightThemeButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (!isPrimary) {
                    Modifier.border(
                        width = 1.dp,
                        color = SolarBorderBrown.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) DeepTeal else SolarSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) Color.White else SolarTextPrimary
        )
    }
}
