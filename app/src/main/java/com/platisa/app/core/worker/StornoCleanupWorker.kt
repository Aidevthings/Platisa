package com.platisa.app.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.platisa.app.core.data.helper.BillDuplicateDetector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker koji automatski čisti stare STORNO račune.
 */
@HiltWorker
class StornoCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val duplicateDetector: BillDuplicateDetector
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🧹 Starting automatic STORNO cleanup...")
            
            val result = duplicateDetector.cleanupOldStornoBills(retentionDays = 7)
            
            if (result.deleted > 0) {
                Log.d(TAG, "✅ ${result.message}")
                result.deletedBills.forEach { bill ->
                    Log.d(TAG, "   - Račun: ${bill.invoiceNumber}")
                }
            } else {
                Log.d(TAG, "ℹ️ ${result.message}")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup failed: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "StornoCleanupWorker"
        const val WORK_NAME = "storno_cleanup_work"
    }
}

