package com.spendwise.android.data.repository

import com.spendwise.android.data.db.AppDatabase
import androidx.room.withTransaction
import com.spendwise.android.domain.security.SpendoSecureKeyStorage
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
import com.spendwise.android.ui.spendo.model.AffordabilityEvaluation
import com.spendwise.android.ui.spendo.model.AffordabilityVerdict
import com.spendwise.android.ui.spendo.model.SpendoSavingsGoal
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
) : com.spendwise.android.domain.security.SpendoSecurityStorage {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val bankAccountDao = database.bankAccountDao()
    private val incomeSourceDao = database.incomeSourceDao()
    private val notificationRecordDao = database.notificationRecordDao()
    private val appPreferenceDao = database.appPreferenceDao()
    private val aiProviderConfigDao = database.aiProviderConfigDao()
    val secureKeyStorage = SpendoSecureKeyStorage(this)

    init {
        // Seed default foundational data if empty
        coroutineScope.launch {
            seedDefaultsIfEmpty()
        }
    }

    private fun notifyWidgetUpdate() {
        try {
            val ctx = com.spendwise.android.SpendWiseApplication.instance
            com.spendwise.android.widget.SpendoWidgetProvider.updateAllWidgets(ctx)
        } catch (_: Exception) {}
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

    val pendingReviewTransactions: Flow<List<SpendoTransaction>> =
        transactionDao.getTransactionsByStatus("NEEDS_REVIEW").map { entities ->
            entities.map { it.toSpendoTransaction() }
        }

    val pendingReviewCount: Flow<Int> = transactionDao.getPendingReviewCount()

    fun getTransactionsForMerchant(merchantQuery: String): Flow<List<SpendoTransaction>> {
        return transactionDao.getTransactionsForMerchant(merchantQuery).map { entities ->
            entities.map { it.toSpendoTransaction() }
        }
    }

    suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long {
        return transactionDao.getMerchantTotalSpendDirect(merchantQuery)
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
        occurredAtEpochMs: Long = System.currentTimeMillis(),
        bankAccountId: String? = null
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
            bankAccountName = bankAccountName,
            bankAccountId = bankAccountId
        )
        transactionDao.insertTransaction(entity)

        // Adjust bank account balance (debit deducts)
        adjustBankBalance(bankAccountId, bankAccountName, -amountPaise)
        notifyWidgetUpdate()
        return id
    }

    suspend fun addIncome(
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        note: String?,
        occurredAtEpochMs: Long = System.currentTimeMillis(),
        bankAccountId: String? = null
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
            bankAccountName = bankAccountName,
            bankAccountId = bankAccountId
        )
        transactionDao.insertTransaction(entity)

        // Adjust bank account balance (credit adds)
        adjustBankBalance(bankAccountId, bankAccountName, amountPaise)
        notifyWidgetUpdate()
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
        occurredAtEpochMs: Long,
        bankAccountId: String? = null
    ) {
        require(amountPaise > 0) { "Updated amount must be strictly positive integer paise" }
        val existing = transactionDao.getTransactionByIdDirect(id) ?: return
        val direction = if (type == TransactionType.EXPENSE) "DEBIT" else "CREDIT"

        // 1. Reverse previous balance impact
        val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
        adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)

        // 2. Apply new balance impact
        val applyDelta = if (direction == "DEBIT") -amountPaise else amountPaise
        val targetBankId = bankAccountId ?: existing.bankAccountId
        adjustBankBalance(targetBankId, bankAccountName, applyDelta)

        val updated = existing.copy(
            merchant = merchant,
            amountPaise = amountPaise,
            direction = direction,
            categoryId = category,
            bankAccountName = bankAccountName,
            bankAccountId = targetBankId,
            note = note,
            occurredAtEpochMillis = occurredAtEpochMs,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updated)
        notifyWidgetUpdate()
    }

    suspend fun deleteTransaction(id: String) {
        val existing = transactionDao.getTransactionByIdDirect(id) ?: return
        transactionDao.deleteTransactionById(id)

        // Reverse balance impact if bank known
        val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
        adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)
        notifyWidgetUpdate()
    }

    // -------------------------------------------------------------
    // Review Transactions Actions & Merchant-Category Memory
    // -------------------------------------------------------------

    suspend fun approveReviewTransaction(id: String) {
        val tx = transactionDao.getTransactionByIdDirect(id) ?: return
        transactionDao.updateReviewStatus(id, "CONFIRMED")
        tx.bankAccountName?.let { bankName ->
            val delta = if (tx.direction == "DEBIT") -tx.amountPaise else tx.amountPaise
            adjustBankBalanceByName(bankName, delta)
        }
        notifyWidgetUpdate()
    }

    suspend fun rejectReviewTransaction(id: String) {
        transactionDao.deleteTransactionById(id)
        notifyWidgetUpdate()
    }

    suspend fun updateAndApproveReviewTransaction(
        id: String,
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        rememberMerchantPreference: Boolean = false
    ) {
        val tx = transactionDao.getTransactionByIdDirect(id) ?: return
        val updated = tx.copy(
            merchant = merchant,
            amountPaise = amountPaise,
            categoryId = category,
            bankAccountName = bankAccountName,
            reviewStatus = "CONFIRMED",
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updated)
        val delta = if (tx.direction == "DEBIT") -amountPaise else amountPaise
        adjustBankBalanceByName(bankAccountName, delta)

        if (rememberMerchantPreference && merchant.isNotBlank()) {
            setMerchantCategoryRule(merchant, category)
        }
        notifyWidgetUpdate()
    }

    suspend fun setMerchantCategoryRule(merchant: String, category: String) {
        val cleanKey = "merchant_cat:" + merchant.trim().lowercase(Locale.ROOT)
        appPreferenceDao.setPreference(AppPreferenceEntity(cleanKey, category))
    }

    fun getMerchantCategoryRule(merchant: String): Flow<String?> {
        val cleanKey = "merchant_cat:" + merchant.trim().lowercase(Locale.ROOT)
        return appPreferenceDao.getPreference(cleanKey).map { it?.value }
    }

    suspend fun getMerchantCategoryRuleDirect(merchant: String): String? {
        val cleanKey = "merchant_cat:" + merchant.trim().lowercase(Locale.ROOT)
        return appPreferenceDao.getPreferenceValueDirect(cleanKey)
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
        notifyWidgetUpdate()

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
        notifyWidgetUpdate()
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
        notifyWidgetUpdate()
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

    suspend fun getSpendingComparison(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfThisMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, -1)
        val startOfLastMonth = cal.timeInMillis
        val daysInCurrentMonth = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, daysInCurrentMonth)
        val endOfEquivalentLastMonth = cal.timeInMillis

        val thisMonthDebits = transactionDao.getDebitsInDateRangeDirect(startOfThisMonth, now)
        val lastMonthDebits = transactionDao.getDebitsInDateRangeDirect(startOfLastMonth, endOfEquivalentLastMonth)
        return Pair(thisMonthDebits, lastMonthDebits)
    }

    suspend fun evaluateAffordability(
        itemAmountPaise: Long,
        totalBalancePaise: Long,
        monthlyLimitPaise: Long,
        monthlySpentPaise: Long
    ): AffordabilityEvaluation {
        val billsPaise = try {
            database.commitmentDao().sumActiveCommitmentsDirect()
        } catch (_: Exception) {
            0L
        }
        val plannedSavingsPaise = 2500000L // ₹25,000.00 planned savings reserve

        val freeDiscretionaryPaise = (totalBalancePaise - billsPaise - plannedSavingsPaise).coerceAtLeast(0L)
        val monthlyRemainingPaise = (monthlyLimitPaise - monthlySpentPaise).coerceAtLeast(0L)

        val verdict = when {
            itemAmountPaise <= freeDiscretionaryPaise && itemAmountPaise <= monthlyRemainingPaise -> {
                AffordabilityVerdict.AFFORDABLE
            }
            itemAmountPaise <= (totalBalancePaise - billsPaise) -> {
                AffordabilityVerdict.CAUTION
            }
            else -> {
                AffordabilityVerdict.RISKY
            }
        }

        val reasonText = when (verdict) {
            AffordabilityVerdict.AFFORDABLE ->
                "Comfortably affordable. Fits within your free cash (₹%,.2f) and remaining monthly ceiling (₹%,.2f)."
                    .format(Locale.ENGLISH, freeDiscretionaryPaise / 100.0, monthlyRemainingPaise / 100.0)
            AffordabilityVerdict.CAUTION ->
                "Proceed with caution. You have liquid funds, but this purchase exceeds your free cash or dips into planned savings."
            AffordabilityVerdict.RISKY ->
                "Risky purchase. Endangers upcoming essential commitments (₹%,.2f) or exceeds available liquid balances."
                    .format(Locale.ENGLISH, billsPaise / 100.0)
        }

        return AffordabilityEvaluation(
            itemAmountPaise = itemAmountPaise,
            liquidBalancePaise = totalBalancePaise,
            upcomingBillsPaise = billsPaise,
            plannedSavingsPaise = plannedSavingsPaise,
            freeDiscretionaryPaise = freeDiscretionaryPaise,
            monthlyRemainingPaise = monthlyRemainingPaise,
            verdict = verdict,
            reasonText = reasonText
        )
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
        notifyWidgetUpdate()
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
        maskedNumber: String = "•••• ${(1000..9999).random()}",
        accountNickname: String? = null,
        notes: String? = null
    ): String {
        val id = "bank_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val count = bankAccountDao.getBankAccountCount()
        val account = BankAccountEntity(
            id = id,
            bankName = bankName,
            accountNickname = accountNickname?.trim()?.ifBlank { null },
            accountType = accountType,
            maskedAccountNumber = maskedNumber,
            balancePaise = initialBalancePaise,
            openingBalancePaise = initialBalancePaise,
            isPrimary = count == 0,
            notes = notes?.trim()?.ifBlank { null },
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        bankAccountDao.insertBankAccount(account)
        notifyWidgetUpdate()
        return id
    }

    suspend fun updateBankAccountDetails(
        id: String,
        bankName: String,
        accountNickname: String?,
        accountType: String,
        maskedNumber: String,
        balancePaise: Long,
        notes: String?
    ) {
        val existing = bankAccountDao.getBankAccountByIdDirect(id) ?: return
        val updated = existing.copy(
            bankName = bankName,
            accountNickname = accountNickname?.trim()?.ifBlank { null },
            accountType = accountType,
            maskedAccountNumber = maskedNumber,
            balancePaise = balancePaise,
            notes = notes?.trim()?.ifBlank { null },
            updatedAtEpochMs = System.currentTimeMillis()
        )
        bankAccountDao.updateBankAccount(updated)
        notifyWidgetUpdate()
    }

    suspend fun updateBankAccountBalance(id: String, newBalancePaise: Long) {
        bankAccountDao.updateBalance(id, newBalancePaise)
        notifyWidgetUpdate()
    }

    suspend fun deleteBankAccount(id: String) {
        bankAccountDao.deleteBankAccountById(id)
        notifyWidgetUpdate()
    }

    suspend fun transferBetweenAccounts(
        sourceAccountId: String,
        destinationAccountId: String,
        amountPaise: Long,
        note: String? = null
    ) {
        require(amountPaise > 0) { "Transfer amount must be strictly positive" }
        require(sourceAccountId != destinationAccountId) { "Source and destination accounts must be different" }

        val sourceAccount = bankAccountDao.getBankAccountByIdDirect(sourceAccountId) ?: return
        val destAccount = bankAccountDao.getBankAccountByIdDirect(destinationAccountId) ?: return

        // 1. Deduct from source
        adjustBankBalanceById(sourceAccountId, -amountPaise)
        // 2. Credit to destination
        adjustBankBalanceById(destinationAccountId, amountPaise)

        // 3. Record internal transfer transaction
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = "tx_${UUID.randomUUID()}",
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "TRANSFER",
            merchant = "Transfer: ${sourceAccount.bankName} ➔ ${destAccount.bankName}",
            note = note ?: "Internal account transfer",
            categoryId = "TRANSFER",
            occurredAtEpochMillis = now,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountId = sourceAccountId,
            bankAccountName = "${sourceAccount.bankName} ➔ ${destAccount.bankName}"
        )
        transactionDao.insertTransaction(tx)
    }

    suspend fun adjustBankBalanceById(bankAccountId: String, deltaPaise: Long) {
        val account = bankAccountDao.getBankAccountByIdDirect(bankAccountId) ?: return
        val newBalance = (account.balancePaise + deltaPaise).coerceAtLeast(0L)
        bankAccountDao.updateBalance(account.id, newBalance)
    }

    private suspend fun adjustBankBalanceByName(bankName: String, deltaPaise: Long) {
        val account = bankAccountDao.getBankAccountByNameDirect(bankName) ?: return
        val newBalance = (account.balancePaise + deltaPaise).coerceAtLeast(0L)
        bankAccountDao.updateBalance(account.id, newBalance)
    }

    private suspend fun adjustBankBalance(bankAccountId: String?, bankAccountName: String?, deltaPaise: Long) {
        if (!bankAccountId.isNullOrBlank()) {
            adjustBankBalanceById(bankAccountId, deltaPaise)
        } else if (!bankAccountName.isNullOrBlank()) {
            adjustBankBalanceByName(bankAccountName, deltaPaise)
        }
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

    override suspend fun setPreference(key: String, value: String) {
        appPreferenceDao.setPreference(AppPreferenceEntity(key = key, value = value))
    }

    override suspend fun getPreferenceDirect(key: String): String? {
        return appPreferenceDao.getPreferenceValueDirect(key)
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

    suspend fun updateTransaction(transaction: TransactionEntity) {
        val existing = transactionDao.getTransactionByIdDirect(transaction.id)
        if (existing != null) {
            val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
            adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)

            val applyDelta = if (transaction.direction == "DEBIT") -transaction.amountPaise else transaction.amountPaise
            adjustBankBalance(transaction.bankAccountId, transaction.bankAccountName, applyDelta)
        }
        transactionDao.updateTransaction(transaction)
    }

    suspend fun importTransactionsFromCsv(inputStream: java.io.InputStream): com.spendwise.android.util.CsvImportResult {
        val (parsedEntities, rejectedCount) = com.spendwise.android.util.CsvImporter.parseCsvDetailed(inputStream)
        if (parsedEntities.isEmpty() && rejectedCount == 0) {
            return com.spendwise.android.util.CsvImportResult(0, 0, 0, 0, "Selected file is empty")
        }

        val existingIds = transactionDao.getAllTransactionIds().toSet()
        val toInsert = mutableListOf<TransactionEntity>()
        var skippedCount = 0

        for (entity in parsedEntities) {
            if (existingIds.contains(entity.id)) {
                skippedCount++
            } else {
                toInsert.add(entity)
            }
        }

        if (toInsert.isNotEmpty()) {
            database.withTransaction {
                transactionDao.insertTransactions(toInsert)
            }
        }

        return com.spendwise.android.util.CsvImportResult(
            importedCount = toInsert.size,
            skippedCount = skippedCount,
            rejectedCount = rejectedCount,
            totalProcessed = parsedEntities.size + rejectedCount
        )
    }

    suspend fun exportTransactionsToCsv(outputStream: java.io.OutputStream): Int {
        val confirmedEntities = transactionDao.getConfirmedTransactionsDirect()
        com.spendwise.android.util.CsvExporter.writeFullEntitiesToStream(confirmedEntities, outputStream)
        return confirmedEntities.size
    }

    suspend fun saveSecureAiConfig(providerId: String, model: String, apiKey: String, lastTestMessage: String? = null) {
        secureKeyStorage.saveApiKey(providerId, apiKey)
        secureKeyStorage.saveActiveProvider(providerId, model)
        saveAiConfigMetadata(
            providerId = providerId,
            displayName = providerId,
            selectedModel = model,
            isConfigured = apiKey.isNotBlank(),
            lastTestMessage = lastTestMessage
        )
    }

    suspend fun loadSecureAiApiKey(providerId: String): String? {
        return secureKeyStorage.getApiKey(providerId)
    }

    suspend fun deleteSecureAiConfig(providerId: String) {
        secureKeyStorage.deleteApiKey(providerId)
        saveAiConfigMetadata(
            providerId = providerId,
            displayName = providerId,
            selectedModel = "",
            isConfigured = false,
            lastTestMessage = null
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
                BankAccountEntity("bank_1", "Indian Bank", "Salary Account", "•••• 4892", 5240000L, 5240000L, true, now, now),
                BankAccountEntity("bank_2", "Kotak Mahindra Bank", "Smart Savings", "•••• 1920", 2850050L, 2850050L, false, now, now),
                BankAccountEntity("bank_3", "Karur Vysya Bank", "Branch Account (KVB)", "•••• 7731", 1520000L, 1520000L, false, now, now)
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
        notifyWidgetUpdate()
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
        note = note,
        reviewStatus = reviewStatus,
        reviewReason = note,
        bankAccountId = bankAccountId
    )
}

fun BankAccountEntity.toSpendoBank(): SpendoBank {
    return SpendoBank(
        id = id,
        bankName = bankName,
        accountType = accountType,
        maskedAccountNumber = maskedAccountNumber,
        balancePaise = balancePaise,
        isPrimary = isPrimary,
        accountNickname = accountNickname,
        notes = notes
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
