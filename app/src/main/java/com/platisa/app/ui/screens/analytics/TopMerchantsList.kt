package com.platisa.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.platisa.app.ui.theme.*
import java.math.BigDecimal

@Composable
fun TopMerchantsList(
    topMerchants: List<Pair<String, BigDecimal>>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Text(
            text = "TOP PRIMAOCI",
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color.Black,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (topMerchants.isEmpty()) {
            Text(
                text = "Nema podataka",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            topMerchants.forEachIndexed { index, (merchantName, amount) ->
                MerchantItem(
                    rank = index + 1,
                    merchantName = merchantName,
                    amount = amount,
                    currency = currency,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MerchantItem(
    rank: Int,
    merchantName: String,
    amount: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = when(rank) {
                        1 -> ElectricYellow.copy(alpha = 0.2f)
                        2 -> DeepCyan.copy(alpha = 0.2f)
                        3 -> NeonPurple.copy(alpha = 0.2f)
                        else -> TextSecondary.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelMedium,
                color = when(rank) {
                    1 -> ElectricYellow
                    2 -> DeepCyan
                    3 -> NeonPurple
                    else -> TextSecondary
                },
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Merchant name
        Text(
            text = merchantName,
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.Black,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Amount
        Text(
            text = com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(amount, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.Black,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

