package com.spendwise.android.domain.model

/**
 * Standard expense and income categories per spec.
 */
enum class TransactionCategory(val displayName: String) {
    FOOD("Food & Dining"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    HEALTH("Health & Medical"),
    ENTERTAINMENT("Entertainment"),
    INCOME("Income & Salary"),
    TRANSFER("Account Transfer"),
    OTHER("Other");

    companion object {
        fun fromId(id: String?): TransactionCategory {
            if (id == null) return OTHER
            return try {
                if (id.equals("TRANSFERS", ignoreCase = true)) {
                    TRANSFER
                } else {
                    valueOf(id.uppercase())
                }
            } catch (e: Exception) {
                OTHER
            }
        }
    }
}
