package com.spendwise.android.notification

import android.util.Log
import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.banking.BankMessageParserRegistry
import com.spendwise.android.domain.banking.ParsedBankMessage
import com.spendwise.android.domain.dedup.TransactionDeduplicator
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.parser.ParserResult
import com.spendwise.android.domain.parser.ReviewReason
import com.spendwise.android.domain.parser.TransactionCandidate
import com.spendwise.android.domain.parser.TransactionParser
import com.spendwise.android.notification.model.RawNotificationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production Ingestion Engine with Indian Bank Parsers (Indian Bank, Kotak, KVB)
 * and reference-aware deduplication.
 */
class NotificationIngestionEngine(
    private val transactionDao: TransactionDao,
    private val parser: TransactionParser = TransactionParser(),
    private val deduplicator: TransactionDeduplicator = TransactionDeduplicator(transactionDao),
    private val bankAccountDao: BankAccountDao? = null
) : TransactionNotificationConsumer {

    override suspend fun onNotificationReceived(payload: RawNotificationPayload) {
        withContext(Dispatchers.IO) {
            try {
                // 1. First attempt parsing with specialized Indian Bank Parsers (Indian Bank, Kotak, KVB)
                val bankParsed = BankMessageParserRegistry.parse(payload)
                if (bankParsed != null) {
                    processBankParsedMessage(bankParsed)
                    return@withContext
                }

                // 2. Fallback to generic TransactionParser
                val parseResult = parser.parse(payload)
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
                Log.w(TAG, "Ingestion error: ${e.message}")
            }
        }
    }

    private suspend fun processBankParsedMessage(message: ParsedBankMessage) {
        val amount = message.amountPaise
        if (amount <= 0L) return

        // Deduplication check via cryptographic fingerprint
        val candidate = TransactionCandidate(
            amountPaise = message.amountPaise,
            currencyCode = "INR",
            direction = message.direction,
            merchant = message.merchantOrPayee,
            timestampEpochMs = message.timestampEpochMs,
            sourcePackage = message.sourcePackage,
            confidence = message.confidence,
            suggestedCategory = if (message.isAtmCashMovement) TransactionCategory.TRANSFER else TransactionCategory.OTHER,
            dedupeFingerprint = message.dedupeFingerprint
        )

        val isDuplicate = deduplicator.isDuplicate(candidate)
        if (isDuplicate) {
            Log.d(TAG, "Duplicate bank alert filtered: bank=${message.bankName}, ref=${message.referenceNumber}")
            return
        }

        // Determine category: ATM Cash Movements are marked TRANSFER so they don't double count expenses
        val categoryId = if (message.isAtmCashMovement) "TRANSFER" else "OTHER"
        val noteText = when {
            message.isAtmCashMovement -> "ATM Cash Movement (${message.transactionType.displayName})"
            message.referenceNumber != null -> "Ref: ${message.referenceNumber}"
            else -> null
        }

        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            amountPaise = message.amountPaise,
            currencyCode = "INR",
            direction = message.direction.name,
            merchant = message.merchantOrPayee,
            note = noteText,
            categoryId = categoryId,
            occurredAtEpochMillis = message.timestampEpochMs,
            sourceType = "NOTIFICATION",
            sourcePackage = message.sourcePackage,
            confidence = message.confidence,
            reviewStatus = if (message.reviewRequired) "NEEDS_REVIEW" else "CONFIRMED",
            dedupeKey = message.dedupeFingerprint,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountName = message.bankName
        )

        transactionDao.insertTransaction(entity)

        // Adjust bank account balance and snapshot if confirmed
        if (entity.reviewStatus == "CONFIRMED" && bankAccountDao != null) {
            val account = bankAccountDao.getBankAccountByNameDirect(message.bankName)
            if (account != null) {
                val newBalance = if (message.reportedBalancePaise != null) {
                    message.reportedBalancePaise
                } else {
                    val delta = if (message.direction.name == "DEBIT") -message.amountPaise else message.amountPaise
                    (account.balancePaise + delta).coerceAtLeast(0L)
                }
                bankAccountDao.updateBalance(account.id, newBalance)
            }
        }

        Log.d(TAG, "Bank transaction ingested: bank=${message.bankName}, amount=${message.amountPaise}, type=${message.transactionType}")
    }

    private suspend fun processParsedCandidate(
        candidate: TransactionCandidate,
        isReview: Boolean,
        reviewReason: ReviewReason?
    ) {
        val amount = candidate.amountPaise
        if (amount == null || amount <= 0L) {
            if (!isReview) return
        }

        val isDuplicate = deduplicator.isDuplicate(candidate)
        if (isDuplicate) {
            Log.d(TAG, "Duplicate transaction alert filtered: package=${candidate.sourcePackage}")
            return
        }

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
        private const val TAG = "SpendoIngestion"
    }
}
