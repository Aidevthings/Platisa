package com.example.platisa.core.data.repository

import android.content.Context
import com.example.platisa.core.data.network.GmailService
import com.example.platisa.core.domain.repository.GmailRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class GmailRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gmailService: GmailService,
    private val secureStorage: com.example.platisa.core.domain.SecureStorage
) : GmailRepository {

    override suspend fun fetchReceipts(account: GoogleSignInAccount, ignoreTimestamp: Boolean, lookbackDays: Int?): List<File> {
        android.util.Log.d("GmailRepo", "fetchReceipts called for account: ${account.email}, ignoreTimestamp: $ignoreTimestamp, lookbackDays: $lookbackDays")
        val files = mutableListOf<File>()
        
        // Get last sync timestamp for incremental sync (unless ignored)
        val lastSync = secureStorage.getLastGmailSyncTimestamp()
        
        // Smart Lookback:
        // 1. If we have a lastSync > 0, use it (Incremental Sync).
        // 2. If lastSync == 0 (First Run), limit to last X days (default 90) to avoid scanning years of old bills.
        // 3. If ignoreTimestamp is true (Manual Force Resync), scan everything (or we could limit this too, but let's assume 'Force' means all).
        val afterTimestamp = if (!ignoreTimestamp) {
            if (lastSync > 0) {
                lastSync
            } else {
                // First run: Default to 90 days ago if no specific lookback requested
                val daysToLookBack = lookbackDays ?: 90
                val lookbackInSeconds = daysToLookBack.toLong() * 24 * 60 * 60
                (System.currentTimeMillis() / 1000) - lookbackInSeconds
            }
        } else {
            null
        }
        
        android.util.Log.d("GmailRepo", "Last sync timestamp: $lastSync, afterTimestamp: $afterTimestamp")
        
        // Efficient, spam-resistant query
        val query = """
            (subject:(Račun OR Faktura OR Izvod OR Racun OR eRačun OR eRacun OR Bill OR Рачун OR Фактура OR Извод OR еРачун OR Обавеза))
            has:attachment
            filename:pdf
            -from:(linkedin.com OR facebookmail.com OR twitter.com OR youtube.com OR google.com OR instagram.com OR tiktok.com OR pinterest.com OR netflix.com OR spotify.com)
            -category:social
            -category:promotions
            -is:spam
            -is:trash
        """.trimIndent().replace("\n", " ")

        val messages = gmailService.listMessages(
            account, 
            query,
            afterTimestamp
        )
        
        android.util.Log.d("GmailRepo", "Found ${messages.size} messages matching query")
        
        // Process in parallel chunks to improve speed while respecting rate limits
        val downloadedSet = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val synchronizedFiles = java.util.Collections.synchronizedList(files)

        kotlinx.coroutines.coroutineScope {
            val scope = this
            // Chunk size of 10 to avoid hitting Google API rate limits too hard
            messages.chunked(10).forEach { batch ->
                batch.map { msg ->
                    scope.async {
                        android.util.Log.d("GmailRepo", "Processing message ID: ${msg.id} (Async)")
                        val details = gmailService.getMessageDetails(account, msg.id)
                        if (details != null) {
                            // Process all parts including nested parts
                            val allParts = mutableListOf<com.google.api.services.gmail.model.MessagePart>()
                            details.payload?.let { collectAllParts(it, allParts) }
                            
                            for (part in allParts) {
                                if (part.filename.isNullOrEmpty()) continue
                                val mimeType = part.mimeType
                                
                                if (mimeType == "application/pdf" || mimeType?.startsWith("image/") == true) {
                                    val attachmentId = part.body?.attachmentId
                                    if (attachmentId != null) {
                                        // Create unique ID to prevent duplicates
                                        val uniqueId = "${msg.id}_$attachmentId"
                                        if (downloadedSet.contains(uniqueId)) {
                                            android.util.Log.d("GmailRepo", "Skipping duplicate: $uniqueId")
                                            continue
                                        }
                                        
                                        val data = gmailService.getAttachment(account, msg.id, attachmentId)
                                        if (data != null) {
                                            val filename = "${msg.id}_${part.filename}"
                                            val file = saveToFile(data, filename)
                                            if (file != null) {
                                                android.util.Log.d("GmailRepo", "Successfully saved file: ${file.absolutePath}")
                                                synchronizedFiles.add(file)
                                                downloadedSet.add(uniqueId)
                                            }
                                        } else {
                                            android.util.Log.e("GmailRepo", "Failed to get attachment data for ${part.filename}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.forEach { it.await() } // Wait for batch to complete
            }
        }
        
        // Update last sync timestamp
        if (messages.isNotEmpty()) {
            android.util.Log.d("GmailRepo", "Updating last sync timestamp")
            secureStorage.setLastGmailSyncTimestamp(System.currentTimeMillis() / 1000)
        }
        
        android.util.Log.d("GmailRepo", "Total files downloaded: ${files.size}")
        return files
    }

    private suspend fun saveToFile(data: ByteArray, filename: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "gmail_$filename")
                val fos = FileOutputStream(file)
                fos.write(data)
                fos.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    private fun collectAllParts(
        part: com.google.api.services.gmail.model.MessagePart,
        collected: MutableList<com.google.api.services.gmail.model.MessagePart>
    ) {
        collected.add(part)
        part.parts?.forEach { subPart ->
            collectAllParts(subPart, collected)
        }
    }
}
