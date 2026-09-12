package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer

class LinuxStackApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
        try {
            com.example.core.EmbeddedDatabaseStackServer.getInstance(this).startServers()
        } catch (e: Exception) {
            android.util.Log.w("LinuxStackApp", "Embedded database stack auto-start: ${e.message}")
        }
    }
}
