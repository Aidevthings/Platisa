package com.platisa.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.BaseViewModel
import com.platisa.app.core.common.SnackbarManager
import com.platisa.app.core.domain.manager.ExportManager
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.data.repository.FirestoreRepository
import com.platisa.app.core.data.network.FeedbackApi
import com.platisa.app.core.data.network.FeedbackRequest
import com.platisa.app.core.domain.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val repository: ReceiptRepository,
    private val secureStorage: SecureStorage,
    private val workManager: androidx.work.WorkManager,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val notificationScheduler: com.platisa.app.core.notification.NotificationScheduler,
    private val firestoreRepository: FirestoreRepository,
    private val feedbackApi: FeedbackApi,
    private val vibrationHelper: com.platisa.app.core.common.VibrationHelper
) : BaseViewModel() {

    private val _currency = MutableStateFlow(secureStorage.getCurrency())
    val currency = _currency.asStateFlow()

    fun vibrate(type: com.platisa.app.core.common.VibrationHelper.HapticType) {
        vibrationHelper.vibrate(type)
    }

    private val _isDarkTheme = MutableStateFlow(preferenceManager.isDarkTheme)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(preferenceManager.hapticEnabled)
    val hapticEnabled = _hapticEnabled.asStateFlow()

    private val _connectedEmail = MutableStateFlow<String?>(null)
    val connectedEmail = _connectedEmail.asStateFlow()

    private val _connectedAccounts = MutableStateFlow<Set<String>>(emptySet())
    val connectedAccounts = _connectedAccounts.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()



    // Notification preferences
    private val _notifyDue3Days = MutableStateFlow(preferenceManager.notifyDue3Days)
    val notifyDue3Days = _notifyDue3Days.asStateFlow()

    private val _notifyDue1Day = MutableStateFlow(preferenceManager.notifyDue1Day)
    val notifyDue1Day = _notifyDue1Day.asStateFlow()

    private val _notifyOverdue = MutableStateFlow(preferenceManager.notifyOverdue)
    val notifyOverdue = _notifyOverdue.asStateFlow()

    private val _notifyDuplicate = MutableStateFlow(preferenceManager.notifyDuplicate)
    val notifyDuplicate = _notifyDuplicate.asStateFlow()

    private val _notificationTimeHour = MutableStateFlow(preferenceManager.notificationTimeHour)
    val notificationTimeHour = _notificationTimeHour.asStateFlow()

    private val _notificationTimeMinute = MutableStateFlow(preferenceManager.notificationTimeMinute)
    val notificationTimeMinute = _notificationTimeMinute.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow(preferenceManager.subscriptionStatus)
    val subscriptionStatus = _subscriptionStatus.asStateFlow()

    init {
        checkConnectedAccount()
        loadConnectedAccounts()
        observeSyncWork()

        notificationScheduler.scheduleNotificationChecks() // Schedule notifications on app start
    }



    private fun observeSyncWork() {
        val workInfoFlow = workManager.getWorkInfosForUniqueWorkFlow("GmailSyncOneTime")
        
        launchCatching(showLoading = false) {
            workInfoFlow.collect { workInfoList ->
                val workInfo = workInfoList.firstOrNull()
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress
                            val status = progress.getString("status")
                            _syncStatus.value = if (status == "syncing") "Sinhronizacija u toku..." else "Obrada..."
                        }
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val count = workInfo.outputData.getInt(com.platisa.app.core.worker.GmailSyncWorker.KEY_NEW_RECEIPTS, 0)
                            _syncStatus.value = "Završeno: $count novih računa"
                            kotlinx.coroutines.delay(3000)
                            _syncStatus.value = null
                        }
                        androidx.work.WorkInfo.State.FAILED -> {

                            val error = workInfo.outputData.getString(com.platisa.app.core.worker.GmailSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
                            if (error.contains("Duplikat", ignoreCase = true) || error.contains("Duplicate", ignoreCase = true)) {
                                _syncStatus.value = "Info: Već postoje računi ($error)"
                            } else {
                                _syncStatus.value = "Greška: $error"
                            }
                            kotlinx.coroutines.delay(5000)
                            _syncStatus.value = null
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun syncNow() {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        launchCatching(showLoading = false) {
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.platisa.app.core.worker.GmailSyncWorker>()
                .setInputData(androidx.work.workDataOf(
                    "force_full_sync" to false,
                    "lookback_days" to 90
                ))
                .build()
            
            workManager.enqueueUniqueWork(
                "GmailSyncOneTime",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }

    fun checkConnectedAccount() {
        val account = com.platisa.app.core.common.GoogleAuthManager.getSignedInAccount(context)
        // Just ensure valid accounts are in our list
        if (account?.email != null) {
            secureStorage.addConnectedAccount(account.email!!)
        }
        loadConnectedAccounts()
    }
    
    fun loadConnectedAccounts() {
        _connectedAccounts.value = secureStorage.getConnectedAccounts()
    }
    
    fun addAccount(email: String) {
        secureStorage.addConnectedAccount(email)
        loadConnectedAccounts()
        scheduleGmailSync()
        syncNow() // Trigger immediate scan for the new account
    }
    
    fun setConnectedAccount(email: String?) {
        _connectedEmail.value = email // Keep for UI "just added" feedback if needed, but logic shouldn't rely on it
        if (email != null) {
            addAccount(email)
        }
    }
    
    fun setCurrency(newCurrency: String) {
        secureStorage.setCurrency(newCurrency)
        _currency.value = newCurrency
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }

    fun toggleHaptic(enabled: Boolean) {
        preferenceManager.hapticEnabled = enabled
        _hapticEnabled.value = enabled
        if (enabled) vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.SUCCESS)
    }

    fun toggleTheme(isDark: Boolean) {
        preferenceManager.isDarkTheme = isDark
        _isDarkTheme.value = isDark
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }
    
    private val _forceLogoutEvent = kotlinx.coroutines.flow.MutableSharedFlow<Boolean>()
    val forceLogoutEvent = _forceLogoutEvent.asSharedFlow()

    fun removeAccount(email: String) {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        secureStorage.removeConnectedAccount(email)
        
        // Multi-Account Logic: We don't sign out the global session just because one account is removed.
        // We only wipe if ALL accounts are gone.
        
        loadConnectedAccounts()
        
        // ZERO-ACCOUNT POLICY:
        // If no accounts remain, we MUST wipe data and force logout.
        if (secureStorage.getConnectedAccounts().isEmpty()) {
            launchCatching {
                // AWAIT wipe completion before navigating (fixes race condition)
                performFullWipeSuspend()
                
                // Only emit logout event AFTER wipe is complete
                _forceLogoutEvent.emit(true)
            }
        }
    }
    
    // Renamed from resetGmailSync and generalized
    fun performFullWipe(silent: Boolean = false) {
        launchCatching {
            android.util.Log.d("SettingsViewModel", "🗑️ STARTING FULL DATA WIPE...")
            
            // 0. SIGN OUT of Google and Firebase FIRST (prevents auto-login on restart)
            com.platisa.app.core.common.GoogleAuthManager.signOut(context) {
                android.util.Log.d("SettingsViewModel", "✅ Signed out of Google/Firebase")
            }
            
            // 1. BULK DELETE all database tables (fast & reliable)
            repository.deleteAllReceiptItems() // Delete items first (foreign key)
            repository.deleteAllEpsData()       // Delete EPS data
            repository.deleteAllReceipts()      // Delete all receipts
            
            // 2. Clear connected accounts and sync timestamp
            secureStorage.clearAllData()
            
            // 3. Reset the local state
            _connectedAccounts.value = emptySet()
            _connectedEmail.value = null
            
            // 4. Delete all cached files (Gmail attachments, images)
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SettingsViewModel", "Failed to delete cache file: ${file.name}")
                }
            }
            
            // 5. Reset tutorial flag
            preferenceManager.hasSeenTutorial = false
            
            android.util.Log.d("SettingsViewModel", "✅ FULL DATA WIPE COMPLETE")
            
            if (!silent) {
                SnackbarManager.showMessage("Svi podaci su obrisani! Prijavite se ponovo.")
            }
        }
    }
    
    /**
     * Suspending version of performFullWipe that can be awaited.
     * Use this when you need to ensure wipe completes before continuing.
     */
    private suspend fun performFullWipeSuspend() {
        android.util.Log.d("SettingsViewModel", "🗑️ STARTING FULL DATA WIPE (suspend)...")
        
        // 0. CANCEL ALL WORKMANAGER JOBS (fixes stale "16 bills found" issue)
        workManager.cancelAllWork()
        workManager.pruneWork() // Remove completed/cancelled work from database
        android.util.Log.d("SettingsViewModel", "✅ Cancelled all WorkManager jobs")
        
        // 1. SIGN OUT of Google and Firebase
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            com.platisa.app.core.common.GoogleAuthManager.signOut(context) {
                android.util.Log.d("SettingsViewModel", "✅ Signed out of Google/Firebase")
                continuation.resume(Unit) {}
            }
        }
        
        // 2. BULK DELETE all database tables (fast & reliable)
        repository.deleteAllReceiptItems()
        repository.deleteAllEpsData()
        repository.deleteAllReceipts()
        
        // 3. Clear connected accounts and sync timestamp
        secureStorage.clearAllData()
        
        // 4. Reset the local state
        _connectedAccounts.value = emptySet()
        _connectedEmail.value = null
        
        // 5. Delete all cached files
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            } catch (e: Exception) {
                android.util.Log.w("SettingsViewModel", "Failed to delete cache file: ${file.name}")
            }
        }
        
        // 6. Reset preferences
        preferenceManager.hasSeenTutorial = false
        
        android.util.Log.d("SettingsViewModel", "✅ FULL DATA WIPE COMPLETE (suspend)")
    }

    
    fun resetGmailSync() {
        performSyncReset()
    }

    private fun performSyncReset() {
        launchCatching {
            android.util.Log.d("SettingsViewModel", "🔄 STARTING SYNC RESET (Keep Auth)...")
            
            // 0. Cancel any running syncs to prevent database contention
            workManager.cancelUniqueWork("GmailSync")
            workManager.cancelUniqueWork("GmailSyncOneTime")

            // 1. Clear ONLY Gmail receipts (preserves manual/camera scans)
            // Note: EpsData and ReceiptItems are automatically deleted via CASCADE
            repository.deleteGmailReceipts()
            
            // 2. Clear ONLY cache files (Gmail PDFs)
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("gmail_")) {
                    try { file.delete() } catch (e: Exception) {}
                }
            }
            
            // 3. Reset Timestamp to 0 to trigger fresh sync
            secureStorage.setLastGmailSyncTimestamp(0)
            
            android.util.Log.d("SettingsViewModel", "✅ Gmail data cleared. Triggering re-sync...")
            SnackbarManager.showMessage("Gmail sinhronizacija resetovana. Ručni unosi sačuvani.")
            
            // 4. Trigger immediate sync
            syncNow() 
            
            // 5. Re-schedule periodic sync after this one finishes (or concurrently)
            scheduleGmailSync()
        }
    }

    // ========== HARD RESET (Testing Only) ==========
    private val _hardResetResult = MutableStateFlow<String?>(null)
    val hardResetResult = _hardResetResult.asStateFlow()
    
    private val _isResetting = MutableStateFlow(false)
    val isResetting = _isResetting.asStateFlow()
    
    /**
     * Hard reset for testing purposes. Clears ALL data WITHOUT logging out.
     * - Clears Firestore paid statuses
     * - Resets per-account sync timestamps
     * - Deletes all local receipts and EPS data
     */
    fun hardReset() {
        viewModelScope.launch {
            _isResetting.value = true
            _hardResetResult.value = "⏳ Starting hard reset..."
            
            try {
                // 0. Cancel any running syncs
                workManager.cancelUniqueWork("GmailSync")
                workManager.cancelUniqueWork("GmailSyncOneTime")
                
                // 1. Get all connected accounts
                val accounts = secureStorage.getConnectedAccounts()
                android.util.Log.d("SettingsViewModel", "🗑️ HARD RESET: Found ${accounts.size} accounts")
                
                // 2. Clear Firestore paid statuses for each account
                accounts.forEach { email ->
                    android.util.Log.d("SettingsViewModel", "🗑️ Clearing Firestore for: $email")
                    firestoreRepository.deleteAllPaidStatuses(email)
                }
                _hardResetResult.value = "⏳ Firestore cleared..."
                
                // 3. Clear per-account sync timestamps
                accounts.forEach { email ->
                    secureStorage.setLastGmailSyncTimestamp(email, 0L)
                }
                // Also clear global timestamp
                secureStorage.setLastGmailSyncTimestamp(0L)
                _hardResetResult.value = "⏳ Sync timestamps reset..."
                
                // 4. Delete all local data
                repository.deleteAllReceiptItems()
                repository.deleteAllEpsData()
                repository.deleteAllReceipts()
                _hardResetResult.value = "⏳ Local database cleared..."
                
                // 5. Clear cache files
                val cacheDir = context.cacheDir
                cacheDir.listFiles()?.forEach { file ->
                    try {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    } catch (_: Exception) {}
                }
                
                android.util.Log.d("SettingsViewModel", "✅ HARD RESET COMPLETE")
                _hardResetResult.value = "✅ Hard Reset Complete!\n" +
                    "• Firestore paid statuses: CLEARED\n" +
                    "• Sync timestamps: RESET\n" +
                    "• Local receipts: DELETED\n" +
                    "• Cache: CLEARED\n\n" +
                    "Ready for fresh scan!"
                    
                SnackbarManager.showMessage("Hard reset complete! Ready for fresh scan.")
                    
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "❌ HARD RESET FAILED", e)
                _hardResetResult.value = "❌ Error: ${e.message}"
            } finally {
                _isResetting.value = false
            }
        }
    }

    private val _syncOnWifi = MutableStateFlow(secureStorage.getSyncOnWifi())
    val syncOnWifi = _syncOnWifi.asStateFlow()

    private val _syncOnMobileData = MutableStateFlow(secureStorage.getSyncOnMobileData())
    val syncOnMobileData = _syncOnMobileData.asStateFlow()

    fun toggleSyncOnWifi(enabled: Boolean) {
        secureStorage.setSyncOnWifi(enabled)
        _syncOnWifi.value = enabled
        scheduleGmailSync()
    }

    fun toggleSyncOnMobileData(enabled: Boolean) {
        secureStorage.setSyncOnMobileData(enabled)
        _syncOnMobileData.value = enabled
        scheduleGmailSync()
    }

    fun scheduleGmailSync() {
        val networkType = if (_syncOnMobileData.value) {
            androidx.work.NetworkType.CONNECTED // WiFi or Mobile Data
        } else {
            androidx.work.NetworkType.UNMETERED // WiFi only
        }

        // If both are disabled, we can either cancel work or just set to UNMETERED but it won't run if no wifi.
        // Ideally if both disabled, we should cancel.
        if (!_syncOnWifi.value && !_syncOnMobileData.value) {
            workManager.cancelUniqueWork("GmailSync")
            return
        }

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.platisa.app.core.worker.GmailSyncWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "GmailSync",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE, // Use UPDATE to apply new constraints
            syncRequest
        )
    }

    fun exportCsv(context: Context) {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        launchCatching {
            val receipts = repository.getAllReceipts().first()
            val file = ExportManager.exportToCsv(context, receipts)
            if (file != null) {
                SnackbarManager.showMessage("CSV kreiran! Izaberite gde želite da sačuvate.")
                shareFile(context, file, "text/csv")
            } else {
                SnackbarManager.showMessage("Export failed")
            }
        }
    }

    fun importCsv(uri: android.net.Uri) {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        launchCatching {
            var importedCount = 0
            var updatedCount = 0
            var skippedCount = 0
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                var line = reader.readLine() // Read header
                
                // Verify header roughly
                if (line == null || !line.contains("Date") || !line.contains("Amount")) {
                    SnackbarManager.showMessage("Greška: Neispravan CSV format")
                    return@launchCatching
                }
                
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val matchedReceiptIds = mutableSetOf<Long>() // Track matched IDs to prevent greedy matching
                
                while (reader.readLine().also { line = it } != null) {
                    try {
                        val parts = line!!.split(",")
                        if (parts.size < 3) continue
                        
                        // SMART PARSING: Handle shifted columns (e.g. merchant names with commas)
                        // Find the Currency column index (usually "RSD")
                        val currencyPartIndex = parts.indexOfLast { it.trim().uppercase() in listOf("RSD", "EUR", "USD", "DIN") }
                        
                        var dateStr = parts[0]
                        var merchant = parts[1]
                        var amountStr = parts[2].trim()
                        var currency = "RSD"
                        var statusStr = "UNPAID"
                        var invoiceNum = ""
                        var extId = ""
                        var source = "IMPORTED"

                        if (currencyPartIndex > 1) {
                            // Column mapping based on anchor "Currency"
                            // Date | Merchant Part 1 | ... | Merchant Part N | Amount | Currency | Status | ...
                            
                            currency = parts[currencyPartIndex].trim()
                            amountStr = parts[currencyPartIndex - 1].trim()
                            
                            // Merchant is everything between Date (0) and Amount (currency - 1)
                            // We join them back together in case it was split by commas
                            merchant = parts.subList(1, currencyPartIndex - 1).joinToString(" ") { it.trim() }.replace("  ", " ")
                            
                            // Capture other fields if available
                            if (parts.size > currencyPartIndex + 1) statusStr = parts[currencyPartIndex + 1]
                            if (parts.size > currencyPartIndex + 2) invoiceNum = parts[currencyPartIndex + 2]
                            if (parts.size > currencyPartIndex + 3) extId = parts[currencyPartIndex + 3]
                            if (parts.size > currencyPartIndex + 4) source = parts[currencyPartIndex + 4]
                        } else {
                            //  Fallback to standard positions if no currency found
                            if (parts.size > 3) currency = parts[3]
                            if (parts.size > 4) statusStr = parts[4]
                            if (parts.size > 5) invoiceNum = parts[5]
                            if (parts.size > 6) extId = parts[6]
                            if (parts.size > 7) source = parts[7]
                        }

                        // Robust Amount Parsing (Handles "3299.00" and "3.299,00")
                        val amount = try {
                            // 1. Try standard US/Code format first
                            java.math.BigDecimal(amountStr)
                        } catch (e: Exception) {
                            try {
                                // 2. Try European/Serbian format (swap dot and comma)
                                val clean = amountStr.replace(".", "").replace(",", ".")
                                java.math.BigDecimal(clean)
                            } catch (e2: Exception) {
                                // 3. Log error and skip
                                android.util.Log.e("SettingsViewModel", "Failed to parse amount: '$amountStr' from line: $line")
                                continue
                            }
                        }
                        
                        // Fix for Ambiguous "3.299" (Thousands vs Decimals)
                        var finalAmount = amount
                        if (amount.toDouble() > 0 && amount.toDouble() < 100 && amount.scale() == 3 && currency.uppercase() == "RSD") {
                             finalAmount = amount.multiply(java.math.BigDecimal(1000))
                             android.util.Log.w("SettingsViewModel", "Ambiguous amount detected: $amountStr passed as $amount, auto-corrected to $finalAmount")
                        }


                        val paymentStatus = try { 
                            com.platisa.app.core.domain.model.PaymentStatus.valueOf(statusStr) 
                        } catch(e: Exception) { 
                            com.platisa.app.core.domain.model.PaymentStatus.UNPAID 
                        }

                        // UNIVERSAL MATCHING logic (3-Factor: Merchant + Amount + Date)
                        // User explicitly requested to IGNORE InvoiceNumber and ExternalId for matching
                        // to prevent duplicates caused by inconsistent IDs.

                        // 1. Search DB for candidates
                        // CRITICAL FIX: Payment Date (CSV) is usually 15-30 days AFTER Bill Date (DB).
                        // So we must look BACKWARDS from the CSV date.
                        // Range: [PaymentDate - 120 days] to [PaymentDate + 10 days] (User requested 120 days/4 months)
                        
                        val targetDate = try { dateFormat.parse(dateStr)!! } catch (e: Exception) { java.util.Date() }
                        // WIDE NET SEARCH
                         // Range: [TargetDate - 240 days] to [TargetDate + 30 days]
                         // We pull a massive window (8 months) to handle old unpaid bills or date format errors.
                        val startRange = java.util.Calendar.getInstance().apply { time = targetDate; add(java.util.Calendar.DAY_OF_YEAR, -240) }.time.time
                        val endRange = java.util.Calendar.getInstance().apply { time = targetDate; add(java.util.Calendar.DAY_OF_YEAR, 30) }.time.time
                        
                        // Single massive fetch
                        val allCandidates = repository.getReceiptsInRange(startRange, endRange).distinctBy { it.id }
                        
                        // 2. Filter by Merchant Name AND Amount in Memory
                        val existingReceipt = allCandidates.find { candidate ->
                            // STICKY MATCH: Skip if already used in this session to prevent greedy matching
                            if (matchedReceiptIds.contains(candidate.id)) return@find false
                            
                            // Amount Check (Robust BigDecimal comparison)
                            // Widen tolerance to 2.0 to handle weird fee structures
                             val diff = candidate.totalAmount.subtract(finalAmount).abs()
                            val amountMatch = diff.toDouble() < 2.0
                            
                            // Check for "Unit Mismatch" (e.g. DB has 4.377 [4 dinars], CSV has 4377.00)
                            // This handles legacy data where dots were ignored
                            val diffScale = candidate.totalAmount.multiply(java.math.BigDecimal(1000)).subtract(finalAmount).abs()
                            val amountMatchScale = diffScale.toDouble() < 2.0
                            
                            if (!amountMatch && !amountMatchScale) return@find false
                            
                            // Normalize strings for check
                            val dbName = candidate.merchantName.lowercase().replace(" ", "").replace("-", "").replace(".", "")
                            val csvName = merchant.lowercase().replace(" ", "").replace("-", "").replace(".", "")
                            
                            // Direct match with normalized strings
                            var match = dbName.contains(csvName) || csvName.contains(dbName)
                            
                            // Alias match
                            if (!match) {
                                val aliases = mapOf(
                                    "mts" to listOf("telekom", "telekom srbija", "mts"),
                                    "telekom" to listOf("mts", "telekom srbija", "telekom"),
                                    "telenor" to listOf("yettel", "telenor", "mobi banka", "mobibanka"),
                                    "yettel" to listOf("telenor", "yettel", "mobi banka", "mobibanka"),
                                    "eps" to listOf("eps", "elektroprivreda", "struja", "eps distribucija", "eps snabdevanje"),
                                    "infostan" to listOf("jkp infostan", "infostan tehnologije", "infostan"),
                                    "sbb" to listOf("sbb", "serbian broadband"),
                                    "a1" to listOf("vip", "vip mobile", "a1"),
                                    "vip" to listOf("a1", "vip", "vip mobile")
                                )
                                
                                for ((key, values) in aliases) {
                                    val safeKey = key.replace(" ", "")
                                    if (csvName.contains(safeKey) && values.any { dbName.contains(it.replace(" ", "")) }) match = true
                                    if (dbName.contains(safeKey) && values.any { csvName.contains(it.replace(" ", "")) }) match = true
                                }
                            }
                            match
                        }

                        if (existingReceipt != null) {
                            // MATCH FOUND -> UPDATE STATUS
                            matchedReceiptIds.add(existingReceipt.id) // Mark as used
                            
                            var needsUpdate = false
                            var receiptToUpdate = existingReceipt

                            // If CSV says PAID and DB says UNPAID, update it
                            // Also update if DB shows date as Today (rescan) but CSV has real historical date
                            if (paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID && 
                                existingReceipt.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                                receiptToUpdate = receiptToUpdate.copy(paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID)
                                needsUpdate = true
                            }
                            
                            // Also update Source if missing
                            if (existingReceipt.originalSource.isEmpty() && source.isNotEmpty()) {
                                 receiptToUpdate = receiptToUpdate.copy(originalSource = source)
                                 needsUpdate = true
                            }
                            
                            if (needsUpdate) {
                                repository.updateReceipt(receiptToUpdate)
                                updatedCount++
                            } else {
                                skippedCount++
                            }
                        } else {
                            // NO MATCH -> INSERT NEW
                            // Log why we missed it (DEBUG)
                            android.util.Log.w("SettingsViewModel", "NO MATCH for: $merchant ($finalAmount). Candidates searched: ${allCandidates.size}")
                            if (allCandidates.isNotEmpty()) {
                                 allCandidates.forEach { 
                                     val diff = it.totalAmount.subtract(finalAmount).abs()
                                     android.util.Log.w("SettingsViewModel", "   Candidate: ${it.merchantName} - ${it.totalAmount} (Diff: $diff) Date: ${it.date}")
                                 }
                            }
                            
                            // NO MATCH -> INSERT NEW
                            val receipt = com.platisa.app.core.domain.model.Receipt(
                                merchantName = merchant,
                                date = targetDate,
                                totalAmount = finalAmount, // Use the corrected amount
                                currency = currency,
                                paymentStatus = paymentStatus,
                                invoiceNumber = invoiceNum, 
                                externalId = extId,
                                originalSource = source,
                                imagePath = "" 
                            )
                            val newId = repository.insertReceipt(receipt)
                            matchedReceiptIds.add(newId) // Also track new ones just in case
                            importedCount++
                        }


                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Skip malformed line
                    }
                }
            }
            
            SnackbarManager.showMessage("Import: $importedCount novih, $updatedCount ažurirano")
        }
    }

    fun exportPdf(context: Context) {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        launchCatching {
            val receipts = repository.getAllReceipts().first()
            val file = ExportManager.exportToPdf(context, receipts)
            if (file != null) {
                SnackbarManager.showMessage("PDF kreiran! Izaberite gde želite da sačuvate.")
                shareFile(context, file, "application/pdf")
            } else {
                SnackbarManager.showMessage("PDF export nije uspeo. Pokušajte ponovo.")
            }
        }
    }

    private suspend fun shareFile(context: Context, file: java.io.File, mimeType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = android.content.Intent.createChooser(intent, "Podeli izveštaj")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            SnackbarManager.showMessage("Greška pri deljenju fajla: ${e.message}")
        }
    }
    
    // Notification preference toggles
    fun toggleNotifyDue3Days(enabled: Boolean) {
        preferenceManager.notifyDue3Days = enabled
        _notifyDue3Days.value = enabled
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }
    
    fun toggleNotifyDue1Day(enabled: Boolean) {
        preferenceManager.notifyDue1Day = enabled
        _notifyDue1Day.value = enabled
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }
    
    fun toggleNotifyOverdue(enabled: Boolean) {
        preferenceManager.notifyOverdue = enabled
        _notifyOverdue.value = enabled
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }
    
    fun toggleNotifyDuplicate(enabled: Boolean) {
        preferenceManager.notifyDuplicate = enabled
        _notifyDuplicate.value = enabled
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }
    
    fun setNotificationTime(hour: Int, minute: Int) {
        preferenceManager.notificationTimeHour = hour
        preferenceManager.notificationTimeMinute = minute
        _notificationTimeHour.value = hour
        _notificationTimeMinute.value = minute
        
        notificationScheduler.rescheduleNotificationChecks() // Reschedule with new time
    }

    fun sendBugReport(message: String) {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.SUCCESS)
        launchCatching {
            val deviceInfo = """
                App Version: 1.6
                Android: ${android.os.Build.VERSION.RELEASE}
                Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                Email: ${_connectedEmail.value ?: "Anonymous"}
            """.trimIndent()
            
            val request = FeedbackRequest(
                email = _connectedEmail.value ?: "anonymous@platisa.app",
                message = message,
                _subject = "Platisa Feedback (v1.6)",
                device_info = deviceInfo
            )
            
            // Post to Formspree Endpoint
            feedbackApi.sendFeedback("https://formspree.io/f/mykwagzk", request)
            
            SnackbarManager.showMessage("Hvala! Vaša poruka je poslata.")
        }
    }


}

