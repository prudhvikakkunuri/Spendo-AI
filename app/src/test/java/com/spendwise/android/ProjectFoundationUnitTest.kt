package com.spendwise.android

import com.spendwise.android.domain.model.ReviewStatus
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Phase 0 Project Foundation and non-negotiable rules:
 * - Money is formatted using integer paise (Long) without floating-point arithmetic.
 * - Architecture models and package structure are intact.
 */
class ProjectFoundationUnitTest {

    @Test
    fun currencyFormatter_formatsZeroPaiseCorrectly() {
        val result = CurrencyFormatter.formatPaise(0L)
        assertEquals("₹0.00", result)
    }

    @Test
    fun currencyFormatter_formatsStandardAmountsCorrectly() {
        assertEquals("₹1.00", CurrencyFormatter.formatPaise(100L))
        assertEquals("₹350.50", CurrencyFormatter.formatPaise(35050L))
        assertEquals("₹0.05", CurrencyFormatter.formatPaise(5L))
        assertEquals("-₹50.00", CurrencyFormatter.formatPaise(-5000L))
    }

    @Test
    fun currencyFormatter_formatsIndianNumberGroupingCorrectly() {
        // 1,00,000 INR (10,000,000 paise)
        val oneLakhPaise = 10_000_000L
        assertEquals("₹1,00,000.00", CurrencyFormatter.formatPaise(oneLakhPaise))

        // 12,34,567.89 INR (123,456,789 paise)
        val largeAmount = 123_456_789L
        assertEquals("₹12,34,567.89", CurrencyFormatter.formatPaise(largeAmount))
    }

    @Test
    fun domainEnums_haveExpectedValues() {
        assertEquals(2, TransactionDirection.values().size)
        assertNotNull(TransactionDirection.valueOf("DEBIT"))
        assertNotNull(TransactionDirection.valueOf("CREDIT"))

        assertEquals(3, ReviewStatus.values().size)
        assertNotNull(ReviewStatus.valueOf("CONFIRMED"))
        assertNotNull(ReviewStatus.valueOf("NEEDS_REVIEW"))
        assertNotNull(ReviewStatus.valueOf("IGNORED"))

        assertEquals(2, SourceType.values().size)
        assertNotNull(SourceType.valueOf("MANUAL"))
        assertNotNull(SourceType.valueOf("NOTIFICATION"))

        assertTrue(TransactionCategory.values().isNotEmpty())
    }
}
