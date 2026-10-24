package com.spendwise.android.domain.model

/**
 * Domain model representing spending breakdown for a specific category.
 */
data class CategorySpendSummary(
    val category: TransactionCategory,
    val totalSpendPaise: Long,
    val transactionCount: Int,
    val percentageOfTotal: Int = 0
)
