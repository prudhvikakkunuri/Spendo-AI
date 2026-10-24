package com.spendwise.android.domain.parser

import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection

/**
 * Reasons why an incoming notification was determined NOT to be a confirmed or actionable financial transaction.
 */
enum class NotTransactionReason {
    AUTH_OR_OTP,
    OFFER_OR_PROMOTION,
    BALANCE_INQUIRY_ONLY,
    FAILED_TRANSACTION,
    EMPTY_OR_NO_TEXT,
    UNSUPPORTED_LANGUAGE_OR_FORMAT,
    DUPLICATE,
    UNSELECTED_PACKAGE
}

/**
 * Reasons why a parsed transaction candidate requires explicit user confirmation in the Review Queue.
 */
enum class ReviewReason {
    AMBIGUOUS_MULTIPLE_AMOUNTS,
    UNCERTAIN_MERCHANT,
    REFUND_OR_REVERSAL,
    FAILED_TRANSACTION_ALERT,
    SUSPECTED_TRANSFER,
    LOW_CONFIDENCE,
    UNUSUAL_FORMAT,
    HIGH_VALUE_TRANSACTION
}

/**
 * Intermediate candidate extracted from an incoming notification before persistence.
 */
data class TransactionCandidate(
    val amountPaise: Long?,
    val currencyCode: String = "INR",
    val direction: TransactionDirection?,
    val merchant: String?,
    val timestampEpochMs: Long,
    val sourcePackage: String,
    val confidence: Float,
    val reviewReasons: List<ReviewReason> = emptyList(),
    val suggestedCategory: TransactionCategory = TransactionCategory.OTHER,
    val isRefundOrReversal: Boolean = false,
    val isFailed: Boolean = false,
    val note: String? = null,
    val dedupeFingerprint: String
)

/**
 * Result of executing TransactionParser on an incoming notification payload.
 */
sealed class ParserResult {
    data class Parsed(val candidate: TransactionCandidate) : ParserResult()
    data class NeedsReview(val candidate: TransactionCandidate, val reasonCode: ReviewReason) : ParserResult()
    data class NotTransaction(val reasonCode: NotTransactionReason) : ParserResult()
}
