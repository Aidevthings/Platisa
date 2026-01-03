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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val sectionDao: SectionDao,
    private val tagDao: TagDao,
    private val epsDao: EpsDao,
    private val duplicateDetector: BillDuplicateDetector,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager
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
                android.util.Log.e("ReceiptRepository", "🛑 BLOKIRANJE: Duplikat neplaćenog računa!")
                android.util.Log.e("ReceiptRepository", "   Razlog: ${duplicateCheck.message}")
                android.util.Log.e("ReceiptRepository", "   Postojeći ID: ${duplicateCheck.existingReceipt.id}")
                throw DuplicateBillException(duplicateCheck.message)
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
        receiptDao.updateReceipt(receipt.toEntity())
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt.toEntity())
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

