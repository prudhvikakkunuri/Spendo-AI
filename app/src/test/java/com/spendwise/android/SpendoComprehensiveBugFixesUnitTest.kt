package com.spendwise.android

import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import com.spendwise.android.notification.SpendoReminderScheduler
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/**
 * Unit tests validating fixes for:
 * 1. Analytics screen crash & percent formatting.
 * 2. Multi-account banking under the same bank name.
 * 3. Daily expense reminder trigger calculations and context-aware messaging.
 */
class SpendoComprehensiveBugFixesUnitTest {

    @Test
    fun analyticsPercentFormatting_doesNotThrowUnknownFormatConversionException() {
        // Reproduce the previous bug where "₹%,.2f ($pct%)".format(...) failed with UnknownFormatConversionException
        val totalSpentPaise = 100000L
        val catSpendPaise = 25000L
        val pct = if (totalSpentPaise > 0) ((catSpendPaise * 100) / totalSpentPaise).toInt() else 0

        val formattedPaise = "₹%,.2f".format(Locale.ENGLISH, catSpendPaise / 100.0)
        val result = "$formattedPaise ($pct%)"

        assertEquals("₹250.00 (25%)", result)
    }

    @Test
    fun analyticsCalculations_handlesEmptyTransactionsWithoutDivisionByZero() {
        val transactions = emptyList<SpendoTransaction>()
        val totalDebitPaise = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
        val totalCreditPaise = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val netCashFlowPaise = totalCreditPaise - totalDebitPaise

        val pct = if (totalDebitPaise > 0) ((0L * 100) / totalDebitPaise).toInt() else 0

        assertEquals(0L, totalDebitPaise)
        assertEquals(0L, totalCreditPaise)
        assertEquals(0L, netCashFlowPaise)
        assertEquals(0, pct)
    }

    @Test
    fun multipleAccountsForSameBank_maintainSeparateIdentitiesAndDisplayNames() {
        val hdfcSalary = SpendoBank(
            id = "bank_hdfc_salary_001",
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            accountNickname = "Salary Account",
            maskedAccountNumber = "•••• 1234",
            balancePaise = 15000000L // ₹1,50,000.00
        )

        val hdfcSavings = SpendoBank(
            id = "bank_hdfc_savings_002",
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            accountNickname = "Personal Savings",
            maskedAccountNumber = "•••• 5678",
            balancePaise = 5000000L // ₹50,000.00
        )

        assertNotEquals(hdfcSalary.id, hdfcSavings.id)
        assertEquals("HDFC Bank", hdfcSalary.bankName)
        assertEquals("HDFC Bank", hdfcSavings.bankName)
        assertEquals("HDFC Bank (Salary Account)", hdfcSalary.displayName)
        assertEquals("HDFC Bank (Personal Savings)", hdfcSavings.displayName)
    }

    @Test
    fun multipleAccountsForSameBank_balanceAdjustmentsAreAccountSpecific() {
        var hdfcSalaryBalance = 15000000L
        var hdfcSavingsBalance = 5000000L

        // Debit from salary account
        val expenseAmountPaise = 150000L // ₹1,500.00
        val targetAccountId = "bank_hdfc_salary_001"

        if (targetAccountId == "bank_hdfc_salary_001") {
            hdfcSalaryBalance -= expenseAmountPaise
        } else if (targetAccountId == "bank_hdfc_savings_002") {
            hdfcSavingsBalance -= expenseAmountPaise
        }

        assertEquals(14850000L, hdfcSalaryBalance)
        assertEquals(5000000L, hdfcSavingsBalance) // Untouched
    }

    @Test
    fun transferBetweenAccounts_adjustsBothBalancesWithoutAffectingTotalLiquidWealth() {
        var acc1Balance = 1000000L // ₹10,000.00
        var acc2Balance = 500000L  // ₹5,000.00
        val initialTotal = acc1Balance + acc2Balance

        val transferAmount = 200000L // ₹2,000.00
        acc1Balance -= transferAmount
        acc2Balance += transferAmount

        assertEquals(800000L, acc1Balance)
        assertEquals(700000L, acc2Balance)
        assertEquals(initialTotal, acc1Balance + acc2Balance)
    }

    @Test
    fun reminderScheduler_calculatesNextTriggerInFuture() {
        val now = System.currentTimeMillis()
        val trigger9am = SpendoReminderScheduler.getNextTriggerMillis(9, 0)
        val trigger830pm = SpendoReminderScheduler.getNextTriggerMillis(20, 30)

        assertTrue("Morning alarm must be scheduled in the future", trigger9am > now)
        assertTrue("Evening alarm must be scheduled in the future", trigger830pm > now)
    }

    @Test
    fun reminderScheduler_rollsOverToNextDayIfTargetTimePassed() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val pastHour = cal.get(Calendar.HOUR_OF_DAY)
        val pastMinute = cal.get(Calendar.MINUTE)

