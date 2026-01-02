package com.platisa.app.ui.screens.home

import android.content.Context
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.platisa.app.core.common.BaseViewModel
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.core.worker.GmailSyncWorker
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val repository: ReceiptRepository,
    val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val secureStorage: com.platisa.app.core.domain.SecureStorage
) : BaseViewModel() {

    private val _selectedHomePeriod = MutableStateFlow(com.platisa.app.ui.screens.analytics.GraphPeriod.MONTHLY)
    val selectedHomePeriod: StateFlow<com.platisa.app.ui.screens.analytics.GraphPeriod> = _selectedHomePeriod.asStateFlow()

    fun setHomePeriod(period: com.platisa.app.ui.screens.analytics.GraphPeriod) {
        _selectedHomePeriod.value = period
    }

    private val _currency = MutableStateFlow(secureStorage.getCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _userName = MutableStateFlow(secureStorage.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    val receipts: StateFlow<List<Receipt>> = combine(
        repository.getAllReceipts(),
        _selectedHomePeriod,
        _currency
    ) { list, selectedPeriod, currentCurrency ->
        list
            .filter { receipt ->
                val isUtility = receipt.category == com.platisa.app.core.domain.model.BillCategory.ELECTRICITY ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.WATER ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.TELECOM ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.GAS ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.UTILITIES ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.OTHER

                if (!isUtility) return@filter false

                // Always show UNPAID and PROCESSING
                if (receipt.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                    return@filter true
                }

                // For PAID bills, filter by time period using PAYMENT DATE
                val dateToCheck = if (receipt.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID) {
                    receipt.paymentDate ?: receipt.date
                } else {
                    receipt.date
                }

                com.platisa.app.ui.screens.analytics.isDateInGraphPeriod(dateToCheck, selectedPeriod)
            }
            .map { receipt ->
                // CURRENCY CONVERSION LOGIC
                // Rate: 1 EUR = 117.5 RSD
                val conversionRate = java.math.BigDecimal("117.5")
                
                if (currentCurrency == "EUR" && receipt.currency == "RSD") {
                    receipt.copy(
                        totalAmount = receipt.totalAmount.divide(conversionRate, 2, java.math.RoundingMode.HALF_UP),
                        currency = "EUR"
                    )
                } else if (currentCurrency == "RSD" && receipt.currency == "EUR") {
                    receipt.copy(
                        totalAmount = receipt.totalAmount.multiply(conversionRate),
                        currency = "RSD"
                    )
                } else {
                    receipt
                }
            }
            .sortedWith(
                compareBy<Receipt> { receipt ->
                    when (receipt.paymentStatus) {
                        com.platisa.app.core.domain.model.PaymentStatus.UNPAID -> 0
                        com.platisa.app.core.domain.model.PaymentStatus.PROCESSING -> 1
                        com.platisa.app.core.domain.model.PaymentStatus.PAID -> 2
                    }
                }.thenByDescending { it.date }
            )
    }
    .flowOn(Dispatchers.Default) // Perform filtering/sorting off the main thread
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val totalSpending: StateFlow<java.math.BigDecimal> = receipts
        .map { list -> 
            list.filter { it.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID }
                .mapNotNull { it.totalAmount }
                .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) } 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = java.math.BigDecimal.ZERO
        )

    val totalPaid: StateFlow<java.math.BigDecimal> = receipts
        .map { list -> 
            list.filter { it.paymentStatus == com.platisa.app.core.domain.model.PaymentStatus.PAID }
                .mapNotNull { it.totalAmount }
                .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) } 
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = java.math.BigDecimal.ZERO
        )

    val totalUnpaid: StateFlow<java.math.BigDecimal> = receipts
        .map { list -> 
            list.filter { it.paymentStatus != com.platisa.app.core.domain.model.PaymentStatus.PAID }
                .mapNotNull { it.totalAmount }
                .fold(java.math.BigDecimal.ZERO) { acc, amount -> acc.add(amount) } 
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = java.math.BigDecimal.ZERO
        )

    val epsDataMap: StateFlow<Map<Long, com.platisa.app.core.domain.model.EpsData>> = repository.getAllEpsData()
        .map { list -> list.toMap() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _isFirstLaunch = MutableStateFlow(preferenceManager.isFirstLaunch)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    fun markFirstLaunchCompleted() {
        preferenceManager.isFirstLaunch = false
        _isFirstLaunch.value = false
    }

    private val _connectedAccount = MutableStateFlow<com.google.android.gms.auth.api.signin.GoogleSignInAccount?>(null)
    val connectedAccount: StateFlow<com.google.android.gms.auth.api.signin.GoogleSignInAccount?> = _connectedAccount.asStateFlow()



    private val _avatarPath = MutableStateFlow(secureStorage.getAvatarPath())
    val avatarPath: StateFlow<String?> = _avatarPath.asStateFlow()

    private val _celebrationImagePath = MutableStateFlow(secureStorage.getCelebrationImagePath())
    val celebrationImagePath: StateFlow<String?> = _celebrationImagePath.asStateFlow()

    init {
        checkConnectedAccount()
    }

    private fun checkConnectedAccount() {
        _connectedAccount.value = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
    }

    fun setConnectedAccount(email: String) {
        checkConnectedAccount()
    }

    fun scheduleGmailSync() {
        val workManager = WorkManager.getInstance(context)
        val syncRequest = OneTimeWorkRequestBuilder<GmailSyncWorker>()
            .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
            .build()
        workManager.enqueue(syncRequest)
    }

    fun markReceiptAsPaid(receiptId: Long) {
        launchCatching {
            val receipt = repository.getReceiptById(receiptId)
            receipt?.let {
                // Delete the QR code from gallery if it was saved
                com.platisa.app.core.common.QrSaveManager.deleteQrFromGallery(context, it.savedQrUri)

                repository.updateReceipt(
                    it.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                        paymentDate = java.util.Date()  // Set current date as payment date
                    )
                )
            }
        }
    }

    fun refreshProfileData() {
        _userName.value = secureStorage.getUserName()
        _avatarPath.value = secureStorage.getAvatarPath()
        _celebrationImagePath.value = secureStorage.getCelebrationImagePath()
        _currency.value = secureStorage.getCurrency()
    }

    private val _isDarkTheme = MutableStateFlow(preferenceManager.isDarkTheme)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        preferenceManager.isDarkTheme = newTheme
        _isDarkTheme.value = newTheme
    }
}

