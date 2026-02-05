package com.platisa.app.ui.screens.billdetails

import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.domain.repository.EpsDataRepository
import com.platisa.app.core.common.VibrationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import android.content.Context
import android.app.AlarmManager
import android.content.Intent
import android.app.PendingIntent
import io.mockk.every

// Mock Logs
class LogMock {
    companion object {
        @JvmStatic
        fun d(tag: String, msg: String): Int = 0
        @JvmStatic
        fun e(tag: String, msg: String, tr: Throwable? = null): Int = 0
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BillDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: BillDetailsViewModel
    private val receiptRepository: ReceiptRepository = mockk(relaxed = true)
    private val epsDataRepository: EpsDataRepository = mockk(relaxed = true)
    private val vibrationHelper: VibrationHelper = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        
        // Mock Android Logger
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        
        // Mock Context & AlarmManager
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.getApplicationContext() } returns context
        
        viewModel = BillDetailsViewModel(
            context,
            receiptRepository,
            epsDataRepository,
            vibrationHelper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scheduleDiscountReminder handles extra text in date string and updates receipt`() = runTest {
        // GIVEN
        val receiptId = 123L
        val dates = listOf("28.01.2026 [Sreda]") // Problematic input
        val receipt = Receipt(
            id = receiptId,
            merchantName = "EPS",
            date = Date(),
            totalAmount = java.math.BigDecimal.TEN,
            imagePath = "",
            metadata = "[DISCOUNT:5%~28.01.2026 [Sreda]~515,90]"
        )

        coEvery { receiptRepository.getReceiptById(receiptId) } returns receipt
        coEvery { receiptRepository.updateReceipt(any()) } just Runs

        // WHEN
        viewModel.scheduleDiscountReminder(receiptId, dates)
        dispatcher.scheduler.advanceUntilIdle()

        // THEN
        // 1. Alarm should be scheduled (parsing succeeded)
        verifyAlarmScheduled()
        
        // 2. Receipt should be updated with [REMINDER_SET]
        coVerify { 
            receiptRepository.updateReceipt(match { 
                it.id == receiptId && it.metadata?.contains("[REMINDER_SET]") == true 
            }) 
        }
    }
    
    private fun verifyAlarmScheduled() {
        io.mockk.verify { alarmManager.set(any(), any(), any()) }
    }
}
