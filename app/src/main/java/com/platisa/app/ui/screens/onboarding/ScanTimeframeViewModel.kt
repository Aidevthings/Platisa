package com.platisa.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.core.data.repository.TrialRepository
import com.platisa.app.core.data.repository.TrialStatus
import com.platisa.app.core.domain.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ScanTimeframeViewModel @Inject constructor(
    private val trialRepository: TrialRepository,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun checkTrialAndProceed(
        onProceed: () -> Unit,
        onExpired: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("ScanTimeframeVM", "Starting trial check...")
            try {
                val account = GoogleAuthManager.getSignedInAccount(context)
                val email = account?.email 
                android.util.Log.d("ScanTimeframeVM", "Account email: $email")
                
                if (email == null) {
                    _isLoading.value = false
                    return@launch onError("Niste prijavljeni!")
                }

                // Fail-safe: Timeout after 5 seconds to prevent blocking user
                val status = try {
                    kotlinx.coroutines.withTimeout(5000L) {
                        trialRepository.checkTrialStatus(email)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w("ScanTimeframeVM", "Trial check timed out. Defaulting to VALID.")
                    TrialStatus.Active(7)
                } catch (e: Exception) {
                    android.util.Log.e("ScanTimeframeVM", "Trial check failed completely. Defaulting to VALID.", e)
                    TrialStatus.Active(7)
                }

                android.util.Log.d("ScanTimeframeVM", "Trial status: $status")

                when (status) {
                    is TrialStatus.Active -> {
                        secureStorage.setOnboardingCompleted(true)
                        onProceed()
                    }
                    is TrialStatus.Expired -> onExpired()
                    is TrialStatus.Error -> {
                        // Even on error, we might want to let them in if it's a network/db issue (Fail Open)
                        // But if it's explicit "Error", maybe just Log and Proceed?
                        // Let's Proceed for now to unblock the user.
                        android.util.Log.w("ScanTimeframeVM", "Trial Error returned: " + status.message + ". Proceeding anyway.")
                        onProceed()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScanTimeframeVM", "Critical Exception in checkTrialAndProceed", e)
                // Fail Open
                onProceed()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

