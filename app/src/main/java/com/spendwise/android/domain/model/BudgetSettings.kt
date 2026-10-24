package com.spendwise.android.domain.model

import com.spendwise.android.data.db.entity.BudgetSettingsEntity

/**
 * Domain model representing budget and safe-to-spend baseline configuration.
 *
 * Rules:
 * - All monetary amounts are stored in integer paise (Long).
 * - startingSpendableBalancePaise is optional and entered manually by the user.
 * - monthlyIncomePaise is an optional baseline used for reference or when starting balance is not provided.
 * - savingsReservePaise is protected from daily safe-to-spend calculations.
 */
data class BudgetSettings(
    val id: Int = 1,
    val budgetPeriod: String = "MONTHLY",
    val monthlyIncomePaise: Long? = null,
    val startingSpendableBalancePaise: Long? = null,
    val savingsReservePaise: Long = 0L,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    init {
        monthlyIncomePaise?.let {
            require(it >= 0L) { "Monthly income must be non-negative paise." }
        }
        startingSpendableBalancePaise?.let {
            require(it >= 0L) { "Starting spendable balance must be non-negative paise." }
        }
        require(savingsReservePaise >= 0L) { "Savings reserve must be non-negative paise." }
    }

    fun toEntity(): BudgetSettingsEntity {
        return BudgetSettingsEntity(
            id = id,
            budgetPeriod = budgetPeriod,
            monthlyIncomePaise = monthlyIncomePaise,
            startingSpendableBalancePaise = startingSpendableBalancePaise,
            savingsReservePaise = savingsReservePaise,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    companion object {
        fun fromEntity(entity: BudgetSettingsEntity): BudgetSettings {
            return BudgetSettings(
                id = entity.id,
                budgetPeriod = entity.budgetPeriod,
                monthlyIncomePaise = entity.monthlyIncomePaise,
                startingSpendableBalancePaise = entity.startingSpendableBalancePaise,
                savingsReservePaise = entity.savingsReservePaise,
                createdAtEpochMs = entity.createdAtEpochMs,
                updatedAtEpochMs = entity.updatedAtEpochMs
            )
        }
    }
}
