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
    val updatedAtEpochMs: Long
)
