package com.spendwise.android.domain.model

import com.spendwise.android.data.db.entity.CommitmentEntity
import java.util.UUID

/**
 * Domain model representing a financial commitment (e.g., rent, EMI, utilities, subscription).
 *
 * Rules:
 * - Amount in integer paise must be strictly positive.
 * - Title cannot be blank.
 */
data class Commitment(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amountPaise: Long,
    val dueAtEpochMs: Long,
    val isPaid: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    init {
        require(amountPaise > 0L) { "Commitment amount must be strictly positive integer paise." }
        require(title.isNotBlank()) { "Commitment title cannot be blank." }
    }

    fun toEntity(): CommitmentEntity {
        return CommitmentEntity(
            id = id,
            title = title,
            amountPaise = amountPaise,
            dueAtEpochMs = dueAtEpochMs,
            isPaid = isPaid,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    companion object {
        fun fromEntity(entity: CommitmentEntity): Commitment {
            return Commitment(
                id = entity.id,
                title = entity.title,
                amountPaise = entity.amountPaise,
                dueAtEpochMs = entity.dueAtEpochMs,
                isPaid = entity.isPaid,
                createdAtEpochMs = entity.createdAtEpochMs,
                updatedAtEpochMs = entity.updatedAtEpochMs
            )
        }
    }
}
