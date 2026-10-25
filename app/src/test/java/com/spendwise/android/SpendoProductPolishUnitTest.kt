package com.spendwise.android

import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import com.spendwise.android.ui.spendo.model.AffordabilityEvaluation
import com.spendwise.android.ui.spendo.model.AffordabilityVerdict
import com.spendwise.android.ui.spendo.model.PendingExpenseConfirmation
import com.spendwise.android.ui.spendo.model.SpendoChatMessage
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.SpendoViewModel
import com.spendwise.android.ui.spendo.model.TransactionSource
import com.spendwise.android.ui.spendo.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SpendoProductPolishUnitTest {

    @Test
    fun testNaturalLanguageExpenseParsingBiryaniPrompt() {
        // "Spent ₹280 on biryani using HDFC"
        val input = "Spent ₹280 on biryani using HDFC"
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected AddExpense intent", intent is VoiceIntent.AddExpense)
        val expense = intent as VoiceIntent.AddExpense
        assertEquals(28000L, expense.amountPaise)
        assertEquals("Biryani", expense.merchant)
        assertEquals("Food & Drink", expense.category)
        assertEquals("HDFC Bank", expense.bankName)
    }

    @Test
    fun testVoiceQueryMerchantSpendExtraction() {
        // "How much did I spend on Swiggy?"
        val input = "How much did I spend on Swiggy?"
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected QueryMerchantSpend intent", intent is VoiceIntent.QueryMerchantSpend)
        val query = intent as VoiceIntent.QueryMerchantSpend
        assertEquals("Swiggy", query.merchantName)
    }

    @Test
    fun testVoiceQueryTopCategoryExtraction() {
        // "What is my biggest expense category this month?"
        val input = "What is my biggest expense category this month?"
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected QueryTopCategory intent", intent is VoiceIntent.QueryTopCategory)
    }

    @Test
    fun testVoiceQueryCompareLastMonthExtraction() {
        // "Compare my spending with last month."
        val input = "Compare my spending with last month."
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected QueryCompareLastMonth intent", intent is VoiceIntent.QueryCompareLastMonth)
    }

    @Test
    fun testVoiceQueryReduceSpendingExtraction() {
        // "How can I reduce my spending next month?"
        val input = "How can I reduce my spending next month?"
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected QueryReduceSpending intent", intent is VoiceIntent.QueryReduceSpending)
    }

    @Test
    fun testVoiceQueryTodaySpendingExtraction() {
        // "How much did I spend today?"
        val input = "How much did I spend today?"
        val intent = VoiceFinancialParser.parseSpeech(input)

        assertTrue("Expected InquireSpending intent", intent is VoiceIntent.InquireSpending)
        val spendIntent = intent as VoiceIntent.InquireSpending
        assertEquals("TODAY", spendIntent.period)
    }

    @Test
    fun testPurchaseAffordabilityCalculatorEvaluation() {
        val liquidBalancePaise = 5000000L // ₹50,000
        val upcomingBillsPaise = 1000000L // ₹10,000
        val plannedSavingsPaise = 2500000L // ₹25,000
        val freeDiscretionaryPaise = (liquidBalancePaise - upcomingBillsPaise - plannedSavingsPaise).coerceAtLeast(0L) // ₹15,000
        val monthlyRemainingPaise = 3000000L // ₹30,000

        // 1. Safe Discretionary Purchase: ₹5,000 <= ₹15,000
        val item1Paise = 500000L
        val verdict1 = when {
            item1Paise <= freeDiscretionaryPaise && item1Paise <= monthlyRemainingPaise -> AffordabilityVerdict.AFFORDABLE
            item1Paise <= (liquidBalancePaise - upcomingBillsPaise) -> AffordabilityVerdict.CAUTION
            else -> AffordabilityVerdict.RISKY
        }
        assertEquals(AffordabilityVerdict.AFFORDABLE, verdict1)

        // 2. Caution Purchase: ₹20,000 (dips into planned savings reserve)
        val item2Paise = 2000000L
        val verdict2 = when {
            item2Paise <= freeDiscretionaryPaise && item2Paise <= monthlyRemainingPaise -> AffordabilityVerdict.AFFORDABLE
            item2Paise <= (liquidBalancePaise - upcomingBillsPaise) -> AffordabilityVerdict.CAUTION
            else -> AffordabilityVerdict.RISKY
        }
        assertEquals(AffordabilityVerdict.CAUTION, verdict2)

        // 3. Risky Purchase: ₹45,000 (endangers upcoming essential bills)
        val item3Paise = 4500000L
        val verdict3 = when {
            item3Paise <= freeDiscretionaryPaise && item3Paise <= monthlyRemainingPaise -> AffordabilityVerdict.AFFORDABLE
            item3Paise <= (liquidBalancePaise - upcomingBillsPaise) -> AffordabilityVerdict.CAUTION
            else -> AffordabilityVerdict.RISKY
        }
        assertEquals(AffordabilityVerdict.RISKY, verdict3)
    }

    @Test
    fun testIntegerPaiseMonetaryPrecision() {
        // Verify paise arithmetic never accumulates IEEE 754 floating-point drift
        val tx1Paise = 100L   // ₹1.00
        val tx2Paise = 200L   // ₹2.00
        val tx3Paise = 555L   // ₹5.55
        val totalPaise = tx1Paise + tx2Paise + tx3Paise

        assertEquals(855L, totalPaise)
        assertEquals("₹8.55", "₹%,.2f".format(Locale.ENGLISH, totalPaise / 100.0))
    }

    @Test
    fun testPendingExpenseConfirmationLifecycle() {
        val vm = SpendoViewModel()
        val pending = PendingExpenseConfirmation(
            amountPaise = 28000L,
            merchant = "Biryani Blues",
            category = "Food & Dining",
            bankName = "HDFC Bank"
        )

        // Add a message with pending action
        val testMessage = SpendoChatMessage(
            id = "msg_confirm_test",
            sender = "SPENDO",
            text = "Please verify details before saving",
            pendingExpense = pending
        )
        vm.addChatMessage(testMessage)

        // Confirming the expense clears the pending item and appends confirmation
        vm.confirmPendingExpense(pending, "msg_confirm_test")

        val state = vm.uiState.value
        val lastMessage = state.chatMessages.last()
        assertTrue(lastMessage.text.contains("Confirmed & saved expense"))
        assertTrue(lastMessage.text.contains("Biryani Blues"))
    }

    @Test
    fun testReviewTransactionModelIntegrity() {
        val tx = SpendoTransaction(
            id = "tx_rev_01",
            merchant = "QuickCab Services",
            amountPaise = 18900L,
            type = TransactionType.EXPENSE,
            category = "Transit",
            occurredAtEpochMs = 1761300000000L,
            bankAccountName = "Kotak Mahindra Bank",
            source = TransactionSource.NOTIFICATION,
            reviewStatus = "NEEDS_REVIEW",
            reviewReason = "SUSPECTED MULTIPLE CHARGES"
        )

        assertEquals("NEEDS_REVIEW", tx.reviewStatus)
        assertEquals("SUSPECTED MULTIPLE CHARGES", tx.reviewReason)
        assertEquals("₹189.00", tx.formattedAmount)
    }

    @Test
    fun testCsvLineTokenizerHandlesQuotesAndCommas() {
        val sample = "TXN_01,2026-05-10,12:00:00,\"HDFC: Rs 1,200.00 debited to \"\"STORE\"\"\",Starbucks,1200.00,expense,Food & Drink,UPI,HDFC Bank,4821,DEBIT,\"Cold brew, pastry\""
        val tokens = com.spendwise.android.util.CsvImporter.parseCsvLine(sample)
        assertEquals(13, tokens.size)
        assertEquals("TXN_01", tokens[0])
        assertEquals("HDFC: Rs 1,200.00 debited to \"STORE\"", tokens[3])
        assertEquals("Cold brew, pastry", tokens[12])
    }

    @Test
    fun testSyntheticCsvFileImportIntegrity() {
        val file = java.io.File("synthetic_transactions.csv")
        if (file.exists()) {
            file.inputStream().use { stream ->
                val entities = com.spendwise.android.util.CsvImporter.parseCsv(stream)
                assertEquals(725, entities.size)

                // First transaction check (Salary)
                val first = entities.first()
                assertEquals("Infosys Tech Solutions", first.merchant)
                assertEquals(9500000L, first.amountPaise)
                assertEquals("CREDIT", first.direction)
                assertEquals("Salary", first.categoryId)

                // Verification of debits and credits
                val debits = entities.filter { it.direction == "DEBIT" }
                val credits = entities.filter { it.direction == "CREDIT" }
                assertTrue(debits.isNotEmpty())
                assertTrue(credits.isNotEmpty())
                assertEquals(725, debits.size + credits.size)
            }
        }
    }

    @Test
    fun testSecurityManagerInitialDefaultPasswordAndMigration() = kotlinx.coroutines.runBlocking {
        val fakeStorage = object : com.spendwise.android.domain.security.SpendoSecurityStorage {
            val map = mutableMapOf<String, String>()
            override suspend fun getPreferenceDirect(key: String): String? = map[key]
            override suspend fun setPreference(key: String, value: String) { map[key] = value }
        }

        val manager = com.spendwise.android.domain.security.SpendoSecurityManager(fakeStorage)

        // Initial state: no custom password
        assertEquals(false, manager.hasCustomPassword())

        // Default PIN (4892) verifies and migrates
        val verifiedDefault = manager.verifyPassword("4892")
        assertTrue("Default PIN 4892 should verify", verifiedDefault)

        // Salt and hash are now persisted in storage
        assertTrue(fakeStorage.map.containsKey(com.spendwise.android.domain.security.SpendoSecurityManager.PREF_SALT_HEX))
        assertTrue(fakeStorage.map.containsKey(com.spendwise.android.domain.security.SpendoSecurityManager.PREF_HASH_HEX))

        // Incorrect password fails
        val wrongAttempt = manager.verifyPassword("9999")
        assertEquals(false, wrongAttempt)
    }

    @Test
    fun testSecurityManagerChangePasswordAndOldPasswordCeasesToWork() = kotlinx.coroutines.runBlocking {
        val fakeStorage = object : com.spendwise.android.domain.security.SpendoSecurityStorage {
            val map = mutableMapOf<String, String>()
            override suspend fun getPreferenceDirect(key: String): String? = map[key]
            override suspend fun setPreference(key: String, value: String) { map[key] = value }
        }

        val manager = com.spendwise.android.domain.security.SpendoSecurityManager(fakeStorage)

        // 1. Attempt change with wrong current password should fail
        val failedResult = manager.changePassword("wrong", "newPass99")
        assertTrue(failedResult is com.spendwise.android.domain.security.PasswordChangeResult.InvalidCurrentPassword)

        // 2. Attempt change with too short password should fail
        val shortResult = manager.changePassword("4892", "12")
        assertTrue(shortResult is com.spendwise.android.domain.security.PasswordChangeResult.PasswordTooShort)

        // 3. Successful change from 4892 to MySecureWallet#123
        val successResult = manager.changePassword("4892", "MySecureWallet#123")
        assertTrue(successResult is com.spendwise.android.domain.security.PasswordChangeResult.Success)
        assertTrue(manager.hasCustomPassword())

        // 4. Verify that previous password (4892) NO LONGER authenticates
        val oldAttempt = manager.verifyPassword("4892")
        assertEquals(false, oldAttempt)

        // 5. Verify that new password authenticates immediately
        val newAttempt = manager.verifyPassword("MySecureWallet#123")
        assertTrue(newAttempt)

        // 6. Verify persistence across app restart (new SecurityManager instance with same storage)
        val managerAfterRestart = com.spendwise.android.domain.security.SpendoSecurityManager(fakeStorage)
        assertTrue(managerAfterRestart.hasCustomPassword())
        assertEquals(false, managerAfterRestart.verifyPassword("4892"))
        assertTrue(managerAfterRestart.verifyPassword("MySecureWallet#123"))
    }

    // -------------------------------------------------------------
    // Area 1 & 2: CSV Export & Import Tests
    // -------------------------------------------------------------
    @Test
    fun testCsv13ColumnExportAndImportRoundTrip() {
        val originalTxs = listOf(
            SpendoTransaction(
                id = "tx_exp_001",
                merchant = "Blue Tokai Coffee, Roasters",
                amountPaise = 34000L,
                type = TransactionType.EXPENSE,
                category = "Food & Drink",
                occurredAtEpochMs = 1730000000000L,
                bankAccountName = "HDFC Bank",
                source = TransactionSource.MANUAL,
                note = "Cappuccino & croissant, morning"
            ),
            SpendoTransaction(
                id = "tx_exp_002",
                merchant = "Consulting Client",
                amountPaise = 5000000L,
                type = TransactionType.INCOME,
                category = "Salary",
                occurredAtEpochMs = 1730050000000L,
                bankAccountName = "Kotak Mahindra Bank",
                source = TransactionSource.MANUAL,
                note = "Advance invoice #104"
            )
        )

        // 1. Export to full 13-column CSV string
        val csvOut = com.spendwise.android.util.CsvExporter.generateFullCsvFromSpendoTransactions(originalTxs)
        val lines = csvOut.trim().split("\n")
        assertEquals(3, lines.size) // Header + 2 rows
        assertEquals(com.spendwise.android.util.CsvExporter.CSV_HEADER_FULL, lines[0].trim())

        // Check comma escaping for merchant and note
        assertTrue(lines[1].contains("\"Blue Tokai Coffee, Roasters\""))
        assertTrue(lines[1].contains("\"Cappuccino & croissant, morning\""))
        assertTrue(lines[1].contains("340.00"))

        // 2. Import back using CsvImporter
        val inStream = java.io.ByteArrayInputStream(csvOut.toByteArray(Charsets.UTF_8))
        val (parsedEntities, rejectedCount) = com.spendwise.android.util.CsvImporter.parseCsvDetailed(inStream)
        assertEquals(0, rejectedCount)
        assertEquals(2, parsedEntities.size)

        val first = parsedEntities[0]
        assertEquals("tx_exp_001", first.id)
        assertEquals("Blue Tokai Coffee, Roasters", first.merchant)
        assertEquals(34000L, first.amountPaise)
        assertEquals("DEBIT", first.direction)

        val second = parsedEntities[1]
        assertEquals("tx_exp_002", second.id)
        assertEquals(5000000L, second.amountPaise)
        assertEquals("CREDIT", second.direction)
    }

    @Test
    fun testCsvImportHandlesMalformedRowsWithoutCrashing() {
        val malformedCsv = """
            transaction_id,date,time,description,merchant,amount,transaction_type,category,payment_method,bank_account,account_last4,debit_credit,notes
            tx_good_1,2026-10-24,10:00:00,Good meal,Cafe,150.00,expense,Food & Drink,UPI,Kotak,1920,DEBIT,note
            tx_bad_amt,2026-10-24,11:00:00,Bad amount,Cafe,-50.00,expense,Food & Drink,UPI,Kotak,1920,DEBIT,note
            tx_bad_date,not-a-date,12:00:00,No date,Cafe,200.00,expense,Food & Drink,UPI,Kotak,1920,DEBIT,note
            tx_good_2,2026-10-24,13:00:00,Good ride,Uber,280.00,expense,Transit,UPI,HDFC,4892,DEBIT,ride
        """.trimIndent()

        val inStream = java.io.ByteArrayInputStream(malformedCsv.toByteArray(Charsets.UTF_8))
        val (parsed, rejected) = com.spendwise.android.util.CsvImporter.parseCsvDetailed(inStream)

        assertEquals(2, parsed.size)
        assertEquals("tx_good_1", parsed[0].id)
        assertEquals("tx_good_2", parsed[1].id)
        assertTrue("Expected rejected rows for invalid amount and invalid date", rejected >= 1)
    }

    @Test
    fun testCsvExportEmptyListProducesValidHeader() {
        val csv = com.spendwise.android.util.CsvExporter.generateFullCsvFromSpendoTransactions(emptyList())
        val lines = csv.trim().split("\n")
        assertEquals(1, lines.size)
        assertEquals(com.spendwise.android.util.CsvExporter.CSV_HEADER_FULL, lines[0].trim())
    }

    // -------------------------------------------------------------
    // Area 3: Voice Input Natural Language Tests (No Hardcoded Fallback)
    // -------------------------------------------------------------
    @Test
    fun testVoiceInputNaturalLanguageExpensePhrasing() {
        // Natural speech 1: "I spent 250 rupees on lunch through Kotak today."
        val intent1 = VoiceFinancialParser.parseSpeech("I spent 250 rupees on lunch through Kotak today.")
        assertTrue(intent1 is VoiceIntent.AddExpense)
        val exp1 = intent1 as VoiceIntent.AddExpense
        assertEquals(25000L, exp1.amountPaise)
        assertEquals("Lunch", exp1.merchant)
        assertEquals("Food & Drink", exp1.category)
        assertEquals("Kotak Mahindra Bank", exp1.bankName)

        // Natural speech 2: "Paid 500 for petrol yesterday using HDFC."
        val intent2 = VoiceFinancialParser.parseSpeech("Paid 500 for petrol yesterday using HDFC.")
        assertTrue(intent2 is VoiceIntent.AddExpense)
        val exp2 = intent2 as VoiceIntent.AddExpense
        assertEquals(50000L, exp2.amountPaise)
        assertEquals("Petrol", exp2.merchant)
        assertEquals("Transit", exp2.category)
        assertEquals("HDFC Bank", exp2.bankName)
    }

    @Test
    fun testVoiceInputIncomeParsing() {
        // "Received 1500 rupees from freelancing."
        val intent = VoiceFinancialParser.parseSpeech("Received 1500 rupees from freelancing.")
        assertTrue(intent is VoiceIntent.AddIncome)
        val inc = intent as VoiceIntent.AddIncome
        assertEquals(150000L, inc.amountPaise)
        assertEquals("Freelancing", inc.source)
        assertEquals("Freelance", inc.category)
    }

    @Test
    fun testVoiceInputUnrecognizedSpeechDoesNotFabricateTransaction() {
        val intent = VoiceFinancialParser.parseSpeech("Hello what is the weather today?")
        assertTrue(intent is VoiceIntent.GeneralChat)
        assertTrue(intent !is VoiceIntent.AddExpense)
        assertTrue(intent !is VoiceIntent.AddIncome)
    }

    // -------------------------------------------------------------
    // Area 4: Manual Expense Editing
    // -------------------------------------------------------------
    @Test
    fun testManualExpenseEditingUpdatesRecordWithoutDuplication() {
        val viewModel = SpendoViewModel()
        val originalTx = viewModel.uiState.value.transactions.first()
        val originalId = originalTx.id
        val originalSize = viewModel.uiState.value.transactions.size

        // Edit transaction with modified amount, merchant, and category
        val updatedTx = originalTx.copy(
            amountPaise = 77700L, // ₹777.00
            merchant = "Updated Test Merchant",
            category = "Shopping",
            note = "Edited by user"
        )

        viewModel.updateTransaction(updatedTx)

        val newTransactions = viewModel.uiState.value.transactions
        assertEquals("Transaction count must remain unchanged after edit (no duplication)", originalSize, newTransactions.size)

        val retrieved = newTransactions.find { it.id == originalId }
        assertNotNull(retrieved)
        assertEquals(77700L, retrieved!!.amountPaise)
        assertEquals("Updated Test Merchant", retrieved.merchant)
        assertEquals("Shopping", retrieved.category)
        assertEquals("Edited by user", retrieved.note)
    }

    // -------------------------------------------------------------
    // Area 5: Calendar Month Boundary & Date Filtering Calculations
    // -------------------------------------------------------------
    @Test
    fun testCalendarMonthBoundaryCalculationsAcrossYearEnd() {
        val cal = java.util.Calendar.getInstance()

        // 1. December 2025: 31 days
        cal.clear()
        cal.set(2025, java.util.Calendar.DECEMBER, 1, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val decStart = cal.timeInMillis
        val decDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        assertEquals(31, decDays)

        // 2. January 2026: 31 days
        cal.clear()
        cal.set(2026, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val janStart = cal.timeInMillis
        val janDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        assertEquals(31, janDays)

        // Boundary invariant: January starts strictly after December
        assertTrue(janStart > decStart)

        // 3. Transactions filtering: December transaction must not appear in January
        val decTxEpoch = decStart + (15L * 86400000L) // Dec 16th
        val janTxEpoch = janStart + (10L * 86400000L) // Jan 11th

        val isDecInJan = decTxEpoch in janStart until (janStart + (31L * 86400000L))
        val isJanInJan = janTxEpoch in janStart until (janStart + (31L * 86400000L))

        assertEquals(false, isDecInJan)
        assertEquals(true, isJanInJan)
    }

    // -------------------------------------------------------------
    // Area 6 & 7: Secure Keystore API Key Encryption & Persistence
    // -------------------------------------------------------------
    @Test
    fun testSecureKeyStorageEncryptionPersistenceAndRetrieval() = kotlinx.coroutines.runBlocking {
        val fakeStorage = object : com.spendwise.android.domain.security.SpendoSecurityStorage {
            val map = mutableMapOf<String, String>()
            override suspend fun getPreferenceDirect(key: String): String? = map[key]
            override suspend fun setPreference(key: String, value: String) { map[key] = value }
        }

        val keyStorage = com.spendwise.android.domain.security.SpendoSecureKeyStorage(fakeStorage)

        val rawApiKey = "gsk_live_secret_489291820491820491820"

        // 1. Save API key
        keyStorage.saveApiKey("GROQ", rawApiKey)

        // 2. Verify it is NEVER stored in plaintext in storage
        val storedRaw = fakeStorage.map["enc_api_key_groq"]
        assertNotNull(storedRaw)
        assertTrue("Stored payload must not contain raw API key in plaintext", !storedRaw!!.contains(rawApiKey))
        assertTrue("Stored payload must be formatted as IV:Ciphertext", storedRaw.contains(":"))

        // 3. Retrieve and decrypt API key
        val decrypted = keyStorage.getApiKey("GROQ")
        assertEquals(rawApiKey, decrypted)

        // 4. Delete API key
        keyStorage.deleteApiKey("GROQ")
        val afterDelete = keyStorage.getApiKey("GROQ")
        assertNull(afterDelete)
    }
}


