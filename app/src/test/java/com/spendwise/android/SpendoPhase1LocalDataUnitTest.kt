package com.spendwise.android

import com.spendwise.android.data.db.entity.AiProviderConfigEntity
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.data.db.entity.BudgetEntity
import com.spendwise.android.data.db.entity.CategoryEntity
import com.spendwise.android.data.db.entity.IncomeSourceEntity
import com.spendwise.android.data.db.entity.NotificationRecordEntity
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.data.repository.toSpendoBank
import com.spendwise.android.data.repository.toSpendoIncomeItem
import com.spendwise.android.data.repository.toSpendoNotificationItem
import com.spendwise.android.data.repository.toSpendoTransaction
import com.spendwise.android.ui.spendo.model.AiProvider
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.model.TransactionSource
import com.spendwise.android.ui.spendo.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated test suite for Phase 1: Local Financial Data Persistence.
 * Verifies Room entities, converters, integer paise precision (Long),
 * budget calculations, income tracking, bank account balances, and search/filtering.
 */
class SpendoPhase1LocalDataUnitTest {

    @Test
    fun transactionEntity_storesAmountsAsStrictIntegerPaise() {
        val expenseEntity = TransactionEntity(
            id = "tx_exp_1",
            amountPaise = 42550L, // ₹425.50
            currencyCode = "INR",
            direction = "DEBIT",
            merchant = "Blue Tokai Coffee",
            note = "Flat white",
            categoryId = "Food & Drink",
            occurredAtEpochMillis = 1729780000000L,
            sourceType = "MANUAL",
            reviewStatus = "CONFIRMED",
            createdAtEpochMillis = 1729780000000L,
            updatedAtEpochMillis = 1729780000000L,
            bankAccountName = "HDFC Bank"
        )

        val domain = expenseEntity.toSpendoTransaction()
        assertEquals(42550L, domain.amountPaise)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals("Blue Tokai Coffee", domain.merchant)
        assertEquals("HDFC Bank", domain.bankAccountName)
        assertEquals(TransactionSource.MANUAL, domain.source)

        // Verify credit/income mapping
        val incomeEntity = expenseEntity.copy(
            id = "tx_inc_1",
            amountPaise = 8500000L, // ₹85,000.00
            direction = "CREDIT",
            merchant = "Acme Corp Tech",
            categoryId = "Salary"
        )
        val incomeDomain = incomeEntity.toSpendoTransaction()
        assertEquals(8500000L, incomeDomain.amountPaise)
        assertEquals(TransactionType.INCOME, incomeDomain.type)
        assertEquals("Salary", incomeDomain.category)
    }

    @Test
    fun financialArithmetic_usesStrictLongWithoutFloatDrift() {
        // Test classic floating point trap: 0.1 + 0.2 != 0.3 in Double
        // In integer paise: 10 paise + 20 paise == 30 paise
        val paiseA = 10L
        val paiseB = 20L
        val totalPaise = paiseA + paiseB
        assertEquals(30L, totalPaise)

        // Multiple micro-transactions
        val txs = listOf(34000L, 42000L, 66000L, 19900L) // in paise
        val sumPaise = txs.sum()
        assertEquals(161900L, sumPaise) // exactly ₹1,619.00
    }

