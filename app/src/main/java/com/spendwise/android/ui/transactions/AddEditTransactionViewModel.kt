package com.spendwise.android.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.domain.model.ReviewStatus
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddEditTransactionUiState(
    val id: String = UUID.randomUUID().toString(),
    val isEditMode: Boolean = false,
    val amountInput: String = "",
    val merchantInput: String = "",
    val noteInput: String = "",
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val category: TransactionCategory = TransactionCategory.FOOD,
    val occurredAtEpochMillis: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)

class AddEditTransactionViewModel(
    private val repository: TransactionRepository,
    private val initialTransactionId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    init {
        if (initialTransactionId != null) {
            loadTransaction(initialTransactionId)
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existing = repository.getTransactionByIdDirect(id)
            if (existing != null) {
                val rupees = existing.amountPaise / 100
                val paise = existing.amountPaise % 100
                val amountStr = if (paise == 0L) rupees.toString() else String.format(java.util.Locale.ROOT, "%d.%02d", rupees, paise)

                _uiState.update {
                    it.copy(
                        id = existing.id,
                        isEditMode = true,
                        amountInput = amountStr,
                        merchantInput = existing.merchant,
                        noteInput = existing.note ?: "",
                        direction = existing.direction,
                        category = existing.category,
                        occurredAtEpochMillis = existing.occurredAtEpochMillis,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amountInput = amount, errorMessage = null) }
    }

    fun onMerchantChange(merchant: String) {
        _uiState.update { it.copy(merchantInput = merchant, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(noteInput = note) }
    }

    fun onDirectionChange(direction: TransactionDirection) {
        _uiState.update { it.copy(direction = direction) }
    }

    fun onCategoryChange(category: TransactionCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun saveTransaction() {
        val state = _uiState.value
        val amountDouble = state.amountInput.toDoubleOrNull()

        if (amountDouble == null || amountDouble <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter an amount greater than ₹0.00") }
            return
        }

        if (state.merchantInput.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Merchant or payee description is required") }
            return
        }

        val paise = Math.round(amountDouble * 100.0)
        val transaction = Transaction(
            id = state.id,
            amountPaise = paise,
            direction = state.direction,
            merchant = state.merchantInput.trim(),
            note = state.noteInput.trim().ifEmpty { null },
            category = state.category,
            occurredAtEpochMillis = state.occurredAtEpochMillis,
            sourceType = SourceType.MANUAL,
            reviewStatus = ReviewStatus.CONFIRMED,
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (state.isEditMode) {
                repository.updateTransaction(transaction)
            } else {
                repository.addTransaction(transaction)
            }
            _uiState.update { it.copy(isSaved = true, errorMessage = null) }
        }
    }

    fun deleteTransaction() {
        val state = _uiState.value
        if (state.isEditMode) {
            viewModelScope.launch {
                repository.deleteTransactionById(state.id)
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TransactionRepository,
            transactionId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddEditTransactionViewModel(repository, transactionId) as T
            }
        }
    }
}
