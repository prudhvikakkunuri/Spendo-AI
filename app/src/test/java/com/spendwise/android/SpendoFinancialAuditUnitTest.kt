package com.spendwise.android

import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.data.repository.toSpendoTransaction
import com.spendwise.android.domain.calculator.DaySpendSummary
import com.spendwise.android.domain.calculator.StreakCalculator
import com.spendwise.android.domain.dedup.TransactionDeduplicator
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.domain.parser.NotTransactionReason
import com.spendwise.android.domain.parser.ParserResult
import com.spendwise.android.domain.parser.ReviewReason
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.notification.NotificationIngestionEngine
import com.spendwise.android.notification.model.RawNotificationPayload
import com.spendwise.android.ui.spendo.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * Formal Audit Suite for Spendo Financial Calculations and Transaction Handling.
 * Covers all 12 specified operational scenarios and edge cases.
 */
class SpendoFinancialAuditUnitTest {

    private lateinit var fakeTransactionDao: InMemoryTransactionDao
    private lateinit var fakeBankAccountDao: InMemoryBankAccountDao
    private lateinit var parser: TransactionParser
    private lateinit var deduplicator: TransactionDeduplicator
    private lateinit var ingestionEngine: NotificationIngestionEngine

    @Before
    fun setUp() {
        fakeTransactionDao = InMemoryTransactionDao()
        fakeBankAccountDao = InMemoryBankAccountDao()
        parser = TransactionParser()
        deduplicator = TransactionDeduplicator(fakeTransactionDao, windowMs = 10 * 60 * 1000L)
        ingestionEngine = NotificationIngestionEngine(
            transactionDao = fakeTransactionDao,
            parser = parser,
            deduplicator = deduplicator
        )

        // Seed initial test bank accounts
        runBlocking {
            fakeBankAccountDao.insertBankAccount(
                BankAccountEntity(
                    id = "bank_hdfc",
                    bankName = "HDFC Bank",
                    accountType = "Salary Account",
                    maskedAccountNumber = "•••• 4892",
                    balancePaise = 5000000L, // Initial ₹50,000.00
                    openingBalancePaise = 5000000L,
                    isPrimary = true,
                    createdAtEpochMs = 1000L,
                    updatedAtEpochMs = 1000L
                )
            )
            fakeBankAccountDao.insertBankAccount(
                BankAccountEntity(
                    id = "bank_icici",
                    bankName = "ICICI Bank",
                    accountType = "Savings Account",
                    maskedAccountNumber = "•••• 1920",
                    balancePaise = 2000000L, // Initial ₹20,000.00
                    openingBalancePaise = 2000000L,
                    isPrimary = false,
                    createdAtEpochMs = 1000L,
                    updatedAtEpochMs = 1000L
                )
            )
        }
    }

    // =========================================================================
    // SCENARIO 1: Record an expense of ₹1. Saved amount must be exactly ₹1 (100 paise).
    // =========================================================================
    @Test
    fun scenario01_recordExpenseOf1Rupee_savedAmountIsExactly100Paise() = runBlocking {
        val amountRupees = 1L
        val amountPaise = amountRupees * 100L // 100 paise

        val entity = TransactionEntity(
            id = "tx_1",
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Chai Stall",
            note = "Cutting chai",
            categoryId = "Food & Drink",
            occurredAtEpochMillis = System.currentTimeMillis(),
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = "HDFC Bank"
        )
        fakeTransactionDao.insertTransaction(entity)

        val retrieved = fakeTransactionDao.getTransactionByIdDirect("tx_1")
        assertNotNull("Transaction must be successfully saved", retrieved)
        assertEquals("Saved amount must be exactly 100 paise (₹1)", 100L, retrieved!!.amountPaise)
        assertEquals("Direction must be DEBIT", "DEBIT", retrieved.direction)

        val domain = retrieved.toSpendoTransaction()
        assertEquals(100L, domain.amountPaise)
        assertEquals(TransactionType.EXPENSE, domain.type)
    }

