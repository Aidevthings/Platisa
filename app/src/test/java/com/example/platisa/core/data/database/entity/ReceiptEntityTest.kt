package com.example.platisa.core.data.database.entity

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

/**
 * Unit testovi za ReceiptEntity model.
 * 
 * Testira kreiranje, podrazumevane vrednosti i data class funkcionalnosti.
 */
class ReceiptEntityTest {

    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    
    private fun createTestDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.OCTOBER, 15)
        return calendar.time
    }

    // ============================================
    // CREATION TESTS
    // ============================================

    @Test
    fun `create ReceiptEntity with minimum required fields`() {
        val receipt = ReceiptEntity(
            merchantName = "EPS SNABDEVANJE",
            date = createTestDate(),
            totalAmount = BigDecimal("4521.36"),
            imagePath = "/storage/bills/eps_oct.pdf"
        )
        
        assertEquals("EPS SNABDEVANJE", receipt.merchantName)
        assertEquals(BigDecimal("4521.36"), receipt.totalAmount)
        assertEquals(0L, receipt.id) // Auto-generate starts at 0
    }

    @Test
    fun `create ReceiptEntity with all fields`() {
        val date = createTestDate()
        val dueDate = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_MONTH, 15)
        }.time
        
        val receipt = ReceiptEntity(
            id = 42,
            merchantName = "EPS SNABDEVANJE",
            date = date,
            dueDate = dueDate,
            totalAmount = BigDecimal("4521.36"),
            currency = "RSD",
            imagePath = "/storage/bills/eps_oct.pdf",
            qrCodeData = "K:PR|V:01|C:1|R:265000000009164816|N:EPS",
            invoiceNumber = "987654321",
            naplatniNumber = "2004158536",
            paymentId = "2004158536-20251005-20251101",
            isStorno = false,
            isVisible = true,
            sectionId = 1L,
            metadata = "{\"provider\":\"EPS\"}",
            syncStatus = SyncStatus.SYNCED,
            paymentStatus = PaymentStatus.UNPAID,
            sourceType = SourceType.GMAIL,
            externalId = "gmail-12345",
            recipientName = "MILAN MILANOVIC",
            recipientAddress = "Kneza Milosa 15, Beograd"
        )
        
        assertEquals(42L, receipt.id)
        assertEquals("EPS SNABDEVANJE", receipt.merchantName)
        assertEquals("987654321", receipt.invoiceNumber)
        assertEquals("2004158536", receipt.naplatniNumber)
        assertEquals("2004158536-20251005-20251101", receipt.paymentId)
        assertFalse(receipt.isStorno)
        assertTrue(receipt.isVisible)
    }

    // ============================================
    // DEFAULT VALUES TESTS
    // ============================================

    @Test
    fun `ReceiptEntity has correct default values`() {
        val receipt = ReceiptEntity(
            merchantName = "Test",
            date = createTestDate(),
            totalAmount = BigDecimal.ONE,
            imagePath = "/test"
        )
        
        assertEquals("RSD", receipt.currency)
        assertNull(receipt.dueDate)
        assertNull(receipt.qrCodeData)
        assertNull(receipt.invoiceNumber)
        assertNull(receipt.naplatniNumber)
        assertNull(receipt.paymentId)
        assertFalse(receipt.isStorno)
        assertTrue(receipt.isVisible)
        assertNull(receipt.sectionId)
        assertNull(receipt.metadata)
        assertEquals(SyncStatus.PENDING, receipt.syncStatus)
        assertEquals(PaymentStatus.UNPAID, receipt.paymentStatus)
        assertEquals(SourceType.CAMERA, receipt.sourceType)
        assertNull(receipt.externalId)
        assertNull(receipt.savedQrUri)
        assertNull(receipt.recipientName)
        assertNull(receipt.recipientAddress)
    }

    // ============================================
    // ENUM TESTS
    // ============================================

    @Test
    fun `SyncStatus enum has correct values`() {
        assertEquals(3, SyncStatus.values().size)
        assertTrue(SyncStatus.values().contains(SyncStatus.PENDING))
        assertTrue(SyncStatus.values().contains(SyncStatus.SYNCED))
        assertTrue(SyncStatus.values().contains(SyncStatus.FAILED))
    }

    @Test
    fun `PaymentStatus enum has correct values`() {
        assertEquals(3, PaymentStatus.values().size)
        assertTrue(PaymentStatus.values().contains(PaymentStatus.UNPAID))
        assertTrue(PaymentStatus.values().contains(PaymentStatus.PROCESSING))
        assertTrue(PaymentStatus.values().contains(PaymentStatus.PAID))
    }

    @Test
    fun `SourceType enum has correct values`() {
        assertEquals(5, SourceType.values().size)
        assertTrue(SourceType.values().contains(SourceType.CAMERA))
        assertTrue(SourceType.values().contains(SourceType.GMAIL))
        assertTrue(SourceType.values().contains(SourceType.MANUAL))
        assertTrue(SourceType.values().contains(SourceType.CAMERA_FISCAL))
        assertTrue(SourceType.values().contains(SourceType.CAMERA_IPS))
    }

    // ============================================
    // DATA CLASS COPY TESTS
    // ============================================

    @Test
    fun `copy creates independent copy with modified field`() {
        val original = ReceiptEntity(
            id = 1,
            merchantName = "Original",
            date = createTestDate(),
            totalAmount = BigDecimal("100.00"),
            imagePath = "/test",
            paymentStatus = PaymentStatus.UNPAID
        )
        
        val copy = original.copy(paymentStatus = PaymentStatus.PAID)
        
        assertEquals(PaymentStatus.UNPAID, original.paymentStatus)
        assertEquals(PaymentStatus.PAID, copy.paymentStatus)
        assertEquals(original.merchantName, copy.merchantName)
    }

    @Test
    fun `copy preserves all fields when only one changed`() {
        val original = ReceiptEntity(
            id = 42,
            merchantName = "Test Merchant",
            date = createTestDate(),
            totalAmount = BigDecimal("999.99"),
            imagePath = "/test/path",
            invoiceNumber = "INV-001",
            naplatniNumber = "NAP-001",
            paymentId = "PAY-001",
            isStorno = true,
            isVisible = false
        )
        
        val copy = original.copy(isVisible = true)
        
        assertEquals(42L, copy.id)
        assertEquals("Test Merchant", copy.merchantName)
        assertEquals(BigDecimal("999.99"), copy.totalAmount)
        assertEquals("INV-001", copy.invoiceNumber)
        assertEquals("NAP-001", copy.naplatniNumber)
        assertEquals("PAY-001", copy.paymentId)
        assertTrue(copy.isStorno)
        assertTrue(copy.isVisible)
    }

    // ============================================
    // EQUALITY TESTS
    // ============================================

    @Test
    fun `two ReceiptEntities with same values are equal`() {
        val date = createTestDate()
        val receipt1 = ReceiptEntity(
            id = 1,
            merchantName = "EPS",
            date = date,
            totalAmount = BigDecimal("100.00"),
            imagePath = "/test"
        )
        val receipt2 = ReceiptEntity(
            id = 1,
            merchantName = "EPS",
            date = date,
            totalAmount = BigDecimal("100.00"),
            imagePath = "/test"
        )
        
        assertEquals(receipt1, receipt2)
    }

    @Test
    fun `two ReceiptEntities with different IDs are not equal`() {
        val date = createTestDate()
        val receipt1 = ReceiptEntity(
            id = 1,
            merchantName = "EPS",
            date = date,
            totalAmount = BigDecimal("100.00"),
            imagePath = "/test"
        )
        val receipt2 = ReceiptEntity(
            id = 2,
            merchantName = "EPS",
            date = date,
            totalAmount = BigDecimal("100.00"),
            imagePath = "/test"
        )
        
        assertNotEquals(receipt1, receipt2)
    }

    // ============================================
    // STORNO TESTS
    // ============================================

    @Test
    fun `STORNO receipt is correctly identified`() {
        val stornoReceipt = ReceiptEntity(
            merchantName = "EPS",
            date = createTestDate(),
            totalAmount = BigDecimal("0.00"),
            imagePath = "/test",
            isStorno = true
        )
        
        assertTrue(stornoReceipt.isStorno)
    }

    @Test
    fun `regular receipt is not STORNO`() {
        val regularReceipt = ReceiptEntity(
            merchantName = "EPS",
            date = createTestDate(),
            totalAmount = BigDecimal("1000.00"),
            imagePath = "/test"
        )
        
        assertFalse(regularReceipt.isStorno)
    }

    // ============================================
    // VISIBILITY TESTS
    // ============================================

    @Test
    fun `hidden STORNO receipt`() {
        val receipt = ReceiptEntity(
            merchantName = "EPS",
            date = createTestDate(),
            totalAmount = BigDecimal("0.00"),
            imagePath = "/test",
            isStorno = true,
            isVisible = false
        )
        
        assertTrue(receipt.isStorno)
        assertFalse(receipt.isVisible)
    }

    // ============================================
    // AMOUNT TESTS
    // ============================================

    @Test
    fun `receipt handles large amount`() {
        val receipt = ReceiptEntity(
            merchantName = "Test",
            date = createTestDate(),
            totalAmount = BigDecimal("999999999.99"),
            imagePath = "/test"
        )
        
        assertEquals(BigDecimal("999999999.99"), receipt.totalAmount)
    }

    @Test
    fun `receipt handles zero amount`() {
        val receipt = ReceiptEntity(
            merchantName = "Test",
            date = createTestDate(),
            totalAmount = BigDecimal.ZERO,
            imagePath = "/test"
        )
        
        assertEquals(BigDecimal.ZERO, receipt.totalAmount)
    }

    @Test
    fun `receipt handles negative amount for STORNO`() {
        val receipt = ReceiptEntity(
            merchantName = "Test",
            date = createTestDate(),
            totalAmount = BigDecimal("-1000.00"),
            imagePath = "/test",
            isStorno = true
        )
        
        assertEquals(BigDecimal("-1000.00"), receipt.totalAmount)
        assertTrue(receipt.isStorno)
    }
}
