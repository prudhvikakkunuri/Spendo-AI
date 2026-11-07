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
            coroutineScope.launch {
                com.spendwise.android.notification.SpendoNotificationHelper.checkAndNotifyBudgetThreshold(ctx)
            }
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
        database.withTransaction {
            transactionDao.insertTransaction(entity)
            // Adjust bank account balance (debit deducts)
            adjustBankBalance(bankAccountId, bankAccountName, -amountPaise)
        }
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
        database.withTransaction {
            transactionDao.insertTransaction(entity)
            // Adjust bank account balance (credit adds)
            adjustBankBalance(bankAccountId, bankAccountName, amountPaise)
        }
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
        database.withTransaction {
            val existing = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            val direction = if (type == TransactionType.EXPENSE) "DEBIT" else "CREDIT"

            if (existing.reviewStatus == "CONFIRMED") {
                // 1. Reverse previous balance impact
                val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
                adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)

                // 2. Apply new balance impact
                val applyDelta = if (direction == "DEBIT") -amountPaise else amountPaise
                val targetBankId = bankAccountId ?: existing.bankAccountId
                adjustBankBalance(targetBankId, bankAccountName, applyDelta)
            }

            val targetBankId = bankAccountId ?: existing.bankAccountId
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
        }
        notifyWidgetUpdate()
    }

    suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            val existing = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            transactionDao.deleteTransactionById(id)

            // Only reverse balance impact if the transaction was CONFIRMED.
            // Pending (NEEDS_REVIEW) and REJECTED transactions never altered balances.
            if (existing.reviewStatus == "CONFIRMED") {
                val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
                adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)
            }
        }
        notifyWidgetUpdate()
    }

    // -------------------------------------------------------------
    // Review Transactions Actions & Merchant-Category Memory
    // -------------------------------------------------------------

    suspend fun approveReviewTransaction(id: String) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            if (tx.reviewStatus == "CONFIRMED") return@withTransaction // Strict idempotency guard against double-approvals
            transactionDao.updateReviewStatus(id, "CONFIRMED")
            val delta = if (tx.direction == "DEBIT") -tx.amountPaise else tx.amountPaise
            adjustBankBalance(tx.bankAccountId, tx.bankAccountName, delta)
        }
        notifyWidgetUpdate()
    }

    suspend fun rejectReviewTransaction(id: String) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            // Mark as REJECTED so it is safely archived/removed from queue without altering ledger or balances
            transactionDao.updateReviewStatus(id, "REJECTED")
        }
        notifyWidgetUpdate()
    }

    suspend fun updateAndApproveReviewTransaction(
        id: String,
        merchant: String,
        amountPaise: Long,
        category: String,
        bankAccountName: String,
        bankAccountId: String? = null,
        rememberMerchantPreference: Boolean = false
    ) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            if (tx.reviewStatus == "CONFIRMED") return@withTransaction
            val targetBankId = bankAccountId ?: tx.bankAccountId
            val updated = tx.copy(
                merchant = merchant,
                amountPaise = amountPaise,
                categoryId = category,
                bankAccountName = bankAccountName,
                bankAccountId = targetBankId,
                reviewStatus = "CONFIRMED",
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            transactionDao.updateTransaction(updated)
            val delta = if (tx.direction == "DEBIT") -amountPaise else amountPaise
            adjustBankBalance(targetBankId, bankAccountName, delta)

            if (rememberMerchantPreference && merchant.isNotBlank()) {
                setMerchantCategoryRule(merchant, category)
            }
        }
        notifyWidgetUpdate()
    }

    suspend fun updateReviewTransactionAccount(
        id: String,
        bankAccountId: String,
        bankAccountName: String
    ) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdDirect(id) ?: return@withTransaction
            if (tx.reviewStatus != "NEEDS_REVIEW") return@withTransaction
            val updated = tx.copy(
                bankAccountId = bankAccountId,
                bankAccountName = bankAccountName,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            transactionDao.updateTransaction(updated)
        }
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
        maskedNumber: String = "",
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

        database.withTransaction {
            val sourceAccount = bankAccountDao.getBankAccountByIdDirect(sourceAccountId) ?: return@withTransaction
            val destAccount = bankAccountDao.getBankAccountByIdDirect(destinationAccountId) ?: return@withTransaction

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
        notifyWidgetUpdate()
    }

    suspend fun adjustBankBalanceById(bankAccountId: String, deltaPaise: Long) {
        val account = bankAccountDao.getBankAccountByIdDirect(bankAccountId) ?: return
        val newBalance = (account.balancePaise + deltaPaise).coerceAtLeast(0L)
        bankAccountDao.updateBalance(account.id, newBalance)
    }

    private suspend fun adjustBankBalanceByName(bankName: String, deltaPaise: Long) {
        val matching = bankAccountDao.getBankAccountsByNameDirectList(bankName)
        if (matching.size == 1) {
            val account = matching.first()
            val newBalance = (account.balancePaise + deltaPaise).coerceAtLeast(0L)
            bankAccountDao.updateBalance(account.id, newBalance)
        }
        // If multiple accounts match this bank name, NEVER guess or arbitrarily adjust
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
        database.withTransaction {
            val existing = transactionDao.getTransactionByIdDirect(transaction.id)
            if (existing != null) {
                if (existing.reviewStatus == "CONFIRMED") {
                    val reverseDelta = if (existing.direction == "DEBIT") existing.amountPaise else -existing.amountPaise
                    adjustBankBalance(existing.bankAccountId, existing.bankAccountName, reverseDelta)
                }

                if (transaction.reviewStatus == "CONFIRMED") {
                    val applyDelta = if (transaction.direction == "DEBIT") -transaction.amountPaise else transaction.amountPaise
                    adjustBankBalance(transaction.bankAccountId, transaction.bankAccountName, applyDelta)
                }
            }
            transactionDao.updateTransaction(transaction)
        }
        notifyWidgetUpdate()
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
    // Onboarding & Profile Preferences
    // -------------------------------------------------------------

    suspend fun hasTransactionsDirect(): Boolean = transactionDao.getAllTransactionIds().isNotEmpty()

    suspend fun hasBankAccountsDirect(): Boolean = bankAccountDao.getBankAccountCount() > 0

    suspend fun completeOnboarding(
        userName: String,
        monthlyIncomePaise: Long,
        incomeFrequency: String,
        accounts: List<com.spendwise.android.ui.spendo.model.OnboardingBankDraft>,
        dailyLimitPaise: Long
    ) {
        setPreference("spendo_user_name", userName)
        setPreference("spendo_onboarding_completed", "true")
        if (monthlyIncomePaise > 0L) {
            setPreference("spendo_monthly_income_paise", monthlyIncomePaise.toString())
            setPreference("spendo_income_frequency", incomeFrequency)
        }
        if (dailyLimitPaise > 0L) {
            val currentMonthly = budgetDao.getOverallBudgetByPeriodDirect("MONTHLY")?.limitPaise ?: (dailyLimitPaise * 30L)
            updateBudgetLimits(dailyPaise = dailyLimitPaise, monthlyPaise = currentMonthly)
        }
        for (acc in accounts) {
            val last4 = acc.last4Digits.trim()
            val masked = if (last4.isNotBlank()) "•••• $last4" else ""
            addBankAccount(
                bankName = acc.bankName,
                accountType = acc.accountType,
                initialBalancePaise = acc.initialBalancePaise,
                maskedNumber = masked,
                accountNickname = acc.nickname?.ifBlank { null }
            )
        }
        notifyWidgetUpdate()
    }

    suspend fun updateProfile(
        userName: String,
        monthlyIncomePaise: Long,
        incomeFrequency: String,
        dailyLimitPaise: Long
    ) {
        setPreference("spendo_user_name", userName)
        setPreference("spendo_monthly_income_paise", monthlyIncomePaise.toString())
        setPreference("spendo_income_frequency", incomeFrequency)
        if (dailyLimitPaise > 0L) {
            val currentMonthly = budgetDao.getOverallBudgetByPeriodDirect("MONTHLY")?.limitPaise ?: (dailyLimitPaise * 30L)
            updateBudgetLimits(dailyPaise = dailyLimitPaise, monthlyPaise = currentMonthly)
        }
        notifyWidgetUpdate()
    }


    // -------------------------------------------------------------
    // Seed Data Helper
    // -------------------------------------------------------------

    private suspend fun seedDefaultsIfEmpty() {
        // 1. Seed essential base categories with clean monochrome styling if none exist
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity("cat_food", "Food & Drink", "restaurant", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_transit", "Transit", "directions_car", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_groceries", "Groceries", "shopping_cart", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_shopping", "Shopping", "shopping_bag", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_entertainment", "Entertainment", "movie", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_subscriptions", "Subscriptions", "repeat", "#FFFFFF", "EXPENSE"),
                CategoryEntity("cat_salary", "Salary", "payments", "#FFFFFF", "INCOME"),
                CategoryEntity("cat_freelance", "Freelance", "laptop", "#FFFFFF", "INCOME"),
                CategoryEntity("cat_investments", "Investments", "trending_up", "#FFFFFF", "INCOME"),
                CategoryEntity("cat_other", "Other", "category", "#888888", "BOTH")
            )
            categoryDao.insertCategories(defaultCategories)
        }

        // 2. Safe cleanup of legacy seed dummy transactions (tx_1..tx_10, tx_demo_*, tx_seed_*)
        val legacySeedTxIds = listOf(
            "tx_1", "tx_2", "tx_3", "tx_4", "tx_5", "tx_6", "tx_7", "tx_8", "tx_9", "tx_10",
            "tx_demo_1", "tx_demo_2", "tx_seed_1", "tx_seed_2"
        )
        for (dummyTxId in legacySeedTxIds) {
            transactionDao.deleteTransactionById(dummyTxId)
        }

        // 3. Safe cleanup of legacy seed demo bank accounts (bank_1, bank_2, bank_3)
        // Only delete if zero real transactions are attached, preserving genuine user data
        val seedBankIds = listOf("bank_1", "bank_2", "bank_3")
        for (seedId in seedBankIds) {
            val linkedTxCount = transactionDao.getTransactionCountForAccount(seedId)
            if (linkedTxCount == 0) {
                bankAccountDao.deleteBankAccountById(seedId)
            }
        }

        // 4. Clean up legacy seed dummy incomes
        val seedIncomeIds = listOf("inc_1", "inc_2", "inc_3")
        for (seedInc in seedIncomeIds) {
            incomeSourceDao.deleteIncomeSourceById(seedInc)
        }

        // 5. Clean up legacy seed dummy notification records
        val seedNotifIds = listOf("notif_1", "notif_2", "notif_3", "notif_4", "notif_5")
        for (seedNotif in seedNotifIds) {
            notificationRecordDao.deleteNotificationRecordById(seedNotif)
        }

        // 6. Clean up legacy hardcoded budgets if user has not completed custom onboarding
        val completedPref = getPreferenceDirect("spendo_onboarding_completed")
        if (completedPref != "true") {
            budgetDao.deleteBudgetById("budget_daily_overall")
            budgetDao.deleteBudgetById("budget_monthly_overall")
        }

        notifyWidgetUpdate()
    }

    suspend fun hasUserBankAccountsDirect(): Boolean {
        val allBanks = bankAccountDao.getAllBankAccountsDirect()
        val userBanks = allBanks.filter { it.id != "bank_1" && it.id != "bank_2" && it.id != "bank_3" }
        return userBanks.isNotEmpty()
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
    val meta = com.spendwise.android.domain.banking.TransactionReviewMetadata.parse(note)
    val effectiveAccountId = bankAccountId ?: meta.matchedAccountId
    return SpendoTransaction(
        id = id,
        merchant = merchant ?: "Unspecified Payee",
        amountPaise = amountPaise,
        type = txType,
        category = categoryId,
        occurredAtEpochMs = occurredAtEpochMillis,
        bankAccountName = bankAccountName ?: "Primary Bank",
        source = txSource,
        note = meta.cleanNote ?: note,
        reviewStatus = reviewStatus,
        reviewReason = meta.explanation ?: note,
        bankAccountId = effectiveAccountId,
        detectedBank = meta.detectedBank,
        extractedAccountSuffix = meta.extractedSuffix,
        accountMatchStatus = meta.matchStatus,
        referenceNumber = meta.referenceNumber,
        parserConfidence = confidence ?: meta.parserConfidence,
        accountMatchConfidence = meta.matchConfidence
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
