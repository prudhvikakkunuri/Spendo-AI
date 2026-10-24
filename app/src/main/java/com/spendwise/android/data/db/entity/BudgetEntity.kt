package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Budget limit record.
 * Amounts are strictly stored as integer paise (Long).
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String? = null, // null indicates overall wallet/period budget
    val periodType: String, // "DAILY", "MONTHLY"
    val limitPaise: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
