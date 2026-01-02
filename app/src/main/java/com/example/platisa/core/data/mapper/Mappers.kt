package com.example.platisa.core.data.mapper

import com.example.platisa.core.data.database.entity.ReceiptEntity
import com.example.platisa.core.data.database.entity.SectionEntity
import com.example.platisa.core.data.database.entity.TagEntity
import com.example.platisa.core.domain.model.Receipt
import com.example.platisa.core.domain.model.Section
import com.example.platisa.core.domain.model.SyncStatus
import com.example.platisa.core.domain.model.Tag

fun ReceiptEntity.toDomain(): Receipt {
    return Receipt(
        id = id,
        merchantName = merchantName,
        date = date,
        dueDate = dueDate,
        totalAmount = totalAmount,
        currency = currency,
        imagePath = imagePath,
        qrCodeData = qrCodeData,
        invoiceNumber = invoiceNumber,
        naplatniNumber = naplatniNumber,
        paymentId = paymentId,
        isStorno = isStorno,
        isVisible = isVisible,
        sectionId = sectionId,
        metadata = metadata,
        syncStatus = SyncStatus.valueOf(syncStatus.name),
        paymentStatus = com.example.platisa.core.domain.model.PaymentStatus.valueOf(paymentStatus.name),
        paymentDate = updatedAt,
        originalSource = sourceType.name,
        externalId = externalId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        savedQrUri = savedQrUri,
        recipientName = recipientName,
        recipientAddress = recipientAddress
    )
}

fun Receipt.toEntity(): ReceiptEntity {
    return ReceiptEntity(
        id = id,
        merchantName = merchantName,
        date = date,
        dueDate = dueDate,
        totalAmount = totalAmount,
        currency = currency,
        imagePath = imagePath,
        qrCodeData = qrCodeData,
        invoiceNumber = invoiceNumber,
        naplatniNumber = naplatniNumber,
        paymentId = paymentId,
        isStorno = isStorno,
        isVisible = isVisible,
        sectionId = sectionId,
        metadata = metadata,
        syncStatus = com.example.platisa.core.data.database.entity.SyncStatus.valueOf(syncStatus.name),
        paymentStatus = com.example.platisa.core.data.database.entity.PaymentStatus.valueOf(paymentStatus.name),
        sourceType = try {
            val sanitizedSource = if (originalSource.contains("(")) {
                originalSource.substringBefore("(").trim()
            } else {
                originalSource
            }
            com.example.platisa.core.data.database.entity.SourceType.valueOf(sanitizedSource)
        } catch (e: Exception) {
            com.example.platisa.core.data.database.entity.SourceType.MANUAL
        },
        externalId = externalId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        savedQrUri = savedQrUri,
        recipientName = recipientName,
        recipientAddress = recipientAddress
    )
}

fun SectionEntity.toDomain(): Section {
    return Section(
        id = id,
        name = name,
        color = color,
        icon = icon
    )
}

fun Section.toEntity(): SectionEntity {
    return SectionEntity(
        id = id,
        name = name,
        color = color,
        icon = icon
    )
}

fun TagEntity.toDomain(): Tag {
    return Tag(
        id = id,
        name = name
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        name = name
    )
}

fun com.example.platisa.core.data.database.entity.EpsDataEntity.toDomain(): com.example.platisa.core.domain.model.EpsData {
    return com.example.platisa.core.domain.model.EpsData(
        edNumber = edNumber,
        billingPeriod = billingPeriod,
        consumptionVt = consumptionVt?.let { java.math.BigDecimal(it) },
        consumptionNt = consumptionNt?.let { java.math.BigDecimal(it) },
        totalConsumption = totalConsumption?.let { java.math.BigDecimal(it) },
        naplatniBroj = null,
        invoiceNumber = null,
        periodStart = null,
        periodEnd = null,
        isStorno = false,
        dueDate = null,
        paymentId = null
    )
}
