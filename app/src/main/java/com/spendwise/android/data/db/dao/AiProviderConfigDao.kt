package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.AiProviderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderConfigDao {

    @Query("SELECT * FROM ai_provider_configs ORDER BY updatedAtEpochMs DESC")
    fun getAllConfigs(): Flow<List<AiProviderConfigEntity>>

    @Query("SELECT * FROM ai_provider_configs WHERE providerId = :providerId LIMIT 1")
    fun getConfig(providerId: String): Flow<AiProviderConfigEntity?>

    @Query("SELECT * FROM ai_provider_configs WHERE providerId = :providerId LIMIT 1")
    suspend fun getConfigDirect(providerId: String): AiProviderConfigEntity?

    @Query("SELECT * FROM ai_provider_configs WHERE isConfigured = 1 LIMIT 1")
    fun getActiveConfig(): Flow<AiProviderConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AiProviderConfigEntity)

    @Update
    suspend fun updateConfig(config: AiProviderConfigEntity)

    @Query("DELETE FROM ai_provider_configs WHERE providerId = :providerId")
    suspend fun deleteConfig(providerId: String)
}
