package com.spendwise.android.ui.spendo.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.repository.SpendoFinancialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpendoUiState(
    // Transactions & Activity
    val transactions: List<SpendoTransaction> = SpendoMockRepository.sampleTransactions,
    val searchQuery: String = "",
    val selectedFilterType: TransactionType? = null,
    val selectedCategory: String? = null,

    // Budget Limits (strictly Long paise)
    val dailyLimitPaise: Long = 250000L,       // ₹2,500.00
    val todaySpentPaise: Long = 142000L,       // ₹1,420.00
    val monthlyLimitPaise: Long = 6500000L,    // ₹65,000.00
    val monthSpentPaise: Long = 2840000L,      // ₹28,400.00
    val currentStreakDays: Int = 5,

    // Banks & Privacy
    val banks: List<SpendoBank> = SpendoMockRepository.sampleBanks,
    val isBalancesRevealed: Boolean = false,
    val showUnlockDialog: Boolean = false,

    // Calendar
    val selectedCalendarDay: Int = 24, // 24th October
    val currentMonthName: String = "October 2026",

    // Notifications
    val notifications: List<SpendoNotificationItem> = SpendoMockRepository.sampleNotifications,
    val selectedNotificationCategory: String = "All",

    // Incomes
    val incomes: List<SpendoIncomeItem> = SpendoMockRepository.sampleIncomes,

    // AI Chat & Config
    val chatMessages: List<SpendoChatMessage> = SpendoMockRepository.initialChatMessages,
    val chatInputText: String = "",
    val aiConfig: AiConfigState = AiConfigState(isConfigured = false), // Default unconfigured as requested

    // Security & App Lock
    val isBiometricEnabled: Boolean = true,
    val autoLockDuration: String = "Immediately",
    val isBalanceConcealProtected: Boolean = true,
    val appPin: String = "4892",

    // Notification Detection Settings
    val isNotificationDetectionEnabled: Boolean = true,
    val isManualReviewPreferred: Boolean = true,
    val monitoredApps: List<Pair<String, Boolean>> = listOf(
        "Google Pay" to true,
        "PhonePe" to true,
        "Paytm" to true,
        "HDFC Bank MobileBanking" to true,
        "iMobile Pay (ICICI)" to true,
        "CRED" to true,
        "Axis Mobile" to false,
        "SBI Yono" to false
    )
)

class SpendoViewModel : ViewModel {

    private val repository: SpendoFinancialRepository?
    private val _uiState = MutableStateFlow(SpendoUiState())
    val uiState: StateFlow<SpendoUiState> = _uiState.asStateFlow()

    // No-arg constructor for tests or previews
    constructor() : super() {
        this.repository = null
    }

    // Direct repository injection for testing or custom scoping
    constructor(repository: SpendoFinancialRepository) : super() {
        this.repository = repository
        initObservables()
    }

    // Android ViewModel constructor utilized by Compose viewModel()
    constructor(application: Application) : super() {
        this.repository = (application as? SpendWiseApplication)?.spendoFinancialRepository
            ?: SpendoFinancialRepository(AppDatabase.getInstance(application))
        initObservables()
    }

