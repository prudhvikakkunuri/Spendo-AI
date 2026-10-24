package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendwise.android.data.db.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for monitored apps allowlist.
 */
@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY app_name ASC")
    fun getAllMonitoredAppsFlow(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT * FROM monitored_apps ORDER BY app_name ASC")
    suspend fun getAllMonitoredApps(): List<MonitoredAppEntity>

    @Query("SELECT package_name FROM monitored_apps WHERE is_enabled = 1")
    fun getEnabledPackageNamesFlow(): Flow<List<String>>

    @Query("SELECT package_name FROM monitored_apps WHERE is_enabled = 1")
    suspend fun getEnabledPackageNames(): List<String>

    @Query("SELECT * FROM monitored_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getMonitoredApp(packageName: String): MonitoredAppEntity?

    @Query("SELECT COUNT(*) FROM monitored_apps")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: MonitoredAppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultApps(apps: List<MonitoredAppEntity>)

    @Query("UPDATE monitored_apps SET is_enabled = :isEnabled, updated_at_epoch_ms = :updatedAtEpochMs WHERE package_name = :packageName")
    suspend fun updateEnabledState(packageName: String, isEnabled: Boolean, updatedAtEpochMs: Long = System.currentTimeMillis())

    @Query("DELETE FROM monitored_apps WHERE package_name = :packageName")
    suspend fun deleteApp(packageName: String)
}
