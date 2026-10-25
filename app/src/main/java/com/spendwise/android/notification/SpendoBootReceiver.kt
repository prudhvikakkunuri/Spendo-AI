package com.spendwise.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reschedules configured morning and evening reminders when the device reboots or the app updates.
 */
class SpendoBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SpendoReminderScheduler.scheduleAll(context)
        }
    }
}
