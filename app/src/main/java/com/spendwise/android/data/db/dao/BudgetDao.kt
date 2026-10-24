package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE periodType = :periodType AND categoryId IS NULL LIMIT 1")
    fun getOverallBudgetByPeriod(periodType: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE periodType = :periodType AND categoryId IS NULL LIMIT 1")
    suspend fun getOverallBudgetByPeriodDirect(periodType: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND periodType = :periodType LIMIT 1")
    fun getCategoryBudget(categoryId: String, periodType: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String)
}
