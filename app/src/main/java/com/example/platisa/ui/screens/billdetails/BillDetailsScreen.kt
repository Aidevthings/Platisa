package com.example.platisa.ui.screens.billdetails

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.platisa.core.common.QrCodeGenerator
import com.example.platisa.core.domain.model.Receipt
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.platisa.ui.theme.LocalPlatisaColors
import androidx.compose.material3.MaterialTheme

@Composable
fun BillDetailsScreen(
    navController: NavController,
    billId: String,
    viewModel: BillDetailsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val billDetailsState by viewModel.billDetails.collectAsState()
    val saveQrStatus by viewModel.saveQrStatus.collectAsState()
    val receiptItems by viewModel.receiptItems.collectAsState()
    val customColors = LocalPlatisaColors.current
    
    // Load bill details on launch
    LaunchedEffect(billId) {
        viewModel.loadBillDetails(billId)
    }
    
    // Show snackbar on save success
    LaunchedEffect(saveQrStatus) {
        when (saveQrStatus) {
            is SaveQrStatus.Success -> {
                android.util.Log.d("BillDetails", "QR code saved successfully")
                viewModel.resetSaveQrStatus()
            }
            is SaveQrStatus.Error -> {
                android.util.Log.e("BillDetails", "Error: ${(saveQrStatus as SaveQrStatus.Error).message}")
                viewModel.resetSaveQrStatus()
            }
            else -> {}
        }
    }
    
    when (val state = billDetailsState) {
        is BillDetailsState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.example.platisa.ui.components.AppBackground()
                CircularProgressIndicator(color = customColors.neonCyan)
            }
        }
        is BillDetailsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.example.platisa.ui.components.AppBackground()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Greška",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.statusUnpaid
                    )
                    Text(
                        text = state.message,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customColors.neonCyan.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Nazad", color = customColors.neonCyan)
                    }
                }
            }
        }
        is BillDetailsState.Success -> {
            BillDetailsContent(
                navController = navController,
                receipt = state.receipt,
                billType = state.billType,
                vtConsumption = state.vtConsumption,
                ntConsumption = state.ntConsumption,
                receiptItems = receiptItems,
                onSaveQr = { viewModel.saveQrCode() },
                onMarkPaid = { viewModel.markAsPaid() },
                isSaving = saveQrStatus is SaveQrStatus.Saving
            )
        }
    }
}

