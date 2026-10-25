package com.spendwise.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spendwise.android.MainActivity
import com.spendwise.android.R
import com.spendwise.android.data.db.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

/**
 * Helper responsible for creating channels and dispatching morning and evening expense reminders.
 * Adheres strictly to on-device logic and context-aware messaging.
 */
object SpendoNotificationHelper {

    const val CHANNEL_REMINDERS_ID = "spendo_expense_reminders"
    private const val NOTIFICATION_ID_MORNING = 2001
    private const val NOTIFICATION_ID_EVENING = 2002
    private const val NOTIFICATION_ID_TEST = 2003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Expense Reminders"
            val descriptionText = "Morning and evening prompts to review and log daily spending"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_REMINDERS_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun showMorningReminder(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Calculate yesterday's date range
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startOfYesterday = cal.timeInMillis

        var yesterdayCount = 0
        try {
            val db = AppDatabase.getInstance(context)
            val allTx = db.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()
            yesterdayCount = allTx.count { it.occurredAtEpochMillis in startOfYesterday until startOfToday && it.direction == "DEBIT" }
        } catch (_: Exception) {}

        val bodyText = if (yesterdayCount > 0) {
            "You recorded $yesterdayCount expense${if (yesterdayCount > 1) "s" else ""} yesterday. Take a moment to log any missed cash or card purchases."
        } else {
            "Did you miss any expenses yesterday? Take a moment to update Spendo and keep your records accurate."
        }

        // Review Action Intent
        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "activity")
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            301,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Good morning! 🌤️")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Review Expenses", reviewPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
    }

    suspend fun showEveningReminder(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Calculate today's spending count
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        var todayCount = 0
        try {
            val db = AppDatabase.getInstance(context)
            val allTx = db.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()
            todayCount = allTx.count { it.occurredAtEpochMillis >= startOfToday && it.direction == "DEBIT" }
        } catch (_: Exception) {}

        val bodyText = if (todayCount > 0) {
            "You've recorded $todayCount expense${if (todayCount > 1) "s" else ""} today. Add any missing expenses before you call it a day."
        } else {
            "Have you recorded today's spending? Add any missing expenses before you call it a day."
        }

        // Add Expense Action
        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_transaction")
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            302,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Review Today Action
        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "activity")
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            303,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time to update Spendo")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Add Expense", addPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Review Today", reviewPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_transaction")
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            304,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Spendo Reminder Test 🔔")
            .setContentText("Expense notifications are active! Tap to record an expense.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(addPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Add Expense", addPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
    }
}
