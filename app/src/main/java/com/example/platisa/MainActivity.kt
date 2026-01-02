package com.example.platisa

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.platisa.core.notification.PlatisaNotificationManager
import com.example.platisa.ui.MainScreen
import com.example.platisa.ui.theme.PlatisaTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
@androidx.compose.material3.ExperimentalMaterial3Api
class MainActivity : BaseActivity() {
    
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
        // MUST be called before super.onCreate() to properly handle native splash
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val darkTheme by mainViewModel.isDarkTheme.collectAsState(initial = false)

            PlatisaTheme(darkTheme = darkTheme) {
                MainScreen()
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
    private val preferenceManager: com.example.platisa.core.data.preferences.PreferenceManager
) : androidx.lifecycle.ViewModel() {
    val isDarkTheme = preferenceManager.themeFlow
}
