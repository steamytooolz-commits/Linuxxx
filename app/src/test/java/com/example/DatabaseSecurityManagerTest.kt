package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.DatabaseSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseSecurityManagerTest {

    private lateinit var context: Context
    private lateinit var securityManager: DatabaseSecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        context.getSharedPreferences("linuxxx_db_security", Context.MODE_PRIVATE).edit().clear().commit()
        securityManager = DatabaseSecurityManager(context)
    }

    @Test
    fun `getOrCreateMariaDbPassword generates 16-char password and persists it`() {
        val password = securityManager.getOrCreateMariaDbPassword()
        assertNotNull(password)
        assertEquals("Password must be 16 characters long", 16, password.length)

        // Second call should return the exact same persisted password
        val retrieved = securityManager.getOrCreateMariaDbPassword()
        assertEquals("Subsequent call must return identical persisted password", password, retrieved)
        assertEquals("getMariaDbPassword should return the stored password", password, securityManager.getMariaDbPassword())
    }

    @Test
    fun `setMariaDbPassword overrides stored password`() {
        securityManager.getOrCreateMariaDbPassword()
        securityManager.setMariaDbPassword("custom_secure_pass_123")

        assertEquals("custom_secure_pass_123", securityManager.getMariaDbPassword())
    }

    @Test
    fun `generated passwords have high entropy across instances`() {
        val pw1 = securityManager.getOrCreateMariaDbPassword()

        context.getSharedPreferences("linuxxx_db_security", Context.MODE_PRIVATE).edit().clear().commit()
        val securityManager2 = DatabaseSecurityManager(context)
        val pw2 = securityManager2.getOrCreateMariaDbPassword()

        assertNotEquals("Consecutive generated passwords should not collide", pw1, pw2)
    }
}
