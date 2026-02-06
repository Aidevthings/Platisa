package com.platisa.app.core.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit testovi za EpsData model.
 * 
 * Testira Payment ID generisanje koje je ključno za detekciju duplikata.
 * Format: "naplatniBroj-YYYYMMDD-YYYYMMDD"
 */
class EpsDataPaymentIdTest {

    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    
    private fun createDate(year: Int, month: Int, day: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 0, 0, 0) // Month is 0-indexed
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    // ============================================
    // VALID PAYMENT ID CREATION TESTS
    // ============================================

    @Test
    fun `createPaymentId generates correct format`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("2004158536-20251005-20251101-20571.95", result)
    }

    @Test
    fun `createPaymentId handles single digit month and day`() {
        val naplatniBroj = "123456789"
        val periodStart = createDate(2025, 1, 5)
        val periodEnd = createDate(2025, 2, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("123456789-20250105-20250201-20571.95", result)
    }

    @Test
    fun `createPaymentId handles end of year period`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2025, 12, 1)
        val periodEnd = createDate(2026, 1, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("2004158536-20251201-20260101-20571.95", result)
    }

    @Test
    fun `createPaymentId handles leap year date`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2024, 2, 29)
        val periodEnd = createDate(2024, 3, 31)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("2004158536-20240229-20240331-20571.95", result)
    }

    // ============================================
    // NULL INPUT TESTS
    // ============================================

    @Test
    fun `createPaymentId returns null when naplatniBroj is null`() {
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(null, periodStart, periodEnd, amount)
        
        assertNull("Should return null when naplatniBroj is null", result)
    }

    @Test
    fun `createPaymentId returns null when periodStart is null`() {
        val naplatniBroj = "2004158536"
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, null, periodEnd, amount)
        
        assertNull("Should return null when periodStart is null", result)
    }

    @Test
    fun `createPaymentId returns null when periodEnd is null`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2025, 10, 5)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, null, amount)
        
        assertNull("Should return null when periodEnd is null", result)
    }

    @Test
    fun `createPaymentId returns null when all parameters are null`() {
        val result = EpsData.createPaymentId(null, null, null, null)
        
        assertNull("Should return null when all parameters are null", result)
    }

    // ============================================
    // UNIQUENESS TESTS
    // ============================================

    @Test
    fun `same naplatniBroj different periods creates different PaymentIds`() {
        val naplatniBroj = "2004158536"
        val amount = java.math.BigDecimal("20571.95")
        
        val id1 = EpsData.createPaymentId(
            naplatniBroj,
            createDate(2025, 9, 5),
            createDate(2025, 10, 1),
            amount
        )
        
        val id2 = EpsData.createPaymentId(
            naplatniBroj,
            createDate(2025, 10, 5),
            createDate(2025, 11, 1),
            amount
        )
        
        assertNotEquals("Different periods should create different PaymentIds", id1, id2)
    }

    @Test
    fun `different naplatniBroj same period creates different PaymentIds`() {
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val id1 = EpsData.createPaymentId("2004158536", periodStart, periodEnd, amount)
        val id2 = EpsData.createPaymentId("2004158537", periodStart, periodEnd, amount)
        
        assertNotEquals("Different naplatniBroj should create different PaymentIds", id1, id2)
    }

    @Test
    fun `same parameters always creates same PaymentId`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val id1 = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        val id2 = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("Same parameters should always create same PaymentId", id1, id2)
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Test
    fun `createPaymentId handles empty naplatniBroj`() {
        val naplatniBroj = ""
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        // Empty string is not null, so should create a PaymentId
        assertEquals("-20251005-20251101-20571.95", result)
    }

    @Test
    fun `createPaymentId handles very long naplatniBroj`() {
        val naplatniBroj = "12345678901234567890"
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("12345678901234567890-20251005-20251101-20571.95", result)
    }

    @Test
    fun `createPaymentId handles naplatniBroj with special characters`() {
        val naplatniBroj = "200-415-8536"
        val periodStart = createDate(2025, 10, 5)
        val periodEnd = createDate(2025, 11, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        // Special characters are preserved
        assertEquals("200-415-8536-20251005-20251101-20571.95", result)
    }

    @Test
    fun `createPaymentId handles very old date`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(1990, 1, 1)
        val periodEnd = createDate(1990, 2, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("2004158536-19900101-19900201-20571.95", result)
    }

    @Test
    fun `createPaymentId handles future date`() {
        val naplatniBroj = "2004158536"
        val periodStart = createDate(2050, 12, 1)
        val periodEnd = createDate(2051, 1, 1)
        val amount = java.math.BigDecimal("20571.95")
        
        val result = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd, amount)
        
        assertEquals("2004158536-20501201-20510101-20571.95", result)
    }

    // ============================================
    // EPS DATA OBJECT TESTS
    // ============================================

    @Test
    fun `EpsData object correctly stores PaymentId`() {
        val epsData = EpsData(
            edNumber = "12345678",
            billingPeriod = "10/2025",
            consumptionVt = null,
            consumptionNt = null,
            totalConsumption = null,
            naplatniBroj = "2004158536",
            invoiceNumber = "987654321",
            periodStart = createDate(2025, 10, 5),
            periodEnd = createDate(2025, 11, 1),
            dueDate = createDate(2025, 11, 15),
            totalPayAmount = java.math.BigDecimal("20571.95"),
            paymentId = "2004158536-20251005-20251101-20571.95"
        )
        
        assertEquals("2004158536-20251005-20251101-20571.95", epsData.paymentId)
        assertFalse(epsData.isStorno)
    }

    @Test
    fun `EpsData STORNO flag defaults to false`() {
        val epsData = EpsData(
            edNumber = null,
            billingPeriod = null,
            consumptionVt = null,
            consumptionNt = null,
            totalConsumption = null,
            naplatniBroj = "2004158536",
            invoiceNumber = null,
            periodStart = null,
            periodEnd = null,
            dueDate = null,
            totalPayAmount = null,
            paymentId = null
        )
        
        assertFalse("STORNO should default to false", epsData.isStorno)
    }

    @Test
    fun `EpsData can be marked as STORNO`() {
        val epsData = EpsData(
            edNumber = null,
            billingPeriod = null,
            consumptionVt = null,
            consumptionNt = null,
            totalConsumption = null,
            naplatniBroj = "2004158536",
            invoiceNumber = null,
            periodStart = null,
            periodEnd = null,
            dueDate = null,
            totalPayAmount = null,
            paymentId = null,
            isStorno = true
        )
        
        assertTrue("STORNO should be true when set", epsData.isStorno)
    }
}

