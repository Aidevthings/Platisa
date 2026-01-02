package com.platisa.app.ui.screens.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.ProductSearchResult
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<ProductSearchResult>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchItems(query).map { results ->
                    if (preferenceManager.hasScannedRestaurantBill) {
                        results
                    } else {
                        // Filter out RESTAURANT items if not enabled
                        results.filter { 
                            com.platisa.app.core.domain.model.BillCategorizer.categorize(it.merchantName) != 
                            com.platisa.app.core.domain.model.BillCategory.RESTAURANT 
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // List of saved fiscal receipts (from camera scanning)
    val fiscalReceipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .map { receipts ->
            receipts.filter { it.originalSource == "CAMERA_FISCAL" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}

