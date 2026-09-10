package com.example.di

import android.content.Context
import com.example.core.BootstrapExtractor
import com.example.core.LinuxEnvManager
import com.example.core.PRootDistroManager
import com.example.core.UbuntuRootfsManager
import com.example.data.repository.DatabaseRepository
import com.example.data.repository.DatabaseRepositoryImpl

/**
 * Dependency Injection container providing modular singletons
 * for database persistence, Linux environment execution, and rootfs management.
 */
interface AppContainer {
    val databaseRepository: DatabaseRepository
    val linuxEnvManager: LinuxEnvManager
    val pRootDistroManager: PRootDistroManager
    val bootstrapExtractor: BootstrapExtractor
    val ubuntuRootfsManager: UbuntuRootfsManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepositoryImpl()
    }

    override val linuxEnvManager: LinuxEnvManager by lazy {
        LinuxEnvManager(context)
    }

    override val pRootDistroManager: PRootDistroManager by lazy {
        PRootDistroManager(context, linuxEnvManager)
    }

    override val bootstrapExtractor: BootstrapExtractor by lazy {
        BootstrapExtractor(context)
    }

    override val ubuntuRootfsManager: UbuntuRootfsManager by lazy {
        UbuntuRootfsManager(context)
    }
}
