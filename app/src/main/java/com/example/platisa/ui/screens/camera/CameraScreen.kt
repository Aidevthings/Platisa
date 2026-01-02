package com.example.platisa.ui.screens.camera

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.platisa.core.common.SnackbarManager
import com.example.platisa.ui.navigation.Screen
import kotlinx.coroutines.launch

// Colors
private val BackgroundDark = Color(0xFF0A0A0F)
private val NeonCyan = Color(0xFF00EAFF)
private val NeonMagenta = Color(0xFFFF00D9)
private val CardBg = Color(0xFF1A1A2E)

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var manualUrlText by remember { mutableStateOf("") }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                SnackbarManager.showMessage("Obrada slike...")
                val qrData = com.example.platisa.core.common.QrCodeExtractor.extractQrCode(uri.toString(), context)
                navController.navigate(Screen.ReviewReceipt.createRoute(uri.toString(), qrData))
            }
        }
    }
    
    // Google Scanner function
    // Google Scanner function
    val launchGoogleScanner: () -> Unit = {
        if (!isScanning && !isProcessing) {
            isScanning = true
            scope.launch {
                try {
                    val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE,
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417,
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX
                        )
                        .enableAutoZoom()
                        .build()
                    
                    val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
                    
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue ?: run {
                                isScanning = false
                                return@addOnSuccessListener
                            }
                            android.util.Log.d("CameraScreen", "Google Scanner detected: $rawValue")
                            
                            // Check if IPS payment QR
                            val ipsData = com.example.platisa.core.data.parser.IpsParser.parse(rawValue)
                            if (ipsData != null) {
                                isProcessing = true // Block UI
                                scope.launch {
                                    try {
                                        SnackbarManager.showMessage("IPS račun prepoznat! Čuvam...")
                                        viewModel.saveIpsBill(ipsData)
                                        SnackbarManager.showMessage("Račun sačuvan!")
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        SnackbarManager.showMessage("Greška: ${e.message}")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                                isScanning = false
                                return@addOnSuccessListener
                            }
                            
                            // Check if fiscal receipt URL
                            if (com.example.platisa.core.common.FiscalScraper.isFiscalUrl(rawValue)) {
                                isProcessing = true // Block UI
                                scope.launch {
                                    try {
                                        SnackbarManager.showMessage("Fiskalni račun prepoznat! Učitavam...")
                                        val receiptId = viewModel.saveFiscalReceipt(rawValue)
                                        if (receiptId != null) {
                                            SnackbarManager.showMessage("Račun sačuvan!")
                                            navController.navigate(Screen.FiscalReceiptDetails.createRoute(receiptId))
                                        } else {
                                            val fallbackId = viewModel.saveFiscalReceiptFallback(rawValue)
                                            if (fallbackId != null) {
                                                SnackbarManager.showMessage("Link sačuvan!")
                                                navController.navigate(Screen.FiscalReceiptDetails.createRoute(fallbackId))
                                            } else {
                                                SnackbarManager.showMessage("Greška pri čuvanju")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        SnackbarManager.showMessage("Greška: ${e.message}")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            } else {
                                // Unknown QR code - show message
                                scope.launch {
                                    SnackbarManager.showMessage("Nepoznat QR kod")
                                }
                            }
                            isScanning = false
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CameraScreen", "Google Scanner failed", e)
                            scope.launch {
                                SnackbarManager.showMessage("Skeniranje otkazano")
                            }
                            isScanning = false
                        }
                        .addOnCanceledListener {
                            isScanning = false
                            // User cancelled scanning, go to Price Comparison directly
                            scope.launch {
                                navController.navigate(Screen.Market.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        }
                } catch (e: Exception) {
                    SnackbarManager.showMessage("Greška: ${e.message}")
                    isScanning = false
                }
            }
        }
    }
    
    // Request permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    var hasAutoLaunched by rememberSaveable { mutableStateOf(false) }
    
    // Auto-launch scanner ONLY ONCE when permission granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !isScanning && !hasAutoLaunched) {
            hasAutoLaunched = true
            launchGoogleScanner()
        }
    }
    
    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        com.example.platisa.ui.components.AppBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .border(2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Skeniraj QR Kod",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Skeniraj fiskalni ili IPS QR kod\nsa računa za brzu obradu",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Scan Button
            Button(
                onClick = { launchGoogleScanner() },
                enabled = hasCameraPermission && !isScanning && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning || isProcessing) Color.Gray else NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isScanning || isProcessing) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isProcessing) "OBRADA..." else "SKENIRANJE...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "POKRENI SKENER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Secondary buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gallery Button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galerija")
                }
                
                // Manual Link Button
                OutlinedButton(
                    onClick = { showUrlDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Back button
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nazad", color = Color.Gray)
            }
        }
    }
    
    // Manual URL Dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Unesi Link", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = manualUrlText,
                    onValueChange = { manualUrlText = it },
                    label = { Text("URL fiskalnog računa") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualUrlText.isNotBlank()) {
                            navController.navigate(
                                Screen.ReviewReceipt.createRoute("manual_entry", manualUrlText)
                            )
                            showUrlDialog = false
                        }
                    }
                ) {
                    Text("Učitaj", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Odustani", color = Color.Gray)
                }
            },
            containerColor = CardBg
        )
    }
}
