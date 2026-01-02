package com.example.platisa.core.domain.model

import java.math.BigDecimal

data class EpsMonthData(
    val month: String,
    val vtConsumption: BigDecimal,
    val ntConsumption: BigDecimal,
    val totalAmount: BigDecimal
)