@Composable
fun BillDetailsContent(
    navController: NavController,
    receipt: Receipt,
    billType: BillType,
    vtConsumption: Int,
    ntConsumption: Int,
    receiptItems: List<com.example.platisa.core.domain.model.ReceiptItem>,
    onSaveQr: () -> Unit,
    onMarkPaid: () -> Unit,
    isSaving: Boolean
) {
    val customColors = LocalPlatisaColors.current
    // Extract QR code URL from receipt
    val qrCodeUrl = receipt.qrCodeData ?: ""
    
    // Debug logging
    LaunchedEffect(qrCodeUrl) {
        android.util.Log.d("BillDetails", "QR Code URL: $qrCodeUrl")
        android.util.Log.d("BillDetails", "QR Code exists: ${qrCodeUrl.isNotEmpty()}")
    }
    
    // Format amount
    val formattedAmount = com.example.platisa.core.common.Formatters.formatCurrency(
        receipt.totalAmount ?: java.math.BigDecimal.ZERO
    )
    
    // Format date
    // Format date
    val formattedDate = SimpleDateFormat("dd. MMMM yyyy", Locale("sr", "Latn", "RS")).format(receipt.date)
    val formattedDueDate = receipt.dueDate?.let { 
        SimpleDateFormat("dd. MMMM yyyy", Locale("sr", "Latn", "RS")).format(it)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        com.example.platisa.ui.components.AppBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            TopNavigationBar(onBackClick = { navController.popBackStack() })

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                // QR Code Section
                QRCodeSection(
                    qrCodeUrl = qrCodeUrl,
                    onSaveQr = onSaveQr,
                    isSaving = isSaving,
                    navController = navController,
                    receiptId = receipt.id,
                    paymentStatus = receipt.paymentStatus
                )

                // Metrics Section (for electricity)
                if (billType == BillType.ELECTRICITY && (vtConsumption > 0 || ntConsumption > 0)) {
                    ElectricityMetricsSection(
                        vtConsumption = vtConsumption,
                        ntConsumption = ntConsumption
                    )
                }

                // Amount Field
                DataField(
                    label = "IZNOS",
                    value = formattedAmount,
                    icon = Icons.Default.Payment,
                    iconColor = customColors.neonPurple,
                    isLarge = true
                )

                // Invoice Number Field
                if (receipt.invoiceNumber != null) {
                    DataField(
                        label = "BRO J RAČUNA",
                        value = receipt.invoiceNumber,
                        icon = Icons.Default.Tag,
                        iconColor = customColors.neonCyan
                    )
                }

                // Date Field
                DataField(
                    label = "DATUM RAČUNA",
                    value = formattedDate,
                    icon = Icons.Default.CalendarMonth,
                    iconColor = customColors.neonCyan
                )

                // Due Date Field (if exists)
                if (formattedDueDate != null) {
                    DataField(
                        label = "ROK PLAĆANJA",
                        value = formattedDueDate,
                        icon = Icons.Default.Event,
                        iconColor = customColors.neonPurple
                    )
                }

                // Issuer Field
                DataField(
                    label = "IZDAVALAC",
                    value = receipt.merchantName,
                    icon = Icons.Default.Business,
                    iconColor = customColors.neonCyan
                )
                
                // Payment Purpose Field
                DataFieldMultiline(
                    label = "SVRHA PLAĆANJA",
                    value = receipt.metadata ?: "Račun za usluge",
                    icon = Icons.Default.Description,
                    iconColor = customColors.neonCyan
                )
                
                // Receipt Items Section (for fiscal receipts)
                if (receiptItems.isNotEmpty()) {
                    ReceiptItemsSection(items = receiptItems)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TopNavigationBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = "Detalji Računa",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun QRCodeSection(
    qrCodeUrl: String,
    onSaveQr: () -> Unit,
    isSaving: Boolean,
    navController: NavController,
    receiptId: Long,
    paymentStatus: com.example.platisa.core.domain.model.PaymentStatus
) {
    val customColors = LocalPlatisaColors.current
    // Generate QR code bitmap from the data string
    val qrBitmap = remember(qrCodeUrl) {
        if (qrCodeUrl.isNotEmpty()) {
            QrCodeGenerator.generateQrCode(qrCodeUrl, size = 512)
        } else {
            null
        }
    }
    
    val isPaid = paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID
    
    // Scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLineOffset"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // QR Code with 3D Glass Effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if(isPaid) customColors.statusPaid.copy(alpha = 0.5f) else customColors.neonCyan.copy(alpha = 0.5f)
                )
        ) {
            // Outer glow layer
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if(isPaid) customColors.statusPaid.copy(alpha = 0.3f) else customColors.neonCyan.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Main QR container with glass effect
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = if(isPaid) listOf(customColors.statusPaid, customColors.statusPaid) else listOf(
                                customColors.neonCyan,
                                customColors.neonPurple
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = qrBitmap
                if (bitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // QR Code
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize().alpha(if(isPaid) 0.3f else 1f)
                        )
                        
                        if (isPaid) {
                           Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Paid",
                                tint = customColors.statusPaid,
                                modifier = Modifier.size(80.dp).align(Alignment.Center)
                           )
                        } else {
                            // Scan line animation overlay (Only if NOT paid)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = (178.dp * scanLineOffset))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                customColors.neonCyan.copy(alpha = 0.8f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(2.dp),
                                        spotColor = customColors.neonCyan
                                    )
                            )
                        }
                        
                        // Glass shine effect - top left
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.TopStart)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        // Glass reflection - diagonal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .fillMaxHeight()
                                .align(Alignment.CenterStart)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                } else {
                    // Fallback when no QR code data
                    Text(
                        text = "Nema QR koda",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 3D depth effect - bottom shadow
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }

        Button(
            onClick = {
                onSaveQr()
                // Navigate back with the receipt ID to scroll to it
                navController.previousBackStackEntry?.savedStateHandle?.set("scrollToReceiptId", receiptId)
                navController.popBackStack()
            },
            enabled = !isSaving && !isPaid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(72.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = if(isPaid) Color.Gray.copy(alpha = 0.1f) else customColors.neonCyan.copy(alpha = 0.3f)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if(isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = if(isPaid) listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)) else listOf(
                                    customColors.neonCyan.copy(alpha = 0.6f),
                                    customColors.neonPurple.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                )

                if (isSaving) {
                    CircularProgressIndicator(
                        color = customColors.neonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isPaid) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = customColors.neonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = if (isPaid) "RAČUN PLAĆEN" else "SAČUVAJ QR KOD",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) customColors.statusPaid else MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ElectricityMetricsSection(
    vtConsumption: Int,
    ntConsumption: Int
) {
    val customColors = LocalPlatisaColors.current
    val total = vtConsumption + ntConsumption
    if (total == 0) return
    
    val vtPercent = (vtConsumption.toFloat() / total * 100)
    val ntPercent = (ntConsumption.toFloat() / total * 100)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "POTROŠNJA ELEKTRIČNE ENERGIJE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.neonCyan,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = customColors.statusPaid.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${total.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1.")} kWh",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Ukupna potrošnja",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Raspodela potrošnje",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    ConsumptionBar(
                        label = "Viša Tarifa",
                        value = vtConsumption,
                        percentage = vtPercent,
                        color = customColors.neonPurple,
                        modifier = Modifier.weight(1f)
                    )

                    ConsumptionBar(
                        label = "Niža Tarifa",
                        value = ntConsumption,
                        percentage = ntPercent,
                        color = customColors.neonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsumptionBar(
    label: String,
    value: Int,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedHeight by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "barHeight"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = "$value kWh",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeight)
                    .shadow(
                        elevation = 15.dp,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        spotColor = color.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                color.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
            ) {
                // Glass shine overlay - top highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Side gloss effect - left edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.15f)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Bottom inner glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    color.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "${percentage.toInt()}%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DataField(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isLarge: Boolean = false
) {
    val customColors = LocalPlatisaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.neonCyan,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(if (isLarge) 36.dp else 28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                com.example.platisa.ui.components.DynamicSizeText(
                    text = value,
                    minFontSize = if (isLarge) 16.sp else 12.sp,
                    maxFontSize = if (isLarge) 28.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isLarge) FontFamily.Monospace else FontFamily.Default,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun DataFieldMultiline(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    val customColors = LocalPlatisaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.neonCyan,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun ReceiptItemsSection(items: List<com.example.platisa.core.domain.model.ReceiptItem>) {
    val customColors = LocalPlatisaColors.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "STAVKE RAČUNA (${items.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.neonCyan,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        items.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.quantity} kom",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.unitPrice != null && item.unitPrice != java.math.BigDecimal.ZERO) {
                                Text(
                                    text = "× ${com.example.platisa.core.common.Formatters.formatCurrency(item.unitPrice)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = com.example.platisa.core.common.Formatters.formatCurrency(item.total ?: java.math.BigDecimal.ZERO),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.neonPurple
                    )
                }
            }
        }
    }
}


enum class BillType {
    ELECTRICITY,
    WATER,
    PHONE,
    INTERNET,
    APARTMENT
}
