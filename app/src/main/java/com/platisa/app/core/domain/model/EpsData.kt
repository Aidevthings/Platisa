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
    val totalPayAmount: BigDecimal? = null, // "ZA UPLATU" (Total to Pay)
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
         * Kreira Payment ID na osnovu broja računa, perioda i iznosa.
         * NOVI LOGIKA: ID = InvoiceNumber + Period + Amount
         * Naplatni broj se više NE KORISTI za generisanje ID-a jer je konstantan.
         */
        fun createPaymentId(invoiceNumber: String?, periodStart: Date?, periodEnd: Date?, amount: BigDecimal?): String? {
            // Ako fali bilo koji od ključnih podataka, ne možemo kreirati pouzdan ID
            if (invoiceNumber == null || periodStart == null || periodEnd == null || amount == null) {
                return null
            }
            
            val startDateStr = formatDateToYYYYMMDD(periodStart)
            val endDateStr = formatDateToYYYYMMDD(periodEnd)
            // Format amount to ensure consistency (e.g. "20571.95")
            val amountStr = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            
            return "$invoiceNumber-$startDateStr-$endDateStr-$amountStr"
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

