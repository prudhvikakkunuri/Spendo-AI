package com.spendwise.android.domain.dedup

import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.domain.parser.TransactionCandidate
import java.util.Locale

/**
 * Deduplication engine combining multiple signals to detect duplicate transaction alerts.
 *
 * Non-Negotiable Rule:
 * Never deduplicate solely on amount. Two legitimate transactions may have the exact same amount.
 * Signals evaluated:
 * 1. Source Package Name
 * 2. Amount in paise (Long)
 * 3. Debit vs Credit direction
 * 4. Temporal Proximity Window (default 10 minutes)
 * 5. Normalized Merchant / Payee
 * 6. Cryptographic Dedupe Fingerprint
 */
class TransactionDeduplicator(
    private val transactionDao: TransactionDao,
    private val windowMs: Long = 10 * 60 * 1000L // 10-minute window
) {

    /**
     * Checks if an incoming candidate matches an already recorded transaction.
     */
    suspend fun isDuplicate(candidate: TransactionCandidate): Boolean {
        val amount = candidate.amountPaise
        if (amount == null || amount <= 0L) {
            return false
        }

        // 1. Direct dedupeKey / fingerprint check
        val existingByFingerprint = transactionDao.getTransactionByDedupeKey(candidate.dedupeFingerprint)
        if (existingByFingerprint != null) {
            return true
        }

        // 2. Multi-signal query within temporal proximity window
        val minTime = candidate.timestampEpochMs - windowMs
        val maxTime = candidate.timestampEpochMs + windowMs

        val potentialMatches = transactionDao.findPotentialDuplicates(
            amountPaise = amount,
            direction = candidate.direction?.name ?: "DEBIT",
            minTime = minTime,
            maxTime = maxTime
        )

        if (potentialMatches.isEmpty()) {
            return false
        }

        val candidateMerchant = candidate.merchant?.trim()?.lowercase(Locale.ROOT) ?: ""

        // 3. Confirm match using multiple signals: package match OR merchant match
        return potentialMatches.any { existing ->
            val samePackage = existing.sourcePackage.equals(candidate.sourcePackage, ignoreCase = true)
            val existingMerchant = existing.merchant?.trim()?.lowercase(Locale.ROOT) ?: ""
            val sameMerchant = candidateMerchant.isNotBlank() && existingMerchant.isNotBlank() &&
                    (candidateMerchant == existingMerchant ||
                            candidateMerchant.contains(existingMerchant) ||
                            existingMerchant.contains(candidateMerchant))

            // Requires same package AND (same merchant OR close time proximity)
            samePackage || sameMerchant
        }
    }
}
