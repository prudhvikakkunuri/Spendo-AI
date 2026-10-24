package com.spendwise.android.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.spendwise.android.data.repository.NotificationSettingsRepository

enum class NotificationAccessState {
    ACTIVE,
    PAUSED,
    PERMISSION_NEEDED
}

/**
 * Manager handling Android Notification Listener permission detection,
 * system settings intent creation, and overall detection status computation.
 */
class NotificationAccessManager(
    private val context: Context,
    private val settingsRepository: NotificationSettingsRepository
) {

    /**
     * Checks whether the user has granted Android Notification Access to SpendWise.
     * Uses both NotificationManagerCompat and Settings.Secure for maximum OEM compatibility.
     */
    fun isNotificationAccessGranted(): Boolean {
        val packageName = context.packageName

        // Method 1: NotificationManagerCompat
        try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledPackages.contains(packageName)) {
                return true
            }
        } catch (_: Exception) {
            // Fallback to Settings.Secure below
        }

        // Method 2: Settings.Secure enabled_notification_listeners string
        return try {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!flat.isNullOrBlank()) {
                val names = flat.split(":")
                val expectedComponent = ComponentName(context, TransactionNotificationListenerService::class.java).flattenToString()
                names.any { name ->
                    name.equals(expectedComponent, ignoreCase = true) || name.contains(packageName)
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Intent that opens the system Notification Listener Settings screen.
     */
    fun createSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Resolves the overall operational state of notification capture.
     */
    fun getOverallStatus(): NotificationAccessState {
        val hasPermission = isNotificationAccessGranted()
        if (!hasPermission) {
            return NotificationAccessState.PERMISSION_NEEDED
        }
        return if (settingsRepository.isDetectionPaused()) {
            NotificationAccessState.PAUSED
        } else {
            NotificationAccessState.ACTIVE
        }
    }
}
