package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata record for an AI Provider configuration.
 * Note: Secret API keys are Keystore-protected and kept separate from database entities.
 */
@Entity(tableName = "ai_provider_configs")
data class AiProviderConfigEntity(
    @PrimaryKey
    val providerId: String,
    val displayName: String,
    val selectedModel: String,
    val isConfigured: Boolean,
    val lastTestedEpochMs: Long? = null,
    val lastTestMessage: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
