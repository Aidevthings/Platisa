package com.platisa.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.platisa.app.core.data.database.entity.ReceiptEntity
import androidx.room.Embedded
import kotlinx.coroutines.flow.Flow

data class ItemWithContext(
    @Embedded val item: com.platisa.app.core.data.database.entity.ReceiptItemEntity,
    val merchantName: String,
    val date: java.util.Date
)

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY date DESC")
    suspend fun getAllReceiptsList(): List<ReceiptEntity>
    
    @Query("SELECT * FROM receipts WHERE isVisible = 1 ORDER BY date DESC")
    fun getVisibleReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Long): ReceiptEntity?
    
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getReceiptByIdFlow(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getReceiptByInvoiceNumber(invoiceNumber: String): ReceiptEntity?
    
    @Query("SELECT * FROM receipts WHERE paymentId = :paymentId LIMIT 1")
    suspend fun getReceiptByPaymentId(paymentId: String): ReceiptEntity?
    
    // Get ALL receipts with same Payment ID (for duplicate detection)
    @Query("SELECT * FROM receipts WHERE paymentId = :paymentId ORDER BY createdAt ASC")
    suspend fun getAllReceiptsWithPaymentId(paymentId: String): List<ReceiptEntity>
    
    // Get ALL receipts with same Invoice Number (for duplicate detection - Tier 2)
    @Query("SELECT * FROM receipts WHERE invoiceNumber = :invoiceNumber ORDER BY createdAt ASC")
    suspend fun getReceiptsByInvoiceNumber(invoiceNumber: String): List<ReceiptEntity>
    
    // Get ALL receipts with same Naplatni Number (for duplicate detection - Tier 2)
    @Query("SELECT * FROM receipts WHERE naplatniNumber = :naplatniNumber AND (paymentId LIKE '%' || :period || '%' OR metadata LIKE '%' || :period || '%')")
    suspend fun getReceiptsByNaplatniAndPeriod(naplatniNumber: String, period: String): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE naplatniNumber = :naplatniNumber ORDER BY createdAt ASC")
    suspend fun getReceiptsByNaplatniNumber(naplatniNumber: String): List<ReceiptEntity>
    
    // Get ALL receipts with same Amount (for duplicate detection - Tier 3)
    // Using simple equality for now, assuming BigDecimal -> REAL/TEXT conversion matches
    @Query("SELECT * FROM receipts WHERE totalAmount = :amount ORDER BY createdAt ASC")
    suspend fun getReceiptsByAmount(amount: java.math.BigDecimal): List<ReceiptEntity>
    
    @Query("SELECT * FROM receipts WHERE isStorno = 1 AND createdAt < :beforeDate")
    suspend fun getOldStornoReceipts(beforeDate: Long): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE merchantName LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchReceipts(query: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE date >= :start AND date <= :end ORDER BY date DESC")
    suspend fun getReceiptsInRange(start: Long, end: Long): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate AND totalAmount BETWEEN :minAmount AND :maxAmount ORDER BY date DESC")
    fun getReceiptsByFilter(startDate: Long, endDate: Long, minAmount: Double, maxAmount: Double): Flow<List<ReceiptEntity>>

    // Safe Duplicate Check: Billing Period String + Amount
    @Query("""
        SELECT r.* 
        FROM receipts r 
        INNER JOIN eps_data e ON r.id = e.receiptId 
        WHERE e.billingPeriod = :billingPeriod 
        AND ABS(r.totalAmount - :amount) < 0.1
    """)
    suspend fun findByBillingPeriodAndAmount(billingPeriod: String, amount: Double): List<ReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<ReceiptEntity>): List<Long>

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Update
    suspend fun updateReceipts(receipts: List<ReceiptEntity>)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)
    
    @Query("DELETE FROM receipts WHERE id IN (:ids)")
    suspend fun deleteReceiptsById(ids: List<Long>)
    
    @Query("SELECT * FROM receipts WHERE imagePath = :imagePath LIMIT 1")
    suspend fun getReceiptByPath(imagePath: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE externalId = :externalId LIMIT 1")
    suspend fun getReceiptByExternalId(externalId: String): ReceiptEntity?

    // Receipt Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<com.platisa.app.core.data.database.entity.ReceiptItemEntity>)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun getReceiptItems(receiptId: Long): List<com.platisa.app.core.data.database.entity.ReceiptItemEntity>
    
    @Query("SELECT i.*, r.merchantName, r.date FROM receipt_items i INNER JOIN receipts r ON i.receiptId = r.id WHERE i.name LIKE '%' || :query || '%' ORDER BY i.unitPrice ASC")
    fun searchItems(query: String): Flow<List<ItemWithContext>>
    
    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()
    
    @Query("DELETE FROM receipt_items")
    suspend fun deleteAllReceiptItems()

    @Query("DELETE FROM receipts WHERE sourceType = :sourceType")
    suspend fun deleteReceiptsBySource(sourceType: com.platisa.app.core.data.database.entity.SourceType)

    // Cascade Payment Support - Robust text matching
    @Query("SELECT * FROM receipts WHERE TRIM(merchantName) = TRIM(:merchantName) COLLATE NOCASE AND paymentStatus = :status AND id != :excludeId")
    suspend fun getUnpaidReceiptsForMerchant(merchantName: String, status: com.platisa.app.core.data.database.entity.PaymentStatus, excludeId: Long): List<ReceiptEntity>

    @Query("SELECT MAX(date) FROM receipts WHERE TRIM(merchantName) = TRIM(:merchantName) COLLATE NOCASE")
    suspend fun getLatestReceiptDateForMerchant(merchantName: String): Long?

    // Real-time Sync Helpers
    // GLOBAL UPDATE: If ID matches, it IS the same bill. Ignore sourceEmail (handles Manual/Shared/Forwarded bills)
    @Query("UPDATE receipts SET paymentStatus = 'PAID', updatedAt = :timestamp, metadata = metadata || ' [Sync:Paid]' WHERE externalId IN (:ids) AND paymentStatus != 'PAID'")
    suspend fun markAsPaid(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE receipts SET paymentStatus = 'PROCESSING', updatedAt = :timestamp, metadata = metadata || ' [Sync:Processing]' WHERE externalId IN (:ids) AND paymentStatus != 'PAID'")
    suspend fun markAsProcessing(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    // UNPAID SYNC: Strict scoping to avoid race conditions. 
    // Only unmark bills that clearly belong to this source (prevents wiping other people's payments)
    @Query("UPDATE receipts SET paymentStatus = 'UNPAID', updatedAt = :timestamp, metadata = metadata || ' [Sync:Unpaid]' WHERE externalId NOT IN (:ids) AND sourceEmail = :sourceEmail AND externalId IS NOT NULL AND paymentStatus = 'PAID'")
    suspend fun markAsUnpaid(ids: List<String>, sourceEmail: String, timestamp: Long = System.currentTimeMillis())

    // SAFE SYNC HELPERS (v2)
    @Query("SELECT externalId FROM receipts WHERE sourceEmail = :sourceEmail AND paymentStatus = 'PAID' AND externalId IS NOT NULL")
    suspend fun getPaidExternalIdsBySource(sourceEmail: String): List<String>

    @Query("SELECT externalId FROM receipts WHERE sourceEmail = :sourceEmail AND paymentStatus = 'PROCESSING' AND externalId IS NOT NULL")
    suspend fun getProcessingExternalIdsBySource(sourceEmail: String): List<String>

    @Query("UPDATE receipts SET paymentStatus = 'UNPAID', updatedAt = :timestamp, metadata = metadata || ' [Sync:Unpaid]' WHERE externalId IN (:ids)")
    suspend fun markAsUnpaidByIds(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    // Cascade Validation: Get SUM of unpaid bills OLDER than this one
    @Query("SELECT SUM(totalAmount) FROM receipts WHERE TRIM(merchantName) = TRIM(:merchantName) COLLATE NOCASE AND paymentStatus = 'UNPAID' AND date < :beforeDate")
    suspend fun getUnpaidPastBillsSum(merchantName: String, beforeDate: Long): Double?

    // Anomaly Detection History
    @Query("SELECT * FROM receipts WHERE TRIM(merchantName) = TRIM(:merchantName) COLLATE NOCASE ORDER BY date DESC LIMIT :limit")
    suspend fun getLastReceiptsForMerchant(merchantName: String, limit: Int): List<ReceiptEntity>
}

