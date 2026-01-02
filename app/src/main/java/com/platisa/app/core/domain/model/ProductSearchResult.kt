package com.platisa.app.core.domain.model

import java.math.BigDecimal
import java.util.Date

data class ProductSearchResult(
    val id: Long,
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val total: BigDecimal,
    val merchantName: String,
    val date: Date,
    val currency: String = "RSD"
)

