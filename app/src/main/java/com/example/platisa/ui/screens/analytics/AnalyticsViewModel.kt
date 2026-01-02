package com.example.platisa.ui.screens.analytics

import androidx.lifecycle.viewModelScope
import com.example.platisa.core.common.BaseViewModel
import com.example.platisa.core.domain.repository.ReceiptRepository
import com.example.platisa.core.domain.model.EpsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.math.BigDecimal

data class GroceryStats(
    val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
    val average: BigDecimal = BigDecimal.ZERO,
    val topMerchants: List<Pair<String, BigDecimal>> = emptyList()
)

data class PaymentStats(
    val paidTotal: BigDecimal = BigDecimal.ZERO,
    val paidCount: Int = 0,
    val unpaidTotal: BigDecimal = BigDecimal.ZERO,
    val unpaidCount: Int = 0,
    val totalCount: Int = 0
)

data class PharmacyStats(
    val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
    val average: BigDecimal = BigDecimal.ZERO,
    val topMerchants: List<Pair<String, BigDecimal>> = emptyList()
)

data class RestaurantStats(
    val total: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
    val average: BigDecimal = BigDecimal.ZERO,
    val topMerchants: List<Pair<String, BigDecimal>> = emptyList()
)

data class CategoryDetailedStats(
    val category: com.example.platisa.core.domain.model.BillCategory,
    val totalAmount: BigDecimal,
    val count: Int,
    val merchantBreakdown: List<Pair<String, BigDecimal>>
)



