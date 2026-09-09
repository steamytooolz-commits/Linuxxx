package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DbLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: DbLogEntry): Long

    @Query("SELECT * FROM db_logs ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 300): Flow<List<DbLogEntry>>

    @Query("SELECT * FROM db_logs ORDER BY id ASC")
    suspend fun getAllLogsAscending(): List<DbLogEntry>

    @Query("DELETE FROM db_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM db_logs")
    suspend fun getLogCount(): Int
}
