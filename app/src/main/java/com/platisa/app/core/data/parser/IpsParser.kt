package com.platisa.app.core.data.parser

import java.math.BigDecimal

data class IpsData(
    val recipientName: String?,
    val recipientAccount: String?,
    val amount: BigDecimal?,
    val currency: String = "RSD", // Default for IPS
    val referenceNumber: String?,
    val purposeCode: String?,
    val purposeDescription: String?,
    val payerName: String? = null,
    val payerAddress: String? = null
)

object IpsParser {

    fun parse(qrContent: String): IpsData? {
        // Basic validation for NBS IPS QR code
        if (!qrContent.startsWith("K:PR")) return null

        val map = mutableMapOf<String, String>()
        val parts = qrContent.split("|")

        for (part in parts) {
            val keyValue = part.split(":", limit = 2)
            if (keyValue.size == 2) {
                map[keyValue[0]] = keyValue[1]
            }
        }

        // Extract fields based on NBS IPS standard
        // ... (standard fields K, V, C, R, N, I, SF, S, RO)
        // P: Payer Data (Name, Address, City) - often comma separated

        val amountString = map["I"] // e.g., "RSD1234,56"
        val amount = parseAmount(amountString)

        // Parse Payer Data (Tag P)
        // Format varies but often: "Name Lastname, Street 123, City"
        val rawPayerData = map["P"]
        var payerName: String? = null
        var payerAddress: String? = null

        if (!rawPayerData.isNullOrBlank()) {
            val payerParts = rawPayerData.split(",", limit = 2)
            payerName = com.platisa.app.core.domain.parser.ReceiptParser.normalizeText(payerParts.getOrNull(0)?.trim())
            
            if (payerParts.size > 1) {
                val candidateAddress = com.platisa.app.core.domain.parser.ReceiptParser.normalizeText(payerParts[1].trim())
                // Validate address to avoid "189 RSD" issues from bad IPS data
                if (candidateAddress != null && 
                    !candidateAddress.uppercase().contains("RSD") && 
                    !candidateAddress.uppercase().contains("DIN") &&
                    !candidateAddress.matches(Regex("^[\\d.,\\s]+$"))) {
                    payerAddress = candidateAddress
                }
            }
        }

        var refinedReferenceNumber = map["RO"]
        
        // JKP INFOSTAN FIX (Global for Camera & Gmail):
        // Override the Reference Number (RO) which is just a static Space Code.
        // Extract the unique Bill Number (e.g. 2026/01-0859349) from Purpose (S).
        // Only apply if Recipient (N) indicates Infostan.
        val recipientName = map["N"]
        val purposeDesc = map["S"]
        
        if (recipientName != null && (recipientName.contains("INFOSTAN", ignoreCase = true) || recipientName.contains("JKP", ignoreCase = true))) {
             if (purposeDesc != null) {
                 val billNumberRegex = Regex("""(\d{4}/\d{2}-\d+)""")
                 val match = billNumberRegex.find(purposeDesc)
                 if (match != null) {
                     refinedReferenceNumber = match.value
                     // Log isn't easily available here in pure object, but this value propagates to data model
                 }
             }
        }

        return IpsData(
            recipientName = map["N"],
            recipientAccount = map["R"],
            amount = amount,
            referenceNumber = refinedReferenceNumber,
            purposeCode = map["SF"],
            purposeDescription = map["S"],
            payerName = payerName,
            payerAddress = payerAddress
        )
    }

    private fun parseAmount(amountString: String?): BigDecimal? {
        if (amountString == null) return null
        // Remove currency "RSD" and replace comma with dot
        val cleanString = amountString.replace("RSD", "").replace(",", ".").trim()
        return try {
            BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            null
        }
    }
}

