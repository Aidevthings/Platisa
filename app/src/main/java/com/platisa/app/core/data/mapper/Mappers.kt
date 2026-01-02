package com.platisa.app.core.data.mapper

import com.platisa.app.core.data.database.entity.ReceiptEntity
import com.platisa.app.core.data.database.entity.SectionEntity
import com.platisa.app.core.data.database.entity.TagEntity
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.model.Section
import com.platisa.app.core.domain.model.SyncStatus
import com.platisa.app.core.domain.model.Tag

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
        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.valueOf(paymentStatus.name),
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
        syncStatus = com.platisa.app.core.data.database.entity.SyncStatus.valueOf(syncStatus.name),
        paymentStatus = com.platisa.app.core.data.database.entity.PaymentStatus.valueOf(paymentStatus.name),
        sourceType = try {
            val sanitizedSource = if (originalSource.contains("(")) {
                originalSource.substringBefore("(").trim()
            } else {
                originalSource
            }
            com.platisa.app.core.data.database.entity.SourceType.valueOf(sanitizedSource)
        } catch (e: Exception) {
            com.platisa.app.core.data.database.entity.SourceType.MANUAL
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

fun com.platisa.app.core.data.database.entity.EpsDataEntity.toDomain(): com.platisa.app.core.domain.model.EpsData {
    return com.platisa.app.core.domain.model.EpsData(
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

