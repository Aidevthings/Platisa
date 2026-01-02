package com.platisa.app.core.domain.parser

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceipt(
    val merchantName: String? = null,
    val date: Date? = null,
    val totalAmount: BigDecimal? = null,
    val qrCodeData: String? = null,
    val invoiceNumber: String? = null,  // Račun broj - unique invoice number
    val items: List<com.platisa.app.core.domain.model.ReceiptItem> = emptyList(),
    val dueDate: Date? = null,
    val recipientName: String? = null,
    val recipientAddress: String? = null
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
        val (recipientName, recipientAddress) = extractRecipientInfo(text)
        val items = extractItems(text)
        
        return ParsedReceipt(
            merchantName = merchant, 
            date = date, 
            totalAmount = amount, 
            qrCodeData = null, 
            invoiceNumber = invoiceNumber, 
            items = items, 
            dueDate = dueDate,
            recipientName = recipientName,
            recipientAddress = recipientAddress
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

    private fun extractRecipientInfo(text: String): Pair<String?, String?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        // List of major Serbian municipalities/cities (Latin and Cyrillic)
        val municipalities = listOf(
            "BEOGRAD", "БЕОГРАД",
            "NOVI BEOGRAD", "НОВИ БЕОГРАД",
            "ZEMUN", "ЗЕМУН",
            "NOVI SAD", "НОВИ САД",
            "NIŠ", "NIS", "НИШ",
            "KRAGUJEVAC", "КРАГУЈЕВАЦ",
            "SUBOTICA", "СУБОТИЦА",
            "PANČEVO", "PANCEVO", "ПАНЧЕВО",
            "ČAČAK", "CACAK", "ЧАЧАК",
            "KRUŠEVAC", "KRUSEVAC", "КРУШЕВАЦ",
            "KRALJEVO", "КРАЉЕВО",
            "NOVI PAZAR", "НОВИ ПАЗАР",
            "SMEDEREVO", "СМЕДЕРЕВО",
            "LESKOVAC", "ЛЕСКОВАЦ",
            "VALJEVO", "ВАЉЕВО",
            "VRANJE", "ВРАЊЕ",
            "STARI GRAD", "СТАРИ ГРАД",
            "VRAČAR", "VRACAR", "ВРАЧАР",
            "ZVEZDARA", "ЗВЕЗДАРА",
            "SAVSKI VENAC", "САВСКИ ВЕНАЦ",
            "VOŽDOVAC", "VOZDOVAC", "ВОЖДОВАЦ",
            "ČUKARICA", "CUKARICA", "ЧУКАРИЦА",
            "RAKOVICA", "РАКОВИЦА",
            "PALILULA", "ПАЛИЛУЛА",
            "SURČIN", "SURCIN", "СУРЧИН"
        )
        
           // Strategy 1: Explicit labels (with common OCR misreadings)
        val anchors = listOf(
            "KUPAC:", "КОРИСНИК:", "Korisnik:", "PRIMALAC:", "ПОТРОШАЧ:", "Potrošač:", "PLATILAC:", "ПЛАТИЛАЦ:",
            "KYPAC:", "KORISNIK:", "PRIMILAC:", "PO PRIMALAC:", "KORlSNIK:", "KORISNlK:", "PLATILAC"
        )
        for (i in lines.indices) {
            val line = lines[i]
            for (anchor in anchors) {
                if (line.contains(anchor, ignoreCase = true)) {
                    val name = line.substringAfter(anchor).trim()
                    // If name is short/empty on same line, try next line
                    if (name.length < 3 && i < lines.size - 1) {
                        val nextLine = lines[i+1]
                        if (nextLine.length > 5 && !municipalities.any { nextLine.uppercase().contains(it) }) {
                             val address = if (i < lines.size - 2) lines[i + 2] else null
                             return normalizeText(nextLine) to normalizeText(address)
                        }
                    }
                    val address = if (i < lines.size - 1) lines[i + 1] else null
                    if (name.isNotEmpty()) return normalizeText(name) to normalizeText(address)
                }
            }
        }

        // Strategy 2: Municipality anchors (Implicit)
        // STRICTLY limit to the top 50% of the document to avoid footer noise (like "Prigovore...")
        val searchLimit = (lines.size * 0.5).toInt().coerceAtLeast(10)
        
        for (i in 0 until searchLimit.coerceAtMost(lines.size)) {
            val line = lines[i]
            val upperLine = line.uppercase()
            
            // Check for municipality in line (Infostan style)
            val munMatch = municipalities.find { upperLine.contains(it) }
            if (munMatch != null && upperLine.length < 35) {
                // Heuristic: Name is line ABOVE, Address is often line BELOW or combines with municipality
                val name = if (i > 0) lines[i - 1] else null
                val addressLine = if (i < lines.size - 1) lines[i + 1] else null
                
                // If the name line looks like a name (short, no numbers usually, capitalized)
                if (name != null && name.length > 3 && !name.contains(Regex("\\d{2}[./-]\\d{2}")) && !name.contains(Regex("\\d{3,}"))) {
                     // Extra check: Forbidden words in name (footer protection)
                     val forbidden = listOf("prigovore", "možete", "podneti", "reklamacije", "račun", "rok")
                     if (forbidden.none { name.lowercase().contains(it) }) {
                        // Sometimes the address is exactly the municipality line or below it
                        val address = if (upperLine.length > munMatch.length + 5) line else addressLine
                        return normalizeText(name) to normalizeText(address)
                     }
                }
            }
        }
        
        // Strategy 3: Look for street names + number pattern
        val addressPattern = Regex(".*[A-ZA-Žа-за-ж\\s]{3,}\\s+\\d+[a-z]*.*", RegexOption.IGNORE_CASE)
        for (i in lines.indices) {
            val line = lines[i]
            if (addressPattern.matches(line)) {
                // If we found an address, maybe the name is above it?
                val possibleName = if (i > 0) lines[i-1] else null
                if (possibleName != null && possibleName.length > 5 && possibleName.length < 40 && !possibleName.contains(Regex("\\d"))) {
                    return normalizeText(possibleName) to normalizeText(line)
                }
            }
        }
        
        return null to null
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
            
            // FALLBACK: ED broj / ЕД број (for EPS bills without Račun broj visible)
            Pattern.compile("(?:ED\\s+broj|ЕД\\s+број)[:\\s]+([\\d/-]+)", Pattern.CASE_INSENSITIVE),
            
            // NOTE: DO NOT use Naplatni broj as fallback!
            // Naplatni broj is the meter/account number - SAME for all bills from same address
            // It should NOT be used for duplicate detection
            
            // Telekom specific patterns (both Cyrillic and Latin)
            Pattern.compile("(?:Број\\s+рачуна)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),  // Cyrillic
            Pattern.compile("(?:Broj\\s+ra\u010duna)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),  // Latin
            Pattern.compile("(?:Ra\u010dun\\s+br)[:\\s\\.]+(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Рачун\\s+бр)[:\\s\\.]+(\\d+)", Pattern.CASE_INSENSITIVE),  // Cyrillic abbreviated
            Pattern.compile("(?:Faktura|Фактура)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),  // Both scripts
            
            // Serbian - Poziv na broj (very common on utility bills) - BOTH scripts
            Pattern.compile("(?:Poziv\\s+na\\s+broj|Poziv\\s+na\\s+broj\\s+plaćanja|Позив\\s+на\\s+број)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // Serbian - Broj fakture - BOTH scripts
            Pattern.compile("(?:Broj\\s+fakture|Faktura\\s+broj|Број\\s+фактуре|Фактура\\s+број)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // Serbian - Broj dokumenta - BOTH scripts
            Pattern.compile("(?:Broj\\s+dokumenta|Dokument\\s+broj|Број\\s+документа|Документ\\s+број)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // Account number (common on telecom bills)
            Pattern.compile("(?:Account\\s+number|Acc\\s+no)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // English - Invoice/Bill number
            Pattern.compile("(?:Invoice\\s+number|Bill\\s+number|Invoice\\s+no|Bill\\s+no|Invoice\\s+#)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // English - Document number
            Pattern.compile("(?:Document\\s+number|Doc\\s+number|Doc\\s+no)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // English - Reference number
            Pattern.compile("(?:Reference\\s+number|Ref\\s+number|Reference|Ref)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
            
            // Abbreviated forms - BOTH scripts
            Pattern.compile("(?:Ra\u010d\\.\\s*br|Br\\.\\s*ra\u010duna|Fakt\\.\\s*br|Рач\\.\\s*бр|Бр\\.\\с*рачуна|Факт\\.\\s*бр)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE)
            
            // REMOVED: Generic fallbacks that were matching Naplatni broj instead of Račun broj:
            // - "(?:Račun|...|Bill)[^\d]*(\d{10,})" - too generic, matches ANY number after "Račun"
            // - "\b(\d{10,})\b" - matches ANY 10+ digit number
            // - "(?:ID|Id)[:\s]+(\d{10,})" - too generic
        )
        
        // Only search first 30% of text for standalone numbers to avoid picking up amounts
        val searchText = text.take((text.length * 0.3).toInt().coerceAtLeast(500))
        
        for ((index, pattern) in patterns.withIndex()) {
            val textToSearch = if (index == patterns.size - 1) searchText else text  // Last pattern only searches top
            val matcher = pattern.matcher(textToSearch)
            if (matcher.find()) {
                val rawNumber = matcher.group(1)?.trim()
                if (rawNumber != null) {
                    // Clean the number: remove dashes for storage (but keep for logging)
                    val cleanNumber = rawNumber.replace("-", "")
                    
                    if (cleanNumber.length >= 7) {  // Minimum 7 digits (Telekom has 7-digit invoices)
                        android.util.Log.d("ReceiptParser", "✅ Found invoice number: $rawNumber (cleaned: $cleanNumber) using pattern #$index")
                        android.util.Log.d("ReceiptParser", "Pattern: ${pattern.pattern().take(80)}")
                        return cleanNumber  // Return cleaned number without dashes
                    } else {
                        android.util.Log.w("ReceiptParser", "⚠️ Found number $rawNumber but too short (${cleanNumber.length} digits, need 7+)")
                    }
                }
            }
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
            "EPS DISTRIBUCIJA", "EPS SNABDEVANJE", "ELEKTROPRIVREDA SRBIJE", "JKP INFOSTAN", 
            "INFOSTAN TEHNOLOGIJE", "TELEKOM SRBIJA", "YETTEL", "A1 SRBIJA", "SBB", "ORION TELEKOM",
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
        
        // Ako tekst sadrži mala slova, verovatno je već normalizovan
        if (text.any { it.isLowerCase() }) return text
        
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
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
                    val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                    if (date != null) {
                        // Validate year to avoid noise (e.g. 2000-2030)
                        val year = Integer.parseInt(SimpleDateFormat("yyyy", Locale.getDefault()).format(date))
                        if (year in 2020..2030) {
                            val line = findLineFor(matcher.start())
                            dateCandidates.add(date to line)
                        }
                    }
                } catch (e: Exception) {
                    // Continue
                }
            }
        }
        
        if (dateCandidates.isEmpty()) return null
        
        // Priority Keywords (Latin & Cyrillic)
        val keywords = listOf(
            "DATUM", "DANA", "IZDAT", "PROMET", "MESTO", "BEOGRAD", 
            "ДАТУМ", "ДАНА", "ИЗДАТ", "ПРОМЕТ", "МЕСТО", "БЕОГРАД" // often "Beograd, 30.11.2025"
        )
        
        // 1. Look for date on same line as keyword
        for ((date, line) in dateCandidates) {
            val upperLine = line.uppercase()
            if (keywords.any { upperLine.contains(it) }) {
                android.util.Log.d("ReceiptParser", "✅ Found Priority Date: $date in line: $line")
                return date
            }
        }
        
        // 2. Fallback: Return the FIRST detected valid date (top of document usually)
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
                val dateStr = matcher.group(1).replace(" ", "")
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
}


