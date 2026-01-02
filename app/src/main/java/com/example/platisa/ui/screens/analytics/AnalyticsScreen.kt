package com.example.platisa.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.platisa.core.common.BaseScreen
import com.example.platisa.ui.components.PlatisaButton
import com.example.platisa.ui.navigation.Screen
import com.example.platisa.ui.theme.*

@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val totalSpending by viewModel.totalSpending.collectAsState()
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    BaseScreen(viewModel = viewModel) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.platisa.ui.components.AppBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "STATISTIKA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = androidx.compose.ui.graphics.Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                
                // Total Spending Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f))
                        .background(
                            color = com.example.platisa.ui.theme.SolarSurface, // Very Light
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(CyberCyan.copy(alpha=0.5f), NeonPurple.copy(alpha=0.5f))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    // Currency Toggle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // RSD Button (Top Left)
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.setCurrency("RSD") },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "RSD",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (currency == "RSD") androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                fontWeight = if (currency == "RSD") FontWeight.Black else FontWeight.Normal
                            )
                        }

                        // EUR Button (Top Right)
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.setCurrency("EUR") },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "EUR",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (currency == "EUR") androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                fontWeight = if (currency == "EUR") FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp).padding(bottom = 8.dp)
                        )
                        Text(
                            text = "UKUPNA POTROŠNJA",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f), // Darker gray
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = com.example.platisa.core.common.Formatters.formatCurrencyWithSuffix(totalSpending, currency),
                            style = MaterialTheme.typography.displaySmall, // Much bigger
                            color = androidx.compose.ui.graphics.Color.Black, // Dark text
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category Breakdown Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
                        .background(
                            color = com.example.platisa.ui.theme.SolarBorderBrown.copy(alpha=0.5f), // Original Taupe equivalent
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    CategoryBreakdownChart(
                        spendingByCategory = spendingByCategory,
                        totalSpending = totalSpending,
                        currency = currency
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Top Merchants List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
                        .background(
                            color = com.example.platisa.ui.theme.SolarBorderBrown, // Darker/Richer
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    TopMerchantsList(
                        topMerchants = topMerchants,
                        currency = currency
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // EPS Analytics Button
                PlatisaButton(
                    text = "EPS Statistika",
                    onClick = { navController.navigate(Screen.EpsAnalytics.route) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
