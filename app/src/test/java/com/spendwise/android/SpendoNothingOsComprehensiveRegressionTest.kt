package com.spendwise.android

import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.banking.BankMessageParserRegistry
import com.spendwise.android.domain.banking.BankTransactionType
import com.spendwise.android.domain.dedup.TransactionDeduplicator
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import com.spendwise.android.notification.NotificationIngestionEngine
import com.spendwise.android.notification.model.RawNotificationPayload
import com.spendwise.android.ui.spendo.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Production regression test suite verifying Nothing OS-inspired Spendo AI features:
 * 1. Resolution of the duplicate ₹1 transfer bug (two distinct ₹1 transfers recorded vs duplicate delivery dropped).
 * 2. High-precision Indian bank message parsing for Indian Bank, Kotak Mahindra Bank, and Karur Vysya Bank (KVB).
 * 3. ATM cash movements handled as transfers without inflating expenses.
 * 4. Voice Financial Assistant deterministic intent parsing.
 * 5. ThemeMode settings and values.
 */
class SpendoNothingOsComprehensiveRegressionTest {

    private lateinit var fakeDao: TestTransactionDao
    private lateinit var parser: TransactionParser
    private lateinit var deduplicator: TransactionDeduplicator
    private lateinit var ingestionEngine: NotificationIngestionEngine

    @Before
    fun setUp() {
        fakeDao = TestTransactionDao()
        parser = TransactionParser()
        deduplicator = TransactionDeduplicator(fakeDao, windowMs = 10 * 60 * 1000L)
        ingestionEngine = NotificationIngestionEngine(fakeDao, parser, deduplicator)
    }

    // =========================================================================
    // 1. DUPLICATE ₹1 BUG REGRESSION TESTS
    // =========================================================================

    @Test
    fun testTwoDistinctOneRupeeTransfersAreBothPersisted() = runBlocking {
        val now = System.currentTimeMillis()

        // Transfer 1: First genuine ₹1 transfer with UPI Ref A
        val payload1 = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Sent ₹1.00 to Merchant Chai Point via UPI ref 429582910381",
            postTimeEpochMs = now
        )

