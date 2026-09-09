package com.example.di

import android.content.Context
import com.example.core.BootstrapExtractor
import com.example.core.LinuxEnvManager
import com.example.core.PRootDistroManager
import com.example.data.local.AppDatabase
import com.example.data.repository.DatabaseRepository
import com.example.data.repository.DatabaseRepositoryImpl

/**
 * Dependency Injection container providing production-grade modular singletons
 * for database persistence, Linux environment execution, and bootstrap unpacking.
 */
interface AppContainer {
    val appDatabase: AppDatabase
    val databaseRepository: DatabaseRepository
    val linuxEnvManager: LinuxEnvManager
    val bootstrapExtractor: BootstrapExtractor
    val pRootDistroManager: PRootDistroManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepositoryImpl(appDatabase.dbLogDao())
    }

    override val linuxEnvManager: LinuxEnvManager by lazy {
        LinuxEnvManager(context)
    }

    override val bootstrapExtractor: BootstrapExtractor by lazy {
        BootstrapExtractor(context)
    }

    override val pRootDistroManager: PRootDistroManager by lazy {
        PRootDistroManager(context, linuxEnvManager)
    }
}
