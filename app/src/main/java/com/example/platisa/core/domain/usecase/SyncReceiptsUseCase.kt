package com.example.platisa.core.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.platisa.core.common.GoogleAuthManager
import com.example.platisa.core.common.ImageUtils
import com.example.platisa.core.common.OcrManager
import com.example.platisa.core.common.PdfUtils
import com.example.platisa.core.domain.model.Receipt
import com.example.platisa.core.domain.parser.AutoTagger
import com.example.platisa.core.domain.parser.ReceiptParser
import com.example.platisa.core.data.parser.IpsParser
import com.example.platisa.core.data.parser.EpsParser
import com.example.platisa.core.domain.model.EpsData
import com.example.platisa.core.domain.repository.GmailRepository
import com.example.platisa.core.domain.repository.ReceiptRepository
import com.example.platisa.core.data.repository.DuplicateBillException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

data class SyncStats(
    val emailsFound: Int = 0,
    val filesDownloaded: Int = 0,
    val receiptsParsed: Int = 0,
    val duplicatesBlocked: Int = 0,
    val errors: List<String> = emptyList()
)

class SyncReceiptsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gmailRepository: GmailRepository,
    private val receiptRepository: ReceiptRepository,
    private val secureStorage: com.example.platisa.core.domain.SecureStorage
) {

    suspend operator fun invoke(forceResync: Boolean = false, lookbackDays: Int? = null): SyncStats {
        android.util.Log.d("SyncReceiptsUseCase", "Starting MULTI-ACCOUNT sync... lookbackDays: $lookbackDays")
        
        // 1. Get all connected accounts from storage
        val connectedEmails = secureStorage.getConnectedAccounts()
        
        // Fallback: If storage is empty (first run/legacy), try getting the active Google session
        val emailsToSync = if (connectedEmails.isNotEmpty()) {
            connectedEmails.toList()
        } else {
            val legacyAccount = GoogleAuthManager.getSignedInAccount(context)
            if (legacyAccount?.email != null) {
                listOf(legacyAccount.email!!)
            } else {
                emptyList()
            }
        }

        if (emailsToSync.isEmpty()) {
            android.util.Log.e("SyncReceiptsUseCase", "No connected accounts found!")
            return SyncStats(errors = listOf("Nije povezan nijedan Google nalog."))
        }

        android.util.Log.d("SyncReceiptsUseCase", "Found ${emailsToSync.size} accounts to sync: $emailsToSync")

        // 2. Loop through each account and sync
        var totalEmailsFound = 0
        var totalFilesDownloaded = 0
        var totalReceiptsParsed = 0
        var totalDuplicatesBlocked = 0
        val allErrors = mutableListOf<String>()

        for (email in emailsToSync) {
            android.util.Log.d("SyncReceiptsUseCase", "🔄 Syncing account: $email")
            
            // 2a. Perform Silent Sign-In to get fresh token for THIS specific email
            val account = GoogleAuthManager.performSilentSignIn(context, email)
            
            if (account == null) {
                val error = "Neuspešna autorizacija za: $email"
                android.util.Log.e("SyncReceiptsUseCase", error)
                allErrors.add(error)
                continue
            }

            // 2b. Fetch & Process for this account
            try {
                val stats = syncSingleAccount(account, forceResync, lookbackDays)
                totalFilesDownloaded += stats.filesDownloaded
                totalReceiptsParsed += stats.receiptsParsed
                totalDuplicatesBlocked += stats.duplicatesBlocked
                allErrors.addAll(stats.errors)
            } catch (e: Exception) {
                val error = "Greška kod naloga $email: ${e.message}"
                android.util.Log.e("SyncReceiptsUseCase", error, e)
                allErrors.add(error)
            }
        }

        return SyncStats(
            emailsFound = -1, // Not tracking specific email count anymore
            filesDownloaded = totalFilesDownloaded,
            receiptsParsed = totalReceiptsParsed,
            duplicatesBlocked = totalDuplicatesBlocked,
            errors = allErrors
        )
    }

    private suspend fun syncSingleAccount(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, 
        forceResync: Boolean, 
        lookbackDays: Int?
    ): SyncStats {
        val errorList = mutableListOf<String>()
        val files = try {
             gmailRepository.fetchReceipts(account, forceResync, lookbackDays)
        } catch (e: Exception) {
             android.util.Log.e("SyncReceiptsUseCase", "Error fetching receipts for ${account.email}: ${e.message}", e)
             return SyncStats(errors = listOf("${e.message}"))
        }

        android.util.Log.d("SyncReceiptsUseCase", "Fetched ${files.size} files from ${account.email}")

        // Process downloaded files in parallel
        // Process downloaded files SEQUENTIALLY to avoid OOM
        // (Parallel processing caused massive memory spikes with large PDFs)
        val parsedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val duplicatesBlocked = java.util.concurrent.atomic.AtomicInteger(0)
        
        files.forEach { file ->
            try {
                // Ensure garbage collection has a chance to run between heavy files
                // System.gc() // Optional: Hint to GC if memory is very tight
                processFile(file, account.email ?: "unknown", parsedCount, duplicatesBlocked, receiptRepository, context)
            } catch (e: Exception) {
                 android.util.Log.e("SyncReceiptsUseCase", "Error processing ${file.name}", e)
                 errorList.add("Error: ${file.name} - ${e.message}")
            }
        }
        
        return SyncStats(
            filesDownloaded = files.size,
            receiptsParsed = parsedCount.get(),
            duplicatesBlocked = duplicatesBlocked.get(),
            errors = errorList
        )
    }
    
    private suspend fun processFile(
        file: File, 
        accountEmail: String,
        parsedCount: java.util.concurrent.atomic.AtomicInteger,
        duplicatesBlocked: java.util.concurrent.atomic.AtomicInteger,
        repo: ReceiptRepository,
        ctx: Context
    ) {
        android.util.Log.d("SyncReceiptsUseCase", "Processing file: ${file.name}")
        
        // Extract unique ID from filename (format: gmail_messageId_filename)
        val externalId = file.name.removePrefix("gmail_")
        
        // Check if this Gmail attachment was already processed
        val existingReceipt = repo.getReceiptByExternalId(externalId)
        if (existingReceipt != null) {
            android.util.Log.w("SyncReceiptsUseCase", "⏭️ SKIPPING: External ID already processed: $externalId")
            return
        }
        
        // Fallback: Check path (legacy support)
        val existingByPath = repo.getReceiptByPath(file.absolutePath)
        if (existingByPath != null) {
            android.util.Log.w("SyncReceiptsUseCase", "⏭️ SKIPPING: File path already processed: ${file.name}")
            return
        }
        
        // Try QR code extraction first for PDFs
        var qrAmount: BigDecimal? = null
        var merchantName: String? = null
        var qrCodeData: String? = null
        
        if (file.extension.equals("pdf", ignoreCase = true)) {
            val qrContent = PdfUtils.extractQrCode(file)
            if (qrContent != null) {
                qrCodeData = qrContent
                val ipsData = IpsParser.parse(qrContent)
                if (ipsData != null) {
                    qrAmount = ipsData.amount
                    merchantName = ipsData.recipientName?.let { 
                        ReceiptParser.cleanMerchantName(it) 
                    }
                }
            }
        }
        
        // ===========================================
        // FILTER 1: QR CODE REQUIRED
        // Računi bez QR koda se ignorisu - ne možemo ih platiti kroz aplikaciju
        // ===========================================
        if (qrCodeData == null) {
            android.util.Log.d("SyncReceiptsUseCase", "❌ SKIPPING (No QR Code): ${file.name}")
            android.util.Log.d("SyncReceiptsUseCase", "   Računi bez IPS QR koda se ne mogu platiti kroz aplikaciju")
            return
        }
        
        // Extract text using OCR (works for both PDFs and images)
        val text = OcrManager.processImage(ctx, Uri.fromFile(file))
        
        // ===========================================
        // FILTER 2: BANK STATEMENT BLACKLIST
        // Izvodi iz banke imaju QR kod ali nisu računi za plaćanje
        // ===========================================
        if (isBankStatement(text)) {
            android.util.Log.d("SyncReceiptsUseCase", "🏦 SKIPPING (Bank Statement): ${file.name}")
            android.util.Log.d("SyncReceiptsUseCase", "   Bankovni izvodi se ignorisu")
            return
        }

        // Tier 2: Local Pre-Check (legacy, ali QR već postoji pa je sigurno račun)
        val looksValid = if (qrAmount != null) true else looksLikeBill(text)

        if (looksValid) {
            val parsed = ReceiptParser.parse(text)
            val epsData = EpsParser.parse(text)
            
            // DEBUG: Log file with parsed data for easier tracing
            android.util.Log.d("SyncReceiptsUseCase", "📄 ========== PARSED FILE ==========")
            android.util.Log.d("SyncReceiptsUseCase", "📄 File: ${file.name}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Naplatni: ${epsData.naplatniBroj}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 PaymentId: ${epsData.paymentId}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Invoice: ${epsData.invoiceNumber}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Period: ${epsData.periodStart} - ${epsData.periodEnd}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 STORNO: ${epsData.isStorno}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Parsed Amount: ${parsed.totalAmount}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Parsed Merchant: ${parsed.merchantName}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 ==================================")
            
            // Priority: QR Payer Name > EPS Recipient > Parser Recipient
            val finalRecipientName = (if (file.extension.equals("pdf", ignoreCase = true)) {
                val qrContent = PdfUtils.extractQrCode(file)
                if (qrContent != null) IpsParser.parse(qrContent)?.payerName else null
            } else null) ?: epsData.recipientName ?: parsed.recipientName
            
            val finalRecipientAddress = epsData.recipientAddress ?: parsed.recipientAddress

            // Priority: QR amount > EPS amount > Receipt parser amount
            val totalAmount = qrAmount ?: epsData.totalConsumption ?: parsed.totalAmount ?: BigDecimal.ZERO
            val finalMerchant = merchantName ?: parsed.merchantName ?: "Unknown"
            
            // Create receipt with Payment ID fields AND externalId
            val receipt = Receipt(
                merchantName = finalMerchant,
                totalAmount = totalAmount,
                // CRITICAL FIX: Use EPS Billing Period End if available (normalizes Storno/Original dates)
                date = epsData.periodEnd ?: parsed.date ?: Date(),
                dueDate = epsData.dueDate ?: parsed.dueDate,
                imagePath = file.absolutePath,
                qrCodeData = qrCodeData,
                invoiceNumber = epsData.invoiceNumber ?: parsed.invoiceNumber,
                naplatniNumber = epsData.naplatniBroj,
                paymentId = epsData.paymentId,
                isStorno = epsData.isStorno,
                isVisible = !epsData.isStorno,
                originalSource = "GMAIL($accountEmail)", // Track source email if needed
                externalId = externalId,
                recipientName = finalRecipientName,
                recipientAddress = finalRecipientAddress
            )
            
            try {
                // Pass billingPeriod string explicitly for safe duplicate check
                val id = repo.insertReceipt(receipt, epsData.billingPeriod)
                android.util.Log.d("SyncReceiptsUseCase", "✅ Receipt saved with ID: $id")
                android.util.Log.d("SyncReceiptsUseCase", "💰 FINAL AMOUNT: ${receipt.totalAmount} | Merchant: ${receipt.merchantName} | Date: ${receipt.date}")
                repo.insertEpsData(epsData, id)
                parsedCount.incrementAndGet()
            } catch (e: DuplicateBillException) {
                android.util.Log.w("SyncReceiptsUseCase", "🛑 DUPLICATE BLOCKED: ${e.message}")
                duplicatesBlocked.incrementAndGet()
            }
        }
    }


    private fun looksLikeBill(text: String): Boolean {
        if (text.isBlank()) return false
        
        val keywords = listOf(
            // Latin
            "iznos", "rok", "račun", "faktura", "obaveza", "uplatnica", "qr", "poziv na broj", "svrha", "valuta", "primilac", "uplatilac",
            // Cyrillic
            "износ", "рок", "рачун", "фактура", "обавеза", "уплатница", "позив на број", "сврха", "валута", "примилац", "уплатилац"
        )
        
        val lowerText = text.lowercase()
        // Require at least 2 keywords to be relatively sure
        val matchCount = keywords.count { lowerText.contains(it) }
        
        if (matchCount < 1) {
             android.util.Log.d("SyncReceiptsUseCase", "❌ BLOCKED by Local Pre-Check: No keywords found")
        }
        
        return matchCount >= 1
    }
    
    /**
     * Proverava da li je dokument bankovni izvod (statement) koji treba ignorisati.
     * Koristi STRONG blacklist - samo fraze koje se NIKAD ne pojavljuju na pravim računima.
     */
    private fun isBankStatement(text: String): Boolean {
        val lowerText = text.lowercase()
        
        // STRONG blacklist - fraze koje se pojavljuju SAMO u izvodima, nikad u računima
        val strongBlacklist = listOf(
            // Latinica
            "mesečni izvod", "mesecni izvod",
            "izvod sa računa", "izvod sa racuna",
            "izvod iz banke",
            "pregled prometa",
            "pregled transakcija",
            "stanje na dan",
            "bank statement",
            "account statement",
            "promet po računu", "promet po racunu",
            "dnevni izvod",
            "izvod broj",
            
            // Ćirilica
            "месечни извод",
            "извод са рачуна",
            "извод из банке",
            "преглед промета",
            "преглед трансакција",
            "стање на дан",
            "промет по рачуну",
            "дневни извод",
            "извод број"
        )
        
        for (keyword in strongBlacklist) {
            if (lowerText.contains(keyword)) {
                android.util.Log.d("SyncReceiptsUseCase", "🏦 BANK STATEMENT DETECTED: Found '$keyword'")
                return true
            }
        }
        
        return false
    }
}
