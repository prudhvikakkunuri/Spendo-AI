package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.CommitmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitmentDao {

    @Query("SELECT * FROM commitments ORDER BY due_at_epoch_ms ASC")
    fun getAllCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE is_paid = 0 ORDER BY due_at_epoch_ms ASC")
    fun getUnpaidCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE is_paid = 0 AND due_at_epoch_ms <= :upToEpochMs ORDER BY due_at_epoch_ms ASC")
    fun getPendingCommitmentsDueBefore(upToEpochMs: Long): Flow<List<CommitmentEntity>>

    @Query("SELECT COALESCE(SUM(amount_paise), 0) FROM commitments WHERE is_paid = 0 AND due_at_epoch_ms <= :upToEpochMs")
    fun sumPendingCommitmentsDueBefore(upToEpochMs: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commitment: CommitmentEntity)

    @Update
    suspend fun update(commitment: CommitmentEntity)

    @Delete
    suspend fun delete(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments")
    suspend fun clear()
}
