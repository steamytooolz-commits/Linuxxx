package com.example.data.repository

import com.example.data.local.DbLogDao
import com.example.data.local.DbLogEntry
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    val recentLogs: Flow<List<DbLogEntry>>
    suspend fun log(tag: String, message: String, level: String = "INFO")
    suspend fun clearLogs()
    suspend fun getLogCount(): Int
}

class DatabaseRepositoryImpl(
    private val dbLogDao: DbLogDao
) : DatabaseRepository {

    override val recentLogs: Flow<List<DbLogEntry>> = dbLogDao.getRecentLogs(300)

    override suspend fun log(tag: String, message: String, level: String) {
        dbLogDao.insertLog(
            DbLogEntry(
                tag = tag,
                message = message,
                level = level
            )
        )
    }

    override suspend fun clearLogs() {
        dbLogDao.clearAllLogs()
    }

    override suspend fun getLogCount(): Int {
        return dbLogDao.getLogCount()
    }
}
