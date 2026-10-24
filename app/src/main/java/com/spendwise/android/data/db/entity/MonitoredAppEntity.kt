package com.spendwise.android.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an application package explicitly authorized by the user
 * for notification inspection.
 *
 * Privacy rule: SpendWise MUST NEVER process notifications from any package
 * that is not explicitly enabled in this table.
 */
@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
