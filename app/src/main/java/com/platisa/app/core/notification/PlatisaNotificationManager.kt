package com.platisa.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION", "UnstableApiUsage", "ExperimentalMaterialApi")
@Singleton
class PlatisaNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        const val EXTRA_BILL_ID = "bill_id"
        const val EXTRA_SHOW_SETTINGS = "show_settings"
        
        private const val CHANNEL_ID_DUE_SOON = "bill_due_soon"
        private const val CHANNEL_ID_OVERDUE = "bill_overdue"
        private const val CHANNEL_ID_DUPLICATE = "duplicate_warning"
        private const val CHANNEL_ID_AUTH = "auth_errors"
        
        private const val NOTIFICATION_ID_DUE_3_DAYS = 1001
        private const val NOTIFICATION_ID_DUE_1_DAY = 1002
        private const val NOTIFICATION_ID_OVERDUE = 1003
        private const val NOTIFICATION_ID_DUPLICATE = 1004
        private const val NOTIFICATION_ID_AUTH = 1100
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dueSoonChannel = NotificationChannel(
                CHANNEL_ID_DUE_SOON,
                "Računi koji dospevaju",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Obaveštenja o računima koji uskoro dospevaju"
            }
            
            val overdueChannel = NotificationChannel(
                CHANNEL_ID_OVERDUE,
                "Prekoračeni računi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obaveštenja o računima koji su prekoračili rok"
            }
            
            val duplicateChannel = NotificationChannel(
                CHANNEL_ID_DUPLICATE,
                "Upozorenje o duplikatu",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upozorenja o mogućem duplom plaćanju"
            }
            
            val authChannel = NotificationChannel(
                CHANNEL_ID_AUTH,
                "Greške prijavljivanja",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obaveštenja o problemima sa prijavljivanjem"
            }
            
            notificationManager.createNotificationChannel(dueSoonChannel)
            notificationManager.createNotificationChannel(overdueChannel)
            notificationManager.createNotificationChannel(duplicateChannel)
            notificationManager.createNotificationChannel(authChannel)
        }
    }
    
    @Suppress("UnstableApiUsage")
    private fun createPendingIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply {
                setClassName(context, "${context.packageName}.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    @Suppress("UnstableApiUsage")
    private fun createBillPendingIntent(billId: Long): PendingIntent {
        val intent = Intent().apply {
            setClassName(context, "${context.packageName}.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BILL_ID, billId)
        }
        
        return PendingIntent.getActivity(
            context,
            billId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    @Suppress("UnstableApiUsage")
    private fun createAuthErrorPendingIntent(email: String): PendingIntent {
        val intent = Intent().apply {
            setClassName(context, "${context.packageName}.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_SETTINGS, true)
            putExtra("target_email", email)
        }
        
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_AUTH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    fun showDueSoonBillNotification(billId: Long, merchantName: String, amount: Double, daysUntilDue: Int) {
        val title = when (daysUntilDue) {
            1 -> "Račun dospeva sutra!"
            else -> "Račun dospeva za $daysUntilDue dana"
        }
        val message = "$merchantName - ${formatAmount(amount)} RSD"
        
        val channelId = CHANNEL_ID_DUE_SOON
        val priority = if (daysUntilDue == 1) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        
        showBillNotification(
            notificationId = (NOTIFICATION_ID_DUE_3_DAYS + billId).toInt(),
            channelId = channelId,
            title = title,
            message = message,
            priority = priority,
            billId = billId
        )
    }
    
    fun showOverdueBillNotification(billId: Long, merchantName: String, amount: Double) {
        val title = "⚠️ Račun je prekoračio rok!"
        val message = "$merchantName - ${formatAmount(amount)} RSD"
        
        showBillNotification(
            notificationId = (NOTIFICATION_ID_OVERDUE + billId).toInt(),
            channelId = CHANNEL_ID_OVERDUE,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH,
            billId = billId
        )
    }
    
    private fun showBillNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        priority: Int,
        billId: Long
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(createBillPendingIntent(billId))
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    fun showDue3DaysNotification(billCount: Int, totalAmount: Double) {
        val title = "Računi dospevaju za 3 dana"
        val message = if (billCount == 1) {
            "Imate 1 račun koji dospeva za 3 dana (${formatAmount(totalAmount)} RSD)"
        } else {
            "Imate $billCount računa koji dospevaju za 3 dana (${formatAmount(totalAmount)} RSD)"
        }
        
        showNotification(
            notificationId = NOTIFICATION_ID_DUE_3_DAYS,
            channelId = CHANNEL_ID_DUE_SOON,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    fun showDue1DayNotification(billCount: Int, totalAmount: Double) {
        val title = "Računi dospevaju sutra!"
        val message = if (billCount == 1) {
            "Imate 1 račun koji dospeva sutra (${formatAmount(totalAmount)} RSD)"
        } else {
            "Imate $billCount računa koji dospevaju sutra (${formatAmount(totalAmount)} RSD)"
        }
        
        showNotification(
            notificationId = NOTIFICATION_ID_DUE_1_DAY,
            channelId = CHANNEL_ID_DUE_SOON,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    fun showOverdueNotification(billCount: Int, totalAmount: Double) {
        val title = "⚠️ Prekoračeni računi!"
        val message = if (billCount == 1) {
            "Imate 1 račun koji je prekoračio rok (${formatAmount(totalAmount)} RSD)"
        } else {
            "Imate $billCount računa koji su prekoračili rok (${formatAmount(totalAmount)} RSD)"
        }
        
        showNotification(
            notificationId = NOTIFICATION_ID_OVERDUE,
            channelId = CHANNEL_ID_OVERDUE,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    fun showDuplicatePaymentWarning(billDescription: String) {
        val title = "⚠️ Upozorenje o duplom plaćanju"
        val message = "Račun \"$billDescription\" je možda već plaćen. Proverite pre nego što platite."
        
        showNotification(
            notificationId = NOTIFICATION_ID_DUPLICATE,
            channelId = CHANNEL_ID_DUPLICATE,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    fun showAuthErrorNotification(email: String) {
        val title = "⚠️ Konekcija sa Gmail-om prekinuta"
        val message = "Ne možemo pristupiti nalogu $email. Promenjena lozinka? Dodirnite za ponovno prijavljivanje."
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_AUTH)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createAuthErrorPendingIntent(email))
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
            
        try {
            notificationManager.notify(NOTIFICATION_ID_AUTH, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun showNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        priority: Int
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }
    
    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

