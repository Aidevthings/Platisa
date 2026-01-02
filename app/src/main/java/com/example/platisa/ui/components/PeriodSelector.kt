package com.example.platisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platisa.ui.screens.analytics.GraphPeriod
import com.example.platisa.ui.screens.analytics.TimePeriod

@Composable
fun TimePeriodDropdownSelector(
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    availablePeriods: List<TimePeriod> = listOf(
        TimePeriod.MONTHLY,
        TimePeriod.SIX_MONTHS,
        TimePeriod.YEARLY
    )
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = when(selectedPeriod) {
                    TimePeriod.MONTHLY -> "MESEČNO"
                    TimePeriod.SIX_MONTHS -> "6 MESECI"
                    TimePeriod.YEARLY -> "GODIŠNJE"
                },
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            availablePeriods.forEach { period ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = when(period) {
                                TimePeriod.MONTHLY -> "Mesečno"
                                TimePeriod.SIX_MONTHS -> "6 Meseci"
                                TimePeriod.YEARLY -> "Godišnje"
                            }, 
                            color = Color.Black
                        ) 
                    },
                    onClick = { 
                        onPeriodChange(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PeriodDropdownSelector(
    selectedPeriod: GraphPeriod,
    onPeriodChange: (GraphPeriod) -> Unit,
    availablePeriods: List<GraphPeriod> = listOf(
        GraphPeriod.MONTHLY,
        GraphPeriod.SIX_MONTHS,
        GraphPeriod.THIS_YEAR
    )
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = when(selectedPeriod) {
                    GraphPeriod.MONTHLY -> "MESEČNO"
                    GraphPeriod.SIX_MONTHS -> "6 MESECI"
                    GraphPeriod.THIS_YEAR -> "OVA GODINA"
                },
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            availablePeriods.forEach { period ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = when(period) {
                                GraphPeriod.MONTHLY -> "Mesečno"
                                GraphPeriod.SIX_MONTHS -> "6 Meseci"
                                GraphPeriod.THIS_YEAR -> "Ova Godina"
                            }, 
                            color = Color.Black
                        ) 
                    },
                    onClick = { 
                        onPeriodChange(period)
                        expanded = false
                    }
                )
            }
        }
    }
}
