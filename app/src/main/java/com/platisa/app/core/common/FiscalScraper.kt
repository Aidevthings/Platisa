package com.platisa.app.core.common

import com.platisa.app.core.domain.parser.ParsedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import com.platisa.app.core.domain.model.ReceiptItem

object FiscalScraper {

    private const val FISCAL_DOMAIN = "suf.purs.gov.rs"

    /**
     * Checks if the given QR data is a valid Serbian fiscal verification URL.
     */
    fun isFiscalUrl(qrData: String): Boolean {
        return qrData.contains(FISCAL_DOMAIN, ignoreCase = true) && 
               (qrData.contains("/v/") || qrData.contains("/verify/"))
    }

    sealed class ScrapeResult {
        data class Success(val receipt: ParsedReceipt, val debugHtml: String) : ScrapeResult()
        data class Error(val message: String) : ScrapeResult()
    }

    /**
     * Fetches and parses the fiscal receipt data from the government portal.
     */
    suspend fun scrapeFiscalData(url: String): ScrapeResult = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            android.util.Log.d("FiscalScraper", "Scraping URL: $cleanUrl")
            val connection = Jsoup.connect(cleanUrl)
                .timeout(30000)  // 30 seconds for slow connections
                .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                .method(org.jsoup.Connection.Method.GET)
            
            val response = connection.execute()
            val doc = response.parse()
            val cookies = response.cookies()

            android.util.Log.d("FiscalScraper", "Page title: ${doc.title()}")

            // 1. Extract Merchant Name & Address
            // Priority: Try to extract from "Journal" text
            var merchant: String? = null
            var journalAddress: String? = null
            
            val journalInfo = extractMerchantFromJournal(doc)
            if (journalInfo != null) {
                merchant = journalInfo.first
                journalAddress = journalInfo.second
            }
            
            if (merchant == null) {
                // Fallback: Standard ID selectors
                merchant = doc.select("#shopFullNameLabel").text()
                if (merchant.contains("-")) {
                    merchant = merchant.substringAfter("-").trim()
                }
            }
            if (merchant == null) merchant = ""
            
            // Fix for generic names like "Prodavnica br. 0166" (Latin & Cyrillic)
            
            // Fix for generic names like "Prodavnica br. 0166" (Latin & Cyrillic)
            val isGenericStoreName = merchant.startsWith("Prodavnica", ignoreCase = true) || 
                                     merchant.startsWith("Продавница", ignoreCase = true) ||
                                     merchant.startsWith("Store", ignoreCase = true) ||
                                     merchant.matches(Regex(".*(br\\.|бр\\.)\\s*[0-9]+.*", RegexOption.IGNORE_CASE))

            if (merchant.isBlank() || isGenericStoreName) {
                // Try finding the Taxpayer Name (Obveznik) which is usually the company name (e.g. DELHAIZE SERBIA)
                val taxpayer = doc.select("#taxpayerNameLabel").text()
                
                if (!taxpayer.isNullOrBlank()) {
                    merchant = taxpayer
                } else {
                     // Fallback 1: Look for label "Obveznik:" and get following text
                     val obveznikLabel = doc.getElementsContainingOwnText("Obveznik").firstOrNull() 
                                      ?: doc.getElementsContainingOwnText("Taxpayer").firstOrNull()
                     val obveznikValue = obveznikLabel?.nextElementSibling()?.text() 
                                      ?: obveznikLabel?.parent()?.text()?.substringAfter("Obveznik")?.substringAfter("Taxpayer")?.trim()
                                      
                     if (!obveznikValue.isNullOrBlank()) {
                         merchant = obveznikValue.replace(":", "").trim()
                     } else {
                         // Fallback 2: Try other classes
                         val fallback = doc.select(".merchant-name, .naziv-pfr").text()
                         if (fallback.isNotBlank()) merchant = fallback
                     }
                }
            }
            
            // 2. Extract Address (New Requirement)
            // Found in: <span id="addressLabel"> ... </span>
            var address: String = journalAddress ?: "" // Prefer Journal Address
            
