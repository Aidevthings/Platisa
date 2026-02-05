package com.platisa.app.core.domain.model

import java.math.BigDecimal
import java.util.Date

/**
 * Represents a single row from the EPS discount table.
 * Contains early payment discount information.
 */
data class DiscountRow(
    val percentage: String,  // e.g., "5%", "6%", "7%"
    val deadline: String,    // e.g., "за уплату до 28.11.2025.г."
    val amount: String       // e.g., "29,04" (savings in dinars)
)

data class EpsData(
    val edNumber: String?,
    val billingPeriod: String?,
    val consumptionVt: BigDecimal?,
    val consumptionNt: BigDecimal?,
    val totalConsumption: BigDecimal?,
    // Monetary fields (Smart Parsing)
    val currentMonthAmount: BigDecimal? = null,
    val previousDebtAmount: BigDecimal? = null,
    // Payment ID fields
    val naplatniBroj: String?,           // Naplatni broj (Account number)
    val invoiceNumber: String?,          // Racun broj (Invoice number)
    val periodStart: Date?,              // Period start date
    val periodEnd: Date?,                // Period end date
    val isStorno: Boolean = false,       // Da li je STORNO racun
    val isCorrection: Boolean = false,   // Da li je KORIGOVAN (ispravljen) racun
    val dueDate: Date?,                  // Rok plaćanja
    val paymentId: String?,              // Jedinstveni Payment ID
    val recipientName: String? = null,
    val recipientAddress: String? = null,
    // Early payment discount info (Base cost for calculation)
    val electricityBaseCost: BigDecimal? = null,
    val discountDeadline: String? = null,
    val discountThresholdAmount: BigDecimal? = null,
    val discountThresholdMessage: String? = null,
    // Note: discountTable is removed to support lazy calculation only for the latest bill
    // val discountTable: List<DiscountRow>? = null
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

