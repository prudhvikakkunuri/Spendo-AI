package com.spendwise.android

import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.dedup.TransactionDeduplicator
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.domain.parser.NotTransactionReason
import com.spendwise.android.domain.parser.ParserResult
import com.spendwise.android.domain.parser.ReviewReason
import com.spendwise.android.domain.parser.TransactionCandidate
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.notification.NotificationIngestionEngine
import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Tests for Phase 5:
 * Transaction Parsing, Deduplication, and Review Queue.
 *
 * Uses synthetic test fixtures to test:
 * 1. Debit transactions (Google Pay, PhonePe, Paytm, Bank alerts)
 * 2. Credit & Salary transactions
 * 3. Protection against mistaking account balance for transaction amount
 * 4. Detection of refunds, reversals, and failed transactions
 * 5. Rule that refunds are never treated as salary and failed payments are never confirmed
 * 6. Ambiguous multiple amounts routing to review
 * 7. OTP, promotional, and balance inquiry rejection
 * 8. Multi-signal deduplication
 * 9. Ingestion Engine Room persistence routing
 */
class Phase5TransactionParsingUnitTest {

    private lateinit var parser: TransactionParser

    @Before
    fun setUp() {
        parser = TransactionParser()
    }

    // ---------------------------------------------------------
    // 1. Debit Transaction Parsing Tests
    // ---------------------------------------------------------

    @Test
    fun parse_debit_googlePay_extractsCorrectFields() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            postTimeEpochMs = 1713500000000L,
            title = "Paid to Swiggy",
            text = "₹450.00 debited from Axis Bank **1234 using UPI",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Should be Parsed result", result is ParserResult.Parsed)
        val candidate = (result as ParserResult.Parsed).candidate

