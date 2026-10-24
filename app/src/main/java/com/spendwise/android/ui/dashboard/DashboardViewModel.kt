package com.spendwise.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.repository.BudgetRepository
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.domain.calculator.SafeToSpendResult
import com.spendwise.android.domain.model.CategorySpendSummary
import com.spendwise.android.domain.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val spentTodayPaise: Long = 0L,
    val receivedTodayPaise: Long = 0L,
    val monthSpendPaise: Long = 0L,
    val safeToSpendResult: SafeToSpendResult? = null,
    val categorySpendSummaries: List<CategorySpendSummary> = emptyList(),
    val pendingReviewCount: Int = 0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
) {
    // Convenience helper for UI
    val safeToSpendEstimatePaise: Long
        get() = safeToSpendResult?.dailySafeToSpendPaise ?: 0L

    val isSafeToSpendComplete: Boolean
        get() = safeToSpendResult?.isComplete ?: false
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getTodayDebitsPaise(),
        transactionRepository.getTodayCreditsPaise(),
        transactionRepository.getPeriodDebitsPaise(),
        transactionRepository.getPendingReviewCount(),
        transactionRepository.confirmedTransactions,
        budgetRepository.getSafeToSpendResult(),
        budgetRepository.getCategorySpendSummaries()
    ) { params ->
        val todayDebits = (params[0] as? Number)?.toLong() ?: 0L
        val todayCredits = (params[1] as? Number)?.toLong() ?: 0L
        val periodDebits = (params[2] as? Number)?.toLong() ?: 0L
        val reviewCount = (params[3] as? Number)?.toInt() ?: 0
        @Suppress("UNCHECKED_CAST")
        val confirmedList = (params[4] as? List<Transaction>) ?: emptyList()
        val safeToSpend = params[5] as? SafeToSpendResult
        @Suppress("UNCHECKED_CAST")
        val categorySummaries = (params[6] as? List<CategorySpendSummary>) ?: emptyList()

        DashboardUiState(
            spentTodayPaise = todayDebits,
            receivedTodayPaise = todayCredits,
            monthSpendPaise = periodDebits,
            safeToSpendResult = safeToSpend,
            categorySpendSummaries = categorySummaries,
            pendingReviewCount = reviewCount,
            recentTransactions = confirmedList.take(5),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    companion object {
        fun provideFactory(
            transactionRepository: TransactionRepository,
            budgetRepository: BudgetRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(transactionRepository, budgetRepository) as T
                }
            }
    }
}