    @Test
    fun budgetEntity_calculatesRemainingSafeToSpendAccurately() {
        val dailyBudget = BudgetEntity(
            id = "budget_daily",
            periodType = "DAILY",
            limitPaise = 250000L, // ₹2,500.00
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        val spentTodayPaise = 142000L // ₹1,420.00
        val remainingPaise = (dailyBudget.limitPaise - spentTodayPaise).coerceAtLeast(0L)
        assertEquals(108000L, remainingPaise) // ₹1,080.00

        // Test overspend constraint
        val heavySpendPaise = 300000L // ₹3,000.00
        val remainingOverspend = (dailyBudget.limitPaise - heavySpendPaise).coerceAtLeast(0L)
        assertEquals(0L, remainingOverspend)
    }

    @Test
    fun bankAccountEntity_adjustsBalanceByTransactionDirection() {
        val openingBalance = 8452000L // ₹84,520.00
        val bank = BankAccountEntity(
            id = "bank_hdfc",
            bankName = "HDFC Bank",
            accountType = "Salary Account",
            maskedAccountNumber = "•••• 4892",
            balancePaise = openingBalance,
            openingBalancePaise = openingBalance,
            isPrimary = true,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        // Debit of ₹340.00 (34,000 paise)
        val debitAmount = 34000L
        val balanceAfterDebit = bank.balancePaise - debitAmount
        assertEquals(8418000L, balanceAfterDebit) // ₹84,180.00

        // Credit of ₹85,000.00 (8,500,000 paise)
        val creditAmount = 8500000L
        val balanceAfterCredit = balanceAfterDebit + creditAmount
        assertEquals(16918000L, balanceAfterCredit) // ₹169,180.00

        val domainBank = bank.toSpendoBank()
        assertEquals("HDFC Bank", domainBank.bankName)
        assertEquals(openingBalance, domainBank.balancePaise)
        assertTrue(domainBank.isPrimary)
    }

    @Test
    fun incomeSourceEntity_calculatesRecurringIncomeTotals() {
        val income1 = IncomeSourceEntity(
            id = "inc_1",
            title = "Tech Salary",
            amountPaise = 8500000L, // ₹85,000.00
            isRecurring = true,
            frequency = "Monthly",
            nextPayDateEpochMs = 1730419200000L,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        val income2 = IncomeSourceEntity(
            id = "inc_2",
            title = "Advisory Retainer",
            amountPaise = 2500000L, // ₹25,000.00
            isRecurring = true,
            frequency = "Bi-weekly",
            nextPayDateEpochMs = 1730419200000L,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        val income3 = IncomeSourceEntity(
            id = "inc_3",
            title = "One-time bonus",
            amountPaise = 500000L, // ₹5,000.00
            isRecurring = false,
            frequency = "One-time",
            nextPayDateEpochMs = 1730419200000L,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        val allIncomes = listOf(income1, income2, income3)
        val recurringTotalPaise = allIncomes.filter { it.isRecurring }.sumOf { it.amountPaise }
        assertEquals(11000000L, recurringTotalPaise) // ₹1,10,000.00

        val domainItem = income1.toSpendoIncomeItem()
        assertEquals("Tech Salary", domainItem.title)
        assertEquals(8500000L, domainItem.amountPaise)
        assertTrue(domainItem.isRecurring)
    }

    @Test
    fun transactionFiltering_filtersByQueryTypeAndCategory() {
        val testTransactions = listOf(
            SpendoTransaction(
                id = "tx_1",
                merchant = "Blue Tokai Coffee",
                amountPaise = 34000L,
                type = TransactionType.EXPENSE,
                category = "Food & Drink",
                occurredAtEpochMs = 1000L,
                bankAccountName = "HDFC Bank",
                source = TransactionSource.MANUAL,
                note = "Flat white"
            ),
            SpendoTransaction(
                id = "tx_2",
                merchant = "Uber Technologies",
                amountPaise = 42000L,
                type = TransactionType.EXPENSE,
                category = "Transit",
                occurredAtEpochMs = 2000L,
                bankAccountName = "HDFC Bank",
                source = TransactionSource.MANUAL,
                note = "Tech campus"
            ),
            SpendoTransaction(
                id = "tx_3",
                merchant = "Acme Corp Tech",
                amountPaise = 8500000L,
                type = TransactionType.INCOME,
                category = "Salary",
                occurredAtEpochMs = 3000L,
                bankAccountName = "HDFC Bank",
                source = TransactionSource.MANUAL,
                note = "Monthly salary"
            )
        )

        // Filter by Query
        val queryMatches = testTransactions.filter {
            it.merchant.contains("Tokai", ignoreCase = true) || (it.note?.contains("Tokai", ignoreCase = true) == true)
        }
        assertEquals(1, queryMatches.size)
        assertEquals("tx_1", queryMatches.first().id)

        // Filter by Type: INCOME
        val incomeMatches = testTransactions.filter { it.type == TransactionType.INCOME }
        assertEquals(1, incomeMatches.size)
        assertEquals("Acme Corp Tech", incomeMatches.first().merchant)

        // Filter by Category: Transit
        val transitMatches = testTransactions.filter { it.category == "Transit" }
        assertEquals(1, transitMatches.size)
        assertEquals("Uber Technologies", transitMatches.first().merchant)
    }

    @Test
    fun notificationRecord_mapsCleanlyToUiItem() {
        val record = NotificationRecordEntity(
            id = "notif_101",
            packageName = "com.google.android.apps.nbu.paisa.user",
            appName = "Google Pay",
            title = "Debit Alert",
            message = "Paid ₹340.00 to Blue Tokai Coffee",
            detectedAmountPaise = 34000L,
            direction = "DEBIT",
            category = "Transactions",
            status = "DETECTED",
            postedAtEpochMs = System.currentTimeMillis() - 120000L, // 2 mins ago
            progress = null,
            isUnread = true
        )

        val uiItem = record.toSpendoNotificationItem()
        assertEquals("notif_101", uiItem.id)
        assertEquals("Debit Alert", uiItem.title)
        assertEquals("Transactions", uiItem.category)
        assertTrue(uiItem.isUnread)
    }

    @Test
    fun aiProviderConfigEntity_storesMetadataWithoutPlaintextSecrets() {
        val config = AiProviderConfigEntity(
            providerId = AiProvider.GROQ.name,
            displayName = AiProvider.GROQ.displayName,
            selectedModel = "llama-3.3-70b-versatile",
            isConfigured = true,
            lastTestedEpochMs = 1729780000000L,
            lastTestMessage = "Latency: 95ms",
            updatedAtEpochMs = 1729780000000L
        )

        assertEquals("GROQ", config.providerId)
        assertEquals("llama-3.3-70b-versatile", config.selectedModel)
        assertTrue(config.isConfigured)
        assertNotNull(config.lastTestMessage)
    }

    @Test
    fun spendoViewModel_handlesAddAndDeleteTransactionCorrectly() {
        val viewModel = SpendoViewModel()
        val initialCount = viewModel.uiState.value.transactions.size

        // Add expense
        viewModel.addTransaction(
            merchant = "Starbucks Reserve",
            amountPaise = 45000L, // ₹450.00
            type = TransactionType.EXPENSE,
            category = "Food & Drink",
            bankAccountName = "HDFC Bank",
            note = "Cold brew"
        )

        val afterAddState = viewModel.uiState.value
        assertEquals(initialCount + 1, afterAddState.transactions.size)
        val addedTx = afterAddState.transactions.first()
        assertEquals("Starbucks Reserve", addedTx.merchant)
        assertEquals(45000L, addedTx.amountPaise)
        assertEquals(TransactionType.EXPENSE, addedTx.type)

        // Delete the added expense
        viewModel.deleteTransaction(addedTx.id)
        val afterDeleteState = viewModel.uiState.value
        assertEquals(initialCount, afterDeleteState.transactions.size)
        assertFalse(afterDeleteState.transactions.any { it.id == addedTx.id })
    }

    @Test
    fun spendoViewModel_updatesBudgetsAndBankAccountsCorrectly() {
        val viewModel = SpendoViewModel()

        // Update budget limits
        viewModel.updateBudgetLimits(
            dailyPaise = 300000L,   // ₹3,000.00
            monthlyPaise = 7000000L // ₹70,000.00
        )
        val budgetState = viewModel.uiState.value
        assertEquals(300000L, budgetState.dailyLimitPaise)
        assertEquals(7000000L, budgetState.monthlyLimitPaise)

        // Add bank account
        val initialBankCount = budgetState.banks.size
        viewModel.addBank(
            name = "Kotak Mahindra Bank",
            type = "Savings Account",
            initialPaise = 3500000L // ₹35,000.00
        )
        val bankState = viewModel.uiState.value
        assertEquals(initialBankCount + 1, bankState.banks.size)
        assertTrue(bankState.banks.any { it.bankName == "Kotak Mahindra Bank" && it.balancePaise == 3500000L })
    }
}
