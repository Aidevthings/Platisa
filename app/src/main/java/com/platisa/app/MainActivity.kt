package com.platisa.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.platisa.app.core.notification.PlatisaNotificationManager
import com.platisa.app.ui.MainScreen
import com.platisa.app.ui.theme.PlatisaTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels

@AndroidEntryPoint
@androidx.compose.material3.ExperimentalMaterial3Api
class MainActivity : BaseActivity() {
    
    private val splashViewModel: com.platisa.app.ui.screens.splash.SplashViewModel by viewModels()
    
    companion object {
        private val _pendingBillId = MutableStateFlow<Long?>(null)
        val pendingBillId: StateFlow<Long?> = _pendingBillId.asStateFlow()
        
        private val _pendingSettingsOpen = MutableStateFlow<Boolean>(false)
        val pendingSettingsOpen: StateFlow<Boolean> = _pendingSettingsOpen.asStateFlow()
        
        fun clearPendingBillId() {
            _pendingBillId.value = null
        }
        
        fun clearPendingSettingsOpen() {
            _pendingSettingsOpen.value = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and hold it until app is ready
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            // Hold native splash until SplashViewModel completes loading
            splashViewModel.splashState.value is com.platisa.app.ui.screens.splash.SplashState.Loading
        }
        
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            // Use saved theme preference (from SplashViewModel) as initial value
            val darkTheme by mainViewModel.isDarkTheme.collectAsState(initial = splashViewModel.isDarkTheme)

            PlatisaTheme(darkTheme = darkTheme) {
                MainScreen(mainViewModel)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        val billId = intent?.getLongExtra(PlatisaNotificationManager.EXTRA_BILL_ID, -1L)
        if (billId != null && billId != -1L) {
            _pendingBillId.value = billId
            android.util.Log.d("MainActivity", "Deep link to bill ID: $billId")
        }
        
        if (intent?.getBooleanExtra(PlatisaNotificationManager.EXTRA_SHOW_SETTINGS, false) == true) {
            _pendingSettingsOpen.value = true
             android.util.Log.d("MainActivity", "Deep link to settings")
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class MainViewModel @javax.inject.Inject constructor(
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val vibrationHelper: com.platisa.app.core.common.VibrationHelper
) : androidx.lifecycle.ViewModel() {
    val isDarkTheme = preferenceManager.themeFlow
    
    fun vibrate(type: com.platisa.app.core.common.VibrationHelper.HapticType = com.platisa.app.core.common.VibrationHelper.HapticType.LIGHT) {
        vibrationHelper.vibrate(type)
    }
}

