package com.example.platisa.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.platisa.core.data.preferences.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    
    companion object {
        private const val NOTIFICATION_WORK_NAME = "bill_notification_check"
    }
    
    fun scheduleNotificationChecks() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val preferredHour = preferenceManager.notificationTimeHour
        
        // Calculate delay until next preferred notification time
        val delayHours = if (currentHour < preferredHour) {
            preferredHour - currentHour
        } else {
            24 - currentHour + preferredHour
        }
        
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS // Run once per day
        )
            .setInitialDelay(delayHours.toLong(), TimeUnit.HOURS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if already exists
            workRequest
        )
    }
    
    fun cancelNotificationChecks() {
        WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATION_WORK_NAME)
    }
    
    fun rescheduleNotificationChecks() {
        cancelNotificationChecks()
        scheduleNotificationChecks()
    }
}
