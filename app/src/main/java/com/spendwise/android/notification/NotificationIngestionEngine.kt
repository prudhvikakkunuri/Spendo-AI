package com.spendwise.android.notification

import android.util.Log
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.dedup.TransactionDeduplicator
import com.spendwise.android.domain.parser.ParserResult
import com.spendwise.android.domain.parser.ReviewReason
import com.spendwise.android.domain.parser.TransactionCandidate
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Ingestion engine executing deterministic local transaction parsing and multi-signal deduplication.
 *
 * Privacy Guarantees:
 * 1. Executes 100% locally on-device.
 * 2. Raw notification text is processed in memory and discarded immediately after parsing.
 * 3. Never logs raw notification content or account/monetary figures.
 * 4. Deduplicates against recent transactions to avoid duplicate ledger entries.
 * 5. High-confidence transactions are saved as CONFIRMED.
 * 6. Ambiguous, failed, or uncertain alerts are routed to the Review Queue (NEEDS_REVIEW).
 */
class NotificationIngestionEngine(
    private val transactionDao: TransactionDao,
    private val parser: TransactionParser = TransactionParser(),
    private val deduplicator: TransactionDeduplicator = TransactionDeduplicator(transactionDao)
) : TransactionNotificationConsumer {

    override suspend fun onNotificationReceived(payload: RawNotificationPayload) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Run pure Kotlin rule-based parsing locally
                val parseResult = parser.parse(payload)

                // 2. Handle parsing outcome
                when (parseResult) {
                    is ParserResult.NotTransaction -> {
                        Log.d(TAG, "Notification dropped: reason=${parseResult.reasonCode}")
                    }

                    is ParserResult.Parsed -> {
                        processParsedCandidate(parseResult.candidate, isReview = false, reviewReason = null)
                    }

                    is ParserResult.NeedsReview -> {
                        processParsedCandidate(parseResult.candidate, isReview = true, reviewReason = parseResult.reasonCode)
                    }
                }
            } catch (e: Exception) {
                // Defensive shield: ingestion errors must never crash background listener
                Log.w(TAG, "Ingestion error: ${e.message}")
            }
        }
    }

    private suspend fun processParsedCandidate(
        candidate: TransactionCandidate,
        isReview: Boolean,
        reviewReason: ReviewReason?
    ) {
        val amount = candidate.amountPaise
        if (amount == null || amount <= 0L) {
            // Missing amounts cannot form ledger entries without review
            if (!isReview) return
        }

        // 3. Multi-signal deduplication check
        val isDuplicate = deduplicator.isDuplicate(candidate)
        if (isDuplicate) {
            Log.d(TAG, "Duplicate transaction alert filtered: package=${candidate.sourcePackage}")
            return
        }

        // 4. Construct ledger record
        val noteText = when {
            reviewReason != null -> formatReviewNote(reviewReason)
            candidate.isRefundOrReversal -> "Refund / Reversal"
            else -> null
        }

        val resolvedDirection = candidate.direction?.name ?: "DEBIT"

        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            amountPaise = candidate.amountPaise ?: 0L,
            currencyCode = candidate.currencyCode,
            direction = resolvedDirection,
            merchant = candidate.merchant ?: "Unknown Payee",
            note = noteText,
            categoryId = candidate.suggestedCategory.name,
            occurredAtEpochMillis = candidate.timestampEpochMs,
            sourceType = "NOTIFICATION",
            sourcePackage = candidate.sourcePackage,
            confidence = candidate.confidence,
            reviewStatus = if (isReview) "NEEDS_REVIEW" else "CONFIRMED",
            dedupeKey = candidate.dedupeFingerprint,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        // 5. Persist to local Room database
        transactionDao.insertTransaction(entity)
        Log.d(TAG, "Transaction ingested: status=${entity.reviewStatus}, package=${entity.sourcePackage}")
    }

    private fun formatReviewNote(reason: ReviewReason): String {
        return when (reason) {
            ReviewReason.AMBIGUOUS_MULTIPLE_AMOUNTS -> "Multiple amounts detected in alert"
            ReviewReason.UNCERTAIN_MERCHANT -> "Unclear merchant or payee"
            ReviewReason.REFUND_OR_REVERSAL -> "Suspected refund or reversal"
            ReviewReason.FAILED_TRANSACTION_ALERT -> "Failed / declined payment alert"
            ReviewReason.SUSPECTED_TRANSFER -> "Suspected internal account transfer"
            ReviewReason.LOW_CONFIDENCE -> "Low parsing confidence"
            ReviewReason.UNUSUAL_FORMAT -> "Unusual notification layout"
            ReviewReason.HIGH_VALUE_TRANSACTION -> "High-value transaction alert"
        }
    }

    companion object {
        private const val TAG = "SpendWiseIngestion"
    }
}
