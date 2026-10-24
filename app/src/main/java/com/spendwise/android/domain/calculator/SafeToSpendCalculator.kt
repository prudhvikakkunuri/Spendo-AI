package com.spendwise.android.domain.calculator

/**
 * Input parameters for Safe-to-Spend deterministic calculation.
 * All monetary amounts are integer paise (Long).
 */
data class SafeToSpendInput(
    val startingSpendableBalancePaise: Long?,
    val monthlyIncomePaise: Long?,
    val savingsReservePaise: Long = 0L,
    val periodDebitsPaise: Long = 0L,
    val incomeCreditsPaise: Long = 0L,
    val otherCreditsPaise: Long = 0L,
    val remainingCommitmentsPaise: Long = 0L,
    val daysRemainingInPeriod: Int
)

/**
 * Result of the Safe-to-Spend calculation.
 */
data class SafeToSpendResult(
    val isComplete: Boolean,
    val incompleteReason: String?,
    val dailySafeToSpendPaise: Long,
    val flexibleBudgetPaise: Long,
    val availableFundsPaise: Long,
    val baseFundsPaise: Long,
    val totalSpendThisMonthPaise: Long,
    val savingsReservePaise: Long,
    val remainingCommitmentsPaise: Long,
    val otherCreditsPaise: Long,
    val daysRemaining: Int,
    val isStartingBalanceUsed: Boolean
) {
    companion object {
        const val DISCLAIMER =
            "This estimate is a mathematical spending projection based on manual inputs and confirmed ledger entries, not a verified bank account balance."

        const val FORMULA_EXPLANATION =
            "Available Funds = Base Funds (Starting Balance or Income) + Non-Income Credits − Confirmed Debits\n" +
            "Flexible Budget = max(0, Available Funds − Remaining Commitments − Savings Reserve)\n" +
            "Daily Safe-to-Spend = Flexible Budget ÷ max(1, Remaining Days)"
    }
}

/**
 * Pure Kotlin calculator for Safe-to-Spend projections.
 *
 * Principles:
 * - 100% integer paise arithmetic using Long.
 * - Zero floating-point operations.
 * - Prevents double-counting when both monthly income setting and salary transactions exist.
 * - Strictly distinguishes manually entered starting balance from ledger totals.
 * - Returns "Estimate incomplete" when required baseline inputs are missing.
 * - Guarantees non-negative daily allowance and safe boundary handling for zero days.
 */
object SafeToSpendCalculator {

    fun calculate(input: SafeToSpendInput): SafeToSpendResult {
        // Validate presence of baseline funds (either starting spendable balance or monthly income)
        if (input.startingSpendableBalancePaise == null && input.monthlyIncomePaise == null) {
            return SafeToSpendResult(
                isComplete = false,
                incompleteReason = "Configure a starting spendable balance or monthly income to calculate daily safe-to-spend.",
                dailySafeToSpendPaise = 0L,
                flexibleBudgetPaise = 0L,
                availableFundsPaise = 0L,
                baseFundsPaise = 0L,
                totalSpendThisMonthPaise = input.periodDebitsPaise,
                savingsReservePaise = input.savingsReservePaise,
                remainingCommitmentsPaise = input.remainingCommitmentsPaise,
                otherCreditsPaise = input.otherCreditsPaise,
                daysRemaining = maxOf(1, input.daysRemainingInPeriod),
                isStartingBalanceUsed = false
            )
        }

        // Determine base funds and avoid double-counting income:
        // If the user entered an explicit starting spendable balance, that balance is the ground truth
        // starting cash pool (which already incorporates salary).
        // If starting balance is omitted, monthly income is used as the base funds.
        // In the monthly income case, if an actual salary/income transaction was recorded in the ledger,
        // we take maxOf(monthlyIncome, incomeCredits) rather than summing them, preventing double-counting.
        val isStartingBalance = input.startingSpendableBalancePaise != null
        val baseFundsPaise: Long = if (isStartingBalance) {
            input.startingSpendableBalancePaise!!
        } else {
            val incomeSetting = input.monthlyIncomePaise ?: 0L
            val actualIncomeCredits = input.incomeCreditsPaise
            maxOf(incomeSetting, actualIncomeCredits)
        }

        // Available funds = Base Funds + other non-income credits (refunds/cashbacks) - confirmed debits
        val availableFundsPaise = baseFundsPaise + input.otherCreditsPaise - input.periodDebitsPaise

        // Deduct savings reserve and upcoming unpaid commitments
        // Flexible budget cannot go below 0
        val unadjustedFlexible = availableFundsPaise - input.remainingCommitmentsPaise - input.savingsReservePaise
        val flexibleBudgetPaise = maxOf(0L, unadjustedFlexible)

        // Calculate daily safe-to-spend
        // Safe division by at least 1 remaining day (handles 0 or negative days remaining gracefully)
        val daysRemaining = maxOf(1, input.daysRemainingInPeriod)
        val dailySafeToSpendPaise = flexibleBudgetPaise / daysRemaining

        return SafeToSpendResult(
            isComplete = true,
            incompleteReason = null,
            dailySafeToSpendPaise = dailySafeToSpendPaise,
            flexibleBudgetPaise = flexibleBudgetPaise,
            availableFundsPaise = availableFundsPaise,
            baseFundsPaise = baseFundsPaise,
            totalSpendThisMonthPaise = input.periodDebitsPaise,
            savingsReservePaise = input.savingsReservePaise,
            remainingCommitmentsPaise = input.remainingCommitmentsPaise,
            otherCreditsPaise = input.otherCreditsPaise,
            daysRemaining = daysRemaining,
            isStartingBalanceUsed = isStartingBalance
        )
    }
}
