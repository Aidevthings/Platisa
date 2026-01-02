package com.example.platisa.ui.screens.review

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.platisa.ui.screens.review.ReviewReceiptViewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.platisa.core.common.BaseScreen
import com.example.platisa.ui.components.PlatisaInput
import com.example.platisa.ui.components.PlatisaButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.width

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ReviewReceiptScreen(
    navController: NavController,
    imageUri: String,
    viewModel: ReviewReceiptViewModel = hiltViewModel()
) {
    val parsedReceipt by viewModel.parsedReceipt.collectAsState()
    val suggestedSection by viewModel.suggestedSection.collectAsState()
    val isExistingReceipt by viewModel.isExistingReceipt.collectAsState()
    val isPdfSource by viewModel.isPdfSource.collectAsState()
    val isDuplicate by viewModel.isDuplicate.collectAsState()
    val duplicateReceiptId by viewModel.duplicateReceiptId.collectAsState()
    
    // Manual URL Dialog State
    var showUrlDialog by remember { mutableStateOf(false) }
    var manualUrlText by remember { mutableStateOf("") }
    
    // Local state for editing
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var merchant by remember(parsedReceipt) { mutableStateOf(parsedReceipt?.merchantName ?: "") }
    
    // Format number to Serbian locale (1.234,56)
    var total by remember(parsedReceipt) { 
        mutableStateOf(
            com.example.platisa.core.common.Formatters.formatCurrency(parsedReceipt?.totalAmount)
        ) 
    }
    
    // Format date to dd.MM.yyyy
    var date by remember(parsedReceipt) { 
        mutableStateOf(
            parsedReceipt?.date?.let { 
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(it) 
            } ?: ""
        ) 
    }
    
    // Fallback URI for non-PDF images
    var currentImageUri by remember { mutableStateOf(Uri.parse(imageUri)) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                currentImageUri = uriContent
                viewModel.reprocessImage(uriContent)
            }
        } else {
             // Handle error
        }
    }

    BaseScreen(viewModel = viewModel) {
    BaseScreen(viewModel = viewModel) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.platisa.ui.components.AppBackground()

            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        title = { 
                            Text(
                                "Pregled Računa",
                                color = androidx.compose.ui.graphics.Color.White
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                                    contentDescription = "Nazad",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        },
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        actions = {
                            // Manual URL Button
                            IconButton(onClick = { showUrlDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Manual URL",
                                    tint = com.example.platisa.ui.theme.NeonPurple
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isExistingReceipt) {
                        // Only show save button for NEW receipts (camera)
                        PlatisaButton(
                            text = "Sačuvaj Račun (Kamera)",
                            onClick = {
                                viewModel.confirmReceipt(
                                    merchant = merchant,
                                    total = total,
                                    dateStr = date,
                                    invoiceNumber = parsedReceipt?.invoiceNumber
                                )
                                navController.navigateUp()
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ONLY show image preview for Camera sources (not PDFs)
                if (!isPdfSource) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(currentImageUri),
                            contentDescription = "Captured Receipt",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        IconButton(
                            onClick = {
                                val options = CropImageOptions(
                                    imageSourceIncludeGallery = false,
                                    imageSourceIncludeCamera = false
                                )
                                cropLauncher.launch(
                                    CropImageContractOptions(currentImageUri, options)
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // QR CODE SECTION - Moved out of image box to prevent clipping
                parsedReceipt?.qrCodeData?.let { qrData ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.platisa.ui.components.NeonCard(
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "IPS QR KOD",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = com.example.platisa.ui.theme.CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                val qrBitmap = remember(qrData) {
                                    com.example.platisa.core.common.QrCodeGenerator.generateQrCode(qrData, 300)
                                }
                                
                                qrBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier
                                            .size(180.dp)
                                            .background(
                                                Color.White,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Save to Gallery Button
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            viewModel.saveQrCodeToGallery(merchant, total, date)
                                            navController.navigateUp()
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = com.example.platisa.ui.theme.CyberCyan
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Download,
                                            contentDescription = "Save",
                                            tint = androidx.compose.ui.graphics.Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "SAČUVAJ QR",
                                            color = androidx.compose.ui.graphics.Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Skeniraj za plaćanje",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.platisa.ui.theme.TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                // DUPLICATE WARNING BANNER
                if (isDuplicate && duplicateReceiptId != null) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFB800).copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            androidx.compose.ui.graphics.Color(0xFFFFB800)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFFFFB800),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                com.example.platisa.ui.components.DynamicSizeText(
                                    text = "⚠️ UPOZORENJE: DUPLIKAT",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = androidx.compose.ui.graphics.Color(0xFFFFB800),
                                    minFontSize = 14.sp,
                                    maxFontSize = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                            }
                            Text(
                                text = "Ra\u010dun sa ovim brojem ve\u0107 postoji u bazi!",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (parsedReceipt?.invoiceNumber != null) {
                                Text(
                                    text = "Ra\u010dun broj: ${parsedReceipt?.invoiceNumber}",
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            androidx.compose.material3.Button(
                                onClick = { 
                                    navController.navigate(
                                        com.example.platisa.ui.navigation.Screen.BillDetails.createRoute(
                                            duplicateReceiptId.toString()
                                        )
                                    ) 
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFB800)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "POGLEDAJ POSTOJE\u0106I RA\u010cUN",
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                
                // DATA SECTIONS BASED ON CATEGORY
                val epsData by viewModel.epsData.collectAsState()
                val parsedReceipt by viewModel.parsedReceipt.collectAsState()
                val merchantName = parsedReceipt?.merchantName ?: ""
                val category = com.example.platisa.core.domain.model.BillCategorizer.categorize(merchantName)
                
                when (category) {
                    com.example.platisa.core.domain.model.BillCategory.ELECTRICITY -> {
                        if (epsData != null) {
                            // Calculate total kWh directly from VT + NT (don't trust database value)
                            val vt = epsData!!.consumptionVt ?: java.math.BigDecimal.ZERO
                            val nt = epsData!!.consumptionNt ?: java.math.BigDecimal.ZERO
                            val totalKwh = vt.add(nt).toInt()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .border(
                                        width = 2.dp,
                                        color = com.example.platisa.ui.theme.MatrixGreen,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        color = com.example.platisa.ui.theme.CardSurface,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    // Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            text = "⚡",
                                            fontSize = 24.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "EPS Potrošnja",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = com.example.platisa.ui.theme.MatrixGreen,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    
                                    // Viša Tarifa
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Viša Tarifa:",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${vt.toInt()} kWh",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    
                                    // Niža Tarifa
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Niža Tarifa:",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${nt.toInt()} kWh",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    
                                    // Divider
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(com.example.platisa.ui.theme.MatrixGreen.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Total (calculated from VT + NT)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "UKUPNO:",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "$totalKwh kWh",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    com.example.platisa.core.domain.model.BillCategory.WATER -> {
                        // TODO: Implement Water bill specific data display (m3 consumption)
                    }
                    
                    com.example.platisa.core.domain.model.BillCategory.TELECOM -> {
                        // TODO: Implement Telecom bill specific data display (minutes, data usage)
                    }
                    
                    com.example.platisa.core.domain.model.BillCategory.GAS -> {
                        // TODO: Implement Gas bill specific data display (m3 consumption)
                    }
                    
                    else -> {
                        // No specific data display for other categories
                    }
                }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    PlatisaInput(
                        value = merchant,
                        onValueChange = { if (!isExistingReceipt) merchant = it },
                        label = "Prodavac",
                        readOnly = isExistingReceipt
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PlatisaInput(
                        value = total,
                        onValueChange = { if (!isExistingReceipt) total = it },
                        label = "Ukupan Iznos",
                        suffix = { Text("dinara") },
                        readOnly = isExistingReceipt
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PlatisaInput(
                        value = date,
                        onValueChange = { if (!isExistingReceipt) date = it },
                        label = "Datum",
                        readOnly = isExistingReceipt
                    )
                    
                    // Show invoice number if extracted
                    if (parsedReceipt?.invoiceNumber != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PlatisaInput(
                            value = parsedReceipt?.invoiceNumber ?: "",
                            onValueChange = { },
                            label = "Ra\u010dun Broj (Invoice Number)",
                            readOnly = true
                        )
                    }

                    // ITEMS TABLE
                    if (!parsedReceipt?.items.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Stavke Računa (${parsedReceipt?.items?.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = com.example.platisa.ui.theme.CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = com.example.platisa.ui.theme.CardSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.platisa.ui.theme.CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                parsedReceipt?.items?.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Name & Quantity
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${com.example.platisa.core.common.Formatters.formatCurrency(item.quantity)} x ${com.example.platisa.core.common.Formatters.formatCurrency(item.unitPrice)}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                        
                                        // Total Price
                                        Text(
                                            text = com.example.platisa.core.common.Formatters.formatCurrency(item.total),
                                            color = com.example.platisa.ui.theme.CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    if (index < (parsedReceipt?.items?.size ?: 0) - 1) {
                                        androidx.compose.material3.Divider(
                                            thickness = 0.5.dp,
                                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // DEBUG: Show QR status
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DEBUG: QR Data = ${if (parsedReceipt?.qrCodeData != null) "Postoji (${parsedReceipt?.qrCodeData?.take(20)}...)" else "NEMA"}",
                        color = androidx.compose.ui.graphics.Color.Yellow,
                        fontSize = 10.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    if (suggestedSection != null) {
                        Text("Predložena Sekcija: $suggestedSection", color = androidx.compose.ui.graphics.Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Debug Info Button
                    var showDebugDialog by remember { mutableStateOf(false) }
                    val rawText by viewModel.rawText.collectAsState()
                    
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showDebugDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Prikaži Debug Info (Gemini & OCR)", color = com.example.platisa.ui.theme.NeonCyan)
                    }
                    
                    if (showDebugDialog) {
                        val debugInfo = remember(parsedReceipt, rawText) {
                            "Prodavac: ${parsedReceipt?.merchantName}\n" +
                            "Iznos: ${parsedReceipt?.totalAmount}\n" +
                            "Datum: ${parsedReceipt?.date}\n\n" +
                            "--- RAW TEXT START ---\n" +
                            rawText +
                            "\n--- RAW TEXT END ---"
                        }
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDebugDialog = false },
                            title = { Text("Sirovi Podaci (Gemini Status)") },
                            text = {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .height(300.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(debugInfo, fontSize = 12.sp)
                                }
                            },
                            confirmButton = {
                                androidx.compose.foundation.layout.Row {
                                    androidx.compose.material3.TextButton(onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(debugInfo))
                                    }) {
                                        Text("Kopiraj")
                                    }
                                    androidx.compose.material3.TextButton(onClick = { showDebugDialog = false }) {
                                        Text("Zatvori")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Manual URL Dialog
    if (showUrlDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Unesi Fiskalni Link") },
            text = {
                Column {
                    Text("Ako skeniranje ne radi, nalepi link ovde:", color = androidx.compose.ui.graphics.Color.Gray)
                    androidx.compose.material3.TextField(
                        value = manualUrlText,
                        onValueChange = { manualUrlText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text("https://suf.purs.gov.rs/...") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.processManualUrl(manualUrlText)
                        showUrlDialog = false
                    }
                ) {
                    Text("Skeniraj Link", color = com.example.platisa.ui.theme.CyberCyan)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUrlDialog = false }) {
                    Text("Odustani", color = androidx.compose.ui.graphics.Color.Gray)
                }
            },
            containerColor = com.example.platisa.ui.theme.CardSurface,
            titleContentColor = androidx.compose.ui.graphics.Color.White,
            textContentColor = androidx.compose.ui.graphics.Color.White
        )
    }
    }
}
}

