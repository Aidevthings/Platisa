package com.platisa.app.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["externalId"], unique = true),
        Index(value = ["invoiceNumber"], unique = false),
        Index(value = ["paymentId"], unique = false),
        Index(value = ["merchantName"], unique = false), // Optimize Search
        Index(value = ["date"], unique = false)          // Optimize Sorting/Bucketing
    ]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val date: Date,
    val dueDate: Date? = null,
    val totalAmount: BigDecimal,
    val currency: String = "RSD",
    val imagePath: String,
    val qrCodeData: String? = null,
    val invoiceNumber: String? = null,
    
    // Payment ID fields for duplicate detection
    val naplatniNumber: String? = null,
    val paymentId: String? = null,
    val isStorno: Boolean = false,
    val isVisible: Boolean = true,
    
    val sectionId: Long? = null,
    val metadata: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val sourceType: SourceType = SourceType.CAMERA,
    val externalId: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val savedQrUri: String? = null,
    val recipientName: String? = null,
    val recipientAddress: String? = null
)

enum class SyncStatus {
    PENDING, SYNCED, FAILED
}

enum class PaymentStatus {
    UNPAID, PROCESSING, PAID
}

enum class SourceType {
    CAMERA, GMAIL, MANUAL, CAMERA_FISCAL, CAMERA_IPS
}

