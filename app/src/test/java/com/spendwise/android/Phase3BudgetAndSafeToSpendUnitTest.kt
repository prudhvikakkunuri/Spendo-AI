package com.spendwise.android

import com.spendwise.android.domain.calculator.SafeToSpendCalculator
import com.spendwise.android.domain.calculator.SafeToSpendInput
import com.spendwise.android.domain.model.BudgetSettings
import com.spendwise.android.domain.model.Commitment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 Unit Tests:
 * Validates SafeToSpendCalculator, paise arithmetic precision, missing inputs,
 * boundary conditions, commitments, refunds, and income double-counting prevention.
 */
class Phase3BudgetAndSafeToSpendUnitTest {

    @Test
    fun safeToSpend_calculatesCorrectDailyAllowanceWithStartingBalance() {
        // Given:
        // Starting balance: ₹85,000.00 (8,500,000 paise)
        // Confirmed debits: ₹25,000.00 (2,500,000 paise)
        // Non-income refunds: ₹2,000.00 (200,000 paise)
        // Unpaid commitments: ₹20,000.00 (2,000,000 paise)
        // Savings reserve: ₹10,000.00 (1,000,000 paise)
        // Remaining days: 16
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = 8500000L,
            monthlyIncomePaise = null,
            savingsReservePaise = 1000000L,
            periodDebitsPaise = 2500000L,
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 200000L,
            remainingCommitmentsPaise = 2000000L,
            daysRemainingInPeriod = 16
        )

        // Available funds = 8,500,000 + 200,000 - 2,500,000 = 6,200,000 paise (₹62,000)
        // Flexible budget = 6,200,000 - 2,000,000 - 1,000,000 = 3,200,000 paise (₹32,000)
        // Daily safe-to-spend = 3,200,000 / 16 = 200,000 paise (₹2,000.00 / day)
        val result = SafeToSpendCalculator.calculate(input)