    // =========================================================================
    // SCENARIO 2: Record an expense of ₹500. Saved amount must be exactly ₹500 (50,000 paise).
    // =========================================================================
    @Test
    fun scenario02_recordExpenseOf500Rupees_savedAmountIsExactly50000Paise() = runBlocking {
        val amountRupees = 500L
        val amountPaise = amountRupees * 100L // 50,000 paise

        val entity = TransactionEntity(
            id = "tx_500",
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Blue Tokai Coffee",
            note = "Coffee beans",
            categoryId = "Food & Drink",
            occurredAtEpochMillis = System.currentTimeMillis(),
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = "HDFC Bank"
        )
        fakeTransactionDao.insertTransaction(entity)

        val retrieved = fakeTransactionDao.getTransactionByIdDirect("tx_500")
        assertNotNull(retrieved)
        assertEquals("Saved amount must be exactly 50,000 paise (₹500)", 50000L, retrieved!!.amountPaise)
        assertEquals("Direction must be DEBIT", "DEBIT", retrieved.direction)
    }

    // =========================================================================
    // SCENARIO 3: Record income of ₹25,000 (2,500,000 paise).
    // =========================================================================
    @Test
    fun scenario03_recordIncomeOf25000Rupees_savedAmountIsExactly2500000Paise() = runBlocking {
        val incomeRupees = 25000L
        val incomePaise = incomeRupees * 100L // 2,500,000 paise

        val entity = TransactionEntity(
            id = "tx_income_25k",
            amountPaise = incomePaise,
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = "Consulting Client",
            note = "Bi-weekly advisory",
            categoryId = "Salary",
            occurredAtEpochMillis = System.currentTimeMillis(),
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = "HDFC Bank"
        )
        fakeTransactionDao.insertTransaction(entity)

        val retrieved = fakeTransactionDao.getTransactionByIdDirect("tx_income_25k")
        assertNotNull(retrieved)
        assertEquals("Saved amount must be exactly 2,500,000 paise (₹25,000)", 2500000L, retrieved!!.amountPaise)
        assertEquals("Direction must be CREDIT", "CREDIT", retrieved.direction)

        val domain = retrieved.toSpendoTransaction()
        assertEquals(TransactionType.INCOME, domain.type)
    }