@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val preferenceManager: com.example.platisa.core.data.preferences.PreferenceManager,
    private val secureStorage: com.example.platisa.core.domain.SecureStorage
) : BaseViewModel() {

    private val _currency = kotlinx.coroutines.flow.MutableStateFlow(secureStorage.getCurrency())
    val currency: kotlinx.coroutines.flow.StateFlow<String> = _currency.asStateFlow()

    // Dynamic Data Logic
    private val _selectedPeriod = kotlinx.coroutines.flow.MutableStateFlow(TimePeriod.MONTHLY)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _selectedGraphPeriod = kotlinx.coroutines.flow.MutableStateFlow(GraphPeriod.SIX_MONTHS)
    val selectedGraphPeriod: StateFlow<GraphPeriod> = _selectedGraphPeriod.asStateFlow()

    fun setCurrency(newCurrency: String) {
        _currency.value = newCurrency
    }

    // Category Specific Periods (Independent of main graph)
    private val _selectedGroceryPeriod = kotlinx.coroutines.flow.MutableStateFlow(GraphPeriod.MONTHLY)
    val selectedGroceryPeriod: StateFlow<GraphPeriod> = _selectedGroceryPeriod.asStateFlow()

    private val _selectedPharmacyPeriod = kotlinx.coroutines.flow.MutableStateFlow(GraphPeriod.MONTHLY)
    val selectedPharmacyPeriod: StateFlow<GraphPeriod> = _selectedPharmacyPeriod.asStateFlow()

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }
    
    fun setGraphPeriod(period: GraphPeriod) {
        _selectedGraphPeriod.value = period
    }

    fun setGroceryPeriod(period: GraphPeriod) {
        _selectedGroceryPeriod.value = period
    }

    fun setPharmacyPeriod(period: GraphPeriod) {
        _selectedPharmacyPeriod.value = period
    }

    private val _selectedRestaurantPeriod = kotlinx.coroutines.flow.MutableStateFlow(GraphPeriod.MONTHLY)
    val selectedRestaurantPeriod: StateFlow<GraphPeriod> = _selectedRestaurantPeriod.asStateFlow()

    fun setRestaurantPeriod(period: GraphPeriod) {
        _selectedRestaurantPeriod.value = period
    }

    // Feature Flag
    val showRestaurantSection: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        emit(preferenceManager.hasScannedRestaurantBill)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    private fun isDateInPeriod(date: java.util.Date, period: TimePeriod): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        
        calendar.time = date
        val billYear = calendar.get(java.util.Calendar.YEAR)
        val billMonth = calendar.get(java.util.Calendar.MONTH)
        
        return when (period) {
            TimePeriod.MONTHLY -> billYear == currentYear && billMonth == currentMonth
            TimePeriod.SIX_MONTHS -> {
                val sixMonthsAgo = java.util.Calendar.getInstance()
                sixMonthsAgo.add(java.util.Calendar.MONTH, -6)
                date.after(sixMonthsAgo.time)
            }
            TimePeriod.YEARLY -> billYear == currentYear
        }
    }

    // isDateInGraphPeriod moved to GraphPeriod.kt

    // Helper for currency conversion
    private fun convertAmount(amount: BigDecimal, fromCurrency: String, toCurrency: String): BigDecimal {
        val rate = BigDecimal("117.5")
        return if (toCurrency == "EUR" && fromCurrency == "RSD") {
            amount.divide(rate, 2, java.math.RoundingMode.HALF_UP)
        } else if (toCurrency == "RSD" && fromCurrency == "EUR") {
            amount.multiply(rate)
        } else {
            amount
        }
    }

    val totalSpending: StateFlow<java.math.BigDecimal> = combine(
        repository.getAllReceipts(),
        _selectedPeriod,
        _currency
    ) { receiptList, period, currentCurrency ->
        val filtered = receiptList.filter { 
            val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                it.paymentDate ?: it.date
            } else {
                it.date
            }
            isDateInPeriod(dateToUse, period) 
        }
        val amounts = filtered.mapNotNull { 
            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
        }
        if (amounts.isEmpty()) {
            java.math.BigDecimal.ZERO
        } else {
             amounts.fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = java.math.BigDecimal.ZERO
    )
    
    // Spending by category - uses PAYMENT DATE for accurate monthly expense tracking
    val spendingByCategory: StateFlow<Map<com.example.platisa.core.domain.model.BillCategory, java.math.BigDecimal>> = 
        combine(
            repository.getAllReceipts(),
            _selectedPeriod,
            _currency
        ) { receipts, period, currentCurrency ->
            // Only count PAID bills, filtered by when they were actually paid
            val paidReceipts = receipts.filter { 
                it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID 
            }
            val filtered = paidReceipts.filter { 
                val dateToUse = it.paymentDate ?: it.date // Use paymentDate if available, fallback to date
                isDateInPeriod(dateToUse, period) 
            }
            
            filtered.groupBy { it.category }
                .mapValues { entry ->
                    val receiptsInCategory = entry.value
                    
                    val amounts = receiptsInCategory.mapNotNull { 
                        it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                    }
                    if (amounts.isEmpty()) {
                        java.math.BigDecimal.ZERO
                    } else {
                        amounts.fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    }
                }
                .filterValues { it > java.math.BigDecimal.ZERO }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    // Spending Trends
    val spendingTrends: StateFlow<List<kotlin.Pair<String, Float>>> = combine(
        repository.getAllReceipts(),
        _selectedGraphPeriod,
        _currency
    ) { receipts, graphPeriod, currentCurrency ->
            val calendar = java.util.Calendar.getInstance()
            val result = mutableListOf<kotlin.Pair<String, Float>>()
            
            when (graphPeriod) {
                GraphPeriod.MONTHLY -> {
                    // Weekly breakdown for current month (User Request: "make a bar for each week")
                    val currentYear = calendar.get(java.util.Calendar.YEAR)
                    val currentMonth = calendar.get(java.util.Calendar.MONTH)
                    
                    // Determine how many weeks are in this month (usually 4-6)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    val maxWeeks = calendar.getActualMaximum(java.util.Calendar.WEEK_OF_MONTH)

                    for (week in 1..maxWeeks) {
                        // Label: "Ned 1", "Ned 2"...
                        val weekLabel = "Ned $week"
                        
                        val weekReceipts = receipts.filter { 
                            val c = java.util.Calendar.getInstance()
                            // First set minimal time to avoid potential timezone/overflow issues when setting fields later? 
                            // Actually just ensuring we use the correct date object is enough.
                            val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                                it.paymentDate ?: it.date
                            } else {
                                it.date
                            }
                            c.time = dateToUse
                            
                            c.get(java.util.Calendar.YEAR) == currentYear &&
                            c.get(java.util.Calendar.MONTH) == currentMonth &&
                            c.get(java.util.Calendar.WEEK_OF_MONTH) == week
                        }
                        
                        val totalForWeek = weekReceipts.mapNotNull { 
                            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                        }
                            .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                            
                        result.add(kotlin.Pair(weekLabel, totalForWeek.toFloat()))
                    }
                }
                
                // THREE_MONTHS case removed


                GraphPeriod.SIX_MONTHS -> {
                    val dateFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                    for (i in 5 downTo 0) {
                        val targetCalendar = java.util.Calendar.getInstance()
                        targetCalendar.add(java.util.Calendar.MONTH, -i)
                        val targetMonth = targetCalendar.get(java.util.Calendar.MONTH)
                        val targetYear = targetCalendar.get(java.util.Calendar.YEAR)
                        val monthLabel = dateFormat.format(targetCalendar.time).uppercase()
                        
                        val monthReceipts = receipts.filter { 
                            val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                                it.paymentDate ?: it.date
                            } else {
                                it.date
                            }
                            calendar.time = dateToUse
                            calendar.get(java.util.Calendar.YEAR) == targetYear && 
                            calendar.get(java.util.Calendar.MONTH) == targetMonth
                        }
                        val total = monthReceipts.mapNotNull { 
                            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                        }
                            .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                        result.add(kotlin.Pair(monthLabel, total.toFloat()))
                    }
                }
                
                GraphPeriod.THIS_YEAR -> {
                    val dateFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                    val currentYear = calendar.get(java.util.Calendar.YEAR)
                    for (month in 0..11) {
                         val targetCalendar = java.util.Calendar.getInstance()
                         targetCalendar.set(java.util.Calendar.YEAR, currentYear)
                         targetCalendar.set(java.util.Calendar.MONTH, month)
                         targetCalendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                         
                         val monthLabel = dateFormat.format(targetCalendar.time).uppercase()
                         
                         val monthReceipts = receipts.filter { 
                            val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                                it.paymentDate ?: it.date
                            } else {
                                it.date
                            }
                            calendar.time = dateToUse
                            calendar.get(java.util.Calendar.YEAR) == currentYear && 
                            calendar.get(java.util.Calendar.MONTH) == month
                         }
                         val total = monthReceipts.mapNotNull { 
                            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                         }
                            .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                         result.add(kotlin.Pair(monthLabel, total.toFloat()))
                    }
                }
            }
            
            // Normalize
            // Return raw values (UI handles normalization and scaling)
            val finalResult = result.map { 
                kotlin.Pair(it.first, it.second)
            }
            finalResult
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val topMerchants: StateFlow<List<kotlin.Pair<String, java.math.BigDecimal>>> = 
        combine(
            repository.getAllReceipts(),
            _selectedPeriod,
            _currency
        ) { receipts, period, currentCurrency ->
            val filtered = receipts.filter { 
                val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                    it.paymentDate ?: it.date
                } else {
                    it.date
                }
                isDateInPeriod(dateToUse, period) 
            }
            
            filtered.groupBy { it.merchantName.trim().uppercase() }
                .mapValues { (normalizedName, receiptsForMerchant) ->
                    val displayName = receiptsForMerchant.firstOrNull()?.merchantName ?: normalizedName
                    val amounts = receiptsForMerchant.mapNotNull { 
                        it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                    }
                    val totalAmount = if (amounts.isEmpty()) {
                        java.math.BigDecimal.ZERO
                    } else {
                        amounts.fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    }
                    kotlin.Pair(displayName, totalAmount)
                }
                .values
                .sortedByDescending { it.second }
                .take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Grocery Specific Stats
    val groceryStats: StateFlow<GroceryStats> = combine(
        repository.getAllReceipts(),
        _selectedGroceryPeriod,
        _currency
    ) { receipts, period, currentCurrency ->
        val groceryReceipts = receipts.filter { 
            isDateInGraphPeriod(it.date, period) && it.category == com.example.platisa.core.domain.model.BillCategory.GROCERY
        }
        
        if (groceryReceipts.isEmpty()) {
            GroceryStats()
        } else {
            val total = groceryReceipts.mapNotNull { 
                it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
            }
                .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                
            val count = groceryReceipts.size
            
            val average = if (count > 0) {
                total.divide(BigDecimal(count), java.math.MathContext.DECIMAL128)
            } else BigDecimal.ZERO
            
            val topMerchants = groceryReceipts
                .groupBy { it.merchantName.trim().uppercase() }
                .mapValues { (normalizedName, merchantReceipts) ->
                    val displayName = merchantReceipts.firstOrNull()?.merchantName ?: normalizedName
                    val merchantTotal = merchantReceipts.mapNotNull { 
                        it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                    }
                        .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    displayName to merchantTotal
                }
                .values
                .sortedByDescending { it.second }
                .take(3)
                
            GroceryStats(
                total = total,
                count = count,
                average = average,
                topMerchants = topMerchants
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroceryStats()
        )
        
    // Pharmacy Specific Stats
    val pharmacyStats: StateFlow<PharmacyStats> = combine(
        repository.getAllReceipts(),
        _selectedPharmacyPeriod,
        _currency
    ) { receipts, period, currentCurrency ->
        val pharmacyReceipts = receipts.filter { 
            isDateInGraphPeriod(it.date, period) && it.category == com.example.platisa.core.domain.model.BillCategory.PHARMACY
        }
        
        if (pharmacyReceipts.isEmpty()) {
            PharmacyStats()
        } else {
            val total = pharmacyReceipts.mapNotNull { 
                it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
            }
                .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                
            val count = pharmacyReceipts.size
            
            val average = if (count > 0) {
                total.divide(BigDecimal(count), java.math.MathContext.DECIMAL128)
            } else BigDecimal.ZERO
            
            val topMerchants = pharmacyReceipts
                .groupBy { it.merchantName.trim().uppercase() }
                .mapValues { (normalizedName, merchantReceipts) ->
                    val displayName = merchantReceipts.firstOrNull()?.merchantName ?: normalizedName
                    val merchantTotal = merchantReceipts.mapNotNull { 
                        it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                    }
                        .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    displayName to merchantTotal
                }
                .values
                .sortedByDescending { it.second }
                .take(3)
                
            PharmacyStats(
                total = total,
                count = count,
                average = average,
                topMerchants = topMerchants
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PharmacyStats()
        )
        
    // Restaurant Specific Stats
    val restaurantStats: StateFlow<RestaurantStats> = combine(
        repository.getAllReceipts(),
        _selectedRestaurantPeriod,
        _currency
    ) { receipts, period, currentCurrency ->
        val restaurantReceipts = receipts.filter { 
            isDateInGraphPeriod(it.date, period) && it.category == com.example.platisa.core.domain.model.BillCategory.RESTAURANT
        }
        
        if (restaurantReceipts.isEmpty()) {
            RestaurantStats()
        } else {
            val total = restaurantReceipts.mapNotNull { 
                it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
            }
                .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                
            val count = restaurantReceipts.size
            
            val average = if (count > 0) {
                total.divide(BigDecimal(count), java.math.MathContext.DECIMAL128)
            } else BigDecimal.ZERO
            
            val topMerchants = restaurantReceipts
                .groupBy { it.merchantName.trim().uppercase() }
                .mapValues { (normalizedName, merchantReceipts) ->
                    val displayName = merchantReceipts.firstOrNull()?.merchantName ?: normalizedName
                    val merchantTotal = merchantReceipts.mapNotNull { 
                        it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                    }
                        .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    displayName to merchantTotal
                }
                .values
                .sortedByDescending { it.second }
                .take(3)
                
            RestaurantStats(
                total = total,
                count = count,
                average = average,
                topMerchants = topMerchants
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RestaurantStats()
        )

    // Payment Stats (Paid vs Unpaid)
    // Filter out GROCERY and PHARMACY as they are FISCAL and inherently PAID (Market tab)
    // Focused on front-page Bills (Utilities) that need tracking/payment
    // IMPORTANT: For PAID bills, we use paymentDate (when the bill was paid)
    //            For UNPAID bills, we use date (when the bill was issued/due)
    val paymentStats: StateFlow<PaymentStats> = combine(
        repository.getAllReceipts(),
        _selectedPeriod,
        _currency
    ) { receipts, period, currentCurrency ->
        // Filter to include ONLY utility categories (same as HomeViewModel for consistency)
        val utilityBills = receipts.filter {
            it.category == com.example.platisa.core.domain.model.BillCategory.ELECTRICITY ||
            it.category == com.example.platisa.core.domain.model.BillCategory.WATER ||
            it.category == com.example.platisa.core.domain.model.BillCategory.TELECOM ||
            it.category == com.example.platisa.core.domain.model.BillCategory.GAS ||
            it.category == com.example.platisa.core.domain.model.BillCategory.UTILITIES ||
            it.category == com.example.platisa.core.domain.model.BillCategory.OTHER
        }
        
        // PAID bills: Filter by paymentDate (when payment was made)
        val paidBills = utilityBills.filter { 
            it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID &&
            isDateInPeriod(it.paymentDate ?: it.date, period)
        }
        
        // UNPAID bills: Show ALL unpaid bills regardless of period
        // This ensures old unpaid bills are always visible as outstanding debt
        val unpaidBills = utilityBills.filter { 
            it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.UNPAID || 
            it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PROCESSING
        }
        
        val paidTotal = paidBills.mapNotNull { 
            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
        }
             .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
             
        val unpaidTotal = unpaidBills.mapNotNull { 
            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
        }
             .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
             
        PaymentStats(
            paidTotal = paidTotal,
            paidCount = paidBills.size,
            unpaidTotal = unpaidTotal,
            unpaidCount = unpaidBills.size,
            totalCount = paidBills.size + unpaidBills.size
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PaymentStats()
        )

    val categoryDetails: StateFlow<List<CategoryDetailedStats>> = combine(
        repository.getAllReceipts(),
        _selectedPeriod,
        _currency
    ) { receipts, period, currentCurrency ->
        val relevantReceipts = receipts.filter {
            val dateToUse = if (it.paymentStatus == com.example.platisa.core.domain.model.PaymentStatus.PAID) {
                it.paymentDate ?: it.date
            } else {
                it.date
            }
            isDateInPeriod(dateToUse, period)
        }

        relevantReceipts.groupBy { it.category }
            .map { (category, categoryReceipts) ->
                val amounts = categoryReceipts.mapNotNull {
                    it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                }
                val total = if (amounts.isEmpty()) BigDecimal.ZERO else amounts.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                val count = categoryReceipts.size

                val merchants = categoryReceipts
                    .groupBy { it.merchantName.trim().uppercase() }
                    .mapValues { (normalizedName, merchantReceipts) ->
                        val displayName = merchantReceipts.firstOrNull()?.merchantName?.trim() ?: normalizedName
                        val merchantTotal = merchantReceipts.mapNotNull {
                            it.totalAmount?.let { amount -> convertAmount(amount, it.currency, currentCurrency) }
                        }
                            .fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                        displayName to merchantTotal
                    }
                    .values
                    .filter { it.first.isNotBlank() && it.first.length > 1 } // Filter out empty or single chars
                    .sortedByDescending { it.second }
                    .toList()

                CategoryDetailedStats(category, total, count, merchants)
            }
            .sortedByDescending { it.totalAmount }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
