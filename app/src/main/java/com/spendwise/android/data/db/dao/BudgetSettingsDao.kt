package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendwise.android.data.db.entity.BudgetSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSettingsDao {

    @Query("SELECT * FROM budget_settings WHERE id = 1 LIMIT 1")
    fun getBudgetSettings(): Flow<BudgetSettingsEntity?>

    @Query("SELECT * FROM budget_settings WHERE id = 1 LIMIT 1")
    suspend fun getBudgetSettingsSync(): BudgetSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: BudgetSettingsEntity)

    @Query("DELETE FROM budget_settings")
    suspend fun clear()
}
