package com.spendwise.android.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.domain.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Review Queue for uncertain and detected transactions.
 */
class ReviewViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val reviewTransactions: StateFlow<List<Transaction>> = repository.reviewTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun confirmTransaction(id: String) {
        viewModelScope.launch {
            repository.confirmReviewTransaction(id)
        }
    }

    fun ignoreTransaction(id: String) {
        viewModelScope.launch {
            repository.ignoreReviewTransaction(id)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    companion object {
        fun provideFactory(
            repository: TransactionRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReviewViewModel(repository) as T
            }
        }
    }
}
