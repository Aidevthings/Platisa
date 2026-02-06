package com.platisa.app.core.data.parser

import com.platisa.app.core.domain.model.DiscountRow
import com.platisa.app.core.domain.model.EpsData
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EpsParser {

    fun parse(text: String): EpsData? {
        android.util.Log.d("EpsParser", "=== POČETAK PARSIRANJA EPS RAČUNA ===")
        android.util.Log.d("EpsParser", "Dužina teksta: ${text.length} karaktera")
        
        // Normalize OCR text to fix common Latin↔Cyrillic confusions
        val normalizedText = normalizeOcrText(text)
        
        val vt = extractConsumptionVt(normalizedText)
        val nt = extractConsumptionNt(normalizedText)
        
        val totalKwh = if (vt != null || nt != null) {
            (vt ?: BigDecimal.ZERO).add(nt ?: BigDecimal.ZERO)
        } else {
            extractTotalConsumption(normalizedText)
        }

        // Smart Parsing: Extract monetary values
        val currentAmount = extractCurrentMonthAmount(normalizedText)
        val previousDebt = extractPreviousDebt(normalizedText)
        val totalPayAmount = extractTotalPayAmount(normalizedText)
        
        android.util.Log.d("EpsParser", "=== SMART PARSING RESULT ===")
        android.util.Log.d("EpsParser", "Current Month Amount: $currentAmount")
        android.util.Log.d("EpsParser", "Previous Debt: $previousDebt")
        android.util.Log.d("EpsParser", "Total Pay Amount: $totalPayAmount")
        
        // Extract payment ID fields
        val naplatniBroj = extractNaplatniBroj(normalizedText)
        var invoiceNumber = extractInvoiceNumber(normalizedText)
        
        // CRITICAL CHECK: Naplatni broj vs Invoice Number Collision
        // Sometimes OCR finds "Naplatni broj" digits when looking for "Račun broj" if the layout is tricky.
        if (invoiceNumber != null && naplatniBroj != null) {
            val cleanInvoice = invoiceNumber.replace(Regex("[^0-9]"), "")
            val cleanNaplatni = naplatniBroj.replace(Regex("[^0-9]"), "")
            
            if (cleanInvoice == cleanNaplatni) {
                android.util.Log.w("EpsParser", "⚠️ REJECTING Invoice Number ($invoiceNumber) because it matches Naplatni Broj!")
                invoiceNumber = null
            }
        }

        val isStorno = detectStorno(normalizedText)
        val isCorrection = detectCorrection(normalizedText)
        if (isCorrection) {
             android.util.Log.w("EpsParser", "🏁 BILL TYPE: KORIGOVAN (CORRECTED) - This will supersede other bills!")
        }
        val dueDate = extractDueDate(normalizedText)
        
        android.util.Log.d("EpsParser", "=== IZVUČENI PODACI ZA DUPLIKAT DETEKCIJU ===")
        android.util.Log.d("EpsParser", "Račun broj: $invoiceNumber")
        android.util.Log.d("EpsParser", "STORNO: $isStorno")
        
        // KRITIČNO: Izvuci period obračuna (Период обрачуна) - potrebno za PaymentId
        // Pokušaj oba načina i koristi prvi koji uspe
        val (periodStartA, periodEndA) = extractPeriodDates(normalizedText)
        val billingPeriodB = extractBillingPeriod(normalizedText)
        
        android.util.Log.d("EpsParser", "extractPeriodDates() -> start=$periodStartA, end=$periodEndA")
        android.util.Log.d("EpsParser", "extractBillingPeriod() -> ${billingPeriodB?.first} - ${billingPeriodB?.second}")
        
        // Koristi uspešnu ekstrakciju
        val periodStart = periodStartA ?: billingPeriodB?.first
        val periodEnd = periodEndA ?: billingPeriodB?.second
        
        android.util.Log.d("EpsParser", "Finalni period: $periodStart - $periodEnd")
        
        // Header date kao fallback (npr. "ОКТОБАР 2025")
        val headerDate = extractMonthFromHeader(normalizedText)
        android.util.Log.d("EpsParser", "Header datum (fallback): $headerDate")
        
        // Finalni datum za Receipt.date
        val finalDate = periodEnd ?: headerDate ?: dueDate
        android.util.Log.d("EpsParser", "Finalni datum za račun: $finalDate")
        
        // KRITIČNO: Kreiraj PaymentId koristeći Račun broj (Invoice Number) + Period + Iznos
        // Naplatni broj se više NE KORISTI za ID jer je statičan.
        val paymentId = EpsData.createPaymentId(invoiceNumber, periodStart, periodEnd ?: billingPeriodB?.second, totalPayAmount)
        
        android.util.Log.d("EpsParser", "=== KREIRAN PAYMENT ID ===")
        android.util.Log.d("EpsParser", "PaymentId: $paymentId")
        
        if (paymentId == null) {
            android.util.Log.w("EpsParser", "⚠️ PaymentId je NULL! Nedostaje InvoiceNumber, Period ili Amount!")
            android.util.Log.w("EpsParser", "   - invoiceNumber: $invoiceNumber")
            android.util.Log.w("EpsParser", "   - periodStart: $periodStart")
            android.util.Log.w("EpsParser", "   - amount: $totalPayAmount")
        }
        
        val (recipientName, recipientAddress) = extractRecipientInfo()
        
        return EpsData(
            edNumber = extractEdNumber(normalizedText),
            billingPeriod = extractPeriod(normalizedText),
            consumptionVt = vt,
            consumptionNt = nt,
            totalConsumption = totalKwh,
            naplatniBroj = naplatniBroj,
            invoiceNumber = invoiceNumber,
            periodStart = periodStart,
            periodEnd = finalDate,
            isStorno = isStorno,
            isCorrection = isCorrection,
            dueDate = dueDate,
            paymentId = paymentId,
            recipientName = recipientName,
            recipientAddress = recipientAddress,
            currentMonthAmount = currentAmount,
            previousDebtAmount = previousDebt,
            totalPayAmount = totalPayAmount,
            electricityBaseCost = null, // No longer scanning page 2
            discountDeadline = null,
            discountThresholdAmount = null,
            discountThresholdMessage = null
        )
    }

    /**
     * Extracts "Zaduženje za obračunski period" (Charge for billing period).
     * This represents the TRUE monthly cost without previous debts.
     */
    private fun extractCurrentMonthAmount(text: String): BigDecimal? {
        val patterns = listOf(
            // Cyrillic
            Regex("""Задужење\s+за\s+обрачунски\s+период[:\s]+([\d.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""Zaduzenje\s+za\s+obracunski\s+period[:\s]+([\d.,]+)""", RegexOption.IGNORE_CASE),
            // Fallback: Look for "Zaduženje" near "obračunski period" on lines
            Regex("""(?:Задужење|Zaduzenje).{0,50}?([\d.,]+)""", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null && amount > BigDecimal("100")) return amount
            }
        }
        return null
    }

    /**
     * Extracts "Dug iz prethodnog perioda" (Debt from previous period).
     */
    private fun extractPreviousDebt(text: String): BigDecimal? {
        val patterns = listOf(
            // --- ROBUST FLEXIBLE PATTERNS ---
            // 1. "Dug" ... "prethod" ... [amount]
            // Matches: "Dug iz prethodnog perioda" (Latin/Cyrillic mixed supported)
            Regex("""(Дуг|Dug).*?(prethod|претход).*?[:\s]+([\d.,]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            
            // 2. "Prethodni" ... "dug" ... [amount]
            Regex("""(Претходни|Prethodni).*?(дуг|dug)[:\s]+([\d.,]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

            // 3. "Dug" ... "zakljucno" ... [amount]
            Regex("""(Дуг|Dug).*?(zaklju[cč]no|закључно).*?[:\s]+([\d.,]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

            // 4. "Ukupan" ... "dug" ... [amount]
            Regex("""(Укупан|Ukupan).*?(дуг|dug)[:\s]+([\d.,]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

            // 5. Fallback: Isolated "Dug"
            Regex("""\b(Дуг|Dug)\b[^:\n]{0,15}[:\s]\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                // Use last() because regexes have variable number of capturing groups, 
                // but the amount is always the last specific group.
                val amount = parseAmount(match.groupValues.last())
                if (amount != null) return amount
            }
        }
        return null
    }

    /**
     * Extracts "ZA UPLATU" amount (Total Pay Amount).
     * This is the final amount to be paid, including all debts and discounts.
     */
    private fun extractTotalPayAmount(text: String): BigDecimal? {
        val patterns = listOf(
            // "ZA UPLATU ZA ELEKTRIČNU ENERGIJU (A+B) ... 20.571,95"
            Regex("""(?:ZA\s+UPLATU|ЗА\s+УПЛАТУ)[^kK\d]{0,100}?([\d.,]+)(?:\s*din|\s*дин|\s*RSD|\s*РСД)?""", RegexOption.IGNORE_CASE),
            // "UKUPNO ZA UPLATU ... "
            Regex("""(?:UKUPNO\s+ZA\s+UPLATU|УКУПНО\s+ЗА\s+УПЛАТУ)[^kK\d]{0,100}?([\d.,]+)""", RegexOption.IGNORE_CASE),
            // Fallback: Just "ZA UPLATU" followed by amount
            Regex("""(?:ZA\s+UPLATU|ЗА\s+УПЛАТУ)[:\s]+([\d.,]+)""", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                // Basic validation: Amount should be positive and reasonable
                if (amount != null && amount > BigDecimal.ZERO) return amount
            }
        }
        return null
    }


    /**
     * Normalize OCR text by fixing common Latin↔Cyrillic character confusions.
     */
    private fun normalizeOcrText(text: String): String {
        var normalized = text
        
        val replacements = mapOf(
            "Ynnara no payHy" to "Уплата на рачун",
            "Ynara no payHy" to "Уплата на рачун",
            "Ynnara no pavHy" to "Уплата на рачун",
            "PRIVALAC" to "PRIMALAC",
            "ADRESA PRIMALOCA" to "ADRESA PRIMAOCA",
            "POrAYA" to "РОГАЧА",
            "PORAYA" to "РОГАЧА",
            "POrA4A" to "РОГАЧА",
            "MVIOCABA BNAJMHA" to "МИЛОСАВА ВЛАЈИЋА",
            "MVIOCABA BNAJMlHA" to "МИЛОСАВА ВЛАЈИЋА",
            "MV1OCABA BNAJMHA" to "МИЛОСАВА ВЛАЈИЋА",
            "MILOCABA BNAJMHA" to "МИЛОСАВА ВЛАЈИЋА"
        )
        
        for ((garbled, correct) in replacements) {
            normalized = normalized.replace(garbled, correct, ignoreCase = true)
        }
        
        val lines = normalized.lines()
        val normalizedLines = lines.map { line ->
            if (line.any { it in 'А'..'я' } || (line.length > 3 && line == line.uppercase())) {
                normalizeCharacters(line)
            } else {
                line
            }
        }
        
        return normalizedLines.joinToString("\n")
    }

    private fun normalizeCharacters(line: String): String {
        val hasCyrillic = line.any { it in 'А'..'я' }
        val hasLatin = line.any { it in 'A'..'Z' || it in 'a'..'z' }
        
        if (!hasCyrillic || !hasLatin) {
            return line
        }
        
        val charMap = mapOf(
            'A' to 'А', 'a' to 'а',
            'B' to 'В',
            'C' to 'С', 'c' to 'с',
            'E' to 'Е', 'e' to 'е',
            'H' to 'Н',
            'K' to 'К', 'k' to 'к',
            'M' to 'М', 'm' to 'м',
            'O' to 'О', 'o' to 'о',
            'P' to 'Р', 'p' to 'р',
            'T' to 'Т',
            'X' to 'Х', 'x' to 'х',
            'Y' to 'У', 'y' to 'у'
        )
        
        return line.map { char ->
        charMap[char] ?: char
        }.joinToString("")
    }

    private fun extractRecipientInfo(): Pair<String?, String?> {
        // SAFETY: Return null for address to prevent hijacking non-EPS bills (Infostan).
        // The main ReceiptParser should handle address extraction.
        return null to null
    }

    private fun findNameAbove(lines: List<String>, addressIndex: Int): String? {
        // Search up to 3 lines above for a valid name
        for (j in (addressIndex - 1) downTo (addressIndex - 3).coerceAtLeast(0)) {
            val candidate = lines[j]
            if (isValidName(candidate)) return candidate
        }
        return null
    }

    private fun isValidAddress(text: String): Boolean {
        if (text.length < 5) return false
        val upper = text.uppercase()
        // Reject money lines
        if (upper.contains("RSD") || upper.contains("DIN") || upper.contains("РСД") || upper.contains("ДИН") || upper.contains("EUR") || upper.contains("€")) return false
        if (text.contains(Regex("\\d+,\\d{2}"))) return false // No decimals like ,00
        // Reject purely numeric
        if (text.matches(Regex("^[\\d.,\\s-]+$"))) return false
        
        // Don't reject for metadata anymore, we just clean it up
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
            val regex = Regex("(?i)$keyword[:\\s]+[^,]*", RegexOption.IGNORE_CASE)
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

    private fun isValidName(text: String): Boolean {
        if (text.length < 3) return false
        val upper = text.uppercase()
        val forbidden = listOf("RSD", "DIN", "РСД", "ДИН", "ADRESA", "ULICA", "OBRAČUN", "RAČUN")
        if (forbidden.any { upper.contains(it) }) return false
        if (text.matches(Regex(".*\\d{3,}.*"))) return false // Names don't usually have long numbers
        return true
    }

    /**
     * Detektuje da li je račun STORNO.
     */
    /**
     * Detektuje da li je račun STORNO.
     * Use strict word boundaries to avoid false positives (e.g. "Prostorno", "Istorija").
     */
    private fun detectStorno(text: String): Boolean {
        // STRICTER PATTERNS: Must be whole word "STORNO" or "СТОРНО"
        // Ideally, it should be associated with "Račun", "Tip", "Dokument"
        
        val strictPatterns = listOf(
            Regex("""\bСТОРНО\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSTORNO\b""", RegexOption.IGNORE_CASE),
            Regex("""-\s*СТОРНО""", RegexOption.IGNORE_CASE), // Often appears as line item " - STORNO"
            Regex("""-\s*STORNO""", RegexOption.IGNORE_CASE),
            Regex("""Tip\s+računa\s*:\s*Storno""", RegexOption.IGNORE_CASE),
            Regex("""Tip\s+računa\s*:\s*Сторно""", RegexOption.IGNORE_CASE)
        )
        
        // Anti-patterns: Contexts where "Storno" might appear but NOT mean the bill is Storno
        // (Currently none specific, but keeping logic structure ready)

        val isStorno = strictPatterns.any { it.containsMatchIn(text) }
        
        if (isStorno) {
            android.util.Log.d("EpsParser", "⚠️ STORNO DETECTED (Strict Check)")
        }
        
        return isStorno
    }

    /**
     * Detektuje da li je račun KORIGOVAN (ispravljen).
     */
    private fun detectCorrection(text: String): Boolean {
        val patterns = listOf(
            Regex("""\bКОРИГОВАН\b""", RegexOption.IGNORE_CASE),
            Regex("""\bKORIGOVAN\b""", RegexOption.IGNORE_CASE),
            Regex("""КОРИГОВАН\s+\d{4}""", RegexOption.IGNORE_CASE),
            Regex("""KORIGOVAN\s+\d{4}""", RegexOption.IGNORE_CASE),
            Regex("""(?:JANUAR|FEBRUAR|MART|APRIL|MAJ|JUN|JUL|AVGUST|SEPTEMBAR|OKTOBAR|NOVEMBAR|DECEMBAR).{0,50}КОРИГОВАН""", RegexOption.IGNORE_CASE),
            Regex("""(?:JANUAR|FEBRUAR|MART|APRIL|MAJ|JUN|JUL|AVGUST|SEPTEMBAR|OKTOBAR|NOVEMBAR|DECEMBAR).{0,50}KORIGOVAN""", RegexOption.IGNORE_CASE),
            Regex("""(?:ЈАНУАР|ФЕБРУАР|МАРТ|АПРИЛ|МАЈ|ЈУН|ЈУЛ|АВГУСТ|СЕПТЕМБАР|ОКТОБАР|НОВЕМБАР|ДЕЦЕМБАР).{0,50}КОРИГОВАН""", RegexOption.IGNORE_CASE)
        )
        
        val isCorrection = patterns.any { it.containsMatchIn(text) }
        if (isCorrection) {
            android.util.Log.d("EpsParser", "📝 CORRECTION DETECTED (KORIGOVAN)")
        }
        return isCorrection
    }

    /**
     * Izvlači naplatni broj iz EPS računa.
     */
    private fun extractNaplatniBroj(text: String): String? {
        val patterns = listOf(
            // Ćirilica
            Regex("""Наплатни\s+број[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Наплатни\s*број[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            // Latinica
            Regex("""Naplatni\s+broj[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Naplatni\s*broj[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            // Bez razmaka
            Regex("""[Nn]aplatni.{0,20}(\d{10,})"""),
            Regex("""[Нн]аплатни.{0,20}(\d{10,})""")
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val result = match.groupValues.getOrNull(1)
                android.util.Log.d("EpsParser", "Pronađen naplatni broj: $result (pattern: ${regex.pattern.take(30)})")
                return result
            }
        }
        android.util.Log.w("EpsParser", "Naplatni broj NIJE pronađen!")
        return null
    }

    /**
     * Izvlači račun broj (Invoice number).
     */
    private fun extractInvoiceNumber(text: String): String? {
        val patterns = listOf(
            Regex("""(?:Рачун|Pauun|Payyn|Pa4yn|Pacun|Ra[čc]un)[\s.]{0,10}?(?:број|6poj|broj)[:\s]*(\d{5,30})""", RegexOption.IGNORE_CASE),
            Regex("""(?:Број|Broj)[\s.]{0,10}?(?:рачуна|racuna|ra[čc]una)[:\s]*(\d{5,30})""", RegexOption.IGNORE_CASE),
            Regex("""(?:Račun|Racun|Рачун)[\s.]{0,5}br[:\s.]*(\d{5,30})""", RegexOption.IGNORE_CASE)
        )
        
        val lines = text.lines()
        for (i in lines.indices) {
            val line = lines[i]
            val isInvoiceLabel = line.contains("Račun", ignoreCase = true) || 
                                line.contains("Рачун", ignoreCase = true) ||
                                line.contains("Racun", ignoreCase = true) ||
                                line.contains("Pauun", ignoreCase = true)
                               
            if (isInvoiceLabel) {
                // 1. Try regex on current line
                for (regex in patterns) {
                    val match = regex.find(line)
                    if (match != null) {
                        val result = validateInvoice(match.groupValues.last())
                        if (result != null) return result
                    }
                }
                
                // 2. Try next line for pure digits if current line has label but no value
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1].trim()
                    val digitsMatch = Regex("""\b(\d{5,30})\b""").find(nextLine)
                    if (digitsMatch != null) {
                        val result = validateInvoice(digitsMatch.groupValues[1])
                        if (result != null) return result
                    }
                }
            }
        }

        // Final broad fallback
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val result = validateInvoice(match.groupValues.last())
                if (result != null) return result
            }
        }
        
        android.util.Log.d("EpsParser", "Broj računa nije pronađen")
        return null
    }

    private fun validateInvoice(candidate: String): String? {
        val cleanDigits = candidate.replace(Regex("[^0-9]"), "")
        if (cleanDigits.length < 5) return null
        val numInt = cleanDigits.toIntOrNull()
        if (numInt != null && numInt in 2020..2030) return null
        android.util.Log.d("EpsParser", "Pronađen i validiran broj računa: $cleanDigits")
        return cleanDigits
    }

    /**
     * Izvlači datume perioda obračuna (Период обрачуна / Period obračuna).
     * Format: DD.MM.YYYY - DD.MM.YYYY
     */
    private fun extractPeriodDates(text: String): Pair<Date?, Date?> {
        // Pattern koji hvata: "05.10.2025 - 01.11.2025" ili "05.10.2025. - 01.11.2025."
        val patterns = listOf(
            // Sa opcionalnim tačkama na kraju godina
            Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})\.?\s*[-–—]\s*(\d{1,2})\.(\d{1,2})\.(\d{4})\.?"""),
            // Eksplicitno traženje posle "Period obračuna" ili "Период обрачуна"
            Regex("""(?:Period\s+obra[čc]una|Период\s+обрачуна)[:\s]*(\d{1,2})\.(\d{1,2})\.(\d{4})\.?\s*[-–—]\s*(\d{1,2})\.(\d{1,2})\.(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues
                    val startDay = groups[1].padStart(2, '0')
                    val startMonth = groups[2].padStart(2, '0')
                    val startYear = groups[3]
                    val endDay = groups[4].padStart(2, '0')
                    val endMonth = groups[5].padStart(2, '0')
                    val endYear = groups[6]
                    
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val periodStart = dateFormat.parse("$startDay.$startMonth.$startYear")
                    val periodEnd = dateFormat.parse("$endDay.$endMonth.$endYear")
                    
                    android.util.Log.d("EpsParser", "extractPeriodDates USPEO: $periodStart - $periodEnd")
                    return Pair(periodStart, periodEnd)
                } catch (e: Exception) {
                    android.util.Log.e("EpsParser", "Greška pri parsiranju datuma perioda", e)
                }
            }
        }
        
        android.util.Log.w("EpsParser", "extractPeriodDates NIJE pronašao period!")
        return Pair(null, null)
    }

    /**
     * Alternativna ekstrakcija billing perioda sa širim opsegom.
     */
    private fun extractBillingPeriod(text: String): Pair<Date, Date>? {
        val patterns = listOf(
            // Dozvoli više razmaka i noise između labele i datuma
            Regex("""(?:Period\s+obra[čc]una|Период\s+обрачуна)(?:.|\n){0,50}?(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})\.?\s*[-–—]\s*(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues
                    val d1Str = "${groups[1].padStart(2, '0')}.${groups[2].padStart(2, '0')}.${groups[3]}"
                    val d2Str = "${groups[4].padStart(2, '0')}.${groups[5].padStart(2, '0')}.${groups[6]}"
                    
                    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val d1 = format.parse(d1Str)
                    val d2 = format.parse(d2Str)
                    
                    if (d1 != null && d2 != null) {
                        android.util.Log.d("EpsParser", "extractBillingPeriod USPEO: $d1 - $d2")
                        return Pair(d1, d2)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EpsParser", "Greška u extractBillingPeriod", e)
                }
            }
        }
        
        android.util.Log.d("EpsParser", "extractBillingPeriod nije pronašao period")
        return null
    }

    private fun extractPeriod(text: String): String? {
        val regex = Regex("""(\d{2}\.\d{2}\.\d{4})\.?\s*[-–—]\s*(\d{2}\.\d{2}\.\d{4})""")
        val match = regex.find(text)
        return if (match != null) {
            "${match.groupValues[1]} - ${match.groupValues[2]}"
        } else {
            null
        }
    }

    private fun extractEdNumber(text: String): String? {
        val patterns = listOf(
            Regex("""ED\s*broj[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""ЕД\s*број[:\s]*(\d+)""", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                return match.groupValues.getOrNull(1)
            }
        }
        return null
    }

    private fun extractConsumptionVt(text: String): BigDecimal? {
        val vtVariations = listOf(
            "VT", "Viša", "Visa", "Виша", "ВТ",
            "V1sa", "Vlsa", "VIsa", "V5a",
            "Bnwa", "Bula", "BIIIa", "Bwa", "BnIIIa", "Buca",
            "BT"
        )
        
        val variationPattern = vtVariations.joinToString("|") { Regex.escape(it) }
        
        val patterns = listOf(
            Regex("""($variationPattern)[^kK\d]{0,200}?(?<![.\d])([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        for (regex in patterns) {
            val matches = regex.findAll(text)
            for (match in matches) {
                val fullMatchText = match.value
                val groupVal = match.groupValues.last()
                
                if (fullMatchText.contains("Ukupno", ignoreCase = true) || 
                    fullMatchText.contains("Svega", ignoreCase = true) ||
                    fullMatchText.contains("Total", ignoreCase = true)) {
                    continue 
                }

                val result = parseAmount(groupVal)
                if (result != null && result > BigDecimal.ZERO) {
                    return result
                }
            }
        }
        return null
    }

    private fun extractConsumptionNt(text: String): BigDecimal? {
        val ntVariations = listOf(
            "NT", "Niža", "Niza", "Нижа", "НТ",
            "N1za", "Nlza", "NIsa", "N5a",
            "Hnwa", "Hula", "Huca", "Hwa", "Hu3a",
            "HT"
        )
        
        val variationPattern = ntVariations.joinToString("|") { Regex.escape(it) }
        
        val patterns = listOf(
             Regex("""($variationPattern)[^kK\d]{0,200}?(?<![.\d])([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        for (regex in patterns) {
            val matches = regex.findAll(text)
            for (match in matches) {
                val fullMatchText = match.value
                val groupVal = match.groupValues.last()
                
                if (fullMatchText.contains("Ukupno", ignoreCase = true) || 
                    fullMatchText.contains("Svega", ignoreCase = true) ||
                    fullMatchText.contains("Total", ignoreCase = true)) {
                    continue 
                }

                val result = parseAmount(groupVal)
                if (result != null && result > BigDecimal.ZERO) {
                    return result
                }
            }
        }
        return null
    }

    private fun extractTotalConsumption(text: String): BigDecimal? {
        val priorityPatterns = listOf(
            Regex("""ПОТРОШЊА\s+У\s+ОБРАЧУНСКОМ\s+ПЕРИОДУ[^kK\d]{0,200}?([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""POTROSNJA\s+U\s+OBRACUNSKOM\s+PERIODU[^kK\d]{0,200}?([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""Potrošnja\s+u\s+obračunskom\s+periodu[^kK\d]{0,200}?([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        for (regex in priorityPatterns) {
            val match = regex.find(text)
            if (match != null) {
                val groupVal = match.groupValues.last()
                val result = parseAmount(groupVal)
                if (result != null && result > BigDecimal.ZERO) {
                    return result
                }
            }
        }
        
        val totalVariations = listOf(
            "Ukupno", "Svega", "Total", "Ostvarena", "Potrosnja", "Потрошња", "Укупно", "Свега"
        )
        val variationPattern = totalVariations.joinToString("|") { Regex.escape(it) }
        
        val patterns = listOf(
             Regex("""($variationPattern)[^kK\d]{0,200}?(?<![.\d])([\d.,]+)\s*kWh""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        for (regex in patterns) {
            val matches = regex.findAll(text)
            for (match in matches) {
                 val fullMatchText = match.value
                 if (fullMatchText.contains("godinu", ignoreCase = true) || 
                     fullMatchText.contains("year", ignoreCase = true) ||
                     fullMatchText.contains("prose", ignoreCase = true) ||
                     fullMatchText.contains("godisn", ignoreCase = true) ||
                     fullMatchText.contains("yearl", ignoreCase = true)) {
                     continue
                 }
                 
                 val groupVal = match.groupValues.last()
                 val result = parseAmount(groupVal)
                 if (result != null && result > BigDecimal.ZERO) {
                     return result
                 }
            }
        }
        
        val looseKwh = Regex("""(?<![.\d])\b(\d{2,}[.,]?\d*)\s*kWh""", setOf(RegexOption.IGNORE_CASE))
        val looseMatches = looseKwh.findAll(text)
        
        for (match in looseMatches) {
            val valStr = match.groupValues[1]
            val matchStart = match.range.first
            val startSearch = (matchStart - 60).coerceAtLeast(0)
            val context = text.substring(startSearch, matchStart).lowercase()
            
            if (context.contains("godinu") || context.contains("year") || 
                context.contains("prose") || context.contains("godisn")) {
                continue
            }
            
            val value = parseAmount(valStr)
            if (value != null && value > BigDecimal.valueOf(10) && value < BigDecimal.valueOf(10000)) { 
                 return value
            }
        }

        return null
    }

    /**
     * Izvlači rok plaćanja iz računa.
     */
    private fun extractDueDate(text: String): Date? {
        val patterns = listOf(
            Regex("""(?:Rok\s+za\s+pla[ćc]anje|Рок\s+за\s+плаћање|Rok\s+pla[ćc]anja|Рок\s+плаћања).{0,200}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""(?:[PpRr][oO0][kK]\s+[zZ3\u0437]a\s+\w+).{0,200}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""[PpRr][oO0][kK].{0,50}[:\s]\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""(?:Rok\s+dospelosti|Рок\s+доспелости|Datum\s+dospelosti|Датум\s+доспелости).{0,200}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""(?:Datum\s+valute|Valuta|Датум\s+валуте|Валута).{0,200}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""(?:Platiti\s+do|Uplatiti\s+do|Платити\s+до|Уплатити\s+до|Platiti\s+najkasnije\s+do).{0,200}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("""Datum\s+pla[ćc]anja[:\s].{0,100}?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                try {
                    val dateStr = match.groupValues[1] 
                    val cleanDate = dateStr.replace("/", ".").replace("-", ".").replace(" ", "")
                    
                    val formats = listOf("dd.MM.yyyy", "dd.MM.yy", "d.M.yyyy")
                    for (fmt in formats) {
                        try {
                            val dateFormat = SimpleDateFormat(fmt, Locale.getDefault())
                            return dateFormat.parse(cleanDate)
                        } catch (e: Exception) {
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EpsParser", "Greška pri parsiranju roka plaćanja", e)
                }
            }
        }
        return null
    }

    private fun parseAmount(amountString: String?): BigDecimal? {
        if (amountString == null) return null
        val cleanString = amountString
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return try {
            BigDecimal(cleanString)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun extractMonthFromHeader(text: String): Date? {
        val monthNames = mapOf(
            "JANUAR" to 0, "JAN" to 0, "ЈАНУАР" to 0, "ЈАН" to 0,
            "FEBRUAR" to 1, "FEB" to 1, "ФЕБРУАР" to 1, "ФЕБ" to 1,
            "MART" to 2, "MAR" to 2, "МАРТ" to 2, "МАР" to 2,
            "APRIL" to 3, "APR" to 3, "АПРИЛ" to 3, "АПР" to 3,
            "MAJ" to 4, "MAY" to 4, "МАЈ" to 4,
            "JUN" to 5, "JUN" to 5, "ЈУН" to 5,
            "JUL" to 6, "JUL" to 6, "ЈУЛ" to 6,
            "AVGUST" to 7, "AVG" to 7, "АВГУСТ" to 7, "АВГ" to 7,
            "SEPTEMBAR" to 8, "SEP" to 8, "СЕПТЕМБАР" to 8, "СЕП" to 8,
            "OKTOBAR" to 9, "OCT" to 9, "ОКТОБАР" to 9, "ОКТ" to 9,
            "NOVEMBAR" to 10, "NOV" to 10, "НОВЕМБАР" to 10, "НОВ" to 10,
            "DECEMBAR" to 11, "DEC" to 11, "ДЕЦЕМБАР" to 11, "ДЕЦ" to 11
        )
        
         val yearRegex = Regex("""(JANUAR|JAN|ЈАНУАР|ЈАН|FEBRUAR|FEB|ФЕБРУАР|ФЕБ|MART|MAR|МАРТ|МАР|APRIL|APR|АПРИЛ|АПР|MAJ|MAY|МАЈ|JUN|ЈУН|JUL|ЈУЛ|AVGUST|AVG|АВГУСТ|АВГ|SEPTEMBAR|SEP|СЕПТЕМБАР|СЕП|OKTOBAR|OCT|ОКТОБАР|ОКТ|NOVEMBAR|NOV|НОВЕМБАР|НОВ|DECEMBAR|DEC|ДЕЦЕМБАР|ДЕЦ)\s+[-—]?\s*(\d{4})""", setOf(RegexOption.IGNORE_CASE))

        val match = yearRegex.find(text)
        if (match != null) {
            val monthStr = match.groupValues[1].uppercase()
            val yearStr = match.groupValues[2]
            val monthIndex = monthNames[monthStr] ?: return null
            
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.YEAR, yearStr.toInt())
            calendar.set(java.util.Calendar.MONTH, monthIndex)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            
            calendar.add(java.util.Calendar.MONTH, 1) 
            return calendar.time
        }
        return null
    }


    private fun formatAmount(amount: BigDecimal): String {
        return String.format("%,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".")
    }

}
