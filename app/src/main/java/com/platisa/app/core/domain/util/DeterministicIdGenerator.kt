package com.platisa.app.core.domain.util

import com.platisa.app.core.domain.model.Receipt
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

object DeterministicIdGenerator {

    fun generate(receipt: Receipt): String {
        return generate(
            merchantName = receipt.merchantName,
            date = receipt.date,
            totalAmount = receipt.totalAmount,
            invoiceNumber = receipt.invoiceNumber
        )
    }

    fun generate(
        merchantName: String,
        date: java.util.Date?,
        totalAmount: BigDecimal?,
        invoiceNumber: String?
    ): String {
        try {
            // 1. Normalize Merchant: Use unified logic
            val merchantNorm = com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(merchantName)

            // 2. Date: Standardize to YYYYMMDD based on Local interpretation
            val cal = java.util.Calendar.getInstance()
            cal.time = date ?: java.util.Date()
            val dateStr = String.format(Locale.US, "%04d%02d%02d", 
                cal.get(java.util.Calendar.YEAR), 
                cal.get(java.util.Calendar.MONTH) + 1, 
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            
            // 3. Amount: Standard 2-decimal string
            val amountStr = try {
                val bd = totalAmount ?: BigDecimal.ZERO
                bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } catch (e: Exception) { "0.00" }
            
            // 4. Invoice: Robust normalization (handles letters/symbols consistently)
            val invoiceNorm = com.platisa.app.core.utils.SerbianGrammarUtils.normalizeForSync(invoiceNumber)
            
            // 5. Hero ID / Payment ID Logic
            // For EPS, the PaymentId (Naplatni + Period) is the legal unique identifier.
            // If it exists, we use it to prevent collisions between different months on the same account.
            val epsData = if (merchantNorm == "eps") {
                // We can't easily pass EpsData here without breaking API, so we look for it in metadata
                // or if the invoice number IS the payment ID for some reason.
                // However, the best way is to check if the caller provided a PaymentId-like invoice number.
                null
            } else null

            // LOGIC v6: EPS STABILIZATION
            val rawString = when {
                // If it's EPS and we have an invoice number, use it (standard)
                merchantNorm == "eps" && invoiceNorm.length >= 8 -> {
                    "${merchantNorm}_inv_${invoiceNorm}"
                }
                
                // If it's EPS and we have a custom Payment ID (likely passed via metadata or specialized invoice field)
                // we should really use it. 
                // For now, if invoice is missing for EPS, we MUST include the date to separate monthly bills.
                merchantNorm == "eps" -> {
                    "${merchantNorm}_${dateStr}_${amountStr}"
                }

                // Standard Hero ID: Merchant + Invoice
                invoiceNorm.length >= 5 -> {
                    "${merchantNorm}_inv_${invoiceNorm}"
                }
                
                // Fallback: Merchant + Date + Amount
                else -> {
                    "${merchantNorm}_${dateStr}_${amountStr}"
                }
            }
            
            android.util.Log.d("DeterministicIdGen", "🔐 HASHING SOURCE: '$rawString' (Merchant=$merchantNorm, InvLen=${invoiceNorm.length})")
            
            // Create SHA-256 Hash
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawString.toByteArray(StandardCharsets.UTF_8))
            
            // Convert to Hex String (take first 16 chars)
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            val finalId = "det_$hexString"
            
            return finalId
            
        } catch (e: Exception) {
            e.printStackTrace()
            return java.util.UUID.randomUUID().toString() 
        }
    }
}
