package com.example.platisa.core.data.parser

import com.example.platisa.core.domain.model.EpsData
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EpsParser {

    fun parse(text: String): EpsData {
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
        
        // Extract payment ID fields
        val naplatniBroj = extractNaplatniBroj(normalizedText)
        val invoiceNumber = extractInvoiceNumber(normalizedText)
        val isStorno = detectStorno(normalizedText)
        val dueDate = extractDueDate(normalizedText)
        
        android.util.Log.d("EpsParser", "=== IZVUČENI PODACI ZA DUPLIKAT DETEKCIJU ===")
        android.util.Log.d("EpsParser", "Naplatni broj: $naplatniBroj")
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
        
        // KRITIČNO: Kreiraj PaymentId sa ispravnim datumima
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd ?: billingPeriodB?.second)
        
        android.util.Log.d("EpsParser", "=== KREIRAN PAYMENT ID ===")
        android.util.Log.d("EpsParser", "PaymentId: $paymentId")
        
        if (paymentId == null) {
            android.util.Log.w("EpsParser", "⚠️ PaymentId je NULL! Duplikat detekcija neće raditi po periodu!")
            android.util.Log.w("EpsParser", "   - naplatniBroj: $naplatniBroj")
            android.util.Log.w("EpsParser", "   - periodStart: $periodStart")
            android.util.Log.w("EpsParser", "   - periodEnd: $periodEnd")
        }
        
        val (recipientName, recipientAddress) = extractRecipientInfo(normalizedText)

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
            dueDate = dueDate,
            paymentId = paymentId,
            recipientName = recipientName,
            recipientAddress = recipientAddress
        )
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

    private fun extractRecipientInfo(text: String): Pair<String?, String?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        val anchors = listOf("KUPAC:", "КОРИСНИК:", "Korisnik:", "PRIMALAC:", "ПОТРОШАЧ:", "Potrošač:", "PLATILAC:", "ПЛАТИЛАЦ:",
            "KYPAC:", "KORISNIK:", "PRIMILAC:", "PO PRIMALAC:", "KORlSNIK:", "KORISNlK:", "PLATILAC")
        for (i in lines.indices) {
            val line = lines[i]
            for (anchor in anchors) {
                if (line.contains(anchor, ignoreCase = true)) {
                    val name = line.substringAfter(anchor).trim()
                    
                    if (name.length < 3 && i < lines.size - 1) {
                         val nextLine = lines[i+1]
                         if (nextLine.length > 5) {
                             val address = if (i < lines.size - 2) lines[i + 2] else null
                             return com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(nextLine) to 
                                    com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(address)
                         }
                    }
                    
                    val address = if (i < lines.size - 1) lines[i + 1] else null
                    if (name.isNotEmpty()) {
                        return com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(name) to 
                               com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(address)
                    }
                }
            }
        }
        
        val forbiddenNamePhrases = listOf(
            "prigovore", "možete", "podneti", "reklamacije", "račun", "rok", "dana", "iznos", "obračun", "e-mail", "www", "http",
            "приговоре", "можете", "поднети", "рекламације", "рачун", "рок", "дана", "износ", "обрачун",
            "npurobope", "mokete", "noghetm",
            "broj", "bpoj", "brojila", "naplatni", "ed broj", "бpoj", "број", "5poj"
        )
        
        val searchLimit = (lines.size * 0.6).toInt().coerceAtLeast(10)
        val zipCityRegex = Regex(""".*?(\d{5})\s+([A-Za-zČĆŽŠĐčćžšđА-Яа-я]+(\s+[A-Za-zČĆŽŠĐčćžšđА-Яа-я]+){0,2}).*""")

        for (i in 0 until searchLimit.coerceAtMost(lines.size)) {
            val line = lines[i]
            
            val match = zipCityRegex.find(line)
            if (match != null) {
                if (i >= 2) {
                    val lineMinus1 = lines[i - 1]
                    val lineMinus2 = lines[i - 2]
                    
                    fun isValidName(text: String): Boolean {
                        if (text.length < 3) return false
                        val lower = text.lowercase()
                        if (forbiddenNamePhrases.any { lower.contains(it) }) return false
                        if (Regex("^[\\d./-]+$").matches(text)) return false
                        return true
                    }
                    
                    var recipientName: String? = null
                    var recipientAddress: String? = null
                    
                    if (i >= 3) {
                        val lineMinus3 = lines[i - 3]
                        if (isValidName(lineMinus3)) {
                            if (!forbiddenNamePhrases.any { lineMinus2.lowercase().contains(it) }) {
                                recipientName = lineMinus3
                                recipientAddress = "$lineMinus2, $lineMinus1, $line"
                            }
                        }
                    }
                    
                    if (recipientName == null) {
                         if (isValidName(lineMinus2)) {
                             recipientName = lineMinus2
                             recipientAddress = "$lineMinus1, $line"
                         }
                    }

                    if (recipientName != null) {
                         return com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(recipientName) to 
                                com.example.platisa.core.domain.parser.ReceiptParser.normalizeText(recipientAddress)
                    }
                }
            }
        }
        
        return null to null
    }

    /**
     * Detektuje da li je račun STORNO.
     */
    private fun detectStorno(text: String): Boolean {
        val stornoPatterns = listOf(
            Regex("""СТОРНО""", RegexOption.IGNORE_CASE),
            Regex("""STORNO""", RegexOption.IGNORE_CASE),
            Regex("""-\s*СТОРНО""", RegexOption.IGNORE_CASE),
            Regex("""-\s*STORNO""", RegexOption.IGNORE_CASE)
        )
        val isStorno = stornoPatterns.any { it.containsMatchIn(text) }
        android.util.Log.d("EpsParser", "STORNO detekcija: $isStorno")
        return isStorno
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
            Regex("""[Nn]aplatni.*?(\d{10,})"""),
            Regex("""[Нн]аплатни.*?(\d{10,})""")
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
            // Ćirilica
            Regex("""Рачун\s+број[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Број\s+рачуна[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            // Latinica
            Regex("""Račun\s+broj[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Racun\s+broj[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Broj\s+računa[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""Broj\s+racuna[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
        )
        
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val result = match.groupValues.getOrNull(1)
                android.util.Log.d("EpsParser", "Pronađen broj računa: $result")
                return result
            }
        }
        android.util.Log.d("EpsParser", "Broj računa nije pronađen")
        return null
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
}
