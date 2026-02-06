package com.platisa.app.ui.screens.billdetails

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
import kotlinx.coroutines.launch
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
import com.platisa.app.core.common.QrCodeGenerator
import com.platisa.app.core.domain.model.Receipt
import java.text.SimpleDateFormat
import java.util.Locale

import com.platisa.app.ui.theme.LocalPlatisaColors
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import java.math.BigDecimal

enum class PaymentOption {
    CURRENT_MONTH,
    TOTAL_DEBT
}


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
                com.platisa.app.ui.components.AppBackground()
                CircularProgressIndicator(color = customColors.neonCyan)
            }
        }
        is BillDetailsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.platisa.app.ui.components.AppBackground()
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
            // Determine if popup should be shown:
            // Only for ELECTRICITY bills AND only for the LATEST bill (cascade payment system)
            // AND only if reminder hasn't been set yet
            val hasReminderSet = state.receipt.metadata?.contains("[REMINDER_SET]") == true
            val shouldShowPopup = (state.billType == BillType.ELECTRICITY || state.infostanDeadline != null) && 
                                 state.isLatestForMerchant && !hasReminderSet
            
            // State to control popup visibility
            var showDiscountPopup by remember { mutableStateOf(shouldShowPopup) }
            
            // Debug logging
            android.util.Log.d("BillDetails", "🎯 POPUP: billType=${state.billType}, isLatest=${state.isLatestForMerchant}, hasReminder=$hasReminderSet, shouldShow=$shouldShowPopup")
            
            // Show discount popup overlay (only for latest electricity bills)
            if (showDiscountPopup) {
                android.util.Log.d("BillDetails", "🎉 POPUP: Rendering DiscountPopup for latest electricity bill!")
                DiscountPopup(
                    discountTable = state.discountTable,
                    infostanDeadline = state.infostanDeadline,
                    onDismiss = { 
                        android.util.Log.d("BillDetails", "👋 POPUP: Dismissed by user")
                        showDiscountPopup = false 
                    },
                    onRemind = {
                        // Schedule reminder for the discount deadline
                        // We extract ALL deadlines from the table OR use the single Infostan deadline
                        val deadlines = if (state.infostanDeadline != null) {
                            listOf(state.infostanDeadline)
                        } else {
                            state.discountTable?.mapNotNull { it.deadline } ?: emptyList()
                        }
                        viewModel.scheduleDiscountReminder(state.receipt.id, deadlines)
                        showDiscountPopup = false
                    }
                )
            }
            
            BillDetailsContent(
                navController = navController,
                receipt = state.receipt,
                billType = state.billType,
                vtConsumption = state.vtConsumption,
                ntConsumption = state.ntConsumption,
                receiptItems = receiptItems,
                onSaveQr = { viewModel.saveQrCode(it) },
                onMarkPaid = { payTotal -> viewModel.markAsPaid(payTotal) },
                isSaving = saveQrStatus is SaveQrStatus.Saving,
                isLatestForMerchant = state.isLatestForMerchant,
                vibrate = { viewModel.vibrate(it) },
                isDebtPartiallyPaid = state.isDebtPartiallyPaid,
                localUnpaidSum = state.localUnpaidSum,
                billDebt = state.billDebt,
                smartTotalDebt = state.smartTotalDebt,
                paidPastBillsSum = state.paidPastBillsSum,
                currency = state.currency
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
    receiptItems: List<com.platisa.app.core.domain.model.ReceiptItem>,
    onSaveQr: (Boolean) -> Unit,
    onMarkPaid: (Boolean) -> Unit,
    isSaving: Boolean,
    isLatestForMerchant: Boolean,
    vibrate: (com.platisa.app.core.common.VibrationHelper.HapticType) -> Unit,
    isDebtPartiallyPaid: Boolean = false,
    localUnpaidSum: Double = 0.0,
    billDebt: Double = 0.0,
    smartTotalDebt: Double = 0.0,
    paidPastBillsSum: Double = 0.0,
    currency: String = "RSD"
) {
    val customColors = LocalPlatisaColors.current
    // Extract QR code URL from receipt
    val qrCodeUrl = receipt.qrCodeData ?: ""
    
    // Debug logging
    LaunchedEffect(qrCodeUrl) {
        android.util.Log.d("BillDetails", "QR Code URL: $qrCodeUrl")
        android.util.Log.d("BillDetails", "QR Code exists: ${qrCodeUrl.isNotEmpty()}")
    }
    
    // Scope for suspend functions (Snackbar)
    val scope = rememberCoroutineScope()

    // Smart Payment Logic
    var selectedOption by remember { mutableStateOf(PaymentOption.CURRENT_MONTH) }

    // Calculate display values based on selection
    val currentAmount = receipt.currentMonthAmount ?: BigDecimal.ZERO
    
    // SMART TOTAL DEBT: Use the calculated value from VM if we have a match, otherwise fallback to simple sum
    val totalDebtAmount = if (smartTotalDebt > 0.0) {
        BigDecimal.valueOf(smartTotalDebt)
    } else {
        (receipt.currentMonthAmount ?: BigDecimal.ZERO) + (receipt.previousDebtAmount ?: BigDecimal.ZERO)
    }
    
    // Only show Total Debt option if it is the latest bill AND total debt > current amount
    val showTotalDebtOption = remember(receipt, isLatestForMerchant, totalDebtAmount, currentAmount) {
        isLatestForMerchant &&
        totalDebtAmount > currentAmount
    }
    
    // Force selection to CURRENT_MONTH if Total Debt is hidden
    LaunchedEffect(showTotalDebtOption) {
        if (!showTotalDebtOption) {
            selectedOption = PaymentOption.CURRENT_MONTH
        }
    }
    
    var displayAmount = if (showTotalDebtOption && selectedOption == PaymentOption.TOTAL_DEBT) {
        totalDebtAmount
    } else {
        currentAmount
    }

    // FALLBACK: If displayAmount is essentially zero, but totalAmount exists, use that.
    // This fixed the "0.00" bug where previousDebt was found but currentMonth was nil.
    if (displayAmount <= BigDecimal.ZERO && (receipt.totalAmount ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
        android.util.Log.d("BillDetails", "Fallback: displayAmount was $displayAmount, using totalAmount ${receipt.totalAmount}")
        displayAmount = receipt.totalAmount ?: BigDecimal.ZERO
    }

    // Patch QR Code if needed - ALWAYS patch for Smart Debt to ensure correct amount
    val displayQrCode = remember(qrCodeUrl, displayAmount, showTotalDebtOption) {
        patchQrAmount(qrCodeUrl, displayAmount)
    }

    // Format amount
    val formattedAmount = com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(displayAmount, currency)

    // Format date
    val formattedDate = SimpleDateFormat("dd. MMMM yyyy", Locale("sr", "Latn", "RS")).format(receipt.date)
    val formattedDueDate = receipt.dueDate?.let { 
        SimpleDateFormat("dd. MMMM yyyy", Locale("sr", "Latn", "RS")).format(it)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        com.platisa.app.ui.components.AppBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            TopNavigationBar(
                onBackClick = { 
                    vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                    navController.popBackStack() 
                },
                onMarkPaid = {
                    vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.HEAVY)
                    onMarkPaid(it)
                },
                selectedOption = selectedOption
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))
                
                // ANOMALY WARNING
                if (receipt.anomalyType != null && !receipt.isAnomalyConfirmed) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Anomaly",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(
                                    text = "Neuobičajen Iznos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = receipt.anomalyMessage ?: "Ovaj račun značajno odstupa od vašeg proseka.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // INFORMATION: Smart Debt Calculation
                // Only show if we actually deducted something (paidPastBillsSum > 0)
                // AND if we are offering the Total Debt option (meaning there is still extra debt to pay)
                if (isDebtPartiallyPaid && paidPastBillsSum > 0.01 && showTotalDebtOption) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = customColors.neonCyan.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Smart Calc",
                                tint = customColors.neonCyan
                            )
                            Column {
                                Text(
                                    text = "Pametni obračun duga",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.neonCyan
                                )
                                Text(
                                    text = "Vaš ukupan dug je umanjen za račune koje ste već platili kroz aplikaciju (${com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(BigDecimal(paidPastBillsSum), currency)}).",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Payment Option Toggle (Only visible if Latest Bill AND has Extra Debt)
                if (showTotalDebtOption) {
                    PaymentOptionSelector(
                        selectedOption = selectedOption,
                        onOptionSelected = { 
                            vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                            selectedOption = it 
                        },
                        currentMonthAmount = currentAmount,
                        totalDebtAmount = totalDebtAmount,
                        isTotalDebtDisabled = false, // Always enabled if shown
                        showTotalDebtOption = true,
                        currency = currency
                    )
                }

                // QR Code Section
                QRCodeSection(
                    qrCodeUrl = displayQrCode,
                    onSaveQr = { 
                        vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.HEAVY)
                        onSaveQr(selectedOption == PaymentOption.TOTAL_DEBT) 
                    },
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

                // Property Address Field - Prioritize recipientAddress for utility bills
                val address = receipt.recipientAddress ?: receipt.payerAddress
                
                // Helper to validate if the string looks like a real address and not a misread price line
                fun isValidAddress(text: String): Boolean {
                    if (text.length < 3) return false
                    val upper = text.uppercase()
                    
                    // If it's already explicitly labeled by our parser, it's valid
                    if (upper.contains("ADRESA:") || upper.contains("OPŠTINA:") || upper.contains("NASelje:")) return true

                    // Reject if contains currency or price-like patterns (Latin & Cyrillic)
                    val moneyPatterns = listOf("RSD", "DIN", "РСД", "ДИН", "EUR", "€")
                    if (moneyPatterns.any { upper.contains(it) }) return false
                    
                    // Reject if it's just numbers, dots, and spaces (like a date or amount)
                    if (text.matches(Regex("^[\\d.,\\s-]+$"))) return false
                    
                    // Reject if it contains a decimal amount like ,00 or .00
                    if (text.contains(Regex("\\d+[.,]\\d{2}"))) return false

                    return true
                }
                
                fun formatAddress(text: String): String {
                    var formatted = text
                    
                    // 1. Clean metadata prefixes if they exist (optional, but good for cleanup)
                    val metadataKeywords = listOf(
                        "VRSTA PROSTORA", "ŠIFRA PROSTORA", "SIFRA PROSTORA",
                        "ВРСТА ПРОСТОРА", "ШИФРА ПРОСТОРА", "KATEGORIJA", "POVRŠINA",
                        "VRSTA", "SIFRA", "ŠIFRA"
                    )
                    for (keyword in metadataKeywords) {
                        val regex = Regex("(?i)$keyword[:\\s]+[^,]*", RegexOption.IGNORE_CASE)
                        formatted = formatted.replace(regex, "").trim()
                    }
                    formatted = formatted.removePrefix(":").removePrefix(",").trim().removeSuffix(",").trim()

                    // 2. Insert Newlines before key structural keywords
                    // Case insensitive matching for Rejon, Pak, Opstina, etc.
                    val splitKeywords = listOf("Rejon:", "Pak:", "Opština:", "Opstina:", "Naselje:", "Ulica:", "Broj:")
                    for (keyword in splitKeywords) {
                        // Replace " Keyword:" with "\nKeyword:"
                        // We look for word boundary or space before it
                        val regex = Regex("(?i)\\b(${keyword})", RegexOption.IGNORE_CASE)
                        formatted = formatted.replace(regex, "\n$1")
                    }

                    // 3. Move Postal Codes (5 digits) to new line
                    // Matches 5 digits that are distinct words (e.g. 11000)
                    // Replaces: "Street 10, 11000 City" -> "Street 10,\n11000 City"
                    val postalCodeRegex = Regex("[,\\s]+(\\d{5})\\b")
                    formatted = formatted.replace(postalCodeRegex, "\n$1")

                    // 3. Fix double newlines just in case
                    formatted = formatted.replace("\n\n", "\n").trim()
                    
                    return formatted
                }

                // FORCE DISPLAY FOR DEBUGGING
                // if (address != null && isValidAddress(address)) {
                if (address != null) {
                    val cleanedDisplayAddress = formatAddress(address)
                   if (cleanedDisplayAddress.isNotBlank()) {
                       DataFieldMultiline(
                           label = "ADRESA OBJEKTA",
                           value = cleanedDisplayAddress,
                           icon = Icons.Default.Home,
                           iconColor = customColors.neonPurple
                       )
                   }
                }

                // Invoice Number Field (Always visible as requested)
                DataField(
                    label = "BROJ RAČUNA",
                    value = receipt.invoiceNumber ?: "Nije detektovan",
                    icon = Icons.Default.Tag,
                    iconColor = if (receipt.invoiceNumber != null) customColors.neonCyan else Color.Gray
                )

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
                



                

                
                // Receipt Items Section (for fiscal receipts)
                if (receiptItems.isNotEmpty()) {
                    ReceiptItemsSection(items = receiptItems, currency = currency)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TopNavigationBar(
    onBackClick: () -> Unit,
    onMarkPaid: (Boolean) -> Unit, // Boolean: isTotalDebt
    selectedOption: PaymentOption
) {
    var showMenu by remember { mutableStateOf(false) }
    
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

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Označi kao plaćeno") },
                    onClick = {
                        onMarkPaid(selectedOption == PaymentOption.TOTAL_DEBT)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
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
    paymentStatus: com.platisa.app.core.domain.model.PaymentStatus
) {
    val customColors = LocalPlatisaColors.current
    
    // Check if already in PROCESSING state (QR already saved)
    val isProcessing = paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PROCESSING
    
    // Generate QR code bitmap from the data string
    val qrBitmap = remember(qrCodeUrl) {
        if (qrCodeUrl.isNotEmpty()) {
            QrCodeGenerator.generateQrCode(qrCodeUrl, size = 512)
        } else {
            null
        }
    }
    
    val isPaid = paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID
    
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
        verticalArrangement = Arrangement.spacedBy(8.dp), // Reduced spacing
        modifier = Modifier.fillMaxWidth()
    ) {
        // QR Code with 3D Glass Effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp) // Reduced from 220.dp
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if(isPaid) customColors.statusPaid.copy(alpha = 0.5f) else customColors.neonCyan.copy(alpha = 0.5f)
                )
        ) {
            // Outer glow layer
            Box(
                modifier = Modifier
                    .size(160.dp) // Reduced from 220.dp
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
                    .size(150.dp) // Reduced from 210.dp
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
                    .padding(12.dp), // Reduced padding
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
                                modifier = Modifier.size(60.dp).align(Alignment.Center) // Reduced icon
                           )
                        } else {
                            // Scan line animation overlay (Only if NOT paid)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = (130.dp * scanLineOffset)) // Adjusted for new height
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
                                .size(60.dp)
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
                        fontSize = 12.sp
                    )
                }
            }
            
            // 3D depth effect - bottom shadow
            Box(
                modifier = Modifier
                    .size(150.dp)
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

        // Custom "Save QR" Button - Clean 3D Glass Design
        // Disabled when: saving, already paid, or already in processing (QR already saved)
        val isButtonDisabled = isPaid || isProcessing
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if(isButtonDisabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                    else customColors.neonCyan.copy(alpha = 0.1f) // Slightly more transparent Blue
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if(isButtonDisabled) listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)) else listOf(
                            customColors.neonCyan.copy(alpha = 0.4f), // Lighter top
                            customColors.neonCyan.copy(alpha = 0.8f)  // Darker bottom for 3D
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled = !isSaving && !isPaid && !isProcessing,
                    onClick = {
                        // Pass 'true' if Total Debt option is selected
                        onSaveQr()
                        navController.previousBackStackEntry?.savedStateHandle?.set("scrollToReceiptId", receiptId)
                        navController.popBackStack()
                    }
                )
        ) {
            // Glass/Gloss Shine Effect (Top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content Center
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                        // Show different icon based on state
                        val buttonIcon = when {
                            isPaid -> null // No icon for paid
                            isProcessing -> Icons.Default.CheckCircle // Checkmark for processing
                            else -> Icons.Default.Download // Download for normal
                        }
                        
                        buttonIcon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isProcessing) Color.Gray else customColors.neonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        // Show different text based on state
                        val buttonText = when {
                            isPaid -> "RAČUN PLAĆEN"
                            isProcessing -> "QR KOD SAČUVAN"
                            else -> "SAČUVAJ QR KOD"
                        }
                        
                        val textColor = when {
                            isPaid -> customColors.statusPaid
                            isProcessing -> Color.Gray
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        
                        Text(
                            text = buttonText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
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
                com.platisa.app.ui.components.DynamicSizeText(
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 28.sp
                )
            }
        }
    }
}

