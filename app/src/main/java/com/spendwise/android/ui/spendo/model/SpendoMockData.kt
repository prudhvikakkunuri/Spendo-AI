package com.spendwise.android.ui.spendo.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionType {
    EXPENSE, INCOME
}

enum class TransactionSource {
    MANUAL, NOTIFICATION
}

data class SpendoTransaction(
    val id: String,
    val merchant: String,
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val occurredAtEpochMs: Long,
    val bankAccountName: String,
    val source: TransactionSource,
    val note: String? = null,
    val reviewStatus: String = "CONFIRMED",
    val reviewReason: String? = null,
    val bankAccountId: String? = null
) {
    val amountRupees: Double get() = amountPaise / 100.0
    val formattedAmount: String get() = "₹%,.2f".format(Locale.ENGLISH, amountRupees)
    val formattedDate: String get() {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(occurredAtEpochMs))
    }
}

enum class BalanceSource {
    REPORTED,   // Direct bank snapshot from message/statement
    CALCULATED, // Derived from opening balance + posted ledger transactions
    ESTIMATED   // Incomplete transaction history
}

data class SpendoBank(
    val id: String,
    val bankName: String,
    val accountType: String,
    val maskedAccountNumber: String,
    val balancePaise: Long,
    val isPrimary: Boolean = false,
    val balanceSource: BalanceSource = BalanceSource.CALCULATED,
    val lastUpdatedText: String = "Updated recently",
    val recentTransactionSummary: String? = null,
    val accountNickname: String? = null,
    val notes: String? = null
) {
    val displayName: String get() = if (!accountNickname.isNullOrBlank()) "$bankName ($accountNickname)" else bankName
    val balanceRupees: Double get() = balancePaise / 100.0
    val formattedBalance: String get() = "₹%,.2f".format(Locale.ENGLISH, balanceRupees)
}

data class SpendoNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: String, // "Transactions", "Budgets", "Motivation"
    val timestampText: String,
    val progress: Float? = null,
    val isUnread: Boolean = true
)

data class SpendoIncomeItem(
    val id: String,
    val title: String,
    val amountPaise: Long,
    val isRecurring: Boolean,
    val frequency: String,
    val nextPayDate: String
) {
    val formattedAmount: String get() = "₹%,.2f".format(Locale.ENGLISH, amountPaise / 100.0)
}

data class SpendoStreakData(
    val currentStreakDays: Int = 5,
    val bestStreakDays: Int = 14,
    val dailyAdherence: List<Boolean> = listOf(true, true, true, true, true, false, true),
    val motivationalQuote: String = "5 consecutive days within budget. Your financial discipline is sharp."
)

data class PendingExpenseConfirmation(
    val amountPaise: Long,
    val merchant: String,
    val category: String,
    val bankName: String
) {
    val formattedAmount: String get() = "₹%,.2f".format(Locale.ENGLISH, amountPaise / 100.0)
}

