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
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.platisa.app.R.drawable.pozadina),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Removed VoidBackground to show image
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
                    tint = PlatisaTheme.colors.textPrimary
                )
            }
            Text(
                text = "Pretplata",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- STATUS CARD ---
            if (status != null) {
                StatusCard(status = status!!, daysRemaining = daysRemaining.toInt())
            } else {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF1A1D24), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // --- UPGRADE SECTION ---
            Text(
                text = "Izaberite Plan",
                color = PlatisaTheme.colors.textPrimary,
                fontSize = 18.sp,
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
                onClick = { 
                    android.widget.Toast.makeText(context, "Coming Soon: Google Play Billing", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Yearly Plan
            PlanCard(
                title = "Godišnje",
                price = "1000 RSD",
                subtitle = "/ godišnje",
                isBestValue = true,
                badge = "UŠTEDA 17%",
                onClick = {
                    android.widget.Toast.makeText(context, "Coming Soon: Google Play Billing", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // --- MANAGE ACTIONS ---
            Divider(color = PlatisaTheme.colors.textPrimary.copy(alpha = 0.1f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                     android.widget.Toast.makeText(context, "Checking for past purchases...", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Obnovi kupovinu", color = PlatisaTheme.colors.textLabel)
            }
            
            TextButton(
                onClick = {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                     context.startActivity(intent)
                }
            ) {
                Text("Upravljaj preko Google Play-a", color = PlatisaTheme.colors.textLabel)
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

@Composable
fun PromoCodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unesite Promo Kod", color = PlatisaTheme.colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Kod") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlatisaTheme.colors.neonCyan,
                    unfocusedBorderColor = PlatisaTheme.colors.textLabel,
                    focusedTextColor = PlatisaTheme.colors.textPrimary,
                    unfocusedTextColor = PlatisaTheme.colors.textPrimary
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
                Text("Potvrdi", color = PlatisaTheme.colors.textOnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži", color = PlatisaTheme.colors.textLabel)
            }
        },

        containerColor = PlatisaTheme.colors.surfaceContainer,
        textContentColor = PlatisaTheme.colors.textPrimary
    )
}


@Composable
fun StatusCard(
    status: TrialStatus, 
    daysRemaining: Int
) {
    val (statusText, statusColor, subText) = when (status) {
        is TrialStatus.Active -> Triple("PREMIUM", PlatisaTheme.colors.success, "Preostalo dana: $daysRemaining") // Or specific success color
        is TrialStatus.Expired -> Triple("EXPIRED", PlatisaTheme.colors.error, "Nadogradite za nastavak")
        is TrialStatus.Error -> Triple("ERROR", PlatisaTheme.colors.error, (status as TrialStatus.Error).message)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "VAŠ PLAN",
                    color = PlatisaTheme.colors.textLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subText,
                    color = PlatisaTheme.colors.textLabel,
                    fontSize = 14.sp
                )
            }
            
            // Circular Progress (Visual only for now)
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(90.dp),
                    color = PlatisaTheme.colors.textLabel.copy(alpha = 0.2f),
                    trackColor = Color.Transparent,
                )
                CircularProgressIndicator(
                    progress = (daysRemaining / 90f).coerceIn(0f, 1f),
                    modifier = Modifier.size(90.dp),
                    color = statusColor,
                    trackColor = Color.Transparent,
                )
                com.platisa.app.ui.components.DynamicSizeText(
                    text = "$daysRemaining",
                    color = PlatisaTheme.colors.textPrimary,
                    minFontSize = 14.sp,
                    maxFontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp) // padding to keep inside circle
                )
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
    badge: String? = null,
    onClick: () -> Unit
) {
    val borderColor = if (isBestValue) MatrixGreen else PlatisaTheme.colors.textPrimary.copy(alpha = 0.1f)
    val bgColor = if (isBestValue) MatrixGreen.copy(alpha = 0.05f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
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
                        color = MatrixGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(MatrixGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = title,
                    color = PlatisaTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    color = PlatisaTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = PlatisaTheme.colors.textPrimary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

