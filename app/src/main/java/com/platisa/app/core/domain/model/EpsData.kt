package com.platisa.app.core.domain.model

import java.math.BigDecimal
import java.util.Date

data class EpsData(
    val edNumber: String?,
    val billingPeriod: String?,
    val consumptionVt: BigDecimal?,
    val consumptionNt: BigDecimal?,
    val totalConsumption: BigDecimal?,
    // Payment ID fields
    val naplatniBroj: String?,           // Naplatni broj (Account number)
    val invoiceNumber: String?,          // Racun broj (Invoice number)
    val periodStart: Date?,              // Period start date
    val periodEnd: Date?,                // Period end date
    val isStorno: Boolean = false,       // Da li je STORNO racun
    val dueDate: Date?,                  // Rok plaćanja
    val paymentId: String?,              // Jedinstveni Payment ID
    val recipientName: String? = null,
    val recipientAddress: String? = null
) {
    companion object {
        /**
         * Kreira Payment ID na osnovu naplatnog broja i perioda obracuna.
         * Format: "naplatniBroj-YYYYMMDD-YYYYMMDD"
         * Primer: "2004158536-20251005-20251101"
         */
        fun createPaymentId(naplatniBroj: String?, periodStart: Date?, periodEnd: Date?): String? {
            if (naplatniBroj == null || periodStart == null || periodEnd == null) {
                return null
            }
            
            val startDateStr = formatDateToYYYYMMDD(periodStart)
            val endDateStr = formatDateToYYYYMMDD(periodEnd)
            
            return "$naplatniBroj-$startDateStr-$endDateStr"
        }
        
        private fun formatDateToYYYYMMDD(date: Date): String {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
            val day = String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))
            return "$year$month$day"
        }
    }
}

