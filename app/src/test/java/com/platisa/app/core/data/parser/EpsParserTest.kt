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
        
        val result = EpsParser.parse(text)!!
        
        assertEquals("2004158536", result.naplatniBroj)
    }

    @Test
    fun `extract naplatni broj from cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Наплатни број: 2004158536
            Рачун број: 123456
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
        assertEquals("2004158536", result.naplatniBroj)
    }

    @Test
    fun `extract invoice number from latin text`() {
        val text = """
            EPS SNABDEVANJE
            Račun broj: 987654321
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
        assertEquals("987654321", result.invoiceNumber)
    }

    @Test
    fun `extract invoice number from cyrillic text`() {
        val text = """
            ЕПС СНАБДЕВАЊЕ
            Рачун број: 987654321
            Наплатни број: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
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
        
        val result = EpsParser.parse(text)!!
        
        assertNotNull("Period start should be extracted", result.periodStart)
        assertNotNull("Period end should be extracted", result.periodEnd)
        
        assertEquals("05.10.2025", dateFormat.format(result.periodStart!!))
        assertEquals("01.11.2025", dateFormat.format(result.periodEnd!!))
    }

    @Test
    fun `create valid PaymentId from extracted data`() {
        val text = """
            EPS SNABDEVANJE
            Naplatni broj: 2004158536
            Račun broj: 987654321
            Period obračuna: 05.10.2025 - 01.11.2025
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
        assertNotNull("PaymentId should be created", result.paymentId)
        // Now uses Invoice Number (987654321) instead of Naplatni Broj
        assertEquals("987654321-20251005-20251101", result.paymentId)
    }

    @Test
    fun `detect STORNO in latin text`() {
        val text = """
            EPS SNABDEVANJE
            STORNO
            Naplatni broj: 2004158536
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
        assertTrue("STORNO should be detected", result.isStorno)
    }

    @Test
    fun `extract VT consumption in latin`() {
        val text = """
            EPS SNABDEVANJE
            Viša tarifa (VT): 150 kWh
            Niža tarifa (NT): 80 kWh
        """.trimIndent()
        
        val result = EpsParser.parse(text)!!
        
        assertNotNull("VT consumption should be extracted", result.consumptionVt)
        assertEquals(BigDecimal("150"), result.consumptionVt)
    }
}
