package com.spendwise.android.data.repository

import com.spendwise.android.data.db.dao.BudgetSettingsDao
import com.spendwise.android.data.db.dao.CommitmentDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.domain.calculator.SafeToSpendCalculator
import com.spendwise.android.domain.calculator.SafeToSpendInput
import com.spendwise.android.domain.calculator.SafeToSpendResult
import com.spendwise.android.domain.model.BudgetSettings
import com.spendwise.android.domain.model.CategorySpendSummary
import com.spendwise.android.domain.model.Commitment
import com.spendwise.android.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Repository coordinating budget configuration, commitments, and real-time safe-to-spend projections.
 */
class BudgetRepository(
    private val budgetSettingsDao: BudgetSettingsDao,
    private val commitmentDao: CommitmentDao,
    private val transactionDao: TransactionDao
) {

    val budgetSettings: Flow<BudgetSettings?> = budgetSettingsDao.getBudgetSettings()
        .map { entity -> entity?.let { BudgetSettings.fromEntity(it) } }

    val allCommitments: Flow<List<Commitment>> = commitmentDao.getAllCommitments()
        .map { entities -> entities.map { Commitment.fromEntity(it) } }

    val unpaidCommitments: Flow<List<Commitment>> = commitmentDao.getUnpaidCommitments()
        .map { entities -> entities.map { Commitment.fromEntity(it) } }

    suspend fun saveBudgetSettings(settings: BudgetSettings) {
        budgetSettingsDao.insertOrUpdate(settings.toEntity())
    }

    suspend fun addCommitment(commitment: Commitment) {
        commitmentDao.insert(commitment.toEntity())
    }

    suspend fun updateCommitment(commitment: Commitment) {
        commitmentDao.update(commitment.toEntity())
    }

    suspend fun deleteCommitment(commitment: Commitment) {
        commitmentDao.delete(commitment.toEntity())
    }

    suspend fun clearBudgetData() {
        budgetSettingsDao.clear()
        commitmentDao.clear()
    }

    /**
     * Category-level spending breakdown for the current monthly period.
     * Maps raw database totals into domain summaries with percentages.
     */
    fun getCategorySpendSummaries(startOfPeriod: Long = getStartOfMonthEpochMs()): Flow<List<CategorySpendSummary>> {
        return transactionDao.getCategorySpendTotals(startOfPeriod).map { spendingList ->
            val totalAllPaise = spendingList.sumOf { it.totalPaise }
            spendingList.map { item ->
                val category = TransactionCategory.fromId(item.categoryId)
                val percentage = if (totalAllPaise > 0L) {
                    ((item.totalPaise * 100L) / totalAllPaise).toInt()
                } else 0
                CategorySpendSummary(
                    category = category,
                    totalSpendPaise = item.totalPaise,
                    transactionCount = item.count,
                    percentageOfTotal = percentage
                )
            }
        }
    }

    /**
     * Reactive Safe-to-Spend projection stream.
     * Combines user budget settings, unpaid commitments due this month,
     * month-to-date confirmed debits, and non-income credits.
     */
    fun getSafeToSpendResult(): Flow<SafeToSpendResult> {
        val startOfMonth = getStartOfMonthEpochMs()
        val endOfMonth = getEndOfMonthEpochMs()
        val daysRemaining = getRemainingDaysInMonth()

        return combine(
            budgetSettings,
            commitmentDao.sumPendingCommitmentsDueBefore(endOfMonth),
            transactionDao.getPeriodDebitsPaise(startOfMonth),
            transactionDao.getPeriodIncomeCredits(startOfMonth),
            transactionDao.getPeriodRefundCredits(startOfMonth)
        ) { settings, pendingCommitmentsPaise, periodDebits, incomeCredits, refundCredits ->
            val input = SafeToSpendInput(
                startingSpendableBalancePaise = settings?.startingSpendableBalancePaise,
                monthlyIncomePaise = settings?.monthlyIncomePaise,
                savingsReservePaise = settings?.savingsReservePaise ?: 0L,
                periodDebitsPaise = periodDebits,
                incomeCreditsPaise = incomeCredits,
                otherCreditsPaise = refundCredits,
                remainingCommitmentsPaise = pendingCommitmentsPaise,
                daysRemainingInPeriod = daysRemaining
            )
            SafeToSpendCalculator.calculate(input)
        }
    }

    companion object {
        fun getStartOfMonthEpochMs(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfMonthEpochMs(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        fun getRemainingDaysInMonth(): Int {
            val cal = Calendar.getInstance()
            val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            return maxOf(1, totalDaysInMonth - currentDay + 1)
        }
    }
}
