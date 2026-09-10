package com.example.data.local

data class DbLogEntry(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: String = "INFO"
)