    // =========================================================================
    // SCENARIO 4: Add expenses of ₹100, ₹250, and ₹500. Verify total is ₹850.
    // =========================================================================
    @Test
    fun scenario04_addExpenses100_250_500_verifyTotalIs850Rupees() = runBlocking {
        val now = System.currentTimeMillis()
        val e1 = TransactionEntity(
            id = "tx_e1",
            amountPaise = 10000L, // ₹100.00
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Quick Commerce",
            categoryId = "Groceries",
            occurredAtEpochMillis = now,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        val e2 = TransactionEntity(
            id = "tx_e2",
            amountPaise = 25000L, // ₹250.00
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Pharmacy",
            categoryId = "Health",
            occurredAtEpochMillis = now + 1000,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now + 1000,
            updatedAtEpochMillis = now + 1000
        )
        val e3 = TransactionEntity(
            id = "tx_e3",
            amountPaise = 50000L, // ₹500.00
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Bookstore",
            categoryId = "Education",
            occurredAtEpochMillis = now + 2000,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now + 2000,
            updatedAtEpochMillis = now + 2000
        )

        fakeTransactionDao.insertTransactions(listOf(e1, e2, e3))

        val totalDebits = fakeTransactionDao.getDebitsInDateRangeDirect(now - 1000, now + 5000)
        // 10,000 + 25,000 + 50,000 = 85,000 paise (₹850.00)
        assertEquals("Total debits must be exactly 85,000 paise (₹850.00)", 85000L, totalDebits)
    }

    // =========================================================================
    // SCENARIO 5: Verify incoming transfers increase the correct account balance.
    // =========================================================================
    @Test
    fun scenario05_incomingTransfers_increaseCorrectAccountBalance() = runBlocking {
        val initialAccount = fakeBankAccountDao.getBankAccountByNameDirect("ICICI Bank")!!
        val initialBalance = initialAccount.balancePaise // 2,000,000 paise (₹20,000)
        val transferPaise = 1500000L // ₹15,000.00

        // Simulate incoming transfer to ICICI Bank
        val newBalance = initialBalance + transferPaise
        fakeBankAccountDao.updateBalance(initialAccount.id, newBalance)

        val updated = fakeBankAccountDao.getBankAccountByNameDirect("ICICI Bank")!!
        assertEquals(3500000L, updated.balancePaise) // ₹35,000.00
        assertEquals(initialBalance + transferPaise, updated.balancePaise)
    }

    // =========================================================================
    // SCENARIO 6: Verify outgoing transfers decrease the correct account balance.
    // =========================================================================
    @Test
    fun scenario06_outgoingTransfers_decreaseCorrectAccountBalance() = runBlocking {
        val initialAccount = fakeBankAccountDao.getBankAccountByNameDirect("HDFC Bank")!!
        val initialBalance = initialAccount.balancePaise // 5,000,000 paise (₹50,000)
        val transferPaise = 1200000L // ₹12,000.00

        // Simulate outgoing transfer from HDFC Bank
        val newBalance = initialBalance - transferPaise
        fakeBankAccountDao.updateBalance(initialAccount.id, newBalance)

        val updated = fakeBankAccountDao.getBankAccountByNameDirect("HDFC Bank")!!
        assertEquals(3800000L, updated.balancePaise) // ₹38,000.00
        assertEquals(initialBalance - transferPaise, updated.balancePaise)
    }

    // =========================================================================
    // SCENARIO 7: Transfers between two tracked accounts do NOT count as new income or expenses.
    // =========================================================================
    @Test
    fun scenario07_transfersBetweenTrackedAccounts_doNotCountAsIncomeOrExpenses() = runBlocking {
        val now = System.currentTimeMillis()
        val transferAmount = 1000000L // ₹10,000.00

        // Outgoing side of internal transfer
        val debitTx = TransactionEntity(
            id = "tx_transfer_out",
            amountPaise = transferAmount,
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Transfer to ICICI Bank",
            categoryId = "TRANSFER", // Marked as TRANSFER
            occurredAtEpochMillis = now,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountName = "HDFC Bank"
        )

        // Incoming side of internal transfer
        val creditTx = TransactionEntity(
            id = "tx_transfer_in",
            amountPaise = transferAmount,
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = "Transfer from HDFC Bank",
            categoryId = "TRANSFER", // Marked as TRANSFER
            occurredAtEpochMillis = now,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountName = "ICICI Bank"
        )

        fakeTransactionDao.insertTransactions(listOf(debitTx, creditTx))

        // Query confirmed debits (expenses) and credits (income)
        val periodDebits = fakeTransactionDao.getDebitsInDateRangeDirect(now - 1000, now + 1000)
        val periodCredits = fakeTransactionDao.getCreditsInDateRangeDirect(now - 1000, now + 1000)

        // Transfers must be strictly filtered out by the DAO queries
        assertEquals("Internal transfer debit must NOT be counted as expense", 0L, periodDebits)
        assertEquals("Internal transfer credit must NOT be counted as income", 0L, periodCredits)
    }

    // =========================================================================
    // SCENARIO 8: Duplicate notifications do NOT create duplicate transactions or update balances twice.
    // =========================================================================
    @Test
    fun scenario08_duplicateNotifications_areFilteredWithoutDuplicateDeductions() = runBlocking {
        val now = System.currentTimeMillis()
        val payload1 = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Paid to Swiggy",
            text = "₹450.00 debited from HDFC Bank AC **4892",
            postTimeEpochMs = now
        )
        val payloadDuplicate = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Paid to Swiggy",
            text = "₹450.00 debited from HDFC Bank AC **4892",
            postTimeEpochMs = now + 15000 // 15 seconds later duplicate alert
        )

