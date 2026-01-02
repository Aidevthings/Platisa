package com.example.platisa.core.data.parser

import java.math.BigDecimal

data class IpsData(
    val recipientName: String?,
    val recipientAccount: String?,
    val amount: BigDecimal?,
    val currency: String = "RSD", // Default for IPS
    val referenceNumber: String?,
    val purposeCode: String?,
    val purposeDescription: String?,
    val payerName: String? = null
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
        // K: Key (PR)
        // V: Version
        // C: Character Set
        // R: Recipient Account
        // N: Recipient Name
        // I: Amount (format: RSD1234,56)
        // P: Payer Name
        // SF: Purpose Code
        // S: Purpose Description
        // RO: Reference Number

        val amountString = map["I"] // e.g., "RSD1234,56"
        val amount = parseAmount(amountString)

        return IpsData(
            recipientName = map["N"],
            recipientAccount = map["R"],
            amount = amount,
            referenceNumber = map["RO"],
            purposeCode = map["SF"],
            purposeDescription = map["S"],
            payerName = com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(map["P"])
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
