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
    const val CHANNEL_BUDGET_ALERTS_ID = "spendo_budget_alerts"
    const val CHANNEL_QUEUE_ALERTS_ID = "spendo_queue_alerts"
    private const val NOTIFICATION_ID_MORNING = 2001
    private const val NOTIFICATION_ID_EVENING = 2002
    private const val NOTIFICATION_ID_TEST = 2003
    private const val NOTIFICATION_ID_BUDGET_ALERT = 2010

    private const val PREFS_BUDGET_ALERTS = "spendo_budget_alerts_prefs"
    private const val KEY_ALERT_DATE = "last_alert_date"
    private const val KEY_ALERT_LEVEL = "last_alert_level"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val queueChannel = NotificationChannel(
                CHANNEL_QUEUE_ALERTS_ID,
                "Transaction Review Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for incoming debit and credit transactions pending verification"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(queueChannel)

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Daily Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning and evening prompts to review pending transactions"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS_ID,
                "Daily Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely progress alerts when daily budget limits are approached or reached"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(budgetChannel)
        }
    }

    /**
     * Dispatches a review-first notification immediately after a transaction is saved into Room DB.
     */
    suspend fun notifyTransactionQueued(
        context: Context,
        transactionId: String,
        merchant: String,
        amountPaise: Long,
        direction: String,
        bankName: String
    ) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val db = AppDatabase.getInstance(context)
        val toneStr = db.appPreferenceDao().getPreferenceValueDirect("spendo_notification_tone")
        val tone = try {
            NotificationPersonalityTone.valueOf(toneStr ?: "WITTY")
        } catch (_: Exception) {
            NotificationPersonalityTone.WITTY
        }

        val amountRupees = amountPaise / 100.0
        val copy = SpendoNotificationPersonality.getTransactionQueuedCopy(
            tone = tone,
            merchant = merchant,
            amountRupees = amountRupees,
            direction = direction
        )

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "spendo_review")
        }
        val reqCode = (transactionId.hashCode() and 0x7FFFFFFF)
        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_QUEUE_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Review", pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reqCode, notification)
    }

    suspend fun checkAndNotifyBudgetThreshold(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 86399999L

        val todayDateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)

        var todaySpentPaise = 0L
        var dailyBudgetLimitPaise: Long? = null

        try {
            val db = AppDatabase.getInstance(context)
            todaySpentPaise = db.transactionDao().getDebitsInDateRangeDirect(startOfToday, endOfToday)
            val dailyBudgetEntity = db.budgetDao().getOverallBudgetByPeriodDirect("DAILY")
            val dailyLimitPref = db.appPreferenceDao().getPreferenceValueDirect("spendo_daily_limit_paise")?.toLongOrNull()
            dailyBudgetLimitPaise = dailyBudgetEntity?.limitPaise ?: dailyLimitPref
        } catch (_: Exception) {
            return
        }

        if (dailyBudgetLimitPaise == null || dailyBudgetLimitPaise <= 0L) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_BUDGET_ALERTS, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_ALERT_DATE, null)
        val lastLevel = if (savedDate == todayDateKey) prefs.getInt(KEY_ALERT_LEVEL, 0) else 0

        val ratio = todaySpentPaise.toDouble() / dailyBudgetLimitPaise
        val currentPercent = (ratio * 100).toInt()
        val clampedProgress = currentPercent.coerceIn(0, 100)

        val currentLevel = when {
            ratio >= 1.0 -> 2 // 100%+ reached or exceeded
            ratio >= 0.80 -> 1 // 80%+ approaching
            else -> 0
        }

        // Handle downward adjustment (e.g. refunds or expense deletion)
        if (currentLevel < lastLevel) {
            prefs.edit()
                .putString(KEY_ALERT_DATE, todayDateKey)
                .putInt(KEY_ALERT_LEVEL, currentLevel)
                .apply()
            return
        }

        // Avoid repeated duplicate notifications for the same threshold level
        if (currentLevel == lastLevel || currentLevel == 0) {
            return
        }

        val (titleText, bodyText) = if (currentLevel == 2) {
            if (todaySpentPaise == dailyBudgetLimitPaise) {
                Pair(
                    "Daily budget reached",
                    "You've used 100% of today's limit. Pause new non-essential spending and review today's expenses."
                )
            } else {
                val overRupees = (todaySpentPaise - dailyBudgetLimitPaise) / 100
                Pair(
                    "Daily budget exceeded",
                    "Exceeded by ₹$overRupees. Review your expenses before spending more."
                )
            }
        } else {
            val remainingRupees = (dailyBudgetLimitPaise - todaySpentPaise) / 100
            Pair(
                "Your budget is getting close",
                "You've used $currentPercent% of today's limit. ₹$remainingRupees left—keep your next spend intentional."
            )
        }

        // Budget navigation action
        val budgetIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "budget")
        }
        val budgetPendingIntent = PendingIntent.getActivity(
            context,
            310,
            budgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Activity review action
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "activity")
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            311,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setProgress(100, clampedProgress, false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(budgetPendingIntent)
            .addAction(0, "View budget", budgetPendingIntent)
            .addAction(0, "Review spending", activityPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_ALERT, notification)
            prefs.edit()
                .putString(KEY_ALERT_DATE, todayDateKey)
                .putInt(KEY_ALERT_LEVEL, currentLevel)
                .apply()
        } catch (_: Exception) {}
    }

    suspend fun showMorningReminder(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val db = AppDatabase.getInstance(context)
        val pendingCount = try {
            db.transactionDao().getPendingReviewCountDirect()
        } catch (_: Exception) {
            0
        }

        // Send ONLY when pending transactions exist
        if (pendingCount <= 0) {
            return
        }

        val toneStr = db.appPreferenceDao().getPreferenceValueDirect("spendo_notification_tone")
        val tone = try {
            NotificationPersonalityTone.valueOf(toneStr ?: "WITTY")
        } catch (_: Exception) {
            NotificationPersonalityTone.WITTY
        }

        val copy = SpendoNotificationPersonality.getMorningReminderCopy(tone, pendingCount)

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "spendo_review")
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            301,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(0, "Review Queue ($pendingCount)", reviewPendingIntent)
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

        val db = AppDatabase.getInstance(context)
        val pendingCount = try {
            db.transactionDao().getPendingReviewCountDirect()
        } catch (_: Exception) {
            0
        }

        // Send ONLY when pending transactions exist
        if (pendingCount <= 0) {
            return
        }

        val toneStr = db.appPreferenceDao().getPreferenceValueDirect("spendo_notification_tone")
        val tone = try {
            NotificationPersonalityTone.valueOf(toneStr ?: "WITTY")
        } catch (_: Exception) {
            NotificationPersonalityTone.WITTY
        }

        val copy = SpendoNotificationPersonality.getEveningReminderCopy(tone, pendingCount)

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "spendo_review")
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            303,
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(reviewPendingIntent)
            .addAction(0, "Review Queue ($pendingCount)", reviewPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
    }

    fun showWeeklyReviewNotification(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "activity")
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            305,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle("Your weekly money check-in")
            .setContentText("Review your spending and plan the week ahead.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Review your spending and plan the week ahead."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(activityPendingIntent)
            .addAction(0, "Review week", activityPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(2004, notification)
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
            306,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_spendo_symbol)
            .setContentTitle("Keep your records complete")
            .setContentText("Add today's missing expenses to stay on track.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Add today's missing expenses to stay on track."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(addPendingIntent)
            .addAction(0, "Add expense", addPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
    }
}
