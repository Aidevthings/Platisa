package com.platisa.app.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.platisa.app.core.data.preferences.PreferenceManager
import com.platisa.app.core.domain.repository.ReceiptRepository
import com.platisa.app.core.domain.model.PaymentStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val receiptRepository: ReceiptRepository,
    private val preferenceManager: PreferenceManager,
    private val notificationManager: PlatisaNotificationManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            checkAndNotify()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private suspend fun checkAndNotify() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Get all unpaid, visible receipts
        val allReceipts = receiptRepository.getAllReceipts().first()
            .filter { it.paymentStatus == PaymentStatus.UNPAID && it.isVisible }
        
        // Receipts due in 3 days - show individual notification per bill
        if (preferenceManager.notifyDue3Days) {
            val receiptsDueIn3Days = allReceipts.filter { receipt ->
                receipt.dueDate?.let { dueDate ->
                    val daysDiff = getDaysDifference(today.time, dueDate)
                    daysDiff == 3L
                } ?: false
            }
            
            receiptsDueIn3Days.forEach { receipt ->
                notificationManager.showDueSoonBillNotification(
                    billId = receipt.id,
                    merchantName = receipt.merchantName,
                    amount = receipt.totalAmount.toDouble(),
                    daysUntilDue = 3
                )
            }
        }
        
        // Receipts due in 1 day (tomorrow) - show individual notification per bill
        if (preferenceManager.notifyDue1Day) {
            val receiptsDueTomorrow = allReceipts.filter { receipt ->
                receipt.dueDate?.let { dueDate ->
                    val daysDiff = getDaysDifference(today.time, dueDate)
                    daysDiff == 1L
                } ?: false
            }
            
            receiptsDueTomorrow.forEach { receipt ->
                notificationManager.showDueSoonBillNotification(
                    billId = receipt.id,
                    merchantName = receipt.merchantName,
                    amount = receipt.totalAmount.toDouble(),
                    daysUntilDue = 1
                )
            }
        }
        
        // Overdue receipts - show individual notification per bill
        if (preferenceManager.notifyOverdue) {
            val overdueReceipts = allReceipts.filter { receipt ->
                receipt.dueDate?.let { dueDate ->
                    dueDate.before(today.time)
                } ?: false
            }
            
            overdueReceipts.forEach { receipt ->
                notificationManager.showOverdueBillNotification(
                    billId = receipt.id,
                    merchantName = receipt.merchantName,
                    amount = receipt.totalAmount.toDouble()
                )
            }
        }
    }
    
    private fun getDaysDifference(fromDate: Date, toDate: Date): Long {
        val diffInMillis = toDate.time - fromDate.time
        return TimeUnit.MILLISECONDS.toDays(diffInMillis)
    }
}

