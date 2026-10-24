package com.spendwise.android.data.repository

import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.db.entity.AiProviderConfigEntity
import com.spendwise.android.data.db.entity.AppPreferenceEntity
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.data.db.entity.BudgetEntity
import com.spendwise.android.data.db.entity.CategoryEntity
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.IncomeSourceEntity
import com.spendwise.android.data.db.entity.NotificationRecordEntity
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoIncomeItem
import com.spendwise.android.ui.spendo.model.SpendoMockRepository
import com.spendwise.android.ui.spendo.model.SpendoNotificationItem
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.TransactionSource
import com.spendwise.android.ui.spendo.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Single local-first repository for all Spendo financial data.
 * Adheres strictly to Integer Paise (Long) for monetary operations.
 */
class SpendoFinancialRepository(
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val bankAccountDao = database.bankAccountDao()
    private val incomeSourceDao = database.incomeSourceDao()
    private val notificationRecordDao = database.notificationRecordDao()
    private val appPreferenceDao = database.appPreferenceDao()
    private val aiProviderConfigDao = database.aiProviderConfigDao()

    init {
        // Seed default foundational data if empty
        coroutineScope.launch {
            seedDefaultsIfEmpty()
        }
    }

    // -------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------

    val allTransactions: Flow<List<SpendoTransaction>> = transactionDao.getAllTransactions().map { entities ->
        entities.map { it.toSpendoTransaction() }
    }

    val confirmedTransactions: Flow<List<SpendoTransaction>> = transactionDao.getConfirmedTransactions().map { entities ->
        entities.map { it.toSpendoTransaction() }
    }

    fun getTransactionById(id: String): Flow<SpendoTransaction?> {
        return transactionDao.getTransactionById(id).map { it?.toSpendoTransaction() }
    }

    fun searchTransactions(query: String): Flow<List<SpendoTransaction>> {
        return if (query.isBlank()) {
            allTransactions
        } else {
            transactionDao.searchTransactions(query).map { entities ->
                entities.map { it.toSpendoTransaction() }
            }
        }
    }

    fun filterTransactions(
        searchQuery: String,
        type: TransactionType?,
        category: String?
    ): Flow<List<SpendoTransaction>> {
        return allTransactions.map { list ->
            list.filter { tx ->
                val matchesSearch = searchQuery.isBlank() ||
                    tx.merchant.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    tx.category.contains(searchQuery, ignoreCase = true)

                val matchesType = type == null || tx.type == type
                val matchesCategory = category == null || tx.category.equals(category, ignoreCase = true)

                matchesSearch && matchesType && matchesCategory
            }
        }
    }

    suspend fun addExpense(
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        note: String?,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        require(amountPaise > 0) { "Expense amount must be strictly positive integer paise" }
        val id = "tx_${UUID.randomUUID()}"
        val entity = TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = merchant.ifBlank { "Unspecified Merchant" },
            note = note,
            categoryId = category,
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = bankAccountName
        )
        transactionDao.insertTransaction(entity)

        // Adjust bank account balance (debit deducts)
        adjustBankBalanceByName(bankAccountName, -amountPaise)
        return id
    }

    suspend fun addIncome(
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        note: String?,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        require(amountPaise > 0) { "Income amount must be strictly positive integer paise" }
        val id = "tx_${UUID.randomUUID()}"
        val entity = TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = merchant.ifBlank { "Salary / Payout" },
            note = note,
            categoryId = category,
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = bankAccountName
        )
        transactionDao.insertTransaction(entity)

        // Adjust bank account balance (credit adds)
        adjustBankBalanceByName(bankAccountName, amountPaise)
        return id
    }

    suspend fun updateTransaction(
        id: String,
        merchant: String,
        amountPaise: Long,
        type: TransactionType,
        category: String,
        bankAccountName: String,
        note: String?,
        occurredAtEpochMs: Long
    ) {
        require(amountPaise > 0) { "Updated amount must be strictly positive integer paise" }
        val existing = transactionDao.getTransactionByIdDirect(id) ?: return
        val direction = if (type == TransactionType.EXPENSE) "DEBIT" else "CREDIT"

        // 1. Reverse previous balance impact
        existing.bankAccountName?.let { oldBank ->
            if (oldBank.isNotBlank()) {
                val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
                adjustBankBalanceByName(oldBank, reverseDelta)
            }
        }

        // 2. Apply new balance impact
        if (bankAccountName.isNotBlank()) {
            val applyDelta = if (direction == "DEBIT") -amountPaise else amountPaise
            adjustBankBalanceByName(bankAccountName, applyDelta)
        }

        val updated = existing.copy(
            merchant = merchant,
            amountPaise = amountPaise,
            direction = direction,
            categoryId = category,
            bankAccountName = bankAccountName,
            note = note,
            occurredAtEpochMillis = occurredAtEpochMs,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updated)
    }

    suspend fun deleteTransaction(id: String) {
        val existing = transactionDao.getTransactionByIdDirect(id) ?: return
        transactionDao.deleteTransactionById(id)

        // Reverse balance impact if bank known
        existing.bankAccountName?.let { bankName ->
            val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
            adjustBankBalanceByName(bankName, reverseDelta)
        }
    }

    // -------------------------------------------------------------
    // Transfers Between Accounts & External Transfers
    // -------------------------------------------------------------

    suspend fun recordTransferBetweenAccounts(
        sourceBankName: String,
        destinationBankName: String,
        amountPaise: Long,
        note: String? = null,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ): Pair<String, String> {
        require(amountPaise > 0) { "Transfer amount must be strictly positive integer paise" }
        require(sourceBankName != destinationBankName) { "Source and destination accounts must be distinct" }

        val now = System.currentTimeMillis()
        val debitId = "tx_xfer_out_${UUID.randomUUID()}"
        val creditId = "tx_xfer_in_${UUID.randomUUID()}"

        // Outgoing debit (TRANSFER category -> excluded from expenses)
        val debitEntity = TransactionEntity(
            id = debitId,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Transfer to $destinationBankName",
            note = note,
            categoryId = "TRANSFER",
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountName = sourceBankName
        )

        // Incoming credit (TRANSFER category -> excluded from income)
        val creditEntity = TransactionEntity(
            id = creditId,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = "Transfer from $sourceBankName",
            note = note,
            categoryId = "TRANSFER",
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountName = destinationBankName
        )

        transactionDao.insertTransactions(listOf(debitEntity, creditEntity))

        // Balance impact: source decreases, destination increases
        adjustBankBalanceByName(sourceBankName, -amountPaise)
        adjustBankBalanceByName(destinationBankName, amountPaise)

        return Pair(debitId, creditId)
    }

    suspend fun recordIncomingTransfer(
        toBankName: String,
        amountPaise: Long,
        senderDescription: String = "External Account",
        note: String? = null,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        require(amountPaise > 0) { "Transfer amount must be strictly positive integer paise" }
        val id = "tx_xfer_in_${UUID.randomUUID()}"
        val entity = TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = "Transfer from $senderDescription",
            note = note,
            categoryId = "TRANSFER",
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = toBankName
        )
        transactionDao.insertTransaction(entity)
        adjustBankBalanceByName(toBankName, amountPaise)
        return id
    }

    suspend fun recordOutgoingTransfer(
        fromBankName: String,
        amountPaise: Long,
        recipientDescription: String = "External Account",
        note: String? = null,
        occurredAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        require(amountPaise > 0) { "Transfer amount must be strictly positive integer paise" }
        val id = "tx_xfer_out_${UUID.randomUUID()}"
        val entity = TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Transfer to $recipientDescription",
            note = note,
            categoryId = "TRANSFER",
            occurredAtEpochMillis = occurredAtEpochMs,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = fromBankName
        )
        transactionDao.insertTransaction(entity)
        adjustBankBalanceByName(fromBankName, -amountPaise)
        return id
    }

    suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long {
        return transactionDao.getDebitsInDateRangeDirect(startTime, endTime)
    }

    suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long {
        return transactionDao.getCreditsInDateRangeDirect(startTime, endTime)
    }

    // -------------------------------------------------------------
    // Totals & Calculations
    // -------------------------------------------------------------

    fun getTodayDebitsPaise(): Flow<Long> {
        val (startOfDay, endOfDay) = getTodayEpochRange()
        return transactionDao.getTodayDebitsPaise(startOfDay, endOfDay)
    }

    fun getTodayCreditsPaise(): Flow<Long> {
        val (startOfDay, endOfDay) = getTodayEpochRange()
        return transactionDao.getTodayCreditsPaise(startOfDay, endOfDay)
    }

    fun getMonthlyDebitsPaise(): Flow<Long> {
        val startOfMonth = getStartOfMonthEpoch()
        return transactionDao.getPeriodDebitsPaise(startOfMonth)
    }

    fun getCategorySpendTotals(): Flow<List<CategorySpending>> {
        val startOfMonth = getStartOfMonthEpoch()
        return transactionDao.getCategorySpendTotals(startOfMonth)
    }

    // -------------------------------------------------------------
    // Budgets
    // -------------------------------------------------------------

    val dailyBudget: Flow<BudgetEntity?> = budgetDao.getOverallBudgetByPeriod("DAILY")
    val monthlyBudget: Flow<BudgetEntity?> = budgetDao.getOverallBudgetByPeriod("MONTHLY")

    suspend fun updateBudgetLimits(dailyPaise: Long, monthlyPaise: Long) {
        val now = System.currentTimeMillis()
        budgetDao.insertBudget(
            BudgetEntity(
                id = "budget_daily_overall",
                categoryId = null,
                periodType = "DAILY",
                limitPaise = dailyPaise,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        budgetDao.insertBudget(
            BudgetEntity(
                id = "budget_monthly_overall",
                categoryId = null,
                periodType = "MONTHLY",
                limitPaise = monthlyPaise,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    // -------------------------------------------------------------
    // Bank Accounts
    // -------------------------------------------------------------

    val bankAccounts: Flow<List<SpendoBank>> = bankAccountDao.getAllBankAccounts().map { entities ->
        entities.map { it.toSpendoBank() }
    }

    suspend fun addBankAccount(
        bankName: String,
        accountType: String,
        initialBalancePaise: Long,
        maskedNumber: String = "•••• ${(1000..9999).random()}"
    ): String {
        val id = "bank_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val count = bankAccountDao.getBankAccountCount()
        val account = BankAccountEntity(
            id = id,
            bankName = bankName,
            accountType = accountType,
            maskedAccountNumber = maskedNumber,
            balancePaise = initialBalancePaise,
            openingBalancePaise = initialBalancePaise,
            isPrimary = count == 0,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        bankAccountDao.insertBankAccount(account)
        return id
    }

    suspend fun updateBankAccountBalance(id: String, newBalancePaise: Long) {
        bankAccountDao.updateBalance(id, newBalancePaise)
    }

    suspend fun deleteBankAccount(id: String) {
        bankAccountDao.deleteBankAccountById(id)
    }

    private suspend fun adjustBankBalanceByName(bankName: String, deltaPaise: Long) {
        val account = bankAccountDao.getBankAccountByNameDirect(bankName) ?: return
        val newBalance = (account.balancePaise + deltaPaise).coerceAtLeast(0L)
        bankAccountDao.updateBalance(account.id, newBalance)
    }

    // -------------------------------------------------------------
    // Income Sources & Salary
    // -------------------------------------------------------------

    val incomeSources: Flow<List<SpendoIncomeItem>> = incomeSourceDao.getAllIncomeSources().map { entities ->
        entities.map { it.toSpendoIncomeItem() }
    }

    val totalRecurringIncomePaise: Flow<Long> = incomeSourceDao.getTotalRecurringIncomePaise()

    suspend fun addIncomeSource(
        title: String,
        amountPaise: Long,
        isRecurring: Boolean,
        frequency: String,
        nextPayDateEpochMs: Long,
        category: String = "Salary"
    ): String {
        val id = "inc_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val entity = IncomeSourceEntity(
            id = id,
            title = title,
            amountPaise = amountPaise,
            isRecurring = isRecurring,
            frequency = frequency,
            nextPayDateEpochMs = nextPayDateEpochMs,
            category = category,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        incomeSourceDao.insertIncomeSource(entity)
        return id
    }

    suspend fun deleteIncomeSource(id: String) {
        incomeSourceDao.deleteIncomeSourceById(id)
    }

    // -------------------------------------------------------------
    // Notification Records
    // -------------------------------------------------------------

    val notificationRecords: Flow<List<SpendoNotificationItem>> = notificationRecordDao.getAllNotificationRecords().map { entities ->
        entities.map { it.toSpendoNotificationItem() }
    }

    suspend fun addNotificationRecord(
        title: String,
        message: String,
        category: String,
        detectedAmountPaise: Long? = null,
        direction: String? = null,
        progress: Float? = null
    ) {
        val id = "notif_${UUID.randomUUID()}"
        val entity = NotificationRecordEntity(
            id = id,
            packageName = "com.spendwise.android",
            appName = "Spendo",
            title = title,
            message = message,
            detectedAmountPaise = detectedAmountPaise,
            direction = direction,
            category = category,
            status = "DETECTED",
            postedAtEpochMs = System.currentTimeMillis(),
            progress = progress,
            isUnread = true
        )
        notificationRecordDao.insertNotificationRecord(entity)
    }

    suspend fun clearAllNotifications() {
        notificationRecordDao.clearAllNotificationRecords()
    }

    // -------------------------------------------------------------
    // App Preferences
    // -------------------------------------------------------------

    fun getPreference(key: String): Flow<String?> {
        return appPreferenceDao.getPreference(key).map { it?.value }
    }

    suspend fun setPreference(key: String, value: String) {
        appPreferenceDao.setPreference(AppPreferenceEntity(key = key, value = value))
    }

    // -------------------------------------------------------------
    // AI Provider Config Metadata
    // -------------------------------------------------------------

    val activeAiConfig: Flow<AiProviderConfigEntity?> = aiProviderConfigDao.getActiveConfig()

    suspend fun saveAiConfigMetadata(
        providerId: String,
        displayName: String,
        selectedModel: String,
        isConfigured: Boolean,
        lastTestMessage: String? = null
    ) {
        aiProviderConfigDao.saveConfig(
            AiProviderConfigEntity(
                providerId = providerId,
                displayName = displayName,
                selectedModel = selectedModel,
                isConfigured = isConfigured,
                lastTestedEpochMs = System.currentTimeMillis(),
                lastTestMessage = lastTestMessage,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    // -------------------------------------------------------------
    // Seed Data Helper
    // -------------------------------------------------------------

    private suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity("cat_food", "Food & Drink", "restaurant", "#B7F34A", "EXPENSE"),
                CategoryEntity("cat_transit", "Transit", "directions_car", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_groceries", "Groceries", "shopping_cart", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_shopping", "Shopping", "shopping_bag", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_entertainment", "Entertainment", "movie", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_subscriptions", "Subscriptions", "repeat", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_salary", "Salary", "payments", "#B7F34A", "INCOME"),
                CategoryEntity("cat_freelance", "Freelance", "laptop", "#B7F34A", "INCOME"),
                CategoryEntity("cat_investments", "Investments", "trending_up", "#B7F34A", "INCOME"),
                CategoryEntity("cat_other", "Other", "category", "#888888", "BOTH")
            )
            categoryDao.insertCategories(defaultCategories)
        }

        if (bankAccountDao.getBankAccountCount() == 0) {
            val now = System.currentTimeMillis()
            val defaultBanks = listOf(
                BankAccountEntity("bank_1", "HDFC Bank", "Salary Account", "•••• 4892", 8452000L, 8452000L, true, now, now),
                BankAccountEntity("bank_2", "ICICI Bank", "Savings & Investments", "•••• 1920", 14200000L, 14200000L, false, now, now),
                BankAccountEntity("bank_3", "State Bank of India", "Emergency Reserve", "•••• 7731", 5000000L, 5000000L, false, now, now)
            )
            bankAccountDao.insertBankAccounts(defaultBanks)
        }

        if (budgetDao.getOverallBudgetByPeriodDirect("DAILY") == null) {
            val now = System.currentTimeMillis()
            budgetDao.insertBudget(BudgetEntity("budget_daily_overall", null, "DAILY", 250000L, now, now))
            budgetDao.insertBudget(BudgetEntity("budget_monthly_overall", null, "MONTHLY", 6500000L, now, now))
        }

        if (incomeSourceDao.getIncomeCount() == 0) {
            val now = System.currentTimeMillis()
            val defaultIncomes = listOf(
                IncomeSourceEntity("inc_1", "Primary Tech Salary", 8500000L, true, "Monthly (1st of month)", now + 604800000L, "bank_1", "Salary", now, now),
                IncomeSourceEntity("inc_2", "Design Advisory Retainer", 2500000L, true, "Bi-weekly", now + 345600000L, "bank_2", "Freelance", now, now),
                IncomeSourceEntity("inc_3", "Index Fund Dividends", 380000L, false, "Quarterly", now + 4406400000L, "bank_2", "Investments", now, now)
            )
            incomeSourceDao.insertIncomeSources(defaultIncomes)
        }

        if (notificationRecordDao.getNotificationCount() == 0) {
            val defaultNotifications = SpendoMockRepository.sampleNotifications.map { item ->
                NotificationRecordEntity(
                    id = item.id,
                    packageName = "com.spendwise.android",
                    appName = "Spendo",
                    title = item.title,
                    message = item.message,
                    category = item.category,
                    postedAtEpochMs = System.currentTimeMillis() - 3600000L,
                    status = "DETECTED",
                    progress = item.progress,
                    isUnread = item.isUnread
                )
            }
            notificationRecordDao.insertNotificationRecords(defaultNotifications)
        }
    }

    private fun getTodayEpochRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun getStartOfMonthEpoch(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

// -------------------------------------------------------------
// Entity to UI Model Converters
// -------------------------------------------------------------

fun TransactionEntity.toSpendoTransaction(): SpendoTransaction {
    val txType = if (direction == "CREDIT") TransactionType.INCOME else TransactionType.EXPENSE
    val txSource = if (sourceType == "NOTIFICATION") TransactionSource.NOTIFICATION else TransactionSource.MANUAL
    return SpendoTransaction(
        id = id,
        merchant = merchant ?: "Unspecified Payee",
        amountPaise = amountPaise,
        type = txType,
        category = categoryId,
        occurredAtEpochMs = occurredAtEpochMillis,
        bankAccountName = bankAccountName ?: "Primary Bank",
        source = txSource,
        note = note
    )
}

fun BankAccountEntity.toSpendoBank(): SpendoBank {
    return SpendoBank(
        id = id,
        bankName = bankName,
        accountType = accountType,
        maskedAccountNumber = maskedAccountNumber,
        balancePaise = balancePaise,
        isPrimary = isPrimary
    )
}

fun IncomeSourceEntity.toSpendoIncomeItem(): SpendoIncomeItem {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val nextDateFormatted = sdf.format(Date(nextPayDateEpochMs))
    return SpendoIncomeItem(
        id = id,
        title = title,
        amountPaise = amountPaise,
        isRecurring = isRecurring,
        frequency = frequency,
        nextPayDate = nextDateFormatted
    )
}

fun NotificationRecordEntity.toSpendoNotificationItem(): SpendoNotificationItem {
    val elapsedMs = System.currentTimeMillis() - postedAtEpochMs
    val timeAgo = when {
        elapsedMs < 3600000L -> "${(elapsedMs / 60000L).coerceAtLeast(1)}m ago"
        elapsedMs < 86400000L -> "${elapsedMs / 3600000L}h ago"
        else -> "${elapsedMs / 86400000L}d ago"
    }

    return SpendoNotificationItem(
        id = id,
        title = title,
        message = message,
        category = category,
        timestampText = timeAgo,
        progress = progress,
        isUnread = isUnread
    )
}
