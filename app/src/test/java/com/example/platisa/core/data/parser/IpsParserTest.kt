package com.example.platisa.core.data.parser

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests za IpsParser - parser za NBS IPS QR kodove.
 * 
 * NBS IPS format:
 * K:PR|V:01|C:1|R:123456789012345678|N:Primalac|I:RSD1234,56|...
 */
class IpsParserTest {

    // ============================================
    // VALID QR CODE TESTS
    // ============================================

    @Test
    fun `parse valid IPS QR code with all fields`() {
        val qrContent = "K:PR|V:01|C:1|R:265000000009164816|N:EPS SNABDEVANJE|I:RSD3456,78|SF:289|S:Uplata po racunu|RO:97172221100170515"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull("Parser treba da vrati rezultat", result)
        assertEquals("EPS SNABDEVANJE", result!!.recipientName)
        assertEquals("265000000009164816", result.recipientAccount)
        assertEquals(BigDecimal("3456.78"), result.amount)
        assertEquals("289", result.purposeCode)
        assertEquals("Uplata po racunu", result.purposeDescription)
        assertEquals("97172221100170515", result.referenceNumber)
        assertEquals("RSD", result.currency)
    }

    @Test
    fun `parse IPS QR code with Serbian amount format`() {
        // Srpski format koristi zarez kao decimalni separator
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Test Primalac|I:RSD12345,67"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals(BigDecimal("12345.67"), result!!.amount)
    }

    @Test
    fun `parse IPS QR code with whole number amount`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Test|I:RSD5000"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals(BigDecimal("5000"), result!!.amount)
    }

    @Test
    fun `parse IPS QR code with minimum required fields`() {
        val qrContent = "K:PR|V:01|C:1|R:265000000009164816|N:Primalac"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("Primalac", result!!.recipientName)
        assertEquals("265000000009164816", result.recipientAccount)
        assertNull("Amount should be null when not provided", result.amount)
    }

    @Test
    fun `parse IPS QR code with payer name`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Primalac|I:RSD1000,00|P:PETAR PETROVIC"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertNotNull("Payer name should be parsed", result!!.payerName)
    }

    // ============================================
    // INVALID QR CODE TESTS
    // ============================================

    @Test
    fun `parse returns null for non-IPS QR code`() {
        val qrContent = "https://example.com/some-url"
        
        val result = IpsParser.parse(qrContent)
        
        assertNull("Non-IPS QR code should return null", result)
    }

    @Test
    fun `parse returns null for empty string`() {
        val result = IpsParser.parse("")
        
        assertNull("Empty string should return null", result)
    }

    @Test
    fun `parse returns null for random text`() {
        val result = IpsParser.parse("Random text without IPS format")
        
        assertNull(result)
    }

    @Test
    fun `parse returns null for QR starting with wrong prefix`() {
        val qrContent = "V:PR|K:01|C:1|R:123456789012345678" // Wrong prefix order
        
        val result = IpsParser.parse(qrContent)
        
        assertNull("QR not starting with K:PR should return null", result)
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `parse handles special characters in recipient name`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:ČĆŽŠĐ Kompanija d.o.o.|I:RSD100,00"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("ČĆŽŠĐ Kompanija d.o.o.", result!!.recipientName)
    }

    @Test
    fun `parse handles cyrillic text in recipient name`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:ЕПС СНАБДЕВАЊЕ|I:RSD100,00"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("ЕПС СНАБДЕВАЊЕ", result!!.recipientName)
    }

    @Test
    fun `parse handles very large amounts`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Test|I:RSD999999999,99"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals(BigDecimal("999999999.99"), result!!.amount)
    }

    @Test
    fun `parse handles zero amount`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Test|I:RSD0,00"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals(BigDecimal("0.00"), result!!.amount)
    }

    @Test
    fun `parse handles long reference number`() {
        val qrContent = "K:PR|V:01|C:1|R:123456789012345678|N:Test|I:RSD100,00|RO:971722211001705151234567890"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("971722211001705151234567890", result!!.referenceNumber)
    }

    // ============================================
    // REAL-WORLD EPS EXAMPLES
    // ============================================

    @Test
    fun `parse real EPS QR code example`() {
        // Simulacija pravog EPS QR koda
        val qrContent = "K:PR|V:01|C:1|R:265000000009164816|N:EPS SNABDEVANJE DOO BEOGRAD|I:RSD4521,36|SF:289|S:Racun za el.energiju|RO:97172221100170515"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("EPS SNABDEVANJE DOO BEOGRAD", result!!.recipientName)
        assertEquals("265000000009164816", result.recipientAccount)
        assertEquals(BigDecimal("4521.36"), result.amount)
        assertEquals("289", result.purposeCode)
        assertTrue(result.purposeDescription!!.contains("Racun"))
    }

    @Test
    fun `parse real Telekom QR code example`() {
        val qrContent = "K:PR|V:01|C:1|R:160000000012345678|N:TELEKOM SRBIJA AD|I:RSD2890,00|SF:289|S:Mesecni racun|RO:123456789012"
        
        val result = IpsParser.parse(qrContent)
        
        assertNotNull(result)
        assertEquals("TELEKOM SRBIJA AD", result!!.recipientName)
        assertEquals(BigDecimal("2890.00"), result.amount)
    }
}
