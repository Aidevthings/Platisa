package com.example.platisa.core.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import com.example.platisa.core.data.preferences.PreferenceManager

sealed class TrialStatus {
    data class Active(val daysRemaining: Int) : TrialStatus()
    object Expired : TrialStatus()
    data class Error(val message: String) : TrialStatus()
}

@Singleton
class TrialRepository @Inject constructor(
    private val preferenceManager: PreferenceManager
) {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    
    // 90 Days in milliseconds
    private val TRIAL_PERIOD_MS = 90L * 24 * 60 * 60 * 1000 

    suspend fun checkTrialStatus(email: String): TrialStatus {
        // 0. Check Lifetime Access (Founders/Promo)
        if (preferenceManager.isLifetime) {
             return TrialStatus.Active(99999) // Lifetime access
        }

        // 1. Check Local Prefs First (Fast) - User Requested "Instant"
        val localStart = preferenceManager.trialStartDate
        if (localStart > 0) {
            val status = calculateStatus(Date(localStart))
            return status // Return immediately!
        }

        return try {
            val docRef = usersCollection.document(email)
            // If we are here, we don't have local start date, so we MUST wait for network
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val startDate = snapshot.getTimestamp("trialStartDate")?.toDate()
                val hasSubscribed = snapshot.getBoolean("hasSubscribed") ?: false

                if (hasSubscribed) {
                    // Treat subscribed as Active with max days
                    return TrialStatus.Active(999) 
                }

                if (startDate == null) {
                    return startTrial(email)
                }
                
                // Sync local pref
                if (preferenceManager.trialStartDate == 0L) {
                    preferenceManager.trialStartDate = startDate.time
                }

                return calculateStatus(startDate)
                
            } else {
                android.util.Log.d("TrialRepository", "New user $email detected. Starting 3-month trial.")
                return startTrial(email)
            }
        } catch (e: Exception) {
            android.util.Log.e("TrialRepository", "Error checking trial: ${e.message}")
            // Fallback to local if server fails
            if (preferenceManager.trialStartDate > 0) {
                 return calculateStatus(Date(preferenceManager.trialStartDate))
            }
            return TrialStatus.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun startTrial(email: String): TrialStatus {
        val now = Date()
        val data = hashMapOf(
            "email" to email,
            "trialStartDate" to FieldValue.serverTimestamp(),
            "hasSubscribed" to false,
            "deviceModel" to android.os.Build.MODEL
        )
        
        try {
            usersCollection.document(email).set(data, SetOptions.merge()).await()
            preferenceManager.trialStartDate = now.time
        } catch (e: Exception) {
             // If offline, at least set local
             preferenceManager.trialStartDate = now.time
        }
        
        return TrialStatus.Active(90)
    }
    
    private fun calculateStatus(startDate: Date): TrialStatus {
        val now = Date()
        val diff = now.time - startDate.time
        
        if (diff > TRIAL_PERIOD_MS) {
            return TrialStatus.Expired
        } else {
            val daysLeft = ((TRIAL_PERIOD_MS - diff) / (24 * 60 * 60 * 1000)).toInt()
            return TrialStatus.Active(daysLeft.coerceAtLeast(0))
        }
    }

    fun enableLifetimeAccess() {
        preferenceManager.isLifetime = true
        // Ideally sync to server too, but local is enough for now
    }
    
    fun extendTrial(days: Int) {
        val currentStart = preferenceManager.trialStartDate
        if (currentStart > 0) {
            // Adding time to start date makes the trial "younger" (extends it)
            preferenceManager.trialStartDate += (days * 24 * 60 * 60 * 1000L)
        }
    }
}

