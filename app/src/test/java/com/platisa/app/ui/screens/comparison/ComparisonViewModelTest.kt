package com.platisa.app.ui.screens.comparison

import com.platisa.app.core.domain.SecureStorage
import com.platisa.app.core.common.VibrationHelper
import com.platisa.app.core.data.preferences.PreferenceManager
import com.platisa.app.core.domain.model.ProductSearchResult
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonViewModelTest {

    private lateinit var viewModel: ComparisonViewModel
    private lateinit var repository: ReceiptRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var secureStorage: SecureStorage
    private lateinit var vibrationHelper: VibrationHelper
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        preferenceManager = mockk()
        secureStorage = mockk<SecureStorage>(relaxed = true)
        vibrationHelper = mockk<VibrationHelper>(relaxed = true)
        
        // Default mocks
        every { preferenceManager.hasScannedRestaurantBill } returns false
        
        viewModel = ComparisonViewModel(repository, preferenceManager, secureStorage, vibrationHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fiscalReceipts should filter out receipts older than 6 months`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val threeMonthsAgo = now - (90L * 24 * 60 * 60 * 1000)
        val eightMonthsAgo = now - (240L * 24 * 60 * 60 * 1000)
        
        val newReceipt = mockk<Receipt>(relaxed = true) {
            every { date } returns Date(threeMonthsAgo)
            every { originalSource } returns "CAMERA_FISCAL"
            every { id } returns 1
        }
        
        val oldReceipt = mockk<Receipt>(relaxed = true) {
            every { date } returns Date(eightMonthsAgo)
            every { originalSource } returns "CAMERA_FISCAL"
            every { id } returns 2
        }
        
        coEvery { repository.getAllReceipts() } returns flowOf(listOf(newReceipt, oldReceipt))

        // When
        val results = viewModel.fiscalReceipts.first()

        // Then
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
    }

    @Test
    fun `searchResults should filter out items older than 6 months`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val threeMonthsAgo = now - (90L * 24 * 60 * 60 * 1000)
        val eightMonthsAgo = now - (240L * 24 * 60 * 60 * 1000)
        
        val newItem = ProductSearchResult(
            id = 1, name = "Milk", merchantName = "Maxi", 
            date = Date(threeMonthsAgo), unitPrice = BigDecimal.TEN, 
            quantity = BigDecimal.ONE, total = BigDecimal.TEN
        )
        
        val oldItem = ProductSearchResult(
            id = 2, name = "Bread", merchantName = "Idea", 
            date = Date(eightMonthsAgo), unitPrice = BigDecimal.TEN, 
            quantity = BigDecimal.ONE, total = BigDecimal.TEN
        )
        
        coEvery { repository.searchItems("query") } returns flowOf(listOf(newItem, oldItem))
        
        viewModel.onSearchQueryChanged("query")

        // When
        // Need to advance time/dispatcher because of debounce(300)
        testDispatcher.scheduler.advanceUntilIdle()
        val results = viewModel.searchResults.first()

        // Then
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
    }
}
