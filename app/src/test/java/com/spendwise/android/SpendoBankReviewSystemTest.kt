package com.spendwise.android

import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.domain.banking.AccountMatchStatus
import com.spendwise.android.domain.banking.BankAccountMatcher
import com.spendwise.android.domain.banking.BankIdentifier
import com.spendwise.android.domain.banking.BankMessageParserRegistry
import com.spendwise.android.domain.banking.HdfcBankParser
import com.spendwise.android.domain.banking.SbiBankParser
import com.spendwise.android.domain.banking.IciciBankParser
import com.spendwise.android.domain.banking.AxisBankParser
import com.spendwise.android.domain.banking.KotakBankParser
import com.spendwise.android.domain.banking.KvbBankParser
import com.spendwise.android.domain.banking.IndianBankParser
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.NotificationMessageCopy
import com.spendwise.android.notification.NotificationPersonalityTone
import com.spendwise.android.notification.SpendoNotificationPersonality
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

class SpendoBankReviewSystemTest {

    // -------------------------------------------------------------
    // 1. Bank Identification & Negation Guard
    // -------------------------------------------------------------

    @Test
    fun `BankIdentifier correctly identifies HDFC with high confidence`() {
        val payload = RawNotificationPayload(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank Alert",
            text = "Rs 1,450.00 debited from HDFC Bank A/c **1234 to ZOMATO on 24-Oct-26. Info: UPI/291823. Avl bal INR 18,200.00",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("HDFC Bank", result.bankName)
        assertTrue("Confidence must be >= 0.85", result.confidence >= 0.85f)
        assertFalse("Should not be ambiguous", result.isAmbiguous)
    }

    @Test
    fun `BankIdentifier prevents payee negation false positives`() {
        // Text contains 'Kotak Securities' as merchant, but originating bank is HDFC
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "HDFC Bank",
            text = "Rs 5,000.00 debited from a/c **4321 towards Kotak Securities Ltd on 24-Oct-26. Avl bal INR 25,000.00",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("Originating bank must be HDFC Bank, not Kotak", "HDFC Bank", result.bankName)
    }

    @Test
    fun `BankIdentifier identifies SBI from alert text and sender code`() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "SBIUPI",
            text = "Dear SBI User, your A/c ending 5678 debited by Rs 250.00 on 24-Oct-26 by transfer to SWIGGY. Ref No 293817293812. Bal Rs 4,500.00",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("State Bank of India", result.bankName)
    }

    @Test
    fun `BankIdentifier identifies ICICI from alert text`() {
        val payload = RawNotificationPayload(
            packageName = "com.csam.icici.bank.imobile",
            title = "ICICI Bank Alert",
            text = "Acct XX1029 debited with INR 890.00 on 24-Oct-26. Info: BIL/AMZN. Avl Bal: INR 12,450.00",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("ICICI Bank", result.bankName)
    }

    @Test
    fun `BankIdentifier identifies Axis Bank from alert text`() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "AXISBK",
            text = "INR 350.00 debited from Axis Bank A/c no. XX9988 on 24-Oct-26 towards UBER INDIA. UPI Ref 38291038291. Avl Bal INR 1,200.00",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("Axis Bank", result.bankName)
    }

