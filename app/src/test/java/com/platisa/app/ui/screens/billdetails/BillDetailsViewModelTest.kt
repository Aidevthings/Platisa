package com.platisa.app.ui.screens.billdetails

import android.content.Context
import com.platisa.app.core.common.VibrationHelper
import com.platisa.app.core.data.repository.EpsDataRepository
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class BillDetailsViewModelTest {

    private lateinit var viewModel: BillDetailsViewModel
    private val context: Context = mockk(relaxed = true)
    private val receiptRepository: ReceiptRepository = mockk(relaxed = true)
    private val epsDataRepository: EpsDataRepository = mockk(relaxed = true)
    private val vibrationHelper: VibrationHelper = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BillDetailsViewModel(context, receiptRepository, epsDataRepository, vibrationHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBillDetails SHOULD detect partial debt payment WHEN past bills are locally paid`() = runTest {
        // GIVEN a scenario where we have a new bill claiming debt, but we paid previous bills locally

        // 1. Setup the "Latest Bill" (The one we are viewing)
        // It claims: Total = 3000, Current = 1000. So Debt = 2000.
        val billDate = Date() // Today
        val merchantName = "EPS Snabdevanje"
        val latestReceipt = Receipt(
            id = 100,
            merchantName = merchantName,
            date = billDate,
            totalAmount = BigDecimal("3000.00"),
            currentMonthAmount = BigDecimal("1000.00"),
            previousDebtAmount = BigDecimal("2000.00"), // Claimed Debt matches calculated debt
            paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.UNPAID,
            imagePath = "",
            invoiceNumber = "123-456",
            qrCodeData = "dummy_qr"
        )

        // 2. Setup Repository Mocks to simulate "Past Bills are Paid"
        // We simulate that the user has marked all past bills as paid in the app.
        // So getUnpaidPastBillsSum should return 0.0
        coEvery { receiptRepository.getReceiptById(100) } returns latestReceipt
        coEvery { receiptRepository.isLatestReceipt(merchantName, billDate) } returns true
        
        // This is the CRITICAL part: 
        // hasAnyPastBills = TRUE (We have history)
        // getUnpaidPastBillsSum = 0.0 (But we paid it all locally)
        coEvery { receiptRepository.hasAnyPastBills(merchantName, billDate.time) } returns true
        coEvery { receiptRepository.getUnpaidPastBillsSum(merchantName, billDate.time) } returns 0.0

        // WHEN we load the bill details
        viewModel.loadBillDetails("100")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN the safeguard should be triggered
        val state = viewModel.billDetails.value
        assertTrue("State should be Success", state is BillDetailsState.Success)
        
        val successState = state as BillDetailsState.Success
        
        // Verify Safeguard Logic:
        // Bill Debt (2000) != Local Unpaid (0) -> Difference (2000) > Tolerance (200) -> isDebtPartiallyPaid should be TRUE
        println("Safeguard Result: isDebtPartiallyPaid = ${successState.isDebtPartiallyPaid}")
        println("Local Unpaid: ${successState.localUnpaidSum}, Bill Debt: ${successState.billDebt}")
        
        assertTrue(
            "Safeguard FAILED! Expected isDebtPartiallyPaid=true because local unpaid debt is 0 but bill claims 2000.",
            successState.isDebtPartiallyPaid
        )
    }
}
