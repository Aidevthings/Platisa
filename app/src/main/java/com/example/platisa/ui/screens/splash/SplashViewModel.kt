package com.example.platisa.ui.screens.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.platisa.core.common.GoogleAuthManager
import com.example.platisa.core.domain.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashState {
    data object Loading : SplashState()
    data object NavigateToHome : SplashState()
    data object NavigateToLogin : SplashState()
    data object NavigateToOnboarding : SplashState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val preferenceManager: com.example.platisa.core.data.preferences.PreferenceManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expose splash style for UI to determine which image to show
    val splashScreenStyle: String
        get() = preferenceManager.splashScreenStyle

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState = _splashState.asStateFlow()

    init {
        checkStartDestination()
    }

    private fun checkStartDestination() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Keep the 3 second delay for branding

            val account = GoogleAuthManager.getSignedInAccount(context)
            if (account != null) {
                if (secureStorage.isOnboardingCompleted()) {
                    _splashState.value = SplashState.NavigateToHome
                } else {
                    // Logged in but interrupted before selecting timeframe
                    _splashState.value = SplashState.NavigateToOnboarding
                }
            } else {
                _splashState.value = SplashState.NavigateToLogin
            }
        }
    }
}
