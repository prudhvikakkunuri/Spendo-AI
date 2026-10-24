package com.spendwise.android.ui.demo

import com.spendwise.android.domain.model.ReviewStatus
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import java.util.concurrent.TimeUnit

/**
 * Clearly identified Demo/Mock data for Phase 1 UI and Navigation previews.
 * Separated strictly from production persistence logic.
 * Monetary amounts are represented strictly in integer paise (Long).
 */
data class DemoTransaction(
    val id: String,
    val amountPaise: Long,
    val direction: TransactionDirection,
    val merchant: String,
    val note: String? = null,
    val category: TransactionCategory,
    val occurredAtEpochMillis: Long,
    val sourceType: SourceType,
    val sourcePackage: String? = null,
    val reviewStatus: ReviewStatus = ReviewStatus.CONFIRMED
)

data class DemoReviewCandidate(
    val id: String,
    val amountPaise: Long?,
    val direction: TransactionDirection,
    val sourcePackage: String,
    val sourceAppName: String,
    val postedAtEpochMillis: Long,
    val confidenceScore: Float,
    val uncertaintyReason: String,
    val suggestedMerchant: String?
)

data class DemoMonitoredApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val category: String
)

object DemoData {
    private val now = System.currentTimeMillis()
    private val oneHour = TimeUnit.HOURS.toMillis(1)
    private val oneDay = TimeUnit.DAYS.toMillis(1)

    val sampleTransactions = listOf(
        DemoTransaction(
            id = "tx_1",
            amountPaise = 35000L, // ₹350.00
            direction = TransactionDirection.DEBIT,
            merchant = "Blue Tokai Coffee",
            note = "Cold brew & croissant",
            category = TransactionCategory.FOOD,
            occurredAtEpochMillis = now - (2 * oneHour),
            sourceType = SourceType.NOTIFICATION,
            sourcePackage = "com.google.android.apps.nbu.paisa.user"
        ),
        DemoTransaction(
            id = "tx_2",
            amountPaise = 18500L, // ₹185.00
            direction = TransactionDirection.DEBIT,
            merchant = "Uber India",
            note = "Commute to office",
            category = TransactionCategory.TRANSPORT,
            occurredAtEpochMillis = now - (5 * oneHour),
            sourceType = SourceType.NOTIFICATION,
            sourcePackage = "net.one97.paytm"
        ),
        DemoTransaction(
            id = "tx_3",
            amountPaise = 149900L, // ₹1,499.00
            direction = TransactionDirection.DEBIT,
            merchant = "Amazon India",
            note = "Books & desk accessories",
            category = TransactionCategory.SHOPPING,
            occurredAtEpochMillis = now - oneDay,
            sourceType = SourceType.MANUAL
        ),
        DemoTransaction(
            id = "tx_4",
            amountPaise = 12500000L, // ₹1,25,000.00
            direction = TransactionDirection.CREDIT,
            merchant = "Acme Corp Tech",
            note = "Monthly salary credit",
            category = TransactionCategory.INCOME,
            occurredAtEpochMillis = now - (3 * oneDay),
            sourceType = SourceType.MANUAL
        ),
        DemoTransaction(
            id = "tx_5",
            amountPaise = 129900L, // ₹1,299.00
            direction = TransactionDirection.DEBIT,
            merchant = "Airtel Broadband",
            note = "Fiber internet bill",
            category = TransactionCategory.BILLS,
            occurredAtEpochMillis = now - (4 * oneDay),
            sourceType = SourceType.NOTIFICATION,
            sourcePackage = "com.phonepe.app"
        )
    )

    val sampleReviewCandidates = listOf(
        DemoReviewCandidate(
            id = "rev_1",
            amountPaise = 84000L, // ₹840.00
            direction = TransactionDirection.DEBIT,
            sourcePackage = "com.phonepe.app",
            sourceAppName = "PhonePe",
            postedAtEpochMillis = now - (45 * 60 * 1000L),
            confidenceScore = 0.72f,
            uncertaintyReason = "Merchant name ambiguous; matched secondary payment reference",
            suggestedMerchant = "Star Bazaar Supermarket"
        ),
        DemoReviewCandidate(
            id = "rev_2",
            amountPaise = 250000L, // ₹2,500.00
            direction = TransactionDirection.DEBIT,
            sourcePackage = "net.one97.paytm",
            sourceAppName = "Paytm",
            postedAtEpochMillis = now - (3 * oneHour),
            confidenceScore = 0.68f,
            uncertaintyReason = "Multiple monetary values detected; potential balance statement",
            suggestedMerchant = "PVR Inox Cinemas"
        )
    )

    val sampleMonitoredApps = listOf(
        DemoMonitoredApp(
            packageName = "com.google.android.apps.nbu.paisa.user",
            appName = "Google Pay",
            isEnabled = true,
            category = "UPI & Payments"
        ),
        DemoMonitoredApp(
            packageName = "com.phonepe.app",
            appName = "PhonePe",
            isEnabled = true,
            category = "UPI & Payments"
        ),
        DemoMonitoredApp(
            packageName = "net.one97.paytm",
            appName = "Paytm",
            isEnabled = true,
            category = "UPI & Payments"
        ),
        DemoMonitoredApp(
            packageName = "in.org.npci.upiapp",
            appName = "BHIM UPI",
            isEnabled = false,
            category = "UPI"
        ),
        DemoMonitoredApp(
            packageName = "com.hdfcbank.payzapp",
            appName = "HDFC Bank / PayZapp",
            isEnabled = false,
            category = "Banking"
        ),
        DemoMonitoredApp(
            packageName = "com.icicibank.mobile",
            appName = "iMobile ICICI Bank",
            isEnabled = false,
            category = "Banking"
        )
    )

    // Quick Stats Summary for preview
    val todaySpentPaise = 53500L // ₹535.00
    val todayReceivedPaise = 0L // ₹0.00
    val monthSpendPaise = 428000L // ₹4,280.00
    val safeToSpendTodayPaise = 185000L // ₹1,850.00
}
