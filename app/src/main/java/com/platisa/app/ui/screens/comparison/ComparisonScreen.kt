package com.platisa.app.ui.screens.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.core.common.Formatters
import com.platisa.app.core.domain.model.ReceiptItem
import com.platisa.app.core.domain.model.BillCategorizer
import com.platisa.app.core.domain.model.BillCategory
import com.platisa.app.ui.theme.CardBorder
import com.platisa.app.ui.theme.CardSurface
import com.platisa.app.ui.theme.CyberCyan
import com.platisa.app.ui.theme.LocalPlatisaColors
import com.platisa.app.ui.theme.NeonPurple
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import java.text.SimpleDateFormat
import java.util.Locale

// Define Chill Green
private val ChillGreen = Color(0xFF4CAF50)
private val DarkChillGreen = Color(0xFF388E3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    navController: NavController,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val fiscalReceipts by viewModel.fiscalReceipts.collectAsState()
    
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
    val localFocusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.platisa.app.ui.components.AppBackground()
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("Pretraži i Uštedi", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (searchQuery.isNotBlank()) {
                            viewModel.onSearchQueryChanged("")
                        } else {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        
        // Search Bar (Keeps Neon Green)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Pretraži proizvod (npr. mleko, hleb)...", color = Color.DarkGray) },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = ChillGreen,
                    modifier = Modifier.size(48.dp)
                ) 
            },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ChillGreen,
                unfocusedBorderColor = ChillGreen,
                cursorColor = Color.Black
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                localFocusManager.clearFocus()
            })
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // SEARCH RESULTS MODE
            if (searchQuery.isNotBlank()) {
                 item {
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color(0xFF252540))
                            .padding(12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text("Prodavnica", color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                        Text("Cena", color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                 }
                 
                 items(searchResults) { result ->
                     SearchResultRow(result)
                     Spacer(modifier = Modifier.height(8.dp))
                 }
                 
                 if (searchResults.isEmpty()) {
                     item {
                         Text(
                             "Nema rezultata.",
                             color = Color.DarkGray,
                             modifier = Modifier.padding(16.dp)
                         )
                     }
                 }
            } else {
                // NORMAL MODE (Fiscal Receipts)
                if (fiscalReceipts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Moji Računi (${fiscalReceipts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    
                    // Group by Category
                    val groupedReceipts = fiscalReceipts.groupBy { it.category }
                    
                    // Sort categories by amount
                    val sortedCategories = groupedReceipts.keys.sortedByDescending { category -> 
                        groupedReceipts[category]?.sumOf { it.totalAmount } ?: java.math.BigDecimal.ZERO
                    }
                    
                    items(sortedCategories) { category ->
                         val categoryReceipts = groupedReceipts[category] ?: emptyList()
                         MarketCategoryDropdown(category, categoryReceipts, dateFormat, navController)
                         Spacer(modifier = Modifier.height(12.dp))
                    }
                    


                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Nema sačuvanih računa.\nSkeniraj QR kod računa kamerom!", 
                                color = Color.DarkGray,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiscalReceiptCard(
    receipt: com.platisa.app.core.domain.model.Receipt,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val isDark = LocalPlatisaColors.current.isDark
    val containerColor = if (isDark) CardSurface else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    
    // Get Category Color
    val categoryColor = receipt.category.color

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(receipt.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = Formatters.formatCurrency(receipt.totalAmount),
                style = MaterialTheme.typography.titleLarge,
                color = categoryColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ComparisonItemCard(item: ReceiptItem) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
    val isDark = LocalPlatisaColors.current.isDark
    val containerColor = if (isDark) CardSurface else Color.White
    val contentColor = if (isDark) Color.White else Color.Black

    val category = BillCategorizer.categorize(item.merchantName ?: "")
    val categoryColor = category.color

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Merchant & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.merchantName ?: "Nepoznato",
                    style = MaterialTheme.typography.labelLarge,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.date?.let { dateFormat.format(it) } ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Product Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jedinična cena:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = Formatters.formatCurrency(item.unitPrice),
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkChillGreen, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Količina: ${Formatters.formatAmount(item.quantity)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Ukupno: ${Formatters.formatCurrency(item.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(item: com.platisa.app.core.domain.model.ProductSearchResult) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
    val isDark = LocalPlatisaColors.current.isDark
    val containerColor = if (isDark) CardSurface else Color.White
    val contentColor = if (isDark) Color.White else Color.Black

    val category = BillCategorizer.categorize(item.merchantName)
    val categoryColor = category.color
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(item.merchantName, color = contentColor, fontWeight = FontWeight.Bold)
                Text(item.name, color = Color.Gray, fontSize = 12.sp)
                Text(dateFormat.format(item.date), color = Color.Gray, fontSize = 12.sp)
            }
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                     Formatters.formatCurrency(item.unitPrice),
                     color = categoryColor, 
                     fontWeight = FontWeight.Bold,
                     fontSize = 16.sp,
                     textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
fun MarketCategoryDropdown(
    category: BillCategory, 
    receipts: List<com.platisa.app.core.domain.model.Receipt>,
    dateFormat: SimpleDateFormat,
    navController: NavController
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDark = LocalPlatisaColors.current.isDark
    
    // Theme Colors
    val cardBg = if (isDark) CardSurface else Color(0xFFF5F5F5) // Light Gray for light mode container
    val borderColor = if (isDark) category.color.copy(alpha = 0.3f) else Color.Transparent
    
    // Header styling
    val headerBg = if (isDark) {
         Brush.horizontalGradient(listOf(category.color.copy(alpha = 0.1f), Color.Transparent))
    } else {
         Brush.horizontalGradient(listOf(Color.White, Color.White))
    }
    
    val headerTextColor = if (isDark) Color.White else Color.Black
    val countTextColor = if (isDark) Color.Gray else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .background(headerBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             // Icon Box
             Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(category.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .border(1.dp, category.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium, // Larger Title
                    color = headerTextColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${receipts.size} Transakcija",
                    style = MaterialTheme.typography.bodySmall,
                    color = countTextColor
                )
            }
            
            // Total Amount for Category
            val totalAmount = receipts.sumOf { it.totalAmount }
            
            Column(horizontalAlignment = Alignment.End) {
                 Text(
                    text = Formatters.formatCurrency(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = headerTextColor, 
                    fontWeight = FontWeight.Bold
                )
                 Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = countTextColor
                )
            }
        }
        
        // Body (List of Receipts)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp) 
            ) {
                 // Add Divider like in StatisticsScreen
                 androidx.compose.material3.Divider(color = borderColor, thickness = 1.dp)
                 Spacer(modifier = Modifier.height(8.dp))

                receipts.forEach { receipt ->
                     CompactFiscalReceiptRow(
                        receipt = receipt,
                        onClick = {
                            navController.navigate(com.platisa.app.ui.navigation.Screen.FiscalReceiptDetails.createRoute(receipt.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactFiscalReceiptRow(
    receipt: com.platisa.app.core.domain.model.Receipt,
    onClick: () -> Unit
) {
    val isDark = LocalPlatisaColors.current.isDark
    // Match colors from StatisticsScreen (CategoryDetailCard)
    // Merchant: White (alpha 0.8) if dark, else Black/DarkGray
    val mainTextColor = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black
    
    // Amount: Green (0xFF4CAF50) - ChillGreen
    val amountColor = Color(0xFF4CAF50)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = receipt.merchantName,
            fontSize = 14.sp,
            color = mainTextColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            text = Formatters.formatCurrency(receipt.totalAmount),
            fontSize = 14.sp,
            color = amountColor,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}


