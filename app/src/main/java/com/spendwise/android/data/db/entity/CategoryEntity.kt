package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Category record for classifying expenses and income.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String, // "EXPENSE", "INCOME", "BOTH"
    val isDefault: Boolean = true
)
