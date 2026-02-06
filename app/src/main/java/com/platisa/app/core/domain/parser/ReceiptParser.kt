package com.platisa.app.core.domain.parser

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import com.platisa.app.core.utils.SerbianGrammarUtils

data class ParsedReceipt(
    val merchantName: String? = null,
    val date: Date? = null,
    val totalAmount: BigDecimal? = null,
    val qrCodeData: String? = null,
    val invoiceNumber: String? = null,  // Račun broj - unique invoice number
    val items: List<com.platisa.app.core.domain.model.ReceiptItem> = emptyList(),
    val dueDate: Date? = null,
    val recipientName: String? = null,
    val recipientAddress: String? = null,
    val payerName: String? = null,
    val payerAddress: String? = null,
    val discountDeadline: String? = null,
    val currentMonthAmount: java.math.BigDecimal? = null,
    val previousDebtAmount: java.math.BigDecimal? = null
)

object ReceiptParser {

    // Pre-compiled regex patterns for performance optimization
    private val STANDARD_FISCAL_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d{2}))\\s+(?:[A-ZĐЖČĆŠ]\\s+)?(\\d+(?:[.,]\\d{3})?)\\s+(?:[A-ZĐЖČĆŠ]\\s+)?(\\d+(?:[.,]\\d{2}))[\\sA-ZĐЖČĆŠ]*$")
    private val QXP_FISCAL_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d{3})?)\\s*[x*]\\s*(\\d+(?:[.,]\\d{2})?)\\s+(\\d+(?:[.,]\\d{2}))[\\sA-ZĐЖČĆŠ]*$")

    fun parse(text: String): ParsedReceipt {
        val merchant = extractMerchant(text)
        val date = extractDate(text)
        val amount = extractTotalAmount(text)
        val invoiceNumber = extractInvoiceNumber(text)
        val dueDate = extractDueDate(text)
        val (name, addr) = extractRecipientInfo(text, merchant)
        val items = extractItems(text)
        val discountDeadline = extractDiscountDeadline(text)
        val currentMonthAmount = extractCurrentMonthAmount(text)
        val previousDebtAmount = extractPreviousDebtAmount(text)
        
        return ParsedReceipt(
            merchantName = merchant, 
            date = date, 
            totalAmount = amount, 
            qrCodeData = null, 
            invoiceNumber = invoiceNumber, 
            items = items, 
            dueDate = dueDate,
            recipientName = name,
            recipientAddress = addr,
            payerName = name,
            payerAddress = addr,
            discountDeadline = discountDeadline,
            currentMonthAmount = currentMonthAmount,
            previousDebtAmount = previousDebtAmount
        )
    }

    private fun extractItems(text: String): List<com.platisa.app.core.domain.model.ReceiptItem> {
        val items = mutableListOf<com.platisa.app.core.domain.model.ReceiptItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        // Regex patterns for fiscal item lines are now pre-compiled constants
        // Pattern 1: Price [Label?] Qty [Label?] Total [Label?] (Standard e-Fiscal)
        // e.g. "120,00 A 2 240,00" or "120,00 2 240,00 A"
        // Captures: 1=Price, 2=Qty, 3=Total (See STANDARD_FISCAL_PATTERN)
        
        // Pattern 2: Qty x Price Total 
        // e.g. "2 x 120,00 240,00" (See QXP_FISCAL_PATTERN)

        for (i in lines.indices) {
            val line = lines[i]
            
            // Try Pattern 1 (Price Qty Total)
            var matcher = STANDARD_FISCAL_PATTERN.matcher(line)
            if (matcher.find()) {
                val priceStr = matcher.group(1)
                val qtyStr = matcher.group(2)
                val totalStr = matcher.group(3)
                
                try {
                    val price = parseAmount(priceStr)
                    val qty = parseAmount(qtyStr)
                    val total = parseAmount(totalStr)
                    
                    if (price != null && qty != null && total != null) {
                        // Check math: Price * Qty ~= Total (allow small rounding diff)
                        val calculated = price.multiply(qty)
                        val diff = calculated.subtract(total).abs()
                        
                        if (diff.toDouble() < 1.0) {
                            // Found a match! Name is likely the rest of this line OR the previous line
                            var name = line.substring(0, matcher.start()).trim()
                            if (name.isEmpty() || name.length < 3) {
                                // Checking previous line
                                if (i > 0) {
                                    val prevLine = lines[i-1]
                                    // Ensure previous line isn't another item line (heuristic)
                                    if (!STANDARD_FISCAL_PATTERN.matcher(prevLine).find() && !QXP_FISCAL_PATTERN.matcher(prevLine).find()) {
                                        name = prevLine
                                    }
                                }
                            }
                            
                            if (name.isNotEmpty() && !name.contains("------") && !name.uppercase().contains("UKUPNO")) {
                                items.add(com.platisa.app.core.domain.model.ReceiptItem(
                                    name = normalizeText(name) ?: name,
                                    quantity = qty,
                                    unitPrice = price,
                                    total = total
                                ))
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
            
            // Try Pattern 2 (Qty x Price Total)
            matcher = QXP_FISCAL_PATTERN.matcher(line)
            if (matcher.find()) {
                val qtyStr = matcher.group(1)
                val priceStr = matcher.group(2)
                val totalStr = matcher.group(3)
                
                try {
                    val price = parseAmount(priceStr)
                    val qty = parseAmount(qtyStr)
                    val total = parseAmount(totalStr)
                    
                    if (price != null && qty != null && total != null) {
                        val calculated = price.multiply(qty)
                        val diff = calculated.subtract(total).abs()
                        
                        if (diff.toDouble() < 1.0) {
                            var name = line.substring(0, matcher.start()).trim()
                             if (name.isEmpty() || name.length < 3) {
                                if (i > 0) items.add(com.platisa.app.core.domain.model.ReceiptItem(
                                    name = normalizeText(lines[i-1]) ?: lines[i-1],
                                    quantity = qty,
                                    unitPrice = price,
                                    total = total
                                ))
                             } else {
                                  items.add(com.platisa.app.core.domain.model.ReceiptItem(
                                    name = normalizeText(name) ?: name,
                                    quantity = qty,
                                    unitPrice = price,
                                    total = total
                                ))
                             }
                        }
                    }
                } catch (e: Exception) {}
            }
        }
        
        return items
    }
    
    private fun parseAmount(text: String?): BigDecimal? {
        if (text == null) return null
        return try {
            BigDecimal(text.replace(".", "").replace(",", "."))
        } catch (e: Exception) {
             try {
                BigDecimal(text.replace(",", "."))
             } catch (e2: Exception) { null }
        }
    }

    private fun extractRecipientInfo(text: String, merchantName: String? = null): Pair<String?, String?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var name: String? = null
        var address: String? = null

        // SPECIAL CASE: JKP INFOSTAN
        // 1. Check merchant name (Latin & Cyrillic)
        val upperMerchant = merchantName?.uppercase()
        val isInfostanMerchant = upperMerchant?.contains("INFOSTAN") == true || upperMerchant?.contains("ИНФОСТАН") == true
        
        // 2. Check for unique Infostan layout pattern (Opstina anywhere in document)
        // Broaden match to catch OCR errors (missing 'O', mixed scripts)
        val hasOpstina = lines.any { 
            val upper = it.uppercase()
            upper.contains("OPŠTINA") || upper.contains("OPSTINA") || 
            upper.contains("ОПШТИНА") || upper.contains("OПШТИНА") || 
            upper.contains("0ПШТИНА") || upper.contains("ПШТИНА") || // Missing 'O' (Cyrillic)
            upper.contains("PSTINA")     // Missing 'O' (Latin)
        }
        
        // Removed strict Naselje check to ensure we trigger even if regex missed it
        val hasInfostanLayout = hasOpstina

        if (isInfostanMerchant || hasInfostanLayout) {
            val (infostanName, infostanAddress) = parseInfostanRecipient(text)
            if (infostanAddress != null) return infostanName to infostanAddress
        }

        // SPECIAL CASE: TELEKOM / MTS / YETTEL / A1 (Explicit merchant check)
        val isTelekomMerchant = upperMerchant?.contains("TELEKOM") == true || upperMerchant?.contains("MTS") == true ||
                                upperMerchant?.contains("YETTEL") == true || upperMerchant?.contains("A1") == true ||
                                upperMerchant?.contains("SBB") == true || upperMerchant?.contains("ORION") == true
        
        if (isTelekomMerchant) {
             val (telName, telAddr) = parseTelekomRecipient(text)
             // Only return if we actually found an address, otherwise fall through to generic
             if (telAddr != null) return telName to telAddr
        }

        // 1. SPECIFIC KEYWORDS (Priority)
        // User requested: "Adresa korisnika", "Adresa mernog mesta", "Opstina"
        val addressKeywords = listOf(
            "Adresa mernog mesta", "Адреса мерног места",
            "Adresa korisnika", "Адреса корисника",
            "Adresa objekta", "Адреса објекта"
        )
        
        for (i in lines.indices) {
            val line = lines[i]
            val keywordMatch = addressKeywords.find { line.contains(it, ignoreCase = true) }
            if (keywordMatch != null) {
                // DETECT TELEKOM-STYLE ADDRESS (3 lines) via "Adresa korisnika"
                // Even if merchant detection failed, this keyword implies the format.
                if (keywordMatch.contains("korisnika", ignoreCase = true)) {
                    val (telName, telAddr) = parseTelekomRecipient(text)
                    if (telAddr != null) return telName to telAddr
                }

                // Address is usually after the keyword or on the next line
                var candidate = line.substringAfter(keywordMatch).trim().removePrefix(":").trim()
                
                // FORCE CAPTURE: If we found "Adresa objekta", TRUST the next part/line!
                if (candidate.isBlank() && i < lines.size - 1) {
                     val nextLine = lines[i+1]
                     // Basic sanity check only (no dates/money)
                     if (!nextLine.contains("RSD") && !nextLine.contains("DIN") && !nextLine.contains(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))) {
                         address = nextLine
                         // Append second line if it looks like City/Opstina
                         if (i < lines.size - 2 && isValidSubsequentAddressLine(lines[i+2])) {
                             address += ", ${lines[i+2]}"
                         }
                         if (name == null && i > 0) name = findNameAbove(lines, i)
                         break
                     }
                } else if (candidate.isNotBlank()) {
                    address = candidate
                    if (i < lines.size - 1 && isValidSubsequentAddressLine(lines[i+1])) {
                        address += ", ${lines[i+1]}"
                    }
                    if (name == null && i > 0) name = findNameAbove(lines, i)
                    break
                }
            }
        }
        
        // 3. Fallback: Standard Anchors (Korisnik, Kupac)
        if (name == null || address == null) {
            val anchors = listOf("KUPAC:", "КОРИСНИК:", "Korisnik:", "PRIMALAC:", "ПОТРОШАЧ:", "Potrošač:", "PLATILAC:", "ПЛАТИЛАЦ:")
            for (i in lines.indices) {
                val line = lines[i]
                for (anchor in anchors) {
                    if (line.contains(anchor, ignoreCase = true)) {
                        val candidateName = line.substringAfter(anchor).trim()
                        if (isValidName(candidateName)) name = candidateName
                        
                        // Look for address below
                        if (address == null && i < lines.size - 1) {
                            val candidateAddr = lines[i+1]
                            if (isValidAddress(candidateAddr)) {
                                address = candidateAddr
                                // Check for second row of address
                                if (i < lines.size - 2 && isValidSubsequentAddressLine(lines[i+2])) {
                                    address += ", ${lines[i+2]}"
                                }
                                break
                            } else {
                                // DEBUG: Capture rejected fallback
                                // address = "DEBUG: FALLBACK REJECTED [${lines[i+1]}]"
                            }
                        }
                    }
                }
            }
        }
        
        // 4. GLOBAL DEBUG CATCH-ALL
        if (address == null) {
            address = "ADRESA: DEBUG GLOBAL FAILURE. Lines: ${lines.take(2)}" 
        }

        return normalizeText(name) to normalizeText(address)?.let { SerbianGrammarUtils.transliterateCyrillicToLatin(it) }
    }

    /**
     * Specialized parser for Telekom/MTS recipient info (3-line format).
     * Format:
     * Adresa korisnika:
     * [Street Address]
     * [Postal Code City]
     * [Municipality]
     */
    private fun parseTelekomRecipient(text: String): Pair<String?, String?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var name: String? = null
        var address: String? = null
        
        val keywords = listOf("Adresa korisnika", "Адреса корисника")
        
        for (i in lines.indices) {
            val line = lines[i]
            if (keywords.any { line.contains(it, ignoreCase = true) }) {
                // Determine name (usually above "Adresa korisnika" if present, but user screenshot shows Adresa block isolated)
                // If name is needed, we look above.
                if (i > 0) name = findNameAbove(lines, i)
                
                // Address parts: Line i+1 (Street), Line i+2 (City), Line i+3 (Municipality)
                val parts = mutableListOf<String>()
                
                // Line 1: Street
                if (i + 1 < lines.size) {
                    val l1 = cleanAddress(lines[i+1])
                    if (l1 != null && !l1.contains("RSD") && !l1.contains("DIN")) parts.add(l1)
                }
                
                // Line 2: City / Postal
                if (i + 2 < lines.size) {
                    val l2 = lines[i+2]
                    if (!l2.contains("RSD") && !l2.contains("DIN")) parts.add(l2)
                }
                
                // Line 3: Municipality (user said "third one is county")
                if (i + 3 < lines.size) {
                    val l3 = lines[i+3]
                    // Basic sanity check
                    if (!l3.contains("RSD") && !l3.contains("DIN") && !l3.matches(Regex(".*\\d{2}\\.\\d{2}\\.\\d{4}.*"))) {
                        parts.add(l3)
                    }
                }
                
                if (parts.isNotEmpty()) {
                    address = parts.joinToString(", ")
                    break
                }
            }
        }
        
        // Transliterate and normalize (Telekom uses Title Case preference too)
        return normalizeText(name) to address?.let { normalizeText(SerbianGrammarUtils.transliterateCyrillicToLatin(it).uppercase()) }
    }

    /**
     * Specialized parser for JKP Infostan recipient info.
     * REWRITE (User Request): 
     * - Focus ONLY on "Opština" line and the immediate next line for Address.
     * - Address is ALWAYs effectively the row below Opština.
     * - "Naselje" is ignored/stripped if possible to focus on address.
     */
    private fun parseInfostanRecipient(text: String): Pair<String?, String?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var name: String? = null
        var opstina: String? = null
        var address: String? = null
        
        // Key concept: Find specific "anchor" line (Opština)
        // Then assume the line immediately FOLLOWING it is the street address.
        
        val opstinaKeywords = listOf(
            "Opština", "Opstina", "Општина", "Oпштина", "Oпштuна", "0ПШТИНА",
            "ПШТИНА", "PSTINA" // Broaden anchors to match trigger logic
        )
        
        android.util.Log.d("ReceiptParser", "INFOSTAN: Starting parse. Lines count: ${lines.size}")

        for (i in lines.indices) {
            val line = lines[i]
            
            // 1. Locate Opština
            if (opstinaKeywords.any { line.contains(it, ignoreCase = true) }) {
                
                // Determine value for Opština (strip label)
                var cleanedOpstina = line
                for (kw in opstinaKeywords) {
                     cleanedOpstina = cleanedOpstina.replace(kw, "", ignoreCase = true).replace(":", "").trim()
                }
                
                val naseljeSplit = listOf("Naselje", "Насеље", "NASELJE", "НАСЕЉЕ")
                for (nas in naseljeSplit) {
                    if (cleanedOpstina.contains(nas, ignoreCase = true)) {
                        cleanedOpstina = cleanedOpstina.substringBefore(nas, cleanedOpstina).trim()
                    }
                }
                
                opstina = cleanedOpstina.trim().removeSuffix(":").trim()
                
                // 2. CAPTURE ADDRESS FROM NEXT LINE
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1]
                    
                    if (!nextLine.contains("RAČUN") && !nextLine.contains("DATUM") && !nextLine.contains("IZNOS")) {
                       address = cleanAddress(nextLine) ?: nextLine
                    } else {
                        // DEBUG: Capture why we rejected it
                        address = "ADRESA: DEBUG REJECTED: ${nextLine.take(20)}"
                    }
                } else {
                    address = "ADRESA: DEBUG NO NEXT LINE"
                }
                
                // 3. CAPTURE NAME FROM PREVIOUS LINE
                if (name == null && i > 0) {
                     name = lines[i-1] 
                }
                
                break 
            }
        }
        
        // DEBUG: IF NO OPSTINA FOUND AT ALL
        if (address == null) {
            address = "ADRESA: DEBUG NO OPSTINA FOUND. Lines: ${lines.size}"
        }
        
        android.util.Log.d("ReceiptParser", "INFOSTAN: Final - Name: $name, Addr: $address, Mun: $opstina")

        // DE-DUPLICATION: Remove Name from Opstina if possibly merged
        if (!name.isNullOrBlank() && !opstina.isNullOrBlank()) {
            // Simple containment check (case insensitive)
            if (opstina.contains(name, ignoreCase = true)) {
                opstina = opstina.replace(name, "", ignoreCase = true).trim()
            }
            // Also try removing common mis-scans or partials if the name is long enough
            if (name.length > 5) {
                // Remove similar looking strings or if name is just prepended
                 opstina = opstina.replace(Regex("(?i)^${Regex.escape(name)}"), "").trim()
            }
        }

        // FORMAT OUTPUT: "[Addr], [Opstina]" (No "Adresa:" labels)
        val finalParts = mutableListOf<String>()
        address?.takeIf { it.isNotBlank() && !it.startsWith("ADRESA: DEBUG") }?.let { finalParts.add(it) }
        opstina?.takeIf { it.isNotBlank() }?.let { finalParts.add(it) }

        val finalAddressString = if (finalParts.isNotEmpty()) finalParts.joinToString(", ") else null
        
        return normalizeText(name) to finalAddressString?.let { normalizeText(SerbianGrammarUtils.transliterateCyrillicToLatin(it).uppercase()) }
    }

    private fun findNameAbove(lines: List<String>, addressIndex: Int): String? {
        // Search up to 3 lines above for a valid name
        for (j in (addressIndex - 1) downTo (addressIndex - 3).coerceAtLeast(0)) {
            val candidate = lines[j]
            if (isValidName(candidate)) return candidate
        }
        return null
    }

    private fun isValidName(text: String): Boolean {
        if (text.length < 3 || text.length > 50) return false
        val upper = text.uppercase()
        // Names shouldn't contain these utility keywords
        val forbidden = listOf(
            "RSD", "DIN", "РСД", "ДИН", "ADRESA", "ULICA", "OBRAČUN", "RAČUN", 
            "OPŠTINA", "NASELJE", "PUT", "ПУТ", "ULAZ", "УЛАЗ", "BROJ", "BR."
        )
        if (forbidden.any { upper.contains(it) }) return false
        
        // Names usually don't have many numbers unless it's a misread
        val digitCount = text.count { it.isDigit() }
        if (digitCount > 3) return false 
        
        // Should have at least some letters
        if (!text.any { it.isLetter() }) return false
        
        return true
    }

    private fun isValidAddress(text: String): Boolean {
        if (text.length < 3) return false
        val upper = text.uppercase()
        
        // Comprehensive address markers including Cyrillic/Latin lookalikes
        val addressMarkers = listOf(
            "ADRESA", "ULICA", "ПУТ", "PUT", "ST.", "CT.", "BB", 
            "АДРЕСА", "АДpеса", "Aдpеса", "Aдреса"
        )
        if (addressMarkers.any { upper.contains(it) }) return true
        
        // Should contain at least one digit (house number) or "BB"
        // RELAXED: Don't strictly require a number. Many addresses are just "Main Street" in header.
        // val hasNumber = text.any { it.isDigit() } || upper.contains("BB")
        // if (!hasNumber) { ... }
        
        // Reject money lines
        if (upper.contains("RSD") || upper.contains("DIN") || upper.contains("РСД") || upper.contains("ДИН") || upper.contains("€") || upper.contains("EUR")) return false
        if (text.contains(Regex("\\d+,\\d{2}"))) return false // No decimals like ,00
        // Reject purely numeric
        if (text.matches(Regex("^[\\d.,\\s-]+$"))) return false
        // Reject dates
        if (text.matches(Regex(".*\\d{2}\\.\\d{2}\\.\\d{4}.*"))) return false
        
        return true
    }

    /**
     * Strips technical metadata and noisy prefixes from addresses.
     */
    private fun cleanAddress(text: String?): String? {
        val input = text ?: return null
        var cleaned: String = input
        val metadataKeywords = listOf(
            "VRSTA PROSTORA", "ŠIFRA PROSTORA", "SIFRA PROSTORA",
            "ВРСТА ПРОСТОРА", "ШИФРА ПРОСТОРА", "KATEGORIJA", "POVRŠINA",
            "VRSTA", "SIFRA", "ŠIFRA"
        )
        
        for (keyword in metadataKeywords) {
            // If the keyword exists, remove everything from it onwards if it's at the end, 
            // or just the keyword and immediate value if it's in the middle.
            val regex = Regex("(?i)$keyword[:\\s]+[^,]*", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replace(regex, "").trim()
        }
        
        // Also strip "Adresa:" or "Ulica:" prefixes if they are at the START of the string
        // (Don't remove them if they are in the middle as part of a street name like "Ulica lipa")
        val prefixLabels = listOf(
            "Adresa", "Адреса", "Адpеса", "Aдpеса", 
            "Ulica", "Ul.", "Ul ", "Улица", "Ул.", "Ул " 
        )
        for (label in prefixLabels) {
            val regex = Regex("^(?i)$label[:\\s]*", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replace(regex, "").trim()
        }
        
        return cleaned.removePrefix(":").removePrefix(",").trim().removeSuffix(",").trim().takeIf { it.isNotBlank() }
    }

    /**
     * Checks if a line following a street address is likely a secondary address line (Opština, City, Postal Code).
     */
    private fun isValidSubsequentAddressLine(text: String): Boolean {
        if (text.length < 3) return false
        val upper = text.uppercase()
        
        // If it contains "Opština", "Grad", or a 5-digit postal code, it's likely a second row
        if (upper.contains("OPŠTINA") || upper.contains("OPSTINA") || upper.contains("ОПШТИНА")) return true
        if (upper.contains("GRAD") || upper.contains("ГРАД")) return true
        if (text.contains(Regex("\\b\\d{5}\\b"))) return true
        
        // If it looks like money, definitely not an address
        if (upper.contains("RSD") || upper.contains("DIN") || upper.contains("РСД") || upper.contains("ДИН")) return false
        
        return false
    }


    private fun extractInvoiceNumber(text: String): String? {
        android.util.Log.d("ReceiptParser", "=== EXTRACTING INVOICE NUMBER ===")
        android.util.Log.d("ReceiptParser", "Text length: ${text.length}")
        android.util.Log.d("ReceiptParser", "First 500 chars: ${text.take(500).replace("\n", " | ")}")
        
        // Comprehensive patterns for ALL bill types (electricity, water, phone, internet, etc.)
        val patterns = listOf(
            // Serbian CYRILLIC - Рачун број / Број рачуна (MOST IMPORTANT!)
            Pattern.compile("(?:Рачун\\s+број|Број\\s+рачуна|Идентификациони\\s+број|ИДЕНТ)[:\\s]+([\\d/-]+)", Pattern.CASE_INSENSITIVE),
            
            // Serbian LATIN - Identifikacioni broj (Infostan usually uses this or Sistem broj)
            Pattern.compile("(?:Identifikacioni\\s+broj|Sistem\\s+broj|Sistemski\\s+broj|IDENT)[:\\s]+([\\d/-]+)", Pattern.CASE_INSENSITIVE),
            
            // Serbian LATIN - Račun broj / Broj računa (allow dashes and slashes for various formats)
            Pattern.compile("(?:Račun\\s+broj|Broj\\s+računa|Racun\\s+broj|Broj\\s+racuna)[:\\s]+([\\d/-]+)", Pattern.CASE_INSENSITIVE),
            
            // NOTE: DO NOT use Naplatni broj as fallback!
            // Naplatni broj is the meter/account number - SAME for all bills from same address
            // It should NOT be used for duplicate detection
            
            // Telekom specific patterns (both Cyrillic and Latin)
            // Relaxed diacritics: c matches č/ć, s matches š, z matches ž
            // LOGIC FIX: (\d+(?:\s*[\.-]\s*\d+)*)
            // - Starts with digits
            // - Continues ONLY if separated by Dash/Dot (with optional spaces)
            // - STOPS at a bare space (column separator)
            Pattern.compile("(?:Број\\s+рачуна)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),  // Cyrillic
            
            // "Broj računa" -> "Broj racuna"
            Pattern.compile("(?:Broj\\s+ra[c\u010d\u0107]una)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            
            // "Račun br" -> "Racun br"
            Pattern.compile("(?:Ra[c\u010d\u0107]un\\s+br)[:\\s\\.]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Рачун\\s+бр)[:\\s\\.]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Faktura|Фактура)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            
            // Serbian - Broj fakture
            Pattern.compile("(?:Broj\\s+fakture|Faktura\\s+broj|Број\\s+фактуре|Фактура\\s+број)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            
            // English - Invoice/Bill number
            Pattern.compile("(?:Invoice\\s+number|Bill\\s+number|Invoice\\s+no|Bill\\s+no|Invoice\\s+#)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            
            // Abbreviated forms
            Pattern.compile("(?:Ra[c\u010d\u0107]\\.\\s*br|Br\\.\\s*ra[c\u010d\u0107]una|Fakt\\.\\s*br|Рач\\.\\s*бр|Бр\\.\\с*рачуна|Факт\\.\\s*бр)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE),
            
            // =====================================================================
            // MULTI-LINE SUPPORT (Telekom Srbija Fix)
            // =====================================================================
            // 1A. JKP INFOSTAN SPECIAL PATTERN (Year/Month-ID e.g. 2026/01-0859349)
            // This MUST be prioritized over generic partial matches to ensure we get the full ID.
            Pattern.compile("(?:Broj\\s+računa|Broj\\s+racuna|Број\\s+рачуна)[:\\s]+(\\d{4}/\\d{2}-\\d+)", Pattern.CASE_INSENSITIVE),

            // =====================================================================
            // MULTI-LINE SUPPORT (Telekom Srbija Fix)
            // =====================================================================
            // Matches "Račun broj" followed by whitespace/newlines and then the number
            // TOLERANT version: Matches "Račun" or "Racun", "Broj" matches "Broj"
            Pattern.compile("(?:Ra[c\u010d\u0107]un\\s+broj|Broj\\s+ra[c\u010d\u0107]una|Рачун\\s+број|Број\\s+рачуна)[:\\s]+(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE),
            
            // "Račun broj" on one line, number on next (without colon)
            Pattern.compile("(?:Ra[c\u010d\u0107]un\\s+broj|Рачун\\s+број)\\s*\\n\\s*(\\d+(?:\\s*[\\.-]\\s*\\d+)*)", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE)
            
            // REMOVED: Generic fallbacks that were matching Naplatni broj instead of Račun broj:
            // - "(?:Račun|...|Bill)[^\d]*(\d{5,})" - too generic
            // - "\b(\d{5,})\b" - matches ANY number
            // - "(?:ID|Id)[:\s]+(\d{5,})" - too generic
        )
        
        // Search the ENTIRE text (some bills have information at the bottom)
        val searchText = text
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(searchText)
            if (matcher.find()) {
                val rawNumber = matcher.group(1)?.trim()
                if (rawNumber != null) {
                    // Clean the number: remove dashes, dots, slashes, spaces for storage
                    val cleanNumber = rawNumber.replace("-", "").replace(".", "").replace("/", "").replace(" ", "")
                    

                    if (cleanNumber.length >= 5) {  // Minimum 5 digits
                        // IGNORE current year as invoice number (avoid false positives)
                        val numInt = cleanNumber.toIntOrNull()
                        val isYear = numInt != null && (numInt in 2020..2030)
                        
                        if (!isYear) {
                            android.util.Log.d("ReceiptParser", "✅ Found invoice number: $rawNumber (cleaned: $cleanNumber) using pattern #$index")
                            return cleanNumber
                        }
                    }
                }
            }
        }
        
        
        // 2. FALLBACK: Manual Line-by-Line Inspection (Robust for "Label \n Value" layouts)
        // If regex failed, let's manually look for "Račun broj" on one line and numbers on the NEXT line
        try {
            val inspectionRegex = Regex("""(\d+(?:\s*[\\.-]\s*\d+)*)""")
            
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            for (i in 0 until lines.size - 1) {
                val currentLine = lines[i].lowercase().replace("č", "c").replace("ć", "c")
                
                // Check if current line acts as a label
                if (currentLine.contains("racun broj") || 
                    currentLine.contains("broj racuna")) {
                        
                    val nextLine = lines[i+1]
                    
                    // Prioritize our Strict Match regex
                    // This captures "64-290..." but STOPS at " 058..." because space matches not [\.-]
                    val match = inspectionRegex.find(nextLine)
                    if (match != null) {
                        val candidate = match.value
                        val digitCount = candidate.count { it.isDigit() }
                        
                        if (digitCount >= 5) {
                            val cleanNumber = candidate.replace(Regex("[^0-9]"), "")
                            // Avoid year-like numbers (2024, 2025) unless they are very long
                            val isYear = cleanNumber.length == 4 && (cleanNumber.startsWith("202"))
                            
                            if (!isYear) {
                                android.util.Log.d("ReceiptParser", "✅ Found invoice number via Line Inspection (Strict): '$candidate' (cleaned: $cleanNumber)")
                                return cleanNumber
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ReceiptParser", "Error in line inspection", e)
        }
        
        android.util.Log.w("ReceiptParser", "❌ No invoice number found in text")
        android.util.Log.d("ReceiptParser", "Full text for debugging: ${text.take(1000)}")
        return null
    }

    private fun extractMerchant(text: String): String? {
        val lines = text.lines().filter { it.trim().isNotEmpty() }
        if (lines.isEmpty()) return null
        
        // 0. KNOWN MERCHANTS - Priority Check
        // Check for specific, known large billers first to avoid ambiguity
        val knownMerchants = listOf(
            "EPS DISTRIBUCIJA", "EPS SNABDEVANJE", "ELEKTROPRIVREDA SRBIJE", 
            "JKP INFOSTAN", "INFOSTAN TEHNOLOGIJE", "ЈКП ИНФОСТАН", "ИНФОСТАН ТЕХНОЛОГИЈЕ",
            "TELEKOM SRBIJA", "YETTEL", "A1 SRBIJA", "SBB", "ORION TELEKOM",
            "UPRAVA CARINA", "J.P. POŠTA", "JP POSTA", "POŠTA SRBIJE", "LIDL", "MAXI", "IDEA", "MERCATOR"
        )
        
        for (line in lines.take(20)) { // Check top 20 lines for known brands
            val upperLine = line.uppercase()
            val match = knownMerchants.find { upperLine.contains(it) }
            if (match != null) {
                // Return the clean match name usually, or the full line if it's short
                return if (match.length > 5) normalizeText(match) else normalizeText(line.trim())
            }
        }
        
        // Strategy 1: Look for company suffixes (DOO, AD, JP, JKP, etc.)
        val companySuffixes = listOf("D.O.O.", "DOO", "A.D.", "AD", "J.P.", "JP", "JKP", "D.O.O", "A.D", "OD", "O.D.")
        for (line in lines.take(15)) { 
            val upperLine = line.uppercase()
            // Ignore if line contains indicators that it's the CONSUMER/USER
            if (isCustomerLine(upperLine)) continue
            
            if (companySuffixes.any { upperLine.contains(it) && isCompanySuffixStart(upperLine, it) }) {
                return cleanMerchantName(line.trim())
            }
        }
        
        // Strategy 2: Explicit Labels ("Prodavac:", "Izdavalac:")
        val merchantLabels = listOf("PRODAVAC:", "IZDAVALAC:", "TRGOVAC:", "PRIMALAC UPLATE:")
        for (line in lines.take(20)) {
            val upperLine = line.uppercase()
            for (label in merchantLabels) {
                if (upperLine.contains(label)) {
                    val name = line.substringAfter(label).trim()
                    if (name.length > 2) return cleanMerchantName(name)
                }
            }
        }
        
        // Strategy 3: First substantial line (Fallback) - but STRICTER now
        // DO NOT grab the first line blindly.
        for (line in lines.take(8)) {
            val trimmed = line.trim()
            val upper = trimmed.uppercase()
            
            if (trimmed.length > 3 && !trimmed.matches(Regex("^[\\d./-]+$"))) {
                // Filter out common header trash
                if (isHeaderOrGreeting(upper)) continue
                if (isCustomerLine(upper)) continue
                if (upper.contains("RAČUN") || upper.contains("IZVEŠTAJ")) continue
                
                // If it looks like a valid name (mostly letters, not too long sentence)
                if (trimmed.length < 50 && trimmed.count { it.isLetter() } > 2) {
                    return cleanMerchantName(trimmed)
                }
            }
        }
        
        return null
    }

    private fun isCompanySuffixStart(text: String, suffix: String): Boolean {
        // Basic check to ensure suffix isn't just part of a word (e.g. "RAD" contains "AD")
        // This is a naive implementation, regex would be better but keeping it simple for now
        val idx = text.indexOf(suffix)
        if (idx > 0 && text[idx-1].isLetter()) return false // Part of word
        return true
    }

    private fun isHeaderOrGreeting(text: String): Boolean {
        val headers = listOf("POŠTOVANI", "POSTOVANI", "DOBRODOŠLI", "OBAVEŠTENJE", "IZVOD", "PREGLED", "STANJE")
        return headers.any { text.startsWith(it) }
    }
    
    private fun isCustomerLine(text: String): Boolean {
        val customerIndicators = listOf("KUPAC", "KORISNIK", "PRIMALAC", "ZA:", "TO:", "POTROŠAČ", "PLATILAC")
        // If line STARTS with these, it's definitely not the merchant name
        return customerIndicators.any { text.startsWith(it) }
    }

    fun cleanMerchantName(line: String): String {
        var cleaned = line
        
        // 0. Cut after company suffix if present to remove address details on same line
        val suffixes = listOf("d.o.o.", "a.d.", "j.k.p.", "j.p.", "doo ", "ad ", "jkp ", "jp ") // added spaces for non-dots
        for (suffix in suffixes) {
            val index = cleaned.indexOf(suffix, ignoreCase = true)
            if (index != -1) {
                // Keep the suffix but cut after it
                // cleaned = cleaned.substring(0, index + suffix.length)
                // Actually, often the name is BEFORE the suffix: "Company Name DOO"
                // No need to cut strictly, just remove address markers later
                break
            }
        }
        
        // 1. Split by comma (often separates name from address/city)
        if (cleaned.contains(",")) {
            cleaned = cleaned.substringBefore(",")
        }
        
        // 2. Remove common address markers
        val addressMarkers = listOf(" ul.", " ul ", " ulica ", " bul.", " bulevar ", " trg ", " put ", " bb")
        for (marker in addressMarkers) {
            val index = cleaned.indexOf(marker, ignoreCase = true)
            if (index != -1) {
                cleaned = cleaned.substring(0, index)
            }
        }
        
        // 3. Remove zip codes/PIB/MB
        cleaned = cleaned.replace(Regex("\\s\\d{5}\\s*$"), "") // Zip
        cleaned = cleaned.replace(Regex("PIB:?\\s*\\d+", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("MB:?\\s*\\d+", RegexOption.IGNORE_CASE), "")
        
        return normalizeText(cleaned.trim()) ?: ""
    }

    /**
     * Normalizuje tekst tako da ne bude sve velikim slovima (Title Case).
     * Npr. "NINKOVIĆ NIKOLA" -> "Ninković Nikola"
     */
    fun normalizeText(text: String?): String? {
        if (text == null || text.isBlank()) return text
        
        // 1. Initial Normalization (Title Case if mostly upper)
        var normalized = if (text.any { it.isLowerCase() }) {
            text // Already has casing, assume good
        } else {
            // Convert ALL-CAPS to Title Case
            text.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
        
        // 2. Fix known abbreviations that should be UPPERCASE
        // "Jkp Infostan" -> "JKP Infostan"
        normalized = normalized
        // 2. Fix known abbreviations that should be UPPERCASE (Handle optional dots)
        // "Jkp Infostan" -> "JKP Infostan"
        // "a.d." -> "AD", "d.o.o." -> "DOO"
        normalized = normalized
            .replace(Regex("(?i)\\bJ\\.?k\\.?p\\.?\\b"), "JKP")
            .replace(Regex("(?i)\\bJ\\.?p\\.?\\b"), "JP")
            .replace(Regex("(?i)\\bD\\.?o\\.?o\\.?\\b"), "DOO")
            .replace(Regex("(?i)\\bA\\.?d\\.?\\b"), "AD")
            .replace(Regex("(?i)\\bEps\\b"), "EPS")
            .replace(Regex("(?i)\\bSbb\\b"), "SBB")
            .replace(Regex("(?i)\\bMts\\b"), "MTS")
            
        return normalized
    }

    private fun extractDate(text: String): Date? {
        // Regex for DD.MM.YYYY or DD/MM/YYYY - Relaxed boundaries to handle OCR noise/Cyrillic
        val datePattern = Pattern.compile("(?<!\\d)(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})(?!\\d)")
        val matcher = datePattern.matcher(text)
        
        val dateCandidates = mutableListOf<Pair<Date, String>>() // Date and source line
        val lines = text.lines()
        
        // Helper to find line containing the match
        fun findLineFor(start: Int): String {
            var count = 0
            for (line in lines) {
                if (start < count + line.length + 1) return line
                count += line.length + 1
            }
            return ""
        }

        while (matcher.find()) {
            val dateStr = matcher.group(1)
            val formats = listOf("dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yy", "dd/MM/yy", "dd-MM-yy")
            for (format in formats) {
                try {
                    if (dateStr != null) {
                        val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                        if (date != null) {
                            // Validate year to avoid noise (e.g. 2000-2030)
                            val year = Integer.parseInt(SimpleDateFormat("yyyy", Locale.getDefault()).format(date))
                            if (year in 2020..2030) {
                                val line = findLineFor(matcher.start())
                                dateCandidates.add(date to line)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue
                }
            }
        }
        
        if (dateCandidates.isEmpty()) return null
    
        // Priority Keywords (Latin & Cyrillic) - specifically for ISSUE dates
        val issueKeywords = listOf(
            "DATUM IZDAVANJA", "DANA", "IZDAT", "PROMET", "MESTO", "BEOGRAD",
            "ДАТУМ ИЗДАВАЊА", "ДАНА", "ИЗДАТ", "ПРОМЕТ", "МЕСТО", "БЕОГРАД"
        )
        
        // 1. Priority: Date on same line as "Issue" keywords
        for ((date, line) in dateCandidates) {
            val upperLine = line.uppercase()
            if (issueKeywords.any { upperLine.contains(it) }) {
                android.util.Log.d("ReceiptParser", "✅ Found Priority (Issue) Date: $date in line: $line")
                return date
            }
        }
        
        // 2. Secondary: If multiple dates exist, pick the STABLE one. 
        // Usually Issue Date is EARLIER than Due Date. 
        // Phone A and Phone B might see dates in different OCR order, 
        // but the set of dates is usually the same. min() is a stable choice.
        if (dateCandidates.size > 1) {
            val minDate = dateCandidates.minByOrNull { it.first.time }?.first
            if (minDate != null) {
                android.util.Log.d("ReceiptParser", "⚖️ MULTIPLE DATES: Selecting earliest (min) for stability: $minDate")
                return minDate
            }
        }
        
        // 3. Fallback: Return the FIRST detected valid date
        return dateCandidates.first().first
    }

    private fun extractDueDate(text: String): Date? {
        // Patterns for payment deadline - MUST SUPPORT BOTH LATIN AND CYRILLIC
        val patterns = listOf(
            // Rok za placanje / Рок за плаћање (allow optional "računa" or other words up to 15 chars)
            Pattern.compile("(?:Rok\\s+za\\s+pla[ćc]anje|Рок\\s+за\\s+плаћање|Rok\\s+pla[ćc]anja|Рок\\s+плаћања)(?:\\s+\\w+){0,2}[:\\s]*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            
            // Rok dospelosti / Datum dospelosti
            Pattern.compile("(?:Rok\\s+dospelosti|Рок\\s+доспелости|Datum\\s+dospelosti|Датум\\s+доспелости)[:\\s]*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})", Pattern.CASE_INSENSITIVE),

            // Datum valute / Датум валуте / Valuta / Валута
            Pattern.compile("(?:Datum\\s+valute|Valuta|Датум\\s+валуте|Валута)[:\\s]*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            
            // Platiti do / Платити до / Uplatiti do / Уплатити до
            Pattern.compile("(?:Platiti\\s+do|Uplatiti\\s+do|Платити\\s+до|Уплатити\\s+до|Platiti\\s+najkasnije\\s+do)[:\\s]*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4})", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateStr = matcher.group(1)?.replace(" ", "") ?: continue
                val formats = listOf("dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yy", "dd/MM/yy", "dd-MM-yy")
                for (format in formats) {
                    try {
                        return SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            }
        }
        return null
    }

    private fun extractTotalAmount(text: String): BigDecimal? {
        // Comprehensive list of keywords for bill amounts in Serbian and English
        val strongKeywords = listOf(
            "ZA UPLATU", "ZA ISPLATU", "UKUPNO ZA UPLATU", "IZNOS ZA UPLATU", 
            "UKUPAN IZNOS", "SVEGA", "IZNOS RAČUNA", "AMOUNT DUE", "TOTAL AMOUNT"
        )
        
        val weakKeywords = listOf("UKUPNO", "IZNOS", "TOTAL", "SUMA", "SALDO", "DUGOVANJE")
        
        val lines = text.lines()
        
        // Strategy 1: Look for STRONG keywords (high confidence)
        for (line in lines) {
            val upperLine = line.uppercase()
            for (keyword in strongKeywords) {
                if (upperLine.contains(keyword)) {
                    val amount = extractAmountFromLine(line)
                    if (amount != null && amount > BigDecimal(10)) return amount
                }
            }
        }
        
        // Strategy 2: Look for STRONG keyword on line i, amount on line i+1
        for (i in 0 until lines.size - 1) {
            val upperLine = lines[i].uppercase()
            for (keyword in strongKeywords) {
                if (upperLine.contains(keyword)) {
                    val amount = extractAmountFromLine(lines[i + 1])
                    if (amount != null && amount > BigDecimal(10)) return amount
                }
            }
        }
        
        // Strategy 3: Look for Currency Symbols (RSD, DIN) - Highest Reliability
        val currencyRegex = Regex("(?i)(RSD|DIN|DINARA|RSD\\.|DIN\\.)")
        for (line in lines) {
            if (currencyRegex.containsMatchIn(line)) {
                val amount = extractAmountFromLine(line)
                if (amount != null && amount > BigDecimal(10)) return amount
            }
        }
        
        // Strategy 4: Fallback to weak keywords (but require larger amounts to avoid date/page noise)
        for (line in lines) {
            val upperLine = line.uppercase()
            for (keyword in weakKeywords) {
                if (upperLine.contains(keyword)) {
                    val amount = extractAmountFromLine(line)
                    if (amount != null && amount > BigDecimal(100)) return amount // Higher threshold
                }
            }
        }
        
        // Strategy 5: Bottom Logic (finding largest number at bottom)
        // DANGEROUS: Only if it looks like a real currency format (decimals)
        val bottomLines = lines.takeLast((lines.size * 0.2).toInt().coerceAtLeast(10))
        var largestAmount: BigDecimal? = null
        
        for (line in bottomLines) {
            val amount = extractAmountFromLine(line, requireDecimal = true) // STRICT MODE
            if (amount != null && amount > BigDecimal(50)) {
                if (largestAmount == null || amount > largestAmount) {
                    largestAmount = amount
                }
            }
        }
        
        return largestAmount
    }

    private fun extractAmountFromLine(line: String, requireDecimal: Boolean = false): BigDecimal? {
        var maxAmount: BigDecimal? = null
        
        // Pattern 1: Serbian format (1.234,56)
        val serbianPattern = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})+,\\d{2})")
        var matcher = serbianPattern.matcher(line)
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(".", "")?.replace(",", ".") ?: continue
            try {
                val amount = BigDecimal(amountStr)
                if (maxAmount == null || amount > maxAmount) maxAmount = amount
            } catch (e: Exception) {}
        }

        // NEW: Serbian format with just dot separator (1.195)
        // ONLY if there are exactly 3 digits after the dot (common for thousands)
        val serbianThousandsPattern = Pattern.compile("\\b(\\d{1,3}(?:\\.\\d{3})+)\\b")
        matcher = serbianThousandsPattern.matcher(line)
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(".", "") ?: continue
            try {
                val amount = BigDecimal(amountStr)
                // Heuristic: If it has dots for thousands, it's likely > 1000
                if (amount > BigDecimal(100)) {
                    if (maxAmount == null || amount > maxAmount) maxAmount = amount
                }
            } catch (e: Exception) {}
        }
        
        // Pattern 2: Serbian/European (1234,56)
        val serbianNoSepPattern = Pattern.compile("(\\d+,\\d{2})\\b")
        matcher = serbianNoSepPattern.matcher(line)
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", ".") ?: continue
            try {
                val amount = BigDecimal(amountStr)
                if (maxAmount == null || amount > maxAmount) maxAmount = amount
            } catch (e: Exception) {}
        }
        
        // Pattern 3: International (1,234.56 or 1234.56)
        val intlPattern = Pattern.compile("(\\d{1,3}(?:,\\d{3})*\\.\\d{2})\\b")
        matcher = intlPattern.matcher(line)
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "") ?: continue
            try {
                val amount = BigDecimal(amountStr)
                if (maxAmount == null || amount > maxAmount) maxAmount = amount
            } catch (e: Exception) {}
        }
        
        // Pattern 4: Integers (ONLY if requireDecimal is FALSE)
        if (!requireDecimal && maxAmount == null) {
            // Must be at least 3 digits to avoid page numbers "1", "2"
            // And avoid "2025" (year) range usually
            val simplePattern = Pattern.compile("\\b(\\d{3,})\\b") 
            matcher = simplePattern.matcher(line)
            while (matcher.find()) {
                val amountStr = matcher.group(1) ?: continue
                try {
                    val amount = BigDecimal(amountStr)
                    // Heuristic: If it looks like a year (2020-2030), ignore it unless it has currency keyword nearby
                    val isYear = amount.toInt() in 2020..2035
                    if (!isYear && amount > BigDecimal(100)) {
                        if (maxAmount == null || amount > maxAmount) maxAmount = amount
                    }
                } catch (e: Exception) {}
            }
        }
        
        if (maxAmount != null) {
             android.util.Log.d("ReceiptParser", "💵 PARSED AMOUNT candidate: $maxAmount from line: ${line.take(50)}")
        }
        return maxAmount
    }

    private fun extractDiscountDeadline(text: String): String? {
        // Pattern for Infostan discount deadline: "do 18.02.2026. godine"
        // Also supports Cyrillic "до 18.02.2026. године"
        val pattern = Regex("""(?:do|до)\s+(\d{1,2}\.\d{1,2}\.\d{4})\.?\s*(?:godine|године)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        val deadline = match?.groupValues?.get(1)
        
        if (deadline != null) {
            android.util.Log.d("ReceiptParser", "🎯 Found DISCOUNT DEADLINE: $deadline")
        }
        return deadline
    }

    private fun extractCurrentMonthAmount(text: String): BigDecimal? {
        val patterns = listOf(
            Regex("(?:Zaduženje\\s+za\\s+tekući\\s+period|Tekuće\\s+zaduženje|Iznos\\s+računa|Zaduženje\\s+u\\s+ovom\\s+obračunu)[:\\s]+([\\d.,]+)", RegexOption.IGNORE_CASE),
            Regex("(?:Zadu\u009eenje\\s+za\\s+teku\u0086i\\s+period)[:\\s]+([\\d.,]+)", RegexOption.IGNORE_CASE) // OCR artifacts
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amount = parseAmount(amountStr)
                if (amount != null && amount > BigDecimal.ZERO) return amount
            }
        }
        return null
    }

    private fun extractPreviousDebtAmount(text: String): BigDecimal? {
        val patterns = listOf(
            Regex("(?:Prethodni\\s+dug|Dug\\s+iz\\s+prethodnog\\s+perioda|Preostali\\s+dug|Dug\\s+na\\s+dan)[:\\s]+([\\d.,-]+)", RegexOption.IGNORE_CASE),
            Regex("(?:Iznos\\s+duga)[:\\s]+([\\d.,-]+)", RegexOption.IGNORE_CASE)
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amount = parseAmount(amountStr)
                // Debt can be negative if overpaid
                if (amount != null) return amount
            }
        }
        return null
    }
}


