package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Bank Account record for locally tracked accounts.
 * Balances are strictly stored as integer paise (Long).
 */
@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey
    val id: String,
    val bankName: String,
    val accountType: String,
    val maskedAccountNumber: String,
    val balancePaise: Long,
    val openingBalancePaise: Long,
    val isPrimary: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val accountNickname: String? = null,
    val notes: String? = null
) {
    val last4Digits: String
        get() = maskedAccountNumber.filter { it.isDigit() }.takeLast(4)

    val displaySuffix: String
        get() = if (last4Digits.isNotBlank()) "•••• $last4Digits" else "No Suffix"

    val displayNameWithSuffix: String
        get() = accountNickname ?: if (last4Digits.isNotBlank()) "$bankName •••• $last4Digits" else bankName
}
