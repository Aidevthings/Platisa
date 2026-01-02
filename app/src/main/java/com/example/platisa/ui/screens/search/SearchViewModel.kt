package com.example.platisa.ui.screens.search

import androidx.lifecycle.viewModelScope
import com.example.platisa.core.common.BaseViewModel
import com.example.platisa.core.domain.model.Receipt
import com.example.platisa.core.domain.model.ProductSearchResult
import com.example.platisa.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : BaseViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate = _endDate.asStateFlow()

    private val _minAmount = MutableStateFlow<Double?>(null)
    val minAmount = _minAmount.asStateFlow()

    private val _maxAmount = MutableStateFlow<Double?>(null)
    val maxAmount = _maxAmount.asStateFlow()

    val searchResults: StateFlow<List<SearchUiItem>> = combine(
        _searchQuery,
        _startDate,
        _endDate,
        _minAmount,
        _maxAmount
    ) { query, start, end, min, max ->
        FilterParams(query, start, end, min, max)
    }.debounce(300)
    .flatMapLatest { params ->
        if (params.query.isNotBlank()) {
            // Parallel Search: Items AND Receipts
            combine(
                repository.searchItems(params.query),
                repository.searchReceipts(params.query)
            ) { products, receipts ->
                val productItems = products.map { SearchUiItem.ProductItem(it) }
                val receiptItems = receipts.map { SearchUiItem.ReceiptItem(it) }
                // Show Products first, then Receipts
                productItems + receiptItems
            }
        } else if (params.start != null && params.end != null) {
            repository.getReceiptsByFilter(
                params.start,
                params.end,
                params.min ?: 0.0,
                params.max ?: Double.MAX_VALUE
            ).map { list -> list.map { SearchUiItem.ReceiptItem(it) } }
        } else {
            repository.getAllReceipts().map { list -> list.map { SearchUiItem.ReceiptItem(it) } }
        }
    }.map { list ->
        // Filter logic if needed, but for now we just pass through
        // Note: Repository filtering for categories applies to Receipts mostly.
        // For Products, we might want to ensure they belong to grocery bills if needed, but DAO usually handles joins.
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDateRangeChanged(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun onAmountRangeChanged(min: Double?, max: Double?) {
        _minAmount.value = min
        _maxAmount.value = max
    }

    private data class FilterParams(
        val query: String,
        val start: Long?,
        val end: Long?,
        val min: Double?,
        val max: Double?
    )
}

sealed interface SearchUiItem {
    data class ReceiptItem(val receipt: Receipt) : SearchUiItem
    data class ProductItem(val product: ProductSearchResult) : SearchUiItem
}
