package com.spendwise.android.notification.model

/**
 * Minimal in-memory representation of an incoming notification from a user-authorized app.
 *
 * Privacy & Security Constraints:
 * 1. Processed in memory only.
 * 2. Never persisted as raw text in SQLite or any logs.
 * 3. Passed to the transaction detection interface for parsing and then discarded.
 */
data class RawNotificationPayload(
    val packageName: String,
    val postTimeEpochMs: Long,
    val title: String?,
    val text: String?,
    val bigText: String? = null,
    val subText: String? = null,
    val isGroupSummary: Boolean = false
) {
    /**
     * Checks if this notification has meaningful textual content.
     */
    fun hasTextContent(): Boolean {
        return !title.isNullOrBlank() || !text.isNullOrBlank() || !bigText.isNullOrBlank()
    }

    /**
     * Safe string representation that redacts sensitive text content.
     */
    override fun toString(): String {
        return "RawNotificationPayload(package=$packageName, postTime=$postTimeEpochMs, isGroupSummary=$isGroupSummary, hasTitle=${!title.isNullOrBlank()}, hasText=${!text.isNullOrBlank()}, hasBigText=${!bigText.isNullOrBlank()})"
    }
}
