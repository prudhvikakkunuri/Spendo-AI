package com.spendwise.android.domain.model

/**
 * Direction of money movement.
 * Never infer spending from negative numbers alone.
 */
enum class TransactionDirection {
    DEBIT,
    CREDIT
}
