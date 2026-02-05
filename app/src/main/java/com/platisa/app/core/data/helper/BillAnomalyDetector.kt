package com.platisa.app.core.data.helper

import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.entity.ReceiptEntity
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Detekcija anomalija u iznosima računa (Smart Anomaly Detection).
 *
 * Služi za upozoravanje korisnika kada je račun sumnjivo mali u odnosu na istoriju.
 * Ovo pomaže u otkrivanju grešaka (npr. pogrešno prepoznat iznos ili pogrešan račun).
 */
class BillAnomalyDetector @Inject constructor(
    private val receiptDao: ReceiptDao
) {

    suspend fun checkAnomaly(newReceipt: ReceiptEntity): AnomalyResult {
        // 1. Fetch history for this merchant (last 3 bills sorted by date desc)
        // We exclude current bill ID to compare against history
        val history = receiptDao.getLastReceiptsForMerchant(
            merchantName = newReceipt.merchantName,
            limit = 3
        ).filter { it.id != newReceipt.id && !it.isStorno && it.isVisible }

        if (history.isEmpty()) return AnomalyResult.None

        val currentAmount = newReceipt.totalAmount

        // 2. CHECK: Average of last 3 months (Severe Drop)
        // Trigger: < 20% of Average (e.g. 1k vs 5k avg)
        val averageAmount = history.map { it.totalAmount }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
            .divide(BigDecimal(history.size), java.math.MathContext.DECIMAL32)
        
        val twentyPercentAvg = averageAmount.multiply(BigDecimal("0.20"))
        
        if (currentAmount < twentyPercentAvg) {
            return AnomalyResult.SuspiciouslyLow(
                average = averageAmount,
                percentOfAverage = (currentAmount.toDouble() / averageAmount.toDouble()) * 100
            )
        }

        // 3. CHECK: Drop vs Previous Month (Moderate Drop)
        // Trigger: < 50% of Previous Month
        val previousMonthBill = history.firstOrNull() // First is latest because of DESC sort
        if (previousMonthBill != null) {
            val fiftyPercentPrev = previousMonthBill.totalAmount.multiply(BigDecimal("0.50"))
            
            if (currentAmount < fiftyPercentPrev) {
                return AnomalyResult.SuddenDrop(
                    previousAmount = previousMonthBill.totalAmount,
                    dropPercent = 100 - ((currentAmount.toDouble() / previousMonthBill.totalAmount.toDouble()) * 100)
                )
            }
        }

        return AnomalyResult.None
    }
}

sealed class AnomalyResult {
    object None : AnomalyResult()

    data class SuspiciouslyLow(
        val average: BigDecimal,
        val percentOfAverage: Double
    ) : AnomalyResult()

    data class SuddenDrop(
        val previousAmount: BigDecimal,
        val dropPercent: Double
    ) : AnomalyResult()
}
