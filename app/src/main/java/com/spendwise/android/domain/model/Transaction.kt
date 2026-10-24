package com.spendwise.android.domain.model

import com.spendwise.android.data.db.entity.TransactionEntity

/**
 * Domain representation of a financial ledger transaction.
 * In accordance with financial rules, amounts are strictly positive integer paise (Long).
 */
data class Transaction(
    val id: String,
    val amountPaise: Long,
    val currencyCode: String = "INR",
    val direction: TransactionDirection,
    val merchant: String,
    val note: String? = null,
    val category: TransactionCategory,
    val occurredAtEpochMillis: Long,
    val sourceType: SourceType,
    val sourcePackage: String? = null,
    val confidence: Float? = null,
    val reviewStatus: ReviewStatus = ReviewStatus.CONFIRMED,
    val dedupeKey: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    init {
        require(amountPaise > 0) { "Transaction amount must be strictly greater than 0 paise" }
    }
}

/**
 * Mapping extension functions between Room entities and Domain models.
 */
fun TransactionEntity.toDomain(): Transaction {
    val dir = try {
        TransactionDirection.valueOf(direction)
    } catch (e: Exception) {
        TransactionDirection.DEBIT
    }

    val cat = try {
        TransactionCategory.valueOf(categoryId)
    } catch (e: Exception) {
        TransactionCategory.OTHER
    }

    val src = try {
        SourceType.valueOf(sourceType)
    } catch (e: Exception) {
        SourceType.MANUAL
    }

    val status = try {
        ReviewStatus.valueOf(reviewStatus)
    } catch (e: Exception) {
        ReviewStatus.CONFIRMED
    }

    return Transaction(
        id = id,
        amountPaise = amountPaise,
        currencyCode = currencyCode,
        direction = dir,
        merchant = merchant ?: "Unknown Payee",
        note = note,
        category = cat,
        occurredAtEpochMillis = occurredAtEpochMillis,
        sourceType = src,
        sourcePackage = sourcePackage,
        confidence = confidence,
        reviewStatus = status,
        dedupeKey = dedupeKey,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amountPaise = amountPaise,
        currencyCode = currencyCode,
        direction = direction.name,
        merchant = merchant,
        note = note,
        categoryId = category.name,
        occurredAtEpochMillis = occurredAtEpochMillis,
        sourceType = sourceType.name,
        sourcePackage = sourcePackage,
        confidence = confidence,
        reviewStatus = reviewStatus.name,
        dedupeKey = dedupeKey,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
