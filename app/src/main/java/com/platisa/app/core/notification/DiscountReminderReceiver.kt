package com.platisa.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class DiscountReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = PlatisaNotificationManager(context) // Manual instantiation
        
        val merchantName = intent.getStringExtra(EXTRA_MERCHANT_NAME) ?: "Nepoznato"
        val expiryDate = intent.getStringExtra(EXTRA_EXPIRY_DATE) ?: ""
        
        notificationManager.showDiscountExpiringNotification(merchantName, expiryDate)
    }

    companion object {
        const val EXTRA_MERCHANT_NAME = "merchant_name"
        const val EXTRA_EXPIRY_DATE = "expiry_date"
    }
}
