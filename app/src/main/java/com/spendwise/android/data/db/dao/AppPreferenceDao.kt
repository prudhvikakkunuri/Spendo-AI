package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendwise.android.data.db.entity.AppPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPreferenceDao {

    @Query("SELECT * FROM app_preferences")
    fun getAllPreferences(): Flow<List<AppPreferenceEntity>>

    @Query("SELECT * FROM app_preferences WHERE `key` = :key LIMIT 1")
    fun getPreference(key: String): Flow<AppPreferenceEntity?>

    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValueDirect(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}
