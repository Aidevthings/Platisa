package com.example.platisa.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.platisa.ui.navigation.Screen
import com.example.platisa.ui.theme.NeonCyan
import com.example.platisa.ui.theme.VoidBackground
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.platisa.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow // Added
import kotlinx.coroutines.launch // Added
import kotlinx.coroutines.flow.flowOn // Added
import kotlinx.coroutines.flow.debounce // Added
import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.FlowPreview // Added

@HiltViewModel
@OptIn(FlowPreview::class)
class SyncWaitViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val workManager: androidx.work.WorkManager,
    private val preferenceManager: com.example.platisa.core.data.preferences.PreferenceManager
) : ViewModel() {
    
    private val _syncStatus = kotlinx.coroutines.flow.MutableStateFlow<String>("Pokrećem sinhronizaciju...")
    val syncStatus = _syncStatus.asStateFlow()

    private val _isSyncComplete = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncComplete = _isSyncComplete.asStateFlow()

    init {
        observeSyncWork()
    }

    private fun observeSyncWork() {
        val workInfoFlow = workManager.getWorkInfosForUniqueWorkFlow("GmailSyncOneTime")

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            workInfoFlow.collect { workInfoList ->
                val workInfo = workInfoList.firstOrNull()
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress
                            val status = progress.getString("status") // Assuming worker publishes this
                            _syncStatus.value = if (status == "syncing") "Skeniram emailove..." else "Obrada podataka..."
                        }
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val count = workInfo.outputData.getInt(com.example.platisa.core.worker.GmailSyncWorker.KEY_NEW_RECEIPTS, 0)
                            _syncStatus.value = "Završeno! Pronađeno $count računa."
                            _isSyncComplete.value = true
                        }
                        androidx.work.WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(com.example.platisa.core.worker.GmailSyncWorker.KEY_ERROR_MESSAGE) ?: "Nepoznata greška"
                            _syncStatus.value = "Greška: $error"
                            _isSyncComplete.value = true 
                            // Let's rely on receipt count as backup
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private var initialCount = -1

    // Tracks ONLY the new receipts found during this specific session
    val newReceiptsCount = repository.getAllReceipts()
        .map { receipts ->
            val currentTotal = receipts.size
            if (initialCount == -1) {
                // First emission: capture the baseline count before sync adds new ones
                initialCount = currentTotal
            }
            // Return only the difference (newly added bills)
            (currentTotal - initialCount).coerceAtLeast(0)
        }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .debounce(300L) // Debounce UI updates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    // Expose preference manager for UI to check
    val prefs = preferenceManager
}

@Composable
fun SyncWaitScreen(
    navController: NavController,
    viewModel: SyncWaitViewModel = hiltViewModel()
) {
    val newReceiptsCount by viewModel.newReceiptsCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncComplete by viewModel.isSyncComplete.collectAsState()
    
    // Safety Timeout: If sync takes too long (> 30 seconds), force proceed
    var timeoutTriggered by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(30_000) // 30 seconds max wait
        timeoutTriggered = true
    }

    // Logic to proceed
    // FIX: Wait for FULL sync completion (or timeout). Do not exit early just because we have receipts (receiptCount > 0),
    // because existing users adding a 2nd account already have receipts. We want to show them the new scan.
    LaunchedEffect(isSyncComplete, timeoutTriggered) {
        if (isSyncComplete || timeoutTriggered) {
            if (!timeoutTriggered) delay(2000) // Show success briefly if not timeout
            
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.SyncWait.route) { inclusive = true }
             }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Background Image (Splash Screen)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.platisa.R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // 2. Dark Overlay - REMOVED per user request for layer "just under text"
        // But keeping it very subtle (0.1f) for overall contrast if needed, or removing entirely.
        // User asked for "layer just under the text". So let's wrap the text container.
        
        // Unified "Popup" Card Style to match Login/ScanTimeframe overlays
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulse Animation or Loader - Resized to 48.dp to match other screens
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Main Status Text
                Text(
                    text = if (newReceiptsCount > 0) "Pronađeno računa: $newReceiptsCount" else syncStatus,
                    fontSize = 20.sp, // Slightly reduced from 24sp for better alignment
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Text(
                    text = "Ovo može potrajati nekoliko trenutaka",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        }
    }

