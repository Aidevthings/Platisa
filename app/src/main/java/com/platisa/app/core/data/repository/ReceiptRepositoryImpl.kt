package com.platisa.app.core.data.repository

import com.platisa.app.core.data.database.dao.EpsDao
import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.dao.SectionDao
import com.platisa.app.core.data.database.dao.TagDao
import com.platisa.app.core.data.database.entity.EpsDataEntity
import com.platisa.app.core.data.database.entity.PaymentStatus
import com.platisa.app.core.data.helper.BillDuplicateDetector
import com.platisa.app.core.data.helper.DuplicateCheckResult
import com.platisa.app.core.data.mapper.toDomain
import com.platisa.app.core.data.mapper.toEntity
import com.platisa.app.core.domain.model.EpsData
import com.platisa.app.core.domain.model.ProductSearchResult
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.domain.SecureStorage // Added import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val sectionDao: SectionDao,
    private val tagDao: TagDao,
    private val epsDao: EpsDao,
    private val duplicateDetector: BillDuplicateDetector,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val firestoreRepository: com.platisa.app.core.data.repository.FirestoreRepository, // Injected
    private val secureStorage: com.platisa.app.core.domain.SecureStorage
) : ReceiptRepository {

    override fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getVisibleReceipts(): Flow<List<Receipt>> {
        return receiptDao.getVisibleReceipts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchReceipts(query: String): Flow<List<Receipt>> {
        return receiptDao.searchReceipts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getReceiptsByFilter(startDate: Long, endDate: Long, minAmount: Double, maxAmount: Double): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByFilter(startDate, endDate, minAmount, maxAmount).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReceiptsInRange(startDate: Long, endDate: Long): List<Receipt> {
        return receiptDao.getReceiptsInRange(startDate, endDate).map { it.toDomain() }
    }

    override suspend fun getReceiptById(id: Long): Receipt? {
        return receiptDao.getReceiptById(id)?.toDomain()
    }

    override suspend fun getReceiptByInvoiceNumber(invoiceNumber: String): Receipt? {
        return receiptDao.getReceiptByInvoiceNumber(invoiceNumber)?.toDomain()
    }
    
    override suspend fun getReceiptsByAmount(amount: java.math.BigDecimal): List<Receipt> {
        return receiptDao.getReceiptsByAmount(amount).map { it.toDomain() }
    }

    override suspend fun insertReceipt(receipt: Receipt, billingPeriod: String?): Long { // Added param
        val entity = receipt.toEntity()
        
        // LOGOVANJE ZA DEBUG
        android.util.Log.d("ReceiptRepository", "=== UMETANJE RAČUNA ===")
        android.util.Log.d("ReceiptRepository", "Račun broj: ${entity.invoiceNumber}")
        android.util.Log.d("ReceiptRepository", "Naplatni broj: ${entity.naplatniNumber}")
        android.util.Log.d("ReceiptRepository", "Payment ID: ${entity.paymentId}")
        android.util.Log.d("ReceiptRepository", "Billing Period: $billingPeriod") // Log
        android.util.Log.d("ReceiptRepository", "STORNO: ${entity.isStorno}")
        android.util.Log.d("ReceiptRepository", "Iznos: ${entity.totalAmount}")
        
        // Proveri duplikat (Pass billingPeriod)
        val duplicateCheck = duplicateDetector.checkForDuplicate(entity, billingPeriod)
        
        when (duplicateCheck) {
            is DuplicateCheckResult.StornoPaidBill -> {
                android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE: STORNO za plaćen račun!")
                throw DuplicateBillException(duplicateCheck.message)
            }
            
            is DuplicateCheckResult.DuplicatePaidBill -> {
                android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE: Duplikat plaćenog računa!")
                throw DuplicateBillException(duplicateCheck.message)
            }
            
            is DuplicateCheckResult.DuplicateUnpaidBill -> {
                android.util.Log.e("ReceiptRepository", "🛑 DUPLIKAT DETEKTOVAN - DEBUG UPDATE MODE")
                // DEBUGGING MODE: Instead of throwing, we DELETE the old one and INSERT the new one
                // This forces the "Debug Echo" address updates to be saved.
                android.util.Log.d("ReceiptRepository", "♻️ BRISANJE STAROG RAČUNA RADI AŽURIRANJA: ${duplicateCheck.existingReceipt.id}")
                receiptDao.deleteReceipt(duplicateCheck.existingReceipt)
                // Continue to insert...
            }
            
            is DuplicateCheckResult.ReplaceExisting -> {
                android.util.Log.d("ReceiptRepository", "♻️ ZAMENA: ${duplicateCheck.message}")
                receiptDao.deleteReceipt(duplicateCheck.existingReceipt)
                // Continue to insert
            }

            DuplicateCheckResult.NoDuplicate -> {
                android.util.Log.d("ReceiptRepository", "✓ Nije duplikat - nastavljam sa umetanjem")
            }
        }
        
        // Označi nove restorane za otkrivanje
        if (receipt.category == com.platisa.app.core.domain.model.BillCategory.RESTAURANT) {
            if (!preferenceManager.hasScannedRestaurantBill) {
                preferenceManager.hasScannedRestaurantBill = true
            }
        }
        
        // Pripremi račun (sakrij STORNO)
        val preparedEntity = duplicateDetector.prepareReceiptForSave(entity)
        
        val insertedId = receiptDao.insertReceipt(preparedEntity)
        android.util.Log.d("ReceiptRepository", "✅ Račun umetnut sa ID: $insertedId")
        
        return insertedId
    }

    override suspend fun insertEpsData(epsData: EpsData, receiptId: Long) {
        val entity = EpsDataEntity(
            receiptId = receiptId,
            edNumber = epsData.edNumber,
            billingPeriod = epsData.billingPeriod,
            consumptionVt = epsData.consumptionVt?.toDouble(),
            consumptionNt = epsData.consumptionNt?.toDouble(),
            totalConsumption = epsData.totalConsumption?.toDouble()
        )
        epsDao.insertEpsData(entity)
    }

    override fun getEpsAnalyticsData(): Flow<List<com.platisa.app.core.domain.model.EpsMonthData>> {
        return epsDao.getEpsAnalyticsData().map { entities ->
            try {
                val dateFormat = java.text.SimpleDateFormat("MMM", java.util.Locale("sr", "RS"))
                
                entities.filter { it.date != null }
                    .groupBy { dateFormat.format(it.date!!) }
                    .map { (month, entries) ->
                        com.platisa.app.core.domain.model.EpsMonthData(
                            month = month,
                            vtConsumption = java.math.BigDecimal(entries.sumOf { it.consumptionVt ?: 0.0 }),
                            ntConsumption = java.math.BigDecimal(entries.sumOf { it.consumptionNt ?: 0.0 }),
                            totalAmount = entries.fold(java.math.BigDecimal.ZERO) { acc, e -> acc.add(e.totalAmount) }
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("ReceiptRepository", "Greška pri mapiranju EPS analitičkih podataka", e)
                emptyList()
            }
        }
    }

    override fun getAllEpsData(): Flow<List<Pair<Long, EpsData>>> {
        return epsDao.getAllEpsData().map { entities ->
            entities.map { entity ->
                entity.receiptId to entity.toDomain()
            }
        }
    }

    override suspend fun updateReceipt(receipt: Receipt) {
        // 1. Update local DB
        receiptDao.updateReceipt(receipt.toEntity())
        
        // 2. Sync Status to Firestore
        syncPaidStatusToCloud(receipt)
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt.toEntity())
        
        // Check if we need to remove paid status (if user deletes a paid bill)
        if (receipt.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID) {
            syncPaidStatusToCloud(receipt, isPaid = false)
        }
    }

    /**
     * Helper to sync paid status to Firestore using the email from `originalSource`.
     */
    private suspend fun syncPaidStatusToCloud(receipt: Receipt, isPaid: Boolean? = null) {
        try {
            val isActuallyPaid = isPaid ?: (receipt.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID)
            val externalId = receipt.externalId
            
            // 🔍 DEBUG: Log all relevant fields
            android.util.Log.d("ReceiptRepository", "🔍 ============= SYNC PAID STATUS DEBUG =============")
            android.util.Log.d("ReceiptRepository", "🔍 Receipt ID: ${receipt.id}")
            android.util.Log.d("ReceiptRepository", "🔍 External ID: $externalId")
            android.util.Log.d("ReceiptRepository", "🔍 Original Source: ${receipt.originalSource}")
            android.util.Log.d("ReceiptRepository", "🔍 Merchant: ${receipt.merchantName}")
            android.util.Log.d("ReceiptRepository", "🔍 Is Paid: $isActuallyPaid")
            android.util.Log.d("ReceiptRepository", "🔍 Metadata: ${receipt.metadata}")
            
            // UNIVERSAL SHARING LOGIC: 
            // 1. If it's a Gmail receipt, sync to that Gmail's shared folder.
            // 2. If it's Manual/Camera, sync to the Main User's folder (fallback).
            var email = receipt.originalSource
            
            android.util.Log.d("ReceiptRepository", "🔍 Step 1 - Raw email from originalSource: $email")
            
            // Check if source is a valid email (simple check)
            if (email == "Manual" || email == "Camera" || email == "GMAIL" || email == "CAMERA" || !email.contains("@")) {
                val firebaseEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                android.util.Log.d("ReceiptRepository", "🔍 Step 2 - Not an email, falling back to Firebase: $firebaseEmail")
                email = firebaseEmail ?: ""
            }

            if (email.isNotBlank() && !externalId.isNullOrBlank()) {
                // FORCE LOWERCASE: Firestore IDs are case sensitive, we use lowercase standardization
                val normalizedEmail = email.lowercase()
                android.util.Log.d("ReceiptRepository", "☁️ SAVING to Firestore: shared_receipts/$normalizedEmail/receipts/$externalId")
                android.util.Log.d("ReceiptRepository", "☁️ Syncing SHARED Paid Status ($isActuallyPaid) for $normalizedEmail / $externalId")
                firestoreRepository.savePaidStatus(normalizedEmail, externalId, isActuallyPaid)
            } else {
                android.util.Log.w("ReceiptRepository", "⚠️ SKIPPED SYNC: email='$email' (blank=${email.isBlank()}), externalId='$externalId' (null/blank=${externalId.isNullOrBlank()})")
            }
            android.util.Log.d("ReceiptRepository", "🔍 =====================================================")
        } catch (e: Exception) {
            android.util.Log.e("ReceiptRepository", "⚠️ Failed to sync paid status to cloud: ${e.message}")
        }
    }


    override suspend fun getReceiptByPath(imagePath: String): Receipt? {
        return receiptDao.getReceiptByPath(imagePath)?.toDomain()
    }

    override suspend fun getReceiptByExternalId(externalId: String): Receipt? {
        return receiptDao.getReceiptByExternalId(externalId)?.toDomain()
    }

    override suspend fun getEpsDataForReceipt(receiptId: Long): EpsData? {
        return epsDao.getEpsDataForReceipt(receiptId)?.toDomain()
    }

    // Implementacija stavki
    override suspend fun insertReceiptItems(items: List<com.platisa.app.core.domain.model.ReceiptItem>, receiptId: Long) {
        val entities = items.map { item ->
            com.platisa.app.core.data.database.entity.ReceiptItemEntity(
                receiptId = receiptId,
                name = item.name,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.total,
                label = item.label
            )
        }
        receiptDao.insertReceiptItems(entities)
    }

    override suspend fun getReceiptItems(receiptId: Long): List<com.platisa.app.core.domain.model.ReceiptItem> {
        return receiptDao.getReceiptItems(receiptId).map { entity ->
            com.platisa.app.core.domain.model.ReceiptItem(
                name = entity.name,
                quantity = entity.quantity,
                unitPrice = entity.unitPrice,
                total = entity.totalPrice,
                label = entity.label
            )
        }
    }

    override fun searchItems(query: String): Flow<List<ProductSearchResult>> {
        return receiptDao.searchItems(query).map { entities ->
            entities.map { entity ->
                ProductSearchResult(
                    id = entity.item.id,
                    name = entity.item.name,
                    quantity = entity.item.quantity,
                    unitPrice = entity.item.unitPrice ?: java.math.BigDecimal.ZERO,
                    total = entity.item.totalPrice ?: java.math.BigDecimal.ZERO,
                    merchantName = entity.merchantName,
                    date = entity.date,
                    currency = "RSD"
                )
            }
        }
    }

    /**
     * Čisti duplikate računa iz baze podataka.
     * Prioritet za zadržavanje računa:
     * 1. PLAĆENI računi (uvek zadrži)
     * 2. Računi sa kompletnijim podacima (QR kod, stavke, itd.)
     * 3. Najnoviji račun (najveći ID)
     */
    override suspend fun deleteDuplicateReceipts(): Int {
        val allReceipts = receiptDao.getAllReceiptsList()
        val toDeleteIds = mutableSetOf<Long>()

        // Pomoćna funkcija za bodovanje kompletnosti računa
        fun scoreReceipt(receipt: com.platisa.app.core.data.database.entity.ReceiptEntity): Int {
            var score = 0
            if (receipt.paymentStatus == PaymentStatus.PAID) score += 100  // Uvek zadrži plaćene
            if (!receipt.qrCodeData.isNullOrEmpty()) score += 10
            if (!receipt.invoiceNumber.isNullOrEmpty()) score += 5
            if (!receipt.paymentId.isNullOrEmpty()) score += 5
            if (!receipt.naplatniNumber.isNullOrEmpty()) score += 3
            score += (receipt.id / 1000).toInt().coerceAtMost(10)  // Blaga prednost za novije
            return score
        }

        // 1. Proveri PaymentId duplikate PRVO (najpouzdanije za komunalne račune)
        allReceipts.filter { !it.paymentId.isNullOrEmpty() }
            .groupBy { it.paymentId!! }
            .forEach { (paymentId, duplicates) ->
                if (duplicates.size > 1) {
                    android.util.Log.d("ReceiptRepository", "Pronađeno ${duplicates.size} duplikata sa PaymentId: $paymentId")
                    val sorted = duplicates.sortedByDescending { scoreReceipt(it) }
                    val toKeep = sorted.first()
                    val toDelete = sorted.drop(1)
                    android.util.Log.d("ReceiptRepository", "  Zadržavam ID=${toKeep.id} (bodovi=${scoreReceipt(toKeep)})")
                    toDelete.forEach { 
                        android.util.Log.d("ReceiptRepository", "  Brišem ID=${it.id} (bodovi=${scoreReceipt(it)})")
                    }
                    toDeleteIds.addAll(toDelete.map { it.id })
                }
            }

        // 2. Proveri duplikate broja računa (za nekomunalne račune)
        allReceipts.filter { !it.invoiceNumber.isNullOrEmpty() && !toDeleteIds.contains(it.id) }
            .groupBy { it.invoiceNumber!! }
            .forEach { (invoiceNumber, duplicates) ->
                if (duplicates.size > 1) {
                    android.util.Log.d("ReceiptRepository", "Pronađeno ${duplicates.size} duplikata sa brojem računa: $invoiceNumber")
                    val sorted = duplicates.sortedByDescending { scoreReceipt(it) }
                    val toDelete = sorted.drop(1)
                    toDeleteIds.addAll(toDelete.map { it.id })
                }
            }

        // 3. Proveri Naplatni broj + Iznos duplikate (fallback za nedostajući PaymentId)
        allReceipts.filter { 
            !it.naplatniNumber.isNullOrEmpty() && 
            it.paymentId.isNullOrEmpty() && 
            !toDeleteIds.contains(it.id) 
        }
        .groupBy { "${it.naplatniNumber}-${it.totalAmount}" }
        .forEach { (key, duplicates) ->
            if (duplicates.size > 1) {
                android.util.Log.d("ReceiptRepository", "Pronađeno ${duplicates.size} duplikata sa Naplatni+Iznos: $key")
                val sorted = duplicates.sortedByDescending { scoreReceipt(it) }
                val toDelete = sorted.drop(1)
                toDeleteIds.addAll(toDelete.map { it.id })
            }
        }

        if (toDeleteIds.isNotEmpty()) {
            android.util.Log.d("ReceiptRepository", "🗑️ Čišćenje ${toDeleteIds.size} duplikata računa")
            receiptDao.deleteReceiptsById(toDeleteIds.toList())
        } else {
            android.util.Log.d("ReceiptRepository", "✓ Nema duplikata za čišćenje")
        }
        
        return toDeleteIds.size
    }
    
    override suspend fun deleteAllReceipts() {
        android.util.Log.d("ReceiptRepository", "🗑️ Brisanje SVIH računa...")
        receiptDao.deleteAllReceipts()
        android.util.Log.d("ReceiptRepository", "✅ Svi računi obrisani")
    }
    
    override suspend fun deleteAllReceiptItems() {
        android.util.Log.d("ReceiptRepository", "🗑️ Brisanje SVIH stavki računa...")
        receiptDao.deleteAllReceiptItems()
        android.util.Log.d("ReceiptRepository", "✅ Sve stavke obrisane")
    }
    
    override suspend fun deleteAllEpsData() {
        android.util.Log.d("ReceiptRepository", "🗑️ Brisanje SVIH EPS podataka...")
        epsDao.deleteAllEpsData()
        android.util.Log.d("ReceiptRepository", "✅ Svi EPS podaci obrisani")
    }

    override suspend fun deleteGmailReceipts() {
        android.util.Log.d("ReceiptRepository", "🗑️ Brisanje SAMO GMAIL računa...")
        receiptDao.deleteReceiptsBySource(com.platisa.app.core.data.database.entity.SourceType.GMAIL)
        android.util.Log.d("ReceiptRepository", "✅ Gmail računi obrisani (ostali sačuvani)")
    }

    override suspend fun markPastBillsAsPaid(merchantName: String, excludeReceiptId: Long) {
        // 1. Fetch ALL unpaid bills to perform fuzzy matching in memory
        // SQL is too strict ("Mts" != "Mts d.o.o."), so we handle it here.
        val allReceipts = receiptDao.getAllReceiptsList()
        val unpaidCandidates = allReceipts.filter { 
            it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.UNPAID && 
            it.id != excludeReceiptId 
        }

        val matchedBills = unpaidCandidates.filter { candidate ->
            val dbName = candidate.merchantName.lowercase().replace(" ", "").replace(".", "").replace("-", "")
            val targetName = merchantName.lowercase().replace(" ", "").replace(".", "").replace("-", "")
            
            // A. Direct Fuzzy Match
            var match = dbName.contains(targetName) || targetName.contains(dbName)
            
            // B. Alias Match (Critical for OCR variations)
            if (!match) {
                val aliases = mapOf(
                    "mts" to listOf("telekom", "telekom srbija", "mts"),
                    "telekom" to listOf("mts", "telekom srbija", "telekom"),
                    "telenor" to listOf("yettel", "telenor", "mobi banka", "mobibanka"),
                    "yettel" to listOf("telenor", "yettel", "mobi banka", "mobibanka"),
                    "eps" to listOf("eps", "elektroprivreda", "struja", "eps distribucija", "eps snabdevanje"),
                    "infostan" to listOf("jkp infostan", "infostan tehnologije", "infostan"),
                    "sbb" to listOf("sbb", "serbian broadband"),
                    "a1" to listOf("vip", "vip mobile", "a1"),
                    "vip" to listOf("a1", "vip", "vip mobile")
                )
                
                for ((key, values) in aliases) {
                    val safeKey = key.replace(" ", "")
                    // Check if target matches an alias Key or Value
                    val isTargetInGroup = targetName.contains(safeKey) || values.any { targetName.contains(it.replace(" ", "")) }
                    
                    if (isTargetInGroup) {
                        // If target is in this group, check if candidate is also in this group
                        if (dbName.contains(safeKey) || values.any { dbName.contains(it.replace(" ", "")) }) {
                            match = true
                            break
                        }
                    }
                }
            }
            
            if (match) {
                 android.util.Log.d("ReceiptRepository", "      ✅ MATCH FOUND: '${candidate.merchantName}' matches '$merchantName'")
            }
            match
        }

        if (matchedBills.isNotEmpty()) {
            android.util.Log.d("ReceiptRepository", "🔄 CASCADE: Marking ${matchedBills.size} bills as PAID.")
            
            matchedBills.forEach { receipt ->
                android.util.Log.d("ReceiptRepository", "   -> Updating Bill ${receipt.id} (${receipt.merchantName})")
                val updated = receipt.copy(
                    paymentStatus = com.platisa.app.core.data.database.entity.PaymentStatus.PAID,
                    updatedAt = java.util.Date(),
                    metadata = (receipt.metadata ?: "") + " [Cascaded]"
                )
                receiptDao.updateReceipt(updated)
                
                // Cloud Sync
                syncPaidStatusToCloud(updated.toDomain(), isPaid = true)
            }
        } else {
            android.util.Log.w("ReceiptRepository", "⚠️ NO MATCHING PAST BILLS found for '$merchantName'")
        }
    }


    override suspend fun isLatestReceipt(merchantName: String, receiptDate: java.util.Date): Boolean {
        val latestDateTimestamp = receiptDao.getLatestReceiptDateForMerchant(merchantName) ?: return true
        // If current receipt date is >= latest date in DB, it is the latest (or one of them).
        // Using timestamp comparison
        return receiptDate.time >= latestDateTimestamp
    }

    override suspend fun deleteAllPaidStatuses(sourceEmail: String) {
        firestoreRepository.deleteAllPaidStatuses(sourceEmail)
    }

    override suspend fun insertReceipts(receipts: List<Receipt>): List<Long> {
        android.util.Log.d("ReceiptRepository", "=== GRUPNO UMETANJE ${receipts.size} RAČUNA ===")
        
        val preparedEntities = mutableListOf<com.platisa.app.core.data.database.entity.ReceiptEntity>()
        var skippedCount = 0
        
        for (receipt in receipts) {
            val entity = receipt.toEntity()
            val duplicateCheck = duplicateDetector.checkForDuplicate(entity)
            
            val shouldInsert = when (duplicateCheck) {
                is DuplicateCheckResult.StornoPaidBill -> {
                    android.util.Log.w("ReceiptRepository", "🛑 PRESKOČENO: STORNO za plaćen račun")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.DuplicatePaidBill -> {
                    android.util.Log.w("ReceiptRepository", "🛑 PRESKOČENO: Duplikat plaćenog računa")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.DuplicateUnpaidBill -> {
                    android.util.Log.w("ReceiptRepository", "🛑 PRESKOČENO: Duplikat neplaćenog računa - ${duplicateCheck.message}")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.ReplaceExisting -> {
                    android.util.Log.d("ReceiptRepository", "♻️ ZAMENA (Bulk): ${duplicateCheck.message}")
                    receiptDao.deleteReceipt(duplicateCheck.existingReceipt)
                    true
                }
                DuplicateCheckResult.NoDuplicate -> true
            }
            
            if (shouldInsert) {
                if (receipt.category == com.platisa.app.core.domain.model.BillCategory.RESTAURANT) {
                    if (!preferenceManager.hasScannedRestaurantBill) {
                        preferenceManager.hasScannedRestaurantBill = true
                    }
                }
                preparedEntities.add(duplicateDetector.prepareReceiptForSave(entity))
            }
        }
        
        android.util.Log.d("ReceiptRepository", "📊 Statistika: ${preparedEntities.size} za umetanje, $skippedCount duplikata preskočeno")
        
        if (preparedEntities.isNotEmpty()) {
            val ids = receiptDao.insertReceipts(preparedEntities)
            android.util.Log.d("ReceiptRepository", "✅ Grupno umetnuto ${ids.size} računa")
            return ids
        }
        
        return emptyList()
    }
}

class DuplicateBillException(message: String) : Exception(message)

