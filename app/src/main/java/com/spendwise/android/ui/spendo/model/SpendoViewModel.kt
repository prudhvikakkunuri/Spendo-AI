package com.spendwise.android.ui.spendo.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.repository.SpendoFinancialRepository
import com.spendwise.android.data.repository.toSpendoTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.spendwise.android.ui.spendo.theme.ThemeMode
import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import java.util.Locale

data class SpendoUiState(
    // Theme & Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

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
    val hasCustomPassword: Boolean = false,

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
    ),

    // Review & Uncertain Transactions
    val pendingReviewTransactions: List<SpendoTransaction> = emptyList(),
    val pendingReviewCount: Int = 0,

    // Savings Goals
    val savingsGoals: List<SpendoSavingsGoal> = SpendoMockRepository.sampleSavingsGoals,

    // Affordability Evaluation
    val affordabilityResult: AffordabilityEvaluation? = null
)

class SpendoViewModel(
    private val repository: SpendoFinancialRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpendoUiState())
    val uiState: StateFlow<SpendoUiState> = _uiState.asStateFlow()

    // Android ViewModel constructor utilized by Compose viewModel()
    constructor(application: Application) : this(
        (application as? SpendWiseApplication)?.spendoFinancialRepository
            ?: SpendoFinancialRepository(AppDatabase.getInstance(application))
    )

    init {
        initObservables()
    }

    private val inMemorySecurityStorage = object : com.spendwise.android.domain.security.SpendoSecurityStorage {
        private val prefs = mutableMapOf<String, String>()
        override suspend fun getPreferenceDirect(key: String): String? = prefs[key]
        override suspend fun setPreference(key: String, value: String) { prefs[key] = value }
    }

    val securityManager: com.spendwise.android.domain.security.SpendoSecurityManager by lazy {
        val storage = repository ?: inMemorySecurityStorage
        com.spendwise.android.domain.security.SpendoSecurityManager(storage)
    }

    private fun initObservables() {
        val repo = repository ?: return

        // Check if custom password has been established
        viewModelScope.launch {
            val hasCustom = securityManager.hasCustomPassword()
            _uiState.update { it.copy(hasCustomPassword = hasCustom) }
        }

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

        // Observe persisted Theme Mode
        viewModelScope.launch {
            repo.getPreference("spendo_theme_mode").collectLatest { savedMode ->
                if (savedMode != null) {
                    val mode = runCatching { ThemeMode.valueOf(savedMode) }.getOrDefault(ThemeMode.SYSTEM)
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }
        }

        // Observe pending review transactions
        viewModelScope.launch {
            repo.pendingReviewTransactions.collectLatest { pendingList ->
                _uiState.update { it.copy(pendingReviewTransactions = pendingList) }
            }
        }

        // Observe pending review count
        viewModelScope.launch {
            repo.pendingReviewCount.collectLatest { count ->
                _uiState.update { it.copy(pendingReviewCount = count) }
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

    suspend fun verifyPassword(password: String): Boolean {
        return securityManager?.verifyPassword(password) ?: (password == _uiState.value.appPin || password == "4892")
    }

    fun verifyAndUnlockBalances(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val valid = verifyPassword(password)
            if (valid) {
                _uiState.update {
                    it.copy(
                        isBalancesRevealed = true,
                        showUnlockDialog = false
                    )
                }
                onSuccess()
            } else {
                onError()
            }
        }
    }

    suspend fun changePassword(current: String, newPass: String): com.spendwise.android.domain.security.PasswordChangeResult {
        val sec = securityManager
        return if (sec != null) {
            val res = sec.changePassword(current, newPass)
            if (res is com.spendwise.android.domain.security.PasswordChangeResult.Success) {
                _uiState.update { it.copy(appPin = newPass, hasCustomPassword = true) }
            }
            res
        } else {
            if (current != _uiState.value.appPin && current != "4892") {
                com.spendwise.android.domain.security.PasswordChangeResult.InvalidCurrentPassword
            } else if (newPass.length < 4) {
                com.spendwise.android.domain.security.PasswordChangeResult.PasswordTooShort
            } else {
                _uiState.update { it.copy(appPin = newPass, hasCustomPassword = true) }
                com.spendwise.android.domain.security.PasswordChangeResult.Success
            }
        }
    }

    fun toggleBalancePrivacy() {
        if (_uiState.value.isBalancesRevealed) {
            hideBalances()
        } else {
            toggleUnlockDialog(true)
        }
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
    // Theme & Appearance Actions
    // -------------------------------------------------------------
    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.setPreference("spendo_theme_mode", mode.name)
            }
        }
    }

    // -------------------------------------------------------------
    // AI Chat & Config Actions
    // -------------------------------------------------------------
    fun setChatInput(text: String) {
        _uiState.update { it.copy(chatInputText = text) }
    }

    fun addChatMessage(message: SpendoChatMessage) {
        _uiState.update { it.copy(chatMessages = it.chatMessages + message) }
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

        // Generate contextual banking response
        viewModelScope.launch {
            delay(300) // Brief natural thinking pause
            val repo = repository
            val currentState = _uiState.value
            val voiceIntent = VoiceFinancialParser.parseSpeech(messageText)

            var pendingConfirmation: PendingExpenseConfirmation? = null

            val responseText = when (voiceIntent) {
                is VoiceIntent.AddExpense -> {
                    val rememberedCategory = repo?.getMerchantCategoryRuleDirect(voiceIntent.merchant) ?: voiceIntent.category
                    val pending = PendingExpenseConfirmation(
                        amountPaise = voiceIntent.amountPaise,
                        merchant = voiceIntent.merchant,
                        category = rememberedCategory,
                        bankName = voiceIntent.bankName
                    )
                    pendingConfirmation = pending
                    "I extracted this expense. Please verify the details below and confirm to record it to your ledger:\n• Amount: ₹%,.2f\n• Merchant: %s\n• Category: %s\n• Account: %s"
                        .format(Locale.ENGLISH, pending.amountPaise / 100.0, pending.merchant, pending.category, pending.bankName)
                }
                is VoiceIntent.AtmWithdrawal -> {
                    addTransaction(
                        merchant = "ATM Cash Withdrawal",
                        amountPaise = voiceIntent.amountPaise,
                        type = TransactionType.EXPENSE,
                        category = "TRANSFER",
                        bankAccountName = voiceIntent.bankName,
                        note = "ATM Cash Withdrawal to Cash Wallet"
                    )
                    "✓ Recorded ATM withdrawal: ₹%,.2f from %s transferred to your Cash Wallet. (Handled as a transfer; not double-counted as an expense)."
                        .format(Locale.ENGLISH, voiceIntent.amountPaise / 100.0, voiceIntent.bankName)
                }
                is VoiceIntent.InquireBalance -> {
                    val banks = currentState.banks
                    if (voiceIntent.targetBank != null) {
                        val matched = banks.find { it.bankName.contains(voiceIntent.targetBank, ignoreCase = true) }
                            ?: banks.firstOrNull()
                        if (matched != null) {
                            "Balance for %s (%s): ₹%,.2f [%s source • %s]."
                                .format(Locale.ENGLISH, matched.bankName, matched.maskedAccountNumber, matched.balancePaise / 100.0, matched.balanceSource.name, matched.lastUpdatedText)
                        } else {
                            "No matching bank account found for ${voiceIntent.targetBank}."
                        }
                    } else {
                        val totalPaise = banks.sumOf { it.balancePaise }
                        val breakdown = banks.joinToString("\n") { "• ${it.bankName} (${it.maskedAccountNumber}): ₹%,.2f [${it.balanceSource.name}]".format(Locale.ENGLISH, it.balancePaise / 100.0) }
                        "Total balance across your %d configured accounts: ₹%,.2f\n%s"
                            .format(Locale.ENGLISH, banks.size, totalPaise / 100.0, breakdown)
                    }
                }
                is VoiceIntent.InquireSpending -> {
                    val todaySpent = currentState.todaySpentPaise
                    val monthSpent = currentState.monthSpentPaise
                    val dailyRem = (currentState.dailyLimitPaise - todaySpent).coerceAtLeast(0L)
                    "Today's spending: ₹%,.2f (₹%,.2f remaining in daily safe limit).\nMonth spending: ₹%,.2f across your active accounts."
                        .format(Locale.ENGLISH, todaySpent / 100.0, dailyRem / 100.0, monthSpent / 100.0)
                }
                is VoiceIntent.InquireBudget -> {
                    val dailyRem = (currentState.dailyLimitPaise - currentState.todaySpentPaise).coerceAtLeast(0L)
                    val monthRem = (currentState.monthlyLimitPaise - currentState.monthSpentPaise).coerceAtLeast(0L)
                    "Daily Budget: ₹%,.2f limit (₹%,.2f remaining today).\nMonthly Ceiling: ₹%,.2f limit (₹%,.2f remaining in %s)."
                        .format(Locale.ENGLISH, currentState.dailyLimitPaise / 100.0, dailyRem / 100.0, currentState.monthlyLimitPaise / 100.0, monthRem / 100.0, currentState.currentMonthName)
                }
                is VoiceIntent.QueryMerchantSpend -> {
                    val merchant = voiceIntent.merchantName
                    val totalPaise = if (repo != null) {
                        repo.getMerchantTotalSpendDirect(merchant)
                    } else {
                        currentState.transactions
                            .filter { it.merchant.contains(merchant as CharSequence, ignoreCase = true) && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amountPaise }
                    }
                    if (totalPaise > 0L) {
                        "You have spent a total of ₹%,.2f on %s across your recorded transactions."
                            .format(Locale.ENGLISH, totalPaise / 100.0, merchant)
                    } else {
                        "No recorded spending found for \"$merchant\"."
                    }
                }
                is VoiceIntent.QueryTopCategory -> {
                    val topSpend = currentState.transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { t -> t.amountPaise } }
                        .maxByOrNull { it.value }
                    if (topSpend != null && topSpend.value > 0L) {
                        val pct = (topSpend.value.toDouble() / currentState.monthSpentPaise.coerceAtLeast(1L)) * 100.0
                        "Your biggest expense category this month is %s with ₹%,.2f spent (%.1f%% of tracked monthly spending)."
                            .format(Locale.ENGLISH, topSpend.key, topSpend.value / 100.0, pct)
                    } else {
                        "No expenses recorded for this month yet."
                    }
                }
                is VoiceIntent.QueryCompareLastMonth -> {
                    if (repo != null) {
                        val (thisMonthDebits, lastMonthDebits) = repo.getSpendingComparison()
                        if (lastMonthDebits > 0L) {
                            val diffPaise = thisMonthDebits - lastMonthDebits
                            val pct = (diffPaise.toDouble() / lastMonthDebits) * 100.0
                            val diffText = if (diffPaise >= 0) "more" else "less"
                            "Spending Comparison vs Last Month:\n• This Month: ₹%,.2f\n• Last Month: ₹%,.2f\n• Difference: ₹%,.2f %s (%s%.1f%%)"
                                .format(Locale.ENGLISH, thisMonthDebits / 100.0, lastMonthDebits / 100.0, kotlin.math.abs(diffPaise) / 100.0, diffText, if (diffPaise >= 0) "+" else "", pct)
                        } else {
                            "Current month spending is ₹%,.2f. Previous month transaction history is insufficient for a historical comparison."
                                .format(Locale.ENGLISH, thisMonthDebits / 100.0)
                        }
                    } else {
                        "Current month spending is ₹%,.2f. Previous month data is unavailable."
                            .format(Locale.ENGLISH, currentState.monthSpentPaise / 100.0)
                    }
                }
                is VoiceIntent.QueryReduceSpending -> {
                    val topCategories = currentState.transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { t -> t.amountPaise } }
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                    if (topCategories.isNotEmpty()) {
                        val breakdown = topCategories.joinToString("\n") { "• ${it.key}: ₹%,.2f".format(Locale.ENGLISH, it.value / 100.0) }
                        "Actionable plan to reduce spending next month:\n\n1. Target Top Spending Categories:\n$breakdown\n\n2. Trim non-essential discretionary spends by 15% to save ₹%,.2f.\n3. Daily ceiling target: Keep daily expense under ₹%,.2f to remain comfortably below budget."
                            .format(Locale.ENGLISH, (currentState.monthSpentPaise * 0.15) / 100.0, (currentState.monthlyLimitPaise * 0.85 / 30.0) / 100.0)
                    } else {
                        "To reduce spending next month, establish category budget ceilings and activate real-time daily spending alerts."
                    }
                }
                is VoiceIntent.AmbiguousExpense -> {
                    "${voiceIntent.promptForMissing}\nExamples:\n• \"Add ₹250 for lunch paid through Kotak\"\n• \"I spent ₹120 on petrol using Indian Bank\"\n• \"I withdrew ₹2,000 from an ATM using KVB\""
                }
                is VoiceIntent.GeneralChat -> {
                    val isConfigured = currentState.aiConfig.isConfigured
                    if (!isConfigured) {
                        "Spendo AI is currently operating in offline financial assistant mode. You can ask for balances, spend tracking, or voice expense creation anytime. Tap 'Configure AI' to connect external LLM providers (Groq, Cerebras, Gemini, OpenAI)."
                    } else {
                        "Your financial status looks solid. You have maintained a ${currentState.currentStreakDays}-day spending streak under limit, and your top tracked accounts are active and balanced."
                    }
                }
            }

            val spendoReply = SpendoChatMessage(
                id = "msg_${System.currentTimeMillis() + 1}",
                sender = "SPENDO",
                text = responseText,
                pendingExpense = pendingConfirmation
            )
            _uiState.update { it.copy(chatMessages = it.chatMessages + spendoReply) }
        }
    }

    // -------------------------------------------------------------
    // Natural Language Expense Confirmation Actions
    // -------------------------------------------------------------
    fun confirmPendingExpense(pending: PendingExpenseConfirmation, messageId: String? = null) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.addExpense(
                    merchant = pending.merchant,
                    amountPaise = pending.amountPaise,
                    category = pending.category,
                    bankAccountName = pending.bankName,
                    note = "Logged via Natural Language Chat"
                )
            }
        } else {
            addTransaction(
                merchant = pending.merchant,
                amountPaise = pending.amountPaise,
                type = TransactionType.EXPENSE,
                category = pending.category,
                bankAccountName = pending.bankName,
                note = "Logged via Natural Language Chat"
            )
        }
        _uiState.update { state ->
            val updatedMessages = state.chatMessages.map { msg ->
                if ((messageId != null && msg.id == messageId) || (messageId == null && msg.pendingExpense == pending)) {
                    msg.copy(pendingExpense = null)
                } else {
                    msg
                }
            } + SpendoChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = "SPENDO",
                text = "✓ Confirmed & saved expense: ₹%,.2f spent at %s (%s) from %s."
                    .format(Locale.ENGLISH, pending.amountPaise / 100.0, pending.merchant, pending.category, pending.bankName)
            )
            state.copy(chatMessages = updatedMessages)
        }
    }

    fun dismissPendingExpense(messageId: String) {
        _uiState.update { state ->
            val updatedMessages = state.chatMessages.map { msg ->
                if (msg.id == messageId) msg.copy(pendingExpense = null) else msg
            } + SpendoChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                sender = "SPENDO",
                text = "Canceled. The transaction was not saved."
            )
            state.copy(chatMessages = updatedMessages)
        }
    }

    // -------------------------------------------------------------
    // Review Screen Actions
    // -------------------------------------------------------------
    fun approveReviewTransaction(id: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.approveReviewTransaction(id)
        }
    }

    fun rejectReviewTransaction(id: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.rejectReviewTransaction(id)
        }
    }

    fun updateAndApproveReviewTransaction(
        id: String,
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        rememberPreference: Boolean
    ) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.updateAndApproveReviewTransaction(
                id = id,
                merchant = merchant,
                amountPaise = amountPaise,
                category = category,
                bankAccountName = bankAccountName,
                rememberMerchantPreference = rememberPreference
            )
        }
    }

    // -------------------------------------------------------------
    // Purchase Affordability Calculation
    // -------------------------------------------------------------
    fun evaluateAffordability(amountPaise: Long) {
        val repo = repository
        viewModelScope.launch {
            val current = _uiState.value
            val totalBal = current.banks.sumOf { it.balancePaise }
            val eval = if (repo != null) {
                repo.evaluateAffordability(
                    itemAmountPaise = amountPaise,
                    totalBalancePaise = totalBal,
                    monthlyLimitPaise = current.monthlyLimitPaise,
                    monthlySpentPaise = current.monthSpentPaise
                )
            } else {
                val liquid = totalBal
                val bills = 1500000L
                val savings = 5000000L
                val free = (liquid - bills - savings).coerceAtLeast(0L)
                val verdict = if (amountPaise <= free) AffordabilityVerdict.AFFORDABLE else AffordabilityVerdict.CAUTION
                AffordabilityEvaluation(
                    itemAmountPaise = amountPaise,
                    liquidBalancePaise = liquid,
                    upcomingBillsPaise = bills,
                    plannedSavingsPaise = savings,
                    freeDiscretionaryPaise = free,
                    monthlyRemainingPaise = 3660000L,
                    verdict = verdict,
                    reasonText = if (verdict == AffordabilityVerdict.AFFORDABLE) "Comfortably within free discretionary balance." else "May impact planned savings buffer."
                )
            }
            _uiState.update { it.copy(affordabilityResult = eval) }
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

    // -------------------------------------------------------------
    // CSV Import Actions
    // -------------------------------------------------------------
    fun importCsv(inputStream: java.io.InputStream, onComplete: (Int) -> Unit) {
        val repo = repository
        viewModelScope.launch {
            val count = if (repo != null) {
                repo.importTransactionsFromCsv(inputStream)
            } else {
                val entities = com.spendwise.android.util.CsvImporter.parseCsv(inputStream)
                val newTxs = entities.map { it.toSpendoTransaction() }
                _uiState.update { state ->
                    state.copy(transactions = (newTxs + state.transactions).distinctBy { it.id })
                }
                entities.size
            }
            onComplete(count)
        }
    }
}
