package com.example.platisa.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platisa.ui.theme.SubscriptionCardDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.platisa.R
import com.example.platisa.core.common.BaseScreen
import com.example.platisa.core.common.GoogleAuthManager
import com.example.platisa.core.common.NetworkUtils
import com.example.platisa.ui.navigation.Screen
import kotlinx.coroutines.launch

import com.example.platisa.ui.theme.LocalPlatisaColors

// --- typography & effects ---
val ReadableFont = FontFamily.Default

// TextStyles moved to inside composables to access Theme colors

@Composable
fun SettingsScreen(
    navController: NavController,
    openSection: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customColors = LocalPlatisaColors.current
    
    // Define styles locally to access Theme
    val ShadowTitle = Shadow(
        color = customColors.neonCyan.copy(alpha = 0.6f),
        offset = Offset(1f, 1f),
        blurRadius = 4f
    )
    val ShadowDropdownText = Shadow(
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )
    
    val TextStyleSectionTitle = TextStyle(
        fontFamily = ReadableFont,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = customColors.neonCyan,
        shadow = ShadowTitle
    )
    
    val TextStyleDropdownTitle = TextStyle(
        fontFamily = ReadableFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        shadow = ShadowDropdownText
    )
    
    // --- Logic & State (Preserved) ---
    var isAccountConfiguring by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isAccountConfiguring = true
            scope.launch {
                try {
                    val data = result.data
                    if (data != null) {
                        val account = GoogleAuthManager.signIn(data)
                        if (account != null) {
                            GoogleAuthManager.firebaseAuthWithGoogle(account)
                            Toast.makeText(context, "Gmail povezan: ${account.email}", Toast.LENGTH_SHORT).show()
                            viewModel.setConnectedAccount(account.email)
                            
                            // NAVIGATE TO SCAN TIMEFRAME SCREEN
                            // This ensures user can choose the lookback period (1, 3, 6 months) for the new account,
                            // rather than just defaulting to a standard sync.
                            navController.navigate(Screen.ScanTimeframe.route)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Greška: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAccountConfiguring = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.importCsv(uri)
    }
    
    // Listen for Force Logout
    LaunchedEffect(Unit) {
        viewModel.checkConnectedAccount()
        viewModel.forceLogoutEvent.collect {
            navController.navigate(Screen.Login.createRoute(autoLogin = false)) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Battery Optimization Logic
    DisposableEffect(Lifecycle.Event.ON_RESUME) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkBatteryOptimization()
            }
        }
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }


    // --- UI Structure ---
    BaseScreen(viewModel = viewModel) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            com.example.platisa.ui.components.AppBackground()
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Bar
                TopBar(
                    onBackClick = { navController.navigateUp() },
                    title = "Podešavanja"
                )

                // 2. Content List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    // --- SECTION: KORISNIK ---
                    item {
                        Text(text = "KORISNIK", style = TextStyleSectionTitle, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    
                    // Gmail Accounts
                    item {
                        val connectedAccounts by viewModel.connectedAccounts.collectAsState()
                        var expanded by remember { mutableStateOf(false) }
                        
                        SettingsDropdown(
                            title = "Gmail Nalozi",
                            icon = Icons.Filled.Email,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            if (connectedAccounts.isEmpty()) {
                                SettingsItemCard {
                                    Text("Nema povezanih naloga", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                                }
                            } else {
                                connectedAccounts.forEach { email ->
                                    SettingsItemCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(email, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                            IconButton(onClick = { viewModel.removeAccount(email) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = customColors.error.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // Add Account Button
                            SettingsItemCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (NetworkUtils.isNetworkAvailable(context)) {
                                                GoogleAuthManager.signOut(context) {
                                                    val client = GoogleAuthManager.getSignInClient(context)
                                                    launcher.launch(client.signInIntent)
                                                }
                                            } else {
                                                Toast.makeText(context, "Nema internet konekcije!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = customColors.neonCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Dodaj Nalog", color = customColors.neonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Subscription (Restored)
                    item {
                        val subStatus by viewModel.subscriptionStatus.collectAsState()
                        // Using a simple Clickable Panel for navigation
                        SettingsGlassPanel(
                            modifier = Modifier.clickable { navController.navigate("subscription_paywall") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CardMembership, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(text = "Pretplata", style = TextStyleDropdownTitle)
                                    Text(
                                        text = if (subStatus == "TRIAL") "Status: Probni period" else "Status: $subStatus",
                                        fontSize = 12.sp,
                                        color = if (subStatus == "TRIAL") customColors.neonCyan else customColors.success
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }


                    // --- SECTION: APLIKACIJA ---
                    item {
                        Text(text = "APLIKACIJA", style = TextStyleSectionTitle, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }

                    // Sync Options
                    item {
                        val syncOnWifi by viewModel.syncOnWifi.collectAsState()
                        val syncOnMobile by viewModel.syncOnMobileData.collectAsState()
                        var expanded by remember { mutableStateOf(false) }

                        SettingsDropdown(
                            title = "Sinhronizacije Putem",
                            icon = Icons.Filled.Wifi,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            SettingsItemCard {
                                SwitchRow(
                                    label = "Wi-Fi mreže",
                                    checked = syncOnWifi,
                                    onCheckedChange = { viewModel.toggleSyncOnWifi(it) }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard {
                                SwitchRow(
                                    label = "Mobilni podaci",
                                    checked = syncOnMobile,
                                    onCheckedChange = { viewModel.toggleSyncOnMobileData(it) }
                                )
                            }
                        }
                    }

                    // Preferences
                    item {
                        val biometric by viewModel.biometricEnabled.collectAsState()
                        val currency by viewModel.currency.collectAsState()
                        var expanded by remember { mutableStateOf(false) }

                        SettingsDropdown(
                            title = "Preferencije",
                            icon = Icons.Filled.Settings,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            SettingsItemCard {
                                SwitchRow(
                                    label = "Biometrija",
                                    checked = biometric,
                                    onCheckedChange = { viewModel.toggleBiometric(it) }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Valuta", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                    Button(
                                        onClick = { viewModel.setCurrency(if (currency == "RSD") "EUR" else "RSD") },
                                        colors = ButtonDefaults.buttonColors(containerColor = customColors.neonCyan),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(currency, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Notifications
                    item {
                        val notifyDue3 by viewModel.notifyDue3Days.collectAsState()
                        val notifyDue1 by viewModel.notifyDue1Day.collectAsState()
                        val notifyOverdue by viewModel.notifyOverdue.collectAsState()
                        val notifyDup by viewModel.notifyDuplicate.collectAsState()
                        var expanded by remember { mutableStateOf(openSection == "notifications") }

                        SettingsDropdown(
                            title = "Obaveštenja",
                            icon = Icons.Filled.Notifications,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            SettingsItemCard { SwitchRow("3 dana pre roka", notifyDue3, { viewModel.toggleNotifyDue3Days(it) }) }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard { SwitchRow("1 dan pre roka", notifyDue1, { viewModel.toggleNotifyDue1Day(it) }) }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard { SwitchRow("Prekoračenje roka", notifyOverdue, { viewModel.toggleNotifyOverdue(it) }, color = customColors.error) }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard { SwitchRow("Potencijalni Duplikati", notifyDup, { viewModel.toggleNotifyDuplicate(it) }, color = customColors.error) }
                        }
                    }
                    
                    // Background Sync (Battery)
                    item {
                        val isIgnoringBattery by viewModel.isIgnoringBatteryOptimizations.collectAsState()
                        var expanded by remember { mutableStateOf(!isIgnoringBattery) } // Auto expand if issue

                        SettingsDropdown(
                            title = "Pozadinska Sinhro",
                            icon = Icons.Filled.Sync,
                            titleFontSize = 16.sp, // Requested smaller font
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                           if (!isIgnoringBattery) {
                               SettingsItemCard {
                                   Column {
                                       Text(
                                           "Za pouzdanu sinhronizaciju, isključite optimizaciju baterije.",
                                           color = MaterialTheme.colorScheme.onSurfaceVariant,
                                           fontSize = 16.sp,
                                           fontWeight = FontWeight.Bold,
                                           modifier = Modifier.padding(bottom = 12.dp)
                                       )
                                       Button(
                                           onClick = {
                                               try {
                                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                               } catch(e: Exception) {}
                                           },
                                           colors = ButtonDefaults.buttonColors(containerColor = customColors.error),
                                           modifier = Modifier.fillMaxWidth(),
                                           shape = RoundedCornerShape(8.dp)
                                       ) {
                                           Text("Reši Problem (Baterija)", color = customColors.onError, fontWeight = FontWeight.Bold)
                                       }
                                   }
                               }
                           } else {
                               SettingsItemCard {
                                   Row(verticalAlignment = Alignment.CenterVertically) {
                                       Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = customColors.success)
                                       Spacer(Modifier.width(8.dp))
                                       Text("Status: Optimizovano (Spremno)", color = customColors.success, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                   }
                               }
                           }
                        }
                    }

                    // --- SECTION: SISTEM ---
                    item {
                        Text(text = "SISTEM", style = TextStyleSectionTitle, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }

                    // Data Management
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        
                        SettingsDropdown(
                            title = "Upravljanje Podacima",
                            icon = Icons.Filled.Storage,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            SettingsItemCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.exportCsv(context) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.FileDownload, contentDescription = null, tint = customColors.neonCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Izvezi u CSV", color = customColors.neonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { importLauncher.launch("text/*") },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = customColors.neonCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Uvezi iz CSV-a", color = customColors.neonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            SettingsItemCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.exportPdf(context) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = customColors.neonCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Izvezi u PDF", color = customColors.neonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // --- SECTION: PODRŠKA ---
                    item {
                        Text(text = "PODRŠKA", style = TextStyleSectionTitle, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }

                    // Report Error
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        var messageText by remember { mutableStateOf("") }
                        
                        SettingsDropdown(
                            title = "Prijavi Grešku",
                            icon = Icons.Filled.BugReport,
                            expanded = expanded,
                            onToggle = { expanded = !expanded }
                        ) {
                            Text(
                                text = "Opišite problem koji ste primetili:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), // Increased Input Font
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = customColors.neonCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                placeholder = { Text("Unesite tekst ovde...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) } // Increased Placeholder
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Button(
                                onClick = { 
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendBugReport(context, messageText)
                                        messageText = "" // Clear field
                                    } else {
                                        Toast.makeText(context, "Unesite opis problema.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = customColors.neonCyan)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null, tint = customColors.textOnPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pošalji", color = customColors.textOnPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // --- ACTIONS (Buttons) ---
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sync Now Button
                            OutlinedButton(
                                onClick = { viewModel.syncNow(); Toast.makeText(context, "Sinhronizacija pokrenuta...", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan),
                                colors = ButtonDefaults.outlinedButtonColors(
// Replace surfaceContainerHigh with surfaceVariant
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = customColors.neonCyan
                                )
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, tint = customColors.neonCyan)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Sinhronizuj Sada",
                                    style = TextStyle(
                                        fontFamily = ReadableFont,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Reset Sync Button
                            OutlinedButton(
                                onClick = {
                                    viewModel.resetGmailSync() 
                                    // ViewModel shows snackbar
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.error.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = customColors.error.copy(alpha = 0.9f)
                                )
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, tint = customColors.error.copy(alpha = 0.9f))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Reset Gmail Sync",
                                    style = TextStyle(
                                        fontFamily = ReadableFont,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun TopBar(onBackClick: () -> Unit, title: String) {
    val auroraBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6A008A).copy(alpha = 0.4f),
            Color(0xFF007bff).copy(alpha = 0.3f),
            Color(0xFF39FF14).copy(alpha = 0.2f)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f) // Approximate diagonal
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(auroraBrush)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.shadow(4.dp, shape = androidx.compose.foundation.shape.CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Nazad", tint = MaterialTheme.colorScheme.onBackground)
        }
        
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.CenterHorizontally),
            style = TextStyle(
                fontFamily = ReadableFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    offset = Offset(1f, 1f),
                    blurRadius = 4f
                )
            )
        )
        
        // Spacer for balance
        Spacer(Modifier.size(48.dp)) 
    }
}

@Composable
fun SettingsGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val customColors = LocalPlatisaColors.current
    val isDark = customColors.isDark
    
    val backgroundModifier = if (isDark) {
        Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        customColors.neonCyan.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(12.dp)
            )
    } else {
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), spotColor = if(isDark) customColors.neonCyan.copy(alpha=0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SettingsDropdown(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    val customColors = LocalPlatisaColors.current
    val isDark = customColors.isDark
    
    val backgroundModifier = if (isDark) {
         Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        customColors.neonCyan.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(12.dp)
            )
    } else {
         Modifier
            .background(com.example.platisa.ui.theme.SolarSurface, RoundedCornerShape(12.dp)) // Solar Surface equivalent/Light Brown
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    }

    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    val ShadowDropdownText = Shadow(
        color = textColor.copy(alpha = 0.5f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )
    val TextStyleDropdownTitle = TextStyle(
        fontFamily = ReadableFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = textColor,
        shadow = ShadowDropdownText
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            // Shadow can be tricky with Columns if not clipped, but we'll try standard modifier order
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), spotColor = if(isDark) customColors.neonCyan.copy(alpha=0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = TextStyleDropdownTitle.copy(fontSize = titleFontSize),
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.rotate(rotation)
            )
        }
        
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Separator line maybe?
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(bottom = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}



@Composable
fun SettingsItemCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val customColors = LocalPlatisaColors.current
    val isDark = customColors.isDark
    
    // 3D Effect styles - Slick & Compact
    val shape = RoundedCornerShape(12.dp) // Slightly tighter corners
    val shadowElevation = 4.dp // Reduced elevation for slicker look
    
    val backgroundModifier = if (isDark) {
        Modifier
            .background(
                color = Color(0xFF2A2A2A), 
                shape = shape
            )
            .border(
                width = 1.dp, // Thinner border
                brush = Brush.verticalGradient(
                    colors = listOf(
                        customColors.neonCyan.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    } else {
        Modifier
            .background(
                color = com.example.platisa.ui.theme.SolarSurface, 
                shape = shape
            )
            .border(
                width = 1.dp, // Thinner border
                brush = Brush.verticalGradient(
                    colors = listOf(
                        com.example.platisa.ui.theme.SolarBorderBrown.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = if (isDark) customColors.neonCyan.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
            )
            .then(backgroundModifier)
            .padding(6.dp), // Ultra-compact padding
        content = content
    )
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val customColors = LocalPlatisaColors.current
        val switchColor = color ?: customColors.neonCyan
        Text(
            label, 
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(
                fontFamily = ReadableFont,
                fontSize = 19.sp, // Increased font size
                fontWeight = FontWeight.SemiBold // Bolder text
            )
        )
        Switch(
            modifier = Modifier.scale(1.1f), // Slightly larger switch
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = switchColor,
                checkedTrackColor = switchColor.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
