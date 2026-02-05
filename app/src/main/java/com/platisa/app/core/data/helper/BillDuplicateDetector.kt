package com.platisa.app.core.data.helper

import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.entity.PaymentStatus
import com.platisa.app.core.data.database.entity.ReceiptEntity
import java.util.Calendar
import javax.inject.Inject

/**
 * Helper klasa za detekciju duplikata i upravljanje STORNO računima.
 * 
 * PRIORITET DETEKCIJE DUPLIKATA:
 * 1. PaymentId podudaranje (naplatni broj + period obračuna)
 * 2. Broj računa podudaranje (normalizovano, podržava ćirilicu)
 * 3. Naplatni broj + Iznos + Blizak datum
 * 4. Iznos + Trgovac + Ista nedelja (strogi fallback)
 */
class BillDuplicateDetector @Inject constructor(
    private val receiptDao: ReceiptDao
) {

    /**
     * Proverava da li je račun duplikat pre nego što se doda u bazu.
     */
    suspend fun checkForDuplicate(receipt: ReceiptEntity, billingPeriod: String? = null): DuplicateCheckResult {
        android.util.Log.d("BillDuplicateDetector", "=== PROVERA DUPLIKATA (v3) ===")
        android.util.Log.d("BillDuplicateDetector", "Ulaz: Račun=${receipt.invoiceNumber}, PaymentId=${receipt.paymentId}")
        android.util.Log.d("BillDuplicateDetector", "Ulaz: BillingPeriod=$billingPeriod, Iznos=${receipt.totalAmount}")
        
        // 1. Dohvati sve kandidate iz istog meseca (koristi timestamp opseg zbog timezone problema)
        val calendar = java.util.Calendar.getInstance()
        calendar.time = receipt.date
        
        // Postavi na prvi dan meseca 00:00:00
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        // Dodaj buffer (idi 5 dana unazad za granične slučajeve)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -5)
        val startRange = calendar.timeInMillis
        
        // Postavi na poslednji dan meseca + 5 dana buffer
        calendar.time = receipt.date
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 5)
        val endRange = calendar.timeInMillis
        
        val candidates = receiptDao.getReceiptsInRange(startRange, endRange)
        
        // ===========================================
        // PROVERA (NEW SAFE): Billing Period + Amount
        // Ovo je "Safety Net" ako PaymentId nije uspeo
        // ===========================================
        if (!billingPeriod.isNullOrEmpty()) {
            val periodDuplicates = receiptDao.findByBillingPeriodAndAmount(billingPeriod, receipt.totalAmount.toDouble())
            
            for (existing in periodDuplicates) {
                if (existing.id == receipt.id) continue // Skip self

                // LOGIC: Same Company -> Ignore Naplatni (Block). Different Company -> Check Naplatni.
                val isSameMerchant = normalizeMerchant(existing.merchantName) == normalizeMerchant(receipt.merchantName)
                
                if (isSameMerchant) {
                    // NEW SAFE CHECK: Even if it's same merchant, if we have Naplatni Number, they MUST match.
                    // This prevents collisions if user has two EPS accounts with same amount.
                    val bothHaveNaplatni = !receipt.naplatniNumber.isNullOrEmpty() && !existing.naplatniNumber.isNullOrEmpty()
                    if (bothHaveNaplatni && receipt.naplatniNumber != existing.naplatniNumber) {
                        android.util.Log.d("BillDuplicateDetector", "✓ Nije duplikat: Isti period+iznos+trgovac, ali RAZLIČIT naplatni broj (${receipt.naplatniNumber} vs ${existing.naplatniNumber})")
                        continue
                    }

                    android.util.Log.w("BillDuplicateDetector", "🚨 DUPLIKAT (SAFE): Period + Amount + Isti Trgovac")
                    val result = evaluateDuplicates(
                        receipt, 
                        listOf(existing), 
                        "Isti period ($billingPeriod), iznos i trgovac"
                    )
                    if (result !is DuplicateCheckResult.NoDuplicate) return result
                } else {
                    // Trgovci su različiti (npr. Struja vs Gas, ili loš parse)
                    // Proveri Naplatni Broj kao tie-breaker
                    val hasSameNaplatni = !receipt.naplatniNumber.isNullOrEmpty() && 
                                          !existing.naplatniNumber.isNullOrEmpty() && 
                                          receipt.naplatniNumber == existing.naplatniNumber
                                          
                    if (hasSameNaplatni) {
                        android.util.Log.w("BillDuplicateDetector", "🚨 DUPLIKAT (SAFE): Različit Trgovac ali Isti Naplatni")
                        val result = evaluateDuplicates(
                            receipt, 
                            listOf(existing), 
                            "Isti period, iznos i naplatni broj (različit trgovac)"
                        )
                        if (result !is DuplicateCheckResult.NoDuplicate) return result
                    } else {
                        android.util.Log.d("BillDuplicateDetector", "✓ Nije duplikat: Isti period+iznos, ali različit trgovac i naplatni.")
                    }
                }
            }
        }

        val normalizedInputInvoice = normalizeString(receipt.invoiceNumber)
        
        for (existing in candidates) {
            // PRESKOČI sebe (ako se ažurira)
            if (existing.id == receipt.id) continue
            
            var isDuplicate = false
            var matchReason = ""

            // ============================================================
            // PROVERA 0: External ID (Deterministic ID) - ABSOLUTE MATCH
            // ============================================================
            if (!receipt.externalId.isNullOrEmpty() && existing.externalId == receipt.externalId) {
                 isDuplicate = true
                 matchReason = "Isti Deterministic ID (${receipt.externalId})"
                 android.util.Log.w("BillDuplicateDetector", "🎯 PROVERA 0: Deterministic ID podudaranje!")
            }

            // ============================================================
            // PROVERA 1: Broj računa (normalizovan sa podrškom za ćirilicu)
            // ============================================================
            if (!isDuplicate) {
                val normalizedExistingInvoice = normalizeString(existing.invoiceNumber)
                if (normalizedInputInvoice.isNotEmpty() && normalizedExistingInvoice.isNotEmpty()) {
                    if (normalizedInputInvoice == normalizedExistingInvoice) {
                        isDuplicate = true
                        matchReason = "Isti broj računa ($normalizedInputInvoice)"
                        android.util.Log.w("BillDuplicateDetector", "🎯 PROVERA 1: Broj računa podudaranje!")
                    }
                }
            }
            
            // REMOVED: Check #2 (Naplatni + Iznos) - Too aggressive
            // REMOVED: Check #3 (Merchant + Amount + Date) - Too aggressive

            if (isDuplicate) {
                android.util.Log.w("BillDuplicateDetector", "🚨 DUPLIKAT PRONAĐEN: $matchReason")
                android.util.Log.w("BillDuplicateDetector", "   Postojeći: ID=${existing.id}, Račun=${existing.invoiceNumber}, PaymentId=${existing.paymentId}")
                val result = evaluateDuplicates(receipt, listOf(existing), matchReason)
                if (result !is DuplicateCheckResult.NoDuplicate) return result
            }
        }
        
        android.util.Log.d("BillDuplicateDetector", "✓ Duplikat nije pronađen")
        return DuplicateCheckResult.NoDuplicate
    }
    
    /**
     * Normalizuje string za poređenje. (Koristi jedinstvenu logiku za sinhronizaciju)
     */
    private fun normalizeString(input: String?): String {
        return com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(input)
    }
    
    /**
     * Normalizuje ime trgovca za poređenje. (Koristi jedinstvenu logiku za sinhronizaciju)
     */
    private fun normalizeMerchant(name: String): String {
        return com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(name)
    }
    
    /**
     * Evaluira postojeće račune i vraća odgovarajući rezultat duplikata.
     */
    private fun evaluateDuplicates(
        newReceipt: ReceiptEntity,
        existingReceipts: List<ReceiptEntity>,
        matchedBy: String
    ): DuplicateCheckResult {
        android.util.Log.w("BillDuplicateDetector", "EVALUACIJA ${existingReceipts.size} duplikata po kriterijumu: $matchedBy")
        
        val newScore = calculateScore(newReceipt)
        android.util.Log.d("BillDuplicateDetector", "📊 Kvalitet NOVOG skena: $newScore (isStorno=${newReceipt.isStorno})")

        existingReceipts.forEachIndexed { index, existing ->
            val existingScore = calculateScore(existing)
            android.util.Log.w("BillDuplicateDetector", "  [$index] ID=${existing.id}, Račun=${existing.invoiceNumber}, Status=${existing.paymentStatus}, STORNO=${existing.isStorno}, SCORE=$existingScore")
        }
        
        // SCENARIO 1: Novi račun je STORNO (Sistematski inferioran)
        if (newReceipt.isStorno) {
            val anyRegularReceipt = existingReceipts.find { !it.isStorno }
            if (anyRegularReceipt != null) {
                android.util.Log.w("BillDuplicateDetector", "🚫 STORNO račun blokiran - originalni regularni račun već postoji.")
                return DuplicateCheckResult.DuplicateUnpaidBill(
                    existingReceipt = anyRegularReceipt,
                    message = "STORNO račun preskočen jer original već postoji",
                    shouldWarn = false
                )
            }
        }
        
        // SCENARIO 2: Provera PLAĆENIH računa
        val paidReceipt = existingReceipts.find { it.paymentStatus == PaymentStatus.PAID }
        if (paidReceipt != null) {
            android.util.Log.w("BillDuplicateDetector", "🛑 Duplikat plaćenog računa - blokiranje!")
            return DuplicateCheckResult.DuplicatePaidBill(
                existingReceipt = paidReceipt,
                message = "Ovaj račun je već plaćen! ($matchedBy)",
                shouldBlock = true,
                shouldHide = false
            )
        }
        
        // SCENARIO 2.5: Zamena STORNO računa REGULARNIM (Uvek dozvoljeno)
        // Ako već imamo STORNO, a stiže nam REGULARAN -> To je verovatno ispravka.
        // Bez obzira na "score", regularan račun uvek pobeđuje storno.
        val existingStorno = existingReceipts.find { it.isStorno }
        if (!newReceipt.isStorno && existingStorno != null) {
            android.util.Log.w("BillDuplicateDetector", "♻️ STORNO-FIX: Zamenjujem postojeći STORNO id=${existingStorno.id} sa novim REGULARNIM.")
            return DuplicateCheckResult.ReplaceExisting(
                existingReceipt = existingStorno,
                message = "Automatska zamena STORNO računa sa ispravnim."
            )
        }
        
        // SCENARIO 2.6: EPS CORRECTION / REPLACEMENT
        // If it's EPS, we handle replacements more liberally if the invoice number changes.
        val isEps = newReceipt.merchantName.lowercase().contains("eps") || matchedBy.contains("PaymentId")
        if (isEps) {
            // Check for different amount
            val differentAmountDuplicate = existingReceipts.find { 
                Math.abs(it.totalAmount.toDouble() - newReceipt.totalAmount.toDouble()) > 1.0 
            }
            
            // NEW: Check for different invoice number (The "Tie-Breaker" for corrections with same amount)
            val differentInvoiceDuplicate = existingReceipts.find { 
                !it.invoiceNumber.isNullOrEmpty() && 
                !newReceipt.invoiceNumber.isNullOrEmpty() && 
                it.invoiceNumber != newReceipt.invoiceNumber
            }

            if (differentAmountDuplicate != null) {
                android.util.Log.w("BillDuplicateDetector", "♻️ EPS-CORRECTION: New amount (${newReceipt.totalAmount}) differs from old. Replacing.")
                return DuplicateCheckResult.ReplaceExisting(
                    existingReceipt = differentAmountDuplicate,
                    message = "Novi iznos za isti nalog/period (Korekcija)."
                )
            } else if (differentInvoiceDuplicate != null) {
                android.util.Log.w("BillDuplicateDetector", "♻️ EPS-REPLACEMENT: Same amount but different Invoice Number (${newReceipt.invoiceNumber}). Replacing.")
                return DuplicateCheckResult.ReplaceExisting(
                    existingReceipt = differentInvoiceDuplicate,
                    message = "Novi broj računa za isti nalog/period (Zamena)."
                )
            }
        }

        // SCENARIO 3: Kvalitativno poređenje (Survival of the Fittest)
        // Ako novi račun ima bolji ili jednak skor od postojećeg, dozvoljavamo zamenu.
        // Npr. ako smo imali STORNO, a sad imamo Regularni -> Zameni.
        // Npr. ako smo imali OCR bez QR-a, a sad imamo QR -> Zameni.
        val bestExisting = existingReceipts.maxByOrNull { calculateScore(it) }
        if (bestExisting != null) {
            val existingScore = calculateScore(bestExisting)
            
            if (newScore > existingScore) {
                android.util.Log.w("BillDuplicateDetector", "♻️ KVALITATIVNA ZAMENA: Novi sken ($newScore) je bolji od starog ($existingScore)")
                return DuplicateCheckResult.ReplaceExisting(
                    existingReceipt = bestExisting,
                    message = "Zamenjujem lošiji sken ($existingScore) sa boljim ($newScore)"
                )
            } else if (newScore < existingScore) {
                android.util.Log.w("BillDuplicateDetector", "🚫 BLOKIRANJE: Novi sken ($newScore) je lošiji od postojećeg ($existingScore)")
                return DuplicateCheckResult.DuplicateUnpaidBill(
                    existingReceipt = bestExisting,
                    message = "Lošiji sken preskočen",
                    shouldWarn = false
                )
            }
        }

        // SCENARIO 4: Identičan kvalitet - standardno blokiranje duplikata
        val regularUnpaidReceipt = existingReceipts.firstOrNull()
        if (regularUnpaidReceipt != null) {
            android.util.Log.w("BillDuplicateDetector", "🛑 Identičan duplikat - blokiranje!")
            return DuplicateCheckResult.DuplicateUnpaidBill(
                existingReceipt = regularUnpaidReceipt,
                message = "Duplikat računa! ($matchedBy)",
                shouldWarn = false
            )
        }
        
        return DuplicateCheckResult.NoDuplicate
    }
    
    /**
     * Izračunava "skor kvaliteta" računa na osnovu dostupnih podataka.
     * Viši skor znači pouzdaniji račun.
     */
    private fun calculateScore(receipt: ReceiptEntity): Int {
        var score = 0
        
        // 1. Platni status (Najbitnije)
        if (receipt.paymentStatus == PaymentStatus.PAID) score += 100
        
        // 2. Tip računa (Regularni > Storno)
        if (!receipt.isStorno) score += 40
        
        // 3. QR kôd (Visoka pouzdanost)
        if (!receipt.qrCodeData.isNullOrBlank()) score += 30
        
        // 4. Broj računa (Postojanje i validnost)
        if (!receipt.invoiceNumber.isNullOrBlank()) {
            score += 10
            // Ako je broj dugačak (pravi EPS broj), dodaj još poena
            if (receipt.invoiceNumber!!.length >= 8) score += 10
        }
        
        // 5. Payment ID (Kompletiran EPS podatak)
        if (!receipt.paymentId.isNullOrBlank()) score += 10
        
        // 6. Correction Flag (Metadata)
        if (receipt.metadata?.contains("IS_CORRECTION:true") == true) {
            score += 50
            android.util.Log.d("BillDuplicateDetector", "🚀 Score BOOST for Correction (+50)")
        }
        
        return score
    }

    /**
     * Priprema račun za čuvanje - automatski sakriva STORNO račune.
     */
    fun prepareReceiptForSave(receipt: ReceiptEntity): ReceiptEntity {
        return if (receipt.isStorno) {
            android.util.Log.d("BillDuplicateDetector", "STORNO račun detektovan - sakrivanje iz liste")
            receipt.copy(isVisible = false)
        } else {
            receipt
        }
    }
    
    /**
     * Čišćenje starih STORNO računa.
     * Podrazumevano: briše STORNO račune starije od 7 dana.
     */
    suspend fun cleanupOldStornoBills(retentionDays: Int = 7): CleanupResult {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -retentionDays)
        val cutoffDate = calendar.timeInMillis
        
        val oldStornoBills = receiptDao.getOldStornoReceipts(cutoffDate)
        
        if (oldStornoBills.isEmpty()) {
            return CleanupResult(
                deleted = 0,
                message = "Nema STORNO računa za brisanje"
            )
        }
        
        val idsToDelete = oldStornoBills.map { it.id }
        receiptDao.deleteReceiptsById(idsToDelete)
        
        android.util.Log.d("BillDuplicateDetector", "Obrisano ${oldStornoBills.size} starih STORNO računa")
        
        return CleanupResult(
            deleted = oldStornoBills.size,
            message = "Obrisano ${oldStornoBills.size} STORNO računa starijih od $retentionDays dana",
            deletedBills = oldStornoBills
        )
    }
}

/**
 * Rezultat provere duplikata.
 */
sealed class DuplicateCheckResult {
    object NoDuplicate : DuplicateCheckResult()
    
    data class StornoPaidBill(
        val existingReceipt: ReceiptEntity,
        val message: String,
        val shouldBlock: Boolean,
        val shouldHide: Boolean
    ) : DuplicateCheckResult()
    
    data class DuplicatePaidBill(
        val existingReceipt: ReceiptEntity,
        val message: String,
        val shouldBlock: Boolean,
        val shouldHide: Boolean
    ) : DuplicateCheckResult()
    
    data class DuplicateUnpaidBill(
        val existingReceipt: ReceiptEntity,
        val message: String,
        val shouldWarn: Boolean
    ) : DuplicateCheckResult()

    data class ReplaceExisting(
        val existingReceipt: ReceiptEntity,
        val message: String
    ) : DuplicateCheckResult()
}

/**
 * Rezultat cleanup operacije.
 */
data class CleanupResult(
    val deleted: Int,
    val message: String,
    val deletedBills: List<ReceiptEntity> = emptyList()
)

