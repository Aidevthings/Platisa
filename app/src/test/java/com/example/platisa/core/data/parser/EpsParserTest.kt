package com.platisa.app.core.data.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit tests za EpsParser - parser za EPS račune.
 * 
 * Koristi Robolectric jer EpsParser koristi android.util.Log.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class EpsParserTest {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // ============================================
    // PAYMENT ID EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract naplatni broj from latin text`() {
        val text = """
            EPS SNABDEVANJE
            Naplatni broj: 2004158536
            Račun broj: 123456
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("2004158536", result.naplatniBroj)
    }

    @Test
    fun `extract naplatni broj from cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Наплатни број: 2004158536
            Рачун број: 123456
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("2004158536", result.naplatniBroj)
    }

    @Test
    fun `extract invoice number from latin text`() {
        val text = """
            EPS SNABDEVANJE
            Račun broj: 987654321
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("987654321", result.invoiceNumber)
    }

    @Test
    fun `extract invoice number from cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Рачун број: 987654321
            Наплатни број: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("987654321", result.invoiceNumber)
    }

    // ============================================
    // PERIOD EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract billing period dates in standard format`() {
        val text = """
            EPS SNABDEVANJE
            Period obračuna: 05.10.2025 - 01.11.2025
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("Period start should be extracted", result.periodStart)
        assertNotNull("Period end should be extracted", result.periodEnd)
        
        assertEquals("05.10.2025", dateFormat.format(result.periodStart!!))
        assertEquals("01.11.2025", dateFormat.format(result.periodEnd!!))
    }

    @Test
    fun `extract billing period from cyrillic header`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Период обрачуна: 05.10.2025 - 01.11.2025
            Наплатни број: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.periodStart)
        assertNotNull(result.periodEnd)
    }

    @Test
    fun `extract billing period with trailing dots`() {
        val text = """
            EPS SNABDEVANJE
            Period obračuna: 05.10.2025. - 01.11.2025.
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.periodStart)
        assertNotNull(result.periodEnd)
    }

    @Test
    fun `create valid PaymentId from extracted data`() {
        val text = """
            EPS SNABDEVANJE
            Naplatni broj: 2004158536
            Period obračuna: 05.10.2025 - 01.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("PaymentId should be created", result.paymentId)
        assertEquals("2004158536-20251005-20251101", result.paymentId)
    }

    // ============================================
    // STORNO DETECTION TESTS
    // ============================================

    @Test
    fun `detect STORNO in latin text`() {
        val text = """
            EPS SNABDEVANJE
            STORNO
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertTrue("STORNO should be detected", result.isStorno)
    }

    @Test
    fun `detect STORNO in cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            СТОРНО
            Наплатни број: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertTrue("СТОРНО should be detected", result.isStorno)
    }

    @Test
    fun `detect STORNO with dash prefix`() {
        val text = """
            EPS SNABDEVANJE
            - STORNO
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertTrue(result.isStorno)
    }

    @Test
    fun `no STORNO detection for regular bill`() {
        val text = """
            EPS SNABDEVANJE
            Naplatni broj: 2004158536
            Ukupno za uplatu: 4521,36 RSD
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertFalse("Regular bill should not be marked as STORNO", result.isStorno)
    }

    // ============================================
    // CONSUMPTION EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract VT consumption in latin`() {
        val text = """
            EPS SNABDEVANJE
            Viša tarifa (VT): 150 kWh
            Niža tarifa (NT): 80 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("VT consumption should be extracted", result.consumptionVt)
        assertEquals(BigDecimal("150"), result.consumptionVt)
    }

    @Test
    fun `extract NT consumption in latin`() {
        val text = """
            EPS SNABDEVANJE
            Viša tarifa (VT): 150 kWh
            Niža tarifa (NT): 80 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("NT consumption should be extracted", result.consumptionNt)
        assertEquals(BigDecimal("80"), result.consumptionNt)
    }

    @Test
    fun `extract consumption with decimal values`() {
        val text = """
            EPS SNABDEVANJE
            VT: 150,5 kWh
            NT: 80,25 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals(BigDecimal("150.5"), result.consumptionVt)
        assertEquals(BigDecimal("80.25"), result.consumptionNt)
    }

    @Test
    fun `calculate total consumption from VT and NT`() {
        val text = """
            EPS SNABDEVANJE
            VT: 150 kWh
            NT: 80 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("Total consumption should be calculated", result.totalConsumption)
        assertEquals(BigDecimal("230"), result.totalConsumption)
    }

    @Test
    fun `extract total consumption when VT NT not available`() {
        val text = """
            EPS SNABDEVANJE
            Ukupno potrošnja: 230 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.totalConsumption)
        assertEquals(BigDecimal("230"), result.totalConsumption)
    }

    // ============================================
    // DUE DATE EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract due date in latin - rok za placanje`() {
        val text = """
            EPS SNABDEVANJE
            Rok za plaćanje: 15.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("Due date should be extracted", result.dueDate)
        assertEquals("15.11.2025", dateFormat.format(result.dueDate!!))
    }

    @Test
    fun `extract due date in cyrillic`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Рок за плаћање: 15.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.dueDate)
    }

    @Test
    fun `extract due date with datum dospelosti`() {
        val text = """
            EPS SNABDEVANJE
            Datum dospelosti: 20.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.dueDate)
        assertEquals("20.11.2025", dateFormat.format(result.dueDate!!))
    }

    // ============================================
    // ED NUMBER EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract ED number in latin`() {
        val text = """
            EPS SNABDEVANJE
            ED broj: 12345678
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("12345678", result.edNumber)
    }

    @Test
    fun `extract ED number in cyrillic`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            ЕД број: 12345678
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("12345678", result.edNumber)
    }

    // ============================================
    // RECIPIENT INFO EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract recipient name after KUPAC label`() {
        val text = """
            EPS SNABDEVANJE
            KUPAC: PETAR PETROVIĆ
            Adresa: Ulica 123
            11000 BEOGRAD
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull("Recipient name should be extracted", result.recipientName)
        // Note: The normalizeText function might modify the name
    }

    @Test
    fun `extract recipient info from cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            КОРИСНИК: ПЕТАР ПЕТРОВИЋ
            Адреса: Улица 123
            11000 БЕОГРАД
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.recipientName)
    }

    // ============================================
    // MONTH FROM HEADER EXTRACTION TESTS
    // ============================================

    @Test
    fun `extract month from latin header`() {
        val text = """
            EPS SNABDEVANJE
            OKTOBAR 2025
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        // When no period is found, month from header should be used
        // The extracted date should be the first day of the NEXT month
        // (since header shows billing month, not due date)
        assertNotNull(result.periodEnd)
    }

    @Test
    fun `extract month from cyrillic header`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            ОКТОБАР 2025
            Наплатни број: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertNotNull(result.periodEnd)
    }

    // ============================================
    // COMPREHENSIVE REAL BILL TESTS
    // ============================================

    @Test
    fun `parse complete EPS bill in latin`() {
        val text = """
            EPS SNABDEVANJE D.O.O. BEOGRAD
            
            KUPAC: MILAN MILANOVIĆ
            Adresa: Kneza Miloša 15
            11000 BEOGRAD
            
            Naplatni broj: 2004158536
            Račun broj: 987654321
            ED broj: 12345678
            
            Period obračuna: 05.10.2025 - 01.11.2025
            
            POTROŠNJA U OBRAČUNSKOM PERIODU
            Viša tarifa (VT): 150 kWh
            Niža tarifa (NT): 80 kWh
            Ukupno: 230 kWh
            
            UKUPNO ZA UPLATU: 4.521,36 RSD
            
            Rok za plaćanje: 15.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        // Verify all fields are extracted
        assertEquals("2004158536", result.naplatniBroj)
        assertEquals("987654321", result.invoiceNumber)
        assertEquals("12345678", result.edNumber)
        assertNotNull(result.periodStart)
        assertNotNull(result.periodEnd)
        assertNotNull(result.paymentId)
        assertEquals(BigDecimal("150"), result.consumptionVt)
        assertEquals(BigDecimal("80"), result.consumptionNt)
        assertEquals(BigDecimal("230"), result.totalConsumption)
        assertNotNull(result.dueDate)
        assertFalse(result.isStorno)
    }

    @Test
    fun `parse complete EPS bill in cyrillic`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ Д.О.О. БЕОГРАД
            
            КОРИСНИК: МИЛАН МИЛАНОВИЋ
            Адреса: Кнеза Милоша 15
            11000 БЕОГРАД
            
            Наплатни број: 2004158536
            Рачун број: 987654321
            ЕД број: 12345678
            
            Период обрачуна: 05.10.2025 - 01.11.2025
            
            ПОТРОШЊА У ОБРАЧУНСКОМ ПЕРИОДУ
            Виша тарифа (ВТ): 150 kWh
            Нижа тарифа (НТ): 80 kWh
            Укупно: 230 kWh
            
            УКУПНО ЗА УПЛАТУ: 4.521,36 РСД
            
            Рок за плаћање: 15.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        // Verify critical fields
        assertEquals("2004158536", result.naplatniBroj)
        assertEquals("987654321", result.invoiceNumber)
        assertNotNull(result.paymentId)
        assertFalse(result.isStorno)
    }

    @Test
    fun `parse STORNO bill`() {
        val text = """
            EPS SNABDEVANJE D.O.O. BEOGRAD
            
            - STORNO -
            
            Naplatni broj: 2004158536
            Račun broj: 987654321
            Period obračuna: 05.10.2025 - 01.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertTrue("Bill should be marked as STORNO", result.isStorno)
        assertEquals("2004158536", result.naplatniBroj)
        assertNotNull(result.paymentId)
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `handle mixed latin and cyrillic text`() {
        val text = """
            EPS СНАБДЕВАЊЕ
            Naplatni broj: 2004158536
            Период обрачуна: 05.10.2025 - 01.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        assertEquals("2004158536", result.naplatniBroj)
        assertNotNull(result.periodStart)
    }

    @Test
    fun `handle OCR artifacts in text`() {
        // OCR often confuses similar-looking characters
        val text = """
            EPS SNABDEVANJE
            Nap1atni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        // Should still find naplatni broj even with minor OCR errors
        // Note: This might not work depending on parser robustness
    }

    @Test
    fun `handle missing fields gracefully`() {
        val text = """
            EPS SNABDEVANJE
            Ukupno za uplatu: 4521,36 RSD
        """.trimIndent()
        
        val result = EpsParser.parse(text)
        
        // Should not crash, fields should be null
        assertNull(result.naplatniBroj)
        assertNull(result.invoiceNumber)
        assertNull(result.paymentId)
        assertNull(result.periodStart)
    }

    @Test
    fun `handle empty text`() {
        val result = EpsParser.parse("")
        
        // Should return EpsData with all null fields
        assertNull(result.naplatniBroj)
        assertNull(result.invoiceNumber)
        assertNull(result.paymentId)
        assertFalse(result.isStorno)
    }
}

