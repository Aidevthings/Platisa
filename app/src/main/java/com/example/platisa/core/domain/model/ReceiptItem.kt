package com.example.platisa.core.domain.model

import java.math.BigDecimal
import java.util.Date

data class ReceiptItem(
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val total: BigDecimal,
    val label: String? = null, // e.g., "Ђ" for tax rate
    // Context fields for comparison
    val merchantName: String? = null,
    val date: java.util.Date? = null
)
