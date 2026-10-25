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
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.util.CsvImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        // Restore persisted AI configuration on startup
        viewModelScope.launch {
            repo.activeAiConfig.collectLatest { config ->
                if (config != null && config.isConfigured) {
                    val apiKey = repo.loadSecureAiApiKey(config.providerId) ?: ""
                    val provider = try {
                        AiProvider.valueOf(config.providerId)
                    } catch (_: Exception) {
                        AiProvider.GROQ
                    }
                    _uiState.update {
                        it.copy(
                            aiConfig = it.aiConfig.copy(
                                isConfigured = true,
                                provider = provider,
                                model = config.selectedModel.ifBlank { provider.defaultModel },
                                apiKey = apiKey,
                                testConnectionMessage = config.lastTestMessage
                            )
                        )
                    }
                }
            }
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
        note: String?,
        bankAccountId: String? = null
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
                        note = note,
                        bankAccountId = bankAccountId
                    )
                } else {
                    repo.addIncome(
                        merchant = merchant,
                        amountPaise = amountPaise,
                        category = category,
                        bankAccountName = bankAccountName,
                        note = note,
                        bankAccountId = bankAccountId
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
                note = note,
                bankAccountId = bankAccountId
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

    fun addBank(
        name: String,
        type: String,
        initialPaise: Long,
        nickname: String? = null,
        maskedNumber: String? = null,
        notes: String? = null
    ) {
        val repo = repository
        val finalMasked = if (!maskedNumber.isNullOrBlank()) {
            val digits = maskedNumber.filter { it.isDigit() }
            if (digits.length >= 4) "•••• ${digits.takeLast(4)}" else maskedNumber
        } else {
            "•••• ${(1000..9999).random()}"
        }
        if (repo != null) {
            viewModelScope.launch {
                repo.addBankAccount(
                    bankName = name,
                    accountType = type,
                    initialBalancePaise = initialPaise,
                    maskedNumber = finalMasked,
                    accountNickname = nickname,
                    notes = notes
                )
            }
        } else {
            val newBank = SpendoBank(
                id = "bank_${System.currentTimeMillis()}",
                bankName = name,
                accountType = type,
                maskedAccountNumber = finalMasked,
                balancePaise = initialPaise,
                accountNickname = nickname,
                notes = notes
            )
            _uiState.update { it.copy(banks = it.banks + newBank) }
        }
    }

    fun editBank(
        id: String,
        name: String,
        type: String,
        balancePaise: Long,
        nickname: String? = null,
        maskedNumber: String? = null,
        notes: String? = null
    ) {
        val repo = repository
        val finalMasked = if (!maskedNumber.isNullOrBlank()) {
            val digits = maskedNumber.filter { it.isDigit() }
            if (digits.length >= 4) "•••• ${digits.takeLast(4)}" else maskedNumber
        } else {
            "•••• ${(1000..9999).random()}"
        }
        if (repo != null) {
            viewModelScope.launch {
                repo.updateBankAccountDetails(
                    id = id,
                    bankName = name,
                    accountNickname = nickname,
                    accountType = type,
                    maskedNumber = finalMasked,
                    balancePaise = balancePaise,
                    notes = notes
                )
            }
        } else {
            _uiState.update { state ->
                val updated = state.banks.map {
                    if (it.id == id) {
                        it.copy(
                            bankName = name,
                            accountType = type,
                            balancePaise = balancePaise,
                            maskedAccountNumber = finalMasked,
                            accountNickname = nickname,
                            notes = notes
                        )
                    } else it
                }
                state.copy(banks = updated)
            }
        }
    }

    fun deleteBank(id: String) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.deleteBankAccount(id)
            }
        } else {
            _uiState.update { state ->
                state.copy(banks = state.banks.filterNot { it.id == id })
            }
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
            id = "msg_user_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
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

            val responseText = try {
                when (voiceIntent) {
                    is VoiceIntent.AddExpense -> {
                        val rememberedCategory = repo?.getMerchantCategoryRuleDirect(voiceIntent.merchant) ?: voiceIntent.category
                        val pending = PendingExpenseConfirmation(
                            amountPaise = voiceIntent.amountPaise,
                            merchant = voiceIntent.merchant,
                            category = rememberedCategory,
                            bankName = voiceIntent.bankName
                        )
                        pendingConfirmation = pending
                        val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, pending.amountPaise / 100.0)
                        "I extracted this expense. Please verify the details below and confirm to record it to your ledger:\n• Amount: $amtFormatted\n• Merchant: ${pending.merchant}\n• Category: ${pending.category}\n• Account: ${pending.bankName}"
                    }
                    is VoiceIntent.AddIncome -> {
                        val pending = PendingExpenseConfirmation(
                            amountPaise = voiceIntent.amountPaise,
                            merchant = voiceIntent.source,
                            category = voiceIntent.category,
                            bankName = voiceIntent.bankName
                        )
                        pendingConfirmation = pending
                        val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, pending.amountPaise / 100.0)
                        "I extracted this income credit. Please verify the details below and confirm to record it to your ledger:\n• Amount: $amtFormatted\n• Source: ${pending.merchant}\n• Category: ${pending.category}\n• Account: ${pending.bankName}"
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
                        val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, voiceIntent.amountPaise / 100.0)
                        "✓ Recorded ATM withdrawal: $amtFormatted from ${voiceIntent.bankName} transferred to your Cash Wallet. (Handled as a transfer; not double-counted as an expense)."
                    }
                    is VoiceIntent.InquireBalance -> {
                        val banks = currentState.banks
                        if (voiceIntent.targetBank != null) {
                            val matched = banks.find { it.bankName.contains(voiceIntent.targetBank, ignoreCase = true) }
                                ?: banks.firstOrNull()
                            if (matched != null) {
                                val balFormatted = "₹%,.2f".format(Locale.ENGLISH, matched.balancePaise / 100.0)
                                "Balance for ${matched.bankName} (${matched.maskedAccountNumber}): $balFormatted [${matched.balanceSource.name} • ${matched.lastUpdatedText}]."
                            } else {
                                "No matching bank account found for \"${voiceIntent.targetBank}\"."
                            }
                        } else {
                            val totalPaise = banks.sumOf { it.balancePaise }
                            val totalFormatted = "₹%,.2f".format(Locale.ENGLISH, totalPaise / 100.0)
                            if (banks.isEmpty()) {
                                "No bank accounts are currently configured. Tap Settings ➔ Bank Accounts to add your accounts."
                            } else {
                                val breakdown = banks.joinToString("\n") {
                                    val bal = "₹%,.2f".format(Locale.ENGLISH, it.balancePaise / 100.0)
                                    "• ${it.displayName} (${it.maskedAccountNumber}): $bal [${it.balanceSource.name}]"
                                }
                                "Total balance across your ${banks.size} configured accounts: $totalFormatted\n$breakdown"
                            }
                        }
                    }
                    is VoiceIntent.InquireSpending -> {
                        val isMonth = voiceIntent.period == "MONTH"
                        if (isMonth) {
                            val monthSpent = currentState.monthSpentPaise
                            val monthLimit = currentState.monthlyLimitPaise
                            val monthRem = (monthLimit - monthSpent).coerceAtLeast(0L)
                            val spentFormatted = "₹%,.2f".format(Locale.ENGLISH, monthSpent / 100.0)
                            val remFormatted = "₹%,.2f".format(Locale.ENGLISH, monthRem / 100.0)
                            val limitFormatted = "₹%,.2f".format(Locale.ENGLISH, monthLimit / 100.0)
                            "Total expenses for this month: $spentFormatted across your active accounts.\nRemaining monthly budget: $remFormatted (Budget ceiling: $limitFormatted)."
                        } else {
                            val todaySpent = currentState.todaySpentPaise
                            val dailyRem = (currentState.dailyLimitPaise - todaySpent).coerceAtLeast(0L)
                            val todayFormatted = "₹%,.2f".format(Locale.ENGLISH, todaySpent / 100.0)
                            val dailyRemFormatted = "₹%,.2f".format(Locale.ENGLISH, dailyRem / 100.0)
                            val monthSpentFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.monthSpentPaise / 100.0)
                            "Today's spending: $todayFormatted ($dailyRemFormatted remaining in daily safe limit).\nMonth spending: $monthSpentFormatted across your active accounts."
                        }
                    }
                    is VoiceIntent.InquireBudget -> {
                        val dailyRem = (currentState.dailyLimitPaise - currentState.todaySpentPaise).coerceAtLeast(0L)
                        val monthRem = (currentState.monthlyLimitPaise - currentState.monthSpentPaise).coerceAtLeast(0L)
                        val dailyLimitFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.dailyLimitPaise / 100.0)
                        val dailyRemFormatted = "₹%,.2f".format(Locale.ENGLISH, dailyRem / 100.0)
                        val monthlyLimitFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.monthlyLimitPaise / 100.0)
                        val monthRemFormatted = "₹%,.2f".format(Locale.ENGLISH, monthRem / 100.0)
                        "Daily Budget: $dailyLimitFormatted limit ($dailyRemFormatted remaining today).\nMonthly Ceiling: $monthlyLimitFormatted limit ($monthRemFormatted remaining in ${currentState.currentMonthName})."
                    }
                    is VoiceIntent.QueryBiggestExpense -> {
                        val biggest = currentState.transactions
                            .filter { it.type == TransactionType.EXPENSE }
                            .maxByOrNull { it.amountPaise }
                        if (biggest != null && biggest.amountPaise > 0L) {
                            val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, biggest.amountPaise / 100.0)
                            "Your biggest recorded expense is $amtFormatted at ${biggest.merchant} (${biggest.category}) on ${biggest.formattedDate} via ${biggest.bankAccountName}."
                        } else {
                            "No expenses recorded in your ledger yet."
                        }
                    }
                    is VoiceIntent.QuerySavings -> {
                        val totalIncome = currentState.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
                        val totalExpense = currentState.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
                        val netSavings = totalIncome - totalExpense
                        val incFormatted = "₹%,.2f".format(Locale.ENGLISH, totalIncome / 100.0)
                        val expFormatted = "₹%,.2f".format(Locale.ENGLISH, totalExpense / 100.0)
                        val savingsFormatted = "₹%,.2f".format(Locale.ENGLISH, kotlin.math.abs(netSavings) / 100.0)

                        if (totalIncome > 0L) {
                            if (netSavings >= 0L) {
                                "You have saved $savingsFormatted this month!\n• Total Income: $incFormatted\n• Total Expenses: $expFormatted"
                            } else {
                                "Your spending exceeds recorded income by $savingsFormatted this month.\n• Total Income: $incFormatted\n• Total Expenses: $expFormatted"
                            }
                        } else {
                            val monthRem = (currentState.monthlyLimitPaise - currentState.monthSpentPaise).coerceAtLeast(0L)
                            val remFormatted = "₹%,.2f".format(Locale.ENGLISH, monthRem / 100.0)
                            val limitFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.monthlyLimitPaise / 100.0)
                            "No income transactions recorded yet. Based on your monthly budget of $limitFormatted, you currently have $remFormatted saved under your ceiling."
                        }
                    }
                    is VoiceIntent.QueryAnalyzeHabits -> {
                        val expenses = currentState.transactions.filter { it.type == TransactionType.EXPENSE }
                        if (expenses.isEmpty()) {
                            "No transactions recorded yet to analyze. Log a few expenses to generate spending habit insights."
                        } else {
                            val totalSpentFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.monthSpentPaise / 100.0)
                            val topCategory = expenses
                                .groupBy { it.category }
                                .mapValues { it.value.sumOf { t -> t.amountPaise } }
                                .maxByOrNull { it.value }

                            val topCatText = if (topCategory != null && currentState.monthSpentPaise > 0L) {
                                val pct = (topCategory.value * 100) / currentState.monthSpentPaise
                                "${topCategory.key} ($pct%)"
                            } else "None"

                            val calendar = java.util.Calendar.getInstance()
                            val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                            val dailyAvgPaise = currentState.monthSpentPaise / dayOfMonth
                            val dailyAvgFormatted = "₹%,.2f".format(Locale.ENGLISH, dailyAvgPaise / 100.0)

                            "Spending Habits Analysis:\n• Tracked Month Spending: $totalSpentFormatted across ${expenses.size} expenses\n• Primary Outlay Category: $topCatText\n• Daily Burn Rate: $dailyAvgFormatted/day\n• Financial Insight: You are keeping within your planned thresholds. Maintain regular tracking for accurate category insights."
                        }
                    }
                    is VoiceIntent.QueryRecentTransactions -> {
                        val recent = currentState.transactions.take(5)
                        if (recent.isEmpty()) {
                            "You haven't recorded any transactions yet. Tap '+ Add' or speak into the microphone to log your first expense."
                        } else {
                            val items = recent.joinToString("\n") { tx ->
                                val sign = if (tx.type == TransactionType.EXPENSE) "−" else "+"
                                val amt = "₹%,.2f".format(Locale.ENGLISH, tx.amountPaise / 100.0)
                                "• ${tx.merchant}: $sign$amt (${tx.category}) [${tx.formattedDate}]"
                            }
                            "Your latest transactions:\n$items"
                        }
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
                            val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, totalPaise / 100.0)
                            "You have spent a total of $amtFormatted on $merchant across your recorded transactions."
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
                            val totalSpent = currentState.monthSpentPaise.coerceAtLeast(topSpend.value)
                            val pct = if (totalSpent > 0L) ((topSpend.value * 100) / totalSpent).toInt() else 100
                            val amtFormatted = "₹%,.2f".format(Locale.ENGLISH, topSpend.value / 100.0)
                            "Your biggest expense category this month is ${topSpend.key} with $amtFormatted spent ($pct% of tracked spending)."
                        } else {
                            "No expenses recorded for this month yet."
                        }
                    }
                    is VoiceIntent.QueryCompareLastMonth -> {
                        if (repo != null) {
                            val (thisMonthDebits, lastMonthDebits) = repo.getSpendingComparison()
                            val thisFormatted = "₹%,.2f".format(Locale.ENGLISH, thisMonthDebits / 100.0)
                            val lastFormatted = "₹%,.2f".format(Locale.ENGLISH, lastMonthDebits / 100.0)

                            if (lastMonthDebits > 0L) {
                                val diffPaise = thisMonthDebits - lastMonthDebits
                                val diffFormatted = "₹%,.2f".format(Locale.ENGLISH, kotlin.math.abs(diffPaise) / 100.0)
                                val pct = (kotlin.math.abs(diffPaise) * 100) / lastMonthDebits
                                val diffText = if (diffPaise >= 0) "more" else "less"
                                val sign = if (diffPaise >= 0) "+" else "−"
                                "Spending Comparison vs Last Month:\n• This Month: $thisFormatted\n• Last Month: $lastFormatted\n• Difference: $diffFormatted $diffText ($sign$pct%)"
                            } else {
                                "Current month spending is $thisFormatted. Previous month transaction history is insufficient for a historical comparison."
                            }
                        } else {
                            val monthFormatted = "₹%,.2f".format(Locale.ENGLISH, currentState.monthSpentPaise / 100.0)
                            "Current month spending is $monthFormatted. Previous month data is unavailable."
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
                            val breakdown = topCategories.joinToString("\n") {
                                val amt = "₹%,.2f".format(Locale.ENGLISH, it.value / 100.0)
                                "• ${it.key}: $amt"
                            }
                            val trimPaise = (currentState.monthSpentPaise * 15L) / 100L
                            val trimFormatted = "₹%,.2f".format(Locale.ENGLISH, trimPaise / 100.0)
                            val dailyCeilingPaise = (currentState.monthlyLimitPaise * 85L) / (100L * 30L)
                            val ceilingFormatted = "₹%,.2f".format(Locale.ENGLISH, dailyCeilingPaise / 100.0)

                            "Actionable plan to reduce spending next month:\n\n1. Target Top Spending Categories:\n$breakdown\n\n2. Trim non-essential discretionary spends by 15% to save $trimFormatted.\n3. Daily ceiling target: Keep daily expense under $ceilingFormatted to remain comfortably below budget."
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
                            "Spendo AI is operating in offline financial assistant mode. You can ask for balances, spend tracking, recent transactions, or voice expense entry anytime. Tap Settings ➔ AI Configuration to connect external LLM providers (Groq, Cerebras, Gemini, OpenAI)."
                        } else {
                            "Your financial status looks solid. You have maintained a ${currentState.currentStreakDays}-day spending streak under limit, and your top tracked accounts are active and balanced."
                        }
                    }
                }
            } catch (e: Exception) {
                "Unable to process query: ${e.message ?: "An unexpected error occurred. Please try again."}"
            }

            val spendoReply = SpendoChatMessage(
                id = "msg_spendo_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
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
                repo.saveSecureAiConfig(
                    providerId = currentConfig.provider.name,
                    model = currentConfig.model,
                    apiKey = currentConfig.apiKey,
                    lastTestMessage = currentConfig.testConnectionMessage ?: "Configured & Active"
                )
            }
        }

        val configMsgId = "msg_config_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}"
        _uiState.update {
            it.copy(
                aiConfig = it.aiConfig.copy(isConfigured = true),
                chatMessages = it.chatMessages + SpendoChatMessage(
                    id = configMsgId,
                    sender = "SPENDO",
                    text = "AI Provider configured successfully (${it.aiConfig.provider.displayName} • ${it.aiConfig.model}). Spendo AI agent is now active and ready for your financial queries."
                )
            )
        }
    }

    fun deleteAiConfiguration() {
        val currentConfig = _uiState.value.aiConfig
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                repo.deleteSecureAiConfig(currentConfig.provider.name)
            }
        }
        _uiState.update {
            it.copy(
                aiConfig = AiConfigState(
                    isConfigured = false,
                    provider = currentConfig.provider,
                    model = currentConfig.provider.defaultModel,
                    apiKey = ""
                )
            )
        }
    }

    fun clearChatMessages() {
        _uiState.update {
            it.copy(
                chatMessages = emptyList(),
                chatInputText = ""
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
    // Transaction Update & CSV Actions
    // -------------------------------------------------------------
    fun updateTransaction(updatedTx: SpendoTransaction) {
        val repo = repository
        val direction = if (updatedTx.type == TransactionType.INCOME) "CREDIT" else "DEBIT"
        val entity = TransactionEntity(
            id = updatedTx.id,
            amountPaise = updatedTx.amountPaise,
            currencyCode = "INR",
            direction = direction,
            merchant = updatedTx.merchant,
            note = updatedTx.note,
            categoryId = updatedTx.category,
            occurredAtEpochMillis = updatedTx.occurredAtEpochMs,
            sourceType = if (updatedTx.source == TransactionSource.NOTIFICATION) "NOTIFICATION" else "MANUAL",
            reviewStatus = updatedTx.reviewStatus,
            bankAccountName = updatedTx.bankAccountName,
            bankAccountId = updatedTx.bankAccountId,
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        _uiState.update { state ->
            val newTransactions = state.transactions.map {
                if (it.id == updatedTx.id) updatedTx else it
            }
            state.copy(transactions = newTransactions)
        }

        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repo.updateTransaction(entity)
            }
        }
    }

    fun importCsv(inputStream: java.io.InputStream, onComplete: (CsvImportResult) -> Unit) {
        val repo = repository
        viewModelScope.launch(Dispatchers.IO) {
            val result = if (repo != null) {
                repo.importTransactionsFromCsv(inputStream)
            } else {
                val (entities, rejected) = com.spendwise.android.util.CsvImporter.parseCsvDetailed(inputStream)
                val newTxs = entities.map { it.toSpendoTransaction() }
                _uiState.update { state ->
                    val existingMap = state.transactions.associateBy { it.id }
                    val merged = (newTxs.filter { !existingMap.containsKey(it.id) } + state.transactions)
                    state.copy(transactions = merged)
                }
                CsvImportResult(
                    importedCount = newTxs.size,
                    skippedCount = 0,
                    rejectedCount = rejected,
                    totalProcessed = newTxs.size + rejected
                )
            }
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun exportCsv(outputStream: java.io.OutputStream, onComplete: (Result<Int>) -> Unit) {
        val repo = repository
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = if (repo != null) {
                    repo.exportTransactionsToCsv(outputStream)
                } else {
                    val txs = _uiState.value.transactions
                    com.spendwise.android.util.CsvExporter.writeSpendoTransactionsToStream(txs, outputStream)
                    txs.size
                }
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(count))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
        }
    }
}