data class SpendoSavingsGoal(
    val id: String,
    val title: String,
    val targetPaise: Long,
    val savedPaise: Long,
    val targetDateText: String = "Dec 2026",
    val iconName: String = "SAVINGS"
) {
    val progress: Float get() = if (targetPaise > 0L) (savedPaise.toFloat() / targetPaise.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedTarget: String get() = "₹%,.2f".format(Locale.ENGLISH, targetPaise / 100.0)
    val formattedSaved: String get() = "₹%,.2f".format(Locale.ENGLISH, savedPaise / 100.0)
    val percentageText: String get() = "${(progress * 100).toInt()}%"
}

enum class AffordabilityVerdict {
    AFFORDABLE,
    CAUTION,
    RISKY
}

data class AffordabilityEvaluation(
    val itemAmountPaise: Long,
    val liquidBalancePaise: Long,
    val upcomingBillsPaise: Long,
    val plannedSavingsPaise: Long,
    val freeDiscretionaryPaise: Long,
    val monthlyRemainingPaise: Long,
    val verdict: AffordabilityVerdict,
    val reasonText: String
)

data class SpendoChatMessage(
    val id: String,
    val sender: String, // "USER" or "SPENDO"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActionable: Boolean = false,
    val pendingExpense: PendingExpenseConfirmation? = null
)

enum class AiProvider(val displayName: String, val defaultModel: String) {
    GROQ("Groq", "llama-3.3-70b-versatile"),
    CEREBRAS("Cerebras", "llama3.1-70b"),
    GEMINI("Google Gemini", "gemini-1.5-flash"),
    SARVAM("Sarvam AI", "sarvam-2b-v0.5"),
    OPENAI("OpenAI", "gpt-4o-mini"),
    META("Meta AI (Hosted)", "meta-llama-3.1-405b")
}

data class AiConfigState(
    val isConfigured: Boolean = false,
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String = "",
    val model: String = AiProvider.GROQ.defaultModel,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null
)

object SpendoMockRepository {

    val sampleBanks = listOf(
        SpendoBank(
            id = "bank_1",
            bankName = "Indian Bank",
            accountType = "Salary Account",
            maskedAccountNumber = "•••• 4892",
            balancePaise = 5240000L,
            isPrimary = true,
            balanceSource = BalanceSource.REPORTED,
            lastUpdatedText = "Reported from SMS (12m ago)",
            recentTransactionSummary = "Last: Swiggy ₹450.00"
        ),
        SpendoBank(
            id = "bank_2",
            bankName = "Kotak Mahindra Bank",
            accountType = "Smart Savings",
            maskedAccountNumber = "•••• 1920",
            balancePaise = 2850050L,
            isPrimary = false,
            balanceSource = BalanceSource.CALCULATED,
            lastUpdatedText = "Calculated from ledger",
            recentTransactionSummary = "Last: Uber ₹420.00"
        ),
        SpendoBank(
            id = "bank_3",
            bankName = "Karur Vysya Bank",
            accountType = "Branch Account (KVB)",
            maskedAccountNumber = "•••• 7731",
            balancePaise = 1520000L,
            isPrimary = false,
            balanceSource = BalanceSource.CALCULATED,
            lastUpdatedText = "Calculated from ledger",
            recentTransactionSummary = "Last: ATM Cash ₹2,000"
        )
    )

    val sampleTransactions = mutableListOf(
        SpendoTransaction(
            id = "tx_1",
            merchant = "Blue Tokai Coffee",
            amountPaise = 34000L,
            type = TransactionType.EXPENSE,
            category = "Food & Drink",
            occurredAtEpochMs = System.currentTimeMillis() - 3600000L,
            bankAccountName = "HDFC Bank",
            source = TransactionSource.NOTIFICATION,
            note = "Flat white & croissant"
        ),
        SpendoTransaction(
            id = "tx_2",
            merchant = "Uber Technologies",
            amountPaise = 42000L,
            type = TransactionType.EXPENSE,
            category = "Transit",
            occurredAtEpochMs = System.currentTimeMillis() - 14400000L,
            bankAccountName = "HDFC Bank",
            source = TransactionSource.NOTIFICATION,
            note = "Ride to tech campus"
        ),
        SpendoTransaction(
            id = "tx_3",
            merchant = "Nature's Basket",
            amountPaise = 66000L,
            type = TransactionType.EXPENSE,
            category = "Groceries",
            occurredAtEpochMs = System.currentTimeMillis() - 43200000L,
            bankAccountName = "ICICI Bank",
            source = TransactionSource.MANUAL,
            note = "Organic produce & sourdough"
        ),
        SpendoTransaction(
            id = "tx_4",
            merchant = "Acme Corp Tech",
            amountPaise = 8500000L,
            type = TransactionType.INCOME,
            category = "Salary",
            occurredAtEpochMs = System.currentTimeMillis() - 172800000L,
            bankAccountName = "HDFC Bank",
            source = TransactionSource.NOTIFICATION,
            note = "Monthly compensation payout"
        ),
        SpendoTransaction(
            id = "tx_5",
            merchant = "Apple Services",
            amountPaise = 19900L,
            type = TransactionType.EXPENSE,
            category = "Subscriptions",
            occurredAtEpochMs = System.currentTimeMillis() - 259200000L,
            bankAccountName = "HDFC Bank",
            source = TransactionSource.MANUAL,
            note = "iCloud+ 2TB storage"
        ),
        SpendoTransaction(
            id = "tx_6",
            merchant = "Freelance UI Client",
            amountPaise = 2500000L,
            type = TransactionType.INCOME,
            category = "Freelance",
            occurredAtEpochMs = System.currentTimeMillis() - 345600000L,
            bankAccountName = "ICICI Bank",
            source = TransactionSource.MANUAL,
            note = "Design system consultation milestone"
        )
    )

    val sampleNotifications = listOf(
        SpendoNotificationItem(
            id = "notif_1",
            title = "Expense Detected",
            message = "HDFC Bank: ₹340.00 debited at Blue Tokai Coffee.",
            category = "Transactions",
            timestampText = "1 hour ago"
        ),
        SpendoNotificationItem(
            id = "notif_2",
            title = "Daily Budget Nearly Exhausted",
            message = "You have spent ₹1,420 of your ₹2,500 daily allowance (56%).",
            category = "Budgets",
            timestampText = "3 hours ago",
            progress = 0.56f
        ),
        SpendoNotificationItem(
            id = "notif_3",
            title = "Money Received",
            message = "₹85,000.00 credited from Acme Corp Tech.",
            category = "Transactions",
            timestampText = "2 days ago"
        ),
        SpendoNotificationItem(
            id = "notif_4",
            title = "Streak Maintained",
            message = "Zero unplanned impulse expenses recorded today. Streak extended to 5 days!",
            category = "Motivation",
            timestampText = "Yesterday"
        ),
        SpendoNotificationItem(
            id = "notif_5",
            title = "Daily Budget Exceeded Alert (Past)",
            message = "On 18 Oct, weekend dining exceeded daily ceiling by ₹250. Adjusted weekly pool.",
            category = "Budgets",
            timestampText = "5 days ago",
            progress = 1.10f
        )
    )

    val sampleIncomes = listOf(
        SpendoIncomeItem(
            id = "inc_1",
            title = "Primary Tech Salary",
            amountPaise = 8500000L,
            isRecurring = true,
            frequency = "Monthly (1st of month)",
            nextPayDate = "01 Nov 2026"
        ),
        SpendoIncomeItem(
            id = "inc_2",
            title = "Design Advisory Retainer",
            amountPaise = 2500000L,
            isRecurring = true,
            frequency = "Bi-weekly",
            nextPayDate = "28 Oct 2026"
        ),
        SpendoIncomeItem(
            id = "inc_3",
            title = "Index Fund Dividends",
            amountPaise = 380000L,
            isRecurring = false,
            frequency = "Quarterly",
            nextPayDate = "15 Dec 2026"
        )
    )

    val initialChatMessages = listOf(
        SpendoChatMessage(
            id = "msg_0",
            sender = "SPENDO",
            text = "Hello! I am Spendo, your local AI money agent. I analyze your spending velocity, monitor daily budget limits, and help you keep more of what you earn."
        )
    )

    val sampleSavingsGoals = listOf(
        SpendoSavingsGoal(
            id = "goal_1",
            title = "Emergency Reserve Fund",
            targetPaise = 30000000L, // ₹3,00,000
            savedPaise = 18500000L,  // ₹1,85,000
            targetDateText = "31 Mar 2027",
            iconName = "shield"
        ),
        SpendoSavingsGoal(
            id = "goal_2",
            title = "Nothing Phone (3) Upgrade",
            targetPaise = 6500000L,  // ₹65,000
            savedPaise = 4200000L,   // ₹42,000
            targetDateText = "15 Jan 2027",
            iconName = "device"
        ),
        SpendoSavingsGoal(
            id = "goal_3",
            title = "Japan Autumn Tour",
            targetPaise = 20000000L, // ₹2,00,000
            savedPaise = 6800000L,   // ₹68,000
            targetDateText = "01 Oct 2027",
            iconName = "flight"
        )
    )
}
