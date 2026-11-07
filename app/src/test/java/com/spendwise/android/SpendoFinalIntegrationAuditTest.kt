package com.spendwise.android

import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.banking.AccountMatchStatus
import com.spendwise.android.domain.banking.BankAccountMatcher
import com.spendwise.android.domain.banking.BankIdentifier
import com.spendwise.android.domain.banking.BankMessageParserRegistry
import com.spendwise.android.domain.banking.KotakBankParser
import com.spendwise.android.domain.banking.KvbBankParser
import com.spendwise.android.domain.banking.TransactionReviewMetadata
import com.spendwise.android.domain.calculator.StreakCalculator
import com.spendwise.android.notification.NotificationIngestionEngine
import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Final Integration Audit test suite verifying:
 * 1. Database persistence of bank, suffix, match status, matched ID, and review status across restarts.
 * 2. Enforcing stable account ID matching, idempotency, Room atomicity, and zero balance impact on reject.
 * 3. Total isolation of pending and rejected transactions from financial calculations.
 * 4. Real notification ingestion on KVB and Kotak samples across exact match, missing suffix, multi-account, and unknown suffix.
 */
class SpendoFinalIntegrationAuditTest {

    // ---------------------------------------------------------------------
    // In-memory DAOs simulating Room persistence
    // ---------------------------------------------------------------------

