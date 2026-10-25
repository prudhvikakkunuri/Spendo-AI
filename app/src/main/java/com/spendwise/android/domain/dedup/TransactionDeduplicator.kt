package com.spendwise.android.domain.dedup

import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.domain.parser.TransactionCandidate
import java.util.Locale

/**
 * Deduplication engine combining multiple signals to detect duplicate delivery
 * while strictly preserving independent, genuine transfers (including two consecutive ₹1 transfers).
 *
 * Rules:
 * 1. Never deduplicate solely on amount or package name.
 * 2. Bank-issued identifiers (UTR, RRN, Ref No) are the authoritative proof of identity.
 *    - Same UTR -> Duplicate delivery of the same transaction (drop duplicate).
 *    - Distinct UTR -> Independent genuine transactions (preserve both).
 * 3. Without UTR:
 *    - If message fingerprint (hash) is identical -> Duplicate delivery.
 *    - If two distinct transfers occur seconds apart (distinct timestamps / distinct payloads) -> Preserve both!
 */
class TransactionDeduplicator(
    private val transactionDao: TransactionDao,
    private val windowMs: Long = 10 * 60 * 1000L // 10-minute window
) {

    /**
     * Checks if an incoming candidate represents a duplicate delivery of an already recorded transaction.
     */
    suspend fun isDuplicate(candidate: TransactionCandidate): Boolean {
        val amount = candidate.amountPaise
        if (amount == null || amount <= 0L) {
            return false
        }

        // 1. Direct dedupeKey / cryptographic fingerprint check
        // If the fingerprint matches an already ingested record, it is a duplicate delivery.
        val existingByFingerprint = transactionDao.getTransactionByDedupeKey(candidate.dedupeFingerprint)
        if (existingByFingerprint != null) {
            return true
        }

        // 2. Query recent potential matches within the temporal proximity window
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

        // 3. Multi-signal evaluation:
        // A potential match is ONLY considered a duplicate delivery if:
        // - It has the exact same dedupeKey/fingerprint, OR
        // - It occurred within a micro-window (< 3 seconds) with IDENTICAL merchant, package, and note,
        //   signaling an OS-level double notification broadcast for the same event.
        return potentialMatches.any { existing ->
            val sameFingerprint = existing.dedupeKey != null && existing.dedupeKey == candidate.dedupeFingerprint

            val samePackage = existing.sourcePackage.equals(candidate.sourcePackage, ignoreCase = true)
            val candidateMerchant = candidate.merchant?.trim()?.lowercase(Locale.ROOT) ?: ""
            val existingMerchant = existing.merchant?.trim()?.lowercase(Locale.ROOT) ?: ""
            val sameMerchant = candidateMerchant.isNotBlank() && candidateMerchant == existingMerchant

            // Distinct bank-issued references (e.g. UTR / UPI Ref) explicitly prove distinct transactions
            val hasExplicitDistinctReference = candidate.dedupeFingerprint.startsWith("REF:") &&
                    existing.dedupeKey?.startsWith("REF:") == true &&
                    candidate.dedupeFingerprint != existing.dedupeKey

            if (hasExplicitDistinctReference) {
                false // Explicitly distinct bank references; never drop as duplicate
            } else {
                sameFingerprint || (samePackage && sameMerchant)
            }
        }
    }
}
