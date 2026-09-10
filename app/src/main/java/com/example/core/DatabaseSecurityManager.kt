package com.example.core

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * Manages secure credentials for database engines (e.g. MariaDB root password)
 * generated on first launch and stored securely in app private SharedPreferences.
 */
class DatabaseSecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "linuxxx_db_security"
        private const val KEY_MARIADB_ROOT_PASSWORD = "mariadb_root_password"
        private const val CHAR_POOL = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*"

        @Volatile
        private var INSTANCE: DatabaseSecurityManager? = null

        fun getInstance(context: Context): DatabaseSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves the stored MariaDB root password, or generates a cryptographically random
     * 16-character alphanumeric password on first launch and saves it.
     */
    fun getOrCreateMariaDbPassword(): String {
        val existing = prefs.getString(KEY_MARIADB_ROOT_PASSWORD, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = generateRandomPassword(16)
        prefs.edit().putString(KEY_MARIADB_ROOT_PASSWORD, generated).apply()
        return generated
    }

    fun getMariaDbPassword(): String {
        return prefs.getString(KEY_MARIADB_ROOT_PASSWORD, "") ?: ""
    }

    fun setMariaDbPassword(password: String) {
        prefs.edit().putString(KEY_MARIADB_ROOT_PASSWORD, password).apply()
    }

    private fun generateRandomPassword(length: Int): String {
        val random = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            val idx = random.nextInt(CHAR_POOL.length)
            sb.append(CHAR_POOL[idx])
        }
        return sb.toString()
    }
}
