package com.platisa.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.BaseViewModel
import com.platisa.app.core.common.SnackbarManager
import com.platisa.app.core.domain.manager.ExportManager
import com.platisa.app.core.domain.repository.ReceiptRepository
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
    private val notificationScheduler: com.platisa.app.core.notification.NotificationScheduler
) : BaseViewModel() {

    private val _biometricEnabled = MutableStateFlow(secureStorage.isBiometricEnabled())
    val biometricEnabled = _biometricEnabled.asStateFlow()

    private val _currency = MutableStateFlow(secureStorage.getCurrency())
    val currency = _currency.asStateFlow()

    private val _connectedEmail = MutableStateFlow<String?>(null)
    val connectedEmail = _connectedEmail.asStateFlow()

    private val _connectedAccounts = MutableStateFlow<Set<String>>(emptySet())
    val connectedAccounts = _connectedAccounts.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(true)
    val isIgnoringBatteryOptimizations = _isIgnoringBatteryOptimizations.asStateFlow()

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

    private val _subscriptionStatus = MutableStateFlow(preferenceManager.subscriptionStatus)
    val subscriptionStatus = _subscriptionStatus.asStateFlow()

    init {
        checkConnectedAccount()
        loadConnectedAccounts()
        observeSyncWork()
        checkBatteryOptimization()
        notificationScheduler.scheduleNotificationChecks() // Schedule notifications on app start
    }

    fun checkBatteryOptimization() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        _isIgnoringBatteryOptimizations.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
    
    fun toggleBiometric(enabled: Boolean) {
        secureStorage.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }
    
    fun setCurrency(newCurrency: String) {
        secureStorage.setCurrency(newCurrency)
        _currency.value = newCurrency
    }
    
    private val _forceLogoutEvent = kotlinx.coroutines.flow.MutableSharedFlow<Boolean>()
    val forceLogoutEvent = _forceLogoutEvent.asSharedFlow()

    fun removeAccount(email: String) {
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

            // 1. Clear database tables
            repository.deleteAllReceiptItems()
            repository.deleteAllEpsData()
            repository.deleteAllReceipts()
            
            // 2. Clear ONLY cache files (Gmail PDFs)
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("gmail_")) {
                    try { file.delete() } catch (e: Exception) {}
                }
            }
            
            // 3. Reset Timestamp to 0 to trigger fresh sync
            secureStorage.setLastGmailSyncTimestamp(0)
            
            android.util.Log.d("SettingsViewModel", "✅ Data cleared. Triggering re-sync...")
            SnackbarManager.showMessage("Podaci obrisani. Ponovno skeniranje pokrenuto...")
            
            // 4. Trigger immediate sync
            syncNow() 
            
            // 5. Re-schedule periodic sync after this one finishes (or concurrently)
            scheduleGmailSync()
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
        launchCatching {
            val receipts = repository.getAllReceipts().first()
            val file = ExportManager.exportToCsv(context, receipts)
            if (file != null) {
                shareFile(context, file, "text/csv")
            } else {
                SnackbarManager.showMessage("Export failed")
            }
        }
    }

    fun importCsv(uri: android.net.Uri) {
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
                        
                        val dateStr = parts[0]
                        val merchant = parts[1]
                        val amountStr = parts[2].trim()
                        
                        // Robust Amount Parsing (Handles "3299.00" and "3.299,00")
                        val amount = try {
                            // 1. Try standard US/Code format first
                            java.math.BigDecimal(amountStr)
                        } catch (e: Exception) {
                            try {
                                // 2. Try European/Serbian format (swap dot and comma)
                                // Remove thousands separator (dot), replace decimal (comma) with dot
                                // Example: "3.299,00" -> "3299.00"
                                val clean = amountStr.replace(".", "").replace(",", ".")
                                java.math.BigDecimal(clean)
                            } catch (e2: Exception) {
                                // 3. Log error and skip
                                android.util.Log.e("SettingsViewModel", "Failed to parse amount: $amountStr from line: $line")
                                continue
                            }
                        }
                        
                        // Fix for Ambiguous "3.299" (Thousands vs Decimals)
                        // If amount is small (< 1000) and has 3 decimal places (e.g. 3.299),
                        // and currency is RSD, it is ALMOST CERTAINLY 3299 RSD (thousands separator), not 3.29 RSD.
                        // Standard BigDecimal behavior: "3.299" -> 3.299
                        // We convert it to 3299 if it fits the pattern.
                        var finalAmount = amount
                        if (amount.toDouble() > 0 && amount.toDouble() < 100 && amount.scale() == 3 && parts.getOrElse(3) { "RSD" }.uppercase() == "RSD") {
                             finalAmount = amount.multiply(java.math.BigDecimal(1000))
                             android.util.Log.w("SettingsViewModel", "Ambiguous amount detected: $amountStr passed as $amount, auto-corrected to $finalAmount")
                        }

                        val currency = parts.getOrElse(3) { "RSD" }
                        val statusStr = parts.getOrElse(4) { "UNPAID" }
                        val invoiceNum = parts.getOrElse(5) { "" }.takeIf { it.isNotEmpty() }
                        val extId = parts.getOrElse(6) { "" }.takeIf { it.isNotEmpty() }
                        val source = parts.getOrElse(7) { "IMPORTED" }
                        
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
        launchCatching {
            val receipts = repository.getAllReceipts().first()
            val file = ExportManager.exportToPdf(context, receipts)
            if (file != null) {
                shareFile(context, file, "application/pdf")
            } else {
                SnackbarManager.showMessage("Export failed")
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
    }
    
    fun toggleNotifyDue1Day(enabled: Boolean) {
        preferenceManager.notifyDue1Day = enabled
        _notifyDue1Day.value = enabled
    }
    
    fun toggleNotifyOverdue(enabled: Boolean) {
        preferenceManager.notifyOverdue = enabled
        _notifyOverdue.value = enabled
    }
    
    fun toggleNotifyDuplicate(enabled: Boolean) {
        preferenceManager.notifyDuplicate = enabled
        _notifyDuplicate.value = enabled
    }
    
    fun setNotificationTime(hour: Int) {
        preferenceManager.notificationTimeHour = hour
        _notificationTimeHour.value = hour
        notificationScheduler.rescheduleNotificationChecks() // Reschedule with new time
    }

    fun sendBugReport(context: Context, userMessage: String) {
        val deviceInfo = """
            
            ----------------------------------------
            Device Info (Auto-generated):
            App Version: 1.0.0
            Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
            Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            Sync Status: ${_syncStatus.value ?: "Inactive"}
            Connected Accounts: ${_connectedAccounts.value.size}
            ----------------------------------------
        """.trimIndent()

        val fullBody = "$userMessage\n$deviceInfo"

        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("developer@platisa.com"))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Platisa Bug Report")
            putExtra(android.content.Intent.EXTRA_TEXT, fullBody)
        }

        try {
            context.startActivity(android.content.Intent.createChooser(intent, "Pošalji izveštaj"))
        } catch (e: Exception) {
            viewModelScope.launch {
                SnackbarManager.showMessage("Nije pronađena email aplikacija.")
            }
        }
    }

}