        // Transfer 2: Second genuine ₹1 transfer 5 seconds later with distinct UPI Ref B
        val payload2 = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Sent ₹1.00 to Merchant Chai Point via UPI ref 429582910382",
            postTimeEpochMs = now + 5000L
        )

        ingestionEngine.onNotificationReceived(payload1)
        assertEquals("First ₹1 transfer must be persisted", 1, fakeDao.transactions.size)
        assertEquals(100L, fakeDao.transactions[0].amountPaise)

        ingestionEngine.onNotificationReceived(payload2)
        assertEquals(
            "Second genuine ₹1 transfer with distinct reference must ALSO be persisted (not dropped)",
            2,
            fakeDao.transactions.size
        )
        assertEquals(100L, fakeDao.transactions[1].amountPaise)
    }

    @Test
    fun testDuplicateDeliveryOfSameOneRupeeTransferIsDropped() = runBlocking {
        val now = System.currentTimeMillis()
        val originalPayload = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Sent ₹1.00 to Quick Tea via UPI ref 998877665544",
            postTimeEpochMs = now
        )
        val duplicatePayload = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Sent ₹1.00 to Quick Tea via UPI ref 998877665544",
            postTimeEpochMs = now + 15000L // 15 seconds later duplicate delivery
        )

        ingestionEngine.onNotificationReceived(originalPayload)
        assertEquals("Initial delivery persisted", 1, fakeDao.transactions.size)

        ingestionEngine.onNotificationReceived(duplicatePayload)
        assertEquals(
            "Duplicate delivery of identical ₹1 message must be dropped without creating second transaction",
            1,
            fakeDao.transactions.size
        )
    }

    // =========================================================================
    // 2. INDIAN BANK MESSAGE PARSING REGRESSION TESTS
    // =========================================================================

    @Test
    fun testIndianBankUpiDebitAndBalanceExtraction() {
        val message = "Dear Customer, A/C 1234 debited by INR 1,500.00 on 24-Oct-2026 by UPI ref 429582910382 to Reliance Fresh. Avl Bal: INR 52,400.00. - Indian Bank"
        val parsed = BankMessageParserRegistry.parse(message, "Indian Bank")

        assertNotNull("Indian Bank message must parse successfully", parsed)
        assertEquals("Indian Bank", parsed!!.bankName)
        assertEquals(150000L, parsed.amountPaise)
        assertEquals(BankTransactionType.UPI_DEBIT, parsed.transactionType)
        assertEquals("1234", parsed.accountSuffix)
        assertEquals("429582910382", parsed.referenceNumber)
        assertEquals(5240000L, parsed.reportedBalancePaise)
        assertEquals(false, parsed.isAtmCashMovement)
    }

    @Test
    fun testIndianBankAtmCashWithdrawal() {
        val message = "Cash withdrawal of INR 2,000.00 at ATM on A/C 1234 on 24-Oct-2026. Avl Bal: INR 50,400.00. - Indian Bank"
        val parsed = BankMessageParserRegistry.parse(message, "Indian Bank")

        assertNotNull(parsed)
        assertEquals(200000L, parsed!!.amountPaise)
        assertEquals(BankTransactionType.ATM_WITHDRAWAL, parsed.transactionType)
        assertEquals(true, parsed.isAtmCashMovement)
        assertEquals(5040000L, parsed.reportedBalancePaise)
    }

    // =========================================================================
    // 3. KOTAK MAHINDRA BANK MESSAGE PARSING REGRESSION TESTS
    // =========================================================================

    @Test
    fun testKotakMahindraBankUpiDebitAndBalance() {
        val message = "Sent Rs.250.00 from Kotak Bank AC 5678 to Swiggy on 24-Oct-26. UPI Ref 839201928472. Bal Rs.28,500.50"
        val parsed = BankMessageParserRegistry.parse(message, "Kotak Mahindra Bank")

        assertNotNull("Kotak message must parse successfully", parsed)
        assertEquals("Kotak Mahindra Bank", parsed!!.bankName)
        assertEquals(25000L, parsed.amountPaise)
        assertEquals(BankTransactionType.UPI_DEBIT, parsed.transactionType)
        assertEquals("5678", parsed.accountSuffix)
        assertEquals("839201928472", parsed.referenceNumber)
        assertEquals(2850050L, parsed.reportedBalancePaise)
    }

    @Test
    fun testKotakAtmCashWithdrawal() {
        val message = "Rs.2,000.00 withdrawn from ATM using Kotak Bank Card ending 5678. Bal Rs.26,500.50"
        val parsed = BankMessageParserRegistry.parse(message, "Kotak Mahindra Bank")

        assertNotNull(parsed)
        assertEquals(200000L, parsed!!.amountPaise)
        assertEquals(BankTransactionType.ATM_WITHDRAWAL, parsed.transactionType)
        assertEquals(true, parsed.isAtmCashMovement)
        assertEquals(2650050L, parsed.reportedBalancePaise)
    }

    // =========================================================================
    // 4. KARUR VYSYA BANK (KVB) MESSAGE PARSING REGRESSION TESTS
    // =========================================================================

    @Test
    fun testKvbUpiDebitAndBalance() {
        val message = "Your KVB A/c 9012 debited for INR 120.00 towards Petrol UPI Ref 938201938472. Avl Bal: INR 15,200.00. Karur Vysya Bank"
        val parsed = BankMessageParserRegistry.parse(message, "Karur Vysya Bank")

        assertNotNull("KVB message must parse successfully", parsed)
        assertEquals("Karur Vysya Bank", parsed!!.bankName)
        assertEquals(12000L, parsed.amountPaise)
        assertEquals(BankTransactionType.UPI_DEBIT, parsed.transactionType)
        assertEquals("9012", parsed.accountSuffix)
        assertEquals("938201938472", parsed.referenceNumber)
        assertEquals(1520000L, parsed.reportedBalancePaise)
    }

    @Test
    fun testKvbAtmCashWithdrawal() {
        val message = "Cash withdrawal INR 2,000.00 at KVB ATM from A/c 9012. Avl Bal: INR 13,200.00. Karur Vysya Bank"
        val parsed = BankMessageParserRegistry.parse(message, "Karur Vysya Bank")

        assertNotNull(parsed)
        assertEquals(200000L, parsed!!.amountPaise)
        assertEquals(BankTransactionType.ATM_WITHDRAWAL, parsed.transactionType)
        assertEquals(true, parsed.isAtmCashMovement)
        assertEquals(1320000L, parsed.reportedBalancePaise)
    }

    // =========================================================================
    // 5. VOICE FINANCIAL ASSISTANT PARSING REGRESSION TESTS
    // =========================================================================

    @Test
    fun testVoiceAddExpenseKotak() {
        val transcript = "Add ₹250 for lunch paid through Kotak"
        val intent = VoiceFinancialParser.parseSpeech(transcript)

        assertTrue(intent is VoiceIntent.AddExpense)
        val addExp = intent as VoiceIntent.AddExpense
        assertEquals(25000L, addExp.amountPaise)
        assertEquals("Kotak Mahindra Bank", addExp.bankName)
        assertEquals("Food & Drink", addExp.category)
    }

    @Test
    fun testVoiceAddExpenseIndianBankPetrol() {
        val transcript = "I spent ₹120 on petrol using Indian Bank"
        val intent = VoiceFinancialParser.parseSpeech(transcript)

        assertTrue(intent is VoiceIntent.AddExpense)
        val addExp = intent as VoiceIntent.AddExpense
        assertEquals(12000L, addExp.amountPaise)
        assertEquals("Indian Bank", addExp.bankName)
        assertEquals("Transit", addExp.category)
    }

    @Test
    fun testVoiceAtmWithdrawalKvb() {
        val transcript = "I withdrew ₹2,000 from an ATM using KVB"
        val intent = VoiceFinancialParser.parseSpeech(transcript)

        assertTrue(intent is VoiceIntent.AtmWithdrawal)
        val atm = intent as VoiceIntent.AtmWithdrawal
        assertEquals(200000L, atm.amountPaise)
        assertEquals("Karur Vysya Bank", atm.bankName)
    }

    @Test
    fun testVoiceTotalBalanceQuery() {
        val transcript = "Show me my total balance across my bank accounts"
        val intent = VoiceFinancialParser.parseSpeech(transcript)

        assertTrue(intent is VoiceIntent.InquireBalance)
        val bal = intent as VoiceIntent.InquireBalance
        assertEquals(null, bal.targetBank)
    }

    @Test
    fun testVoiceAmbiguousExpenseTriggersPrompt() {
        val transcript = "Add expense for coffee"
        val intent = VoiceFinancialParser.parseSpeech(transcript)

        assertTrue(intent is VoiceIntent.AmbiguousExpense)
    }

    // =========================================================================
    // 6. THEME MODE SYSTEM OPTIONS
    // =========================================================================

    @Test
    fun testThemeModeOptions() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
        assertEquals(3, ThemeMode.values().size)
    }

    // =========================================================================
    // IN-MEMORY TEST TRANSACTION DAO
    // =========================================================================
    class TestTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)

        override fun getConfirmedTransactions(): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.reviewStatus == "CONFIRMED" })

        override fun getTransactionsByStatus(reviewStatus: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.reviewStatus == reviewStatus })

        override fun getTransactionsByDirection(direction: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.direction == direction })

        override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.categoryId == categoryId })

        override fun getTransactionById(id: String): Flow<TransactionEntity?> =
            flowOf(transactions.find { it.id == id })

        override suspend fun getTransactionByIdDirect(id: String): TransactionEntity? =
            transactions.find { it.id == id }

        override suspend fun getTransactionByDedupeKey(dedupeKey: String): TransactionEntity? =
            transactions.find { it.dedupeKey == dedupeKey }

        override suspend fun findPotentialDuplicates(
            amountPaise: Long,
            direction: String,
            minTime: Long,
            maxTime: Long
        ): List<TransactionEntity> = transactions.filter {
            it.amountPaise == amountPaise &&
                    it.direction == direction &&
                    it.occurredAtEpochMillis in minTime..maxTime
        }

        override fun getTodayDebitsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "DEBIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.occurredAtEpochMillis in startOfDay..endOfDay }.sumOf { it.amountPaise })

        override fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "CREDIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.occurredAtEpochMillis in startOfDay..endOfDay }.sumOf { it.amountPaise })

        override fun getPeriodDebitsPaise(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "DEBIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.occurredAtEpochMillis >= startOfPeriod }.sumOf { it.amountPaise })

        override suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter { it.direction == "DEBIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.occurredAtEpochMillis in startTime..endTime }.sumOf { it.amountPaise }

        override suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter { it.direction == "CREDIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.occurredAtEpochMillis in startTime..endTime }.sumOf { it.amountPaise }

        override fun getPeriodIncomeCredits(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "CREDIT" && it.categoryId == "INCOME" && it.occurredAtEpochMillis >= startOfPeriod }.sumOf { it.amountPaise })

        override fun getPeriodRefundCredits(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "CREDIT" && !it.categoryId.equals("TRANSFER", ignoreCase = true) && it.categoryId != "INCOME" && it.occurredAtEpochMillis >= startOfPeriod }.sumOf { it.amountPaise })

        override fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<CategorySpending>> =
            flowOf(emptyList())

        override fun getPendingReviewCount(): Flow<Int> =
            flowOf(transactions.count { it.reviewStatus == "NEEDS_REVIEW" })

        override suspend fun updateReviewStatus(id: String, newStatus: String, updatedAt: Long) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx >= 0) transactions[idx] = transactions[idx].copy(reviewStatus = newStatus, updatedAtEpochMillis = updatedAt)
        }

        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { (it.merchant?.contains(query, true) == true) || (it.note?.contains(query, true) == true) })

        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.occurredAtEpochMillis in startTime..endTime })

        override fun getTransactionsForBank(bankAccountName: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.bankAccountName == bankAccountName })

        override fun getTransactionsForMerchant(merchantQuery: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.merchant?.contains(merchantQuery, true) == true })

        override suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long =
            transactions.filter { it.merchant?.contains(merchantQuery, true) == true && it.direction == "DEBIT" && it.reviewStatus == "CONFIRMED" }.sumOf { it.amountPaise }

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactions.add(transaction)
        }

        override suspend fun insertTransactions(list: List<TransactionEntity>) {
            transactions.addAll(list)
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val idx = transactions.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) transactions[idx] = transaction
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            transactions.removeAll { it.id == transaction.id }
        }

        override suspend fun deleteTransactionById(id: String) {
            transactions.removeAll { it.id == id }
        }

        override suspend fun deleteAllTransactions() {
            transactions.clear()
        }
    }
}
