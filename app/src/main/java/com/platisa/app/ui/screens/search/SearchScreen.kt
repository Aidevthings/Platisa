package com.platisa.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.core.common.BaseScreen
import com.platisa.app.ui.components.PlatisaCard
import com.platisa.app.ui.components.PlatisaInput
import com.platisa.app.ui.theme.NeonCyan
import com.platisa.app.ui.theme.NeonPurple
import com.platisa.app.ui.theme.White
import com.platisa.app.core.domain.model.ProductSearchResult
import com.platisa.app.core.common.Formatters
import com.platisa.app.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import com.platisa.app.ui.theme.CyberCyan
import com.platisa.app.ui.theme.AlertRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    
    var showFilters by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDateRangeChanged(
                        datePickerState.selectedStartDateMillis,
                        datePickerState.selectedEndDateMillis
                    )
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }

    BaseScreen(viewModel = viewModel) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.platisa.app.ui.components.AppBackground()
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Search Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatisaInput(
                        value = query,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = "Pretraži prodavca...",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filteri", tint = NeonCyan)
                    }
                }

                // Filters Section
                if (showFilters) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Filteri", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                            
                            // Date Filter
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clickable { showDatePicker = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (startDate != null && endDate != null) {
                                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                        "${sdf.format(Date(startDate!!))} - ${sdf.format(Date(endDate!!))}"
                                    } else "Izaberi period",
                                    color = White
                                )
                            }
                            
                            // Amount Filter (Simplified for now)
                            // Could add RangeSlider here later
                        }
                    }
                }
                
                // Results List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = results,
                        key = { item -> 
                            when(item) {
                                is SearchUiItem.ReceiptItem -> "receipt_${item.receipt.id}"
                                is SearchUiItem.ProductItem -> "product_${item.product.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is SearchUiItem.ReceiptItem -> {
                                val receipt = item.receipt
                                PlatisaCard(onClick = {
                                    when (receipt.category) {
                                        com.platisa.app.core.domain.model.BillCategory.GROCERY,
                                        com.platisa.app.core.domain.model.BillCategory.PHARMACY,
                                        com.platisa.app.core.domain.model.BillCategory.RESTAURANT -> {
                                            navController.navigate(com.platisa.app.ui.navigation.Screen.FiscalReceiptDetails.createRoute(receipt.id))
                                        }
                                        else -> {
                                            navController.navigate(com.platisa.app.ui.navigation.Screen.BillDetails.createRoute(receipt.id.toString()))
                                        }
                                    }
                                }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = receipt.merchantName, color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(receipt.date),
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(receipt.totalAmount, receipt.currency ?: "RSD"),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (receipt.totalAmount != null) CyberCyan else AlertRed,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            is SearchUiItem.ProductItem -> {
                                val product = item.product
                                PlatisaCard(onClick = {
                                    // Navigate to the parent receipt
                                    navController.navigate(com.platisa.app.ui.navigation.Screen.FiscalReceiptDetails.createRoute(product.id))
                                }) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Row 1: Name and UNIT PRICE (The User's Goal!)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.name,
                                                color = White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = com.platisa.app.core.common.Formatters.formatCurrency(product.unitPrice),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = NeonPurple, // Highlight Unit Price
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "po jed.", // "per unit"
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Row 2: Details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = product.merchantName,
                                                    color = NeonCyan,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(product.date),
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            
                                            // Quantity and Total
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "x ${product.quantity}",
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = "Total: " + com.platisa.app.core.common.Formatters.formatCurrency(product.total),
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