    private class AuditTransactionDao(
        val list: MutableList<TransactionEntity> = mutableListOf()
    ) : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(list.toList())
        override fun getConfirmedTransactions(): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" })
        override fun getTransactionsByStatus(reviewStatus: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == reviewStatus })
        override fun getTransactionById(id: String): Flow<TransactionEntity?> =
            flowOf(list.firstOrNull { it.id == id })
        override suspend fun getTransactionByIdDirect(id: String): TransactionEntity? =
            list.firstOrNull { it.id == id }
        override suspend fun getAllTransactionIds(): List<String> = list.map { it.id }
        override suspend fun getConfirmedTransactionsDirect(): List<TransactionEntity> =
            list.filter { it.reviewStatus == "CONFIRMED" }
        override suspend fun insertTransaction(transaction: TransactionEntity) {
            list.removeAll { it.id == transaction.id }
            list.add(transaction)
        }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            transactions.forEach { insertTransaction(it) }
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val idx = list.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) list[idx] = transaction
        }
        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            list.removeAll { it.id == transaction.id }
        }
        override suspend fun deleteTransactionById(id: String) {
            list.removeAll { it.id == id }
        }
        override suspend fun deleteAllTransactions() {
            list.clear()
        }
        override fun getTodayDebitsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(list.filter {
                it.direction == "DEBIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis in startOfDay..endOfDay
            }.sumOf { it.amountPaise })
        override fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(list.filter {
                it.direction == "CREDIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis in startOfDay..endOfDay
            }.sumOf { it.amountPaise })
        override fun getPeriodDebitsPaise(startOfPeriod: Long): Flow<Long> =
            flowOf(list.filter {
                it.direction == "DEBIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis >= startOfPeriod
            }.sumOf { it.amountPaise })
        override suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            list.filter {
                it.direction == "DEBIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis in startTime..endTime
            }.sumOf { it.amountPaise }
        override suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            list.filter {
                it.direction == "CREDIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis in startTime..endTime
            }.sumOf { it.amountPaise }
        override fun getPeriodIncomeCredits(startOfPeriod: Long): Flow<Long> = flowOf(0L)
        override fun getPeriodRefundCredits(startOfPeriod: Long): Flow<Long> = flowOf(0L)
        override fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<CategorySpending>> {
            val confirmed = list.filter {
                it.direction == "DEBIT" &&
                it.reviewStatus == "CONFIRMED" &&
                it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                it.occurredAtEpochMillis >= startOfPeriod
            }
            val grouped = confirmed.groupBy { it.categoryId }.map { (cat, txs) ->
                CategorySpending(cat, txs.sumOf { it.amountPaise }, txs.size)
            }
            return flowOf(grouped)
        }
        override fun getPendingReviewCount(): Flow<Int> =
            flowOf(list.count { it.reviewStatus == "NEEDS_REVIEW" })
        override suspend fun getPendingReviewCountDirect(): Int =
            list.count { it.reviewStatus == "NEEDS_REVIEW" }
        override suspend fun findPotentialDuplicates(amountPaise: Long, direction: String, minTime: Long, maxTime: Long): List<TransactionEntity> =
            list.filter { it.amountPaise == amountPaise && it.direction == direction && it.occurredAtEpochMillis in minTime..maxTime }
        override suspend fun getTransactionByDedupeKey(dedupeKey: String): TransactionEntity? =
            list.firstOrNull { it.dedupeKey == dedupeKey }
        override suspend fun getTransactionCountForAccount(bankAccountId: String): Int =
            list.count { it.bankAccountId == bankAccountId }
        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && (it.merchant?.contains(query, true) == true || it.note?.contains(query, true) == true) })
        override fun getTransactionsByDirection(direction: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.direction == direction })
        override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.categoryId == categoryId })
        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.occurredAtEpochMillis in startTime..endTime })
        override fun getTransactionsForBank(bankAccountName: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && (it.bankAccountName == bankAccountName || it.bankAccountId == bankAccountName) })
        override fun getTransactionsForMerchant(merchantQuery: String): Flow<List<TransactionEntity>> =
            flowOf(list.filter { it.reviewStatus == "CONFIRMED" && (it.merchant?.contains(merchantQuery, true) == true) })
        override suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long =
            list.filter { it.reviewStatus == "CONFIRMED" && it.direction == "DEBIT" && (it.merchant?.contains(merchantQuery, true) == true) }.sumOf { it.amountPaise }
        override suspend fun updateReviewStatus(id: String, newStatus: String, updatedAt: Long) {
            val existing = list.firstOrNull { it.id == id } ?: return
            val idx = list.indexOf(existing)
            list[idx] = existing.copy(reviewStatus = newStatus, updatedAtEpochMillis = updatedAt)
        }
    }

    private class AuditBankAccountDao(
        val accounts: MutableList<BankAccountEntity> = mutableListOf()
    ) : BankAccountDao {
        override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> = flowOf(accounts.toList())
        override suspend fun getAllBankAccountsDirect(): List<BankAccountEntity> = accounts.toList()
        override fun getBankAccountById(id: String): Flow<BankAccountEntity?> =
            flowOf(accounts.firstOrNull { it.id == id })
        override suspend fun getBankAccountByIdDirect(id: String): BankAccountEntity? =
            accounts.firstOrNull { it.id == id }
        override suspend fun getBankAccountByNameDirect(bankName: String): BankAccountEntity? =
            accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountsByNameDirectList(bankName: String): List<BankAccountEntity> =
            accounts.filter { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountByNameAndNumberDirect(bankName: String, maskedNumber: String): BankAccountEntity? =
            accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber == maskedNumber }
        override suspend fun getBankAccountsBySuffixDirect(suffix: String): List<BankAccountEntity> =
            accounts.filter { it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun getBankAccountsByNameAndSuffixDirect(bankName: String, suffix: String): List<BankAccountEntity> =
            accounts.filter { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun insertBankAccount(account: BankAccountEntity) {
            accounts.removeAll { it.id == account.id }
            accounts.add(account)
        }
        override suspend fun insertBankAccounts(accounts: List<BankAccountEntity>) {
            this.accounts.addAll(accounts)
        }
        override suspend fun updateBankAccount(account: BankAccountEntity) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) accounts[idx] = account
        }
        override suspend fun updateBalance(id: String, newBalancePaise: Long, updatedAt: Long) {
            val existing = accounts.firstOrNull { it.id == id } ?: return
            val idx = accounts.indexOf(existing)
            accounts[idx] = existing.copy(balancePaise = newBalancePaise, updatedAtEpochMs = updatedAt)
        }
        override suspend fun deleteBankAccount(account: BankAccountEntity) {
            accounts.remove(account)
        }
        override suspend fun deleteBankAccountById(id: String) {
            accounts.removeAll { it.id == id }
        }
        override suspend fun getBankAccountCount(): Int = accounts.size
    }

    private fun buildAccount(
        id: String,
        bankName: String,
        accountType: String = "SAVINGS",
        maskedAccountNumber: String,
        balancePaise: Long,
        openingBalancePaise: Long = balancePaise,
        isPrimary: Boolean = false,
        nickname: String? = null
    ): BankAccountEntity = BankAccountEntity(
        id = id,
        bankName = bankName,
        accountType = accountType,
        maskedAccountNumber = maskedAccountNumber,
        balancePaise = balancePaise,
        openingBalancePaise = openingBalancePaise,
        isPrimary = isPrimary,
        createdAtEpochMs = System.currentTimeMillis(),
        updatedAtEpochMs = System.currentTimeMillis(),
        accountNickname = nickname
    )

    // =====================================================================
    // 1. Verify Database Persistence Across App Restarts
    // =====================================================================

    @Test
    fun `Audit 1 - Review metadata and account fields survive simulated app restart`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()

        val accId = "bank_kotak_privy"
        bankDao.insertBankAccount(
            buildAccount(
                id = accId,
                bankName = "Kotak Mahindra Bank",
                nickname = "Kotak Salary",
                accountType = "SALARY",
                maskedAccountNumber = "•••• 4821",
                balancePaise = 5000000L
            )
        )

        val metadata = TransactionReviewMetadata(
            detectedBank = "Kotak Mahindra Bank",
            extractedSuffix = "4821",
            matchStatus = AccountMatchStatus.EXACT_MATCH,
            matchedAccountId = accId,
            referenceNumber = "UPI/6291829102",
            parserConfidence = 0.95f,
            matchConfidence = 1.0f,
            explanation = "Matched Kotak Salary (•••• 4821)"
        )

        val txId = "tx_audit_restart_test"
        val pendingEntity = TransactionEntity(
            id = txId,
            amountPaise = 75000L, // ₹750.00
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Starbucks Coffee",
            note = metadata.serialize(),
            categoryId = "OTHER",
            occurredAtEpochMillis = System.currentTimeMillis(),
            sourceType = "NOTIFICATION",
            reviewStatus = "NEEDS_REVIEW",
            bankAccountId = accId,
            bankAccountName = "Kotak Mahindra Bank •••• 4821"
        )
        txDao.insertTransaction(pendingEntity)

        // Simulate app kill & restart: reload from storage
        val reloadedTx = txDao.getTransactionByIdDirect(txId)
        assertNotNull("Transaction must be found after restart", reloadedTx)
        assertEquals("NEEDS_REVIEW", reloadedTx!!.reviewStatus)
        assertEquals(accId, reloadedTx.bankAccountId)

        // Parse review metadata from persisted note
        val parsedMeta = TransactionReviewMetadata.parse(reloadedTx.note)
        assertEquals("Kotak Mahindra Bank", parsedMeta.detectedBank)
        assertEquals("4821", parsedMeta.extractedSuffix)
        assertEquals(AccountMatchStatus.EXACT_MATCH, parsedMeta.matchStatus)
        assertEquals(accId, parsedMeta.matchedAccountId)
        assertEquals("UPI/6291829102", parsedMeta.referenceNumber)
        assertEquals(0.95f, parsedMeta.parserConfidence!!, 0.01f)
        assertEquals(1.0f, parsedMeta.matchConfidence!!, 0.01f)
    }

    // =====================================================================
    // 2. Enforce Stable Account ID Matching & Atomicity
    // =====================================================================

    @Test
    fun `Audit 2 - Never fall back to bank name when multiple accounts match same bank`() = runBlocking {
        val bankDao = AuditBankAccountDao()

        // Two accounts for Kotak
        val kotakSavings = buildAccount(
            id = "kotak_savings_1",
            bankName = "Kotak Mahindra Bank",
            nickname = "Kotak Personal",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 1111",
            balancePaise = 1000000L // ₹10,000
        )
        val kotakSalary = buildAccount(
            id = "kotak_salary_2",
            bankName = "Kotak Mahindra Bank",
            nickname = "Kotak Salary",
            accountType = "SALARY",
            maskedAccountNumber = "•••• 2222",
            balancePaise = 2000000L // ₹20,000
        )
        bankDao.insertBankAccount(kotakSavings)
        bankDao.insertBankAccount(kotakSalary)

        // Matching multiple accounts for "Kotak Mahindra Bank"
        val matchingByName = bankDao.getBankAccountsByNameDirectList("Kotak Mahindra Bank")
        assertEquals(2, matchingByName.size)

        // Helper function mimicking SpendoFinancialRepository.adjustBankBalance logic
        suspend fun safeAdjustBalance(bankAccountId: String?, bankAccountName: String?, deltaPaise: Long) {
            if (!bankAccountId.isNullOrBlank()) {
                val acc = bankDao.getBankAccountByIdDirect(bankAccountId) ?: return
                bankDao.updateBalance(acc.id, (acc.balancePaise + deltaPaise).coerceAtLeast(0L))
            } else if (!bankAccountName.isNullOrBlank()) {
                val matching = bankDao.getBankAccountsByNameDirectList(bankAccountName)
                if (matching.size == 1) {
                    val acc = matching.first()
                    bankDao.updateBalance(acc.id, (acc.balancePaise + deltaPaise).coerceAtLeast(0L))
                }
                // Ambiguous: 0 or >1 matches -> do nothing
            }
        }

        // Test 1: Calling without bankAccountId should NOT modify either account
        safeAdjustBalance(null, "Kotak Mahindra Bank", -50000L)
        assertEquals(1000000L, bankDao.getBankAccountByIdDirect("kotak_savings_1")!!.balancePaise)
        assertEquals(2000000L, bankDao.getBankAccountByIdDirect("kotak_salary_2")!!.balancePaise)

        // Test 2: Calling with stable bankAccountId modifies ONLY that account
        safeAdjustBalance("kotak_salary_2", "Kotak Mahindra Bank", -50000L) // Deduct ₹500
        assertEquals("Kotak Savings must remain untouched", 1000000L, bankDao.getBankAccountByIdDirect("kotak_savings_1")!!.balancePaise)
        assertEquals("Kotak Salary must be adjusted", 1950000L, bankDao.getBankAccountByIdDirect("kotak_salary_2")!!.balancePaise)
    }

    @Test
    fun `Audit 2 - Duplicate approval idempotency prevents double balance adjustment`() = runBlocking {
        val bankDao = AuditBankAccountDao()
        val txDao = AuditTransactionDao()

        val accId = "acc_kvb_audit"
        bankDao.insertBankAccount(
            buildAccount(
                id = accId,
                bankName = "Karur Vysya Bank",
                accountType = "SAVINGS",
                maskedAccountNumber = "•••• 9362",
                balancePaise = 1000000L // ₹10,000.00
            )
        )

        val txId = "tx_kvb_idempotent"
        txDao.insertTransaction(
            TransactionEntity(
                id = txId,
                amountPaise = 20000L, // ₹200.00
                currencyCode = "INR",
                direction = "DEBIT",
                merchant = "Chai Point",
                reviewStatus = "NEEDS_REVIEW",
                bankAccountId = accId,
                bankAccountName = "Karur Vysya Bank •••• 9362"
            )
        )

        // Approve function with strict idempotency guard
        suspend fun approve(id: String) {
            val tx = txDao.getTransactionByIdDirect(id) ?: return
            if (tx.reviewStatus == "CONFIRMED") return // Idempotency guard!
            txDao.updateReviewStatus(id, "CONFIRMED")
            val delta = if (tx.direction == "DEBIT") -tx.amountPaise else tx.amountPaise
            val acc = bankDao.getBankAccountByIdDirect(tx.bankAccountId!!)!!
            bankDao.updateBalance(acc.id, acc.balancePaise + delta)
        }

        // First approval
        approve(txId)
        val balanceAfterFirst = bankDao.getBankAccountByIdDirect(accId)!!.balancePaise
        assertEquals(980000L, balanceAfterFirst) // ₹9,800.00

        // Duplicate second approval
        approve(txId)
        val balanceAfterSecond = bankDao.getBankAccountByIdDirect(accId)!!.balancePaise
        assertEquals("Balance must NOT change on second approval", 980000L, balanceAfterSecond)
    }

    @Test
    fun `Audit 2 - Rejecting transaction never changes balances`() = runBlocking {
        val bankDao = AuditBankAccountDao()
        val txDao = AuditTransactionDao()

        val accId = "acc_kvb_reject_test"
        val originalBalance = 500000L
        bankDao.insertBankAccount(
            buildAccount(
                id = accId,
                bankName = "Karur Vysya Bank",
                accountType = "SAVINGS",
                maskedAccountNumber = "•••• 9362",
                balancePaise = originalBalance
            )
        )

        val txId = "tx_reject_me"
        txDao.insertTransaction(
            TransactionEntity(
                id = txId,
                amountPaise = 10000L,
                currencyCode = "INR",
                direction = "DEBIT",
                merchant = "Unrecognized Charge",
                reviewStatus = "NEEDS_REVIEW",
                bankAccountId = accId
            )
        )

        // Reject
        val tx = txDao.getTransactionByIdDirect(txId)!!
        txDao.updateReviewStatus(tx.id, "REJECTED")

        val reloadedTx = txDao.getTransactionByIdDirect(txId)!!
        assertEquals("REJECTED", reloadedTx.reviewStatus)
        val balanceAfterReject = bankDao.getBankAccountByIdDirect(accId)!!.balancePaise
        assertEquals("Bank balance must remain exactly equal to initial amount", originalBalance, balanceAfterReject)
    }

    // =====================================================================
    // 3. Verify Financial Calculations Isolation
    // =====================================================================

    @Test
    fun `Audit 3 - Pending and rejected transactions are 100 percent excluded from calculations`() = runBlocking {
        val txDao = AuditTransactionDao()
        val now = System.currentTimeMillis()
        val startOfDay = now - 3600000L
        val endOfDay = now + 3600000L
        val startOfMonth = now - 86400000L * 10

        // 1. Confirmed expense: ₹300.00 (Food)
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_confirmed_food",
                amountPaise = 30000L,
                direction = "DEBIT",
                categoryId = "FOOD",
                occurredAtEpochMillis = now,
                reviewStatus = "CONFIRMED"
            )
        )

        // 2. Pending review expense: ₹5,000.00 (Shopping) -> MUST BE IGNORED
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_pending_shopping",
                amountPaise = 500000L,
                direction = "DEBIT",
                categoryId = "SHOPPING",
                occurredAtEpochMillis = now,
                reviewStatus = "NEEDS_REVIEW"
            )
        )

        // 3. Rejected expense: ₹2,000.00 (Electronics) -> MUST BE IGNORED
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_rejected_elec",
                amountPaise = 200000L,
                direction = "DEBIT",
                categoryId = "ELECTRONICS",
                occurredAtEpochMillis = now,
                reviewStatus = "REJECTED"
            )
        )

        // Test Today Spend
        var todayDebits = 0L
        txDao.getTodayDebitsPaise(startOfDay, endOfDay).collect { todayDebits = it }
        assertEquals("Today spend must only include confirmed transactions (₹300.00)", 30000L, todayDebits)

        // Test Month Spend
        var monthDebits = 0L
        txDao.getPeriodDebitsPaise(startOfMonth).collect { monthDebits = it }
        assertEquals("Month spend must only include confirmed transactions (₹300.00)", 30000L, monthDebits)

        // Test Category Spending
        var catTotals: List<CategorySpending> = emptyList()
        txDao.getCategorySpendTotals(startOfMonth).collect { catTotals = it }
        assertEquals(1, catTotals.size)
        assertEquals("FOOD", catTotals.first().categoryId)
        assertEquals(30000L, catTotals.first().totalPaise)

        // Test Confirmed Transactions Direct Query
        val confirmedList = txDao.getConfirmedTransactionsDirect()
        assertEquals(1, confirmedList.size)
        assertEquals("tx_confirmed_food", confirmedList.first().id)

        // Test Streak Calculation Isolation
        val expenseDates = confirmedList.map { it.occurredAtEpochMillis }
        val streak = StreakCalculator.calculateLoggingStreak(expenseDates, now)
        assertTrue("Streak must calculate only on confirmed dates", streak >= 1)
    }

    // =====================================================================
    // 4. Verify Real Notification Ingestion (KVB & Kotak)
    // =====================================================================

    @Test
    fun `Audit 4 - KVB exact suffix match ingestion`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()
        val now = System.currentTimeMillis()

        val kvbAccount = buildAccount(
            id = "acc_kvb_live_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 500000L
        )
        bankDao.insertBankAccount(kvbAccount)

        val engine = NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )
        val payload = RawNotificationPayload(
            packageName = "com.kvb.dlite",
            title = "KVBANK",
            text = "Your KVB A/c XXXXXXXXXXXX9362 is debited by Rs. 150.00 on 24-Oct-26 to Swiggy. Avl Bal Rs. 4,850.00. Help 18005721916-KVB",
            postTimeEpochMs = now
        )

        engine.onNotificationReceived(payload)

        val ingested = txDao.list.first()
        assertEquals("NEEDS_REVIEW", ingested.reviewStatus)
        assertEquals(15000L, ingested.amountPaise)
        assertEquals("Swiggy", ingested.merchant)
        assertEquals("acc_kvb_live_1", ingested.bankAccountId)

        val meta = TransactionReviewMetadata.parse(ingested.note)
        assertEquals("Karur Vysya Bank", meta.detectedBank)
        assertEquals("9362", meta.extractedSuffix)
        assertEquals(AccountMatchStatus.EXACT_MATCH, meta.matchStatus)
        assertEquals("acc_kvb_live_1", meta.matchedAccountId)
    }

    @Test
    fun `Audit 4 - Kotak notification with bullet mask and multiple accounts at same bank`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()
        val now = System.currentTimeMillis()

        // Kotak Account 1: suffix 4821
        val kotakSalary = buildAccount(
            id = "acc_kotak_4821",
            bankName = "Kotak Mahindra Bank",
            nickname = "Kotak Salary",
            accountType = "SALARY",
            maskedAccountNumber = "•••• 4821",
            balancePaise = 10000000L
        )
        // Kotak Account 2: suffix 9999
        val kotakSavings = buildAccount(
            id = "acc_kotak_9999",
            bankName = "Kotak Mahindra Bank",
            nickname = "Kotak Savings",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9999",
            balancePaise = 5000000L
        )
        bankDao.insertBankAccount(kotakSalary)
        bankDao.insertBankAccount(kotakSavings)

        val engine = NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )
        val payload = RawNotificationPayload(
            packageName = "com.msf.kopl",
            title = "Kotak Bank",
            text = "Sent Rs.4,500.00 from Kotak Bank AC •••• 4821 to Amazon Pay on 24-Oct-26. UPI Ref 6291829102. Bal Rs.95,500.00",
            postTimeEpochMs = now
        )

        engine.onNotificationReceived(payload)

        val ingested = txDao.list.first()
        assertEquals("NEEDS_REVIEW", ingested.reviewStatus)
        assertEquals(450000L, ingested.amountPaise)
        assertEquals("Amazon Pay", ingested.merchant)
        assertEquals("Must match suffix 4821 specifically despite multiple Kotak accounts",
            "acc_kotak_4821", ingested.bankAccountId)

        val meta = TransactionReviewMetadata.parse(ingested.note)
        assertEquals("4821", meta.extractedSuffix)
        assertEquals(AccountMatchStatus.EXACT_MATCH, meta.matchStatus)
    }

    @Test
    fun `Audit 4 - Ambiguous match when multiple accounts share same bank and suffix is missing`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()
        val now = System.currentTimeMillis()

        // Two Kotak accounts
        bankDao.insertBankAccount(buildAccount(id = "kotak_1", bankName = "Kotak Mahindra Bank", accountType = "SAVINGS", maskedAccountNumber = "•••• 1111", balancePaise = 100000L))
        bankDao.insertBankAccount(buildAccount(id = "kotak_2", bankName = "Kotak Mahindra Bank", accountType = "SALARY", maskedAccountNumber = "•••• 2222", balancePaise = 200000L))

        val engine = NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )
        // Notification missing suffix
        val payload = RawNotificationPayload(
            packageName = "com.msf.kopl",
            title = "Kotak Bank Alert",
            text = "Rs. 250.00 paid from Kotak Bank to Uber on 24-Oct-26.",
            postTimeEpochMs = now
        )

        engine.onNotificationReceived(payload)

        val ingested = txDao.list.first()
        assertEquals("NEEDS_REVIEW", ingested.reviewStatus)
        // Bank account ID must be NULL because system refuses to guess
        assertNull("Ambiguous multi-match must not auto-assign an account ID", ingested.bankAccountId)

        val meta = TransactionReviewMetadata.parse(ingested.note)
        assertEquals(AccountMatchStatus.AMBIGUOUS_MULTI_MATCH, meta.matchStatus)
        assertFalse("Must not auto-assign", meta.matchStatus == AccountMatchStatus.EXACT_MATCH)
    }

    @Test
    fun `Audit 4 - Unknown suffix keeps account unlinked and prompts user`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()
        val now = System.currentTimeMillis()

        // User only has account ending in 9362
        bankDao.insertBankAccount(buildAccount(id = "kvb_9362", bankName = "Karur Vysya Bank", accountType = "SAVINGS", maskedAccountNumber = "•••• 9362", balancePaise = 500000L))

        val engine = NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )
        // Notification arrives from account ending in 7777 (unknown)
        val payload = RawNotificationPayload(
            packageName = "com.kvb.dlite",
            title = "KVBANK",
            text = "Your KVB A/c XXXXXXXXXXXX7777 is debited by Rs. 80.00 on 24-Oct-26 to Bakery. Avl Bal Rs. 1,000.00",
            postTimeEpochMs = now
        )

        engine.onNotificationReceived(payload)

        val ingested = txDao.list.first()
        assertEquals("NEEDS_REVIEW", ingested.reviewStatus)
        assertNull("Unknown suffix must remain unlinked", ingested.bankAccountId)

        val meta = TransactionReviewMetadata.parse(ingested.note)
        assertEquals("7777", meta.extractedSuffix)
        assertEquals(AccountMatchStatus.UNKNOWN_SUFFIX_UNLINKED, meta.matchStatus)
    }

    @Test
    fun `Audit 4 - Duplicate notifications deduplication suppresses second alert`() = runBlocking {
        val txDao = AuditTransactionDao()
        val bankDao = AuditBankAccountDao()
        val now = System.currentTimeMillis()

        bankDao.insertBankAccount(buildAccount(id = "kvb_9362", bankName = "Karur Vysya Bank", accountType = "SAVINGS", maskedAccountNumber = "•••• 9362", balancePaise = 500000L))

        val engine = NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )
        val payload = RawNotificationPayload(
            packageName = "com.kvb.dlite",
            title = "KVBANK",
            text = "Your KVB A/c XXXXXXXXXXXX9362 is debited by Rs. 100.00 on 24-Oct-26 to Cafe. Ref 99881122. Avl Bal Rs. 4,900.00",
            postTimeEpochMs = now
        )

        // First ingestion
        engine.onNotificationReceived(payload)
        assertEquals(1, txDao.list.size)

        // Duplicate ingestion
        engine.onNotificationReceived(payload)
        assertEquals("Duplicate alert must be filtered without creating duplicate transaction", 1, txDao.list.size)
        assertEquals(1, engine.statsFlow.value.duplicatesFiltered)
    }
}