        assertTrue("Calculation must be marked complete", result.isComplete)
        assertEquals("Available funds mismatch", 6200000L, result.availableFundsPaise)
        assertEquals("Flexible budget mismatch", 3200000L, result.flexibleBudgetPaise)
        assertEquals("Daily safe-to-spend mismatch", 200000L, result.dailySafeToSpendPaise)
        assertEquals("Remaining days mismatch", 16, result.daysRemaining)
        assertTrue("Starting balance should be flagged as used", result.isStartingBalanceUsed)
    }

    @Test
    fun safeToSpend_missingInputs_returnsIncompleteEstimate() {
        // When both starting balance and monthly income are missing:
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = null,
            monthlyIncomePaise = null,
            savingsReservePaise = 1000000L,
            periodDebitsPaise = 500000L,
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 0L,
            remainingCommitmentsPaise = 500000L,
            daysRemainingInPeriod = 10
        )

        val result = SafeToSpendCalculator.calculate(input)

        assertFalse("Must return incomplete when baseline inputs missing", result.isComplete)
        assertNotNull("Incomplete reason must be provided", result.incompleteReason)
        assertEquals("Daily safe to spend must be 0 when incomplete", 0L, result.dailySafeToSpendPaise)
        assertEquals("Flexible budget must be 0 when incomplete", 0L, result.flexibleBudgetPaise)
    }

    @Test
    fun safeToSpend_handlesZeroOrNegativeRemainingDaysSafely() {
        // Zero days remaining (e.g. last hours of last day) must not crash with divide-by-zero
        val inputZeroDays = SafeToSpendInput(
            startingSpendableBalancePaise = 5000000L, // ₹50,000
            monthlyIncomePaise = null,
            savingsReservePaise = 0L,
            periodDebitsPaise = 1000000L, // ₹10,000
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 0L,
            remainingCommitmentsPaise = 0L,
            daysRemainingInPeriod = 0 // Boundary condition!
        )

        val resultZero = SafeToSpendCalculator.calculate(inputZeroDays)
        assertTrue(resultZero.isComplete)
        assertEquals("Days remaining should be clamped to at least 1", 1, resultZero.daysRemaining)
        assertEquals("Daily allowance equals total flexible budget on day 1", 4000000L, resultZero.dailySafeToSpendPaise)

        // Negative days remaining
        val inputNegativeDays = inputZeroDays.copy(daysRemainingInPeriod = -5)
        val resultNegative = SafeToSpendCalculator.calculate(inputNegativeDays)
        assertEquals(1, resultNegative.daysRemaining)
        assertEquals(4000000L, resultNegative.dailySafeToSpendPaise)
    }

    @Test
    fun safeToSpend_overspending_neverReturnsNegativeBudget() {
        // Debits exceed starting funds
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = 1000000L, // ₹10,000
            monthlyIncomePaise = null,
            savingsReservePaise = 500000L, // ₹5,000
            periodDebitsPaise = 1500000L, // ₹15,000 (spent more than starting balance!)
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 0L,
            remainingCommitmentsPaise = 200000L,
            daysRemainingInPeriod = 10
        )

        val result = SafeToSpendCalculator.calculate(input)

        assertTrue(result.isComplete)
        // Available funds = 10,000 - 15,000 = -5,000 (negative)
        assertEquals(-500000L, result.availableFundsPaise)
        // Flexible budget is clamped to 0L, never negative
        assertEquals("Flexible budget must never drop below 0", 0L, result.flexibleBudgetPaise)
        assertEquals("Daily safe to spend must never drop below 0", 0L, result.dailySafeToSpendPaise)
    }

    @Test
    fun safeToSpend_avoidsDoubleCountingIncome_whenBothSettingAndTransactionExist() {
        // User set monthly income setting to ₹100,000 (10,000,000 paise).
        // User also logged an imported salary credit transaction of ₹100,000.
        // The calculator must take maxOf(incomeSetting, actualSalary), NOT sum them (which would be ₹200,000)!
        val inputWithMatchingSalary = SafeToSpendInput(
            startingSpendableBalancePaise = null,
            monthlyIncomePaise = 10000000L, // ₹100,000
            savingsReservePaise = 2000000L, // ₹20,000
            periodDebitsPaise = 3000000L,  // ₹30,000
            incomeCreditsPaise = 10000000L, // ₹100,000 from transaction
            otherCreditsPaise = 0L,
            remainingCommitmentsPaise = 1000000L, // ₹10,000
            daysRemainingInPeriod = 10
        )

        val result = SafeToSpendCalculator.calculate(inputWithMatchingSalary)

        // Base funds should be ₹100,000, NOT ₹200,000
        assertEquals("Base funds must not double-count salary", 10000000L, result.baseFundsPaise)
        // Available funds = 10,000,000 - 3,000,000 = 7,000,000 paise (₹70,000)
        assertEquals(7000000L, result.availableFundsPaise)
        // Flexible budget = 7,000,000 - 1,000,000 - 2,000,000 = 4,000,000 paise (₹40,000)
        assertEquals(4000000L, result.flexibleBudgetPaise)
        // Daily safe to spend = 4,000,000 / 10 = 400,000 paise (₹4,000 / day)
        assertEquals(400000L, result.dailySafeToSpendPaise)

        // When actual salary exceeds expected baseline (e.g. overtime bonus received):
        val inputWithBonusSalary = inputWithMatchingSalary.copy(incomeCreditsPaise = 12000000L) // ₹120,000
        val resultBonus = SafeToSpendCalculator.calculate(inputWithBonusSalary)
        assertEquals("Should use actual higher income credit", 12000000L, resultBonus.baseFundsPaise)
    }

    @Test
    fun safeToSpend_refundCredits_increaseAvailableFundsCautiously() {
        // Non-income credits (like a ₹3,500 refund from an e-commerce return)
        // should increase available funds without inflating salary income baseline.
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = 5000000L, // ₹50,000
            monthlyIncomePaise = null,
            savingsReservePaise = 1000000L,
            periodDebitsPaise = 2000000L, // ₹20,000
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 350000L,  // ₹3,500 refund
            remainingCommitmentsPaise = 0L,
            daysRemainingInPeriod = 10
        )

        val result = SafeToSpendCalculator.calculate(input)

        // Available funds = 50,000 + 3,500 - 20,000 = 33,500 (3,350,000 paise)
        assertEquals(3350000L, result.availableFundsPaise)
        // Flexible budget = 33,500 - 10,000 = 23,500 (2,350,000 paise)
        assertEquals(2350000L, result.flexibleBudgetPaise)
        // Daily safe-to-spend = 2,350,000 / 10 = 235,000 paise (₹2,350.00 / day)
        assertEquals(235000L, result.dailySafeToSpendPaise)
    }

    @Test
    fun budgetSettings_validatesNonNegativeValuesAndMapsToEntity() {
        val settings = BudgetSettings(
            id = 1,
            monthlyIncomePaise = 15000000L,
            startingSpendableBalancePaise = 9000000L,
            savingsReservePaise = 3000000L
        )

        val entity = settings.toEntity()
        assertEquals(1, entity.id)
        assertEquals(15000000L, entity.monthlyIncomePaise)
        assertEquals(9000000L, entity.startingSpendableBalancePaise)
        assertEquals(3000000L, entity.savingsReservePaise)

        val restored = BudgetSettings.fromEntity(entity)
        assertEquals(settings.id, restored.id)
        assertEquals(settings.monthlyIncomePaise, restored.monthlyIncomePaise)
        assertEquals(settings.startingSpendableBalancePaise, restored.startingSpendableBalancePaise)
        assertEquals(settings.savingsReservePaise, restored.savingsReservePaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun budgetSettings_rejectsNegativeMonthlyIncome() {
        BudgetSettings(monthlyIncomePaise = -100L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun budgetSettings_rejectsNegativeStartingBalance() {
        BudgetSettings(startingSpendableBalancePaise = -500L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun budgetSettings_rejectsNegativeSavingsReserve() {
        BudgetSettings(savingsReservePaise = -1000L)
    }

    @Test
    fun commitment_validatesPositiveAmountAndTitle() {
        val commitment = Commitment(
            title = "House Rent",
            amountPaise = 3000000L,
            dueAtEpochMs = 1700000000000L,
            isPaid = false
        )

        assertEquals("House Rent", commitment.title)
        assertEquals(3000000L, commitment.amountPaise)
        assertFalse(commitment.isPaid)

        val entity = commitment.toEntity()
        val restored = Commitment.fromEntity(entity)
        assertEquals(commitment.id, restored.id)
        assertEquals(commitment.title, restored.title)
        assertEquals(commitment.amountPaise, restored.amountPaise)
        assertEquals(commitment.dueAtEpochMs, restored.dueAtEpochMs)
        assertEquals(commitment.isPaid, restored.isPaid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun commitment_rejectsZeroAmount() {
        Commitment(title = "Zero Bill", amountPaise = 0L, dueAtEpochMs = 1700000000000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun commitment_rejectsBlankTitle() {
        Commitment(title = "   ", amountPaise = 100000L, dueAtEpochMs = 1700000000000L)
    }
}
