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

            // REMOVED: Check 0 (PaymentId) - Legacy rule, replaced by Safe Check above.
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
     * Normalizuje string za poređenje.
     * ISPRAVLJENO: Sada uključuje ćirilična slova (srpska: А-Яа-я + specijalna slova)
     */
    private fun normalizeString(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        
        // Zadrži: a-z, A-Z, 0-9, Ćirilica (U+0400-U+04FF pokriva svu ćirilicu uključujući srpsku)
        return input
            .replace(Regex("[^a-zA-Z0-9\\u0400-\\u04FF]"), "")
            .lowercase()
    }
    
    /**
     * Normalizuje ime trgovca za poređenje.
     */
    private fun normalizeMerchant(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9\\u0400-\\u04FF]"), "")
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
        
        existingReceipts.forEachIndexed { index, existing ->
            android.util.Log.w("BillDuplicateDetector", "  [$index] ID=${existing.id}, Račun=${existing.invoiceNumber}, Status=${existing.paymentStatus}, STORNO=${existing.isStorno}")
        }
        
        // SCENARIO 1: Novi račun je STORNO
        if (newReceipt.isStorno) {
            val paidReceipt = existingReceipts.find { it.paymentStatus == PaymentStatus.PAID }
            if (paidReceipt != null) {
                android.util.Log.w("BillDuplicateDetector", "⛔ STORNO za već plaćen račun - blokiranje!")
                return DuplicateCheckResult.StornoPaidBill(
                    existingReceipt = paidReceipt,
                    message = "STORNO za već plaćen račun!",
                    shouldBlock = true,
                    shouldHide = true
                )
            }
            
            // NOVO: Blokiraj STORNO ako postoji bilo koji račun za isti period
            // Ne treba nam STORNO kopija ako već imamo originalni račun
            val existingForSamePeriod = existingReceipts.firstOrNull()
            if (existingForSamePeriod != null) {
                android.util.Log.w("BillDuplicateDetector", "🚫 STORNO račun blokiran - originalni račun već postoji (ID=${existingForSamePeriod.id})")
                return DuplicateCheckResult.DuplicateUnpaidBill(
                    existingReceipt = existingForSamePeriod,
                    message = "STORNO račun - originalni račun već postoji",
                    shouldWarn = false
                )
            }
            
            // Ako nema originalnog računa, dozvoli STORNO (redak slučaj)
            android.util.Log.d("BillDuplicateDetector", "✓ STORNO bez originalnog računa - dozvoljeno")
            return DuplicateCheckResult.NoDuplicate
        }
        
        // SCENARIO 2: Novi račun je regularan (nije STORNO)
        
        // Proveri da li već postoji PLAĆENA verzija
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
        
        // Proveri duplikat NEPLAĆENOG regularnog računa (nije STORNO)
        val regularUnpaidReceipt = existingReceipts.find { !it.isStorno && it.paymentStatus != PaymentStatus.PAID }
        if (regularUnpaidReceipt != null) {
            android.util.Log.w("BillDuplicateDetector", "🛑 Duplikat neplaćenog računa - blokiranje!")
            return DuplicateCheckResult.DuplicateUnpaidBill(
                existingReceipt = regularUnpaidReceipt,
                message = "Duplikat računa! ($matchedBy)",
                shouldWarn = false
            )
        }
        
        // Proveri postojeći STORNO - ako imamo STORNO a sada dobijamo regularan,
        // Korisnik smatra STORNO duplikatom koji treba zameniti ORIGINALOM.
        val stornoReceipt = existingReceipts.find { it.isStorno }
        if (stornoReceipt != null) {
            android.util.Log.w("BillDuplicateDetector", "♻️ ZAMENA: Pronađen STORNO, menjam ga sa Originalnim računom")
            return DuplicateCheckResult.ReplaceExisting(
                existingReceipt = stornoReceipt,
                message = "Zamenjujem STORNO ($matchedBy) sa Originalom"
            )
        }
        
        return DuplicateCheckResult.NoDuplicate
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

