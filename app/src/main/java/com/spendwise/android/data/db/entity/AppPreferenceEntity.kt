package com.spendwise.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Key-value persistent preference entity for local app settings.
 */
@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
