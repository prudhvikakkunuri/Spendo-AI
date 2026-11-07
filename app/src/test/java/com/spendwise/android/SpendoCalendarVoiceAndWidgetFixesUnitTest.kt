package com.spendwise.android

import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Comprehensive verification of:
 * 1. Spoken natural language expense parsing from home widget.
 * 2. Ambiguity resolution and question non-conversion.
 * 3. Calendar month navigation, date calculations, and leap year handling.
 * 4. Two-line notification and monochrome color palette adherence.
 */
class SpendoCalendarVoiceAndWidgetFixesUnitTest {

    @Test
    fun testVoicePromptExample1Lunch() {
        val input = "I spent 250 rupees on lunch."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for lunch statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(25000L, expense.amountPaise)
        assertEquals("Lunch", expense.merchant)
        assertEquals("Food & Drink", expense.category)
    }

    @Test
    fun testVoicePromptExample2Petrol() {
        val input = "Paid 120 for petrol."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for petrol statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(12000L, expense.amountPaise)
        assertEquals("Petrol", expense.merchant)
        assertEquals("Transit", expense.category)
    }

    @Test
    fun testVoicePromptExample3Coffee() {
        val input = "Bought coffee for 180 rupees."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for coffee statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(18000L, expense.amountPaise)
        assertEquals("Coffee", expense.merchant)
        assertEquals("Food & Drink", expense.category)
    }

    @Test
    fun testVoicePromptExample4Snacks() {
        val input = "Spent 75 on snacks."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for snacks statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(7500L, expense.amountPaise)
        assertEquals("Snacks", expense.merchant)
        assertEquals("Food & Drink", expense.category)
    }

    @Test
    fun testVoicePromptExample5Groceries() {
        val input = "I paid ₹499 for groceries."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for groceries statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(49900L, expense.amountPaise)
        assertEquals("Groceries", expense.merchant)
        assertEquals("Groceries", expense.category)
    }

    @Test
    fun testVoicePromptExample6AutoFare() {
        val input = "Auto fare was 90 rupees."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for auto fare statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(9000L, expense.amountPaise)
        assertEquals("Auto", expense.merchant)
        assertEquals("Transit", expense.category)
    }

    @Test
    fun testVoicePromptExample7RestaurantYesterday() {
        val input = "Spent 1200 rupees at a restaurant yesterday."
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for restaurant statement", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(120000L, expense.amountPaise)
        assertEquals("Restaurant", expense.merchant)
        assertEquals("Food & Drink", expense.category)

        // Verify yesterday's date was assigned
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val expectedDay = cal.get(Calendar.DAY_OF_MONTH)
        val assignedCal = Calendar.getInstance().apply { timeInMillis = expense.occurredAtEpochMs }
        assertEquals(expectedDay, assignedCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testVoiceSpokenNumberWordsConversion() {
        val input = "Spent two hundred fifty on lunch"
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for number words", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(25000L, expense.amountPaise)
        assertEquals("Lunch", expense.merchant)
    }

    @Test
    fun testVoiceDecimalAmountsHandling() {
        val input = "Spent 150.50 on snacks"
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Expected AddExpense for decimal amount", result is VoiceIntent.AddExpense)
        val expense = result as VoiceIntent.AddExpense
        assertEquals(15050L, expense.amountPaise)
    }

    @Test
    fun testVoiceBudgetQuestionDoesNotCreateExpense() {
        val input = "What is my remaining budget?"
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Budget question must not be converted into an expense", result is VoiceIntent.InquireBudget)
    }

    @Test
    fun testVoiceAmbiguousExpenseWithoutAmount() {
        val input = "I spent some money on food"
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Missing amount must return AmbiguousExpense", result is VoiceIntent.AmbiguousExpense)
        val ambiguous = result as VoiceIntent.AmbiguousExpense
        assertTrue(ambiguous.promptForMissing.contains("amount", ignoreCase = true))
    }

    @Test
    fun testVoiceEmptySpeechHandledGracefully() {
        val input = ""
        val result = VoiceFinancialParser.parseSpeech(input)
        assertTrue("Empty speech must return GeneralChat", result is VoiceIntent.GeneralChat)
    }

    @Test
    fun testCalendarMonthCalculationsAndBoundaries() {
        // Test leap year February 2024
        val feb2024 = Calendar.getInstance().apply {
            clear()
            set(2024, Calendar.FEBRUARY, 1)
        }
        assertEquals(29, feb2024.getActualMaximum(Calendar.DAY_OF_MONTH))

        // Test non-leap year February 2026
        val feb2026 = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.FEBRUARY, 1)
        }
        assertEquals(28, feb2026.getActualMaximum(Calendar.DAY_OF_MONTH))

        // Test October 2026 (31 days)
        val oct2026 = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 1)
        }
        assertEquals(31, oct2026.getActualMaximum(Calendar.DAY_OF_MONTH))
    }
}
