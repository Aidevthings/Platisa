package com.platisa.app.ui.screens.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.core.domain.SecureStorage
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
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val migrationManager: com.platisa.app.core.data.manager.MigrationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expose splash style for UI to determine which image to show
    val splashScreenStyle: String
        get() = preferenceManager.splashScreenStyle
    
    // Expose isDarkTheme for UI to determine splash background
    val isDarkTheme: Boolean
        get() = preferenceManager.isDarkTheme

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState = _splashState.asStateFlow()

    init {
        checkStartDestination()
    }

    private fun checkStartDestination() {
        viewModelScope.launch {
            // Run startup tasks in parallel with the splash delay
            val startupJob = launch {
                try {
                    android.util.Log.d("SplashViewModel", "⏳ Starting migration...")
                    migrationManager.performUniversalSharingMigration()
                    android.util.Log.d("SplashViewModel", "✅ Migration finished")
                } catch (e: Exception) {
                    android.util.Log.e("SplashViewModel", "❌ Migration FAILED: ${e.message}", e)
                }
            }
            
            val delayJob = launch {
                kotlinx.coroutines.delay(3000) // Keep branding visible
            }
            
            // Wait for both to finish (or at least the delay)
            // But we don't want migration to block user forever if it hangs.
            // So we wait for delay, and if migration is still running, we let it run in background.
            delayJob.join()

            android.util.Log.d("SplashViewModel", "⏳ Checking Google account status...")
            val account = try {
                GoogleAuthManager.getSignedInAccount(context)
            } catch (e: Exception) {
                android.util.Log.e("SplashViewModel", "❌ Error checking Google account: ${e.message}", e)
                null
            }
            
            android.util.Log.d("SplashViewModel", "✅ Account check complete. Account: ${account?.email ?: "None"}")

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

