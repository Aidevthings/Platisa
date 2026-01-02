package com.example.platisa.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("platisa_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        
        // Notification preferences
        private const val KEY_NOTIFY_DUE_3_DAYS = "notify_due_3_days"
        private const val KEY_NOTIFY_DUE_1_DAY = "notify_due_1_day"
        private const val KEY_NOTIFY_OVERDUE = "notify_overdue"
        private const val KEY_NOTIFY_DUPLICATE = "notify_duplicate"
        private const val KEY_NOTIFICATION_TIME_HOUR = "notification_time_hour"
        
        // Theme preference
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val KEY_SPLASH_SCREEN_STYLE = "splash_screen_style"
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()
    
    // Notification preferences (all enabled by default)
    var notifyDue3Days: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_DUE_3_DAYS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_DUE_3_DAYS, value).apply()
    
    var notifyDue1Day: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_DUE_1_DAY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_DUE_1_DAY, value).apply()
    
    var notifyOverdue: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_OVERDUE, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_OVERDUE, value).apply()
    
    var notifyDuplicate: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_DUPLICATE, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_DUPLICATE, value).apply()
    
    var notificationTimeHour: Int
        get() = prefs.getInt(KEY_NOTIFICATION_TIME_HOUR, 9) // Default 9 AM
        set(value) = prefs.edit().putInt(KEY_NOTIFICATION_TIME_HOUR, value).apply()

    // Feature Flags / Discovery
    var hasScannedRestaurantBill: Boolean
        get() = prefs.getBoolean("has_scanned_restaurant_bill", false)
        set(value) = prefs.edit().putBoolean("has_scanned_restaurant_bill", value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("has_seen_tutorial", false)
        set(value) = prefs.edit().putBoolean("has_seen_tutorial", value).apply()

    var hasSeenTrialPopup: Boolean
        get() = prefs.getBoolean("has_seen_trial_popup", false)
        set(value) = prefs.edit().putBoolean("has_seen_trial_popup", value).apply()

    var subscriptionStatus: String
        get() = prefs.getString("subscription_status", "TRIAL") ?: "TRIAL"
        set(value) = prefs.edit().putString("subscription_status", value).apply()

    var subscriptionExpiryDate: Long
        get() = prefs.getLong("subscription_expiry_date", 0)
        set(value) = prefs.edit().putLong("subscription_expiry_date", value).apply()

    var trialStartDate: Long
        get() = prefs.getLong("trial_start_date", 0)
        set(value) = prefs.edit().putLong("trial_start_date", value).apply()

    var isLifetime: Boolean
        get() = prefs.getBoolean("is_lifetime", false)
        set(value) = prefs.edit().putBoolean("is_lifetime", value).apply()

    var splashScreenStyle: String
        get() = prefs.getString(KEY_SPLASH_SCREEN_STYLE, "LIGHT") ?: "LIGHT"
        set(value) = prefs.edit().putString(KEY_SPLASH_SCREEN_STYLE, value).apply()

    private val _themeFlow = kotlinx.coroutines.flow.MutableStateFlow(isDarkTheme)
    val themeFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _themeFlow

    var isDarkTheme: Boolean
        get() {
            if (!prefs.contains(KEY_IS_DARK_THEME)) {
                // If no preference is set, default to System Theme
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            return prefs.getBoolean(KEY_IS_DARK_THEME, true)
        }
        set(value) {
            prefs.edit().putBoolean(KEY_IS_DARK_THEME, value).apply()
            _themeFlow.value = value
        }
}
