package com.example.platisa.core.data.parser

import android.graphics.Bitmap
import com.example.platisa.BuildConfig
import com.example.platisa.core.domain.parser.ParsedReceipt
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeminiParser {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-3-flash-preview",
            // Retrieve API key from BuildConfig
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun parse(bitmap: Bitmap): ParsedReceipt? {
        // If no API key is set, we cannot proceed
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            android.util.Log.e("GeminiParser", "GEMINI_API_KEY is missing!")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze this bill/receipt image and extract the following data in strict JSON format.
                    Focus on extracting accurate Serbian Cyrillic or Latin text for names and addresses.
                    
                    Fields required:
                    - merchant_name (string): Name of the issuer (e.g., "EPS AD Beograd", "Telekom Srbija").
                    - date (string): Date of the bill in DD.MM.YYYY format.
                    - total_amount (number): Total amount to pay.
                    - invoice_number (string): The unique invoice number (Račun broj, Faktura broj).
                    - due_date (string): Payment deadline in DD.MM.YYYY format.
                    - recipient_name (string): Name of the payer/customer (often under "Kupac", "Primalac", "Korisnik"). Use the actual name, NOT labels like "Broj", "Šifra", "ED", "Naplatni".
                    - recipient_address (string): Address of the payer/customer (Street, number, city, zip).
                    
                    Return ONLY the JSON object. Do not include markdown formatting like ```json ... ```.
                """.trimIndent()

                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val responseText = response.text
                if (responseText != null) {
                    // Clean up potential markdown code blocks if any
                    val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                    parseJson(cleanJson)
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("GeminiParser", "Error parsing with Gemini", e)
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseJson(jsonString: String): ParsedReceipt {
        val json = JSONObject(jsonString)
        
        val merchantName = json.optString("merchant_name").takeIf { it.isNotEmpty() }
        val dateStr = json.optString("date")
        val amount = json.optDouble("total_amount", 0.0)
        val invoiceNumber = json.optString("invoice_number").takeIf { it.isNotEmpty() }
        val dueDateStr = json.optString("due_date")
        val recipientName = json.optString("recipient_name").takeIf { it.isNotEmpty() }
        val recipientAddress = json.optString("recipient_address").takeIf { it.isNotEmpty() }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        
        val date = try {
             if (dateStr.isNotEmpty()) dateFormat.parse(dateStr) else null
        } catch (e: Exception) { null }
        
        val dueDate = try {
             if (dueDateStr.isNotEmpty()) dateFormat.parse(dueDateStr) else null
        } catch (e: Exception) { null }

        return ParsedReceipt(
            merchantName = merchantName,
            date = date,
            totalAmount = if (amount > 0) BigDecimal.valueOf(amount) else null,
            invoiceNumber = invoiceNumber,
            dueDate = dueDate,
            recipientName = recipientName,
            recipientAddress = recipientAddress
        )
    }
}
