package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized transaction ledger record persisted in Room local SQLite database.
 * Monetary amounts are stored strictly as integer paise (Long) to prevent floating-point inaccuracies.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredAtEpochMillis"),
        Index("categoryId"),
        Index("reviewStatus")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amountPaise: Long,
    val currencyCode: String = "INR",
    val direction: String, // "DEBIT" or "CREDIT"
    val merchant: String? = null,
    val note: String? = null,
    val categoryId: String = "OTHER",
    val occurredAtEpochMillis: Long = System.currentTimeMillis(),
    val sourceType: String = "MANUAL", // "MANUAL" or "NOTIFICATION"
    val sourcePackage: String? = null,
    val confidence: Float? = null,
    val reviewStatus: String = "CONFIRMED", // "CONFIRMED", "NEEDS_REVIEW", "IGNORED"
    val dedupeKey: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val bankAccountId: String? = null,
    val bankAccountName: String? = null
)
