package com.platisa.app.ui.screens.billdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.DiscountRow
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.domain.repository.EpsDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    
    fun scheduleDiscountReminder(receiptId: Long, deadlines: List<String>) {
        viewModelScope.launch {
            try {
                android.util.Log.d("BillDetailsVM", "🔔 Starting scheduleDiscountReminder for Receipt ID: $receiptId. Deadlines: $deadlines")
                
                var scheduledCount = 0
                // User Request: "One reminder for each date. Not three reminders for one date."
                // Logic: Deduplicate dates. Schedule T-1 for each unique date.
                
                val uniqueDeadlines = deadlines
                    .map { it.trim().removeSuffix(".") }
                    .filter { it.isNotEmpty() }
                    .distinct()
                
                android.util.Log.d("BillDetailsVM", "🔔 Unique Deadlines: $uniqueDeadlines")
                
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())

                for (rawDateStr in uniqueDeadlines) {
                    // Extract date pattern d.M.yyyy or dd.MM.yyyy ignoring extra text
                    val match = Regex("""(\d{1,2}\.\d{1,2}\.\d{4})""").find(rawDateStr)
                    val cleanDateStr = match?.value 
                    
                    if (cleanDateStr == null) {
                        android.util.Log.w("BillDetailsVM", "⚠️ COULD NOT PARSE DATE from string: '$rawDateStr'")
                        continue
                    }
                    
                    val date = sdf.parse(cleanDateStr) ?: continue

                    // Schedule for T-1 (One day before)
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = date
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    
                    // Set time to 09:00 AM
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    
                    // Target trigger time
                    var triggerTime = calendar.timeInMillis
                    
                    // Check if target trigger time is in past
                    if (triggerTime < System.currentTimeMillis()) {
                        // If T-1 09:00 AM is in the past, check if the actual deadline is still in the future
                        // (We use date.time which is midnight, so we check if current time is before the deadline DAY)
                        if (date.time + (24 * 60 * 60 * 1000) > System.currentTimeMillis()) {
                            android.util.Log.d("BillDetailsVM", "⏱️ Target time passed but deadline future. Scheduling for Now + 2 mins.")
                            triggerTime = System.currentTimeMillis() + (2 * 60 * 1000)
                        } else {
                            android.util.Log.d("BillDetailsVM", "⏭️ Skipping past deadline: ${date}")
                            continue
                        }
                    }

                    // Schedule Alarm
                    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    
                    // Determine merchant name for notification
                    val receipt = receiptRepository.getReceiptById(receiptId)
                    val merchantForNotification = if (receipt?.merchantName?.lowercase()?.contains("infostan") == true) {
                        "JKP Infostan"
                    } else {
                        "EPS"
                    }

                    val intent = android.content.Intent(context, com.platisa.app.core.notification.DiscountReminderReceiver::class.java).apply {
                        putExtra(com.platisa.app.core.notification.DiscountReminderReceiver.EXTRA_MERCHANT_NAME, merchantForNotification) 
                        putExtra(com.platisa.app.core.notification.DiscountReminderReceiver.EXTRA_EXPIRY_DATE, cleanDateStr)
                    }
                    
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        cleanDateStr.hashCode(), // Unique RequestCode per unique date
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP, 
                        triggerTime, 
                        pendingIntent
                    )
                    android.util.Log.d("BillDetailsVM", "✅ Scheduled reminder for $cleanDateStr at ${java.util.Date(triggerTime)}")
                    scheduledCount++
                }
                
                // Feedback
                if (scheduledCount > 0) {
                     val message = if (scheduledCount == 1) "Podsetnik zakazan." else "$scheduledCount podsetnika zakazana."
                     com.platisa.app.core.common.SnackbarManager.showMessage(message)
                } else {
                     android.util.Log.w("BillDetailsVM", "⚠️ No valid future dates found. scheduledCount=0.")
                     com.platisa.app.core.common.SnackbarManager.showMessage("Nema validnih datuma u budućnosti.")
                }
                
                // CRITICAL: ALWAYS PERSIST, even if scheduledCount is 0 (maybe user clicked it but date was past, we still want to suppress popup)
                // Actually, if scheduledCount is 0 because all dates are past, we SHOULD suppress it too.
                // But for safety, let's just make sure we save correct metadata.
                
                val receipt = receiptRepository.getReceiptById(receiptId)
                if (receipt != null) {
                     val currentMeta = receipt.metadata ?: ""
                     android.util.Log.d("BillDetailsVM", "💾 Current Metadata before update: '$currentMeta'")
                     
                     if (!currentMeta.contains("[REMINDER_SET]")) {
                         val updatedReceipt = receipt.copy(metadata = "$currentMeta [REMINDER_SET]")
                         android.util.Log.d("BillDetailsVM", "💾 SAVING Metadata: '${updatedReceipt.metadata}'")
                         receiptRepository.updateReceipt(updatedReceipt)
                         
                         // Refresh UI to hide popup permanently
                         loadBillDetails(receiptId.toString())
                     } else {
                         android.util.Log.d("BillDetailsVM", "ℹ️ Metadata already contains [REMINDER_SET]. Skipping update.")
                     }
                } else {
                    android.util.Log.e("BillDetailsVM", "❌ Could not find receipt with ID $receiptId to update metadata!")
                }

            } catch (e: Exception) {
                android.util.Log.e("BillDetailsVM", "❌ Failed to schedule reminder", e)
                com.platisa.app.core.common.SnackbarManager.showMessage("Greška pri zakazivanju.")
            }
        }
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
                    
                    // 1. Get EPS data if it exists (Optional)
                    val epsData = epsDataRepository.getEpsDataByReceiptId(receiptId).firstOrNull()
                    
                    // 2. DEBT SAFETY CHECK (Merchant Agnostic):
                    // Compare "Previous Debt" from the bill vs "Unpaid Local Bills".
                    var smartTotalDebt = 0.0
                    var paidPastBillsSum = 0.0
                    var isDebtPartiallyPaid = false
                    var localUnpaidSum = 0.0
                    var billDebt = 0.0
                    
     
     
                    // 2. DEBT SAFETY CHECK (Merchant Agnostic):
                    // Compare "Previous Debt" from the bill vs "Unpaid Local Bills".
                    
                    if (receipt.currentMonthAmount != null || receipt.previousDebtAmount != null) {
                        // Priority 1: Use explicit Previous Debt field if available (Most accurate)
                        if (receipt.previousDebtAmount != null && receipt.previousDebtAmount > java.math.BigDecimal.ZERO) {
                            billDebt = receipt.previousDebtAmount.toDouble()
                        } 
                        // Priority 2: Infer debt from Total - Current (Fallback)
                        else if (receipt.totalAmount != null && receipt.currentMonthAmount != null) {
                             val totalAmount = receipt.totalAmount.toDouble()
                             val currentAmount = receipt.currentMonthAmount.toDouble()
                             billDebt = totalAmount - currentAmount
                        }

                        if (billDebt > 0.01) { // Ignore rounding errors
                             // Calculate what we ALREADY paid locally
                             paidPastBillsSum = receiptRepository.getPaidPastBillsSum(receipt.merchantName, receipt.date.time)
                             
                             localUnpaidSum = receiptRepository.getUnpaidPastBillsSum(receipt.merchantName, receipt.date.time)
                             val hasAnyPastBills = receiptRepository.hasAnyPastBills(receipt.merchantName, receipt.date.time)
                             
                             // SMART ELASTICITY logic:
                             val tolerance = Math.max(billDebt * 0.1, 100.0)
                             val diff = Math.abs(localUnpaidSum - billDebt)
                             
                             if (hasAnyPastBills && diff > tolerance) {
                                 isDebtPartiallyPaid = true
                                 
                                 // SMART CALCULATION: (Current + Previous) - Locally Paid Past Bills
                                 
                                 // We use billDebt (which is confirmed Previous Debt) + Current Amount as the TRUE total
                                 // We ignore receipt.totalAmount here because it might just be the scan value of the slip (often just current month)
                                 val currentDouble = receipt.currentMonthAmount?.toDouble() ?: 0.0
                                 val truePaperTotal = currentDouble + billDebt
                                 
                                 smartTotalDebt = Math.max(truePaperTotal - paidPastBillsSum, currentDouble)
                                 
                                 android.util.Log.d("BillDetailsVM", "🧠 SMART DEBT: TrueTotal($currentDouble + $billDebt) = $truePaperTotal. Paid=$paidPastBillsSum. Result=$smartTotalDebt")
                             }
                        }
                    }

                    // Lazy calculate discounts for LATEST bill only
                    // This satisfies the requirement: "We don't scan anything else but the latest bill only"
                    val electricityBaseCost = parseBaseCostFromMetadata(receipt.metadata)
                    val discountDeadline = parseDeadlineFromMetadata(receipt.metadata)
                    val discountThreshold = parseDiscountThresholdFromMetadata(receipt.metadata)
                    val infostanDeadline = parseInfostanDeadlineFromMetadata(receipt.metadata)
                    
                    // Use electricity base cost (Page 2) if available, otherwise fallback to total current month (Page 1)
                    val baseForDiscount = electricityBaseCost ?: receipt.currentMonthAmount
                    
                    val discountTable = if (isLatest && baseForDiscount != null) {
                        calculateDiscountTable(baseForDiscount, receipt.date)
                    } else {
                        null 
                    }

                    val threshold = parseDiscountThresholdFromMetadata(receipt.metadata)
                    val thresholdMessage = parseDiscountThresholdMessageFromMetadata(receipt.metadata)
                    
                    // 3. Update UI State
                    _billDetails.value = BillDetailsState.Success(
                        receipt = receipt,
                        vtConsumption = epsData?.consumptionVt?.toInt() ?: 0,
                        ntConsumption = epsData?.consumptionNt?.toInt() ?: 0,
                        billType = determineBillType(receipt),
                        isLatestForMerchant = isLatest,
                        isDebtPartiallyPaid = isDebtPartiallyPaid,
                        localUnpaidSum = localUnpaidSum,
                        smartTotalDebt = smartTotalDebt,
                        paidPastBillsSum = paidPastBillsSum,
                        billDebt = billDebt,
                        discountTable = discountTable,
                        infostanDeadline = if (isLatest) infostanDeadline else null,
                        epsDiscountThreshold = if (isLatest) (thresholdMessage ?: threshold) else null
                    )
                    
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

    /**
     * Parse discount table from Receipt metadata.
     * Format: [DISCOUNT:5%~deadline1~amount1|6%~deadline2~amount2|...]
     */
    private fun parseBaseCostFromMetadata(metadata: String?): java.math.BigDecimal? {
        if (metadata.isNullOrBlank()) return null
        val match = Regex("""EPS_BASE_COST:([\d.,]+)""").find(metadata)
        val valueStr = match?.groupValues?.get(1) ?: return null
        // Normalize format (replace comma with dot if needed)
        /* 
           Using BigDecimal constructor directly might fail with commas. 
           We should normalize or use a formatter. 
           Assuming raw string from BigDecimal.toString() usually uses dot, 
           but let's be safe if we serialized it differently.
        */
        return try {
            java.math.BigDecimal(valueStr.replace(",", "."))
        } catch (e: Exception) { null }
    }

    private fun parseDeadlineFromMetadata(metadata: String?): String? {
        if (metadata.isNullOrBlank()) return null
        val match = Regex("""EPS_DEADLINE:(.+?)(?:\||$)""").find(metadata)
        return match?.groupValues?.get(1)
    }

    private fun parseInfostanDeadlineFromMetadata(metadata: String?): String? {
        if (metadata.isNullOrBlank()) return null
        val match = Regex("""INFOSTAN_DEADLINE:(.+?)(?:\||$)""").find(metadata)
        return match?.groupValues?.get(1)
    }

    private fun parseDiscountThresholdFromMetadata(metadata: String?): String? {
        if (metadata.isNullOrBlank()) return null
        val match = Regex("""EPS_THRESHOLD_AMOUNT:([\d.,]+)""").find(metadata)
        return match?.groupValues?.get(1)
    }

    private fun parseDiscountThresholdMessageFromMetadata(metadata: String?): String? {
        if (metadata.isNullOrBlank()) return null
        val match = Regex("""EPS_THRESHOLD_MESSAGE:(.+?)(?:\||$)""").find(metadata)
        return match?.groupValues?.get(1)
    }

    private fun calculateDiscountTable(baseCost: java.math.BigDecimal, billDate: java.util.Date): List<DiscountRow> {
        val discountRows = mutableListOf<DiscountRow>()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = billDate
        
        // Use the month and year from the bill date directly (matches what is shown in UI)
        val month = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
        val year = calendar.get(java.util.Calendar.YEAR)

        val discountPercentages = listOf(5, 6, 7)
        for (pct in discountPercentages) {
            val discountAmount = baseCost.multiply(java.math.BigDecimal(pct)).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_UP)
            val formattedAmount = String.format("%,.2f", discountAmount).replace(",", "X").replace(".", ",").replace("X", ".")

            val pctKey = "$pct%"
            
            // Rule-based conditions
            val condition = when (pct) {
                5 -> "28.$month.$year."
                6 -> "20.$month.$year. uz samoocitavanje"
                7 -> "20.$month.$year. uz elektronski račun"
                else -> "Odmah"
            }
            
            discountRows.add(DiscountRow(pctKey, condition, formattedAmount))
        }
        return discountRows
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
        val smartTotalDebt: Double = 0.0,
        val paidPastBillsSum: Double = 0.0,
        val billDebt: Double = 0.0,
        val discountTable: List<DiscountRow>? = null,
        val infostanDeadline: String? = null,
        val epsDiscountThreshold: String? = null
    ) : BillDetailsState()
    data class Error(val message: String) : BillDetailsState()
}

sealed class SaveQrStatus {
    object Idle : SaveQrStatus()
    object Saving : SaveQrStatus()
    object Success : SaveQrStatus()
    data class Error(val message: String) : SaveQrStatus()
}

