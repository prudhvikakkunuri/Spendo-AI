package com.spendwise.android.data.db.entity

import androidx.room.ColumnInfo

/**
 * POJO for aggregate Room query returning spending sum and count per category.
 */
data class CategorySpending(
    @ColumnInfo(name = "categoryId")
    val categoryId: String,

    @ColumnInfo(name = "totalPaise")
    val totalPaise: Long,

    @ColumnInfo(name = "count")
    val count: Int
)
