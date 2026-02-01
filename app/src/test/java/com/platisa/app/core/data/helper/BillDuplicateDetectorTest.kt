package com.platisa.app.core.data.helper

import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.entity.PaymentStatus
import com.platisa.app.core.data.database.entity.ReceiptEntity
import com.platisa.app.core.data.database.entity.SourceType
import com.platisa.app.core.data.database.entity.SyncStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.*

/**
 * Unit tests za BillDuplicateDetector.
 * 
 * Testira logiku detekcije duplikata uključujući:
 * - PaymentId podudaranje
 * - Broj računa podudaranje (latinica i ćirilica)
 * - STORNO račun detekciju
 * - Period + iznos podudaranje
 */
class BillDuplicateDetectorTest {

    @MockK
    private lateinit var receiptDao: ReceiptDao

    private lateinit var detector: BillDuplicateDetector

    private val testDate = Date()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        detector = BillDuplicateDetector(receiptDao)
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    
    private fun createReceipt(
        id: Long = 0,
        merchantName: String = "EPS SNABDEVANJE",
        invoiceNumber: String? = null,
        paymentId: String? = null,
        naplatniNumber: String? = null,
        totalAmount: BigDecimal = BigDecimal("1000.00"),
        paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
        isStorno: Boolean = false,
        date: Date = testDate
    ): ReceiptEntity {
        return ReceiptEntity(
            id = id,
            merchantName = merchantName,
            date = date,
            totalAmount = totalAmount,
            currency = "RSD",
            imagePath = "/test/path.jpg",
            invoiceNumber = invoiceNumber,
            paymentId = paymentId,
            naplatniNumber = naplatniNumber,
            paymentStatus = paymentStatus,
            isStorno = isStorno,
            syncStatus = SyncStatus.PENDING,
            sourceType = SourceType.GMAIL
        )
    }

    // ============================================
    // NO DUPLICATE TESTS
    // ============================================

    @Test
    fun `checkForDuplicate returns NoDuplicate when database is empty`() = runTest {
        val newReceipt = createReceipt(
            invoiceNumber = "INV-001",
            paymentId = "2004158536-20251005-20251101"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns emptyList()
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(newReceipt)
        
        assertTrue("Should return NoDuplicate", result is DuplicateCheckResult.NoDuplicate)
    }

    @Test
    fun `checkForDuplicate returns NoDuplicate when different invoice numbers`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-001"
        )
        val newReceipt = createReceipt(
            invoiceNumber = "INV-002"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(newReceipt)
        
        assertTrue("Different invoice numbers should not be duplicates", result is DuplicateCheckResult.NoDuplicate)
    }

    // ============================================
    // INVOICE NUMBER DUPLICATE TESTS
    // ============================================

