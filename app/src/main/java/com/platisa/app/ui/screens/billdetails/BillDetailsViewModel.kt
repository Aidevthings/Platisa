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
    private val epsDataRepository: EpsDataRepository,
    private val vibrationHelper: com.platisa.app.core.common.VibrationHelper
) : ViewModel() {

    fun vibrate(type: com.platisa.app.core.common.VibrationHelper.HapticType) {
        vibrationHelper.vibrate(type)
    }

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
                    val isLatest = receiptRepository.isLatestReceipt(receipt.merchantName, receipt.date)
                    
                    // Load EPS data if available
                    epsDataRepository.getEpsDataByReceiptId(receiptId).collect { epsData ->
                        
                        // DEBT SAFETY CHECK:
                        // Compare "Previous Debt" from the bill vs "Unpaid Local Bills".
                        // If Bill says debt is 5000, but we only have 2000 unpaid locally, it means
                        // we already paid 3000 separately. We should WARN the user not to pay total debt.
                        var isDebtPartiallyPaid = false
                        var localUnpaidSum = 0.0
                        
                        if (receipt.currentMonthAmount != null && receipt.totalAmount != null) {
                             val totalAmount = receipt.totalAmount.toDouble()
                             val currentAmount = receipt.currentMonthAmount.toDouble()
                             val billDebt = totalAmount - currentAmount
                             
                             if (billDebt > 0) {
                                 localUnpaidSum = receiptRepository.getUnpaidPastBillsSum(receipt.merchantName, receipt.date.time)
                                 
                                 // SMART ELASTICITY logic:
                                 // Tolerance = 10% of total debt
                                 val tolerance = billDebt * 0.1
                                 
                                 // If we have SOME local bills, but the sum is significantly LESS than Bill Debt
                                 // It means a chunk is missing or paid separately. Risk of double payment!
                                 // IF localUnpaidSum is 0, we simply don't have the bills scanned yet, so we don't BLOCK.
                                 if (localUnpaidSum > 0 && localUnpaidSum < (billDebt - tolerance)) {
                                     isDebtPartiallyPaid = true
                                     android.util.Log.w("BillDetailsVM", "⚠️ DEBT MISMATCH: Bill Debt is $billDebt. Local Unpaid is $localUnpaidSum. Partial Payment Detected!")
                                 }
                             }
                        }

                        _billDetails.value = BillDetailsState.Success(
                            receipt = receipt,
                            vtConsumption = epsData?.consumptionVt?.toInt() ?: 0,
                            ntConsumption = epsData?.consumptionNt?.toInt() ?: 0,
                            billType = determineBillType(receipt),
                            isLatestForMerchant = isLatest,
                            isDebtPartiallyPaid = isDebtPartiallyPaid,
                            localUnpaidSum = localUnpaidSum,
                            billDebt = if (receipt.totalAmount != null && receipt.currentMonthAmount != null) (receipt.totalAmount.toDouble() - receipt.currentMonthAmount.toDouble()) else 0.0
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

    fun saveQrCode(payTotalDebt: Boolean = false) {
        viewModelScope.launch {
            try {
                _saveQrStatus.value = SaveQrStatus.Saving

                // Get current receipt
                val currentState = _billDetails.value
                if (currentState is BillDetailsState.Success) {
                    val receipt = currentState.receipt

                    // SAFEGUARD: Delete any existing saved QR before saving a new one
                    // This prevents orphaned QR files if user somehow triggers save again
                    if (!receipt.savedQrUri.isNullOrEmpty()) {
                        android.util.Log.d("BillDetailsVM", "Deleting existing QR before saving new one: ${receipt.savedQrUri}")
                        com.platisa.app.core.common.QrSaveManager.deleteQrFromGallery(context, receipt.savedQrUri)
                    }

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
                    // Also store a flag if Total Debt was selected, so markAsPaid can cascade later
                    val updatedReceipt = receipt.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PROCESSING,
                        savedQrUri = qrUri?.toString(),
                        metadata = if (payTotalDebt) {
                            (receipt.metadata ?: "") + " [TOTAL_DEBT_SELECTED]"
                        } else {
                            receipt.metadata
                        }
                    )
                    
                    android.util.Log.d("BillDetailsVM", "📋 saveQrCode: Setting status to PROCESSING for receipt ${receipt.id}")
                    android.util.Log.d("BillDetailsVM", "📋 saveQrCode: payTotalDebt=$payTotalDebt, metadata=${updatedReceipt.metadata}")
                    android.util.Log.d("BillDetailsVM", "📋 saveQrCode: New Status = ${updatedReceipt.paymentStatus}")
                    
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

    fun markAsPaid(payTotalDebt: Boolean = false) {
        viewModelScope.launch {
            try {
                val currentState = _billDetails.value
                if (currentState is BillDetailsState.Success) {
                    val receipt = currentState.receipt
                    
                    // 1. Delete Generated QR from Gallery (The one used for payment)
                    val savedQrUri = receipt.savedQrUri
                    if (!savedQrUri.isNullOrEmpty()) {
                        com.platisa.app.core.common.QrSaveManager.deleteQrFromGallery(context, savedQrUri)
                    }

                    // 2. Delete Original Receipt Image (The one scanned/photo taken)
                    val originalImagePath = receipt.imagePath
                    if (originalImagePath.isNotBlank()) {
                         try {
                              val file = java.io.File(originalImagePath)
                              if (file.exists()) {
                                  file.delete()
                              }
                         } catch (e: Exception) {
                             android.util.Log.e("BillDetailsVM", "Failed to delete original image", e)
                         }
                    }

                    // CASCADE PAYMENT - Check if Total Debt was selected when QR was saved
                    val metadataContainsFlag = receipt.metadata?.contains("[TOTAL_DEBT_SELECTED]") == true
                    val shouldCascade = payTotalDebt || metadataContainsFlag
                    
                    if (shouldCascade) {
                        processCascadePayment(receipt.merchantName, receipt.id, receipt.date.time)
                    }

                    // 3. Update status to PAID and Clear Image Paths
                    // Also clean up the TOTAL_DEBT_SELECTED flag from metadata
                    val cleanedMetadata = receipt.metadata?.replace(" [TOTAL_DEBT_SELECTED]", "") ?: ""
                    val updatedReceipt = receipt.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                        savedQrUri = null, // Clear the URI as we deleted the file
                        imagePath = "", // Clear the path as the file is gone
                        metadata = cleanedMetadata
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

    private suspend fun processCascadePayment(merchantName: String, excludeId: Long, currentBillDate: Long) {
        try {
            receiptRepository.markPastBillsAsPaid(merchantName, excludeId, currentBillDate)
        } catch (e: Exception) {
            android.util.Log.e("BillDetailsVM", "Failed to cascade payment", e)
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
        val billType: BillType,
        val isLatestForMerchant: Boolean = true,
        val isDebtPartiallyPaid: Boolean = false,
        val localUnpaidSum: Double = 0.0,
        val billDebt: Double = 0.0
    ) : BillDetailsState()
    data class Error(val message: String) : BillDetailsState()
}

sealed class SaveQrStatus {
    object Idle : SaveQrStatus()
    object Saving : SaveQrStatus()
    object Success : SaveQrStatus()
    data class Error(val message: String) : SaveQrStatus()
}

