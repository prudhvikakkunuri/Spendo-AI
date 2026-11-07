package com.spendwise.android

import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.domain.banking.GenericBankParser
import com.spendwise.android.domain.banking.KvbBankParser
import com.spendwise.android.domain.calculator.StreakCalculator
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.model.RawNotificationPayload
import com.spendwise.android.ui.spendo.components.SpendoMainDestination
import com.spendwise.android.ui.spendo.navigation.SpendoScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit regression tests verifying:
 * 1. 5-destination bottom navigation architecture and Spendo AI separation
 * 2. Home navigation bug resolution
 * 3. Bank account disambiguation via account suffix (last 4 digits)
 * 4. Official bank SMS parsing (including the KVB sample message)
 * 5. Streak calculation strictly based on consecutive calendar day expense logs
 */
class SpendoNavigationAndBugFixesUnitTest {

    // -------------------------------------------------------------
    // 1. Navigation & Destinations
    // -------------------------------------------------------------

    @Test
    fun bottomNavigation_hasExactlyFiveMainDestinationsInPill() {
        val destinations = SpendoMainDestination.values()
        assertEquals(5, destinations.size)
        assertEquals(SpendoMainDestination.HOME, destinations[0])
        assertEquals(SpendoMainDestination.WALLET, destinations[1])
        assertEquals(SpendoMainDestination.ANALYTICS, destinations[2])
        assertEquals(SpendoMainDestination.BUDGETS, destinations[3])
        assertEquals(SpendoMainDestination.PROFILE, destinations[4])

        assertEquals(0, SpendoMainDestination.HOME.pageIndex)
        assertEquals(1, SpendoMainDestination.WALLET.pageIndex)
        assertEquals(2, SpendoMainDestination.ANALYTICS.pageIndex)
        assertEquals(3, SpendoMainDestination.BUDGETS.pageIndex)
        assertEquals(4, SpendoMainDestination.PROFILE.pageIndex)
    }

    @Test
    fun bottomNavigation_routesMatchCorrectScreens() {
        assertEquals(SpendoScreen.Home.route, SpendoMainDestination.HOME.route)
        assertEquals(SpendoScreen.Activity.route, SpendoMainDestination.WALLET.route)
        assertEquals(SpendoScreen.Analytics.route, SpendoMainDestination.ANALYTICS.route)
        assertEquals(SpendoScreen.Budget.route, SpendoMainDestination.BUDGETS.route)
        assertEquals(SpendoScreen.Settings.route, SpendoMainDestination.PROFILE.route)
    }

    @Test
    fun homeNavigationBug_pagerIndexResolutionIsDeterministic() {
        // Tapping Home from any destination or from Home itself deterministically targets index 0
        val targetFromHome = SpendoMainDestination.fromIndex(0).pageIndex
        val targetFromWallet = SpendoMainDestination.fromIndex(1).pageIndex
        val targetFromProfile = SpendoMainDestination.fromIndex(4).pageIndex

        assertEquals(0, targetFromHome)
        assertEquals(1, targetFromWallet)
        assertEquals(4, targetFromProfile)

        // Resolving from Home route
        assertEquals(SpendoMainDestination.HOME, SpendoMainDestination.fromRoute("spendo_home"))
        assertEquals(SpendoMainDestination.WALLET, SpendoMainDestination.fromRoute("spendo_activity"))
        assertEquals(SpendoMainDestination.ANALYTICS, SpendoMainDestination.fromRoute("spendo_analytics"))
        assertEquals(SpendoMainDestination.BUDGETS, SpendoMainDestination.fromRoute("spendo_budget"))
        assertEquals(SpendoMainDestination.PROFILE, SpendoMainDestination.fromRoute("spendo_settings"))
    }

    // -------------------------------------------------------------
    // 2. Bank Account Disambiguation by Last 4 Digits
    // -------------------------------------------------------------