    @Test
    fun `checkForDuplicate detects duplicate by invoice number`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345"
        )
        val newReceipt = createReceipt(
            invoiceNumber = "INV-12345"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(newReceipt)
        
        assertTrue("Same invoice number should be duplicate", result is DuplicateCheckResult.DuplicateUnpaidBill)
    }

    @Test
    fun `checkForDuplicate detects duplicate with normalized invoice number`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345"
        )
        val newReceipt = createReceipt(
            invoiceNumber = "INV 12345" // Different separator
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(newReceipt)
        
        assertTrue("Normalized invoice numbers should match", result is DuplicateCheckResult.DuplicateUnpaidBill)
    }

    // ============================================
    // PAID BILL DUPLICATE TESTS
    // ============================================

    @Test
    fun `checkForDuplicate returns DuplicatePaidBill when bill already paid`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345",
            paymentStatus = PaymentStatus.PAID
        )
        val newReceipt = createReceipt(
            invoiceNumber = "INV-12345"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(newReceipt)
        
        assertTrue("Duplicate of paid bill should return DuplicatePaidBill", result is DuplicateCheckResult.DuplicatePaidBill)
        assertEquals((result as DuplicateCheckResult.DuplicatePaidBill).existingReceipt.id, 1L)
        assertTrue("Should block import", result.shouldBlock)
    }

    // ============================================
    // STORNO BILL TESTS
    // ============================================

    @Test
    fun `checkForDuplicate handles STORNO bill for paid invoice`() = runTest {
        val paidReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345",
            paymentStatus = PaymentStatus.PAID
        )
        val stornoReceipt = createReceipt(
            invoiceNumber = "INV-12345",
            isStorno = true
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(paidReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(stornoReceipt)
        
        assertTrue("STORNO for paid bill should return StornoPaidBill", result is DuplicateCheckResult.StornoPaidBill)
        assertTrue("Should block STORNO for paid bill", (result as DuplicateCheckResult.StornoPaidBill).shouldBlock)
    }

    @Test
    fun `checkForDuplicate blocks STORNO when original bill exists`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345",
            paymentStatus = PaymentStatus.UNPAID
        )
        val stornoReceipt = createReceipt(
            invoiceNumber = "INV-12345",
            isStorno = true
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(stornoReceipt)
        
        assertTrue("STORNO should be blocked when original exists", result is DuplicateCheckResult.DuplicateUnpaidBill)
    }

    @Test
    fun `checkForDuplicate replaces STORNO with original`() = runTest {
        val stornoReceipt = createReceipt(
            id = 1,
            invoiceNumber = "INV-12345",
            isStorno = true,
            paymentStatus = PaymentStatus.UNPAID
        )
        val originalReceipt = createReceipt(
            invoiceNumber = "INV-12345",
            isStorno = false
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(stornoReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(originalReceipt)
        
        assertTrue("Original should replace STORNO", result is DuplicateCheckResult.ReplaceExisting)
    }

    // ============================================
    // BILLING PERIOD + AMOUNT TESTS
    // ============================================

    @Test
    fun `checkForDuplicate detects duplicate by period and amount for same merchant`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            merchantName = "EPS SNABDEVANJE",
            totalAmount = BigDecimal("4521.36")
        )
        val newReceipt = createReceipt(
            merchantName = "EPS SNABDEVANJE",
            totalAmount = BigDecimal("4521.36")
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns emptyList()
        coEvery { receiptDao.findByBillingPeriodAndAmount("2025-10", 4521.36) } returns listOf(existingReceipt)
        
        val result = detector.checkForDuplicate(newReceipt, "2025-10")
        
        assertTrue("Same period, amount, merchant should be duplicate", result is DuplicateCheckResult.DuplicateUnpaidBill)
    }

    @Test
    fun `checkForDuplicate allows same period amount for different merchants without same naplatni`() = runTest {
        val existingReceipt = createReceipt(
            id = 1,
            merchantName = "TELEKOM",
            totalAmount = BigDecimal("2890.00"),
            naplatniNumber = "TEL123"
        )
        val newReceipt = createReceipt(
            merchantName = "EPS SNABDEVANJE",
            totalAmount = BigDecimal("2890.00"),
            naplatniNumber = "EPS456"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns emptyList()
        coEvery { receiptDao.findByBillingPeriodAndAmount("2025-10", 2890.00) } returns listOf(existingReceipt)
        
        val result = detector.checkForDuplicate(newReceipt, "2025-10")
        
        assertTrue("Different merchants with different naplatni should not be duplicate", result is DuplicateCheckResult.NoDuplicate)
    }

    // ============================================
    // PREPARE RECEIPT FOR SAVE TESTS
    // ============================================

    @Test
    fun `prepareReceiptForSave hides STORNO receipt`() {
        val stornoReceipt = createReceipt(
            isStorno = true,
            invoiceNumber = "INV-12345"
        )
        
        val prepared = detector.prepareReceiptForSave(stornoReceipt)
        
        assertFalse("STORNO receipt should be hidden", prepared.isVisible)
    }

    @Test
    fun `prepareReceiptForSave keeps regular receipt visible`() {
        val regularReceipt = createReceipt(
            isStorno = false,
            invoiceNumber = "INV-12345"
        )
        
        val prepared = detector.prepareReceiptForSave(regularReceipt)
        
        assertTrue("Regular receipt should remain visible", prepared.isVisible)
    }

    // ============================================
    // CLEANUP TESTS
    // ============================================

    @Test
    fun `cleanupOldStornoBills deletes old STORNO receipts`() = runTest {
        val oldStornoReceipts = listOf(
            createReceipt(id = 1, isStorno = true),
            createReceipt(id = 2, isStorno = true)
        )
        
        coEvery { receiptDao.getOldStornoReceipts(any()) } returns oldStornoReceipts
        coEvery { receiptDao.deleteReceiptsById(any()) } just Runs
        
        val result = detector.cleanupOldStornoBills(retentionDays = 7)
        
        assertEquals(2, result.deleted)
        coVerify { receiptDao.deleteReceiptsById(listOf(1L, 2L)) }
    }

    @Test
    fun `cleanupOldStornoBills returns zero when no old STORNO`() = runTest {
        coEvery { receiptDao.getOldStornoReceipts(any()) } returns emptyList()
        
        val result = detector.cleanupOldStornoBills()
        
        assertEquals(0, result.deleted)
        coVerify(exactly = 0) { receiptDao.deleteReceiptsById(any()) }
    }

    // ============================================
    // SKIP SELF TESTS
    // ============================================

    @Test
    fun `checkForDuplicate skips self when updating`() = runTest {
        val existingReceipt = createReceipt(
            id = 42,
            invoiceNumber = "INV-12345"
        )
        val sameReceipt = createReceipt(
            id = 42, // Same ID - updating itself
            invoiceNumber = "INV-12345"
        )
        
        coEvery { receiptDao.getReceiptsInRange(any(), any()) } returns listOf(existingReceipt)
        coEvery { receiptDao.findByBillingPeriodAndAmount(any(), any()) } returns emptyList()
        
        val result = detector.checkForDuplicate(sameReceipt)
        
        assertTrue("Should skip self when updating", result is DuplicateCheckResult.NoDuplicate)
    }
}

