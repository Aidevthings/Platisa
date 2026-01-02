package com.example.platisa.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.platisa.core.domain.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecureStorage {

    private val masterKeyAlias by lazy { MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC) }
    
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun clearToken() {
        sharedPreferences.edit().remove("auth_token").apply()
    }

    override fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean("biometric_enabled", false)
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    override fun getCurrency(): String {
        return sharedPreferences.getString("currency", "RSD") ?: "RSD"
    }

    override fun setCurrency(currency: String) {
        sharedPreferences.edit().putString("currency", currency).apply()
    }
    
    override fun getLastGmailSyncTimestamp(): Long {
        return sharedPreferences.getLong("last_gmail_sync", 0L)
    }
    
    override fun setLastGmailSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong("last_gmail_sync", timestamp).apply()
    }

    override fun getConnectedAccounts(): Set<String> {
        return sharedPreferences.getStringSet("connected_accounts", emptySet()) ?: emptySet()
    }

    override fun addConnectedAccount(email: String) {
        val current = getConnectedAccounts().toMutableSet()
        current.add(email)
        sharedPreferences.edit().putStringSet("connected_accounts", current).apply()
    }

    override fun removeConnectedAccount(email: String) {
        val current = getConnectedAccounts().toMutableSet()
        current.remove(email)
        sharedPreferences.edit().putStringSet("connected_accounts", current).apply()
    }

    override fun getSyncOnWifi(): Boolean {
        return sharedPreferences.getBoolean("sync_on_wifi", true) // Default true
    }

    override fun setSyncOnWifi(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sync_on_wifi", enabled).apply()
    }

    override fun getSyncOnMobileData(): Boolean {
        return sharedPreferences.getBoolean("sync_on_mobile_data", false) // Default false
    }

    override fun setSyncOnMobileData(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sync_on_mobile_data", enabled).apply()
    }
    
    override fun getUserName(): String {
        return sharedPreferences.getString("user_name", "Platiša") ?: "Platiša"
    }
    
    override fun setUserName(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }
    
    override fun getAvatarPath(): String? {
        return sharedPreferences.getString("avatar_path", null)
    }
    
    override fun setAvatarPath(path: String?) {
        if (path != null) {
            sharedPreferences.edit().putString("avatar_path", path).apply()
        } else {
            sharedPreferences.edit().remove("avatar_path").apply()
        }
    }
    
    override fun getCelebrationImagePath(): String? {
        return sharedPreferences.getString("celebration_image_path", "predefined:celebration_4") ?: "predefined:celebration_4"
    }
    
    override fun setCelebrationImagePath(path: String?) {
        if (path != null) {
            sharedPreferences.edit().putString("celebration_image_path", path).apply()
        } else {
            sharedPreferences.edit().remove("celebration_image_path").apply()
        }
    }

    override fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean("onboarding_completed", false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean("onboarding_completed", completed).apply()
    }
    
    override fun clearAllData() {
        sharedPreferences.edit()
            .remove("connected_accounts")
            .remove("last_gmail_sync")
            .remove("auth_token")
            .remove("onboarding_completed") // Clear onboarding state on logout
            .apply()
        android.util.Log.d("SecureStorage", "All user data cleared (accounts, sync timestamp, token)")
    }
}
