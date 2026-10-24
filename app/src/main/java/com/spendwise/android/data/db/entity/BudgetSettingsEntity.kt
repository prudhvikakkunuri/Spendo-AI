package com.spendwise.android.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing user-configured budget parameters.
 * Single row with id = 1 for the active budget configuration.
 */
@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "budget_period")
    val budgetPeriod: String = "MONTHLY",

    @ColumnInfo(name = "monthly_income_paise")
    val monthlyIncomePaise: Long? = null,

    @ColumnInfo(name = "starting_spendable_balance_paise")
    val startingSpendableBalancePaise: Long? = null,

    @ColumnInfo(name = "savings_reserve_paise")
    val savingsReservePaise: Long = 0L,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
