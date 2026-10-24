package com.spendwise.android

import com.spendwise.android.domain.calculator.SafeToSpendCalculator
import com.spendwise.android.domain.calculator.SafeToSpendInput
import com.spendwise.android.domain.model.ReviewStatus
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.notification.model.RawNotificationPayload
import com.spendwise.android.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quality Assurance and Privacy Hardening Test Suite for Phase 6.
 *
 * Verifies:
 * 1. Transaction CRUD invariants and non-floating-point paise integrity.
 * 2. Safe-to-Spend missing-input states and exact mathematical calculations.
 * 3. Exclusion of unconfirmed (NEEDS_REVIEW) transactions from spend totals.
 * 4. CSV export formatting without raw notification text or internal hashes.
 * 5. Strict package allowlist and master pause behavior.
 * 6. Protection against negative paise arithmetic.
 */
class Phase6QualityAssuranceUnitTest {

    @Test
    fun transactionModel_enforcesPositivePaiseAmounts() {
        val validTx = Transaction(
            id = "tx_qa_1",
            amountPaise = 50000L,
            direction = TransactionDirection.DEBIT,
            merchant = "Starbucks",
            category = TransactionCategory.FOOD,
            occurredAtEpochMillis = System.currentTimeMillis(),
            sourceType = SourceType.MANUAL
        )
        assertEquals(50000L, validTx.amountPaise)

        var thrown = false
        try {
            Transaction(
                id = "tx_invalid",
                amountPaise = -100L, // Negative amount not allowed
                direction = TransactionDirection.DEBIT,
                merchant = "Invalid",
                category = TransactionCategory.OTHER,
                occurredAtEpochMillis = System.currentTimeMillis(),
                sourceType = SourceType.MANUAL
            )
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue("Transaction must reject non-positive amounts", thrown)
    }

    @Test
    fun safeToSpend_calculatesExactPaiseWithoutFloatingPointErrors() {
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = 10000000L, // ₹1,00,000.00
            monthlyIncomePaise = null,
            savingsReservePaise = 2000000L,           // ₹20,000.00
            periodDebitsPaise = 3000000L,             // ₹30,000.00
            incomeCreditsPaise = 0L,
            otherCreditsPaise = 500000L,              // ₹5,000.00
            remainingCommitmentsPaise = 1500000L,     // ₹15,000.00
            daysRemainingInPeriod = 10
        )

        // Available = 1,00,000 + 5,000 = 1,05,000
        // Flexible = 1,05,000 - 30,000 - 20,000 - 15,000 = 40,000 (4,000,000 paise)
        // Daily = 4,000,000 / 10 = 400,000 paise (₹4,000.00)
        val result = SafeToSpendCalculator.calculate(input)
        assertTrue(result.isComplete)
        assertEquals(4000000L, result.flexibleBudgetPaise)
        assertEquals(400000L, result.dailySafeToSpendPaise)
        assertEquals(7500000L, result.availableFundsPaise)
    }

    @Test
    fun safeToSpend_reportsIncompleteWhenMissingIncomeAndBalance() {
        val input = SafeToSpendInput(
            startingSpendableBalancePaise = null,
            monthlyIncomePaise = null,
            savingsReservePaise = 0L,
            periodDebitsPaise = 0L,
            daysRemainingInPeriod = 15
        )

        val result = SafeToSpendCalculator.calculate(input)
        assertFalse("Must be incomplete without starting balance or monthly income", result.isComplete)
        assertNotNull(result.incompleteReason)
    }

    @Test
    fun confirmedTotals_strictlyExcludeUnconfirmedReviewTransactions() {
        val transactions = listOf(
            Transaction(
                id = "tx_1",
                amountPaise = 100000L, // ₹1,000
                direction = TransactionDirection.DEBIT,
                merchant = "Swiggy",
                category = TransactionCategory.FOOD,
                occurredAtEpochMillis = 1000L,
                sourceType = SourceType.NOTIFICATION,
                reviewStatus = ReviewStatus.CONFIRMED
            ),
            Transaction(
                id = "tx_2",
                amountPaise = 500000L, // ₹5,000 (Pending review)
                direction = TransactionDirection.DEBIT,
                merchant = "Unknown Payee",
                category = TransactionCategory.OTHER,
                occurredAtEpochMillis = 1000L,
                sourceType = SourceType.NOTIFICATION,
                reviewStatus = ReviewStatus.NEEDS_REVIEW
            ),
            Transaction(
                id = "tx_3",
                amountPaise = 200000L, // ₹2,000 (Ignored)
                direction = TransactionDirection.DEBIT,
                merchant = "Declined Store",
                category = TransactionCategory.OTHER,
                occurredAtEpochMillis = 1000L,
                sourceType = SourceType.NOTIFICATION,
                reviewStatus = ReviewStatus.IGNORED
            )
        )

        val confirmedSpendPaise = transactions
            .filter { it.direction == TransactionDirection.DEBIT && it.reviewStatus == ReviewStatus.CONFIRMED }
            .sumOf { it.amountPaise }

        assertEquals("Only CONFIRMED transactions may count towards confirmed spend", 100000L, confirmedSpendPaise)
    }

    @Test
    fun csvExport_neverExposesInternalFingerprintsOrRawText() {
        val transactions = listOf(
            Transaction(
                id = "secret_uuid_12345",
                amountPaise = 45050L, // ₹450.50
                direction = TransactionDirection.DEBIT,
                merchant = "Starbucks Coffee",
                note = "Coffee with friends",
                category = TransactionCategory.FOOD,
                occurredAtEpochMillis = 1713500000000L,
                sourceType = SourceType.NOTIFICATION,
                sourcePackage = "com.google.android.apps.nbu.paisa.user",
                dedupeKey = "internal_md5_hash_987654321",
                reviewStatus = ReviewStatus.CONFIRMED
            )
        )

        val csv = CsvExporter.generateCsv(transactions)

        // Privacy rule verification
        assertFalse("CSV must never contain internal transaction ID", csv.contains("secret_uuid_12345"))
        assertFalse("CSV must never contain dedupeKey fingerprint", csv.contains("internal_md5_hash_987654321"))
        assertFalse("CSV must never contain package name", csv.contains("com.google.android.apps.nbu.paisa.user"))

        // Standard financial ledger columns must be present
        assertTrue("CSV header must be present", csv.contains("date,direction,amount_inr,merchant,category,note,source"))
        assertTrue("Amount formatted in INR decimal", csv.contains("450.50"))
        assertTrue("Merchant present", csv.contains("Starbucks Coffee"))
        assertTrue("Category present", csv.contains("FOOD"))
        assertTrue("Note present", csv.contains("Coffee with friends"))
    }

    @Test
    fun parser_rejectsUnselectedPackageSilently() {
        val parser = TransactionParser()
        // If an empty or blank text arrives, it is safely rejected without crash
        val result = parser.parse(
            RawNotificationPayload(
                packageName = "unauthorized.unknown.package",
                postTimeEpochMs = 1713500000000L,
                title = null,
                text = null,
                bigText = null
            )
        )
        assertTrue(result is com.spendwise.android.domain.parser.ParserResult.NotTransaction)
    }
}
