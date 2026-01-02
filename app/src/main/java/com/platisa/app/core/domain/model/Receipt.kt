package com.platisa.app.core.domain.model

import java.math.BigDecimal
import java.util.Date

data class Receipt(
    val id: Long = 0,
    val merchantName: String,
    val date: Date,
    val dueDate: Date? = null,
    val totalAmount: BigDecimal,
    val currency: String = "RSD",
    val imagePath: String,
    val qrCodeData: String? = null,
    val invoiceNumber: String? = null,
    
    // Payment ID fields
    val naplatniNumber: String? = null,
    val paymentId: String? = null,
    val isStorno: Boolean = false,
    val isVisible: Boolean = true,
    
    val sectionId: Long? = null,
    val metadata: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentDate: Date? = null,
    val originalSource: String = "CAMERA",
    val externalId: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val savedQrUri: String? = null,
    val recipientName: String? = null,
    val recipientAddress: String? = null
) {
    val category: BillCategory
        get() = BillCategorizer.categorize(merchantName)
}

enum class SyncStatus {
    PENDING, SYNCED, FAILED
}

enum class PaymentStatus {
    UNPAID, PROCESSING, PAID
}

