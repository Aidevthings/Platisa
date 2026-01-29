package com.platisa.app.ui.screens.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.core.data.repository.TrialStatus
import com.platisa.app.ui.theme.NeonCyan
import com.platisa.app.ui.theme.VoidBackground
import com.platisa.app.ui.theme.MatrixGreen
import com.platisa.app.ui.theme.PlatisaTheme

@Composable
fun SubscriptionPaywallScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val status by viewModel.status.collectAsState()
    val daysRemaining by viewModel.daysRemaining.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val context = LocalContext.current
    
    // Theme Adaptation
    val isDark = PlatisaTheme.colors.isDark
    val textColor = if (isDark) Color.White else PlatisaTheme.colors.textPrimary
    val textLabelColor = if (isDark) Color.White.copy(alpha = 0.6f) else PlatisaTheme.colors.textLabel
    val cardBgColor = if (isDark) Color(0xFF1A1D24) else Color.White
    val screenBgColor = if (isDark) Color.Transparent else PlatisaTheme.colors.surfaceContainer

    Box(modifier = Modifier.fillMaxSize().background(screenBgColor)) {
        // Background Image (Only in Dark Mode)
        if (isDark) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.platisa.app.R.drawable.pozadina),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                text = "Pretplata",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f) // Take remaining space but allow bottom button
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- STATUS CARD ---
            if (status != null) {
                StatusCard(status = status!!, daysRemaining = daysRemaining.toInt(), isDark = isDark)
            } else {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(cardBgColor, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // --- UPGRADE SECTION ---
            Text(
                text = "Izaberite Plan",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Plan
            PlanCard(
                title = "Mesečno",
                price = "100 RSD",
                subtitle = "/ mesečno",
                isBestValue = false,
                isSelected = selectedPlan == "MONTHLY",
                isDark = isDark,
                onClick = { viewModel.selectPlan("MONTHLY") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Yearly Plan
            PlanCard(
                title = "Godišnje",
                price = "1000 RSD",
                subtitle = "/ godišnje",
                isBestValue = true,
                isSelected = selectedPlan == "YEARLY",
                isDark = isDark,
                badge = "UŠTEDA 17%",
                onClick = { viewModel.selectPlan("YEARLY") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // SUBSCRIBE BUTTON
            val priceText = if (selectedPlan == "YEARLY") "1000 RSD" else "100 RSD"
            Button(
                onClick = { 
                    context.findActivity()?.let { activity ->
                        viewModel.buySubscription(activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(if (isDark) 12.dp else 4.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Pretplati se - $priceText",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // --- MANAGE ACTIONS ---
            Divider(color = textColor.copy(alpha = 0.15f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                     // TODO: Call billingManager.queryPurchases() explicitly if needed
                     android.widget.Toast.makeText(context, "Osvežavanje statusa...", android.widget.Toast.LENGTH_SHORT).show()
                     viewModel.checkStatus()
                }
            ) {
                Text("Obnovi kupovinu", color = textLabelColor, fontWeight = FontWeight.Medium)
            }
            
            TextButton(
                onClick = {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                     context.startActivity(intent)
                }
            ) {
                Text("Upravljaj preko Google Play-a", color = textLabelColor, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Promo Code Button
            var showPromoDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            
            TextButton(
                onClick = { showPromoDialog = true }
            ) {
                Text("Unesi Promo Kod", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            
            if (showPromoDialog) {
                PromoCodeDialog(
                    onDismiss = { showPromoDialog = false },
                    onSubmit = { code -> 
                        val message = viewModel.applyPromoCode(code)
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        if (message.contains("Activated") || message.contains("produžen")) {
                             showPromoDialog = false
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

// Helper to find Activity from Context
fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun PromoCodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val isDark = PlatisaTheme.colors.isDark
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unesite Promo Kod", color = if (isDark) Color.White else PlatisaTheme.colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Kod") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlatisaTheme.colors.neonCyan,
                    unfocusedBorderColor = if (isDark) Color.Gray else PlatisaTheme.colors.textLabel,
                    focusedTextColor = if (isDark) Color.White else PlatisaTheme.colors.textPrimary,
                    unfocusedTextColor = if (isDark) Color.White.copy(alpha=0.8f) else PlatisaTheme.colors.textPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                         onSubmit(code)
                         // onDismiss() - Handled by parent if successful? Or close always?
                         // Let's keep dialog open if invalid?
                         // For simplicity, let parent decide when to close (or just close here)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Potvrdi", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži", color = if (isDark) Color.White else PlatisaTheme.colors.textPrimary)
            }
        },

        containerColor = if (isDark) Color(0xFF1A1D24) else Color.White,
        textContentColor = if (isDark) Color.White else PlatisaTheme.colors.textPrimary
    )
}


@Composable
fun StatusCard(
    status: TrialStatus, 
    daysRemaining: Int,
    isDark: Boolean
) {
    val (statusText, statusColor, subText) = when (status) {
        is TrialStatus.Active -> Triple("PREMIUM", Color(0xFF00FF87), "Preostalo dana: $daysRemaining")
        is TrialStatus.Expired -> Triple("ISTEKLO", PlatisaTheme.colors.error, "Nadogradite za nastavak")
        is TrialStatus.Error -> Triple("GREŠKA", PlatisaTheme.colors.error, (status as TrialStatus.Error).message)
    }

    // Gradient colors for the circle
    val gradientColors = listOf(
        Color(0xFF00FFFF), // Cyan
        Color(0xFF00FF87), // Green
        Color(0xFF87FF00), // Lime
        Color(0xFF00FF87), // Green
        Color(0xFF00FFFF)  // Cyan
    )
    
    val containerColor = if (isDark) Color(0xFF1A1D24).copy(alpha = 0.95f) else Color.White
    val textColor = if (isDark) Color.White else PlatisaTheme.colors.textPrimary
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.8f) else PlatisaTheme.colors.textLabel

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color(0xFF00FFFF).copy(alpha = 0.3f), Color(0xFF00FF87).copy(alpha = 0.3f))),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(if (isDark) 0.dp else 4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha=0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "VAŠ PLAN",
                    color = subTextColor.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subText,
                    color = subTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Premium Gradient Circle with Days Counter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                // Outer glow effect
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = if (isDark) 0.3f else 0.15f),
                                    statusColor.copy(alpha = if (isDark) 0.1f else 0.05f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Background track circle
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .border(
                            width = 4.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                )
                
                // Gradient progress arc using Canvas
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(95.dp)
                ) {
                    val strokeWidth = 6.dp.toPx()
                    val sweepAngle = (daysRemaining / 90f).coerceIn(0f, 1f) * 360f
                    
                    drawArc(
                        brush = Brush.sweepGradient(gradientColors),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
                
                // Inner circle with gradient background
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF2A2D34), Color(0xFF1A1D24))
                                } else {
                                    listOf(Color(0xFFF0F0F0), Color(0xFFFFFFFF))
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$daysRemaining",
                            color = textColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "dana",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    isBestValue: Boolean,
    isSelected: Boolean,
    isDark: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val gradientBorder = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(com.platisa.app.ui.theme.NeonCyan, com.platisa.app.ui.theme.NeonCyan)
        )
    } else if (isBestValue) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF00FFFF), Color(0xFF00FF87), Color(0xFF87FF00))
        )
    } else {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f))
            } else {
                 listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f))
            }
        )
    }
    
    val bgColor = if (isSelected) com.platisa.app.ui.theme.NeonCyan.copy(alpha = 0.15f) 
                  else if (isBestValue) Color(0xFF00FF87).copy(alpha = 0.08f) 
                  else if (isDark) Color(0xFF1A1D24).copy(alpha = 0.8f)
                  else Color.White

    val textColor = if (isDark) Color.White else PlatisaTheme.colors.textPrimary
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.6f) else PlatisaTheme.colors.textLabel

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 3.dp else if (isBestValue) 2.dp else 1.dp,
                brush = gradientBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .then(if (!isDark && !isSelected && !isBestValue) Modifier.shadow(2.dp, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                if (badge != null) {
                    Text(
                        text = badge,
                        color = if (isDark) Color(0xFF00FF87) else Color(0xFF00008B), // Dark Blue in Light Mode
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0xFF00FF87).copy(alpha = 0.2f), Color(0xFF00FFFF).copy(alpha = 0.2f))
                                    } else {
                                        listOf(Color(0xFF00008B).copy(alpha = 0.1f), Color(0xFF4169E1).copy(alpha = 0.1f)) // Lighter blue background
                                    }
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 13.sp
                )
                
                if (isSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = com.platisa.app.ui.theme.NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

