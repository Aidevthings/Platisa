package com.platisa.app.ui.screens.camera

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
import com.platisa.app.core.common.SnackbarManager
import com.platisa.app.ui.navigation.Screen
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

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
    // Explicit camera permission handling for devices that don't auto-prompt
    var isScanning by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isModuleInstalling by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var manualUrlText by remember { mutableStateOf("") }
    var pendingScan by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            // If user grants permission, trigger scan on next effect.
            pendingScan = true
        } else {
            scope.launch {
                SnackbarManager.showMessage("Kamera dozvola je potrebna za skeniranje.")
            }
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                SnackbarManager.showMessage("Obrada slike...")
                val qrData = com.platisa.app.core.common.QrCodeExtractor.extractQrCode(uri.toString(), context)
                navController.navigate(Screen.ReviewReceipt.createRoute(uri.toString(), qrData))
            }
        }
    }
    
    fun startActualScan() {
        if (isScanning) return
        isScanning = true
        
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
                val ipsData = com.platisa.app.core.data.parser.IpsParser.parse(rawValue)
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
                if (com.platisa.app.core.common.FiscalScraper.isFiscalUrl(rawValue)) {
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
                val mlKitError = (e as? com.google.mlkit.common.MlKitException)?.errorCode
                android.util.Log.e("CameraScreen", "Google Scanner failed (Code: $mlKitError)", e)
                
                scope.launch {
                    val userMsg = when (mlKitError) {
                        com.google.mlkit.common.MlKitException.CODE_SCANNER_UNAVAILABLE -> 
                            "Google skener se ažurira. Pokušajte ponovo za par trenutaka."
                        com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED ->
                            null // Don't show toast on cancel
                        else -> "Google skener nije uspeo: ${e.localizedMessage ?: "Greška"}"
                    }
                    userMsg?.let { SnackbarManager.showMessage(it) }
                    isScanning = false
                }
            }
            .addOnCanceledListener {
                isScanning = false
                // User cancelled scanning, stay on screen.
            }
    }

    // Google Scanner function
    fun launchGoogleScanner() {
        if (isScanning || isProcessing || isModuleInstalling) return
        if (!hasCameraPermission) {
            pendingScan = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        // Safety Check: Verify GMS is actually available before trying to init client
        val gms = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val status = gms.isGooglePlayServicesAvailable(context)
        if (status != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            scope.launch {
                SnackbarManager.showMessage("Google Play servisi nisu dostupni. Koristite Galeriju.")
            }
            return
        }

        // Explicitly check/install the scanner module to avoid Error Code 13
        val moduleInstallClient = com.google.android.gms.common.moduleinstall.ModuleInstall.getClient(context)
        val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
        val moduleInstallRequest = com.google.android.gms.common.moduleinstall.ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()

        isModuleInstalling = true
        moduleInstallClient.installModules(moduleInstallRequest)
            .addOnSuccessListener { response ->
                isModuleInstalling = false
                if (response.areModulesAlreadyInstalled()) {
                    // Start scan normally
                    startActualScan()
                } else {
                    scope.launch {
                        SnackbarManager.showMessage("Preuzimanje komponenti za skeniranje...")
                    }
                }
            }
            .addOnFailureListener { e ->
                isModuleInstalling = false
                android.util.Log.e("CameraScreen", "Module installation failed", e)
                scope.launch {
                    SnackbarManager.showMessage("Inicijalizacija skenera nije uspela: ${e.localizedMessage}")
                }
            }
    }
    
    LaunchedEffect(hasCameraPermission, pendingScan) {
        if (hasCameraPermission && pendingScan) {
            pendingScan = false
            launchGoogleScanner()
        }
    }

    // Auto-launch scanner ONLY ONCE
    var hasAutoLaunched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasAutoLaunched) {
            hasAutoLaunched = true
            launchGoogleScanner()
        }
    }
    
    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        com.platisa.app.ui.components.AppBackground()
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
                onClick = { 
                    viewModel.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                    launchGoogleScanner() 
                },
                enabled = !isScanning && !isProcessing && !isModuleInstalling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning || isProcessing || isModuleInstalling) Color.Gray else NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isScanning || isProcessing || isModuleInstalling) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        when {
                            isModuleInstalling -> "INICIJALIZACIJA..."
                            isProcessing -> "OBRADA..."
                            else -> "SKENIRANJE..."
                        },
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
                    onClick = { 
                        viewModel.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                        galleryLauncher.launch("image/*") 
                    },
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
                    onClick = { 
                        viewModel.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                        showUrlDialog = true 
                    },
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
            Button(
                onClick = { 
                    viewModel.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
                    navController.navigate(Screen.Market.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Nazad u Market",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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

