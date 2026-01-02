package com.platisa.app.ui.screens.billdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.domain.repository.EpsDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillDetailsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val receiptRepository: ReceiptRepository,
    private val epsDataRepository: EpsDataRepository
) : ViewModel() {

    private val _billDetails = MutableStateFlow<BillDetailsState>(BillDetailsState.Loading)
    val billDetails: StateFlow<BillDetailsState> = _billDetails.asStateFlow()

    private val _saveQrStatus = MutableStateFlow<SaveQrStatus>(SaveQrStatus.Idle)
    val saveQrStatus: StateFlow<SaveQrStatus> = _saveQrStatus.asStateFlow()

    private val _receiptItems = MutableStateFlow<List<com.platisa.app.core.domain.model.ReceiptItem>>(emptyList())
    val receiptItems: StateFlow<List<com.platisa.app.core.domain.model.ReceiptItem>> = _receiptItems.asStateFlow()

    fun loadBillDetails(billId: String) {
        viewModelScope.launch {
            try {
                val receiptId = billId.toLongOrNull()
                if (receiptId == null) {
                    _billDetails.value = BillDetailsState.Error("Invalid bill ID")
                    return@launch
                }

                // Load receipt
                val receipt = receiptRepository.getReceiptById(receiptId)
                if (receipt != null) {
                    // Load EPS data if available
                    epsDataRepository.getEpsDataByReceiptId(receiptId).collect { epsData ->
                        _billDetails.value = BillDetailsState.Success(
                            receipt = receipt,
                            vtConsumption = epsData?.consumptionVt?.toInt() ?: 0,
                            ntConsumption = epsData?.consumptionNt?.toInt() ?: 0,
                            billType = determineBillType(receipt)
                        )
                    }
                    
                    // Load receipt items (for fiscal receipts)
                    val items = receiptRepository.getReceiptItems(receiptId)
                    _receiptItems.value = items
                    android.util.Log.d("BillDetailsVM", "Loaded ${items.size} items for receipt $receiptId")
                } else {
                    _billDetails.value = BillDetailsState.Error("Bill not found")
                }
            } catch (e: Exception) {
                _billDetails.value = BillDetailsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveQrCode() {
        viewModelScope.launch {
            try {
                _saveQrStatus.value = SaveQrStatus.Saving

                // Get current receipt
                val currentState = _billDetails.value
                if (currentState is BillDetailsState.Success) {
                    val receipt = currentState.receipt

                    // Actually save QR code to gallery with enhanced visual details
                    val formattedAmount = com.platisa.app.core.common.Formatters.formatCurrency(receipt.totalAmount)
                    val formattedDate = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(receipt.date)
                    
                    val qrUri = com.platisa.app.core.common.QrSaveManager.saveEnhancedQrToGallery(
                        context = context,
                        qrData = receipt.qrCodeData ?: "",
                        merchantName = receipt.merchantName,
                        amount = formattedAmount,
                        date = formattedDate
                    )

                    // Update receipt status to PROCESSING (pink/magenta) and store the gallery URI
                    val updatedReceipt = receipt.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PROCESSING,
                        savedQrUri = qrUri?.toString()
                    )
                    receiptRepository.updateReceipt(updatedReceipt)

                    _saveQrStatus.value = SaveQrStatus.Success
                    
                    // Reload to show updated status
                    loadBillDetails(receipt.id.toString())
                } else {
                    _saveQrStatus.value = SaveQrStatus.Error("No bill data available")
                }
            } catch (e: Exception) {
                _saveQrStatus.value = SaveQrStatus.Error(e.message ?: "Failed to save QR code")
            }
        }
    }

    fun markAsPaid() {
        viewModelScope.launch {
            try {
                val currentState = _billDetails.value
                if (currentState is BillDetailsState.Success) {
                    val receipt = currentState.receipt
                    
                    // 1. Delete Generated QR from Gallery (The one used for payment)
                    val savedQrUri = receipt.savedQrUri
                    if (!savedQrUri.isNullOrEmpty()) {
                        com.platisa.app.core.common.QrSaveManager.deleteQrFromGallery(context, savedQrUri)
                        android.util.Log.d("BillDetailsVM", "Deleted generated QR from gallery: $savedQrUri")
                    }

                    // 2. Delete Original Receipt Image (The one scanned/photo taken)
                    // Only delete if it's a file path (starts with / or has manual source)
                    // We check if imagePath is not empty and looks like a file path
                    val originalImagePath = receipt.imagePath
                    if (originalImagePath.isNotBlank()) {
                         try {
                              val file = java.io.File(originalImagePath)
                              if (file.exists()) {
                                  val deleted = file.delete()
                                  android.util.Log.d("BillDetailsVM", "Deleted original receipt image: $originalImagePath (Success: $deleted)")
                              }
                         } catch (e: Exception) {
                             android.util.Log.e("BillDetailsVM", "Failed to delete original image", e)
                         }
                    }

                    // 3. Update status to PAID and Clear Image Paths
                    val updatedReceipt = receipt.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                        savedQrUri = null,
                        imagePath = "" // Clear the path as the file is gone
                    )
                    receiptRepository.updateReceipt(updatedReceipt)
                    
                    // Reload
                    loadBillDetails(receipt.id.toString())
                    
                    com.platisa.app.core.common.SnackbarManager.showMessage("Račun plaćen! Slike su obrisane.")
                }
            } catch (e: Exception) {
                android.util.Log.e("BillDetailsVM", "Error marking as paid", e)
                com.platisa.app.core.common.SnackbarManager.showMessage("Greška: ${e.message}")
            }
        }
    }

    fun resetSaveQrStatus() {
        _saveQrStatus.value = SaveQrStatus.Idle
    }

    private fun determineBillType(receipt: Receipt): BillType {
        // Determine bill type based on merchant name or category
        return when {
            receipt.merchantName.contains("EPS", ignoreCase = true) ||
            receipt.merchantName.contains("Elektro", ignoreCase = true) -> BillType.ELECTRICITY
            
            receipt.merchantName.contains("Vodovod", ignoreCase = true) ||
            receipt.merchantName.contains("Water", ignoreCase = true) -> BillType.WATER
            
            receipt.merchantName.contains("Telekom", ignoreCase = true) ||
            receipt.merchantName.contains("Telenor", ignoreCase = true) ||
            receipt.merchantName.contains("Yettel", ignoreCase = true) ||
            receipt.merchantName.contains("A1", ignoreCase = true) -> BillType.PHONE
            
            receipt.merchantName.contains("SBB", ignoreCase = true) ||
            receipt.merchantName.contains("Orion", ignoreCase = true) ||
            receipt.merchantName.contains("MTS", ignoreCase = true) -> BillType.INTERNET
            
            receipt.merchantName.contains("Upravnik", ignoreCase = true) ||
            receipt.merchantName.contains("Zgrada", ignoreCase = true) ||
            receipt.merchantName.contains("Stan", ignoreCase = true) -> BillType.APARTMENT
            
            else -> BillType.PHONE // Default fallback
        }
    }
}

sealed class BillDetailsState {
    object Loading : BillDetailsState()
    data class Success(
        val receipt: Receipt,
        val vtConsumption: Int,
        val ntConsumption: Int,
        val billType: BillType
    ) : BillDetailsState()
    data class Error(val message: String) : BillDetailsState()
}

sealed class SaveQrStatus {
    object Idle : SaveQrStatus()
    object Saving : SaveQrStatus()
    object Success : SaveQrStatus()
    data class Error(val message: String) : SaveQrStatus()
}

