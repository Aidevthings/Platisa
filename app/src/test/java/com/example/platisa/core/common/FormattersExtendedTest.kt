package com.example.platisa.core.common

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

/**
 * Prošireni unit testovi za Formatters klasu.
 * 
 * Testira formatiranje valuta za srpski format:
 * - Decimalni separator: zarez (,)
 * - Separator hiljada: tačka (.)
 */
class FormattersExtendedTest {

    // ============================================
    // FORMAT CURRENCY TESTS
    // ============================================

    @Test
    fun `formatCurrency formats typical Serbian bill amount`() {
        val amount = BigDecimal("4521.36")
        val result = Formatters.formatCurrency(amount)
        
        // Srpski format: 4.521,36
        assertTrue("Should contain comma as decimal separator", result.contains(","))
        assertTrue("Should contain dot as thousand separator", result.contains("."))
    }

    @Test
    fun `formatCurrency handles null amount`() {
        val result = Formatters.formatCurrency(null)
        
        assertEquals("0,00", result)
    }

    @Test
    fun `formatCurrency formats small amount correctly`() {
        val amount = BigDecimal("15.50")
        val result = Formatters.formatCurrency(amount)
        
        assertEquals("15,50", result)
    }

    @Test
    fun `formatCurrency formats large amount with thousand separator`() {
        val amount = BigDecimal("123456789.99")
        val result = Formatters.formatCurrency(amount)
        
        // Should have dots as thousand separators
        assertTrue("Large amount should have thousand separators", result.contains("."))
        assertTrue("Should end with ,99", result.endsWith(",99"))
    }

    @Test
    fun `formatCurrency handles zero amount`() {
        val amount = BigDecimal.ZERO
        val result = Formatters.formatCurrency(amount)
        
        assertEquals("0,00", result)
    }

    @Test
    fun `formatCurrency rounds to two decimal places`() {
        val amount = BigDecimal("100.999")
        val result = Formatters.formatCurrency(amount)
        
        // Should round to 101,00 or similar
        assertTrue("Should have exactly 2 decimal places", result.matches(Regex(".*,\\d{2}$")))
    }

    // ============================================
    // FORMAT CURRENCY WITH SUFFIX TESTS
    // ============================================

    @Test
    fun `formatCurrencyWithSuffix adds RSD suffix by default`() {
        val amount = BigDecimal("1000.00")
        val result = Formatters.formatCurrencyWithSuffix(amount)
        
        assertTrue("Should contain RSD suffix", result.endsWith("RSD"))
    }

    @Test
    fun `formatCurrencyWithSuffix adds EUR symbol for EUR currency`() {
        val amount = BigDecimal("100.00")
        val result = Formatters.formatCurrencyWithSuffix(amount, "EUR")
        
        assertTrue("Should contain € symbol", result.endsWith("€"))
    }

    @Test
    fun `formatCurrencyWithSuffix handles typical EPS bill amount`() {
        val amount = BigDecimal("4521.36")
        val result = Formatters.formatCurrencyWithSuffix(amount)
        
        assertTrue("Should contain formatted amount", result.contains("4.521,36"))
        assertTrue("Should end with RSD", result.endsWith("RSD"))
    }

    // ============================================
    // FORMAT CURRENCY SIMPLE TESTS
    // ============================================

    @Test
    fun `formatCurrencySimple removes decimal places`() {
        val amount = BigDecimal("4521.36")
        val result = Formatters.formatCurrencySimple(amount)
        
        assertFalse("Should not contain decimal separator", result.contains(","))
        assertEquals("4.521", result)
    }

    @Test
    fun `formatCurrencySimple handles null`() {
        val result = Formatters.formatCurrencySimple(null)
        
        assertEquals("0", result)
    }

    @Test
    fun `formatCurrencySimple handles large numbers`() {
        val amount = BigDecimal("999999.99")
        val result = Formatters.formatCurrencySimple(amount)
        
        assertEquals("999.999", result)
    }

    // ============================================
    // FORMAT AMOUNT TESTS
    // ============================================

    @Test
    fun `formatAmount removes trailing zeros`() {
        val amount = BigDecimal("100.00")
        val result = Formatters.formatAmount(amount)
        
        assertEquals("100", result)
    }

    @Test
    fun `formatAmount preserves significant decimals`() {
        val amount = BigDecimal("100.50")
        val result = Formatters.formatAmount(amount)
        
        assertEquals("100.5", result)
    }

    @Test
    fun `formatAmount handles null`() {
        val result = Formatters.formatAmount(null)
        
        assertEquals("0", result)
    }

    // ============================================
    // FORMAT DATE TESTS
    // ============================================

    @Test
    fun `formatDate formats correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.JANUARY, 1)
        val date = calendar.time
        
        val result = Formatters.formatDate(date)
        
        assertEquals("01.01.2025", result)
    }

    @Test
    fun `formatDate formats typical bill due date`() {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.NOVEMBER, 15)
        val date = calendar.time
        
        val result = Formatters.formatDate(date)
        
        assertEquals("15.11.2025", result)
    }

    @Test
    fun `formatDate handles December correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.DECEMBER, 31)
        val date = calendar.time
        
        val result = Formatters.formatDate(date)
        
        assertEquals("31.12.2025", result)
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `formatCurrency handles negative amount`() {
        val amount = BigDecimal("-100.50")
        val result = Formatters.formatCurrency(amount)
        
        assertTrue("Should handle negative amounts", result.contains("-"))
    }

    @Test
    fun `formatCurrency handles very small amount`() {
        val amount = BigDecimal("0.01")
        val result = Formatters.formatCurrency(amount)
        
        assertEquals("0,01", result)
    }

    @Test
    fun `formatCurrencySimple truncates rather than rounds`() {
        // This documents the actual behavior
        val amount = BigDecimal("1000.99")
        val result = Formatters.formatCurrencySimple(amount)
        
        // toLong() truncates
        assertEquals("1.000", result)
    }
}
