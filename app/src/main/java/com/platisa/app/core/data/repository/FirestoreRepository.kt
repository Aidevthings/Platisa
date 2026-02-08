package com.platisa.app.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.platisa.app.core.domain.model.PaymentStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_SHARED_RECEIPTS = "shared_receipts" // Universal Sharing
        private const val COLLECTION_RECEIPTS = "receipts"
        private const val FIELD_PAID_AT = "paid_at"
        private const val FIELD_STATUS = "status"
        private const val FIELD_UPDATED_AT = "updated_at"
        private const val FIELD_LOCKED_BY = "locked_by"
        private const val FIELD_LOCKED_AT = "locked_at"
        private const val FIELD_EXTERNAL_ID = "external_id"
        private const val FIELD_EMAIL = "email"
    }

    /**
     * Saves the receipt status to Firestore SHARED COLLECTION.
     * Uses the receipt's source email (connected account) as the parent, 
     * and externalId as the document ID.
     */
    suspend fun saveReceiptStatus(
        sourceEmail: String,
        receiptExternalId: String,
        status: PaymentStatus,
        force: Boolean = false
    ) {
        if (sourceEmail.isBlank() || receiptExternalId.isBlank()) return

        try {
            val normalizedEmail = sourceEmail.lowercase()
            // UNIVERSAL SHARING: Write to shared_receipts/{sourceEmail}/receipts/{externalId}
            val docRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
                .document(normalizedEmail) // The source email (e.g. bills@gmail.com)
                .collection(COLLECTION_RECEIPTS)
                .document(receiptExternalId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val existingStatus = if (snapshot.exists()) {
                    parseStatus(snapshot.getString(FIELD_STATUS)) ?: PaymentStatus.PAID
                } else {
                    null
                }

                val finalStatus = if (force) {
                    status
                } else {
                    when {
                        existingStatus == PaymentStatus.PAID -> PaymentStatus.PAID
                        existingStatus == PaymentStatus.PROCESSING && status == PaymentStatus.UNPAID -> PaymentStatus.PROCESSING
                        else -> status
                    }
                }

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                when (finalStatus) {
                    PaymentStatus.UNPAID -> {
                        transaction.delete(docRef)
                    }
                    PaymentStatus.PROCESSING -> {
                        val data = hashMapOf(
                            FIELD_STATUS to PaymentStatus.PROCESSING.name,
                            FIELD_UPDATED_AT to com.google.firebase.Timestamp.now(),
                            FIELD_LOCKED_BY to currentUid,
                            FIELD_LOCKED_AT to com.google.firebase.Timestamp.now(),
                            FIELD_EXTERNAL_ID to receiptExternalId,
                            FIELD_EMAIL to normalizedEmail
                        )
                        transaction.set(docRef, data, SetOptions.merge())
                    }
                    PaymentStatus.PAID -> {
                        val data = hashMapOf(
                            FIELD_STATUS to PaymentStatus.PAID.name,
                            FIELD_PAID_AT to com.google.firebase.Timestamp.now(),
                            FIELD_UPDATED_AT to com.google.firebase.Timestamp.now(),
                            FIELD_LOCKED_BY to currentUid,
                            FIELD_EXTERNAL_ID to receiptExternalId,
                            FIELD_EMAIL to normalizedEmail
                        )
                        transaction.set(docRef, data, SetOptions.merge())
                    }
                }
                null
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save shared receipt status: ${e.message}")
        }
    }

    /**
     * Backward-compatible wrapper for PAID-only updates.
     */
    suspend fun savePaidStatus(sourceEmail: String, receiptExternalId: String, isPaid: Boolean) {
        val status = if (isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID
        saveReceiptStatus(sourceEmail, receiptExternalId, status, force = isPaid.not())
    }

    /**
     * Retrieves a list of all marked-as-PAID receipt External IDs for a specific source email.
     */
    suspend fun getPaidReceiptIdentifiers(sourceEmail: String): List<String> {
        if (sourceEmail.isBlank()) return emptyList()
        
        val normalizedEmail = sourceEmail.lowercase()
        
        return try {
            val snapshot = firestore.collection(COLLECTION_SHARED_RECEIPTS)
                .document(normalizedEmail)
                .collection(COLLECTION_RECEIPTS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val status = parseStatus(doc.getString(FIELD_STATUS))
                if (status == null || status == PaymentStatus.PAID) doc.id else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch shared paid status: ${e.message}")
            emptyList()
        }
    }

    /**
     * Real-time listener for receipt statuses.
     * Emits a map of External ID -> Status whenever the collection changes.
     */
    fun observeReceiptStatuses(sourceEmail: String): Flow<Map<String, PaymentStatus>> = callbackFlow {
        if (sourceEmail.isBlank()) {
            close()
            return@callbackFlow
        }
        
        val normalizedEmail = sourceEmail.lowercase()
        val collectionRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
            .document(normalizedEmail)
            .collection(COLLECTION_RECEIPTS)

        Log.d(TAG, "👀 Starting Real-time listener for: $normalizedEmail")
        
        val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Listen failed: ${error.message}")
                close(error) // Close flow on error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val statusMap = snapshot.documents.mapNotNull { doc ->
                    val status = parseStatus(doc.getString(FIELD_STATUS)) ?: PaymentStatus.PAID
                    doc.id to status
                }.toMap()
                Log.d(TAG, "🔔 Update received for $normalizedEmail: ${statusMap.size} receipts")
                trySend(statusMap)
            }
        }

        awaitClose {
            Log.d(TAG, "🛑 Stopping listener for: $normalizedEmail")
            listenerRegistration.remove()
        }
    }

    /**
     * Backward-compatible listener for paid receipts only.
     */
    fun observePaidReceipts(sourceEmail: String): Flow<List<String>> {
        return observeReceiptStatuses(sourceEmail).map { statusMap ->
            statusMap.filterValues { it == PaymentStatus.PAID }.keys.toList()
        }
    }

    /**
     * Atomically tries to set PROCESSING lock for a receipt.
     * Returns true if lock was acquired (or already held by this user), false otherwise.
     */
    suspend fun tryAcquireProcessingLock(sourceEmail: String, receiptExternalId: String): Boolean {
        if (sourceEmail.isBlank() || receiptExternalId.isBlank()) return false
        val normalizedEmail = sourceEmail.lowercase()
        val docRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
            .document(normalizedEmail)
            .collection(COLLECTION_RECEIPTS)
            .document(receiptExternalId)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (snapshot.exists()) {
                    val status = parseStatus(snapshot.getString(FIELD_STATUS)) ?: PaymentStatus.PAID
                    val lockedBy = snapshot.getString(FIELD_LOCKED_BY)
                    when (status) {
                        PaymentStatus.PAID -> false
                        PaymentStatus.PROCESSING -> {
                            currentUid != null && lockedBy != null && lockedBy == currentUid
                        }
                        PaymentStatus.UNPAID -> {
                            val data = hashMapOf(
                                FIELD_STATUS to PaymentStatus.PROCESSING.name,
                                FIELD_UPDATED_AT to com.google.firebase.Timestamp.now(),
                                FIELD_LOCKED_BY to currentUid,
                                FIELD_LOCKED_AT to com.google.firebase.Timestamp.now(),
                                FIELD_EXTERNAL_ID to receiptExternalId,
                                FIELD_EMAIL to normalizedEmail
                            )
                            transaction.set(docRef, data, SetOptions.merge())
                            true
                        }
                    }
                } else {
                    val data = hashMapOf(
                        FIELD_STATUS to PaymentStatus.PROCESSING.name,
                        FIELD_UPDATED_AT to com.google.firebase.Timestamp.now(),
                        FIELD_LOCKED_BY to currentUid,
                        FIELD_LOCKED_AT to com.google.firebase.Timestamp.now(),
                        FIELD_EXTERNAL_ID to receiptExternalId,
                        FIELD_EMAIL to normalizedEmail
                    )
                    transaction.set(docRef, data, SetOptions.merge())
                    true
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to acquire processing lock: ${e.message}")
            false
        }
    }

    /**
     * Releases PROCESSING lock if held by this user (or if lock is missing user info).
     * Returns true if released, false otherwise.
     */
    suspend fun releaseProcessingLock(sourceEmail: String, receiptExternalId: String): Boolean {
        if (sourceEmail.isBlank() || receiptExternalId.isBlank()) return false
        val normalizedEmail = sourceEmail.lowercase()
        val docRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
            .document(normalizedEmail)
            .collection(COLLECTION_RECEIPTS)
            .document(receiptExternalId)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction true
                val status = parseStatus(snapshot.getString(FIELD_STATUS)) ?: PaymentStatus.PAID
                val lockedBy = snapshot.getString(FIELD_LOCKED_BY)
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (status == PaymentStatus.PROCESSING &&
                    (lockedBy == null || currentUid == null || lockedBy == currentUid)
                ) {
                    transaction.delete(docRef)
                    true
                } else {
                    false
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release processing lock: ${e.message}")
            false
        }
    }

    /**
     * Deletes ALL paid status markers for a specific source email.
     * WARNING: This permanently resets paid status for all shared bills from this email.
     */
    suspend fun deleteAllPaidStatuses(sourceEmail: String) {
        if (sourceEmail.isBlank()) return
        val normalizedEmail = sourceEmail.lowercase()
        
        try {
            val collectionRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
                .document(normalizedEmail)
                .collection(COLLECTION_RECEIPTS)
                
            val snapshot = collectionRef.get().await()
            val batch = firestore.batch()
            
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            Log.d(TAG, "🗑️ Wiped cloud paid statuses for: $normalizedEmail")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to wipe cloud paid statuses: ${e.message}")
        }
    }

    private fun parseStatus(value: String?): PaymentStatus? {
        return try {
            if (value.isNullOrBlank()) null else PaymentStatus.valueOf(value)
        } catch (e: Exception) {
            PaymentStatus.PAID
        }
    }
    
    /**
     * Saves user feedback/bug report to a global 'feedback' collection.
     * This allows "Send" from app without opening Email Client.
     */
    suspend fun saveFeedback(message: String, metadata: Map<String, Any>) {
        if (message.isBlank()) return
        
        try {
            val data = HashMap(metadata)
            data["message"] = message
            data["timestamp"] = com.google.firebase.Timestamp.now()
            
            firestore.collection("feedback")
                .add(data)
                .await()
                
            Log.d(TAG, "✅ Feedback sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send feedback: ${e.message}")
            throw e // Re-throw to let ViewModel handle UI error
        }
    }
}

