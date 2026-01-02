package com.example.platisa.core.data.network

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GmailService(private val context: Context) {

    private fun getGmailService(account: GoogleSignInAccount): Gmail {
        android.util.Log.d("GmailService", "Creating Gmail service for account: ${account.email}")
        
        if (account.account == null) {
            android.util.Log.e("GmailService", "account.account is NULL! Google Sign-In may not be properly configured.")
            throw IllegalStateException("Google account is null. Please sign in again.")
        }
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(GmailScopes.GMAIL_READONLY)
        )
        credential.selectedAccount = account.account

        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Platisa")
            .build()
    }

    suspend fun listMessages(
        account: GoogleSignInAccount, 
        query: String,
        afterTimestamp: Long? = null // Unix timestamp in seconds
    ): List<Message> {
        return withContext(Dispatchers.IO) {
            val service = getGmailService(account)
            
            // Build query with date filter if provided
            val fullQuery = if (afterTimestamp != null) {
                val date = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US)
                    .format(java.util.Date(afterTimestamp * 1000))
                "$query after:$date"
            } else {
                query
            }
            
            android.util.Log.d("GmailService", "Executing query: $fullQuery")
            
            val response = service.users().messages().list("me")
                .setQ(fullQuery)
                .setMaxResults(100) // Limit to 100 emails per sync
                .execute()
                
            val messages = response.messages ?: emptyList()
            android.util.Log.d("GmailService", "Found ${messages.size} messages")
            messages
        }
    }

    suspend fun getMessageDetails(account: GoogleSignInAccount, messageId: String): Message? {
        return withContext(Dispatchers.IO) {
            try {
                val service = getGmailService(account)
                service.users().messages().get("me", messageId).execute()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    suspend fun getAttachment(account: GoogleSignInAccount, messageId: String, attachmentId: String): ByteArray? {
         return withContext(Dispatchers.IO) {
            try {
                val service = getGmailService(account)
                val part = service.users().messages().attachments().get("me", messageId, attachmentId).execute()
                part.decodeData()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