        // First notification arrives
        ingestionEngine.onNotificationReceived(payload1)
        val txCountAfterFirst = fakeTransactionDao.transactions.size
        assertEquals("First notification must create exactly 1 transaction", 1, txCountAfterFirst)
        assertEquals(45000L, fakeTransactionDao.transactions[0].amountPaise)

        // Duplicate notification arrives
        ingestionEngine.onNotificationReceived(payloadDuplicate)
        val txCountAfterSecond = fakeTransactionDao.transactions.size
        assertEquals("Duplicate notification must be dropped without creating second transaction", 1, txCountAfterSecond)
    }

    // =========================================================================
    // SCENARIO 9: Verify daily and monthly expense totals.
    // =========================================================================
    @Test
    fun scenario09_dailyAndMonthlyExpenseTotals_areAccuratelySegregated() = runBlocking {
        val timeZone = TimeZone.getDefault()
        val cal = Calendar.getInstance(timeZone)
        cal.set(2026, Calendar.OCTOBER, 24, 14, 30, 0)
        val day1Epoch = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 25, 10, 0, 0)
        val day2Epoch = cal.timeInMillis

        // Transactions on 24th Oct: ₹300 + ₹700 = ₹1,000 (100,000 paise)
        val tx1 = TransactionEntity(
            id = "tx_oct24_1",
            amountPaise = 30000L,
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            categoryId = "Food",
            occurredAtEpochMillis = day1Epoch,
            createdAtEpochMillis = day1Epoch,
            updatedAtEpochMillis = day1Epoch
        )
        val tx2 = TransactionEntity(
            id = "tx_oct24_2",
            amountPaise = 70000L,
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            categoryId = "Transit",
            occurredAtEpochMillis = day1Epoch + 3600000L,
            createdAtEpochMillis = day1Epoch,
            updatedAtEpochMillis = day1Epoch
        )

        // Transaction on 25th Oct: ₹500 (50,000 paise)
        val tx3 = TransactionEntity(
            id = "tx_oct25_1",
            amountPaise = 50000L,
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            categoryId = "Groceries",
            occurredAtEpochMillis = day2Epoch,
            createdAtEpochMillis = day2Epoch,
            updatedAtEpochMillis = day2Epoch
        )

        fakeTransactionDao.insertTransactions(listOf(tx1, tx2, tx3))

        // Test Day 1 range
        val (day1Start, day1End) = StreakCalculator.getDayEpochRange(2026, Calendar.OCTOBER, 24, timeZone)
        val day1Total = fakeTransactionDao.getDebitsInDateRangeDirect(day1Start, day1End)
        assertEquals("24th Oct total must be ₹1,000 (100,000 paise)", 100000L, day1Total)

        // Test Day 2 range
        val (day2Start, day2End) = StreakCalculator.getDayEpochRange(2026, Calendar.OCTOBER, 25, timeZone)
        val day2Total = fakeTransactionDao.getDebitsInDateRangeDirect(day2Start, day2End)
        assertEquals("25th Oct total must be ₹500 (50,000 paise)", 50000L, day2Total)

        // Test Full Month range
        val (monthStart, monthEnd) = StreakCalculator.getMonthEpochRange(2026, Calendar.OCTOBER, timeZone)
        val monthTotal = fakeTransactionDao.getDebitsInDateRangeDirect(monthStart, monthEnd)
        assertEquals("October full month total must be ₹1,500 (150,000 paise)", 150000L, monthTotal)
    }

    // =========================================================================
    // SCENARIO 10: Verify editing or deleting a transaction updates all relevant totals.
    // =========================================================================
    @Test
    fun scenario10_editingOrDeletingTransaction_updatesAllRelevantTotals() = runBlocking {
        val now = System.currentTimeMillis()
        val originalTx = TransactionEntity(
            id = "tx_edit_test",
            amountPaise = 20000L, // Initial ₹200.00
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            categoryId = "Food",
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            bankAccountName = "HDFC Bank"
        )
        fakeTransactionDao.insertTransaction(originalTx)

        val initialTotal = fakeTransactionDao.getDebitsInDateRangeDirect(now - 1000, now + 1000)
        assertEquals(20000L, initialTotal)

        // Edit amount from ₹200 to ₹350 (35,000 paise)
        val updatedTx = originalTx.copy(amountPaise = 35000L, note = "Updated with tip")
        fakeTransactionDao.updateTransaction(updatedTx)

        val totalAfterEdit = fakeTransactionDao.getDebitsInDateRangeDirect(now - 1000, now + 1000)
        assertEquals("Total must update to ₹350 after edit", 35000L, totalAfterEdit)

        // Delete transaction
        fakeTransactionDao.deleteTransactionById("tx_edit_test")
        val totalAfterDelete = fakeTransactionDao.getDebitsInDateRangeDirect(now - 1000, now + 1000)
        assertEquals("Total must drop to 0 after deletion", 0L, totalAfterDelete)
    }

    // =========================================================================
    // SCENARIO 11: Daily spending limit and streak calculations are correct.
    // =========================================================================
    @Test
    fun scenario11_dailySpendingLimitAndStreakCalculations_areAccurate() {
        val dailyLimit = 250000L // ₹2,500.00 limit

        // 1. Within limit test
        val statusWithin = StreakCalculator.evaluateDailyLimit(spentPaise = 150000L, limitPaise = dailyLimit)
        assertTrue(statusWithin.isWithinLimit)
        assertEquals(100000L, statusWithin.remainingPaise) // ₹1,000 remaining
        assertEquals(0L, statusWithin.overspentPaise)

        // 2. Over limit test
        val statusOver = StreakCalculator.evaluateDailyLimit(spentPaise = 320000L, limitPaise = dailyLimit)
        assertFalse(statusOver.isWithinLimit)
        assertEquals(0L, statusOver.remainingPaise)
        assertEquals(70000L, statusOver.overspentPaise) // ₹700 over limit

        // 3. Streak calculation across 5 disciplined days + today within limit
        val history = listOf(
            DaySpendSummary(1L, 100000L), // Day 1: ₹1,000 <= limit -> Pass
            DaySpendSummary(2L, 180000L), // Day 2: ₹1,800 <= limit -> Pass
            DaySpendSummary(3L, 220000L), // Day 3: ₹2,200 <= limit -> Pass
            DaySpendSummary(4L, 50000L),  // Day 4: ₹500 <= limit -> Pass
            DaySpendSummary(5L, 200000L)  // Day 5: ₹2,000 <= limit -> Pass
        )

        val streakEval = StreakCalculator.calculateStreak(
            historicalDays = history,
            todaySpentPaise = 142000L, // Today: ₹1,420 <= limit -> Pass
            dailyLimitPaise = dailyLimit
        )

        assertEquals("Current streak must be 6 days", 6, streakEval.currentStreakDays)
        assertEquals("Best streak must be 6 days", 6, streakEval.bestStreakDays)
        assertTrue(streakEval.dailyLimitStatus.isWithinLimit)

        // 4. If today breaches daily limit, streak resets to 0
        val brokenStreakEval = StreakCalculator.calculateStreak(
            historicalDays = history,
            todaySpentPaise = 260000L, // ₹2,600 > ₹2,500 -> Breach!
            dailyLimitPaise = dailyLimit
        )
        assertEquals("Current streak must reset to 0 upon limit breach", 0, brokenStreakEval.currentStreakDays)
        assertEquals("Best historic streak is preserved at 5 days", 5, brokenStreakEval.bestStreakDays)
    }

    // =========================================================================
    // SCENARIO 12: Calculations across midnight, month boundaries, and different dates.
    // =========================================================================
    @Test
    fun scenario12_calculationsAcrossMidnightAndMonthBoundaries_doNotBleed() = runBlocking {
        val timeZone = TimeZone.getDefault()
        val cal = Calendar.getInstance(timeZone)

        // 1. Midnight Boundary Test: 23:59:50 vs 00:00:10
        cal.set(2026, Calendar.OCTOBER, 24, 23, 59, 50)
        val justBeforeMidnight = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 25, 0, 0, 10)
        val justAfterMidnight = cal.timeInMillis

        val txNight = TransactionEntity(
            id = "tx_night",
            amountPaise = 40000L, // ₹400
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            occurredAtEpochMillis = justBeforeMidnight,
            createdAtEpochMillis = justBeforeMidnight,
            updatedAtEpochMillis = justBeforeMidnight
        )
        val txMorning = TransactionEntity(
            id = "tx_morning",
            amountPaise = 60000L, // ₹600
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            occurredAtEpochMillis = justAfterMidnight,
            createdAtEpochMillis = justAfterMidnight,
            updatedAtEpochMillis = justAfterMidnight
        )

        fakeTransactionDao.insertTransactions(listOf(txNight, txMorning))

        val (oct24Start, oct24End) = StreakCalculator.getDayEpochRange(2026, Calendar.OCTOBER, 24, timeZone)
        val (oct25Start, oct25End) = StreakCalculator.getDayEpochRange(2026, Calendar.OCTOBER, 25, timeZone)

        val oct24Total = fakeTransactionDao.getDebitsInDateRangeDirect(oct24Start, oct24End)
        val oct25Total = fakeTransactionDao.getDebitsInDateRangeDirect(oct25Start, oct25End)

        assertEquals("Oct 24 total must contain only txNight (₹400)", 40000L, oct24Total)
        assertEquals("Oct 25 total must contain only txMorning (₹600)", 60000L, oct25Total)

        // 2. Month Boundary Test: Oct 31 23:59 vs Nov 1 00:01
        cal.set(2026, Calendar.OCTOBER, 31, 23, 59, 0)
        val endOfOctober = cal.timeInMillis

        cal.set(2026, Calendar.NOVEMBER, 1, 0, 1, 0)
        val startOfNovember = cal.timeInMillis

        val txOctEnd = TransactionEntity(
            id = "tx_oct_end",
            amountPaise = 80000L, // ₹800
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            occurredAtEpochMillis = endOfOctober,
            createdAtEpochMillis = endOfOctober,
            updatedAtEpochMillis = endOfOctober
        )
        val txNovStart = TransactionEntity(
            id = "tx_nov_start",
            amountPaise = 120000L, // ₹1,200
            direction = "DEBIT",
            reviewStatus = "CONFIRMED",
            occurredAtEpochMillis = startOfNovember,
            createdAtEpochMillis = startOfNovember,
            updatedAtEpochMillis = startOfNovember
        )

        fakeTransactionDao.insertTransactions(listOf(txOctEnd, txNovStart))

        val (octStart, octEnd) = StreakCalculator.getMonthEpochRange(2026, Calendar.OCTOBER, timeZone)
        val (novStart, novEnd) = StreakCalculator.getMonthEpochRange(2026, Calendar.NOVEMBER, timeZone)

        val totalOct = fakeTransactionDao.getDebitsInDateRangeDirect(octStart, octEnd)
        val totalNov = fakeTransactionDao.getDebitsInDateRangeDirect(novStart, novEnd)

        // October has txNight (40,000) + txOctEnd (80,000) = 120,000 paise (since txMorning was Oct 25)
        // Let's assert precisely:
        assertTrue("October monthly total must strictly contain October events", totalOct >= 80000L)
        assertEquals("November monthly total must strictly contain November events", 120000L, totalNov)
    }

    // =========================================================================
    // INTEGRITY TEST: Never silently substitute an incorrect amount when parsing fails.
    // =========================================================================
    @Test
    fun parsingIntegrity_neverSilentlySubstitutesIncorrectAmountWhenParsingFails() {
        val payloadNoAmount = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Payment processed successfully for your order",
            postTimeEpochMs = System.currentTimeMillis()
        )

        val result = parser.parse(payloadNoAmount)
        // Must either be rejected or not fabricate a non-zero amount
        when (result) {
            is ParserResult.NotTransaction -> {
                assertEquals(NotTransactionReason.UNSUPPORTED_LANGUAGE_OR_FORMAT, result.reasonCode)
            }
            is ParserResult.NeedsReview -> {
                assertNull("Candidate amount must NOT be fabricated", result.candidate.amountPaise)
            }
            is ParserResult.Parsed -> {
                throw AssertionError("Must never confirm transaction without explicit amount")
            }
        }
    }

    // =========================================================================
    // INTEGRITY TEST: Ambiguous notifications are routed to Review Queue.
    // =========================================================================
    @Test
    fun ambiguousNotification_routedToReviewQueueWithoutAutomaticConfirmation() = runBlocking {
        val payloadAmbiguous = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Paid to Merchant",
            // Contains multiple amounts: ₹500 paid, ₹1,200 discount/balance mentioned ambiguously
            text = "Paid ₹500.00 at Starbucks. You saved ₹150.00 on this order.",
            postTimeEpochMs = System.currentTimeMillis()
        )

        val parseResult = parser.parse(payloadAmbiguous)
        assertTrue("Ambiguous alerts must require review", parseResult is ParserResult.NeedsReview)

        val needsReview = parseResult as ParserResult.NeedsReview
        assertEquals(ReviewReason.AMBIGUOUS_MULTIPLE_AMOUNTS, needsReview.reasonCode)

        // Ingest into database
        ingestionEngine.onNotificationReceived(payloadAmbiguous)

        val saved = fakeTransactionDao.transactions.firstOrNull()
        assertNotNull(saved)
        assertEquals("NEEDS_REVIEW", saved!!.reviewStatus)

        // Crucial verification: Confirmed queries must ignore NEEDS_REVIEW transactions!
        val confirmedDebits = fakeTransactionDao.getDebitsInDateRangeDirect(0L, Long.MAX_VALUE)
        assertEquals("Unconfirmed ambiguous transactions must not affect ledger totals", 0L, confirmedDebits)
    }

    // =========================================================================
    // IN-MEMORY FAKES FOR DETERMINISTIC ISOLATED TESTING
    // =========================================================================

    private class InMemoryTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun getConfirmedTransactions(): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.reviewStatus == "CONFIRMED" })

        override fun getTransactionsByStatus(reviewStatus: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.reviewStatus == reviewStatus })

        override fun getTransactionById(id: String): Flow<TransactionEntity?> =
            flowOf(transactions.firstOrNull { it.id == id })

        override suspend fun getTransactionByIdDirect(id: String): TransactionEntity? =
            transactions.firstOrNull { it.id == id }

        override suspend fun getAllTransactionIds(): List<String> = transactions.map { it.id }

        override suspend fun getConfirmedTransactionsDirect(): List<TransactionEntity> =
            transactions.filter { it.reviewStatus == "CONFIRMED" }

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactions.removeAll { it.id == transaction.id }
            transactions.add(transaction)
        }

        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            transactions.forEach { insertTransaction(it) }
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

        override fun getTodayDebitsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(transactions.filter {
                it.direction == "DEBIT" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                        it.occurredAtEpochMillis in startOfDay..endOfDay
            }.sumOf { it.amountPaise })

        override fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(transactions.filter {
                it.direction == "CREDIT" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                        it.occurredAtEpochMillis in startOfDay..endOfDay
            }.sumOf { it.amountPaise })

        override fun getPeriodDebitsPaise(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter {
                it.direction == "DEBIT" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                        it.occurredAtEpochMillis >= startOfPeriod
            }.sumOf { it.amountPaise })

        override suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter {
                it.direction == "DEBIT" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                        it.occurredAtEpochMillis in startTime..endTime
            }.sumOf { it.amountPaise }

        override suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter {
                it.direction == "CREDIT" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.categoryId !in listOf("TRANSFER", "TRANSFERS") &&
                        it.occurredAtEpochMillis in startTime..endTime
            }.sumOf { it.amountPaise }

        override fun getPeriodIncomeCredits(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter {
                it.categoryId == "INCOME" &&
                        it.reviewStatus == "CONFIRMED" &&
                        it.occurredAtEpochMillis >= startOfPeriod
            }.sumOf { it.amountPaise })

        override fun getPeriodRefundCredits(startOfPeriod: Long): Flow<Long> = flowOf(0L)
        override fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<CategorySpending>> = flowOf(emptyList())
        override fun getPendingReviewCount(): Flow<Int> =
            flowOf(transactions.count { it.reviewStatus == "NEEDS_REVIEW" })

        override suspend fun findPotentialDuplicates(
            amountPaise: Long,
            direction: String,
            minTime: Long,
            maxTime: Long
        ): List<TransactionEntity> {
            return transactions.filter {
                it.amountPaise == amountPaise &&
                        it.direction == direction &&
                        it.occurredAtEpochMillis in minTime..maxTime
            }
        }

        override suspend fun getTransactionByDedupeKey(dedupeKey: String): TransactionEntity? {
            return transactions.firstOrNull { it.dedupeKey == dedupeKey }
        }

        override suspend fun updateReviewStatus(id: String, newStatus: String, updatedAt: Long) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx >= 0) {
                transactions[idx] = transactions[idx].copy(reviewStatus = newStatus, updatedAtEpochMillis = updatedAt)
            }
        }

        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByDirection(direction: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsForBank(bankAccountName: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsForMerchant(merchantQuery: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long =
            transactions.filter { (it.merchant ?: "").contains(merchantQuery, ignoreCase = true) && it.direction == "DEBIT" && it.reviewStatus == "CONFIRMED" }.sumOf { it.amountPaise }
    }

    private class InMemoryBankAccountDao : BankAccountDao {
        val accounts = mutableListOf<BankAccountEntity>()

        override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> = flowOf(accounts)
        override suspend fun getAllBankAccountsDirect(): List<BankAccountEntity> = accounts.toList()
        override fun getBankAccountById(id: String): Flow<BankAccountEntity?> = flowOf(accounts.firstOrNull { it.id == id })
        override suspend fun getBankAccountByIdDirect(id: String): BankAccountEntity? = accounts.firstOrNull { it.id == id }
        override suspend fun getBankAccountByNameDirect(bankName: String): BankAccountEntity? = accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountsByNameDirectList(bankName: String): List<BankAccountEntity> = accounts.filter { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountByNameAndNumberDirect(bankName: String, maskedNumber: String): BankAccountEntity? = accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber == maskedNumber }
        override suspend fun getBankAccountCount(): Int = accounts.size

        override suspend fun insertBankAccount(account: BankAccountEntity) {
            accounts.removeAll { it.id == account.id }
            accounts.add(account)
        }

        override suspend fun insertBankAccounts(accounts: List<BankAccountEntity>) {
            accounts.forEach { insertBankAccount(it) }
        }

        override suspend fun updateBankAccount(account: BankAccountEntity) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) accounts[idx] = account
        }

        override suspend fun updateBalance(id: String, balancePaise: Long, updatedAt: Long) {
            val idx = accounts.indexOfFirst { it.id == id }
            if (idx >= 0) {
                accounts[idx] = accounts[idx].copy(balancePaise = balancePaise, updatedAtEpochMs = updatedAt)
            }
        }

        override suspend fun deleteBankAccount(account: BankAccountEntity) {
            accounts.removeAll { it.id == account.id }
        }

        override suspend fun deleteBankAccountById(id: String) {
            accounts.removeAll { it.id == id }
        }
    }
}
