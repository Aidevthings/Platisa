package com.example.platisa.ui.screens.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.platisa.core.common.BaseViewModel
import com.example.platisa.core.common.SnackbarManager
import com.example.platisa.core.domain.manager.ExportManager
import com.example.platisa.core.domain.repository.ReceiptRepository
import com.example.platisa.core.domain.SecureStorage
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
    private val preferenceManager: com.example.platisa.core.data.preferences.PreferenceManager,
    private val notificationScheduler: com.example.platisa.core.notification.NotificationScheduler
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
                            val count = workInfo.outputData.getInt(com.example.platisa.core.worker.GmailSyncWorker.KEY_NEW_RECEIPTS, 0)
                            _syncStatus.value = "Završeno: $count novih računa"
                            kotlinx.coroutines.delay(3000)
                            _syncStatus.value = null
                        }
                        androidx.work.WorkInfo.State.FAILED -> {

                            val error = workInfo.outputData.getString(com.example.platisa.core.worker.GmailSyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error"
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
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.platisa.core.worker.GmailSyncWorker>()
                .setInputData(androidx.work.workDataOf("force_full_sync" to true))
                .build()
            
            workManager.enqueueUniqueWork(
                "GmailSyncOneTime",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }

    fun checkConnectedAccount() {
        val account = com.example.platisa.core.common.GoogleAuthManager.getSignedInAccount(context)
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
            com.example.platisa.core.common.GoogleAuthManager.signOut(context) {
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
            com.example.platisa.core.common.GoogleAuthManager.signOut(context) {
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

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.platisa.core.worker.GmailSyncWorker>(
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
                
                while (reader.readLine().also { line = it } != null) {
                    try {
                        val parts = line!!.split(",")
                        if (parts.size < 3) continue
                        
                        val dateStr = parts[0]
                        val merchant = parts[1]
                        val amount = java.math.BigDecimal(parts[2])
                        val currency = parts.getOrElse(3) { "RSD" }
                        val statusStr = parts.getOrElse(4) { "UNPAID" }
                        val invoiceNum = parts.getOrElse(5) { "" }.takeIf { it.isNotEmpty() }
                        val extId = parts.getOrElse(6) { "" }.takeIf { it.isNotEmpty() }
                        val source = parts.getOrElse(7) { "IMPORTED" }
                        
                        // Deduplication Check
                        var exists = false
                        if (invoiceNum != null) {
                            if (repository.getReceiptByInvoiceNumber(invoiceNum) != null) exists = true
                        }
                        if (!exists && extId != null) {
                            // We don't have getByExternalId exposed in Repo easily, but usually InvoiceNum is enough.
                            // However, Gmail receipts rely on External ID.
                            // Let's rely on InvoiceNumber (or hash) for now as generic check.
                            // If user exports from HERE, they have invoice numbers.
                        }
                        
                        if (exists) {
                            skippedCount++
                            continue
                        }
                        
                        val receipt = com.example.platisa.core.domain.model.Receipt(
                            merchantName = merchant,
                            date = try { dateFormat.parse(dateStr)!! } catch (e: Exception) { java.util.Date() },
                            totalAmount = amount,
                            currency = currency,
                            paymentStatus = try { com.example.platisa.core.domain.model.PaymentStatus.valueOf(statusStr) } catch(e: Exception) { com.example.platisa.core.domain.model.PaymentStatus.UNPAID },
                            invoiceNumber = invoiceNum,
                            externalId = extId,
                            originalSource = source,
                            imagePath = "" // No image for imported
                        )
                        
                        repository.insertReceipt(receipt)
                        importedCount++
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Skip malformed line
                    }
                }
            }
            
            SnackbarManager.showMessage("Import završen: $importedCount uvezeno, $skippedCount preskočeno (duplikati)")
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
