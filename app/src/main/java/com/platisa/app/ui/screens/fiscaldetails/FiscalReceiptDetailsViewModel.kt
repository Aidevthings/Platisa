package com.platisa.app.ui.screens.fiscaldetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.model.ReceiptItem
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiscalReceiptDetailsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FiscalReceiptState>(FiscalReceiptState.Loading)
    val state: StateFlow<FiscalReceiptState> = _state.asStateFlow()

    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items.asStateFlow()

    fun loadReceipt(receiptId: String) {
        viewModelScope.launch {
            try {
                val id = receiptId.toLongOrNull()
                if (id == null) {
                    _state.value = FiscalReceiptState.Error("Invalid receipt ID")
                    return@launch
                }

                val receipt = receiptRepository.getReceiptById(id)
                if (receipt != null) {
                    _state.value = FiscalReceiptState.Success(receipt)
                    
                    // Load items
                    val receiptItems = receiptRepository.getReceiptItems(id)
                    _items.value = receiptItems
                    
                    android.util.Log.d("FiscalReceiptVM", "Loaded receipt ${receipt.merchantName} with ${receiptItems.size} items")
                } else {
                    _state.value = FiscalReceiptState.Error("Receipt not found")
                }
            } catch (e: Exception) {
                _state.value = FiscalReceiptState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun refreshItems() {
        val currentReceipt = (_state.value as? FiscalReceiptState.Success)?.receipt ?: return
        val qrUrl = currentReceipt.qrCodeData ?: return
        
        viewModelScope.launch {
            _state.value = FiscalReceiptState.Loading
            
            // Re-fetch using scraper
            val result = com.platisa.app.core.common.FiscalScraper.scrapeFiscalData(qrUrl)
            
            when (result) {
                is com.platisa.app.core.common.FiscalScraper.ScrapeResult.Success -> {
                    val newItems = result.receipt.items
                    if (newItems.isNotEmpty()) {
                        // Save to DB
                        receiptRepository.insertReceiptItems(newItems, currentReceipt.id)
                        // Update UI
                        _items.value = newItems
                        _state.value = FiscalReceiptState.Success(currentReceipt)
                        android.util.Log.d("FiscalReceiptVM", "Manually refreshed ${newItems.size} items")
                    } else {
                        _state.value = FiscalReceiptState.Success(currentReceipt) // Restore state
                        // Could trigger a toast here via a one-time event if I had that architecture set up
                        android.util.Log.w("FiscalReceiptVM", "Refreshed but still 0 items found")
                    }
                }
                is com.platisa.app.core.common.FiscalScraper.ScrapeResult.Error -> {
                     _state.value = FiscalReceiptState.Success(currentReceipt) // Restore state
                     android.util.Log.e("FiscalReceiptVM", "Manual refresh failed: ${result.message}")
                }
            }
        }
    }
}

sealed class FiscalReceiptState {
    object Loading : FiscalReceiptState()
    data class Success(val receipt: Receipt) : FiscalReceiptState()
    data class Error(val message: String) : FiscalReceiptState()
}

