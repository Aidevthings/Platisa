package com.platisa.app.core.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Unit tests za EpsData model i PaymentId kreiranje.
 */
class EpsDataTest {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // ============================================
    // PAYMENT ID CREATION TESTS
    // ============================================

    @Test
    fun `createPaymentId returns correct format`() {
        val naplatniBroj = "2004158536"
        val periodStart = dateFormat.parse("05.10.2025")!!
        val periodEnd = dateFormat.parse("01.11.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("2004158536-20251005-20251101", paymentId)
    }

    @Test
    fun `createPaymentId returns null when naplatniBroj is null`() {
        val periodStart = dateFormat.parse("05.10.2025")!!
        val periodEnd = dateFormat.parse("01.11.2025")!!
        
        val paymentId = EpsData.createPaymentId(null, periodStart, periodEnd)
        
        assertNull("PaymentId should be null when naplatniBroj is null", paymentId)
    }

    @Test
    fun `createPaymentId returns null when periodStart is null`() {
        val naplatniBroj = "2004158536"
        val periodEnd = dateFormat.parse("01.11.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, null, periodEnd)
        
        assertNull("PaymentId should be null when periodStart is null", paymentId)
    }

    @Test
    fun `createPaymentId returns null when periodEnd is null`() {
        val naplatniBroj = "2004158536"
        val periodStart = dateFormat.parse("05.10.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, null)
        
        assertNull("PaymentId should be null when periodEnd is null", paymentId)
    }

    @Test
    fun `createPaymentId returns null when all parameters are null`() {
        val paymentId = EpsData.createPaymentId(null, null, null)
        
        assertNull(paymentId)
    }

    @Test
    fun `createPaymentId handles single digit day and month`() {
        val naplatniBroj = "1234567890"
        val periodStart = dateFormat.parse("05.01.2025")!! // January 5th
        val periodEnd = dateFormat.parse("01.02.2025")!!   // February 1st
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("1234567890-20250105-20250201", paymentId)
    }

    @Test
    fun `createPaymentId handles December to January period`() {
        val naplatniBroj = "1234567890"
        val periodStart = dateFormat.parse("01.12.2024")!!
        val periodEnd = dateFormat.parse("01.01.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("1234567890-20241201-20250101", paymentId)
    }

    @Test
    fun `createPaymentId handles same day period`() {
        val naplatniBroj = "1234567890"
        val sameDate = dateFormat.parse("15.10.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, sameDate, sameDate)
        
        assertEquals("1234567890-20251015-20251015", paymentId)
    }

    // ============================================
    // PAYMENT ID UNIQUENESS TESTS
    // ============================================

    @Test
    fun `same naplatniBroj different period creates different PaymentId`() {
        val naplatniBroj = "2004158536"
        
        val periodStart1 = dateFormat.parse("05.10.2025")!!
        val periodEnd1 = dateFormat.parse("01.11.2025")!!
        
        val periodStart2 = dateFormat.parse("05.11.2025")!!
        val periodEnd2 = dateFormat.parse("01.12.2025")!!
        
        val paymentId1 = EpsData.createPaymentId(naplatniBroj, periodStart1, periodEnd1)
        val paymentId2 = EpsData.createPaymentId(naplatniBroj, periodStart2, periodEnd2)
        
        assertNotEquals("Different periods should create different PaymentIds", paymentId1, paymentId2)
    }

    @Test
    fun `same period different naplatniBroj creates different PaymentId`() {
        val periodStart = dateFormat.parse("05.10.2025")!!
        val periodEnd = dateFormat.parse("01.11.2025")!!
        
        val paymentId1 = EpsData.createPaymentId("1111111111", periodStart, periodEnd)
        val paymentId2 = EpsData.createPaymentId("2222222222", periodStart, periodEnd)
        
        assertNotEquals("Different naplatniBroj should create different PaymentIds", paymentId1, paymentId2)
    }

    @Test
    fun `same inputs create same PaymentId`() {
        val naplatniBroj = "2004158536"
        val periodStart = dateFormat.parse("05.10.2025")!!
        val periodEnd = dateFormat.parse("01.11.2025")!!
        
        val paymentId1 = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        val paymentId2 = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("Same inputs should create same PaymentId", paymentId1, paymentId2)
    }

    // ============================================
    // DATE FORMAT EDGE CASES
    // ============================================

    @Test
    fun `createPaymentId with leap year date`() {
        val naplatniBroj = "1234567890"
        val periodStart = dateFormat.parse("29.02.2024")!! // 2024 is a leap year
        val periodEnd = dateFormat.parse("29.03.2024")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("1234567890-20240229-20240329", paymentId)
    }

    @Test
    fun `createPaymentId with end of year dates`() {
        val naplatniBroj = "1234567890"
        val periodStart = dateFormat.parse("01.12.2025")!!
        val periodEnd = dateFormat.parse("31.12.2025")!!
        
        val paymentId = EpsData.createPaymentId(naplatniBroj, periodStart, periodEnd)
        
        assertEquals("1234567890-20251201-20251231", paymentId)
    }

    // ============================================
    // EpsData CREATION TESTS
    // ============================================

    @Test
    fun `EpsData data class holds all values correctly`() {
        val periodStart = dateFormat.parse("05.10.2025")!!
        val periodEnd = dateFormat.parse("01.11.2025")!!
        val dueDate = dateFormat.parse("15.11.2025")!!
        
        val epsData = EpsData(
            edNumber = "12345678",
            billingPeriod = "05.10.2025 - 01.11.2025",
            consumptionVt = java.math.BigDecimal("150"),
            consumptionNt = java.math.BigDecimal("80"),
            totalConsumption = java.math.BigDecimal("230"),
            naplatniBroj = "2004158536",
            invoiceNumber = "987654321",
            periodStart = periodStart,
            periodEnd = periodEnd,
            isStorno = false,
            dueDate = dueDate,
            paymentId = "2004158536-20251005-20251101",
            recipientName = "PETAR PETROVIĆ",
            recipientAddress = "Ulica 123, 11000 BEOGRAD"
        )
        
        assertEquals("12345678", epsData.edNumber)
        assertEquals("05.10.2025 - 01.11.2025", epsData.billingPeriod)
        assertEquals(java.math.BigDecimal("150"), epsData.consumptionVt)
        assertEquals(java.math.BigDecimal("80"), epsData.consumptionNt)
        assertEquals(java.math.BigDecimal("230"), epsData.totalConsumption)
        assertEquals("2004158536", epsData.naplatniBroj)
        assertEquals("987654321", epsData.invoiceNumber)
        assertEquals(periodStart, epsData.periodStart)
        assertEquals(periodEnd, epsData.periodEnd)
        assertFalse(epsData.isStorno)
        assertEquals(dueDate, epsData.dueDate)
        assertEquals("2004158536-20251005-20251101", epsData.paymentId)
        assertEquals("PETAR PETROVIĆ", epsData.recipientName)
        assertEquals("Ulica 123, 11000 BEOGRAD", epsData.recipientAddress)
    }

    @Test
    fun `EpsData default values are correct`() {
        val epsData = EpsData(
            edNumber = null,
            billingPeriod = null,
            consumptionVt = null,
            consumptionNt = null,
            totalConsumption = null,
            naplatniBroj = null,
            invoiceNumber = null,
            periodStart = null,
            periodEnd = null,
            dueDate = null,
            paymentId = null
        )
        
        assertFalse("Default isStorno should be false", epsData.isStorno)
        assertNull(epsData.recipientName)
        assertNull(epsData.recipientAddress)
    }

    @Test
    fun `EpsData isStorno true works correctly`() {
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
            isStorno = true,
            dueDate = null,
            paymentId = null
        )
        
        assertTrue(epsData.isStorno)
    }
}

