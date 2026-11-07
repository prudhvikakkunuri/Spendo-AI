package com.spendwise.android.notification

import java.util.Locale

enum class NotificationPersonalityTone {
    WITTY,
    FRIENDLY,
    MINIMAL
}

data class NotificationMessageCopy(
    val title: String,
    val body: String
)

/**
 * Personality Copy Engine for Spendo Notifications.
 * Generates witty, motivating, but respectful financial notifications.
 */
object SpendoNotificationPersonality {

    fun getTransactionQueuedCopy(
        tone: NotificationPersonalityTone,
        merchant: String,
        amountRupees: Double,
        direction: String
    ): NotificationMessageCopy {
        val formattedAmount = "₹%,.2f".format(Locale.ENGLISH, amountRupees)
        val cleanMerchant = if (merchant.isNotBlank() && !merchant.equals("Unknown Payee", ignoreCase = true)) merchant else "Merchant"

        return when (tone) {
            NotificationPersonalityTone.WITTY -> {
                if (direction == "CREDIT") {
                    NotificationMessageCopy(
                        title = "Money inbound! 💰",
                        body = "$formattedAmount received from $cleanMerchant. Confirm it to update your balance."
                    )
                } else {
                    NotificationMessageCopy(
                        title = "A new expense is ready",
                        body = "$formattedAmount at $cleanMerchant was detected. Review it before adding it to Spendo."
                    )
                }
            }
            NotificationPersonalityTone.FRIENDLY -> {
                if (direction == "CREDIT") {
                    NotificationMessageCopy(
                        title = "Credit detected",
                        body = "Received $formattedAmount from $cleanMerchant. Tap to review and confirm."
                    )
                } else {
                    NotificationMessageCopy(
                        title = "New expense detected",
                        body = "$formattedAmount at $cleanMerchant is ready for review in your verification queue."
                    )
                }
            }
            NotificationPersonalityTone.MINIMAL -> {
                NotificationMessageCopy(
                    title = if (direction == "CREDIT") "Credit Pending Review" else "Expense Pending Review",
                    body = "$formattedAmount · $cleanMerchant"
                )
            }
        }
    }

    fun getMorningReminderCopy(
        tone: NotificationPersonalityTone,
        pendingCount: Int
    ): NotificationMessageCopy {
        val itemsWord = if (pendingCount == 1) "expense" else "expenses"
        return when (tone) {
            NotificationPersonalityTone.WITTY -> {
                NotificationMessageCopy(
                    title = "Your transactions are waiting backstage",
                    body = "$pendingCount $itemsWord waiting for your approval. A clear ledger brings peaceful coffee ☕"
                )
            }
            NotificationPersonalityTone.FRIENDLY -> {
                NotificationMessageCopy(
                    title = "Start your day with a clear ledger",
                    body = "You have $pendingCount $itemsWord waiting for review. Take a moment to confirm them."
                )
            }
            NotificationPersonalityTone.MINIMAL -> {
                NotificationMessageCopy(
                    title = "Pending Queue Review",
                    body = "$pendingCount $itemsWord awaiting approval."
                )
            }
        }
    }

    fun getEveningReminderCopy(
        tone: NotificationPersonalityTone,
        pendingCount: Int
    ): NotificationMessageCopy {
        val itemsWord = if (pendingCount == 1) "transaction" else "transactions"
        return when (tone) {
            NotificationPersonalityTone.WITTY -> {
                NotificationMessageCopy(
                    title = "Your budget called. It's tired.",
                    body = "You still have $pendingCount $itemsWord to review. Clear your queue before calling it a day."
                )
            }
            NotificationPersonalityTone.FRIENDLY -> {
                NotificationMessageCopy(
                    title = "Your expense queue is waiting",
                    body = "You still have $pendingCount $itemsWord to review. Clear your queue before calling it a day."
                )
            }
            NotificationPersonalityTone.MINIMAL -> {
                NotificationMessageCopy(
                    title = "Evening Ledger Verification",
                    body = "$pendingCount $itemsWord remaining in review queue."
                )
            }
        }
    }

    fun getBudgetApproachingCopy(
        tone: NotificationPersonalityTone,
        percentUsed: Int,
        remainingRupees: Long
    ): NotificationMessageCopy {
        val remainingFormatted = "₹%,d".format(Locale.ENGLISH, remainingRupees)
        return when (tone) {
            NotificationPersonalityTone.WITTY -> {
                NotificationMessageCopy(
                    title = "Easy there, big spender",
                    body = "You've used $percentUsed% of today's budget. $remainingFormatted left to play with."
                )
            }
            NotificationPersonalityTone.FRIENDLY -> {
                NotificationMessageCopy(
                    title = "Daily Budget Update",
                    body = "You have reached $percentUsed% of your daily ceiling. $remainingFormatted remaining for today."
                )
            }
            NotificationPersonalityTone.MINIMAL -> {
                NotificationMessageCopy(
                    title = "Budget at $percentUsed%",
                    body = "Remaining: $remainingFormatted"
                )
            }
        }
    }

    fun getBudgetLimitReachedCopy(
        tone: NotificationPersonalityTone,
        totalSpentRupees: Long
    ): NotificationMessageCopy {
        val spentFormatted = "₹%,d".format(Locale.ENGLISH, totalSpentRupees)
        return when (tone) {
            NotificationPersonalityTone.WITTY -> {
                NotificationMessageCopy(
                    title = "Your wallet is feeling adventurous",
                    body = "You've reached today's limit ($spentFormatted). Maybe give it a little break before the next purchase?"
                )
            }
            NotificationPersonalityTone.FRIENDLY -> {
                NotificationMessageCopy(
                    title = "Daily Limit Reached",
                    body = "Today's expenses have reached your set ceiling ($spentFormatted). Disciplined spending pays off!"
                )
            }
            NotificationPersonalityTone.MINIMAL -> {
                NotificationMessageCopy(
                    title = "Daily Limit Reached",
                    body = "Total today: $spentFormatted"
                )
            }
        }
    }
}

