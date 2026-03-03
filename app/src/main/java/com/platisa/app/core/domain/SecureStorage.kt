package com.platisa.app.core.domain

interface SecureStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)
    
    fun getCurrency(): String
    fun setCurrency(currency: String)
    
    // Gmail sync tracking (global - legacy)
    fun getLastGmailSyncTimestamp(): Long // Unix timestamp
    fun setLastGmailSyncTimestamp(timestamp: Long)
    
    // Per-account Gmail sync tracking (for multi-account support)
    fun getLastGmailSyncTimestamp(email: String): Long
    fun setLastGmailSyncTimestamp(email: String, timestamp: Long)
    
    // Multi-account support
    fun getConnectedAccounts(): Set<String>
    fun addConnectedAccount(email: String)
    fun removeConnectedAccount(email: String)
    
    // Sync preferences
    fun getSyncOnWifi(): Boolean
    fun setSyncOnWifi(enabled: Boolean)
    fun getSyncOnMobileData(): Boolean
    fun setSyncOnMobileData(enabled: Boolean)
    
    // Profile customization
    fun getUserName(): String
    fun setUserName(name: String)
    fun getAvatarPath(): String?
    fun setAvatarPath(path: String?)
    fun getCelebrationImagePath(): String?
    fun setCelebrationImagePath(path: String?)

    // Onboarding tracking
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    
    // Wipe all sync-related timestamps
    fun clearSyncData()
    
    // Wipe all user data (for logout/reset)
    fun clearAllData()
}