@Composable
fun ReceiptItemsSection(items: List<com.platisa.app.core.domain.model.ReceiptItem>, currency: String = "RSD") {
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
                                    text = "× ${com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(item.unitPrice, currency)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(item.total ?: java.math.BigDecimal.ZERO, currency),
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

@Composable
fun PaymentOptionSelector(
    selectedOption: PaymentOption,
    onOptionSelected: (PaymentOption) -> Unit,
    currentMonthAmount: BigDecimal,
    totalDebtAmount: BigDecimal,
    isTotalDebtDisabled: Boolean = false, // New parameter
    showTotalDebtOption: Boolean = true, // New parameter to control visibility of the second option
    currency: String = "RSD"
) {
    val customColors = LocalPlatisaColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, customColors.neonCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Updated Header with larger font
        val headerText = if (showTotalDebtOption) "Izaberite opciju plaćanja" else "Detalji plaćanja"
        
        Text(
            text = headerText,
            fontSize = 16.sp, // Increased from 14.sp
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option 1: Current Month
            PaymentOptionCard(
                title = "Mesečni račun",
                amount = currentMonthAmount,
                isSelected = selectedOption == PaymentOption.CURRENT_MONTH || !showTotalDebtOption, // Select if only option
                onClick = { onOptionSelected(PaymentOption.CURRENT_MONTH) },
                color = customColors.neonCyan,
                currency = currency
            )

            // Option 2: Total Debt (Conditional)
            if (showTotalDebtOption) {
                PaymentOptionCard(
                    title = "Ukupan dug",
                    amount = totalDebtAmount,
                    isSelected = selectedOption == PaymentOption.TOTAL_DEBT,
                    onClick = { onOptionSelected(PaymentOption.TOTAL_DEBT) },
                    color = if (isTotalDebtDisabled) Color.Gray else customColors.neonPurple, // Gray out if disabled
                    isEnabled = !isTotalDebtDisabled, // New parameter
                    currency = currency
                )
            }
        }
        
        if (showTotalDebtOption) {
            val infoText = if (selectedOption == PaymentOption.CURRENT_MONTH) {
                "ℹ️ Plaćate samo zaduženje za ovaj mesec via IPS QR."
            } else {
                "ℹ️ Plaćate celokupan dug uključujući prethodna dugovanja."
            }
            
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Text(
                text = "ℹ️ Prikazan je samo iznos za tekući mesec jer nemate dodatnih dugovanja.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun PaymentOptionCard(
    title: String,
    amount: BigDecimal,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color,
    isEnabled: Boolean = true, // New parameter
    currency: String = "RSD"
) {
    // Visual State Logic
    val borderColor = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    // Use Surface color for background (tinted if selected)
    val containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent
    val titleColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
    val amountColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

    // Using Surface for better click handling and semantic behavior
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1f else 0.5f), // Dim if disabled
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
    ) {
        // Redesigned Row Layout inside Surface - LARGER for better visibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Increased padding for larger touch target
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Radio Indicator + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                 // Radio-button circle indicator - LARGER
                Box(
                    modifier = Modifier
                        .size(22.dp) // Increased from 18.dp
                        .clip(RoundedCornerShape(50))
                        .border(2.dp, if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha=0.5f), RoundedCornerShape(50))
                        .background(if (isSelected) color else Color.Transparent)
                )
                
                Text(
                    text = title,
                    fontSize = 20.sp, // Increased from 18.sp
                    color = titleColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            // Right Side: Amount - LARGER
            Text(
                text = com.platisa.app.core.common.Formatters.formatCurrencyWithSuffix(amount, currency),
                fontSize = 28.sp, // Increased from 24.sp
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

/**
 * Patches the IPS QR code string with a new amount.
 * IPS format example: K:PR|V:01|C:1|R:160...|I:RSD10000,00|...
 * We need to find "I:RSD..." and replace it.
 */
fun patchQrAmount(qrString: String, newAmount: BigDecimal): String {
    try {
        // Format amount as "RSD1234,56" (using comma as decimal separator, no thousands separator)
        // IPS Standard requires: "I:RSD" + amount with 2 decimals, comma separator
        val amountStr = java.text.DecimalFormat("0.00").apply {
            decimalFormatSymbols = java.text.DecimalFormatSymbols.getInstance(Locale.GERMANY) // Force comma
        }.format(newAmount)
        
        val replacement = "I:RSD$amountStr"
        
        // Regex to replace existing amount "I:RSD[digits],[digits]"
        // Also handling potential "I:RSD[digits].[digits]" just in case
        val regex = Regex("I:RSD[0-9.,]+")
        
        return regex.replace(qrString, replacement)
    } catch (e: Exception) {
        android.util.Log.e("BillDetails", "Failed to patch QR amount", e)
        return qrString
    }
}