    @Test
    fun `BankIdentifier correctly identifies KVB with support signature`() {
        val payload = RawNotificationPayload(
            packageName = "com.kvb.dlite",
            title = "KVBANK",
            text = "Your KVB A/c XXXXXXXXXXXX9362 is debited by Rs. 50.00 on 24-Oct-26 to Chai Point. Avl Bal Rs. 14,200.00. Help 18005721916-KVB",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("Karur Vysya Bank", result.bankName)
        assertTrue(result.confidence >= 0.85f)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `BankIdentifier correctly identifies Kotak Mahindra Bank`() {
        val payload = RawNotificationPayload(
            packageName = "com.msf.kbank.mobile",
            title = "KOTAK",
            text = "Sent Rs. 250.00 from Kotak Bank a/c XX7710 to Swiggy on 24-Oct-26. UPI Ref 382910382. Call 18602662666",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("Kotak Mahindra Bank", result.bankName)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun `BankIdentifier correctly identifies Indian Bank`() {
        val payload = RawNotificationPayload(
            packageName = "com.infrasofttech.IndianBank",
            title = "INDBNK",
            text = "Indian Bank: Your A/c ending 4821 debited for INR 1,200.00 towards Amazon. Ref 3829103. Call 180042500000",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertEquals("Indian Bank", result.bankName)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun `BankIdentifier returns null and 0 confidence for unknown non-banking alert`() {
        val payload = RawNotificationPayload(
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "Hey, let's meet tomorrow at 5 PM for coffee!",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertNull("Non-banking alert must return null bankName", result.bankName)
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test
    fun `BankIdentifier handles competing bank indicators by flagging ambiguity`() {
        // Body mentions multiple banks with ambiguous signals
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "Alert",
            text = "Transfer between KVB A/c and Kotak Bank A/c processed for Rs 500",
            postTimeEpochMs = 1792800000000L
        )
        val result = BankIdentifier.identifyBank(payload)
        assertTrue("Competing bank signatures without clear package/header should be flagged ambiguous or low confidence",
            result.isAmbiguous || result.confidence < 0.5f || result.bankName == null)
    }

    // -------------------------------------------------------------
    // 2. Bank Parsers Extraction
    // -------------------------------------------------------------

    @Test
    fun `HdfcBankParser parses debit correctly`() {
        val payload = RawNotificationPayload(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "Rs 1,450.00 debited from HDFC Bank A/c **1234 to ZOMATO on 24-Oct-26. Info: UPI/291823. Avl bal INR 18,200.00",
            postTimeEpochMs = 1792800000000L
        )
        val parser = HdfcBankParser()
        val parsed = parser.parse(payload)
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(145000L, parsed.amountPaise)
        assertEquals(1820000L, parsed.reportedBalancePaise)
        assertEquals("1234", parsed.accountSuffix)
        assertEquals("ZOMATO", parsed.merchantOrPayee)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("HDFC Bank", parsed.bankName)
    }

    @Test
    fun `SbiBankParser parses debit correctly`() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "SBIUPI",
            text = "Dear SBI User, your A/c ending 5678 debited by Rs 250.00 on 24-Oct-26 by transfer to SWIGGY. Ref No 293817293812. Bal Rs 4,500.00",
            postTimeEpochMs = 1792800000000L
        )
        val parser = SbiBankParser()
        val parsed = parser.parse(payload)
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(25000L, parsed.amountPaise)
        assertEquals(450000L, parsed.reportedBalancePaise)
        assertEquals("5678", parsed.accountSuffix)
        assertEquals("SWIGGY", parsed.merchantOrPayee)
        assertEquals("293817293812", parsed.referenceNumber)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
    }

    @Test
    fun `IciciBankParser parses credit refund correctly`() {
        val payload = RawNotificationPayload(
            packageName = "com.csam.icici.bank.imobile",
            title = "ICICI Bank Alert",
            text = "Acct XX1029 credited with INR 499.00 on 24-Oct-26 by REFUND FLIPKART. Avl Bal: INR 12,949.00",
            postTimeEpochMs = 1792800000000L
        )
        val parser = IciciBankParser()
        val parsed = parser.parse(payload)
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(49900L, parsed.amountPaise)
        assertEquals(1294900L, parsed.reportedBalancePaise)
        assertEquals("1029", parsed.accountSuffix)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
    }

    @Test
    fun `AxisBankParser parses debit correctly`() {
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            title = "AXISBK",
            text = "INR 350.00 debited from Axis Bank A/c no. XX9988 on 24-Oct-26 towards UBER INDIA. UPI Ref 38291038291. Avl Bal INR 1,200.00",
            postTimeEpochMs = 1792800000000L
        )
        val parser = AxisBankParser()
        val parsed = parser.parse(payload)
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(35000L, parsed.amountPaise)
        assertEquals(120000L, parsed.reportedBalancePaise)
        assertEquals("9988", parsed.accountSuffix)
        assertEquals("UBER INDIA", parsed.merchantOrPayee)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
    }

    // -------------------------------------------------------------
    // 2b. Account Suffix Extraction Tests
    // -------------------------------------------------------------

    private class TestSuffixParser : com.spendwise.android.domain.banking.BaseIndianBankParser() {
        override val bankName: String = "TestBank"
        override fun canHandle(senderOrPackage: String, text: String): Boolean = true
        override fun parse(payload: RawNotificationPayload) = null
        fun extractSuffixForTest(text: String): String? = extractAccountSuffix(text)
    }

    private val suffixParser = TestSuffixParser()

    @Test
    fun `extractAccountSuffix handles full masked account string ending in four digits`() {
        val text = "Your KVB account XXXXXXXXXXXX9362 is debited Rs. 50.00"
        val suffix = suffixParser.extractSuffixForTest(text)
        assertEquals("9362", suffix)
    }

    @Test
    fun `extractAccountSuffix handles short masked account string and bullets`() {
        val text1 = "Your Kotak account XX7710 is debited Rs. 250.00"
        assertEquals("7710", suffixParser.extractSuffixForTest(text1))

        val text2 = "Spent Rs. 1,000 using Kotak Bank •••• 4821 at Amazon"
        assertEquals("4821", suffixParser.extractSuffixForTest(text2))

        val text3 = "Debited from a/c *1234 INR 500"
        assertEquals("1234", suffixParser.extractSuffixForTest(text3))
    }

    @Test
    fun `extractAccountSuffix handles explicit account-ending text`() {
        val text1 = "Rs 250 debited from A/c No. XXXXX9362 on 12-Oct-26"
        assertEquals("9362", suffixParser.extractSuffixForTest(text1))

        val text2 = "Txn on a/c ending 9362 for Rs 500"
        assertEquals("9362", suffixParser.extractSuffixForTest(text2))

        val text3 = "Spent on card ending in 4821 Rs 1,500"
        assertEquals("4821", suffixParser.extractSuffixForTest(text3))
    }

    @Test
    fun `extractAccountSuffix returns null for missing suffix in notification`() {
        val text = "Alert: Rs 500 debited from your bank account to Swiggy on 24-Oct-26"
        val suffix = suffixParser.extractSuffixForTest(text)
        assertNull("Missing suffix must return null", suffix)
    }

    @Test
    fun `extractAccountSuffix returns null for invalid suffix`() {
        val text = "Transaction on account 12 completed"
        val suffix = suffixParser.extractSuffixForTest(text)
        assertNull("Suffix shorter than 4 digits must return null", suffix)
    }

    @Test
    fun `extractAccountSuffix prioritizes account token over reference numbers and phone numbers`() {
        val text = "Txn of Rs 500.00 on a/c *9362. UPI ref: 123456789012. Call 18005721916"
        val suffix = suffixParser.extractSuffixForTest(text)
        assertEquals("9362", suffix)
    }

    // -------------------------------------------------------------
    // 3. BankAccountMatcher Tests
    // -------------------------------------------------------------

    private class FakeBankAccountDao(
        private val accounts: List<BankAccountEntity>
    ) : BankAccountDao {
        override fun getAllBankAccounts() = flowOf(accounts)
        override suspend fun getAllBankAccountsDirect() = accounts
        override fun getBankAccountById(id: String) = flowOf(accounts.firstOrNull { it.id == id })
        override suspend fun getBankAccountByIdDirect(id: String) = accounts.firstOrNull { it.id == id }
        override suspend fun getBankAccountByNameDirect(bankName: String) = accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountsByNameDirectList(bankName: String) = accounts.filter { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountByNameAndNumberDirect(bankName: String, maskedNumber: String) =
            accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber == maskedNumber }
        override suspend fun getBankAccountsBySuffixDirect(suffix: String) = accounts.filter { it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun getBankAccountsByNameAndSuffixDirect(bankName: String, suffix: String) =
            accounts.filter { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun insertBankAccount(account: BankAccountEntity) {}
        override suspend fun insertBankAccounts(accounts: List<BankAccountEntity>) {}
        override suspend fun updateBankAccount(account: BankAccountEntity) {}
        override suspend fun updateBalance(id: String, newBalancePaise: Long, updatedAt: Long) {}
        override suspend fun deleteBankAccount(account: BankAccountEntity) {}
        override suspend fun deleteBankAccountById(id: String) {}
        override suspend fun getBankAccountCount(): Int = accounts.size
    }

    @Test
    fun `BankAccountMatcher matches exact bank and suffix`() = runBlocking {
        val now = System.currentTimeMillis()
        val hdfcAcc = BankAccountEntity(
            id = "acc_hdfc_1",
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 1234",
            balancePaise = 5000000L,
            openingBalancePaise = 5000000L,
            isPrimary = true,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val sbiAcc = BankAccountEntity(
            id = "acc_sbi_1",
            bankName = "State Bank of India",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 5678",
            balancePaise = 2000000L,
            openingBalancePaise = 2000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(hdfcAcc, sbiAcc)))
        val matchResult = matcher.matchAccount("HDFC Bank", "1234")

        assertEquals(AccountMatchStatus.EXACT_MATCH, matchResult.status)
        assertTrue(matchResult.isAutoAssignable)
        assertEquals(hdfcAcc.id, matchResult.matchedAccount?.id)
    }

    @Test
    fun `BankAccountMatcher flags ambiguous multi-match when multiple accounts share bank and suffix`() = runBlocking {
        val now = System.currentTimeMillis()
        val acc1 = BankAccountEntity(
            id = "acc_hdfc_savings",
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 1234",
            balancePaise = 1000000L,
            openingBalancePaise = 1000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val acc2 = BankAccountEntity(
            id = "acc_hdfc_salary",
            bankName = "HDFC Bank",
            accountType = "SALARY",
            maskedAccountNumber = "•••• 1234",
            balancePaise = 2000000L,
            openingBalancePaise = 2000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(acc1, acc2)))
        val matchResult = matcher.matchAccount("HDFC Bank", "1234")

        assertEquals(AccountMatchStatus.AMBIGUOUS_MULTI_MATCH, matchResult.status)
        assertFalse("Must not auto-assign when ambiguous", matchResult.isAutoAssignable)
        assertNull(matchResult.matchedAccount)
    }

    @Test
    fun `BankAccountMatcher flags unlinked account when no accounts exist`() = runBlocking {
        val matcher = BankAccountMatcher(FakeBankAccountDao(emptyList()))
        val matchResult = matcher.matchAccount("Karur Vysya Bank", "9362")

        assertEquals(AccountMatchStatus.NO_MATCH_UNLINKED, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
        assertNull(matchResult.matchedAccount)
    }

    @Test
    fun `BankAccountMatcher flags conflicting match when bank matches but suffix differs`() = runBlocking {
        val now = System.currentTimeMillis()
        val hdfcAcc = BankAccountEntity(
            id = "acc_hdfc_1",
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9999",
            balancePaise = 5000000L,
            openingBalancePaise = 5000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(hdfcAcc)))
        val matchResult = matcher.matchAccount("HDFC Bank", "1234")

        assertEquals(AccountMatchStatus.UNKNOWN_SUFFIX_UNLINKED, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
    }

    @Test
    fun `BankAccountMatcher matches exact bank and suffix for KVB`() = runBlocking {
        val now = System.currentTimeMillis()
        val kvbAcc = BankAccountEntity(
            id = "acc_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 10000000L,
            openingBalancePaise = 10000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kvbAcc)))
        val matchResult = matcher.matchAccount("KVB", "9362")

        assertEquals(AccountMatchStatus.EXACT_MATCH, matchResult.status)
        assertTrue(matchResult.isAutoAssignable)
        assertEquals("acc_kvb_1", matchResult.matchedAccount?.id)
    }

    @Test
    fun `BankAccountMatcher resolves multiple Kotak accounts to exact suffix`() = runBlocking {
        val now = System.currentTimeMillis()
        val kotakSavings = BankAccountEntity(
            id = "acc_kotak_sav",
            bankName = "Kotak Mahindra Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 4821",
            accountNickname = "Kotak Savings",
            balancePaise = 5000000L,
            openingBalancePaise = 5000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val kotakSalary = BankAccountEntity(
            id = "acc_kotak_sal",
            bankName = "Kotak Mahindra Bank",
            accountType = "SALARY",
            maskedAccountNumber = "•••• 7710",
            accountNickname = "Kotak Salary",
            balancePaise = 15000000L,
            openingBalancePaise = 15000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kotakSavings, kotakSalary)))
        val matchResult = matcher.matchAccount("Kotak", "7710")

        assertEquals(AccountMatchStatus.EXACT_MATCH, matchResult.status)
        assertTrue(matchResult.isAutoAssignable)
        assertEquals("acc_kotak_sal", matchResult.matchedAccount?.id)
    }

    @Test
    fun `BankAccountMatcher flags INFERRED_SINGLE_ACCOUNT when bank is exact but suffix is missing and 1 account saved`() = runBlocking {
        val now = System.currentTimeMillis()
        val kvbAcc = BankAccountEntity(
            id = "acc_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 10000000L,
            openingBalancePaise = 10000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kvbAcc)))
        val matchResult = matcher.matchAccount("Karur Vysya Bank", null)

        assertEquals(AccountMatchStatus.INFERRED_SINGLE_ACCOUNT, matchResult.status)
        assertFalse("Inferred single account must strictly require user review", matchResult.isAutoAssignable)
        assertNull("Must not auto-assign matchedAccount without explicit suffix", matchResult.matchedAccount)
        assertEquals(1, matchResult.candidateAccounts.size)
    }

    @Test
    fun `BankAccountMatcher flags AMBIGUOUS_MULTI_MATCH when bank is exact but suffix is missing and multiple accounts saved`() = runBlocking {
        val now = System.currentTimeMillis()
        val kotak1 = BankAccountEntity(
            id = "acc_kotak_1",
            bankName = "Kotak Mahindra Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 4821",
            balancePaise = 5000000L,
            openingBalancePaise = 5000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val kotak2 = BankAccountEntity(
            id = "acc_kotak_2",
            bankName = "Kotak Mahindra Bank",
            accountType = "SALARY",
            maskedAccountNumber = "•••• 7710",
            balancePaise = 15000000L,
            openingBalancePaise = 15000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kotak1, kotak2)))
        val matchResult = matcher.matchAccount("Kotak Mahindra Bank", null)

        assertEquals(AccountMatchStatus.AMBIGUOUS_MULTI_MATCH, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
        assertEquals(2, matchResult.candidateAccounts.size)
    }

    @Test
    fun `BankAccountMatcher flags CONFLICTING_MATCH when notification bank contradicts suffix ownership`() = runBlocking {
        val now = System.currentTimeMillis()
        val kvbAcc = BankAccountEntity(
            id = "acc_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 10000000L,
            openingBalancePaise = 10000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val kotakAcc = BankAccountEntity(
            id = "acc_kotak_1",
            bankName = "Kotak Mahindra Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 4821",
            balancePaise = 5000000L,
            openingBalancePaise = 5000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kvbAcc, kotakAcc)))

        // Alert says Kotak, but suffix is 9362 which belongs to KVB!
        val matchResult = matcher.matchAccount("Kotak", "9362")

        assertEquals(AccountMatchStatus.CONFLICTING_MATCH, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
        assertNull(matchResult.matchedAccount)
    }

    @Test
    fun `BankAccountMatcher flags UNKNOWN_BANK_MATCH when suffix matches but bank is unverified`() = runBlocking {
        val now = System.currentTimeMillis()
        val kvbAcc = BankAccountEntity(
            id = "acc_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 10000000L,
            openingBalancePaise = 10000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kvbAcc)))
        val matchResult = matcher.matchAccount(null, "9362")

        assertEquals(AccountMatchStatus.UNKNOWN_BANK_MATCH, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
        assertEquals(1, matchResult.candidateAccounts.size)
    }

    @Test
    fun `BankAccountMatcher flags NO_MATCH_UNLINKED when both bank and suffix are unknown`() = runBlocking {
        val now = System.currentTimeMillis()
        val kvbAcc = BankAccountEntity(
            id = "acc_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 10000000L,
            openingBalancePaise = 10000000L,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val matcher = BankAccountMatcher(FakeBankAccountDao(listOf(kvbAcc)))
        val matchResult = matcher.matchAccount(null, null)

        assertEquals(AccountMatchStatus.NO_MATCH_UNLINKED, matchResult.status)
        assertFalse(matchResult.isAutoAssignable)
    }

    // -------------------------------------------------------------
    // 3b. Full Review-First Ingestion & Approval Workflow Test
    // -------------------------------------------------------------

    private class WorkflowTransactionDao : com.spendwise.android.data.db.dao.TransactionDao {
        val list = mutableListOf<com.spendwise.android.data.db.entity.TransactionEntity>()
        override fun getAllTransactions() = flowOf(list.toList())
        override fun getConfirmedTransactions() = flowOf(list.filter { it.reviewStatus == "CONFIRMED" })
        override fun getTransactionsByStatus(reviewStatus: String) = flowOf(list.filter { it.reviewStatus == reviewStatus })
        override fun getTransactionById(id: String) = flowOf(list.firstOrNull { it.id == id })
        override suspend fun getTransactionByIdDirect(id: String) = list.firstOrNull { it.id == id }
        override suspend fun getAllTransactionIds() = list.map { it.id }
        override suspend fun getConfirmedTransactionsDirect() = list.filter { it.reviewStatus == "CONFIRMED" }
        override suspend fun insertTransaction(transaction: com.spendwise.android.data.db.entity.TransactionEntity) {
            list.removeAll { it.id == transaction.id }
            list.add(transaction)
        }
        override suspend fun insertTransactions(transactions: List<com.spendwise.android.data.db.entity.TransactionEntity>) {
            list.addAll(transactions)
        }
        override suspend fun updateTransaction(transaction: com.spendwise.android.data.db.entity.TransactionEntity) {
            val idx = list.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) list[idx] = transaction
        }
        override suspend fun deleteTransaction(transaction: com.spendwise.android.data.db.entity.TransactionEntity) {
            list.removeAll { it.id == transaction.id }
        }
        override suspend fun deleteTransactionById(id: String) {
            list.removeAll { it.id == id }
        }
        override suspend fun deleteAllTransactions() {
            list.clear()
        }
        override fun getTodayDebitsPaise(startOfDay: Long, endOfDay: Long) = flowOf(0L)
        override fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long) = flowOf(0L)
        override fun getPeriodDebitsPaise(startOfPeriod: Long) = flowOf(0L)
        override suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long) = 0L
        override suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long) = 0L
        override fun getPeriodIncomeCredits(startOfPeriod: Long) = flowOf(0L)
        override fun getPeriodRefundCredits(startOfPeriod: Long) = flowOf(0L)
        override fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<com.spendwise.android.data.db.entity.CategorySpending>> = flowOf(emptyList())
        override fun getPendingReviewCount(): Flow<Int> = flowOf(list.count { it.reviewStatus == "NEEDS_REVIEW" })
        override suspend fun getPendingReviewCountDirect(): Int = list.count { it.reviewStatus == "NEEDS_REVIEW" }
        override suspend fun findPotentialDuplicates(amountPaise: Long, direction: String, minTime: Long, maxTime: Long): List<com.spendwise.android.data.db.entity.TransactionEntity> = emptyList()
        override suspend fun getTransactionByDedupeKey(dedupeKey: String): com.spendwise.android.data.db.entity.TransactionEntity? = list.firstOrNull { it.dedupeKey == dedupeKey }
        override suspend fun getTransactionCountForAccount(bankAccountId: String): Int = list.count { it.bankAccountId == bankAccountId }
        override fun searchTransactions(query: String): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" })
        override fun getTransactionsByDirection(direction: String): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.direction == direction })
        override fun getTransactionsByCategory(categoryId: String): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.categoryId == categoryId })
        override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" && it.occurredAtEpochMillis in startTime..endTime })
        override fun getTransactionsForBank(bankAccountName: String): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" && (it.bankAccountName == bankAccountName || it.bankAccountId == bankAccountName) })
        override fun getTransactionsForMerchant(merchantQuery: String): Flow<List<com.spendwise.android.data.db.entity.TransactionEntity>> = flowOf(list.filter { it.reviewStatus == "CONFIRMED" && (it.merchant?.contains(merchantQuery, ignoreCase = true) == true) })
        override suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long = list.filter { it.reviewStatus == "CONFIRMED" && it.direction == "DEBIT" && (it.merchant?.contains(merchantQuery, ignoreCase = true) == true) }.sumOf { it.amountPaise }
        override suspend fun updateReviewStatus(id: String, newStatus: String, updatedAt: Long) {
            val existing = list.firstOrNull { it.id == id } ?: return
            val idx = list.indexOf(existing)
            list[idx] = existing.copy(reviewStatus = newStatus, updatedAtEpochMillis = updatedAt)
        }
    }

    private class WorkflowBankAccountDao(
        val accounts: MutableList<BankAccountEntity>
    ) : BankAccountDao {
        override fun getAllBankAccounts() = flowOf(accounts.toList())
        override suspend fun getAllBankAccountsDirect() = accounts.toList()
        override fun getBankAccountById(id: String) = flowOf(accounts.firstOrNull { it.id == id })
        override suspend fun getBankAccountByIdDirect(id: String) = accounts.firstOrNull { it.id == id }
        override suspend fun getBankAccountByNameDirect(bankName: String) = accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountsByNameDirectList(bankName: String) = accounts.filter { it.bankName.equals(bankName, ignoreCase = true) }
        override suspend fun getBankAccountByNameAndNumberDirect(bankName: String, maskedNumber: String) =
            accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber == maskedNumber }
        override suspend fun getBankAccountsBySuffixDirect(suffix: String) = accounts.filter { it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun getBankAccountsByNameAndSuffixDirect(bankName: String, suffix: String) =
            accounts.filter { it.bankName.equals(bankName, ignoreCase = true) && it.maskedAccountNumber.endsWith(suffix) }
        override suspend fun insertBankAccount(account: BankAccountEntity) { accounts.add(account) }
        override suspend fun insertBankAccounts(accounts: List<BankAccountEntity>) { this.accounts.addAll(accounts) }
        override suspend fun updateBankAccount(account: BankAccountEntity) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) accounts[idx] = account
        }
        override suspend fun updateBalance(id: String, newBalancePaise: Long, updatedAt: Long) {
            val existing = accounts.firstOrNull { it.id == id } ?: return
            val idx = accounts.indexOf(existing)
            accounts[idx] = existing.copy(balancePaise = newBalancePaise)
        }
        override suspend fun deleteBankAccount(account: BankAccountEntity) { accounts.remove(account) }
        override suspend fun deleteBankAccountById(id: String) { accounts.removeAll { it.id == id } }
        override suspend fun getBankAccountCount(): Int = accounts.size
    }

    @Test
    fun `Full workflow Notification to Ingestion to NEEDS_REVIEW to Approval to Balance Updated`() = runBlocking {
        val now = System.currentTimeMillis()
        val initialBalancePaise = 1000000L // ₹10,000.00
        val kvbAccount = BankAccountEntity(
            id = "acc_kvb_live",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = initialBalancePaise,
            openingBalancePaise = initialBalancePaise,
            isPrimary = true,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val txDao = WorkflowTransactionDao()
        val bankDao = WorkflowBankAccountDao(mutableListOf(kvbAccount))

        val engine = com.spendwise.android.notification.NotificationIngestionEngine(
            transactionDao = txDao,
            bankAccountDao = bankDao
        )

        // Step 1 & 2: Receive representative notification
        val payload = RawNotificationPayload(
            packageName = "com.kvb.dlite",
            title = "KVBANK",
            text = "Your KVB A/c XXXXXXXXXXXX9362 is debited by Rs. 50.00 on 24-Oct-26 to Chai Point. Avl Bal Rs. 9,950.00. Help 18005721916-KVB",
            postTimeEpochMs = now
        )

        engine.onNotificationReceived(payload)

        // Step 3: Verify created pending transaction
        assertEquals(1, txDao.list.size)
        val pendingTx = txDao.list.first()
        assertEquals("NEEDS_REVIEW", pendingTx.reviewStatus)
        assertEquals(5000L, pendingTx.amountPaise)
        assertEquals("Chai Point", pendingTx.merchant)
        assertEquals("acc_kvb_live", pendingTx.bankAccountId)

        // Step 4: Verify zero balance impact while pending
        val bankBeforeApproval = bankDao.getBankAccountByIdDirect("acc_kvb_live")
        assertNotNull(bankBeforeApproval)
        assertEquals("Balance must NOT change during ingestion or while pending",
            initialBalancePaise, bankBeforeApproval!!.balancePaise)

        // Step 5: User explicitly approves transaction
        val delta = if (pendingTx.direction == "DEBIT") -pendingTx.amountPaise else pendingTx.amountPaise
        txDao.updateReviewStatus(pendingTx.id, "CONFIRMED")
        bankDao.updateBalance("acc_kvb_live", bankBeforeApproval.balancePaise + delta, System.currentTimeMillis())

        // Step 6: Verify confirmed transaction and updated balance
        val confirmedTx = txDao.getTransactionByIdDirect(pendingTx.id)
        assertNotNull(confirmedTx)
        assertEquals("CONFIRMED", confirmedTx!!.reviewStatus)

        val bankAfterApproval = bankDao.getBankAccountByIdDirect("acc_kvb_live")
        assertNotNull(bankAfterApproval)
        assertEquals(995000L, bankAfterApproval!!.balancePaise) // ₹9,950.00
    }

    // -------------------------------------------------------------
    // 4. SpendoNotificationPersonality Tone Tests
    // -------------------------------------------------------------

    @Test
    fun `SpendoNotificationPersonality generates distinctive copies per tone`() {
        val wittyCopy = SpendoNotificationPersonality.getTransactionQueuedCopy(
            tone = NotificationPersonalityTone.WITTY,
            merchant = "Swiggy",
            amountRupees = 250.0,
            direction = "DEBIT"
        )
        assertTrue(wittyCopy.body.contains("Swiggy"))
        assertTrue(wittyCopy.body.contains("₹250.00"))

        val friendlyCopy = SpendoNotificationPersonality.getTransactionQueuedCopy(
            tone = NotificationPersonalityTone.FRIENDLY,
            merchant = "Swiggy",
            amountRupees = 250.0,
            direction = "DEBIT"
        )
        assertTrue(friendlyCopy.title.contains("New expense"))

        val minimalCopy = SpendoNotificationPersonality.getTransactionQueuedCopy(
            tone = NotificationPersonalityTone.MINIMAL,
            merchant = "Swiggy",
            amountRupees = 250.0,
            direction = "DEBIT"
        )
        assertEquals("Expense Pending Review", minimalCopy.title)
        assertEquals("₹250.00 · Swiggy", minimalCopy.body)
    }

    @Test
    fun `SpendoNotificationPersonality morning and evening reminders format correctly`() {
        val morningWitty = SpendoNotificationPersonality.getMorningReminderCopy(NotificationPersonalityTone.WITTY, 3)
        assertTrue(morningWitty.body.contains("3 expenses"))

        val eveningFriendly = SpendoNotificationPersonality.getEveningReminderCopy(NotificationPersonalityTone.FRIENDLY, 1)
        assertTrue(eveningFriendly.body.contains("1 transaction"))
    }
}
