package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.CategorySpending
import com.spendwise.android.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction persistence operations.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE reviewStatus = 'CONFIRMED' ORDER BY occurredAtEpochMillis DESC")
    fun getConfirmedTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE reviewStatus = :reviewStatus ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsByStatus(reviewStatus: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdDirect(id: String): TransactionEntity?

    @Query("SELECT id FROM transactions")
    suspend fun getAllTransactionIds(): List<String>

    @Query("SELECT * FROM transactions WHERE reviewStatus = 'CONFIRMED' ORDER BY occurredAtEpochMillis DESC")
    suspend fun getConfirmedTransactionsDirect(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'DEBIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startOfDay 
          AND occurredAtEpochMillis <= :endOfDay
    """)
    fun getTodayDebitsPaise(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'CREDIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startOfDay 
          AND occurredAtEpochMillis <= :endOfDay
    """)
    fun getTodayCreditsPaise(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'DEBIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startOfPeriod
    """)
    fun getPeriodDebitsPaise(startOfPeriod: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'DEBIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startTime 
          AND occurredAtEpochMillis <= :endTime
    """)
    suspend fun getDebitsInDateRangeDirect(startTime: Long, endTime: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'CREDIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startTime 
          AND occurredAtEpochMillis <= :endTime
    """)
    suspend fun getCreditsInDateRangeDirect(startTime: Long, endTime: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'CREDIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId = 'INCOME'
          AND occurredAtEpochMillis >= :startOfPeriod
    """)
    fun getPeriodIncomeCredits(startOfPeriod: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE direction = 'CREDIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND categoryId != 'INCOME'
          AND occurredAtEpochMillis >= :startOfPeriod
    """)
    fun getPeriodRefundCredits(startOfPeriod: Long): Flow<Long>

    @Query("""
        SELECT categoryId, COALESCE(SUM(amountPaise), 0) AS totalPaise, COUNT(*) AS count
        FROM transactions 
        WHERE direction = 'DEBIT' 
          AND reviewStatus = 'CONFIRMED' 
          AND categoryId NOT IN ('TRANSFER', 'TRANSFERS')
          AND occurredAtEpochMillis >= :startOfPeriod
        GROUP BY categoryId
        ORDER BY totalPaise DESC
    """)
    fun getCategorySpendTotals(startOfPeriod: Long): Flow<List<CategorySpending>>

    @Query("SELECT COUNT(*) FROM transactions WHERE reviewStatus = 'NEEDS_REVIEW'")
    fun getPendingReviewCount(): Flow<Int>

    @Query("""
        SELECT * FROM transactions 
        WHERE amountPaise = :amountPaise 
          AND direction = :direction 
          AND occurredAtEpochMillis >= :minTime 
          AND occurredAtEpochMillis <= :maxTime
    """)
    suspend fun findPotentialDuplicates(
        amountPaise: Long,
        direction: String,
        minTime: Long,
        maxTime: Long
    ): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE dedupeKey = :dedupeKey LIMIT 1")
    suspend fun getTransactionByDedupeKey(dedupeKey: String): TransactionEntity?

    @Query("UPDATE transactions SET reviewStatus = :newStatus, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateReviewStatus(id: String, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM transactions 
        WHERE (merchant LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR categoryId LIKE '%' || :query || '%')
        ORDER BY occurredAtEpochMillis DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE direction = :direction ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsByDirection(direction: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAtEpochMillis >= :startTime AND occurredAtEpochMillis <= :endTime ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE bankAccountName = :bankAccountName OR bankAccountId = :bankAccountName ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsForBank(bankAccountName: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :merchantQuery || '%' ORDER BY occurredAtEpochMillis DESC")
    fun getTransactionsForMerchant(merchantQuery: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE merchant LIKE '%' || :merchantQuery || '%' AND direction = 'DEBIT' AND reviewStatus = 'CONFIRMED'")
    suspend fun getMerchantTotalSpendDirect(merchantQuery: String): Long
}
