package com.spendwise.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Manages reliable scheduling of morning and evening expense reminders.
 * Stores preferences persistently and survives reboots and timezone shifts.
 */
object SpendoReminderScheduler {

    private const val PREFS_NAME = "spendo_reminder_prefs"
    const val KEY_MORNING_ENABLED = "morning_enabled"
    const val KEY_MORNING_HOUR = "morning_hour"
    const val KEY_MORNING_MINUTE = "morning_minute"
    const val KEY_EVENING_ENABLED = "evening_enabled"
    const val KEY_EVENING_HOUR = "evening_hour"
    const val KEY_EVENING_MINUTE = "evening_minute"

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val morningEnabled = prefs.getBoolean(KEY_MORNING_ENABLED, true)
        val morningHour = prefs.getInt(KEY_MORNING_HOUR, 9)
        val morningMinute = prefs.getInt(KEY_MORNING_MINUTE, 0)

        val eveningEnabled = prefs.getBoolean(KEY_EVENING_ENABLED, true)
        val eveningHour = prefs.getInt(KEY_EVENING_HOUR, 20)
        val eveningMinute = prefs.getInt(KEY_EVENING_MINUTE, 30)

        if (morningEnabled) {
            scheduleReminder(context, isMorning = true, morningHour, morningMinute)
        } else {
            cancelReminder(context, isMorning = true)
        }

        if (eveningEnabled) {
            scheduleReminder(context, isMorning = false, eveningHour, eveningMinute)
        } else {
            cancelReminder(context, isMorning = false)
        }
    }

    fun scheduleReminder(context: Context, isMorning: Boolean, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SpendoReminderReceiver::class.java).apply {
            action = if (isMorning) SpendoReminderReceiver.ACTION_MORNING else SpendoReminderReceiver.ACTION_EVENING
        }
        val requestCode = if (isMorning) 401 else 402
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = getNextTriggerMillis(hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, isMorning: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SpendoReminderReceiver::class.java).apply {
            action = if (isMorning) SpendoReminderReceiver.ACTION_MORNING else SpendoReminderReceiver.ACTION_EVENING
        }
        val requestCode = if (isMorning) 401 else 402
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getNextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun updatePreferences(
        context: Context,
        morningEnabled: Boolean,
        morningHour: Int,
        morningMinute: Int,
        eveningEnabled: Boolean,
        eveningHour: Int,
        eveningMinute: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_MORNING_ENABLED, morningEnabled)
            .putInt(KEY_MORNING_HOUR, morningHour)
            .putInt(KEY_MORNING_MINUTE, morningMinute)
            .putBoolean(KEY_EVENING_ENABLED, eveningEnabled)
            .putInt(KEY_EVENING_HOUR, eveningHour)
            .putInt(KEY_EVENING_MINUTE, eveningMinute)
            .apply()

        scheduleAll(context)
    }
}
