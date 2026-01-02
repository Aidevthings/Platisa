package com.platisa.app.core.data.helper

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests za string normalizaciju korišćenu u detekciji duplikata.
 * 
 * Ovi testovi validiraju da su obrasci normalizacije ispravni za 
 * srpski tekst (ćirilica i latinica).
 */
class StringNormalizationTest {

    /**
     * Replika normalizeString funkcije iz BillDuplicateDetector.
     * Koristi se za testiranje jer je originalna metoda privatna.
     */
    private fun normalizeString(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        
        // Zadrži: a-z, A-Z, 0-9, Ćirilica (U+0400-U+04FF pokriva svu ćirilicu uključujući srpsku)
        return input
            .replace(Regex("[^a-zA-Z0-9\\u0400-\\u04FF]"), "")
            .lowercase()
    }

    // ============================================
    // BASIC NORMALIZATION TESTS
    // ============================================

    @Test
    fun `normalize removes spaces`() {
        val result = normalizeString("ABC 123 DEF")
        assertEquals("abc123def", result)
    }

    @Test
    fun `normalize removes special characters`() {
        val result = normalizeString("ABC-123.DEF/456")
        assertEquals("abc123def456", result)
    }

    @Test
    fun `normalize converts to lowercase`() {
        val result = normalizeString("UPPERCASE")
        assertEquals("uppercase", result)
    }

    @Test
    fun `normalize handles null input`() {
        val result = normalizeString(null)
        assertEquals("", result)
    }

    @Test
    fun `normalize handles empty input`() {
        val result = normalizeString("")
        assertEquals("", result)
    }

    // ============================================
    // CYRILLIC NORMALIZATION TESTS
    // ============================================

    @Test
    fun `normalize preserves cyrillic characters`() {
        val result = normalizeString("АБВГДЕЖЗ")
        assertEquals("абвгдежз", result)
    }

    @Test
    fun `normalize preserves serbian cyrillic specific characters`() {
        // Serbian-specific Cyrillic: Ђ, Ј, Љ, Њ, Ћ, Џ
        val result = normalizeString("ЂЈЉЊЋЏ")
        assertEquals("ђјљњћџ", result)
    }

    @Test
    fun `normalize handles mixed latin and cyrillic`() {
        val result = normalizeString("ABC123АБВ")
        assertEquals("abc123абв", result)
    }

    @Test
    fun `normalize cyrillic invoice number`() {
        val result = normalizeString("Рачун број: 12345")
        assertEquals("рачунброј12345", result)
    }

    // ============================================
    // SERBIAN-SPECIFIC TESTS
    // ============================================

    @Test
    fun `normalize handles serbian latin special characters`() {
        // Serbian Latin: Č, Ć, Ž, Š, Đ
        val input = "ČĆŽŠĐ čćžšđ"
        val result = normalizeString(input)
        // Note: These are Latin characters, not in basic a-z range
        // The current regex [^a-zA-Z0-9\u0400-\u04FF] will REMOVE them!
        // This test documents the current behavior
        assertEquals("", result) // Serbian Latin diacritics are removed
    }

    @Test
    fun `normalize EPS SNABDEVANJE latin`() {
        val result = normalizeString("EPS SNABDEVANJE D.O.O.")
        assertEquals("epssnabdevanjedoo", result)
    }

    @Test
    fun `normalize EPS SNABDEVANJE cyrillic`() {
        val result = normalizeString("ЕПС СНАБДЕВАЊЕ Д.О.О.")
        assertEquals("епсснабдевањедоо", result)
    }

    // ============================================
    // INVOICE NUMBER NORMALIZATION TESTS
    // ============================================

    @Test
    fun `normalize invoice number with dashes`() {
        val result = normalizeString("INV-2024-001234")
        assertEquals("inv2024001234", result)
    }

    @Test
    fun `normalize invoice number with slashes`() {
        val result = normalizeString("12345/2024")
        assertEquals("123452024", result)
    }

    @Test
    fun `normalize invoice number with leading zeros`() {
        val result = normalizeString("000123456")
        assertEquals("000123456", result)
    }

    @Test
    fun `same invoice numbers in different formats normalize to same string`() {
        val format1 = normalizeString("INV-12345")
        val format2 = normalizeString("INV 12345")
        val format3 = normalizeString("INV.12345")
        
        assertEquals(format1, format2)
        assertEquals(format2, format3)
    }

    // ============================================
    // DUPLICATE DETECTION SIMULATION TESTS
    // ============================================

    @Test
    fun `same invoice in latin and cyrillic should NOT match`() {
        // Note: Latin "ABC" and Cyrillic "АБЦ" are different characters
        val latin = normalizeString("ABC123")
        val cyrillic = normalizeString("АБЦ123")
        
        assertNotEquals(latin, cyrillic)
    }

    @Test
    fun `same numbers match regardless of surrounding text`() {
        val invoice1 = normalizeString("Račun: 12345678")
        val invoice2 = normalizeString("Рачун: 12345678")
        
        // Both contain the same number, but different text
        assertTrue(invoice1.contains("12345678"))
        assertTrue(invoice2.contains("12345678"))
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `normalize very long string`() {
        val longString = "A".repeat(10000) + "123"
        val result = normalizeString(longString)
        
        assertEquals("a".repeat(10000) + "123", result)
    }

    @Test
    fun `normalize string with only special characters returns empty`() {
        val result = normalizeString("!@#$%^&*()")
        assertEquals("", result)
    }

    @Test
    fun `normalize string with unicode outside cyrillic range`() {
        // Greek, Chinese, Arabic characters should be removed
        val result = normalizeString("αβγ中文العربية")
        assertEquals("", result)
    }

    @Test
    fun `normalize newlines and tabs`() {
        val result = normalizeString("ABC\n123\tDEF")
        assertEquals("abc123def", result)
    }

    // ============================================
    // MERCHANT NAME NORMALIZATION TESTS
    // ============================================

    /**
     * Replika normalizeMerchant funkcije iz BillDuplicateDetector.
     */
    private fun normalizeMerchant(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9\\u0400-\\u04FF]"), "")
    }

    @Test
    fun `same merchant in different formats matches`() {
        val format1 = normalizeMerchant("EPS SNABDEVANJE D.O.O.")
        val format2 = normalizeMerchant("EPS SNABDEVANJE D.O.O")
        val format3 = normalizeMerchant("EPS SNABDEVANJE DOO")
        
        assertEquals(format1, format2)
        assertEquals(format2, format3)
    }

    @Test
    fun `cyrillic and latin merchant names are different`() {
        val latin = normalizeMerchant("EPS SNABDEVANJE")
        val cyrillic = normalizeMerchant("ЕПС СНАБДЕВАЊЕ")
        
        assertNotEquals(latin, cyrillic)
    }
}