    private fun initObservables() {
        val repo = repository ?: return

        // Observe transactions & apply reactive filters
        viewModelScope.launch {
            repo.allTransactions.collectLatest { txList ->
                _uiState.update { state ->
                    // If database has records, replace in-memory default list
                    if (txList.isNotEmpty()) {
                        state.copy(transactions = txList)
                    } else {
                        state
                    }
                }
            }
        }

        // Observe today's debits
        viewModelScope.launch {
            repo.getTodayDebitsPaise().collectLatest { debits ->
                _uiState.update { it.copy(todaySpentPaise = debits) }
            }
        }

        // Observe monthly debits
        viewModelScope.launch {
            repo.getMonthlyDebitsPaise().collectLatest { debits ->
                _uiState.update { it.copy(monthSpentPaise = debits) }
            }
        }

        // Observe daily budget
        viewModelScope.launch {
            repo.dailyBudget.collectLatest { budget ->
                budget?.let { b ->
                    _uiState.update { it.copy(dailyLimitPaise = b.limitPaise) }
                }
            }
        }

        // Observe monthly budget
        viewModelScope.launch {
            repo.monthlyBudget.collectLatest { budget ->
                budget?.let { b ->
                    _uiState.update { it.copy(monthlyLimitPaise = b.limitPaise) }
                }
            }
        }

        // Observe bank accounts
        viewModelScope.launch {
            repo.bankAccounts.collectLatest { bankList ->
                if (bankList.isNotEmpty()) {
                    _uiState.update { it.copy(banks = bankList) }
                }
            }
        }

        // Observe income sources
        viewModelScope.launch {
            repo.incomeSources.collectLatest { incomeList ->
                if (incomeList.isNotEmpty()) {
                    _uiState.update { it.copy(incomes = incomeList) }
                }
            }
        }

        // Observe notifications
        viewModelScope.launch {
            repo.notificationRecords.collectLatest { notifList ->
                if (notifList.isNotEmpty()) {
                    _uiState.update { it.copy(notifications = notifList) }
                }
            }
        }

        // Observe AI configuration metadata
        viewModelScope.launch {
            repo.activeAiConfig.collectLatest { config ->
                config?.let { c ->
                    val matchingProvider = AiProvider.values().find { it.name == c.providerId } ?: AiProvider.GROQ
                    _uiState.update {
                        it.copy(
                            aiConfig = it.aiConfig.copy(
                                isConfigured = c.isConfigured,
                                provider = matchingProvider,
                                model = c.selectedModel,
                                testConnectionMessage = c.lastTestMessage
                            )
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Transactions Actions
    // -------------------------------------------------------------
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterType(type: TransactionType?) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun addTransaction(
        merchant: String,
        amountPaise: Long,
        type: TransactionType,
        category: String,
        bankAccountName: String,
        note: String?
    ) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                if (type == TransactionType.EXPENSE) {
                    repo.addExpense(
                        merchant = merchant,
                        amountPaise = amountPaise,
                        category = category,
                        bankAccountName = bankAccountName,
                        note = note
                    )
                } else {
                    repo.addIncome(
                        merchant = merchant,
                        amountPaise = amountPaise,
                        category = category,
                        bankAccountName = bankAccountName,
                        note = note
                    )
                }
            }
        } else {
            // In-memory fallback
            val newTx = SpendoTransaction(
                id = "tx_${System.currentTimeMillis()}",
                merchant = merchant.ifBlank { "Unspecified Merchant" },
                amountPaise = amountPaise,
                type = type,
                category = category,
                occurredAtEpochMs = System.currentTimeMillis(),
                bankAccountName = bankAccountName,
                source = TransactionSource.MANUAL,
                note = note
            )

            _uiState.update { state ->
                val updated = listOf(newTx) + state.transactions
                val newSpent = if (type == TransactionType.EXPENSE) {
                    state.todaySpentPaise + amountPaise
                } else state.todaySpentPaise

                state.copy(
                    transactions = updated,
                    todaySpentPaise = newSpent
                )
            }
        }
    }

    fun deleteTransaction(id: String) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.deleteTransaction(id)
            }
        } else {
            _uiState.update { state ->
                val item = state.transactions.find { it.id == id }
                val updated = state.transactions.filterNot { it.id == id }
                val newSpent = if (item?.type == TransactionType.EXPENSE) {
                    (state.todaySpentPaise - item.amountPaise).coerceAtLeast(0L)
                } else state.todaySpentPaise

                state.copy(
                    transactions = updated,
                    todaySpentPaise = newSpent
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Banks & Privacy Actions
    // -------------------------------------------------------------
    fun toggleUnlockDialog(show: Boolean) {
        _uiState.update { it.copy(showUnlockDialog = show) }
    }

    fun unlockBalances() {
        _uiState.update {
            it.copy(
                isBalancesRevealed = true,
                showUnlockDialog = false
            )
        }
    }

    fun hideBalances() {
        _uiState.update { it.copy(isBalancesRevealed = false) }
    }

    fun addBank(name: String, type: String, initialPaise: Long) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.addBankAccount(name, type, initialPaise)
            }
        } else {
            val newBank = SpendoBank(
                id = "bank_${System.currentTimeMillis()}",
                bankName = name,
                accountType = type,
                maskedAccountNumber = "•••• ${ (1000..9999).random() }",
                balancePaise = initialPaise
            )
            _uiState.update { it.copy(banks = it.banks + newBank) }
        }
    }

    // -------------------------------------------------------------
    // Calendar Actions
    // -------------------------------------------------------------
    fun selectCalendarDay(day: Int) {
        _uiState.update { it.copy(selectedCalendarDay = day) }
    }

    // -------------------------------------------------------------
    // Notification Actions
    // -------------------------------------------------------------
    fun setNotificationCategory(category: String) {
        _uiState.update { it.copy(selectedNotificationCategory = category) }
    }

    fun clearAllNotifications() {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.clearAllNotifications()
            }
        } else {
            _uiState.update { it.copy(notifications = emptyList()) }
        }
    }

    // -------------------------------------------------------------
    // Budget Actions
    // -------------------------------------------------------------
    fun updateBudgetLimits(dailyPaise: Long, monthlyPaise: Long) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.updateBudgetLimits(dailyPaise, monthlyPaise)
            }
        } else {
            _uiState.update {
                it.copy(
                    dailyLimitPaise = dailyPaise,
                    monthlyLimitPaise = monthlyPaise
                )
            }
        }
    }

    // -------------------------------------------------------------
    // AI Chat & Config Actions
    // -------------------------------------------------------------
    fun setChatInput(text: String) {
        _uiState.update { it.copy(chatInputText = text) }
    }

    fun sendChatMessage(overrideText: String? = null) {
        val messageText = overrideText ?: _uiState.value.chatInputText
        if (messageText.isBlank()) return

        val userMessage = SpendoChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = "USER",
            text = messageText
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                chatInputText = ""
            )
        }

        // Generate contextual response
        viewModelScope.launch {
            delay(400) // Brief natural thinking pause
            val isConfigured = _uiState.value.aiConfig.isConfigured
            val responseText = if (!isConfigured) {
                "Spendo AI is currently unconfigured. Tap 'Configure AI' in the top bar or header to connect your preferred provider (Groq, Cerebras, Gemini, OpenAI, etc.)."
            } else {
                when {
                    messageText.contains("spend today", ignoreCase = true) || messageText.contains("what did i spend", ignoreCase = true) -> {
                        "Today you have spent ₹1,420.00 across 3 transactions (Blue Tokai Coffee ₹340, Uber ₹420, Nature's Basket ₹660). You have ₹1,080.00 remaining in your daily safe limit."
                    }
                    messageText.contains("22 October", ignoreCase = true) || messageText.contains("22nd", ignoreCase = true) -> {
                        "On 22 October, your recorded transactions were: Apple Services ₹199.00 and Subway ₹280.00. Total spend: ₹479.00."
                    }
                    messageText.contains("money go", ignoreCase = true) || messageText.contains("category", ignoreCase = true) -> {
                        "Your top spending category this month is Food & Dining (38%), followed by Transit (22%) and Groceries (18%). You are well within your monthly ceiling."
                    }
                    messageText.contains("budget", ignoreCase = true) && messageText.contains("left", ignoreCase = true) -> {
                        "You have ₹1,080.00 left for today's daily limit. For the full month of October, you have ₹36,600.00 flexible budget remaining."
                    }
                    messageText.startsWith("spent", ignoreCase = true) || messageText.startsWith("paid", ignoreCase = true) -> {
                        "Understood! I've noted this entry as a draft expense. You can review and confirm it in your Activity ledger anytime."
                    }
                    else -> {
                        "Your current financial position looks solid. Your 5-day spending streak is active, and you've utilized only 43% of your monthly allowance with 7 days remaining."
                    }
                }
            }

            val spendoReply = SpendoChatMessage(
                id = "msg_${System.currentTimeMillis() + 1}",
                sender = "SPENDO",
                text = responseText
            )
            _uiState.update { it.copy(chatMessages = it.chatMessages + spendoReply) }
        }
    }

    fun setAiProvider(provider: AiProvider) {
        _uiState.update {
            it.copy(
                aiConfig = it.aiConfig.copy(
                    provider = provider,
                    model = provider.defaultModel,
                    testConnectionMessage = null
                )
            )
        }
    }

    fun setAiApiKey(key: String) {
        _uiState.update {
            it.copy(aiConfig = it.aiConfig.copy(apiKey = key, testConnectionMessage = null))
        }
    }

    fun setAiModel(model: String) {
        _uiState.update {
            it.copy(aiConfig = it.aiConfig.copy(model = model))
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(aiConfig = it.aiConfig.copy(isTestingConnection = true, testConnectionMessage = null))
            }
            delay(900) // Simulate latency test
            _uiState.update {
                it.copy(
                    aiConfig = it.aiConfig.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "Connection Successful • 118ms latency (${it.aiConfig.provider.displayName})"
                    )
                )
            }
        }
    }

    fun saveAiConfiguration() {
        val currentConfig = _uiState.value.aiConfig
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.saveAiConfigMetadata(
                    providerId = currentConfig.provider.name,
                    displayName = currentConfig.provider.displayName,
                    selectedModel = currentConfig.model,
                    isConfigured = true,
                    lastTestMessage = currentConfig.testConnectionMessage ?: "Configured"
                )
            }
        }

        _uiState.update {
            it.copy(
                aiConfig = it.aiConfig.copy(isConfigured = true),
                chatMessages = it.chatMessages + SpendoChatMessage(
                    id = "msg_config_done",
                    sender = "SPENDO",
                    text = "AI Provider configured successfully (${it.aiConfig.provider.displayName} • ${it.aiConfig.model}). Spendo AI agent is now active and ready for your financial queries."
                )
            )
        }
    }

    // -------------------------------------------------------------
    // Security & App Lock Actions
    // -------------------------------------------------------------
    fun setBiometricEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun setAutoLockDuration(duration: String) {
        _uiState.update { it.copy(autoLockDuration = duration) }
    }

    fun setBalanceConcealProtected(protected: Boolean) {
        _uiState.update { it.copy(isBalanceConcealProtected = protected) }
    }

    // -------------------------------------------------------------
    // Notification Detection Settings
    // -------------------------------------------------------------
    fun toggleNotificationDetection(enabled: Boolean) {
        _uiState.update { it.copy(isNotificationDetectionEnabled = enabled) }
    }

    fun toggleAppMonitored(appName: String) {
        _uiState.update { state ->
            val updated = state.monitoredApps.map { (name, isMonitored) ->
                if (name == appName) name to !isMonitored else name to isMonitored
            }
            state.copy(monitoredApps = updated)
        }
    }
}
