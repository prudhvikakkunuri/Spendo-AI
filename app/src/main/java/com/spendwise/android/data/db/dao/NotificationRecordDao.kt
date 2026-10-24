package com.spendwise.android.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.android.data.db.entity.NotificationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRecordDao {

    @Query("SELECT * FROM notification_records ORDER BY postedAtEpochMs DESC")
    fun getAllNotificationRecords(): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE category = :category ORDER BY postedAtEpochMs DESC")
    fun getRecordsByCategory(category: String): Flow<List<NotificationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationRecord(record: NotificationRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationRecords(records: List<NotificationRecordEntity>)

    @Update
    suspend fun updateNotificationRecord(record: NotificationRecordEntity)

    @Delete
    suspend fun deleteNotificationRecord(record: NotificationRecordEntity)

    @Query("DELETE FROM notification_records WHERE id = :id")
    suspend fun deleteNotificationRecordById(id: String)

    @Query("DELETE FROM notification_records")
    suspend fun clearAllNotificationRecords()

    @Query("SELECT COUNT(*) FROM notification_records")
    suspend fun getNotificationCount(): Int
}
