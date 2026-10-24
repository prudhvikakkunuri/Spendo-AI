package com.spendwise.android.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val selectedDirection: TransactionDirection? = null,
    val selectedCategory: TransactionCategory? = null,
    val selectedSourceType: SourceType? = null,
    val recentlyDeletedTransaction: Transaction? = null,
    val isLoading: Boolean = false
)

class TransactionsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedDirection = MutableStateFlow<TransactionDirection?>(null)
    private val _selectedCategory = MutableStateFlow<TransactionCategory?>(null)
    private val _selectedSourceType = MutableStateFlow<SourceType?>(null)
    private val _recentlyDeleted = MutableStateFlow<Transaction?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.confirmedTransactions,
        _searchQuery,
        _selectedDirection,
        _selectedCategory,
        _selectedSourceType
    ) { allTx, query, direction, category, source ->
        val filtered = allTx.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.merchant.contains(query, ignoreCase = true) ||
                    (tx.note?.contains(query, ignoreCase = true) == true) ||
                    tx.category.displayName.contains(query, ignoreCase = true)

            val matchesDirection = direction == null || tx.direction == direction
            val matchesCategory = category == null || tx.category == category
            val matchesSource = source == null || tx.sourceType == source

            matchesQuery && matchesDirection && matchesCategory && matchesSource
        }

        TransactionsUiState(
            transactions = filtered,
            searchQuery = query,
            selectedDirection = direction,
            selectedCategory = category,
            selectedSourceType = source,
            recentlyDeletedTransaction = _recentlyDeleted.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDirectionFilter(direction: TransactionDirection?) {
        _selectedDirection.value = if (_selectedDirection.value == direction) null else direction
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSourceFilter(source: SourceType?) {
        _selectedSourceType.value = if (_selectedSourceType.value == source) null else source
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedDirection.value = null
        _selectedCategory.value = null
        _selectedSourceType.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _recentlyDeleted.value = transaction
            repository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        val toRestore = _recentlyDeleted.value ?: return
        viewModelScope.launch {
            repository.addTransaction(toRestore)
            _recentlyDeleted.value = null
        }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeleted.value = null
    }

    companion object {
        fun provideFactory(repository: TransactionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TransactionsViewModel(repository) as T
                }
            }
    }
}
