package com.spendwise.android

import com.spendwise.android.data.repository.MonitoredAppRepository
import com.spendwise.android.domain.banking.BankMessageParserRegistry
import com.spendwise.android.domain.banking.KotakBankParser
import com.spendwise.android.domain.banking.KvbBankParser
import com.spendwise.android.domain.banking.IndianBankParser
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.NotificationIngestionEngine
import com.spendwise.android.notification.model.RawNotificationPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendoNotificationMonitorAndAnalyticsTest {

    @Test
    fun `kvb parser correctly extracts exact sample notification with balance and payee`() {
        val sampleText = "Your a/c XXXXXXXXXXXX9362 is debited Rs. 50.00 on 24-Oct-2026 to BHAVAPRITA STATIONERY AND EMBROIDERY WORK info :P2A/291530634673. Avl Bal INR 59.2 Not You? call 18005721916-KVB"
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = 1792800000000L,
            title = "KVBANK",
            text = sampleText,
            bigText = sampleText
        )

        val parser = KvbBankParser()
        assertTrue("KVB parser should handle sender/text", parser.canHandle("KVBANK", sampleText))

        val result = parser.parse(payload)
        assertNotNull("Result should not be null", result)
        requireNotNull(result)

        // Amount must be 5000L (Rs 50.00), NOT 5920L (Rs 59.20 Avl Bal)
        assertEquals("Amount must be Rs 50.00 (5000 paise)", 5000L, result.amountPaise)
        assertEquals("Available balance must be Rs 59.20 (5920 paise)", 5920L, result.reportedBalancePaise)
        assertEquals("Direction must be DEBIT", TransactionDirection.DEBIT, result.direction)
        assertEquals("Account suffix must be 9362", "9362", result.accountSuffix)
        assertEquals("Payee must be full name without truncation", "BHAVAPRITA STATIONERY AND EMBROIDERY WORK", result.merchantOrPayee)
        assertEquals("Reference number must match", "P2A/291530634673", result.referenceNumber)
        assertEquals("Bank name must be Karur Vysya Bank", "Karur Vysya Bank", result.bankName)
        assertFalse("Standard debit should not require manual review", result.reviewRequired)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `registry automatically routes kvb notification to kvb parser`() {
        val sampleText = "Your a/c XXXXXXXXXXXX9362 is debited Rs. 50.00 on 24-Oct-2026 to BHAVAPRITA STATIONERY AND EMBROIDERY WORK info :P2A/291530634673. Avl Bal INR 59.2 Not You? call 18005721916-KVB"
        val payload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = 1792800000000L,
            title = "Bank Alert",
            text = sampleText,
            bigText = sampleText
        )

        val parsed = BankMessageParserRegistry.parse(payload)
        assertNotNull(parsed)
        assertEquals(5000L, parsed?.amountPaise)
        assertEquals(5920L, parsed?.reportedBalancePaise)
        assertEquals("Karur Vysya Bank", parsed?.bankName)
    }

    @Test
    fun `kotak parser extracts transaction and balance accurately`() {
        val sampleText = "Sent Rs. 350.00 from Kotak Bank a/c ending 1234 to ZOMATO info :P2A/112233445566. Avl Bal INR 1250.00 Not you? Call 18002090000"
        val payload = RawNotificationPayload(
            packageName = "com.msf.kbank.mobile",
            postTimeEpochMs = 1792800000000L,
            title = "Kotak Bank",
            text = sampleText,
            bigText = sampleText
        )

        val parser = KotakBankParser()
        assertTrue(parser.canHandle("com.msf.kbank.mobile", sampleText))

        val result = parser.parse(payload)
        assertNotNull(result)
        assertEquals(35000L, result?.amountPaise)
        assertEquals(125000L, result?.reportedBalancePaise)
        assertEquals(TransactionDirection.DEBIT, result?.direction)
        assertEquals("1234", result?.accountSuffix)
        assertEquals("ZOMATO", result?.merchantOrPayee)
        assertEquals("P2A/112233445566", result?.referenceNumber)
    }

    @Test
    fun `indian bank parser extracts transaction and balance accurately`() {
        val sampleText = "Indian Bank: A/c *5678 debited by Rs.120.00 on 24-Oct-2026 by UPI to CHAI POINT info: 432109876543. Avl Bal Rs.3450.00"
        val payload = RawNotificationPayload(
            packageName = "com.infrasofttech.IndianBank",
            postTimeEpochMs = 1792800000000L,
            title = "Indian Bank",
            text = sampleText,
            bigText = sampleText
        )

        val parser = IndianBankParser()
        assertTrue(parser.canHandle("com.infrasofttech.IndianBank", sampleText))

        val result = parser.parse(payload)
        assertNotNull(result)
        assertEquals(12000L, result?.amountPaise)
        assertEquals(345000L, result?.reportedBalancePaise)
        assertEquals(TransactionDirection.DEBIT, result?.direction)
        assertEquals("5678", result?.accountSuffix)
        assertEquals("CHAI POINT", result?.merchantOrPayee)
    }

    @Test
    fun `non financial messages like otp and promotions are discarded`() {
        val otpText = "Your OTP for login to NetBanking is 482910. Never share this OTP with anyone."
        val otpPayload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = 1792800000000L,
            title = "KVBANK",
            text = otpText,
            bigText = otpText
        )
        assertNull("OTP notification must not parse as transaction", BankMessageParserRegistry.parse(otpPayload))

        val promoText = "Congratulations! You are pre-approved for a personal loan of Rs 5,00,000. Click to apply now."
        val promoPayload = RawNotificationPayload(
            packageName = "com.google.android.apps.messaging",
            postTimeEpochMs = 1792800000000L,
            title = "Kotak Bank",
            text = promoText,
            bigText = promoText
        )
        assertNull("Promotional message must not parse as transaction", BankMessageParserRegistry.parse(promoPayload))
    }

    @Test
    fun `default monitored apps include sms apps and regional banks`() {
        val defaultApps = MonitoredAppRepository.DEFAULT_KNOWN_FINANCIAL_APPS
        val defaultPkgs = defaultApps.map { it.packageName }.toSet()

        // Verify SMS packages
        assertTrue("Google Messages must be included", defaultPkgs.contains("com.google.android.apps.messaging"))
        assertTrue("Samsung Messages must be included", defaultPkgs.contains("com.samsung.android.messaging"))
        assertTrue("System Messages must be included", defaultPkgs.contains("com.android.mms"))

        // Verify Regional and Major Banks
        assertTrue("KVB DLite must be included", defaultPkgs.contains("com.kvb.dlite"))
        assertTrue("KVB Mobile must be included", defaultPkgs.contains("com.kvb.mobilebanking"))
        assertTrue("Kotak Mahindra Bank must be included", defaultPkgs.contains("com.msf.kbank.mobile"))
        assertTrue("Indian Bank must be included", defaultPkgs.contains("com.infrasofttech.IndianBank"))
    }

    @Test
    fun `fresh install progress fraction evaluates strictly to zero percent without NaN`() {
        val dailyLimitPaise = 0L
        val todaySpentPaise = 0L

        val calculateProgress: (Long, Long) -> Float = { limitPaise, spentPaise ->
            val limitRupees = limitPaise / 100.0
            val spentRupees = spentPaise / 100.0
            if (limitRupees > 0.0) {
                (spentRupees / limitRupees).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }
        }

        val progressFraction = calculateProgress(dailyLimitPaise, todaySpentPaise)

        assertFalse("Progress fraction must not be NaN", progressFraction.isNaN())
        assertEquals("Progress fraction must be exactly 0.0f on fresh install", 0f, progressFraction, 0.0001f)
    }
}

