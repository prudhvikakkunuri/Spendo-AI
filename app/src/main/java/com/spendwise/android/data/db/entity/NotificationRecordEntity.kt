package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted local notification record for transaction detection, budget alerts, and motivation.
 * Raw notification text is NOT persisted; only structured, minimal transaction metadata is stored.
 */
@Entity(tableName = "notification_records")
data class NotificationRecordEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val detectedAmountPaise: Long? = null,
    val direction: String? = null, // "DEBIT" or "CREDIT"
    val category: String, // "Transactions", "Budgets", "Motivation"
    val status: String = "DETECTED", // "DETECTED", "CONFIRMED", "IGNORED"
    val postedAtEpochMs: Long,
    val progress: Float? = null,
    val isUnread: Boolean = true
)
