package com.example.platisa.core.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.platisa.core.domain.usecase.SyncReceiptsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class GmailSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncReceiptsUseCase: SyncReceiptsUseCase,
    private val platisaNotificationManager: com.example.platisa.core.notification.PlatisaNotificationManager,
    private val secureStorage: com.example.platisa.core.domain.SecureStorage
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "gmail_sync_channel"
        private const val NOTIFICATION_ID = 1001
        const val KEY_NEW_RECEIPTS = "new_receipts_count"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_FORCE_FULL_SYNC = "force_full_sync"
        const val KEY_LOOKBACK_DAYS = "lookback_days"
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("GmailSyncWorker", "=== doWork() STARTED ===")
        android.util.Log.d("GmailSyncWorker", "Worker class: ${this.javaClass.name}")
        android.util.Log.d("GmailSyncWorker", "SyncReceiptsUseCase injected: ${syncReceiptsUseCase != null}")
        
        // Promote to Foreground Service immediately to prevent killing
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            android.util.Log.w("GmailSyncWorker", "Failed to set foreground (might be restricted/missing permission): ${e.message}")
            // Continue anyway, standard background work might still succeed
        }

        return try {
            val forceFullSync = inputData.getBoolean(KEY_FORCE_FULL_SYNC, false)
            val lookbackDays = inputData.getInt(KEY_LOOKBACK_DAYS, -1).takeIf { it >= 0 }
            
            android.util.Log.d("GmailSyncWorker", "Starting Gmail sync... Force: $forceFullSync, Lookback: $lookbackDays")
            
            // Fresh Start Check: If 0 days, skip scanning completely
            if (lookbackDays == 0) {
                android.util.Log.d("GmailSyncWorker", "Fresh Start selected (0 days). Skipping historical scan.")
                setProgress(workDataOf(
                    "status" to "completed",
                    KEY_NEW_RECEIPTS to 0
                ))
                return Result.success(workDataOf(KEY_NEW_RECEIPTS to 0))
            }
            
            setProgress(workDataOf("status" to "syncing"))
            
            // Perform sync
            android.util.Log.d("GmailSyncWorker", "Calling syncReceiptsUseCase...")
            val stats = syncReceiptsUseCase(forceFullSync, lookbackDays)
            
            android.util.Log.d("GmailSyncWorker", "Sync completed. Stats: $stats")
            
            // Update progress with count
            setProgress(workDataOf(
                "status" to "completed",
                KEY_NEW_RECEIPTS to stats.receiptsParsed
            ))
            
            // Show success notification
            showSuccessNotification(stats.filesDownloaded, stats.receiptsParsed)
            
            // Check for errors explicitly returned in stats
            if (stats.errors.isNotEmpty()) {
                val errorMsg = stats.errors.first()
                android.util.Log.e("GmailSyncWorker", "Sync reported errors: $errorMsg")
                
                showErrorNotification(errorMsg)
                
                return Result.failure(workDataOf(
                    KEY_ERROR_MESSAGE to errorMsg,
                    KEY_NEW_RECEIPTS to stats.receiptsParsed
                ))
            }
            
            Result.success(workDataOf(KEY_NEW_RECEIPTS to stats.receiptsParsed))
        } catch (e: Exception) {
            android.util.Log.e("GmailSyncWorker", "Sync failed", e)
            
            // HANDLING AUTH ERRORS
            if (e is com.google.android.gms.auth.UserRecoverableAuthException || 
                e is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException ||
                e.message?.contains("401") == true ||
                e.message?.contains("credentials") == true) {
                
                android.util.Log.e("GmailSyncWorker", "AUTH ERROR DETECTED: Triggering re-login prompt.")
                
                val currentEmail = secureStorage.getConnectedAccounts().firstOrNull() ?: "vaš nalog"
                platisaNotificationManager.showAuthErrorNotification(currentEmail)
                
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Potrebna prijava na Gmail"))
            }

            // Create detailed error message
            val errorMsg = buildString {
                append(e.javaClass.simpleName)
                append(": ")
                append(e.message ?: "No message")
                e.cause?.let { cause ->
                    append(" (Caused by: ${cause.javaClass.simpleName}: ${cause.message})")
                }
            }
            
            android.util.Log.e("GmailSyncWorker", "Detailed error: $errorMsg")
            
            setProgress(workDataOf(
                "status" to "failed",
                KEY_ERROR_MESSAGE to errorMsg
            ))
            
            showErrorNotification(errorMsg)
            
            // Retry with exponential backoff (max 3 attempts)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMsg))
            }
        }
    }
    
    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Gmail Sync")
            .setContentText("Sinhronizacija računa u toku...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
    
    private fun showSuccessNotification(downloaded: Int, parsed: Int) {
        if (parsed == 0) return

        val message = "Novi račun je stigao! Kliknite da pregledate i platite."
        
        val intent = android.content.Intent(applicationContext, Class.forName("com.example.platisa.MainActivity")).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Novi Račun")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Pronađeno $parsed novih računa. " + message))
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification) // Use distinct ID
    }
    
    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Gmail Sync Failed")
            .setContentText("Greška: $error")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification) // Use distinct ID
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gmail Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Gmail receipt sync"
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
