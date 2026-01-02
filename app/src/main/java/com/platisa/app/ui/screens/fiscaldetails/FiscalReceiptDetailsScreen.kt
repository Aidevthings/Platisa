package com.platisa.app.ui.screens.fiscaldetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.model.ReceiptItem
import com.platisa.app.ui.theme.LocalPlatisaColors
import java.text.SimpleDateFormat
import java.util.Locale

// Neon colors
private val BackgroundDark = Color(0xFF111217)
private val NeonCyan = Color(0xFF00EAFF)
private val NeonMagenta = Color(0xFFFF00D9)
private val CardBg = Color(0xFF1A1A2E)
private val Gray400 = Color(0xFF9CA3AF)
private val HtmlNeonGreen = Color(0xFF39FF14)

@Composable
fun FiscalReceiptDetailsScreen(
    navController: NavController,
    receiptId: String,
    viewModel: FiscalReceiptDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val items by viewModel.items.collectAsState()
    
    LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (LocalPlatisaColors.current.isDark) Color.Transparent else Color(0xFFF5F5F5))
    ) {
        if (LocalPlatisaColors.current.isDark) {
            com.platisa.app.ui.components.AppBackground()
        }
        
        when (val currentState = state) {
            is FiscalReceiptState.Loading -> {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is FiscalReceiptState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Greška", color = NeonMagenta, fontSize = 20.sp)
                    Text(currentState.message, color = Color.White)
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Nazad")
                    }
                }
            }
            is FiscalReceiptState.Success -> {
                FiscalReceiptContent(
                    navController = navController,
                    receipt = currentState.receipt,
                    items = items
                )
            }
        }
    }
}

@Composable
fun FiscalReceiptContent(
    navController: NavController,
    receipt: Receipt,
    items: List<ReceiptItem>
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("sr", "RS"))
    
    // Theme Colors
    val isDark = LocalPlatisaColors.current.isDark
    
    // Header Colors
    val headerTextColor = if (isDark) Color.White else Color.Black
    val backButtonBg = if (isDark) Color(0xFF1F2937) else Color.White
    val backButtonIconTint = if (isDark) Color.White else Color.Black
    val backButtonBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    
    // Card Colors
    val cardBgColor = if (isDark) CardBg else Color.White
    val mainTextColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Gray400 else Color.Gray
    val accentColor = if (isDark) NeonCyan else Color(0xFF00ACC1) // Darker Cyan for Light Mode
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    val totalLabelColor = if (isDark) HtmlNeonGreen else Color(0xFF4CAF50) // Material Green for Light
    val totalValueColor = if (isDark) HtmlNeonGreen else Color(0xFF4CAF50)
    
    // Table Colors
    val tableHeaderBg = if (isDark) Color(0xFF252540) else Color(0xFFE0E0E0)
    val tableHeaderTextColor = if (isDark) NeonCyan else Color.Black
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header with back button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(backButtonBg)
                        .border(1.dp, backButtonBorder, CircleShape)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Nazad",
                        tint = backButtonIconTint
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Fiskalni Račun",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                )
            }
        }
        
        // Receipt Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Store name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = receipt.merchantName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor
                        )
                    }
                    
                    Divider(color = dividerColor)
                    
                    // Date and Invoice
                    val dateOnly = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
                    val timeOnly = SimpleDateFormat("HH:mm", Locale("sr", "RS"))
                    
                    // Date and Invoice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date & Time Column
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Datum i vreme", color = secondaryTextColor, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                dateOnly.format(receipt.date),
                                color = mainTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                timeOnly.format(receipt.date),
                                color = accentColor,
                                fontSize = 14.sp
                            )
                        }
                        
                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(dividerColor)
                        )
                        
                        // Invoice Column
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Broj računa", color = secondaryTextColor, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                receipt.invoiceNumber ?: "-",
                                color = mainTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    
                    Divider(color = dividerColor)
                    
                    // Total Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "UKUPNO",
                            color = totalLabelColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            com.platisa.app.core.common.Formatters.formatCurrency(receipt.totalAmount),
                            color = totalValueColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Items Header
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STAVKE RAČUNA",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${items.size} stavki",
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(tableHeaderBg, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Proizvod",
                    color = tableHeaderTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2.4f)
                )
                Text(
                    "Kol.",
                    color = tableHeaderTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    "Ukupno",
                    color = tableHeaderTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
        
        // Items List
        items(items) { item ->
            ItemRow(
                item = item,
                isDark = isDark,
                bgColor = cardBgColor,
                textColor = mainTextColor,
                secondaryColor = secondaryTextColor,
                accentColor = totalValueColor,
                dividerColor = dividerColor
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ItemRow(
    item: ReceiptItem,
    isDark: Boolean,
    bgColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    dividerColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean up item name (remove /KG/12345 suffix)
        val cleanName = item.name
            .replace(Regex("/[a-zA-Z]+/[0-9]+$"), "") // Removes /KG/0080135
            .replace(Regex("/[0-9]+$"), "")           // Removes /12345 if unit matches failed
            
        Text(
            cleanName,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2.4f)
        )
        // Determine unit based on heuristics
        val nameLower = item.name.lowercase()
        val isFuel = nameLower.contains("benzin") || nameLower.contains("dizel") || nameLower.contains("tng")
        val qtyStripped = item.quantity.stripTrailingZeros()
        val isInteger = qtyStripped.scale() <= 0
        
        val unit = when {
            isFuel -> " l"
            !isInteger -> " kg"
            else -> " kom"
        }

        Text(
            item.quantity.setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + unit,
            color = secondaryColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
        )
        // Unit price column removed as requested
        
        // Format large numbers with dots/spaces if needed, or just standard integer
        val totalInt = item.total?.toInt() ?: 0
        // Use standard number format for thousands separator if desired, or just raw string if simpler.
        // Serbian locale usually uses dots for thousands.
        val formattedTotal = java.text.NumberFormat.getIntegerInstance(java.util.Locale("sr", "RS")).format(totalInt)
        
        Text(
            if (item.total != null) formattedTotal else "-",
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.0f)
        )
    }

    
    // Divider
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = dividerColor
    )
}

