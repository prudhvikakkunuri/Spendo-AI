package com.spendwise.android.data.repository

import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.toDomain
import com.spendwise.android.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Single source of truth for transaction ledger records.
 */
class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions().map { entities ->
        entities.map { it.toDomain() }
    }

    val confirmedTransactions: Flow<List<Transaction>> = transactionDao.getConfirmedTransactions().map { entities ->
        entities.map { it.toDomain() }
    }

    val reviewTransactions: Flow<List<Transaction>> = transactionDao.getTransactionsByStatus("NEEDS_REVIEW").map { entities ->
        entities.map { it.toDomain() }
    }

    fun getTransactionById(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(id).map { it?.toDomain() }
    }

    suspend fun getTransactionByIdDirect(id: String): Transaction? {
        return transactionDao.getTransactionByIdDirect(id)?.toDomain()
    }

    suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    suspend fun confirmReviewTransaction(id: String) {
        transactionDao.updateReviewStatus(id, "CONFIRMED")
    }

    suspend fun ignoreReviewTransaction(id: String) {
        transactionDao.updateReviewStatus(id, "IGNORED")
    }

    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    fun getTodayDebitsPaise(): Flow<Long> {
        val (startOfDay, endOfDay) = getTodayEpochRange()
        return transactionDao.getTodayDebitsPaise(startOfDay, endOfDay)
    }

    fun getTodayCreditsPaise(): Flow<Long> {
        val (startOfDay, endOfDay) = getTodayEpochRange()
        return transactionDao.getTodayCreditsPaise(startOfDay, endOfDay)
    }

    fun getPeriodDebitsPaise(): Flow<Long> {
        val startOfPeriod = getStartOfMonthEpoch()
        return transactionDao.getPeriodDebitsPaise(startOfPeriod)
    }

    fun getPendingReviewCount(): Flow<Int> {
        return transactionDao.getPendingReviewCount()
    }

    private fun getTodayEpochRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun getStartOfMonthEpoch(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
