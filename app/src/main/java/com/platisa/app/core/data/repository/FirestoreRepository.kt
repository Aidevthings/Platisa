package com.platisa.app.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        private const val FIELD_EXTERNAL_ID = "external_id"
        private const val FIELD_EMAIL = "email"
    }

    /**
     * Saves the "Paid" status of a receipt to Firestore SHARED COLLECTION.
     * Uses the receipt's source email (connected account) as the parent, 
     * and externalId as the document ID.
     */
    suspend fun savePaidStatus(sourceEmail: String, receiptExternalId: String, isPaid: Boolean) {
        if (sourceEmail.isBlank() || receiptExternalId.isBlank()) return

        try {
            // UNIVERSAL SHARING: Write to shared_receipts/{sourceEmail}/receipts/{externalId}
            val docRef = firestore.collection(COLLECTION_SHARED_RECEIPTS)
                .document(sourceEmail) // The source email (e.g. bills@gmail.com)
                .collection(COLLECTION_RECEIPTS)
                .document(receiptExternalId)

            if (isPaid) {
                val data = hashMapOf(
                    FIELD_PAID_AT to com.google.firebase.Timestamp.now(),
                    FIELD_EXTERNAL_ID to receiptExternalId,
                    FIELD_EMAIL to sourceEmail // Metadata
                )
                docRef.set(data, SetOptions.merge()).await()
            } else {
                // If un-marked as paid, remove the document
                docRef.delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save shared paid status: ${e.message}")
        }
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

            snapshot.documents.mapNotNull { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch shared paid status: ${e.message}")
            emptyList()
        }
    }

    /**
     * Real-time listener for "Paid" receipts.
     * Emits a list of External IDs whenever the collection changes.
     */
    fun observePaidReceipts(sourceEmail: String): Flow<List<String>> = callbackFlow {
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
                val paidIds = snapshot.documents.mapNotNull { it.id }
                Log.d(TAG, "🔔 Update received for $normalizedEmail: ${paidIds.size} paid bills")
                trySend(paidIds)
            }
        }

        awaitClose {
            Log.d(TAG, "🛑 Stopping listener for: $normalizedEmail")
            listenerRegistration.remove()
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

