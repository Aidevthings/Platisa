package com.platisa.app.core.domain.repository

import com.platisa.app.core.domain.model.EpsData
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.model.EpsMonthData
import com.platisa.app.core.domain.model.ProductSearchResult
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getAllReceipts(): Flow<List<Receipt>>
    fun searchReceipts(query: String): Flow<List<Receipt>>
    fun getReceiptsByFilter(startDate: Long, endDate: Long, minAmount: Double, maxAmount: Double): Flow<List<Receipt>>
    suspend fun getReceiptsInRange(startDate: Long, endDate: Long): List<Receipt>
    suspend fun getReceiptById(id: Long): Receipt?
    suspend fun getReceiptByInvoiceNumber(invoiceNumber: String): Receipt?  // For deduplication
    suspend fun getReceiptsByAmount(amount: java.math.BigDecimal): List<Receipt> // For fuzzy deduplication
    suspend fun insertReceipt(receipt: Receipt, billingPeriod: String? = null): Long
    suspend fun insertReceipts(receipts: List<Receipt>): List<Long> // Batch insert
    suspend fun insertEpsData(epsData: EpsData, receiptId: Long)
    fun getEpsAnalyticsData(): Flow<List<EpsMonthData>>
    fun getAllEpsData(): Flow<List<Pair<Long, EpsData>>> // receiptId to EpsData mapping
    suspend fun updateReceipt(receipt: Receipt)
    suspend fun deleteReceipt(receipt: Receipt)
    suspend fun getReceiptByPath(imagePath: String): Receipt?
    suspend fun getReceiptByExternalId(externalId: String): Receipt?
    suspend fun getEpsDataForReceipt(receiptId: Long): EpsData?
    
    // Items
    suspend fun insertReceiptItems(items: List<com.platisa.app.core.domain.model.ReceiptItem>, receiptId: Long)
    suspend fun getReceiptItems(receiptId: Long): List<com.platisa.app.core.domain.model.ReceiptItem>
    fun searchItems(query: String): Flow<List<ProductSearchResult>>
    
    // Maintenance
    suspend fun deleteDuplicateReceipts(): Int
    
    // Bulk delete (for reset/wipe)
    suspend fun deleteAllReceipts()
    suspend fun deleteAllReceiptItems()
    suspend fun deleteAllEpsData()
}

