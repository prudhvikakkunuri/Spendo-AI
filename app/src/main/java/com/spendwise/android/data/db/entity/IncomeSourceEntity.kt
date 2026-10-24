package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Income Source record (salary, recurring advisory, dividends, etc.).
 * Amounts are strictly stored as integer paise (Long).
 */
@Entity(tableName = "income_sources")
data class IncomeSourceEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amountPaise: Long,
    val isRecurring: Boolean,
    val frequency: String, // "Monthly", "Bi-weekly", "Quarterly", "One-time"
    val nextPayDateEpochMs: Long,
    val bankAccountId: String? = null,
    val category: String = "Salary",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
