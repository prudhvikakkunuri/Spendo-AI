package com.spendwise.android.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.data.repository.MonitoredAppRepository
import com.spendwise.android.data.repository.NotificationSettingsRepository
import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android NotificationListenerService for SpendWise.
 *
 * Security & Architecture Guarantees:
 * 1. Requires explicit user authorization in Android System Settings.
 * 2. Checks master pause toggle first (synchronous O(1) in-memory check).
 * 3. Enforces user-selected package allowlist (synchronous O(1) in-memory check).
 *    Notifications from unselected apps are immediately dropped without reading content.
 * 4. Extracts title, text, bigText, and timestamp safely in memory.
 * 5. Dispatches processing off the binder thread using a background coroutine scope.
 * 6. Never logs, persists, or transmits raw notification content over network.
 * 7. Defensive error handling ensures malformed/redacted notifications cannot crash the service.
 */
class TransactionNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Dependencies accessed from SpendWiseApplication or test overrides
    private val app: SpendWiseApplication?
        get() = applicationContext as? SpendWiseApplication

    private val monitoredAppRepository: MonitoredAppRepository?
        get() = app?.monitoredAppRepository

    private val settingsRepository: NotificationSettingsRepository?
        get() = app?.notificationSettingsRepository

    private val consumer: TransactionNotificationConsumer?
        get() = app?.notificationConsumer

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            val packageName = sbn.packageName?.toString() ?: return

            // 1. Ignore notifications from SpendWise itself
            if (packageName == applicationContext?.packageName) {
                return
            }

            // 2. Check if detection is paused
            val isPaused = settingsRepository?.isDetectionPaused() ?: false
            if (isPaused) {
                return
            }

            // 3. Strict allowlist enforcement: drop unselected packages immediately
            val isAllowed = monitoredAppRepository?.isPackageAllowed(packageName) ?: false
            if (!isAllowed) {
                return
            }

            // 4. Extract notification data safely in memory
            val notification = sbn.notification ?: return
            val payload = extractPayload(
                packageName = packageName,
                postTime = sbn.postTime,
                flags = notification.flags,
                extras = notification.extras
            ) ?: return

            // 5. Must contain meaningful text to be considered for transaction parsing
            if (!payload.hasTextContent()) {
                return
            }

            // 6. Safe logging: log ONLY package and event presence, NEVER notification text
            Log.d(TAG, "Received notification from allowlisted package: $packageName")

            // 7. Dispatch off the binder callback thread
            val targetConsumer = consumer
            if (targetConsumer != null) {
                serviceScope.launch {
                    try {
                        targetConsumer.onNotificationReceived(payload)
                    } catch (e: Exception) {
                        Log.w(TAG, "Consumer failed to process notification payload safely: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // Defensive shield: never let malformed extras or OEM quirks crash the service
            Log.w(TAG, "Error handling notification safely: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed for removed notifications in MVP
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "TransactionNotificationListenerService connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "TransactionNotificationListenerService disconnected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "SpendWiseListener"

        /**
         * Pure, deterministic extraction function separated for comprehensive unit testing.
         * Handles missing, redacted, and malformed extras defensively.
         */
        fun extractPayload(
            packageName: String,
            postTime: Long,
            flags: Int,
            extras: Bundle?
        ): RawNotificationPayload? {
            if (extras == null) return null

            return try {
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

                extractPayloadFromFields(
                    packageName = packageName,
                    postTime = postTime,
                    flags = flags,
                    title = title,
                    text = text,
                    bigText = bigText,
                    subText = subText
                )
            } catch (_: Exception) {
                // Defensive extraction against BadParcelableException or malformed bundle contents
                null
            }
        }

        /**
         * Pure Kotlin extraction method for testing handling of title, text, bigText,
         * group summary flags, and malformed/redacted fields without Android Bundle dependencies.
         */
        fun extractPayloadFromFields(
            packageName: String,
            postTime: Long,
            flags: Int,
            title: CharSequence?,
            text: CharSequence?,
            bigText: CharSequence?,
            subText: CharSequence?
        ): RawNotificationPayload {
            val isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
            val validTimestamp = if (postTime > 0) postTime else System.currentTimeMillis()

            return RawNotificationPayload(
                packageName = packageName,
                postTimeEpochMs = validTimestamp,
                title = title?.toString()?.trim()?.takeIf { it.isNotBlank() },
                text = text?.toString()?.trim()?.takeIf { it.isNotBlank() },
                bigText = bigText?.toString()?.trim()?.takeIf { it.isNotBlank() },
                subText = subText?.toString()?.trim()?.takeIf { it.isNotBlank() },
                isGroupSummary = isGroupSummary
            )
        }
    }
}
