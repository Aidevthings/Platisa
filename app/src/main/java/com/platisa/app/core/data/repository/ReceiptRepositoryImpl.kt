package com.platisa.app.core.data.repository

import androidx.room.withTransaction
import com.platisa.app.core.data.database.PlatisaDatabase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val sectionDao: SectionDao,
    private val tagDao: TagDao,
    private val epsDao: EpsDao,
    private val duplicateDetector: BillDuplicateDetector,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val firestoreRepository: com.platisa.app.core.data.repository.FirestoreRepository, // Injected
    private val secureStorage: com.platisa.app.core.domain.SecureStorage,
    private val database: PlatisaDatabase
) : ReceiptRepository {

    override fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getVisibleReceipts(): Flow<List<Receipt>> {
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

    override fun observeReceiptById(id: Long): Flow<Receipt?> {
        return receiptDao.getReceiptByIdFlow(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getReceiptByInvoiceNumber(invoiceNumber: String): Receipt? {
        return receiptDao.getReceiptByInvoiceNumber(invoiceNumber)?.toDomain()
    }
    
    override suspend fun getReceiptsByAmount(amount: java.math.BigDecimal): List<Receipt> {
        return receiptDao.getReceiptsByAmount(amount).map { it.toDomain() }
    }

    override suspend fun insertReceipt(receipt: Receipt, billingPeriod: String?): Long { // Added param
        // Ensure External ID exists (Critical for Sync)
        val finalReceipt = if (receipt.externalId.isNullOrBlank()) {
             val newId = generateDeterministicId(receipt)
             android.util.Log.d("ReceiptRepository", "⚠️ insertReceipt: Generated Deterministic ID: $newId")
             receipt.copy(externalId = newId)
        } else {
             receipt
        }
        
        val entity = finalReceipt.toEntity()
        
        // LOGOVANJE ZA DEBUG
        android.util.Log.d("ReceiptRepository", "=== UMETANJE RAČUNA ===")
        android.util.Log.d("ReceiptRepository", "Račun broj: ${entity.invoiceNumber}")
        android.util.Log.d("ReceiptRepository", "Payment ID: ${entity.paymentId}")
        android.util.Log.d("ReceiptRepository", "Billing Period: $billingPeriod") // Log
        android.util.Log.d("ReceiptRepository", "STORNO: ${entity.isStorno}")
        android.util.Log.d("ReceiptRepository", "Iznos: ${entity.totalAmount}")
        
        // Proveri duplikat (Pass billingPeriod)
        val duplicateCheck = duplicateDetector.checkForDuplicate(entity, billingPeriod)
        
        when (duplicateCheck) {
            is DuplicateCheckResult.StornoPaidBill -> {
                android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE (Storno): Invoice=${entity.invoiceNumber} -> ${duplicateCheck.message}")
                throw DuplicateBillException(duplicateCheck.message)
            }
            
            is DuplicateCheckResult.DuplicatePaidBill -> {
                android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE (Plaćen): Invoice=${entity.invoiceNumber} -> ${duplicateCheck.message}")
                throw DuplicateBillException(duplicateCheck.message)
            }
            
            is DuplicateCheckResult.DuplicateUnpaidBill -> {
                // SYSTEMIC FIX: Check if the new receipt is STORNO and we already have a regular bill.
                // In that case, we MUST NOT follow the "Debug Update Mode" skip/delete logic.
                if (entity.isStorno) {
                    android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE (Storno): Invoice=${entity.invoiceNumber} -> Original već postoji.")
                    throw DuplicateBillException(duplicateCheck.message)
                }

                android.util.Log.e("ReceiptRepository", "🛑 DUPLIKAT (Unpaid): Invoice=${entity.invoiceNumber} -> DEBUG UPDATE MODE")
                // DEBUGGING MODE: Instead of throwing, we DELETE the old one and INSERT the new one
                // This forces the "Debug Echo" address updates to be saved.
                android.util.Log.d("ReceiptRepository", "♻️ BRISANJE STAROG RAČUNA RADI AŽURIRANJA: ${duplicateCheck.existingReceipt.id}")
                receiptDao.deleteReceipt(duplicateCheck.existingReceipt)
                // Continue to insert...
            }
            
            is DuplicateCheckResult.ReplaceExisting -> {
                android.util.Log.d("ReceiptRepository", "♻️ ZAMENA: Invoice=${entity.invoiceNumber} -> ${duplicateCheck.message}")
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
        
        // TRIGGER CONFLICT RESOLUTION (Housekeeping)
        if (!preparedEntity.naplatniNumber.isNullOrEmpty() && !preparedEntity.paymentId.isNullOrEmpty()) {
            duplicateDetector.resolveConflicts(preparedEntity)
        }
        
        return insertedId
    }

    override suspend fun insertReceiptWithData(
        receipt: Receipt,
        epsData: EpsData?,
        items: List<com.platisa.app.core.domain.model.ReceiptItem>?,
        billingPeriod: String?
    ): Long {
        return database.withTransaction {
            val receiptId = insertReceipt(receipt, billingPeriod)
            
            epsData?.let {
                insertEpsData(it, receiptId)
            }
            
            items?.let {
                insertReceiptItems(it, receiptId)
            }
            
            receiptId
        }
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


    /**
     * Generates a deterministic ID based on receipt content.
     * Formula: SHA-256(merchant + date + amount)
     * This ensures that two phones parsing the same bill get the SAME ID.
     */
    private fun generateDeterministicId(receipt: Receipt): String {
        val id = com.platisa.app.core.domain.util.DeterministicIdGenerator.generate(receipt)
        android.util.Log.d("ReceiptRepository", "🔐 Generated Deterministic ID: $id")
        return id
    }

    override suspend fun updateReceipt(receipt: Receipt) {
        // Ensure External ID exists (Critical for Sync)
        val finalReceipt = if (receipt.externalId.isNullOrBlank()) {
             // CHANGE: Use Deterministic ID instead of Random UUID
             val newId = generateDeterministicId(receipt)
             android.util.Log.d("ReceiptRepository", "⚠️ updateReceipt: Generated Deterministic ID: $newId")
             receipt.copy(externalId = newId)
        } else {
             receipt
        }

        // 1. Update local DB
        receiptDao.updateReceipt(finalReceipt.toEntity())
        
        // 2. Sync Status to Firestore
        syncPaidStatusToCloud(finalReceipt)
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt.toEntity())
        
        // Check if we need to remove paid status (if user deletes a paid bill)
        if (receipt.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID) {
            syncPaidStatusToCloud(receipt, isPaid = false)
        }
    }
    
    // Helper to repair existing IDs
    suspend fun repairReceiptIds() {
        android.util.Log.d("ReceiptRepository", "🔧 STARTING ID REPAIR...")
        val allReceipts = getAllReceipts().firstOrNull() ?: emptyList()
        var repairedCount = 0
        
        val receiptsToFix = allReceipts.filter { 
            // Fix if:
            // 1. ID is missing
            // 2. ID looks like a random UUID
            // 3. ID is deterministic (det_) -> FORCE RE-VALIDATE due to updated normalization logic (Cyrillic fix)
            val id = it.externalId
            val isMissing = id.isNullOrBlank()
            val isRandomUuid = id != null && id.length == 36 && id.contains("-") && !id.startsWith("det_")
            val isDeterministic = id != null && id.startsWith("det_")
            
            isMissing || isRandomUuid || isDeterministic
        }
        
        android.util.Log.d("ReceiptRepository", "🔧 Found ${receiptsToFix.size} receipts to repair.")
        
        for (receipt in receiptsToFix) {
            try {
                val newId = generateDeterministicId(receipt)
                val isDifferent = newId != receipt.externalId
                
                if (isDifferent) {
                    // 🔍 COLLISION CHECK: Does another receipt already have this NEW ID?
                    val existingWithSameId = receiptDao.getReceiptByExternalId(newId)
                    
                    if (existingWithSameId != null && existingWithSameId.id != receipt.id) {
                        android.util.Log.w("ReceiptRepository", "⚠️ CLASH DETECTED: Upgrading ID for ${receipt.id} -> $newId, but ${existingWithSameId.id} already has it!")
                        
                        // DECISION: Delete the one we are currently "repairing" because it's a redundant duplicate.
                        // We keep 'existingWithSameId' because it already has the correct deterministic ID.
                        android.util.Log.d("ReceiptRepository", "🗑️ Deleting redundant duplicate ID: ${receipt.id}")
                        receiptDao.deleteReceipt(receipt.toEntity())
                        repairedCount++
                        continue
                    }

                    // Otherwise, proceed with update
                    val updated = receipt.copy(
                        externalId = newId,
                        metadata = (receipt.metadata ?: "") + " [ID_REPAIRED]"
                    )
                    
                    receiptDao.updateReceipt(updated.toEntity())
                    
                    // If it was paid, re-sync under NEW ID
                    if (updated.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                        syncPaidStatusToCloud(updated, isPaid = true)
                    }
                    
                    repairedCount++
                }
            } catch (e: Exception) {
                android.util.Log.e("ReceiptRepository", "❌ Failed to repair receipt ${receipt.id}", e)
            }
        }
        
        android.util.Log.d("ReceiptRepository", "✅ REPAIR COMPLETE. Fixed $repairedCount receipts.")
    }

    /**
     * Helper to sync paid status to Firestore using the email from `originalSource`.
     */
    private suspend fun syncPaidStatusToCloud(receipt: Receipt, isPaid: Boolean? = null) {
        try {
            val statusToSync = if (isPaid != null) {
                if (isPaid) com.platisa.app.core.domain.model.PaymentStatus.PAID else com.platisa.app.core.domain.model.PaymentStatus.UNPAID
            } else {
                receipt.paymentStatus
            }
            val externalId = receipt.externalId
            
            // 🔍 DEBUG: Log all relevant fields
            android.util.Log.d("ReceiptRepository", "🔍 ============= SYNC PAID STATUS DEBUG =============")
            android.util.Log.d("ReceiptRepository", "🔍 Receipt ID: ${receipt.id}")
            android.util.Log.d("ReceiptRepository", "🔍 External ID: $externalId")
            android.util.Log.d("ReceiptRepository", "🔍 Original Source: ${receipt.originalSource}")
            android.util.Log.d("ReceiptRepository", "🔍 Merchant: ${receipt.merchantName}")
            android.util.Log.d("ReceiptRepository", "🔍 Status: $statusToSync")
            android.util.Log.d("ReceiptRepository", "🔍 Metadata: ${receipt.metadata}")
            
            val email = resolveSyncEmail(receipt)
            android.util.Log.d("ReceiptRepository", "🔍 Resolved email: $email")

            if (!email.isNullOrBlank() && !externalId.isNullOrBlank()) {
                // FORCE LOWERCASE: Firestore IDs are case sensitive, we use lowercase standardization
                val normalizedEmail = email.lowercase()
                android.util.Log.d("ReceiptRepository", "☁️ SAVING to Firestore: shared_receipts/$normalizedEmail/receipts/$externalId")
                android.util.Log.d("ReceiptRepository", "☁️ Syncing SHARED Status ($statusToSync) for $normalizedEmail / $externalId")
                firestoreRepository.saveReceiptStatus(normalizedEmail, externalId, statusToSync)
            } else {
                android.util.Log.w("ReceiptRepository", "⚠️ SKIPPED SYNC: email='$email' (blank=${email.isNullOrBlank()}), externalId='$externalId' (null/blank=${externalId.isNullOrBlank()})")
            }
            android.util.Log.d("ReceiptRepository", "🔍 =====================================================")
        } catch (e: Exception) {
            android.util.Log.e("ReceiptRepository", "⚠️ Failed to sync paid status to cloud: ${e.message}")
        }
    }

    private fun resolveSyncEmail(receipt: Receipt): String? {
        var email = receipt.originalSource
        android.util.Log.d("ReceiptRepository", "🔍 Step 1 - Raw email from originalSource: $email")
        
        // Check if source is a valid email (simple check)
        if (email == "Manual" || email == "Camera" || email == "GMAIL" || email == "CAMERA" || !email.contains("@")) {
            val firebaseEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            android.util.Log.d("ReceiptRepository", "🔍 Step 2 - Not an email, falling back to Firebase: $firebaseEmail")
            email = firebaseEmail ?: ""
        }
        return email
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
            if (receipt.paymentStatus == PaymentStatus.PAID) score += 100
            if (!receipt.qrCodeData.isNullOrEmpty()) score += 20
            // Prioritize valid, long EPS invoice numbers
            if (!receipt.invoiceNumber.isNullOrEmpty()) {
                score += 10
                if (receipt.invoiceNumber!!.length >= 8) score += 10
            }
            if (!receipt.paymentId.isNullOrEmpty()) score += 5
            
            // Correction bills are golden
            if (receipt.metadata?.contains("IS_CORRECTION:true") == true) score += 50
            
            score += (receipt.id / 1000).toInt().coerceAtMost(10)  // Slight preference for newer scans
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

        // 3. Proveri Račun broj + Iznos duplikate (fallback za nedostajući PaymentId)
        allReceipts.filter { 
            !it.invoiceNumber.isNullOrEmpty() && 
            it.paymentId.isNullOrEmpty() && 
            !toDeleteIds.contains(it.id) 
        }
        .groupBy { "${it.invoiceNumber}-${it.totalAmount}" }
        .forEach { (key, duplicates) ->
            if (duplicates.size > 1) {
                android.util.Log.d("ReceiptRepository", "Pronađeno ${duplicates.size} duplikata sa Račun+Iznos: $key")
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

    override suspend fun markPastBillsAsPaid(merchantName: String, excludeReceiptId: Long, currentBillDate: Long) {
        // 1. Fetch ALL non-paid bills for fuzzy matching
        val allPotential = receiptDao.getAllReceiptsList().filter { 
            (it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.UNPAID || 
             it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.PROCESSING) && 
            it.id != excludeReceiptId &&
            it.date.time < currentBillDate
        }
        
        val matchedBills = findMatchingBills(merchantName, allPotential)
        
        android.util.Log.d("ReceiptRepository", "🔄 CASCADE: Found ${matchedBills.size} matching past bills for $merchantName")
        
        if (matchedBills.isNotEmpty()) {
            matchedBills.forEach { entity ->
               // Use the repository's updateReceipt to ensure Firestore sync!
               val updatedDomain = entity.toDomain().copy(
                   paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                   updatedAt = java.util.Date(),
                   metadata = (entity.metadata ?: "") + " [Cascaded]"
               )
               updateReceipt(updatedDomain)
            }
        }
    }

    override suspend fun isLatestReceipt(merchantName: String, receiptDate: java.util.Date): Boolean {
       val latestDate = receiptDao.getLatestReceiptDateForMerchant(merchantName) ?: return true
       return receiptDate.time >= latestDate
    }

    override suspend fun getPaidPastBillsSum(merchantName: String, beforeDate: Long): Double {
        val paidPastBills = findMatchingBills(merchantName, receiptDao.getAllReceiptsList().filter { 
            it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.PAID && 
            it.date.time < beforeDate 
        })
        return paidPastBills.sumOf { it.totalAmount.toDouble() }
    }

    override suspend fun getUnpaidPastBillsSum(merchantName: String, beforeDate: Long): Double {
        val unpaidPastBills = findMatchingBills(merchantName, receiptDao.getAllReceiptsList().filter { 
            (it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.UNPAID || 
             it.paymentStatus == com.platisa.app.core.data.database.entity.PaymentStatus.PROCESSING) && 
            it.date.time < beforeDate 
        })
        return unpaidPastBills.sumOf { it.totalAmount.toDouble() }
    }

    override suspend fun hasAnyPastBills(merchantName: String, beforeDate: Long): Boolean {
        return findMatchingBills(merchantName, receiptDao.getAllReceiptsList().filter { 
            it.date.time < beforeDate 
        }).isNotEmpty()
    }

    private fun findMatchingBills(
        targetMerchant: String, 
        candidates: List<com.platisa.app.core.data.database.entity.ReceiptEntity>
    ): List<com.platisa.app.core.data.database.entity.ReceiptEntity> {
        val normalizedTarget = com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(targetMerchant)
        
        val aliases = mapOf(
            "mts" to listOf("telekom", "mts"),
            "telekom" to listOf("mts", "telekom"),
            "yettel" to listOf("telenor", "yettel"),
            "telenor" to listOf("yettel", "telenor"),
            "eps" to listOf("eps", "elektroprivreda", "snabdevanje", "distribucija"),
            "infostan" to listOf("infostan")
        )
        
        return candidates.filter { candidate ->
            val normalizedCandidate = com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(candidate.merchantName)
            if (normalizedCandidate.isBlank()) return@filter false

            // 1. Alias Match (Explicitly grouped providers have priority)
            val targetAliasKey = aliases.keys.find { normalizedTarget.contains(it) }
            if (targetAliasKey != null) {
                val group = aliases[targetAliasKey] ?: emptyList()
                if (group.any { normalizedCandidate.contains(it) }) return@filter true
            }

            // 2. Direct Normalized Match
            if (normalizedCandidate == normalizedTarget) return@filter true
            
            // 3. Fuzzy match ONLY for longer names (to avoid EPS matching PEPSI)
            if (normalizedTarget.length > 3 && normalizedCandidate.contains(normalizedTarget)) return@filter true
            if (normalizedCandidate.length > 3 && normalizedTarget.contains(normalizedCandidate)) return@filter true
            
            false
        }
    }

    override suspend fun deleteAllPaidStatuses(sourceEmail: String) {
        firestoreRepository.deleteAllPaidStatuses(sourceEmail)
    }

    override suspend fun tryAcquireProcessingLock(receipt: Receipt): Boolean {
        val email = resolveSyncEmail(receipt)
        val externalId = receipt.externalId
        if (email.isNullOrBlank() || externalId.isNullOrBlank()) return false
        return firestoreRepository.tryAcquireProcessingLock(email, externalId)
    }

    override suspend fun releaseProcessingLock(receipt: Receipt): Boolean {
        val email = resolveSyncEmail(receipt)
        val externalId = receipt.externalId
        if (email.isNullOrBlank() || externalId.isNullOrBlank()) return false
        return firestoreRepository.releaseProcessingLock(email, externalId)
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
                    android.util.Log.w("ReceiptRepository", "🛑 PRESAKOČENO (Storno): Invoice=${entity.invoiceNumber}")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.DuplicatePaidBill -> {
                    android.util.Log.w("ReceiptRepository", "🛑 PRESAKOČENO (Plaćen): Invoice=${entity.invoiceNumber}")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.DuplicateUnpaidBill -> {
                    android.util.Log.w("ReceiptRepository", "🛑 PRESAKOČENO (Unpaid): Invoice=${entity.invoiceNumber}")
                    skippedCount++
                    false
                }
                is DuplicateCheckResult.ReplaceExisting -> {
                    android.util.Log.d("ReceiptRepository", "♻️ ZAMENA (Bulk): Invoice=${entity.invoiceNumber} -> ${duplicateCheck.message}")
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

    override suspend fun startRealTimeSync() {
        android.util.Log.d("ReceiptRepository", "⚡ Starting REAL-TIME Sync...")

        var allAccounts: List<String>
        while (true) {
            val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()
            val connected = secureStorage.getConnectedAccounts().map { it.lowercase() }
            allAccounts = (connected + listOfNotNull(currentUserEmail)).distinct()

            if (allAccounts.isNotEmpty()) break

            android.util.Log.d("ReceiptRepository", "⚡ No accounts yet. Waiting to start real-time sync...")
            delay(2000)
        }

        android.util.Log.d("ReceiptRepository", "⚡ Listening for updates on: $allAccounts")

        // AUTO-REPAIR on Sync Start: Fixes existing bad IDs
        try {
            repairReceiptIds()
        } catch (e: Exception) {
            android.util.Log.e("ReceiptRepository", "⚠️ Auto-repair failed", e)
        }

        kotlinx.coroutines.coroutineScope {
            // SHARED STATE: Map of Email -> Map<ExternalId, PaymentStatus>
            val statusStateMap = java.util.concurrent.ConcurrentHashMap<String, Map<String, com.platisa.app.core.domain.model.PaymentStatus>>()
            // Initial empty state for all accounts
            allAccounts.forEach { statusStateMap[it] = emptyMap() }
            
            val updateMutex = kotlinx.coroutines.sync.Mutex()

            allAccounts.forEach { email ->
                launch {
                    try {
                        firestoreRepository.observeReceiptStatuses(email).collect { statusMap ->
                            updateMutex.lock()
                            try {
                                android.util.Log.d("ReceiptRepository", "⚡ Update received for $email: ${statusMap.size} receipts")
                                // 1. Update State
                                statusStateMap[email] = statusMap

                                // 2. Calculate UNION of ALL known statuses (Global Truth)
                                val unionPaidIds = statusStateMap.values
                                    .flatMap { it.filterValues { status -> status == com.platisa.app.core.domain.model.PaymentStatus.PAID }.keys }
                                    .toSet()
                                val unionProcessingIds = statusStateMap.values
                                    .flatMap { it.filterValues { status -> status == com.platisa.app.core.domain.model.PaymentStatus.PROCESSING }.keys }
                                    .toSet()
                                    .minus(unionPaidIds)
                                
                                // 3. Mark UNION as PAID (Global Update)
                                if (unionPaidIds.isNotEmpty()) {
                                    receiptDao.markAsPaid(unionPaidIds.toList())
                                }

                                // 3b. Mark UNION as PROCESSING (without downgrading PAID)
                                if (unionProcessingIds.isNotEmpty()) {
                                    receiptDao.markAsProcessing(unionProcessingIds.toList())
                                }

                                // 4. SAFE UNMARKING (The "Total Disaster" Fix)
                                // We iterate through EACH source we are tracking.
                                // For a source (e.g. Wife), we check bills in DB that are:
                                //    - Source = Wife
                                //    - Status = PAID/PROCESSING
                                //    - AND NOT PRESENT IN THE **UNION**
                                // This means: "Wife didn't pay it, Husband didn't pay it, NO ONE paid/processing it." -> Safe to Unmark.
                                
                                allAccounts.forEach { sourceEmail -> 
                                    // Get all bills in DB that claim to be PAID and from this Source
                                    val dbPaidIds = receiptDao.getPaidExternalIdsBySource(sourceEmail)
                                    val dbProcessingIds = receiptDao.getProcessingExternalIdsBySource(sourceEmail)
                                    
                                    // Find bills that are in DB but NOT in the Global Union
                                    val toUnmarkPaid = dbPaidIds.filter { 
                                        !unionPaidIds.contains(it) && !unionProcessingIds.contains(it) 
                                    }
                                    val toUnmarkProcessing = dbProcessingIds.filter { 
                                        !unionPaidIds.contains(it) && !unionProcessingIds.contains(it) 
                                    }
                                    val toUnmark = (toUnmarkPaid + toUnmarkProcessing).distinct()
                                    
                                    if (toUnmark.isNotEmpty()) {
                                        android.util.Log.d("ReceiptRepository", "⚡ SYNC: Safely unmarking ${toUnmark.size} bills from $sourceEmail (Not in Union)")
                                        receiptDao.markAsUnpaidByIds(toUnmark)
                                    }
                                }
                                
                            } finally {
                                updateMutex.unlock()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ReceiptRepository", "⚡ Error in listener for $email", e)
                    }
                }
            }
        }
    }
}

class DuplicateBillException(message: String) : Exception(message)