            if (address.isBlank()) {
                address = doc.select("#addressLabel").text()
                if (address.isBlank()) {
                    address = doc.select("#locationNameLabel").text() // Sometimes location name acts as address
                }
                if (address.isBlank()) {
                    val addressLabel = doc.getElementsContainingOwnText("Adresa").firstOrNull() 
                                    ?: doc.getElementsContainingOwnText("Address").firstOrNull()
                    address = addressLabel?.nextElementSibling()?.text() 
                           ?: addressLabel?.parent()?.text()?.substringAfter("Adresa")?.substringAfter("Address")?.trim() ?: ""
                }
            }
            
            // Combine Merchant + Address
            if (merchant != null && address.isNotBlank()) {
                merchant = "$merchant, $address" // Format: "LIDL SRBIJA KD, Kralja Petra 50"
            }
            
            // GLOBAL FORMATTING: Ensure Title Case for all receipts (Lidl, Maxi, generic, etc.)
            // This fixes "MERCATOR S" -> "Mercator S" even if Journal parsing wasn't used
            if (merchant != null) {
                merchant = merchant.toTitleCase()
            }

            // 3. Extract Total Amount
            // Found in: <span id="totalAmountLabel"> 9.831,30 </span>
            val amountStr = doc.select("#totalAmountLabel").text()
                ?: doc.select("#totalAmount, .total-amount").text()

            val amount = parseAmount(amountStr)

            // 3. Extract Date
            // Found in: <span id="sdcDateTimeLabel"> 8.3.2025. 14:30:00 </span>
            val dateStr = doc.select("#sdcDateTimeLabel").text()
                ?: doc.select("#sdcDateTime, .receipt-date").text()
            
            val date = parseDate(dateStr)

            // 4. Extract Invoice Number (PFR broj)
            // Found in: <span id="invoiceNumberLabel"> 6Q3UWWHW-6Q3UWWHW-3954 </span>
            val invoiceNumber = doc.select("#invoiceNumberLabel").text()
                ?: doc.select("#pfrId, .pfr-number").text()

            android.util.Log.d("FiscalScraper", "Extracted: Merchant=$merchant, Amount=$amount, Date=$date, Invoice=$invoiceNumber")

            if (merchant != null || amount != null) {
                val receipt = ParsedReceipt(
                    merchantName = merchant,
                    totalAmount = amount,
                    date = date,
                    qrCodeData = url,
                    invoiceNumber = invoiceNumber,
                    items = emptyList() // Default empty, will fill below
                )
                
                // 5. Fetch Items (Async JSON)
                val token = extractToken(doc)
                var items: List<ReceiptItem> = emptyList()
                var rawJsonDebug = "Not fetched"
                
                android.util.Log.d("FiscalScraper", "=== ITEMS FETCH DEBUG ===")
                android.util.Log.d("FiscalScraper", "Token found: ${token != null} (${token?.take(20) ?: "null"}...)")
                android.util.Log.d("FiscalScraper", "Invoice number: $invoiceNumber")
                android.util.Log.d("FiscalScraper", "Cookies: ${cookies.size} items")
                
                if (token != null && invoiceNumber != null) {
                    try {
                        // Pass cookies to maintain session
                        android.util.Log.d("FiscalScraper", "Calling fetchItems...")
                        val fetchResult = fetchItems(invoiceNumber, token, cookies)
                        items = fetchResult.first
                        rawJsonDebug = fetchResult.second
                        android.util.Log.d("FiscalScraper", "✅ Fetched ${items.size} items")
                    } catch (e: Exception) {
                        rawJsonDebug = "Error: ${e.message}"
                        android.util.Log.e("FiscalScraper", "Failed to fetch items: ${e.message}")
                    }
                } else {
                    android.util.Log.w("FiscalScraper", "⚠️ Cannot fetch items: token=${token != null}, invoiceNumber=${invoiceNumber != null}")
                }

                val finalReceipt = receipt.copy(items = items)
                
                val fullDebug = "URL: $url\nToken: $token\nItems Count: ${items.size}\nRAW JSON: $rawJsonDebug\n\nHTML Snippet:\n" + doc.toString().take(2000)
                return@withContext ScrapeResult.Success(finalReceipt, fullDebug)
            } else {
                return@withContext ScrapeResult.Error("Parsing failed. Title: ${doc.title()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("FiscalScraper", "Error scraping fiscal data", e)
            return@withContext ScrapeResult.Error("Network Error: ${e.message}")
        }
    }

