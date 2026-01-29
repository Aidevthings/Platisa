package com.platisa.app.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.platisa.app.core.data.preferences.PreferenceManager
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
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, preferenceManager.notificationTimeHour)
            set(Calendar.MINUTE, preferenceManager.notificationTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time is before now, add 1 day
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS // Run once per day
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
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

