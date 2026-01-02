package com.platisa.app

import android.app.Application
import androidx.work.Configuration
import com.platisa.app.core.common.PdfUtils
import com.platisa.app.core.worker.StornoCleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PlatisaApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        

        
        Timber.plant(Timber.DebugTree())
        
        // Initialize PDFBox on background thread to not block startup
        GlobalScope.launch(Dispatchers.IO) {
            PdfUtils.init(this@PlatisaApplication)
        }
        
        // Schedule automatic STORNO cleanup
        StornoCleanupScheduler.schedule(this)
        Timber.d("STORNO cleanup scheduled")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleSync() {
        val workManager = androidx.work.WorkManager.getInstance(this)

        // 1. App Start Sync (Immediate)
        val startSync = androidx.work.OneTimeWorkRequestBuilder<com.platisa.app.core.worker.GmailSyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "AppStartSync", 
            androidx.work.ExistingWorkPolicy.KEEP,
            startSync
        )

        // 2. Daily Sync at 14:00
        val currentDate = java.util.Calendar.getInstance()
        val dueDate = java.util.Calendar.getInstance()
        
        dueDate.set(java.util.Calendar.HOUR_OF_DAY, 14)
        dueDate.set(java.util.Calendar.MINUTE, 0)
        dueDate.set(java.util.Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(java.util.Calendar.HOUR_OF_DAY, 24)
        }
        
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        
        val dailySync = androidx.work.PeriodicWorkRequestBuilder<com.platisa.app.core.worker.GmailSyncWorker>(24, java.util.concurrent.TimeUnit.HOURS)
            .setInitialDelay(timeDiff, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyGmailSync",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            dailySync
        )
        
        Timber.d("Sync scheduled. Immediate: AppStartSync. Daily: DailyGmailSync (delay: ${timeDiff}ms)")
    }
}