        assertEquals(45000L, candidate.amountPaise)
        assertEquals(TransactionDirection.DEBIT, candidate.direction)
        assertEquals("Swiggy", candidate.merchant)
        assertEquals(TransactionCategory.FOOD, candidate.suggestedCategory)
        assertTrue("Confidence must be >= 0.90", candidate.confidence >= 0.90f)
        assertFalse("Must not be failed", candidate.isFailed)
        assertFalse("Must not be refund", candidate.isRefundOrReversal)
    }

    @Test
    fun parse_debit_starbucksPaytm_extractsPaiseAndCategory() {
        val payload = RawNotificationPayload(
            packageName = "net.one97.paytm",
            postTimeEpochMs = 1713500000000L,
            title = "Paid at Starbucks",
            text = "Paid ₹1,200.50 at Starbucks. UPI Ref 987654321",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue(result is ParserResult.Parsed)
        val candidate = (result as ParserResult.Parsed).candidate

        assertEquals(120050L, candidate.amountPaise)
        assertEquals(TransactionDirection.DEBIT, candidate.direction)
        assertEquals("Starbucks", candidate.merchant)
        assertEquals(TransactionCategory.FOOD, candidate.suggestedCategory)
    }

    @Test
    fun parse_debit_uberTransport_extractsTransportCategory() {
        val payload = RawNotificationPayload(
            packageName = "com.phonepe.app",
            postTimeEpochMs = 1713500000000L,
            title = "PhonePe Alert",
            text = "Paid ₹299 to Uber for your ride",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue(result is ParserResult.Parsed)
        val candidate = (result as ParserResult.Parsed).candidate

        assertEquals(29900L, candidate.amountPaise)
        assertEquals(TransactionDirection.DEBIT, candidate.direction)
        assertEquals("Uber", candidate.merchant)
        assertEquals(TransactionCategory.TRANSPORT, candidate.suggestedCategory)
    }

    // ---------------------------------------------------------
    // 2. Credit & Salary Transactions
    // ---------------------------------------------------------

    @Test
    fun parse_credit_salary_categorizedAsIncome() {
        val payload = RawNotificationPayload(
            packageName = "com.hdfcbank.android",
            postTimeEpochMs = 1713500000000L,
            title = "Salary Alert",
            text = "Salary credited: INR 1,25,000.00 to A/c **4321 on 01-Oct-26",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Salary should be parsed successfully", result is ParserResult.Parsed)
        val candidate = (result as ParserResult.Parsed).candidate

        assertEquals(12500000L, candidate.amountPaise)
        assertEquals(TransactionDirection.CREDIT, candidate.direction)
        assertEquals(TransactionCategory.INCOME, candidate.suggestedCategory)
        assertFalse("Salary is not a refund", candidate.isRefundOrReversal)
    }

    @Test
    fun parse_credit_p2pPayment_extractsCreditDirection() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            postTimeEpochMs = 1713500000000L,
            title = "Money Received",
            text = "Received ₹2,000 from Rahul Sharma via Google Pay",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue(result is ParserResult.Parsed)
        val candidate = (result as ParserResult.Parsed).candidate

        assertEquals(200000L, candidate.amountPaise)
        assertEquals(TransactionDirection.CREDIT, candidate.direction)
        assertEquals("Rahul Sharma", candidate.merchant)
    }

    // ---------------------------------------------------------
    // 3. Amount vs. Balance Protection Tests
    // ---------------------------------------------------------

    @Test
    fun parse_amountWithAvailableBalance_doesNotMistakeBalanceForAmount() {
        // Notification with Rs. 500 spent and Rs. 15,400 balance
        val payload = RawNotificationPayload(
            packageName = "com.sbi.lotusintouch",
            postTimeEpochMs = 1713500000000L,
            title = "SBI Alert",
            text = "Rs. 500.00 debited from your A/c **9876. Avl Bal Rs. 15,400.00",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue(result is ParserResult.Parsed || result is ParserResult.NeedsReview)

        val candidate = when (result) {
            is ParserResult.Parsed -> result.candidate
            is ParserResult.NeedsReview -> result.candidate
            else -> throw AssertionError("Expected parsed or review candidate")
        }

        // Amount MUST be 500.00 (50,000 paise), NEVER the balance 15,400.00 (1,540,000 paise)
        assertEquals("Must extract transaction amount, NOT available balance", 50000L, candidate.amountPaise)
        assertNotEquals(1540000L, candidate.amountPaise)
        assertEquals(TransactionDirection.DEBIT, candidate.direction)
    }

    @Test
    fun parse_inrAmountWithClearBalance_extractsDebitAmountOnly() {
        val payload = RawNotificationPayload(
            packageName = "com.axis.mobile",
            postTimeEpochMs = 1713500000000L,
            title = "Axis Bank",
            text = "Acct **1234 is debited by INR 1,500.00 on 22-Oct. Clear Bal: INR 55,200.00",
            bigText = null
        )

        val result = parser.parse(payload)
        val candidate = when (result) {
            is ParserResult.Parsed -> result.candidate
            is ParserResult.NeedsReview -> result.candidate
            else -> throw AssertionError("Expected parsed candidate")
        }

        assertEquals(150000L, candidate.amountPaise)
        assertNotEquals(5520000L, candidate.amountPaise)
    }

    @Test
    fun parse_balanceOnlyNotification_isRejectedAsNotTransaction() {
        val payload = RawNotificationPayload(
            packageName = "com.hdfcbank.android",
            postTimeEpochMs = 1713500000000L,
            title = "Account Balance",
            text = "Your account balance is ₹10,000.00 as of today.",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Balance-only alert must be rejected", result is ParserResult.NotTransaction)
        assertEquals(NotTransactionReason.BALANCE_INQUIRY_ONLY, (result as ParserResult.NotTransaction).reasonCode)
    }

    // ---------------------------------------------------------
    // 4. Refunds, Reversals, and Failed Transactions
    // ---------------------------------------------------------

    @Test
    fun parse_refund_isMarkedAsRefundAndNeverTreatedAsSalary() {
        val payload = RawNotificationPayload(
            packageName = "com.phonepe.app",
            postTimeEpochMs = 1713500000000L,
            title = "Refund Received",
            text = "Refund of ₹350.00 credited for cancelled order from Swiggy",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Refunds should go to NeedsReview for safety", result is ParserResult.NeedsReview)
        val candidate = (result as ParserResult.NeedsReview).candidate

        assertEquals(35000L, candidate.amountPaise)
        assertEquals(TransactionDirection.CREDIT, candidate.direction)
        assertTrue("Must flag as refund or reversal", candidate.isRefundOrReversal)
        assertNotEquals("Refund MUST NEVER be classified as Salary/INCOME", TransactionCategory.INCOME, candidate.suggestedCategory)
        assertEquals(ReviewReason.REFUND_OR_REVERSAL, result.reasonCode)
    }

    @Test
    fun parse_failedPayment_routesToReviewAndNeverConfirmed() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            postTimeEpochMs = 1713500000000L,
            title = "Payment Failed",
            text = "Payment of ₹500 to Blinkit failed due to network timeout",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Failed payment must never be directly parsed as confirmed", result is ParserResult.NeedsReview)
        val reviewResult = result as ParserResult.NeedsReview

        assertTrue("Candidate must be flagged as isFailed", reviewResult.candidate.isFailed)
        assertEquals(ReviewReason.FAILED_TRANSACTION_ALERT, reviewResult.reasonCode)
        assertTrue("Confidence must be very low for failed alerts", reviewResult.candidate.confidence <= 0.20f)
    }

    // ---------------------------------------------------------
    // 5. Ambiguous Multiple Amounts & Formatting
    // ---------------------------------------------------------

    @Test
    fun parse_multipleAmbiguousAmounts_routesToReview() {
        val payload = RawNotificationPayload(
            packageName = "net.one97.paytm",
            postTimeEpochMs = 1713500000000L,
            title = "Paytm Order",
            text = "Debited ₹500 for order with discount ₹100 and fee ₹50 at Retail Store",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Multiple conflicting amounts must route to NeedsReview", result is ParserResult.NeedsReview)
        assertEquals(ReviewReason.AMBIGUOUS_MULTIPLE_AMOUNTS, (result as ParserResult.NeedsReview).reasonCode)
    }

    // ---------------------------------------------------------
    // 6. Unrelated Notifications Mentioning Money (OTP & Spam)
    // ---------------------------------------------------------

    @Test
    fun parse_otpNotification_isRejectedAsNotTransaction() {
        val payload = RawNotificationPayload(
            packageName = "com.hdfcbank.android",
            postTimeEpochMs = 1713500000000L,
            title = "HDFC Bank NetBanking",
            text = "OTP for transaction of Rs 500 at Flipkart is 458921. Do not share with anyone.",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("OTP message must be dropped", result is ParserResult.NotTransaction)
        assertEquals(NotTransactionReason.AUTH_OR_OTP, (result as ParserResult.NotTransaction).reasonCode)
    }

    @Test
    fun parse_promotionalLoanOffer_isRejectedAsNotTransaction() {
        val payload = RawNotificationPayload(
            packageName = "com.cred.club",
            postTimeEpochMs = 1713500000000L,
            title = "CRED Cash",
            text = "Pre-approved instant personal loan of Rs 5,00,000. Apply now in 2 mins.",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Promotional offer must be dropped", result is ParserResult.NotTransaction)
        assertEquals(NotTransactionReason.OFFER_OR_PROMOTION, (result as ParserResult.NotTransaction).reasonCode)
    }

    @Test
    fun parse_cashbackPromoBanner_isRejectedAsNotTransaction() {
        val payload = RawNotificationPayload(
            packageName = "com.phonepe.app",
            postTimeEpochMs = 1713500000000L,
            title = "Special Offer",
            text = "Get flat discount up to ₹200 on your next grocery order. Hurry!",
            bigText = null
        )

        val result = parser.parse(payload)
        assertTrue("Promotional discount banner must be dropped", result is ParserResult.NotTransaction)
        assertEquals(NotTransactionReason.OFFER_OR_PROMOTION, (result as ParserResult.NotTransaction).reasonCode)
    }

    // ---------------------------------------------------------
    // 7. Multi-Signal Deduplication Tests
    // ---------------------------------------------------------

    @Test
    fun deduplicator_detectsDuplicateWithinTimeWindow() = runTest {
        val fakeDao = FakeTransactionDao()
        val deduplicator = TransactionDeduplicator(fakeDao, windowMs = 600_000L) // 10 mins

        // Existing transaction in DB
        fakeDao.insertTransaction(
            TransactionEntity(
                id = "tx_existing",
                amountPaise = 25000L,
                currencyCode = "INR",
                direction = "DEBIT",
                merchant = "Swiggy",
                note = null,
                categoryId = "FOOD",
                occurredAtEpochMillis = 1000000L,
                sourceType = "NOTIFICATION",
                sourcePackage = "com.google.android.apps.nbu.paisa.user",
                confidence = 0.95f,
                reviewStatus = "CONFIRMED",
                dedupeKey = "fingerprint_123",
                createdAtEpochMillis = 1000000L,
                updatedAtEpochMillis = 1000000L
            )
        )

        // Duplicate alert arriving 2 minutes later (120,000 ms later)
        val duplicateCandidate = TransactionCandidate(
            amountPaise = 25000L,
            currencyCode = "INR",
            direction = TransactionDirection.DEBIT,
            merchant = "Swiggy",
            timestampEpochMs = 1120000L,
            sourcePackage = "com.google.android.apps.nbu.paisa.user",
            confidence = 0.95f,
            dedupeFingerprint = "fingerprint_diff"
        )

        val isDup = deduplicator.isDuplicate(duplicateCandidate)
        assertTrue("Must detect duplicate within 10-minute window", isDup)
    }

    @Test
    fun deduplicator_doesNotDeduplicateSolelyOnAmount() = runTest {
        val fakeDao = FakeTransactionDao()
        val deduplicator = TransactionDeduplicator(fakeDao, windowMs = 600_000L)

        // Existing transaction from Google Pay at Starbucks for ₹150
        fakeDao.insertTransaction(
            TransactionEntity(
                id = "tx_starbucks",
                amountPaise = 15000L,
                currencyCode = "INR",
                direction = "DEBIT",
                merchant = "Starbucks",
                note = null,
                categoryId = "FOOD",
                occurredAtEpochMillis = 1000000L,
                sourceType = "NOTIFICATION",
                sourcePackage = "com.google.android.apps.nbu.paisa.user",
                confidence = 0.95f,
                reviewStatus = "CONFIRMED",
                dedupeKey = "fingerprint_starbucks",
                createdAtEpochMillis = 1000000L,
                updatedAtEpochMillis = 1000000L
            )
        )

        // Distinct transaction from Paytm at Chai Point also for ₹150
        val differentCandidate = TransactionCandidate(
            amountPaise = 15000L, // Same amount!
            currencyCode = "INR",
            direction = TransactionDirection.DEBIT,
            merchant = "Chai Point", // Different merchant!
            timestampEpochMs = 1005000L,
            sourcePackage = "net.one97.paytm", // Different package!
            confidence = 0.95f,
            dedupeFingerprint = "fingerprint_chai"
        )

        val isDup = deduplicator.isDuplicate(differentCandidate)
        assertFalse("Never deduplicate solely on amount when package/merchant differ", isDup)
    }

    // ---------------------------------------------------------
    // 8. Ingestion Engine End-to-End Persistence Test
    // ---------------------------------------------------------

    @Test
    fun ingestionEngine_routesConfirmedAndReviewCorrectly() = runTest {
        val fakeDao = FakeTransactionDao()
        val ingestionEngine = NotificationIngestionEngine(
            transactionDao = fakeDao,
            parser = parser,
            deduplicator = TransactionDeduplicator(fakeDao)
        )

        // 1. High-confidence clear transaction
        val payloadConfirmed = RawNotificationPayload(
            packageName = "com.google.android.apps.nbu.paisa.user",
            postTimeEpochMs = 1000000L,
            title = "Paid to Swiggy",
            text = "₹350.00 debited from A/C 1234 using UPI",
            bigText = null
        )
        ingestionEngine.onNotificationReceived(payloadConfirmed)

        assertEquals("Confirmed transaction must be saved", 1, fakeDao.transactions.size)
        val firstSaved = fakeDao.transactions.first()
        assertEquals("CONFIRMED", firstSaved.reviewStatus)
        assertEquals(35000L, firstSaved.amountPaise)
        assertEquals("Swiggy", firstSaved.merchant)

        // 2. Failed payment alert -> Must be routed to NEEDS_REVIEW
        val payloadFailed = RawNotificationPayload(
            packageName = "com.phonepe.app",
            postTimeEpochMs = 2000000L,
            title = "Payment Failed",
            text = "Payment of ₹800 to Nike failed",
            bigText = null
        )
        ingestionEngine.onNotificationReceived(payloadFailed)

        assertEquals(2, fakeDao.transactions.size)
        val secondSaved = fakeDao.transactions.last()
        assertEquals("NEEDS_REVIEW", secondSaved.reviewStatus)
        assertTrue(secondSaved.note?.contains("Failed") == true)
    }

    // ---------------------------------------------------------
    // Fake DAO implementation for isolated pure JVM unit testing
    // ---------------------------------------------------------

    private class FakeTransactionDao : TransactionDao {
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

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactions.removeAll { it.id == transaction.id }
            transactions.add(transaction)
        }

        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            this.transactions.addAll(transactions)
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
            flowOf(transactions.filter { it.direction == "DEBIT" && it.reviewStatus == "CONFIRMED" && it.categoryId !in listOf("TRANSFER", "TRANSFERS") && it.occurredAtEpochMillis in startOfDay..endOfDay }.sumOf { it.amountPaise })

        override fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "CREDIT" && it.reviewStatus == "CONFIRMED" && it.categoryId !in listOf("TRANSFER", "TRANSFERS") && it.occurredAtEpochMillis in startOfDay..endOfDay }.sumOf { it.amountPaise })

        override fun getPeriodDebitsPaise(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter { it.direction == "DEBIT" && it.reviewStatus == "CONFIRMED" && it.categoryId !in listOf("TRANSFER", "TRANSFERS") && it.occurredAtEpochMillis >= startOfPeriod }.sumOf { it.amountPaise })

        override suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter { it.direction == "DEBIT" && it.reviewStatus == "CONFIRMED" && it.categoryId !in listOf("TRANSFER", "TRANSFERS") && it.occurredAtEpochMillis in startTime..endTime }.sumOf { it.amountPaise }

        override suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long =
            transactions.filter { it.direction == "CREDIT" && it.reviewStatus == "CONFIRMED" && it.categoryId !in listOf("TRANSFER", "TRANSFERS") && it.occurredAtEpochMillis in startTime..endTime }.sumOf { it.amountPaise }

        override fun getPeriodIncomeCredits(startOfPeriod: Long): Flow<Long> =
            flowOf(transactions.filter { it.categoryId == "INCOME" && it.reviewStatus == "CONFIRMED" && it.occurredAtEpochMillis >= startOfPeriod }.sumOf { it.amountPaise })

        override fun getPeriodRefundCredits(startOfPeriod: Long): Flow<Long> =
            flowOf(0L)

        override fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<CategorySpending>> =
            flowOf(emptyList())

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

        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter {
                (it.merchant?.contains(query, ignoreCase = true) == true) ||
                (it.note?.contains(query, ignoreCase = true) == true) ||
                it.categoryId.contains(query, ignoreCase = true)
            })

        override fun getTransactionsByDirection(direction: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.direction == direction })

        override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.categoryId == categoryId })

        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.occurredAtEpochMillis in startTime..endTime })

        override fun getTransactionsForBank(bankAccountName: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.bankAccountName == bankAccountName || it.bankAccountId == bankAccountName })
    }
}
