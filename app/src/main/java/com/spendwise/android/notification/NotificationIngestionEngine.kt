package com.spendwise.android.notification

import android.content.Context
import android.util.Log
import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.banking.BankAccountMatcher
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Live statistics tracking notification monitoring pipeline activity.
 */
data class IngestionStats(
    val totalProcessed: Int = 0,
    val extractedTransactions: Int = 0,
    val duplicatesFiltered: Int = 0,
    val lastProcessedEpochMs: Long? = null,
    val lastError: String? = null
)

/**
 * Non-persisted diagnostic analysis of a candidate notification.
 */
data class IngestionTestResult(
    val isSuccess: Boolean,
    val parserUsed: String,
    val bankName: String?,
    val direction: String?,
    val amountPaise: Long?,
    val currency: String = "INR",
    val merchant: String?,
    val accountSuffix: String?,
    val referenceNumber: String?,
    val reportedBalancePaise: Long?,
    val reviewRequired: Boolean,
    val reviewReason: String?,
    val matchedAccountId: String? = null,
    val matchedAccountName: String? = null,
    val matchingStatus: String? = null,
    val ambiguousReason: String? = null,
    val confidence: Float? = null,
    val errorMessage: String? = null
)

/**
 * Production Ingestion Engine with Indian Bank Parsers (Indian Bank, Kotak, KVB)
 * and reference-aware deduplication.
 */
