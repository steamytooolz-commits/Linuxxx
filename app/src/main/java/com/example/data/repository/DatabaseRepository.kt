package com.example.data.repository

import com.example.data.local.DbLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

interface DatabaseRepository {
    val recentLogs: Flow<List<DbLogEntry>>
    suspend fun log(tag: String, message: String, level: String = "INFO")
    suspend fun clearLogs()
    suspend fun getLogCount(): Int
}

class DatabaseRepositoryImpl : DatabaseRepository {
    private val idCounter = AtomicLong(1)
    private val mutex = Mutex()
    private val logList = ArrayList<DbLogEntry>()
    private val _recentLogs = MutableStateFlow<List<DbLogEntry>>(emptyList())
    override val recentLogs: Flow<List<DbLogEntry>> = _recentLogs.asStateFlow()

    override suspend fun log(tag: String, message: String, level: String) {
        val entry = DbLogEntry(
            id = idCounter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level
        )
        mutex.withLock {
            logList.add(entry)
            if (logList.size > 500) {
                logList.removeAt(0)
            }
            _recentLogs.value = logList.toList()
        }
    }

    override suspend fun clearLogs() {
        mutex.withLock {
            logList.clear()
            _recentLogs.value = emptyList()
        }
    }

    override suspend fun getLogCount(): Int {
        mutex.withLock {
            return logList.size
        }
    }
}
