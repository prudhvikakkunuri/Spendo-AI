package com.spendwise.android.notification

import com.spendwise.android.notification.model.RawNotificationPayload

/**
 * Interface for consuming raw notification payloads extracted from allowlisted apps.
 *
 * This decouples the Android NotificationListenerService from the business logic
 * and subsequent transaction parsing (implemented in Phase 5).
 */
interface TransactionNotificationConsumer {

    /**
     * Called asynchronously on a background thread when a valid notification
     * arrives from an authorized monitored application.
     */
    suspend fun onNotificationReceived(payload: RawNotificationPayload)
}
