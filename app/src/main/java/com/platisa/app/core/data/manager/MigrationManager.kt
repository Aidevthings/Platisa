package com.platisa.app.core.data.manager

import com.platisa.app.core.data.preferences.PreferenceManager
import com.platisa.app.core.data.repository.FirestoreRepository
import com.platisa.app.core.domain.repository.ReceiptRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class MigrationManager @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val firestoreRepository: FirestoreRepository,
    private val preferenceManager: PreferenceManager
) {

    suspend fun performUniversalSharingMigration() {
        android.util.Log.d("MigrationManager", "🏁 performUniversalSharingMigration() CALLED")
        
        // Version 2: Force re-migration due to lowercase email fix
        val CURRENT_MIGRATION_VERSION = 2
        val lastMigrationVersion = preferenceManager.universalSharingMigrationVersion
        
        if (lastMigrationVersion >= CURRENT_MIGRATION_VERSION) {
            android.util.Log.d("MigrationManager", "✅ Universal Sharing Migration already complete (v$lastMigrationVersion)")
            return
        }

        android.util.Log.d("MigrationManager", "🚀 Starting Universal Sharing Migration v$CURRENT_MIGRATION_VERSION (was v$lastMigrationVersion)...")

        try {
            // 1. Get ALL receipts (this might be heavy, but it's one-time)
            val allReceipts = receiptRepository.getAllReceipts().first()
            
            // 2. Filter for PAID receipts that need syncing
            val paidReceipts = allReceipts.filter { 
                it.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID &&
                !it.externalId.isNullOrBlank()
            }
            
            android.util.Log.d("MigrationManager", "Found ${paidReceipts.size} PAID receipts to migrate.")
            
            var migratedCount = 0
            
            paidReceipts.forEach { receipt ->
                var sourceEmail = receipt.originalSource
                
                // Parse legacy "GMAIL(email)" format if present
                if (sourceEmail.startsWith("GMAIL(") && sourceEmail.endsWith(")")) {
                    sourceEmail = sourceEmail.substring(6, sourceEmail.length - 1)
                }
                
                // Fallback for Manual/Camera to current user (best effort)
                if (sourceEmail == "Manual" || sourceEmail == "Camera" || !sourceEmail.contains("@")) {
                     val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                     if (currentUser?.email != null) {
                         sourceEmail = currentUser.email!!
                     } else {
                         // Skip if we can't determine an email owner
                         return@forEach 
                     }
                }
                
                if (sourceEmail.isNotBlank()) {
                     firestoreRepository.savePaidStatus(sourceEmail.lowercase(), receipt.externalId!!, true)
                     migratedCount++
                }
            }
            
            android.util.Log.d("MigrationManager", "✅ Migration Complete. Migrated $migratedCount receipts.")
            preferenceManager.universalSharingMigrationVersion = CURRENT_MIGRATION_VERSION
            
        } catch (e: Exception) {
            android.util.Log.e("MigrationManager", "❌ Migration Failed: ${e.message}", e)
            // Do NOT set complete flag, retry next launch
        }
    }
}
