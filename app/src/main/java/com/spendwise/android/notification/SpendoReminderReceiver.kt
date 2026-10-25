package com.spendwise.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager to post morning and evening reminders.
 */
class SpendoReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MORNING = "com.spendwise.android.notification.ACTION_MORNING_REMINDER"
        const val ACTION_EVENING = "com.spendwise.android.notification.ACTION_EVENING_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MORNING -> {
                        SpendoNotificationHelper.showMorningReminder(context)
                        // Reschedule next morning reminder
                        val prefs = context.getSharedPreferences("spendo_reminder_prefs", Context.MODE_PRIVATE)
                        val hour = prefs.getInt(SpendoReminderScheduler.KEY_MORNING_HOUR, 9)
                        val minute = prefs.getInt(SpendoReminderScheduler.KEY_MORNING_MINUTE, 0)
                        SpendoReminderScheduler.scheduleReminder(context, isMorning = true, hour, minute)
                    }
                    ACTION_EVENING -> {
                        SpendoNotificationHelper.showEveningReminder(context)
                        // Reschedule next evening reminder
                        val prefs = context.getSharedPreferences("spendo_reminder_prefs", Context.MODE_PRIVATE)
                        val hour = prefs.getInt(SpendoReminderScheduler.KEY_EVENING_HOUR, 20)
                        val minute = prefs.getInt(SpendoReminderScheduler.KEY_EVENING_MINUTE, 30)
                        SpendoReminderScheduler.scheduleReminder(context, isMorning = false, hour, minute)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
