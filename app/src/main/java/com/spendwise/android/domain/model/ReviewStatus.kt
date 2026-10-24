package com.spendwise.android.domain.model

/**
 * Review state for transaction records.
 * Uncertain or low-confidence notification candidates require explicit user review.
 */
enum class ReviewStatus {
    CONFIRMED,
    NEEDS_REVIEW,
    IGNORED
}
