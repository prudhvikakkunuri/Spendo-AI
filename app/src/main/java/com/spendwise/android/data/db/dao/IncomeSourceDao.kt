package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.IncomeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeSourceDao {

    @Query("SELECT * FROM income_sources ORDER BY nextPayDateEpochMs ASC")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE isRecurring = 1 ORDER BY nextPayDateEpochMs ASC")
    fun getRecurringIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE id = :id LIMIT 1")
    fun getIncomeSourceById(id: String): Flow<IncomeSourceEntity?>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM income_sources WHERE isRecurring = 1")
    fun getTotalRecurringIncomePaise(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(income: IncomeSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSources(incomes: List<IncomeSourceEntity>)

    @Update
    suspend fun updateIncomeSource(income: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(income: IncomeSourceEntity)

    @Query("DELETE FROM income_sources WHERE id = :id")
    suspend fun deleteIncomeSourceById(id: String)

    @Query("SELECT COUNT(*) FROM income_sources")
    suspend fun getIncomeCount(): Int
}