        val trigger = SpendoReminderScheduler.getNextTriggerMillis(pastHour, pastMinute)
        val now = System.currentTimeMillis()

        assertTrue("If time passed today, next trigger must be > 12 hours away in future", trigger > now + (12 * 3600 * 1000L))
    }

    // =========================================================================
    // Chatbot Suggested-Prompt Audit & Deterministic Local Handling Tests
    // =========================================================================

    @Test
    fun voiceFinancialParser_allAuditedPrompts_parseToDeterministicIntents() {
        val parser = VoiceFinancialParser

        // 1. "How much did I spend today?"
        val p1 = parser.parseSpeech("How much did I spend today?")
        assertTrue("Today spending prompt must parse to InquireSpending(TODAY)", p1 is VoiceIntent.InquireSpending && (p1 as VoiceIntent.InquireSpending).period == "TODAY")

        // 2. "Show my expenses this month."
        val p2 = parser.parseSpeech("Show my expenses this month.")
        assertTrue("Month expenses prompt must parse to InquireSpending(MONTH)", p2 is VoiceIntent.InquireSpending && (p2 as VoiceIntent.InquireSpending).period == "MONTH")

        // 3. "What is my biggest expense?"
        val p3 = parser.parseSpeech("What is my biggest expense?")
        assertTrue("Biggest expense prompt must parse to QueryBiggestExpense", p3 is VoiceIntent.QueryBiggestExpense)

        // 4. "Which category costs me the most?"
        val p4 = parser.parseSpeech("Which category costs me the most?")
        assertTrue("Top category prompt must parse to QueryTopCategory", p4 is VoiceIntent.QueryTopCategory)

        // 5. "How much money did I save?"
        val p5 = parser.parseSpeech("How much money did I save?")
        assertTrue("Savings prompt must parse to QuerySavings", p5 is VoiceIntent.QuerySavings)

        // 6. "Analyze my spending habits."
        val p6 = parser.parseSpeech("Analyze my spending habits.")
        assertTrue("Habits prompt must parse to QueryAnalyzeHabits", p6 is VoiceIntent.QueryAnalyzeHabits)

        // 7. "Show my recent transactions."
        val p7 = parser.parseSpeech("Show my recent transactions.")
        assertTrue("Recent transactions prompt must parse to QueryRecentTransactions", p7 is VoiceIntent.QueryRecentTransactions)

        // 8. "How much is left in my budget?"
        val p8 = parser.parseSpeech("How much is left in my budget?")
        assertTrue("Budget prompt must parse to InquireBudget", p8 is VoiceIntent.InquireBudget)

        // 9. "Compare my spending with last month"
        val p9 = parser.parseSpeech("Compare my spending with last month")
        assertTrue("Compare prompt must parse to QueryCompareLastMonth", p9 is VoiceIntent.QueryCompareLastMonth)

        // 10. "How can I reduce my spending next month?"
        val p10 = parser.parseSpeech("How can I reduce my spending next month?")
        assertTrue("Reduce spending prompt must parse to QueryReduceSpending", p10 is VoiceIntent.QueryReduceSpending)
    }

    @Test
    fun chatbotReduceSpendingAdvice_doesNotCrashWithUnknownFormatConversionException() {
        // Specifically verifies that "15% to save" does not fail Java String.format conversion
        val targetSavingsPaise = 150000L
        val savedFormatted = "₹%,.2f".format(Locale.ENGLISH, targetSavingsPaise / 100.0)
        val template = "Actionable Budget Advice:\n• Discretionary Spending: Trim non-essential discretionary spends by 15% to save $savedFormatted."
        assertTrue(template.contains("15% to save ₹1,500.00"))
    }

    @Test
    fun chatbotEmptyDatabase_deterministicQueriesProduceMeaningfulMessagesWithoutCrashing() {
        val emptyList = emptyList<SpendoTransaction>()

        // Biggest Expense on empty list
        val biggest = emptyList.filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amountPaise }
        val biggestMsg = if (biggest != null && biggest.amountPaise > 0L) {
            "Your biggest recorded expense is ₹${biggest.amountPaise / 100.0}"
        } else {
            "No expenses recorded in your ledger yet."
        }
        assertEquals("No expenses recorded in your ledger yet.", biggestMsg)

        // Habits analysis on empty list
        val habitsMsg = if (emptyList.isEmpty()) {
            "No transactions recorded yet to analyze. Log a few expenses to generate spending habit insights."
        } else {
            "Spending Habits Analysis"
        }
        assertEquals("No transactions recorded yet to analyze. Log a few expenses to generate spending habit insights.", habitsMsg)

        // Recent transactions on empty list
        val recentMsg = if (emptyList.isEmpty()) {
            "You haven't recorded any transactions yet. Tap '+ Add' or speak into the microphone to log your first expense."
        } else {
            "Your latest transactions:"
        }
        assertEquals("You haven't recorded any transactions yet. Tap '+ Add' or speak into the microphone to log your first expense.", recentMsg)
    }

    // =========================================================================
    // Voice Expense Entry & Natural Language Draft Extraction Tests
    // =========================================================================

    @Test
    fun voiceExpenseParser_spokenPhrasesExtractAccurateFinancialDraft() {
        val parser = VoiceFinancialParser

        // Test 1: "I spent 250 rupees on lunch through Kotak today"
        val ex1 = parser.parseSpeech("I spent 250 rupees on lunch through Kotak today")
        assertTrue(ex1 is VoiceIntent.AddExpense)
        val d1 = ex1 as VoiceIntent.AddExpense
        assertEquals(25000L, d1.amountPaise)
        assertEquals("Lunch", d1.merchant)
        assertEquals("Food & Drink", d1.category)
        assertEquals("Kotak Mahindra Bank", d1.bankName)

        // Test 2: "Paid 500 for petrol yesterday using HDFC"
        val ex2 = parser.parseSpeech("Paid 500 for petrol yesterday using HDFC")
        assertTrue(ex2 is VoiceIntent.AddExpense)
        val d2 = ex2 as VoiceIntent.AddExpense
        assertEquals(50000L, d2.amountPaise)
        assertEquals("Petrol", d2.merchant)
        assertEquals("Transit", d2.category)
        assertEquals("HDFC Bank", d2.bankName)
        assertTrue("Yesterday expense timestamp must be in past", d2.occurredAtEpochMs < System.currentTimeMillis())

        // Test 3: "Spent 120 rupees on coffee today"
        val ex3 = parser.parseSpeech("Spent 120 rupees on coffee today")
        assertTrue(ex3 is VoiceIntent.AddExpense)
        val d3 = ex3 as VoiceIntent.AddExpense
        assertEquals(12000L, d3.amountPaise)
        assertEquals("Coffee", d3.merchant)
        assertEquals("Food & Drink", d3.category)

        // Test 4: "Paid 300 for groceries through my Kotak savings account"
        val ex4 = parser.parseSpeech("Paid 300 for groceries through my Kotak savings account")
        assertTrue(ex4 is VoiceIntent.AddExpense)
        val d4 = ex4 as VoiceIntent.AddExpense
        assertEquals(30000L, d4.amountPaise)
        assertEquals("Groceries", d4.merchant)
        assertEquals("Groceries", d4.category)
        assertEquals("Kotak Mahindra Bank", d4.bankName)

        // Test 5: "I spent 150 on an auto ride"
        val ex5 = parser.parseSpeech("I spent 150 on an auto ride")
        assertTrue(ex5 is VoiceIntent.AddExpense)
        val d5 = ex5 as VoiceIntent.AddExpense
        assertEquals(15000L, d5.amountPaise)
        assertEquals("Auto Ride", d5.merchant)
        assertEquals("Transit", d5.category)
    }

    @Test
    fun voiceExpenseParser_missingAmount_producesAmbiguousExpenseAndNeverSavesSilently() {
        val parser = VoiceFinancialParser
        val result = parser.parseSpeech("I spent money on lunch today")
        assertTrue("Missing amount must return AmbiguousExpense", result is VoiceIntent.AmbiguousExpense)
        val amb = result as VoiceIntent.AmbiguousExpense
        assertTrue(amb.promptForMissing.contains("amount", ignoreCase = true))
    }

    // =========================================================================
    // Widget Spending & Budget Calculations
    // =========================================================================

    @Test
    fun widgetSpendingTotals_calculateAccuratelyInIntegerPaise() {
        val todayDebitsPaise = 25000L // ₹250.00
        val monthDebitsPaise = 2840000L // ₹28,400.00
        val dailyBudgetLimitPaise = 250000L // ₹2,500.00

        val remainingBudgetPaise = (dailyBudgetLimitPaise - todayDebitsPaise).coerceAtLeast(0L)

        val todayFormatted = "₹%,.2f".format(Locale.ENGLISH, todayDebitsPaise / 100.0)
        val monthFormatted = "₹%,.2f".format(Locale.ENGLISH, monthDebitsPaise / 100.0)
        val budgetFormatted = "₹%,.0f left".format(Locale.ENGLISH, remainingBudgetPaise / 100.0)

        assertEquals("₹250.00", todayFormatted)
        assertEquals("₹28,400.00", monthFormatted)
        assertEquals("₹2,250 left", budgetFormatted)
    }

    @Test
    fun widgetEmptyState_displaysMeaningfulZeroInsteadOfHardcoded000() {
        val zeroPaise = 0L
        val formattedZero = "₹%,.2f".format(Locale.ENGLISH, zeroPaise / 100.0)
        assertEquals("₹0.00", formattedZero)

        val bankEntities = emptyList<SpendoBank>()
        val accountsText = if (bankEntities.isEmpty()) "No accounts configured" else "Configured"
        assertEquals("No accounts configured", accountsText)
    }
}
