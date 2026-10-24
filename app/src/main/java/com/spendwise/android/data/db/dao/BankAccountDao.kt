package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    @Query("SELECT * FROM bank_accounts ORDER BY isPrimary DESC, bankName ASC")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    fun getBankAccountById(id: String): Flow<BankAccountEntity?>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getBankAccountByIdDirect(id: String): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE bankName = :bankName LIMIT 1")
    suspend fun getBankAccountByNameDirect(bankName: String): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccounts(accounts: List<BankAccountEntity>)

    @Update
    suspend fun updateBankAccount(account: BankAccountEntity)

    @Query("UPDATE bank_accounts SET balancePaise = :newBalancePaise, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, newBalancePaise: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteBankAccount(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteBankAccountById(id: String)

    @Query("SELECT COUNT(*) FROM bank_accounts")
    suspend fun getBankAccountCount(): Int
}
