package com.spendwise.android.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendwise.android.data.repository.BudgetRepository
import com.spendwise.android.domain.calculator.SafeToSpendResult
import com.spendwise.android.domain.model.BudgetSettings
import com.spendwise.android.domain.model.Commitment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetUiState(
    val monthlyIncomeInput: String = "",
    val startingBalanceInput: String = "",
    val savingsReserveInput: String = "",
    val commitments: List<Commitment> = emptyList(),
    val safeToSpendResult: SafeToSpendResult? = null,
    val isInitialized: Boolean = false,
    val saveMessage: String? = null,
    val errorMessage: String? = null
)

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _monthlyIncomeInput = MutableStateFlow("")
    private val _startingBalanceInput = MutableStateFlow("")
    private val _savingsReserveInput = MutableStateFlow("")
    private val _isUserEditing = MutableStateFlow(false)
    private val _saveMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.budgetSettings,
        repository.allCommitments,
        repository.getSafeToSpendResult(),
        _monthlyIncomeInput,
        _startingBalanceInput,
        _savingsReserveInput,
        _isUserEditing,
        _saveMessage,
        _errorMessage
    ) { params ->
        val settings = params[0] as BudgetSettings?
        @Suppress("UNCHECKED_CAST")
        val commitments = params[1] as List<Commitment>
        val result = params[2] as SafeToSpendResult
        val incomeInput = params[3] as String
        val balanceInput = params[4] as String
        val reserveInput = params[5] as String
        val userEditing = params[6] as Boolean
        val saveMsg = params[7] as String?
        val errorMsg = params[8] as String?

        // Populate fields from database initially unless user has started editing
        val finalIncome = if (!userEditing && settings?.monthlyIncomePaise != null) {
            (settings.monthlyIncomePaise / 100L).toString()
        } else incomeInput

        val finalBalance = if (!userEditing && settings?.startingSpendableBalancePaise != null) {
            (settings.startingSpendableBalancePaise / 100L).toString()
        } else balanceInput

        val finalReserve = if (!userEditing && settings != null) {
            (settings.savingsReservePaise / 100L).toString()
        } else reserveInput

        BudgetUiState(
            monthlyIncomeInput = finalIncome,
            startingBalanceInput = finalBalance,
            savingsReserveInput = finalReserve,
            commitments = commitments,
            safeToSpendResult = result,
            isInitialized = true,
            saveMessage = saveMsg,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun onMonthlyIncomeChanged(value: String) {
        _isUserEditing.value = true
        _monthlyIncomeInput.value = value.filter { it.isDigit() }
    }

    fun onStartingBalanceChanged(value: String) {
        _isUserEditing.value = true
        _startingBalanceInput.value = value.filter { it.isDigit() }
    }

    fun onSavingsReserveChanged(value: String) {
        _isUserEditing.value = true
        _savingsReserveInput.value = value.filter { it.isDigit() }
    }

    fun saveSettings() {
        val incomeRupees = _monthlyIncomeInput.value.toLongOrNull()
        val balanceRupees = _startingBalanceInput.value.toLongOrNull()
        val reserveRupees = _savingsReserveInput.value.toLongOrNull() ?: 0L

        val incomePaise = incomeRupees?.let { it * 100L }
        val balancePaise = balanceRupees?.let { it * 100L }
        val reservePaise = reserveRupees * 100L

        viewModelScope.launch {
            try {
                val newSettings = BudgetSettings(
                    id = 1,
                    monthlyIncomePaise = incomePaise,
                    startingSpendableBalancePaise = balancePaise,
                    savingsReservePaise = reservePaise,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                repository.saveBudgetSettings(newSettings)
                _saveMessage.value = "Budget configuration saved successfully."
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save: ${e.localizedMessage}"
            }
        }
    }

    fun addCommitment(title: String, amountRupees: String, dueDayOfMonth: Int) {
        if (title.isBlank()) {
            _errorMessage.value = "Commitment title cannot be blank."
            return
        }
        val amount = amountRupees.toLongOrNull()
        if (amount == null || amount <= 0L) {
            _errorMessage.value = "Enter a valid positive commitment amount."
            return
        }

        val cal = Calendar.getInstance()
        val clampedDay = dueDayOfMonth.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.DAY_OF_MONTH, clampedDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)

        val commitment = Commitment(
            title = title.trim(),
            amountPaise = amount * 100L,
            dueAtEpochMs = cal.timeInMillis,
            isPaid = false
        )

        viewModelScope.launch {
            try {
                repository.addCommitment(commitment)
                _saveMessage.value = "Added commitment \"${commitment.title}\"."
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add commitment: ${e.localizedMessage}"
            }
        }
    }

    fun toggleCommitmentPaid(commitment: Commitment) {
        viewModelScope.launch {
            repository.updateCommitment(
                commitment.copy(
                    isPaid = !commitment.isPaid,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteCommitment(commitment: Commitment) {
        viewModelScope.launch {
            repository.deleteCommitment(commitment)
        }
    }

    fun clearMessages() {
        _saveMessage.value = null
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(repository: BudgetRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetViewModel(repository) as T
                }
            }
    }
}