class NotificationIngestionEngine(
    private val transactionDao: TransactionDao,
    private val parser: TransactionParser = TransactionParser(),
    private val deduplicator: TransactionDeduplicator = TransactionDeduplicator(transactionDao),
    private val bankAccountDao: BankAccountDao? = null,
    private val context: Context? = null
) : TransactionNotificationConsumer {

    private val _statsFlow = MutableStateFlow(IngestionStats())
    val statsFlow: StateFlow<IngestionStats> = _statsFlow.asStateFlow()

    override suspend fun onNotificationReceived(payload: RawNotificationPayload) {
        withContext(Dispatchers.IO) {
            _statsFlow.update { it.copy(totalProcessed = it.totalProcessed + 1) }
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
                _statsFlow.update { it.copy(lastError = e.message) }
            }
        }
    }

    /**
     * Pure diagnostic test that parses a notification payload without writing to Room DB.
     */
    fun testParse(payload: RawNotificationPayload): IngestionTestResult {
        try {
            // 1. Try Indian Bank Parsers
            val bankParsed = BankMessageParserRegistry.parse(payload)
            if (bankParsed != null) {
                var matchedId: String? = null
                var matchedName: String? = null
                var matchStatusStr: String? = null
                var ambReason: String? = null
                if (bankAccountDao != null) {
                    try {
                        val matchRes = kotlinx.coroutines.runBlocking {
                            BankAccountMatcher(bankAccountDao).matchAccount(bankParsed.bankName, bankParsed.accountSuffix)
                        }
                        matchStatusStr = matchRes.status.name
                        if (matchRes.matchedAccount != null) {
                            matchedId = matchRes.matchedAccount.id
                            matchedName = matchRes.matchedAccount.displayNameWithSuffix
                        } else {
                            ambReason = matchRes.explanation
                        }
                    } catch (e: Exception) {
                        ambReason = e.message
                    }
                }

                return IngestionTestResult(
                    isSuccess = true,
                    parserUsed = bankParsed.bankName,
                    bankName = bankParsed.bankName,
                    direction = bankParsed.direction.name,
                    amountPaise = bankParsed.amountPaise,
                    currency = "INR",
                    merchant = bankParsed.merchantOrPayee,
                    accountSuffix = bankParsed.accountSuffix,
                    referenceNumber = bankParsed.referenceNumber,
                    reportedBalancePaise = bankParsed.reportedBalancePaise,
                    reviewRequired = bankParsed.reviewRequired,
                    reviewReason = bankParsed.reviewReason,
                    matchedAccountId = matchedId,
                    matchedAccountName = matchedName,
                    matchingStatus = matchStatusStr,
                    ambiguousReason = ambReason,
                    confidence = bankParsed.confidence
                )
            }

            // 2. Try generic TransactionParser
            val parseResult = parser.parse(payload)
            return when (parseResult) {
                is ParserResult.Parsed -> {
                    val candidate = parseResult.candidate
                    IngestionTestResult(
                        isSuccess = true,
                        parserUsed = "Generic Parser (High Confidence)",
                        bankName = "Bank",
                        direction = candidate.direction?.name ?: "DEBIT",
                        amountPaise = candidate.amountPaise,
                        currency = candidate.currencyCode,
                        merchant = candidate.merchant,
                        accountSuffix = null,
                        referenceNumber = null,
                        reportedBalancePaise = null,
                        reviewRequired = false,
                        reviewReason = null
                    )
                }
                is ParserResult.NeedsReview -> {
                    val candidate = parseResult.candidate
                    IngestionTestResult(
                        isSuccess = true,
                        parserUsed = "Generic Parser (Needs Review)",
                        bankName = "Bank",
                        direction = candidate.direction?.name ?: "DEBIT",
                        amountPaise = candidate.amountPaise,
                        currency = candidate.currencyCode,
                        merchant = candidate.merchant,
                        accountSuffix = null,
                        referenceNumber = null,
                        reportedBalancePaise = null,
                        reviewRequired = true,
                        reviewReason = formatReviewNote(parseResult.reasonCode)
                    )
                }
                is ParserResult.NotTransaction -> {
                    IngestionTestResult(
                        isSuccess = false,
                        parserUsed = "None",
                        bankName = null,
                        direction = null,
                        amountPaise = null,
                        currency = "INR",
                        merchant = null,
                        accountSuffix = null,
                        referenceNumber = null,
                        reportedBalancePaise = null,
                        reviewRequired = false,
                        reviewReason = null,
                        errorMessage = "Not recognized as a financial transaction (${parseResult.reasonCode})"
                    )
                }
            }
        } catch (e: Exception) {
            return IngestionTestResult(
                isSuccess = false,
                parserUsed = "Error",
                bankName = null,
                direction = null,
                amountPaise = null,
                currency = "INR",
                merchant = null,
                accountSuffix = null,
                referenceNumber = null,
                reportedBalancePaise = null,
                reviewRequired = false,
                reviewReason = null,
                errorMessage = e.message ?: "Unknown parsing error"
            )
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
            _statsFlow.update { it.copy(duplicatesFiltered = it.duplicatesFiltered + 1) }
            return
        }

        // Determine category: ATM Cash Movements are marked TRANSFER so they don't double count expenses
        val categoryId = if (message.isAtmCashMovement) "TRANSFER" else "OTHER"
        // Resolve Bank Account accurately using BankAccountMatcher
        var resolvedAccountId: String? = null
        var resolvedAccountName: String = message.bankName
        var accountMatchExplanation: String? = null
        var matchStatus: com.spendwise.android.domain.banking.AccountMatchStatus = com.spendwise.android.domain.banking.AccountMatchStatus.NO_MATCH_UNLINKED
        var matchConfidence = 0.0f

        if (bankAccountDao != null) {
            val matcher = BankAccountMatcher(bankAccountDao)
            val matchResult = matcher.matchAccount(message.bankName, message.accountSuffix)
            matchStatus = matchResult.status
            matchConfidence = matchResult.matchConfidence
            accountMatchExplanation = matchResult.explanation

            if (matchResult.isAutoAssignable && matchResult.matchedAccount != null) {
                val acc = matchResult.matchedAccount
                resolvedAccountId = acc.id
                resolvedAccountName = acc.displayNameWithSuffix
            } else {
                resolvedAccountId = null
                resolvedAccountName = if (message.accountSuffix != null) "${message.bankName} •••• ${message.accountSuffix}" else message.bankName
            }
        }

        // Strict Review-First Architecture: 100% of notification-detected transactions enter NEEDS_REVIEW
        val finalReviewStatus = "NEEDS_REVIEW"
        val reviewNote = com.spendwise.android.domain.banking.TransactionReviewMetadata(
            detectedBank = message.bankName,
            extractedSuffix = message.accountSuffix,
            matchStatus = matchStatus,
            matchedAccountId = resolvedAccountId,
            referenceNumber = message.referenceNumber,
            parserConfidence = message.confidence,
            matchConfidence = matchConfidence,
            explanation = accountMatchExplanation ?: message.reviewReason
        ).serialize()

        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            amountPaise = message.amountPaise,
            currencyCode = "INR",
            direction = message.direction.name,
            merchant = message.merchantOrPayee,
            note = reviewNote,
            categoryId = categoryId,
            occurredAtEpochMillis = message.timestampEpochMs,
            sourceType = "NOTIFICATION",
            sourcePackage = message.sourcePackage,
            confidence = message.confidence,
            reviewStatus = finalReviewStatus,
            dedupeKey = message.dedupeFingerprint,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            bankAccountId = resolvedAccountId,
            bankAccountName = resolvedAccountName
        )

        transactionDao.insertTransaction(entity)

        // Dispatch review-first queue notification
        dispatchQueueNotification(entity)

        _statsFlow.update {
            it.copy(
                extractedTransactions = it.extractedTransactions + 1,
                lastProcessedEpochMs = System.currentTimeMillis()
            )
        }
        notifyDownstream()

        Log.d(TAG, "Bank transaction ingested: bank=${resolvedAccountName}, amount=${message.amountPaise}, status=$finalReviewStatus")
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
            _statsFlow.update { it.copy(duplicatesFiltered = it.duplicatesFiltered + 1) }
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
            reviewStatus = "NEEDS_REVIEW", // Strict review-first
            dedupeKey = candidate.dedupeFingerprint,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        transactionDao.insertTransaction(entity)

        // Dispatch review-first queue notification
        dispatchQueueNotification(entity)

        _statsFlow.update {
            it.copy(
                extractedTransactions = it.extractedTransactions + 1,
                lastProcessedEpochMs = System.currentTimeMillis()
            )
        }
        notifyDownstream()

        Log.d(TAG, "Transaction ingested: status=${entity.reviewStatus}, package=${entity.sourcePackage}")
    }

    private suspend fun dispatchQueueNotification(entity: TransactionEntity) {
        context?.let { ctx ->
            try {
                SpendoNotificationHelper.notifyTransactionQueued(
                    context = ctx,
                    transactionId = entity.id,
                    merchant = entity.merchant ?: "Merchant",
                    amountPaise = entity.amountPaise,
                    direction = entity.direction,
                    bankName = entity.bankAccountName ?: "Bank"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to dispatch queue-entry notification: ${e.message}")
            }
        }
    }

    private suspend fun notifyDownstream() {
        context?.let { ctx ->
            try {
                com.spendwise.android.widget.SpendoWidgetProvider.updateAllWidgets(ctx)
                com.spendwise.android.notification.SpendoNotificationHelper.checkAndNotifyBudgetThreshold(ctx)
            } catch (e: Exception) {
                Log.w(TAG, "Downstream widget/budget update error: ${e.message}")
            }
        }
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
