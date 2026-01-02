package com.example.platisa.ui.screens.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.platisa.core.domain.model.Receipt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.platisa.ui.components.DynamicSizeText
import com.example.platisa.ui.components.PeriodDropdownSelector
import com.example.platisa.core.common.BaseScreen
import com.example.platisa.ui.navigation.Screen
import com.example.platisa.core.domain.model.PaymentStatus
// Removed hardcoded color imports - using LocalPlatisaColors instead
import com.example.platisa.ui.screens.help.tutorialTarget
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.platisa.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.platisa.ui.theme.LocalPlatisaColors
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val receipts by viewModel.receipts.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val epsDataMap by viewModel.epsDataMap.collectAsState()
    val connectedAccount by viewModel.connectedAccount.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val avatarPath by viewModel.avatarPath.collectAsState()
    val celebrationImagePath by viewModel.celebrationImagePath.collectAsState()
    val selectedPeriod by viewModel.selectedHomePeriod.collectAsState()
    val totalPaid by viewModel.totalPaid.collectAsState()
    val totalUnpaid by viewModel.totalUnpaid.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
    val customColors = LocalPlatisaColors.current
    
    // Auto-show tutorial for first-time users (NOW IN HOME SCREEN WITH DELAY)
    var showTrialPopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = viewModel.preferenceManager
        val hasSeenTutorial = prefs.hasSeenTutorial
        
        if (!hasSeenTutorial) {
            // WAIT for UI to fully load and settle (User Request)
            kotlinx.coroutines.delay(1500) 
            com.example.platisa.ui.screens.help.TutorialState.show()
            prefs.hasSeenTutorial = true
        } else if (!prefs.hasSeenTrialPopup) {
            // Tutorial already seen, but popup not -> Show Popup with slight delay
            kotlinx.coroutines.delay(1000)
            showTrialPopup = true
        }
    }
    
    // LazyListState for snap-to-top behavior
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Listen for scroll-to-receipt signal from BillDetails
    val scrollToReceiptId = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Long>("scrollToReceiptId")
    
    LaunchedEffect(scrollToReceiptId) {
        scrollToReceiptId?.observeForever { receiptId ->
            receiptId?.let {
                // Find the index of the receipt in the list
                val index = receipts.indexOfFirst { receipt -> receipt.id == receiptId }
                if (index >= 0) {
                    // Scroll to the receipt (+1 to account for header item)
                    scope.launch {
                        listState.animateScrollToItem(index)
                    }
                }
                // Clear the saved state
                navController.currentBackStackEntry?.savedStateHandle?.remove<Long>("scrollToReceiptId")
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Move heavy library init to background thread
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.platisa.core.common.PdfUtils.init(context)
        }
        viewModel.scheduleGmailSync()
    }

    // Refresh profile data when returning from Profile screen
    LaunchedEffect(navController.currentBackStackEntry) {
        viewModel.refreshProfileData()
    }

    var showGmailDialog by remember { mutableStateOf(true) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                try {
                    val data = result.data
                    if (data == null) return@launch
                    
                    val account = com.example.platisa.core.common.GoogleAuthManager.signIn(data)
                    if (account != null) {
                        // Exchange token for Firebase Auth (throws if fails)
                        com.example.platisa.core.common.GoogleAuthManager.firebaseAuthWithGoogle(account)
                        
                        // If we get here, it succeeded
                        viewModel.setConnectedAccount(account.email!!)
                        navController.navigate(Screen.ScanTimeframe.route)
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Greška: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    com.example.platisa.core.common.GoogleAuthManager.signOut(context) {}
                }
            }
        }
    }

    if (connectedAccount == null && showGmailDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGmailDialog = false }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main App Title above the dialog
                Text(
                    text = "Platiša",
                    style = MaterialTheme.typography.displaySmall,
                    color = customColors.neonCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // The Dialog Content Card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Povežite Gmail",
                            style = MaterialTheme.typography.headlineSmall,
                            color = customColors.neonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = "Za automatsko učitavanje računa, povežite svoj Gmail nalog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showGmailDialog = false }) {
                                Text("Kasnije", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    try {
                                        com.example.platisa.core.common.GoogleAuthManager.signOut(context) {
                                            val client = com.example.platisa.core.common.GoogleAuthManager.getSignInClient(context)
                                            launcher.launch(client.signInIntent)
                                        }
                                    } catch (e: Exception) { }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = customColors.neonCyan.copy(alpha = 0.2f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan)
                            ) {
                                Text("Poveži", color = customColors.neonCyan)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTrialPopup) {
        com.example.platisa.ui.screens.subscription.TrialPopup(
            onDismiss = {
                showTrialPopup = false
                viewModel.preferenceManager.hasSeenTrialPopup = true
            }
        )
    }

    // Snap-to-top scroll behavior - DISABLED (causes click issues)
    /*
    var lastScrollTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            lastScrollTime = System.currentTimeMillis()
        } else {
            // Only snap if there was actual scrolling (not just a tap)
            val timeSinceScrollStart = System.currentTimeMillis() - lastScrollTime
            
            // If scroll lasted > 100ms, it was real scrolling, not a tap
            if (timeSinceScrollStart > 100) {
                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                
                if (firstVisibleItem != null && firstVisibleItem.index > 0) {
                    val itemHeight = firstVisibleItem.size
                    val itemOffset = -firstVisibleItem.offset
                    val visibleHeight = itemHeight - itemOffset
                    val visibilityPercentage = (visibleHeight.toFloat() / itemHeight.toFloat()) * 100
                    
                    if (visibilityPercentage < 50) {
                        scope.launch {
                            listState.animateScrollToItem(
                                index = firstVisibleItem.index + 1,
                                scrollOffset = 0
                            )
                        }
                    } else {
                        scope.launch {
                            listState.animateScrollToItem(
                                index = firstVisibleItem.index,
                                scrollOffset = 0
                            )
                        }
                    }
                }
            }
        }
    }
    */

    BaseScreen(viewModel = viewModel) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // New Image Background
            com.example.platisa.ui.components.AppBackground()
            // Background gradient blur - ONLY behind header (stops before summary grid)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // EXACT header height only
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0288D1).copy(alpha = 0.3f), // Light Blue
                                Color(0xFF00E5FF).copy(alpha = 0.3f), // Cyan
                                Color(0xFFB3E5FC).copy(alpha = 0.3f)  // Pale Blue
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            radius = 500f
                        )
                    )
                    .blur(40.dp)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Header row with separate clickable zones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                        .padding(16.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable avatar + greeting section
                    
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .tutorialTarget("avatar_greeting")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                android.util.Log.d("HomeScreen", "Avatar/Greeting clicked! Navigating to Profile...")
                                navController.navigate(Screen.Profile.route)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .shadow(
                                            elevation = 15.dp,
                                            shape = CircleShape,
                                            spotColor = customColors.neonCyan.copy(alpha = 0.4f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(customColors.neonCyan, customColors.neonPurple),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val currentAvatarPath = avatarPath
                                        if (currentAvatarPath != null) {
                                            if (currentAvatarPath.startsWith("custom:")) {
                                                val file = java.io.File(currentAvatarPath.removePrefix("custom:"))
                                                androidx.compose.foundation.Image(
                                                    painter = coil.compose.rememberAsyncImagePainter(file),
                                                    contentDescription = "Avatar",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else if (currentAvatarPath.startsWith("predefined:")) {
                                                val resName = currentAvatarPath.removePrefix("predefined:")
                                                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                                                if (resId != 0) {
                                                    androidx.compose.foundation.Image(
                                                        painter = coil.compose.rememberAsyncImagePainter(resId),
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Avatar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            customColors.neonCyan.copy(alpha = 0.2f),
                                                            Color.Transparent
                                                        ),
                                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                        end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Greeting text
                                Text(
                                    text = "Zdravo, $userName!",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Icons on the right
                    Row {
                            IconButton(
                                onClick = { navController.navigate(Screen.Settings.createRoute("notifications")) }
                            ) {
                                val notifColor = if (isDarkTheme) com.example.platisa.ui.theme.IconBellDark else com.example.platisa.ui.theme.IconBellLight
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .tutorialTarget("notification_bell"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = CircleShape,
                                                spotColor = notifColor.copy(alpha = 0.4f)
                                            )
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        notifColor.copy(alpha = 0.15f),
                                                        notifColor.copy(alpha = 0.05f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        notifColor.copy(alpha = 0.6f),
                                                        notifColor.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    ),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                                )
                                            )
                                    )
                                    
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = notifColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Theme Toggle Icon  
                            IconButton(onClick = { viewModel.toggleTheme() }) {
                                val themeColor = if (isDarkTheme) com.example.platisa.ui.theme.IconThemeDark else com.example.platisa.ui.theme.IconThemeLight
                            Box(
                            modifier = Modifier
                                .size(40.dp)
                                    .tutorialTarget("theme_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = CircleShape,
                                                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        themeColor.copy(alpha = 0.15f),
                                                        themeColor.copy(alpha = 0.05f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        themeColor.copy(alpha = 0.6f),
                                                        themeColor.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        customColors.cardBackground.copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    ),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                                )
                                            )
                                    )
                                    
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Theme",
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Help Icon
                            IconButton(
                                onClick = { com.example.platisa.ui.screens.help.TutorialState.show() }
                            ) {
                                val helpColor = if (isDarkTheme) com.example.platisa.ui.theme.IconHelpDark else com.example.platisa.ui.theme.IconHelpLight
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .tutorialTarget("help_icon"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = CircleShape,
                                                spotColor = helpColor.copy(alpha = 0.4f)
                                            )
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        helpColor.copy(alpha = 0.15f),
                                                        helpColor.copy(alpha = 0.05f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        helpColor.copy(alpha = 0.6f),
                                                        helpColor.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Icon(
                                        imageVector = Icons.Default.Help,
                                        contentDescription = "Help",
                                        tint = helpColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            } // End Help IconButton
                    } // End Row (icons)
                } // End Row (header)

                // Fixed SummaryGrid at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SummaryGrid(
                        totalAmount = totalSpending,
                        totalPaid = totalPaid,
                        totalUnpaid = totalUnpaid,
                        currency = currency,
                        celebrationImagePath = celebrationImagePath,
                        onCameraClick = { navController.navigate(Screen.Camera.route) },
                        receipts = receipts // Still passing for other potential uses
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                
                // Fixed Title ("Nedavni Računi") and Dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Added 16.dp horizontal padding to match other content
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Nedavni Računi",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    
                    PeriodDropdownSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodChange = viewModel::setHomePeriod
                    )
                }

                // Scrollable bill cards section
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                   // Recent Activity Section


                    items(
                        items = receipts,
                        key = { it.id }
                    ) { receipt ->
                        // DEBUG LOG
                        android.util.Log.d("HomeScreen", "Receipt: ${receipt.merchantName}, Status: ${receipt.paymentStatus}")
                        
                        // Add tutorialTarget only to first bill card
                        val isFirstCard = receipts.indexOf(receipt) == 0
                        
                        ModernBillCard(
                            receipt = receipt,
                            epsConsumption = epsDataMap[receipt.id]?.totalConsumption?.toInt(),
                            onNavigateToDetails = { 
                                when (receipt.category) {
                                    com.example.platisa.core.domain.model.BillCategory.GROCERY,
                                    com.example.platisa.core.domain.model.BillCategory.PHARMACY,
                                    com.example.platisa.core.domain.model.BillCategory.RESTAURANT -> {
                                        navController.navigate(Screen.FiscalReceiptDetails.createRoute(receipt.id))
                                    }
                                    else -> {
                                        navController.navigate(Screen.BillDetails.createRoute(receipt.id.toString()))
                                    }
                                }
                            },
                            onConfirmPayment = { viewModel.markReceiptAsPaid(receipt.id) },
                            modifier = if (isFirstCard) {
                                Modifier
                                    .tutorialTarget("bill_card")
                                    .tutorialTarget("bill_card_details")
                            } else {
                                Modifier
                            }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun SummaryGrid(
    totalAmount: java.math.BigDecimal,
    totalPaid: java.math.BigDecimal,
    totalUnpaid: java.math.BigDecimal,
    currency: String,
    celebrationImagePath: String?,
    onCameraClick: () -> Unit,
    receipts: List<Receipt> = emptyList()
) {
    // Use LocalPlatisaColors.isDark for consistency with the rest of the app
    val isDark = LocalPlatisaColors.current.isDark
    
    if (isDark) {
        DarkSummaryGrid(
            totalAmount = totalAmount,
            totalPaid = totalPaid,
            totalUnpaid = totalUnpaid,
            currency = currency,
            celebrationImagePath = celebrationImagePath,
            onCameraClick = onCameraClick
        )
    } else {
        LightSummaryGrid(
            totalAmount = totalAmount,
            totalPaid = totalPaid,
            totalUnpaid = totalUnpaid,
            currency = currency,
            celebrationImagePath = celebrationImagePath,
            onCameraClick = onCameraClick
        )
    }
}

@Composable
fun DarkSummaryGrid(
    totalAmount: java.math.BigDecimal,
    totalPaid: java.math.BigDecimal,
    totalUnpaid: java.math.BigDecimal,
    currency: String,
    celebrationImagePath: String?,
    onCameraClick: () -> Unit
) {
    val displayPaid = remember(totalPaid) { totalPaid }
    val displayUnpaid = remember(totalUnpaid) { totalUnpaid }
    val customColors = LocalPlatisaColors.current
    // Slate 900 for fallback, but main texture comes from images
    val panelBackground = Color(0xFF0F172A) 

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 12.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = customColors.neonCyan.copy(alpha = 0.5f), 
                ambientColor = customColors.neonCyan.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp, 
                color = customColors.neonCyan.copy(alpha = 0.5f), 
                shape = RoundedCornerShape(16.dp)
            )
            .background(panelBackground),
        horizontalArrangement = Arrangement.Start
    ) {
        // --- Dark Gauge Panel ---
        // Explicit Dark Blue background for the Gauge area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0F172A)), // Ensure Dark Blue even if Row bg is clipped
            contentAlignment = Alignment.Center
        ) {

            
            GaugePanel(
                totalAmount = totalAmount,
                totalPaid = displayPaid,
                totalUnpaid = displayUnpaid,
                currency = currency,
                celebrationImagePath = celebrationImagePath,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // --- Divider ---
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            customColors.neonCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // --- Dark Camera Panel ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tutorialTarget("camera_button")
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imageRequest = remember(context) {
                ImageRequest.Builder(context)
                    .data(R.drawable.dark_camera_animation)
                    .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                    .build()
            }
            
            Image(
                painter = rememberAsyncImagePainter(model = imageRequest),
                contentDescription = "Slikaj QR",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun LightSummaryGrid(
    totalAmount: java.math.BigDecimal,
    totalPaid: java.math.BigDecimal,
    totalUnpaid: java.math.BigDecimal,
    currency: String,
    celebrationImagePath: String?,
    onCameraClick: () -> Unit
) {
    val displayPaid = remember(totalPaid) { totalPaid }
    val displayUnpaid = remember(totalUnpaid) { totalUnpaid }
    val customColors = LocalPlatisaColors.current
    val panelBackground = Color(0xFFE1F5FE) // Light Blue background

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(panelBackground),
        horizontalArrangement = Arrangement.Start
    ) {
        // --- Light Gauge Panel ---
        // Transparent box to show the Light Blue Row background
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
             GaugePanel(
                totalAmount = totalAmount,
                totalPaid = displayPaid,
                totalUnpaid = displayUnpaid,
                currency = currency,
                celebrationImagePath = celebrationImagePath,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // --- Divider ---
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.5f))
        )

        // --- Light Camera Panel ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tutorialTarget("camera_button")
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imageRequest = remember(context) {
                ImageRequest.Builder(context)
                    .data(R.drawable.light_animation)
                    .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                    .build()
            }
            
            Image(
                painter = rememberAsyncImagePainter(model = imageRequest),
                contentDescription = "Slikaj QR Light",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



@Composable
fun ModernBillCard(
    receipt: com.example.platisa.core.domain.model.Receipt,
    epsConsumption: Int?,
    onNavigateToDetails: () -> Unit,
    onConfirmPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = LocalPlatisaColors.current
    val isDark = isSystemInDarkTheme()
    val (bgColor, borderColor, iconBgColor) = when (receipt.paymentStatus) {
        PaymentStatus.UNPAID -> Triple(
            customColors.statusUnpaid.copy(alpha = 0.25f),
            customColors.statusUnpaid.copy(alpha = 0.8f),
            customColors.statusUnpaid.copy(alpha = 0.6f)
        )
        PaymentStatus.PROCESSING -> Triple(
            customColors.statusProcessing.copy(alpha = 0.25f),
            customColors.statusProcessing.copy(alpha = 0.8f),
            customColors.statusProcessing.copy(alpha = 0.6f)
        )
        PaymentStatus.PAID -> Triple(
            customColors.statusPaid.copy(alpha = 0.25f),
            customColors.statusPaid.copy(alpha = 0.8f),
            customColors.statusPaid.copy(alpha = 0.6f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onNavigateToDetails)
            .background(color = bgColor)
            .border(1.dp, if (isDark) borderColor.copy(alpha = 0.3f) else com.example.platisa.ui.theme.SolarBorderBrown, RoundedCornerShape(12.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glass3DIcon(
                    icon = receipt.category.icon ?: Icons.Default.Description,
                    backgroundColor = iconBgColor,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val displayName = if (receipt.merchantName.contains("EPS", ignoreCase = true)) {
                        "EPS"
                    } else {
                        receipt.merchantName
                    }
                    
                    DynamicSizeText(
                        text = displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tutorialTarget("bill_merchant_name"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        minFontSize = 14.sp,
                        maxFontSize = 18.sp,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start // Align Left
                    )
                    // Datum računa
                    if (receipt.paymentStatus == PaymentStatus.PAID) {
                        Text(
                            text = "Plaćeno",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (receipt.paymentDate != null) formatDate(receipt.paymentDate) else formatDate(receipt.date),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.tutorialTarget("bill_date")
                        )
                    } else {
                        Text(
                            text = formatDate(receipt.date),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.tutorialTarget("bill_date")
                        )
                    }
                    // Rok plaćanja - ako postoji
                    if (receipt.dueDate != null && receipt.paymentStatus != PaymentStatus.PAID) {
                        Text(
                            text = formatDate(receipt.dueDate),
                            fontSize = 13.sp,
                            color = customColors.neonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.tutorialTarget("bill_due_date")
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val formattedAmount = com.example.platisa.core.common.Formatters.formatCurrency(
                        receipt.totalAmount ?: java.math.BigDecimal.ZERO
                    ).substringBefore(",")

                    DynamicSizeText(
                        text = formattedAmount,
                        modifier = Modifier
                            .widthIn(max = 150.dp)
                            .tutorialTarget("bill_amount"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        minFontSize = 14.sp,
                        maxFontSize = 22.sp, // Reduced from 24sp to fit better
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End // Align Right
                    )
                    // Potrošnja (kWh) ispod iznosa
                    if (epsConsumption != null) {
                        Text(
                            text = "$epsConsumption kWh",
                            fontSize = 12.sp,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.tutorialTarget("bill_consumption")
                        )
                    }
                }
            }
            
            // Confirm Payment button for PROCESSING status
            if (receipt.paymentStatus == PaymentStatus.PROCESSING) {
                Button(
                    onClick = {
                        onConfirmPayment()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp) // Removed vertical padding to pull it up
                        .padding(bottom = 12.dp) // Add bottom margin only
                        .height(56.dp), // Increased height from 48dp to 56dp
                    colors = ButtonDefaults.buttonColors(
                        containerColor = customColors.statusPaid.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.statusPaid),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = customColors.statusPaid,
                        modifier = Modifier.size(24.dp) // Increased icon size
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POTVRDI PLAĆANJE",
                        color = customColors.statusPaid,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, // Increased font size from 16sp
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Glass3DIcon(
    icon: ImageVector,
    backgroundColor: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.White.copy(alpha = 0.1f),
                ambientColor = Color.White.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                )
            )
            .background(backgroundColor)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        backgroundColor
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(50f, 50f)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner bottom-right glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .offset(x = 10.dp, y = 10.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 40f
                    )
                )
        )
        
        // Top shine overlay (simulating clip-path polygon from HTML)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Glass highlight edge
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(100f, 100f)
                    )
                )
        )

        // 3D Levitation/Structure Effect for the Icon Symbol
        Box(contentAlignment = Alignment.Center) {
            // 1. Drop Shadow (Soft, far) - Gives "floating" height
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 3.dp, y = 4.dp)
                    .blur(radius = 3.dp) 
            )

            // 2. Extrusion / Hard Edge (Close, darker) - Gives "thickness"
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 1.dp, y = 1.dp)
            )

            // 3. Main Icon (White Bright)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            // 4. Inner Light Specular (Top-Left) - Subtle emboss highlight
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f), // Faint white
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = (-1).dp, y = (-1).dp)
                    // Clip to keep it "inside" - tricky with vectors, so just soft highlight is enough
            )
        }
    }
}

private fun formatDate(date: java.util.Date): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
}