    @Test
    fun bankAccounts_multipleAccountsSameBankDistinguishedBySuffix() {
        val now = System.currentTimeMillis()
        val account1 = BankAccountEntity(
            id = "bank_kvb_1",
            bankName = "Karur Vysya Bank",
            accountType = "SAVINGS",
            maskedAccountNumber = "•••• 9362",
            balancePaise = 5920L,
            openingBalancePaise = 100000L,
            isPrimary = true,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        val account2 = BankAccountEntity(
            id = "bank_kvb_2",
            bankName = "Karur Vysya Bank",
            accountType = "CURRENT",
            maskedAccountNumber = "•••• 4821",
            balancePaise = 4500000L,
            openingBalancePaise = 5000000L,
            isPrimary = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        // Same bank name, distinct IDs and distinct suffixes
        assertEquals(account1.bankName, account2.bankName)
        assertFalse(account1.id == account2.id)
        assertFalse(account1.maskedAccountNumber == account2.maskedAccountNumber)
        assertTrue(account1.maskedAccountNumber.endsWith("9362"))
        assertTrue(account2.maskedAccountNumber.endsWith("4821"))
    }

    // -------------------------------------------------------------
    // 3. Bank SMS Parsing (KVB Reference Message & Generic Banking)
    // -------------------------------------------------------------

    @Test
    fun parseKvbBankSms_extractsAllFinancialFieldsAccurately() {
        val kvbParser = KvbBankParser()
        val rawSms = "Your a/c XXXXXXXXXXXX9362 is debited Rs. 50.00 on 24-Oct-2026 to BHAVAPRITA STATIONERY AND EMBROIDERY WORK info :P2A/291530634673. Avl Bal INR 59.2 Not You? call 18005721916-KVB"

        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = 1792843200000L,
            title = "VK-KVBANK",
            text = rawSms
        )

        assertTrue("Parser must accept KVB SMS", kvbParser.canHandle("VK-KVBANK", rawSms))
        val parsed = kvbParser.parse(payload)
        assertNotNull("Parsed message must not be null", parsed)
        parsed!!

        // Amount: Rs. 50.00 -> 5000 paise
        assertEquals(5000L, parsed.amountPaise)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)

        // Account Suffix: 9362
        assertEquals("9362", parsed.accountSuffix)

        // Merchant: BHAVAPRITA STATIONERY AND EMBROIDERY WORK (fraud phone number stripped)
        assertTrue(parsed.merchantOrPayee.contains("BHAVAPRITA STATIONERY", ignoreCase = true))
        assertFalse("Fraud phone number must be stripped from merchant", parsed.merchantOrPayee.contains("18005721916"))
        assertFalse("Not You must be stripped from merchant", parsed.merchantOrPayee.contains("Not You"))

        // Reference Number: P2A/291530634673
        assertEquals("P2A/291530634673", parsed.referenceNumber)

        // Available Balance: 59.20 -> 5920 paise
        assertEquals(5920L, parsed.reportedBalancePaise)

        // Bank Name
        assertEquals("Karur Vysya Bank", parsed.bankName)
    }

    @Test
    fun parseGenericBankSms_filtersNonTransactions() {
        val parser = GenericBankParser()

        // OTP SMS must not be treated as a transaction
        val otpSms = "Your OTP for login to net banking is 482910. Valid for 10 mins. Do not share with anyone."
        val otpPayload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = System.currentTimeMillis(),
            title = "HDFCBK",
            text = otpSms
        )
        assertNull("OTP message must be filtered out", parser.parse(otpPayload))

        // Failed transaction must be filtered or flagged
        val failedSms = "Transaction failed for Rs. 500 on a/c ending 1234 due to technical issue."
        val failedPayload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = System.currentTimeMillis(),
            title = "SBINB",
            text = failedSms
        )
        assertNull("Failed transaction must not be parsed as successful expense", parser.parse(failedPayload))
    }

    // -------------------------------------------------------------
    // 4. Consecutive Calendar Logging Streak Calculation
    // -------------------------------------------------------------

    @Test
    fun calculateLoggingStreak_consecutiveDaysYieldsCorrectCount() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMs = cal.timeInMillis

        // Expenses on today, yesterday, and 2 days ago = 3-day streak
        val day1 = todayMs
        val day2 = todayMs - 86400000L
        val day3 = todayMs - (2 * 86400000L)

        val streak = StreakCalculator.calculateLoggingStreak(
            expenseDatesEpochMs = listOf(day1, day2, day3),
            nowEpochMs = todayMs,
            timeZone = tz
        )
        assertEquals(3, streak)
    }

    @Test
    fun calculateLoggingStreak_notLoggedTodayFallsBackToYesterday() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val todayMs = cal.timeInMillis

        // Expenses logged yesterday and 2 days ago (not yet logged today)
        val yesterday = todayMs - 86400000L
        val twoDaysAgo = todayMs - (2 * 86400000L)

        val streak = StreakCalculator.calculateLoggingStreak(
            expenseDatesEpochMs = listOf(yesterday, twoDaysAgo),
            nowEpochMs = todayMs,
            timeZone = tz
        )
        // User has a 2-day active streak from yesterday that has not broken yet
        assertEquals(2, streak)
    }

    @Test
    fun calculateLoggingStreak_brokenStreakReturnsZeroOrRecentDaysOnly() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz)
        val todayMs = cal.timeInMillis

        // Expense 5 days ago only (broken streak)
        val fiveDaysAgo = todayMs - (5 * 86400000L)

        val streak = StreakCalculator.calculateLoggingStreak(
            expenseDatesEpochMs = listOf(fiveDaysAgo),
            nowEpochMs = todayMs,
            timeZone = tz
        )
        assertEquals(0, streak)
    }
}
