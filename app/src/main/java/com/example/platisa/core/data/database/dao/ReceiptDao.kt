package com.example.platisa.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.platisa.core.data.database.entity.ReceiptEntity
import androidx.room.Embedded
import kotlinx.coroutines.flow.Flow

data class ItemWithContext(
    @Embedded val item: com.example.platisa.core.data.database.entity.ReceiptItemEntity,
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
    suspend fun insertReceiptItems(items: List<com.example.platisa.core.data.database.entity.ReceiptItemEntity>)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun getReceiptItems(receiptId: Long): List<com.example.platisa.core.data.database.entity.ReceiptItemEntity>
    
    @Query("SELECT i.*, r.merchantName, r.date FROM receipt_items i INNER JOIN receipts r ON i.receiptId = r.id WHERE i.name LIKE '%' || :query || '%' ORDER BY i.unitPrice ASC")
    fun searchItems(query: String): Flow<List<ItemWithContext>>
    
    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()
    
    @Query("DELETE FROM receipt_items")
    suspend fun deleteAllReceiptItems()
}
