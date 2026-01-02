package com.platisa.app.core.common

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.gmail.GmailScopes
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object GoogleAuthManager {
    // private const val WEB_CLIENT_ID = "1072565451838-ns3qocrha29phhrgr8cpuda1gltourf0.apps.googleusercontent.com"

    fun getSignInClient(context: Context): GoogleSignInClient {
        // Dynamically get the Web Client ID from google-services generated resources
        // This ensures it matches the Firebase configuration exactly.
        val webClientId = context.getString(com.platisa.app.R.string.default_web_client_id)
        android.util.Log.d("GoogleAuthManager", "Using WebClientID: $webClientId")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            // .requestServerAuthCode(webClientId) // Not needed for pure Firebase Auth, only for backend access
            .requestEmail()
            .requestScopes(Scope(GmailScopes.GMAIL_READONLY))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun signIn(intent: Intent): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        return task.await()
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        getSignInClient(context).signOut().addOnCompleteListener { 
            Firebase.auth.signOut()
            onComplete() 
        }
    }

    suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Boolean {
        val idToken = account.idToken ?: throw Exception("Google ID Token is null")
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        return try {
            Firebase.auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            // Re-throw to show in UI
            android.util.Log.e("GoogleAuthManager", "Firebase Auth failed", e)
            throw e 
        }
    }
    fun getClientFor(context: Context, email: String): GoogleSignInClient {
        val webClientId = context.getString(com.platisa.app.R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(Scope(GmailScopes.GMAIL_READONLY))
            .setAccountName(email)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun performSilentSignIn(context: Context, email: String): GoogleSignInAccount? {
        return try {
            val client = getClientFor(context, email)
            client.silentSignIn().await()
        } catch (e: Exception) {
            android.util.Log.e("GoogleAuthManager", "Silent sign-in failed for $email", e)
            null
        }
    }
}

