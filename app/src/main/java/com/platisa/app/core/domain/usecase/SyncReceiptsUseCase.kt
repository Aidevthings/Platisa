package com.platisa.app.core.domain.usecase

import android.content.Context
import android.net.Uri
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.core.common.ImageUtils
import com.platisa.app.core.common.OcrManager
import com.platisa.app.core.common.PdfUtils
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.parser.AutoTagger
import com.platisa.app.core.domain.parser.ReceiptParser
import com.platisa.app.core.data.parser.IpsParser
import com.platisa.app.core.data.parser.EpsParser
import com.platisa.app.core.domain.model.EpsData
import com.platisa.app.core.domain.repository.GmailRepository
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.data.repository.DuplicateBillException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.platisa.app.core.data.helper.BillAnomalyDetector
import com.platisa.app.core.data.helper.AnomalyResult
import com.platisa.app.core.data.mapper.toEntity

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
    private val secureStorage: com.platisa.app.core.domain.SecureStorage,
    private val firestoreRepository: com.platisa.app.core.data.repository.FirestoreRepository,
    private val anomalyDetector: BillAnomalyDetector
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
        
        // ☁️ UNIVERSAL SHARING: Get paid receipts from ALL relevant sources
        // 1. The account we are currently syncing (account.email) - CRITICAL for shared bills
        // 2. The main logged-in user (currentUserEmail) - Fallback for manual/legacy
        // 3. Any other connected accounts if needed (though typically we process one account at a time here)
        
        val sourcesToCheck = mutableSetOf<String>()
        account.email?.let { sourcesToCheck.add(it.lowercase()) }
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.let { sourcesToCheck.add(it.lowercase()) }
        
        android.util.Log.d("SyncReceiptsUseCase", "🔑 SOURCES TO CHECK FOR PAID STATUS: $sourcesToCheck")
        
        val paidIds = try {
            kotlinx.coroutines.withTimeout(8000) { // Increased timeout for multi-fetch
                val deferreds = sourcesToCheck.map { sourceEmail ->
                    async { 
                        android.util.Log.d("SyncReceiptsUseCase", "☁️ Fetching shared status from: $sourceEmail")
                        firestoreRepository.getPaidReceiptIdentifiers(sourceEmail) 
                    }
                }
                deferreds.awaitAll().flatten()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncReceiptsUseCase", "Failed to fetch shared paid status (Timeout/Error)", e)
            emptyList()
        }
        val paidIdsSet = paidIds.toSet() // Optimize for O(1) lookups
        
        android.util.Log.d("SyncReceiptsUseCase", "📊 TOTAL PAID IDs from cloud: ${paidIdsSet.size}")
        if (paidIdsSet.isNotEmpty()) {
            android.util.Log.d("SyncReceiptsUseCase", "   Sample: ${paidIdsSet.take(3)}")
        }

        // Process downloaded files in parallel
        // Process downloaded files SEQUENTIALLY to avoid OOM
        // (Parallel processing caused massive memory spikes with large PDFs)
        val parsedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val duplicatesBlocked = java.util.concurrent.atomic.AtomicInteger(0)
        
        files.forEach { file ->
            try {
                // Ensure garbage collection has a chance to run between heavy files
                // System.gc() // Optional: Hint to GC if memory is very tight
                processFile(file, account.email ?: "unknown", parsedCount, duplicatesBlocked, receiptRepository, context, paidIdsSet)
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
        ctx: Context,
        paidIds: Set<String>
    ) {
        android.util.Log.d("SyncReceiptsUseCase", "Processing file: ${file.name}")
        
        // Extract unique ID from filename (format: gmail_messageId_filename)
        val externalId = file.name.removePrefix("gmail_")
        
        // 🔍 DEBUG: Log what we're looking for
        android.util.Log.d("SyncReceiptsUseCase", "🔍 ========== CHECKING PAID STATUS ==========")
        android.util.Log.d("SyncReceiptsUseCase", "🔍 File: ${file.name}")
        android.util.Log.d("SyncReceiptsUseCase", "🔍 External ID: $externalId")
        android.util.Log.d("SyncReceiptsUseCase", "🔍 Total paid IDs in set: ${paidIds.size}")
        android.util.Log.d("SyncReceiptsUseCase", "🔍 Is this ID in paidIds? ${paidIds.contains(externalId)}")
        if (paidIds.isNotEmpty()) {
            android.util.Log.d("SyncReceiptsUseCase", "🔍 Sample paid IDs: ${paidIds.take(3)}")
        }
        
        // Check if this Gmail attachment was already processed
        val existingReceipt = repo.getReceiptByExternalId(externalId)
        if (existingReceipt != null) {
            android.util.Log.d("SyncReceiptsUseCase", "🔍 Found existing receipt: ID=${existingReceipt.id}, Status=${existingReceipt.paymentStatus}")
            // Check if local status needs update from Firestore (Cloud says PAID, local says UNPAID)
            if (paidIds.contains(externalId) && existingReceipt.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                 android.util.Log.d("SyncReceiptsUseCase", "🔄 Updating existing receipt to PAID based on Firestore: $externalId")
                 val updated = existingReceipt.copy(
                     paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                     updatedAt = Date(),
                     metadata = (existingReceipt.metadata ?: "") + " [Cloud Sync]"
                 )
                 repo.updateReceipt(updated)
            }
            android.util.Log.w("SyncReceiptsUseCase", "⏭️ SKIPPING: External ID already processed: $externalId")
            android.util.Log.d("SyncReceiptsUseCase", "🔍 =============================================")
            return
        }
        
        // Fallback: Check path (legacy support)
        val existingByPath = repo.getReceiptByPath(file.absolutePath)
        if (existingByPath != null) {
            android.util.Log.w("SyncReceiptsUseCase", "⏭️ SKIPPING: File path already processed: ${file.name}")
            return
        }
        
        var qrAmount: BigDecimal? = null
        var merchantName: String? = null
        var qrCodeData: String? = null
        var ipsReferenceNumber: String? = null
        
        if (file.extension.equals("pdf", ignoreCase = true)) {
            val qrContent = PdfUtils.extractQrCode(file)
            if (qrContent != null) {
                qrCodeData = qrContent
                val ipsData = IpsParser.parse(qrContent)
                if (ipsData != null) {
                    qrAmount = ipsData.amount
                    // FIX: Capture the refined Reference Number (Bill Number) from IPS
                    // This creates the correct dynamic ID for JKP Infostan (e.g. 2026/01-XXXXXX)
                    ipsReferenceNumber = ipsData.referenceNumber
                    
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
            android.util.Log.d("SyncReceiptsUseCase", "📄 Naplatni: ${epsData?.naplatniBroj}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 PaymentId: ${epsData?.paymentId}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Invoice: ${epsData?.invoiceNumber}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 IPS Ref: $ipsReferenceNumber")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Period: ${epsData?.periodStart} - ${epsData?.periodEnd}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 STORNO: ${epsData?.isStorno}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Parsed Amount: ${parsed.totalAmount}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 Parsed Merchant: ${parsed.merchantName}")
            android.util.Log.d("SyncReceiptsUseCase", "📄 ==================================")
            
            // Priority: QR Payer Name > EPS Recipient > Parser Recipient
            val finalRecipientName = (if (file.extension.equals("pdf", ignoreCase = true)) {
                val qrContent = PdfUtils.extractQrCode(file)
                if (qrContent != null) IpsParser.parse(qrContent)?.payerName else null
            } else null) ?: epsData?.recipientName ?: parsed.recipientName
            
            val finalRecipientAddress = epsData?.recipientAddress ?: parsed.recipientAddress

            // Priority: QR amount (if no debt) > EPS Current Monthly Charge > EPS amount > Receipt parser amount
            // SMART PARSING FIX: Prioritize 'currentMonthAmount' for Statistics accuracy
            val smartAmount = epsData?.currentMonthAmount
            val totalAmount = if (smartAmount != null && smartAmount > BigDecimal.ZERO) {
                android.util.Log.d("SyncReceiptsUseCase", "💡 USING SMART PARSING AMOUNT: $smartAmount (Original Total: ${epsData?.totalConsumption ?: parsed.totalAmount})")
                smartAmount
            } else {
                qrAmount ?: epsData?.totalConsumption ?: parsed.totalAmount ?: BigDecimal.ZERO
            }
            val finalMerchant = merchantName ?: parsed.merchantName ?: "Unknown"
            
            // Check if PAID in Firestore
            val initialStatus = if (paidIds.contains(externalId)) {
                android.util.Log.d("SyncReceiptsUseCase", "☁️ CLOUD SYNC: Marking as PAID based on Firestore")
                com.platisa.app.core.domain.model.PaymentStatus.PAID
            } else {
                com.platisa.app.core.domain.model.PaymentStatus.UNPAID
            }
            
            // Create PRELIMINARY receipt to generate Deterministic ID
            val isInfostan = finalMerchant.contains("INFOSTAN", ignoreCase = true) || 
                             finalMerchant.contains("JKP", ignoreCase = true) ||
                             finalMerchant.contains("ИНФОСТАН", ignoreCase = true)

            // FIX for Infostan: User explicitly wants "Broj računa" (top of bill) 
            // instead of "Poziv na broj" (QR RO / bottom of bill).
            // Text parser (parsed.invoiceNumber) extracts "Broj računa".
            // QR parser (ipsReferenceNumber) extracts "Poziv na broj".
            val finalInvoiceNumber = if (isInfostan) {
                 parsed.invoiceNumber ?: ipsReferenceNumber ?: epsData?.invoiceNumber
            } else {
                 ipsReferenceNumber ?: epsData?.invoiceNumber ?: parsed.invoiceNumber
            }

            val baseReceipt = Receipt(
                merchantName = finalMerchant,
                totalAmount = totalAmount,
                date = epsData?.periodEnd ?: parsed.date ?: Date(),
                dueDate = epsData?.dueDate ?: parsed.dueDate,
                imagePath = file.absolutePath,
                qrCodeData = qrCodeData,
                invoiceNumber = finalInvoiceNumber,
                naplatniNumber = epsData?.naplatniBroj,
                paymentId = epsData?.paymentId,
                isStorno = epsData?.isStorno ?: false,
                isVisible = !(epsData?.isStorno ?: false),
                originalSource = accountEmail.lowercase(),
                externalId = "", // Placeholder
                recipientName = finalRecipientName,
                recipientAddress = finalRecipientAddress,
                currentMonthAmount = epsData?.currentMonthAmount,
                previousDebtAmount = epsData?.previousDebtAmount,

                metadata = "SOURCE_EMAIL:$accountEmail|GMAIL_ID:$externalId" + 
                           (if (epsData?.electricityBaseCost != null) "|EPS_BASE_COST:${epsData.electricityBaseCost}" else "") +
                           (if (epsData?.discountDeadline != null) "|EPS_DEADLINE:${epsData.discountDeadline}" else "") +
                           (if (epsData?.discountThresholdAmount != null) "|EPS_THRESHOLD_AMOUNT:${epsData.discountThresholdAmount}" else "") +
                           (if (epsData?.discountThresholdMessage != null) "|EPS_THRESHOLD_MESSAGE:${epsData.discountThresholdMessage}" else "") +
                           (if (epsData?.isCorrection == true) "|IS_CORRECTION:true" else "") +
                           (if (parsed.discountDeadline != null && isInfostan) "|INFOSTAN_DEADLINE:${parsed.discountDeadline}" else ""), 
                paymentStatus = initialStatus
            )

            // GENERATE DETERMINISTIC ID
            val deterministicId = com.platisa.app.core.domain.util.DeterministicIdGenerator.generate(baseReceipt)
            android.util.Log.d("SyncReceiptsUseCase", "🔐 Swapping Gmail ID ($externalId) -> Deterministic ID ($deterministicId)")
            
            // SECONDARY CHECK: Does this DETERMINISTIC ID already exist?
            // (e.g. Scanned via camera but now found in Gmail)
            val existingDeterministic = repo.getReceiptByExternalId(deterministicId)
            if (existingDeterministic != null) {
                android.util.Log.d("SyncReceiptsUseCase", "🔍 Found existing deterministic match: ID=${existingDeterministic.id}, Status=${existingDeterministic.paymentStatus}")
                
                // Update PAID status if needed
                if (paidIds.contains(deterministicId) && existingDeterministic.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                    android.util.Log.d("SyncReceiptsUseCase", "🔄 Updating existing deterministic receipt to PAID based on Firestore: $deterministicId")
                    val updated = existingDeterministic.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                        updatedAt = Date(),
                        metadata = (existingDeterministic.metadata ?: "") + " [Cloud Sync (Det)]"
                    )
                    repo.updateReceipt(updated)
                }
                android.util.Log.w("SyncReceiptsUseCase", "⏭️ SKIPPING: Deterministic ID already processed: $deterministicId")
                return
            }

            val finalReceipt = baseReceipt.copy(externalId = deterministicId)
            
            // Re-Check PAID status with DETERMINISTIC ID (since that's what other devices use)
            val finalStatus = if (paidIds.contains(deterministicId)) {
                 com.platisa.app.core.domain.model.PaymentStatus.PAID
            } else {
                 finalReceipt.paymentStatus
            }
            
            // SMART ANOMALY PREVENTION:
            // Check if this new bill is suspiciously low compared to history
            var receiptWithAnomaly = finalReceipt.copy(paymentStatus = finalStatus)
            
            if (!receiptWithAnomaly.isStorno) {
                val anomaly = anomalyDetector.checkAnomaly(receiptWithAnomaly.toEntity())
                when (anomaly) {
                    is AnomalyResult.SuspiciouslyLow -> {
                        android.util.Log.w("SyncReceiptsUseCase", "⚠️ ANOMALY: Bill is too low ${receiptWithAnomaly.totalAmount} (Average: ${anomaly.average})")
                        receiptWithAnomaly = receiptWithAnomaly.copy(
                            anomalyType = "LOW_AVG",
                            anomalyMessage = "Iznos je ${String.format("%.0f", anomaly.percentOfAverage)}% proseka (${anomaly.average} RSD). Da li je ovo greška?"
                        )
                    }
                    is AnomalyResult.SuddenDrop -> {
                        android.util.Log.w("SyncReceiptsUseCase", "⚠️ ANOMALY: Sudden drop from ${anomaly.previousAmount} to ${receiptWithAnomaly.totalAmount}")
                        receiptWithAnomaly = receiptWithAnomaly.copy(
                            anomalyType = "DROP_SPIKE",
                            anomalyMessage = "Veliki pad (-${String.format("%.0f", anomaly.dropPercent)}%) u odnosu na prošli mesec (${anomaly.previousAmount} RSD)."
                        )
                    }
                    else -> {} // No anomaly
                }
            }
            
            val receiptToSave = receiptWithAnomaly

            
            try {
                // ATOMIC INSERT: Receipt + EPS Data in one transaction to prevent FK constraint errors
                val id = repo.insertReceiptWithData(
                    receipt = receiptToSave,
                    epsData = epsData,
                    billingPeriod = epsData?.billingPeriod
                )
                android.util.Log.d("SyncReceiptsUseCase", "✅ Receipt saved with ID: $id")
                android.util.Log.d("SyncReceiptsUseCase", "💰 FINAL AMOUNT: ${receiptToSave.totalAmount} | Merchant: ${receiptToSave.merchantName} | Date: ${receiptToSave.date}")
                
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

