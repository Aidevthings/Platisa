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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val repository: ReceiptRepository,
    val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val secureStorage: com.platisa.app.core.domain.SecureStorage,
    private val currencyApi: com.platisa.app.core.data.network.CurrencyApi,
    private val vibrationHelper: com.platisa.app.core.common.VibrationHelper
) : BaseViewModel() {

    fun vibrate(type: com.platisa.app.core.common.VibrationHelper.HapticType = com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT) {
        vibrationHelper.vibrate(type)
    }

    private val _selectedHomePeriod = MutableStateFlow(com.platisa.app.ui.screens.analytics.GraphPeriod.MONTHLY)
    val selectedHomePeriod: StateFlow<com.platisa.app.ui.screens.analytics.GraphPeriod> = _selectedHomePeriod.asStateFlow()

    fun setHomePeriod(period: com.platisa.app.ui.screens.analytics.GraphPeriod) {
        _selectedHomePeriod.value = period
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
    }

    private val _currency = MutableStateFlow(secureStorage.getCurrency())

    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _userName = MutableStateFlow(secureStorage.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _conversionRate = MutableStateFlow(java.math.BigDecimal(preferenceManager.lastKnownEuroRate.toDouble()))
    val conversionRate: StateFlow<java.math.BigDecimal> = _conversionRate.asStateFlow()

    val receipts: StateFlow<List<Receipt>> = combine(
        repository.getVisibleReceipts(),
        _selectedHomePeriod,
        _currency,
        _conversionRate
    ) { list, selectedPeriod, currentCurrency, rate ->
        list
            .filter { receipt ->
                val isUtility = receipt.category == com.platisa.app.core.domain.model.BillCategory.ELECTRICITY ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.WATER ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.TELECOM ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.GAS ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.UTILITIES ||
                        receipt.category == com.platisa.app.core.domain.model.BillCategory.OTHER

                if (!isUtility) return@filter false
                if (receipt.originalSource == "CAMERA_FISCAL") return@filter false

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
                if (currentCurrency == "EUR" && receipt.currency == "RSD") {
                    receipt.copy(
                        totalAmount = receipt.totalAmount.divide(rate, 2, java.math.RoundingMode.HALF_UP),
                        currency = "EUR"
                    )
                } else if (currentCurrency == "RSD" && receipt.currency == "EUR") {
                    receipt.copy(
                        totalAmount = receipt.totalAmount.multiply(rate),
                        currency = "RSD"
                    )
                } else {
                    receipt
                }
            }
            .sortedWith(
                compareBy<Receipt> { receipt ->
                    when (receipt.paymentStatus) {
                        com.platisa.app.core.domain.model.PaymentStatus.PROCESSING -> 0 // Highest Priority (Ready for Payment)
                        com.platisa.app.core.domain.model.PaymentStatus.UNPAID -> 1
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

    private val _cameraAvatarPath = MutableStateFlow<String?>(null)
    val cameraAvatarPath: StateFlow<String?> = _cameraAvatarPath.asStateFlow()

    private val _celebrationImagePath = MutableStateFlow(secureStorage.getCelebrationImagePath())
    val celebrationImagePath: StateFlow<String?> = _celebrationImagePath.asStateFlow()

    private val _avatarUpdateVersion = MutableStateFlow(preferenceManager.avatarUpdateVersion)
    val avatarUpdateVersion: StateFlow<Long> = _avatarUpdateVersion.asStateFlow()



    init {
        checkConnectedAccount()
        fetchLiveRate()
        checkCameraAvatar()
        
    }

    private fun checkCameraAvatar() {
        val avatarsDir = java.io.File(context.filesDir, "avatars")
        val cameraFile = java.io.File(avatarsDir, "avatar_camera_latest.jpg")
        if (cameraFile.exists()) {
            _cameraAvatarPath.value = cameraFile.absolutePath
        }
    }

    private fun fetchLiveRate() {
        viewModelScope.launch(Dispatchers.IO) {
            val lastFetch = preferenceManager.lastRateFetchTime
            val currentTime = System.currentTimeMillis()
            val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours

            if (currentTime - lastFetch > CACHE_DURATION) {
                try {
                    android.util.Log.d("HomeViewModel", "Fetching live currency rates...")
                    val response = currencyApi.getLatestRate("EUR")
                    val newRate = response.rates["RSD"]
                    if (newRate != null) {
                        preferenceManager.lastKnownEuroRate = newRate.toFloat()
                        preferenceManager.lastRateFetchTime = currentTime
                        _conversionRate.value = java.math.BigDecimal(newRate)
                        android.util.Log.d("HomeViewModel", "✅ Live rate updated: $newRate")
                    } else {
                        android.util.Log.w("HomeViewModel", "⚠️ RSD rate not found in response")
                    }
                } catch (e: Exception) {
                    // CATCH ALL to prevent ClassCastException from crashing the app
                    android.util.Log.e("HomeViewModel", "❌ Failed to fetch live rate (likely ProGuard/Network): ${e.message}")
                    if (e is java.lang.ClassCastException) {
                        android.util.Log.e("HomeViewModel", "CRITICAL: ProGuard is still stripping type info from CurrencyResponse!")
                    }
                }
            } else {
                 android.util.Log.d("HomeViewModel", "Using cached rate: ${preferenceManager.lastKnownEuroRate}")
            }
        }
    }

    private fun checkConnectedAccount() {
        try {
            val gms = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val status = gms.isGooglePlayServicesAvailable(context)
            if (status == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                _connectedAccount.value = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            } else {
                android.util.Log.w("HomeViewModel", "GMS not available ($status), skipping account check.")
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error checking connected account: ${e.message}")
        }
    }

    fun setConnectedAccount(email: String) {
        secureStorage.addConnectedAccount(email)
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
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.SUCCESS)
        launchCatching {
            val receipt = repository.getReceiptById(receiptId)
            receipt?.let {
                // Delete the QR code from gallery if it was saved
                com.platisa.app.core.common.QrSaveManager.deleteQrFromGallery(context, it.savedQrUri)

                // CASCADE PAYMENT - Check if Total Debt was selected when QR was saved
                val metadataContainsFlag = it.metadata?.contains("[TOTAL_DEBT_SELECTED]") == true
                
                if (metadataContainsFlag) {
                    try {
                        repository.markPastBillsAsPaid(it.merchantName, it.id, it.date.time)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "CASCADE PAYMENT failed: ${e.message}", e)
                    }
                }

                // Clean up the flag from metadata
                val cleanedMetadata = it.metadata?.replace(" [TOTAL_DEBT_SELECTED]", "") ?: ""
                
                repository.updateReceipt(
                    it.copy(
                        paymentStatus = com.platisa.app.core.domain.model.PaymentStatus.PAID,
                        paymentDate = java.util.Date(),
                        metadata = cleanedMetadata
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
        _avatarUpdateVersion.value = preferenceManager.avatarUpdateVersion
        checkCameraAvatar()
        // Check rate on profile refresh too if needed, or just let init handle it
    }

    // Use single source of truth from PreferenceManager
    val isDarkTheme: StateFlow<Boolean> = preferenceManager.themeFlow

    fun toggleTheme() {
        vibrationHelper.vibrate(com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT)
        // Toggle the value in preferences - this will update the flow automatically
        preferenceManager.isDarkTheme = !isDarkTheme.value
    }
}

