package com.platisa.app.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.core.data.repository.TrialRepository
import com.platisa.app.core.data.repository.TrialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val trialRepository: TrialRepository,
    private val billingManager: com.platisa.app.core.billing.BillingManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _status = MutableStateFlow<TrialStatus?>(null)
    val status = _status.asStateFlow()
    
    // Calculated days remaining for UI
    private val _daysRemaining = MutableStateFlow<Long>(0)
    val daysRemaining = _daysRemaining.asStateFlow()

    // Plan Selection (YEARLY is default/best value)
    private val _selectedPlan = MutableStateFlow("YEARLY")
    val selectedPlan = _selectedPlan.asStateFlow()

    // Real Billing Data
    val productDetails = billingManager.productDetails
    val purchases = billingManager.purchases

    init {
        checkStatus()
        observePurchases()
    }

    private fun observePurchases() {
        viewModelScope.launch {
            purchases.collect { purchaseList ->
                // If any purchase is valid/active, unlock usage
                if (purchaseList.isNotEmpty()) {
                    // Grants "Lifetime" or "Subscribed" access locally
                    trialRepository.enableLifetimeAccess() 
                    checkStatus()
                }
            }
        }
    }

    fun selectPlan(planId: String) {
        _selectedPlan.value = planId
    }

    fun buySubscription(activity: android.app.Activity) {
        // Map internal ID to Google Play Product ID
        val productId = if (_selectedPlan.value == "YEARLY") 
            com.platisa.app.core.billing.BillingManager.SUB_YEARLY 
        else 
            com.platisa.app.core.billing.BillingManager.SUB_MONTHLY
            
        billingManager.launchPurchaseFlow(activity, productId)
    }

    fun checkStatus() {
        viewModelScope.launch {
            val account = GoogleAuthManager.getSignedInAccount(context)
            val email = account?.email ?: return@launch
            
            try {
                // Check repo status (includes local overrides like 'isLifetime' from purchase)
                val result = trialRepository.checkTrialStatus(email)
                _status.value = result
                
                if (result is TrialStatus.Active) {
                    _daysRemaining.value = result.daysRemaining.toLong()
                } else if (result is TrialStatus.Expired) {
                    _daysRemaining.value = 0
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    fun applyPromoCode(code: String): String {
        val cleanCode = code.trim().uppercase()
        val founders = listOf("CVRLE", "GARA", "JANKO", "BOSILJKA", "BOSA") // Lifetime Access

        return when {
            founders.contains(cleanCode) -> {
                viewModelScope.launch {
                    trialRepository.enableLifetimeAccess()
                    checkStatus() // Refresh UI
                }
                "Dobrodošli osnivaču! (Lifetime Access Activated)"
            }
            cleanCode == "DRUGA_SANSA" || cleanCode == "DRUGASANSA" -> {
                viewModelScope.launch {
                    trialRepository.extendTrial(30)
                    checkStatus()
                }
                "Trial produžen za 30 dana! (Druga Šansa)"
            }
            cleanCode == "PLATISA30" -> {
                 viewModelScope.launch {
                    // Logic for 30 days total (reset start date to now?)
                    // For simplicity, let's just extend by 30 days if they are already in trial
                    trialRepository.extendTrial(30)
                    checkStatus()
                }
                "Trial produžen za 30 dana!"
            }
            else -> "Nevažeći kod"
        }
    }
}