    private fun parseAmount(amountStr: String?): BigDecimal? {
        if (amountStr == null) return null
        return try {
            // Remove everything except numbers, commas and dots
            val cleaned = amountStr.replace(Regex("[^0-9,.]"), "")
            
            // Handle Serbian format 1.234,56
            if (cleaned.contains(",") && cleaned.contains(".")) {
                BigDecimal(cleaned.replace(".", "").replace(",", "."))
            } else if (cleaned.contains(",")) {
                BigDecimal(cleaned.replace(",", "."))
            } else {
                BigDecimal(cleaned)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr == null) return null
        val formats = listOf(
            "dd.MM.yyyy. HH:mm:ss",
            "d.M.yyyy. HH:mm:ss", // For single digits: 8.3.2025.
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy.",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }
    private fun extractToken(doc: org.jsoup.nodes.Document): String? {
        val scripts = doc.select("script")
        // Pattern 1: viewModel.Token('...')
        val pattern1 = Regex("viewModel\\.Token\\(['\"]([a-f0-9-]+)['\"]\\)")
        // Pattern 2: viewModel.Model.Token = '...'
        val pattern2 = Regex("viewModel\\.Model\\.Token\\s*=\\s*['\"]([a-f0-9-]+)['\"]")
        // Pattern 3: var token = '...'
        val pattern3 = Regex("var\\s+token\\s*=\\s*['\"]([a-f0-9-]+)['\"]")
        // Pattern 4 (Fallback): Just look for any GUID-like string in single quotes
        val pattern4 = Regex("['\"]([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})['\"]")
        
        for (script in scripts) {
            val data = script.data()
            pattern1.find(data)?.let { return it.groupValues[1] }
            pattern2.find(data)?.let { return it.groupValues[1] }
            pattern3.find(data)?.let { return it.groupValues[1] }
            // Try generic fallback last
            pattern4.find(data)?.let { return it.groupValues[1] }
        }
        return null
    }

    private fun fetchItems(invoiceNumber: String, token: String, cookies: Map<String, String>): Pair<List<ReceiptItem>, String> {
        // Based on analysis, items are fetched via POST to /Specifications
        val specUrl = "https://$FISCAL_DOMAIN/Specifications"
        
        val response = Jsoup.connect(specUrl)
            .ignoreContentType(true) // Expect JSON
            .ignoreHttpErrors(true) // Capture 400/500 errors so we can see the message
            .cookies(cookies) // IMPORTANT: Include session cookies
            .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            .header("X-Requested-With", "XMLHttpRequest")
            .data("invoiceNumber", invoiceNumber)
            .data("token", token)
            .method(org.jsoup.Connection.Method.POST)
            .execute()

        val jsonBody = response.body()
        android.util.Log.d("FiscalScraper", "Items JSON (Length: ${jsonBody.length}): $jsonBody")
        
        return Pair(parseItemsJson(jsonBody), jsonBody)
    }

    private fun parseItemsJson(jsonStr: String): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        try {
            // The API returns: {"success":true,"items":[{"name":"...","quantity":1.0,...}]}
            val json = JSONObject(jsonStr)
            val specs = json.optJSONArray("items") ?: json.optJSONArray("Specifications") ?: return emptyList()

            for (i in 0 until specs.length()) {
                val itemJson = specs.getJSONObject(i)
                // Keys are lowercase in the JSON response
                val name = itemJson.optString("name").ifEmpty { itemJson.optString("Name", "Unknown") }
                
                val quantityVal = itemJson.optDouble("quantity").takeIf { !it.isNaN() } ?: itemJson.optDouble("Quantity", 0.0)
                val quantity = BigDecimal(quantityVal)
                
                val unitPriceVal = itemJson.optDouble("unitPrice").takeIf { !it.isNaN() } ?: itemJson.optDouble("UnitPrice", 0.0)
                val unitPrice = BigDecimal(unitPriceVal)
                
                val totalVal = itemJson.optDouble("total").takeIf { !it.isNaN() } ?: itemJson.optDouble("Total", 0.0)
                val total = BigDecimal(totalVal)
                
                val label = itemJson.optString("label").ifEmpty { itemJson.optString("Label", "") }
                
                items.add(ReceiptItem(name, quantity, unitPrice, total, label))
            }
        } catch (e: Exception) {
             android.util.Log.e("FiscalScraper", "JSON Parse Error", e)
        }
        return items
    }

    private fun extractMerchantFromJournal(doc: org.jsoup.nodes.Document): Pair<String, String>? {
        // Look for the "Journal" section which usually preserves newlines in the raw text
        // Identified by header "=== ФИСКАЛНИ РАЧУН ===" (Cyrillic) or Latin equivalent
        val headerCyrillic = "=== ФИСКАЛНИ РАЧУН ==="
        val headerLatin = "=== FISCALNI RAČUN ==="
        
        // Use wholeText() if available on the element to ensure newlines are preserved,
        // otherwise doc.text() flattens it.
        // We first find the element containing the header.
        
        val element = doc.getElementsContainingOwnText(headerCyrillic).firstOrNull() 
                   ?: doc.getElementsContainingOwnText(headerLatin).firstOrNull() ?: return null

        // Get text with structure. Jsoup's text() normalizes whitespace, so we try to get structure if possible.
        // If it's in a <pre> tag, text() usually works. If it's <div> with <br>, text() flattens.
        // However, we can try to split by known headers or just looking at the full text dump.
        
        // Safer approach: Get the whole document text or the element's structured text
        // For Poreska Uprava, it's often a collection of spans or a pre block.
        // Let's assume the previous splitting by "  " or "\n" was working for locating lines generally.
        // We will try to reconstruct the block.
        
        val fullText = doc.text() // Fallback plain text
        val rawBlock = element.wholeText() // Preserves whitespace
        
        // Decide which source to use. If rawBlock is short (just the header), use fullText.
        val sourceText = if (rawBlock.length > 50) rawBlock else fullText
        
        val lines = sourceText.split("\n", "\r\n").map { it.trim() }.filter { it.isNotBlank() }
        
        // Find the header index
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].contains(headerCyrillic) || lines[i].contains(headerLatin)) {
                headerIndex = i
                break
            }
        }
        
        if (headerIndex != -1) {
            // User Confirmation (2025-02-04):
            // Row 1 (Index 0): Header "=== ... ==="
            // Row 2 (Index 1): PIB (Tax ID) -> Skip
            // Row 3 (Index 2): Name "dm drogerie markt..." -> EXTRACT (Merchant Name)
            // Row 4 (Index 3): Code "1325274-J06K" -> Skip (Store ID)
            // Row 5 (Index 4): Street "KORZO 10C" -> EXTRACT (Street Address)
            // Row 6 (Index 5): Town "SUBOTICA" -> EXTRACT (Town)
            
            if (headerIndex + 5 < lines.size) {
                // Name is Row 3 (Index 2)
                val nameLine = lines[headerIndex + 2]
                
                // Street is Row 5 (Index 4)
                val streetLine = lines[headerIndex + 4]
                
                // Town is Row 6 (Index 5)
                val townLine = lines[headerIndex + 5]
                
                // Combine for full address
                val fullAddress = "$streetLine, $townLine"
                
                return Pair(nameLine.toTitleCase(), fullAddress.toTitleCase())
            }
        }
        
        return null
    }

    private fun String.toTitleCase(): String {
        return this.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

