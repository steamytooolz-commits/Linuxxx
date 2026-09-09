package com.example.core

import android.content.Context
import java.io.File

data class LinuxDistroInfo(
    val id: String,
    val name: String,
    val version: String,
    val architecture: String = "aarch64",
    val description: String,
    val estimatedSizeExpandedMb: Int,
    val defaultShell: String = "/bin/bash"
)

/**
 * PRoot-Distro & Container Manager for Android.
 * Integrates with the official Termux 'proot-distro' package.
 */
class PRootDistroManager(
    private val context: Context,
    private val linuxEnvManager: LinuxEnvManager
) {
    companion object {
        private const val TAG = "PRootDistroManager"
    }

    val distrosBaseDir: File
        get() = File(linuxEnvManager.PREFIX, "var/lib/proot-distro/installed-rootfs")

    val availableDistros = listOf(
        LinuxDistroInfo(
            id = "ubuntu",
            name = "Ubuntu Linux 24.04 LTS",
            version = "24.04",
            description = "Full glibc container with apt, build-essential, python3 & nodejs.",
            estimatedSizeExpandedMb = 245,
            defaultShell = "/bin/bash"
        ),
        LinuxDistroInfo(
            id = "debian",
            name = "Debian",
            version = "Stable",
            description = "Stable Debian userland with dpkg, apt-get, systemd-shim.",
            estimatedSizeExpandedMb = 210,
            defaultShell = "/bin/bash"
        ),
        LinuxDistroInfo(
            id = "alpine",
            name = "Alpine Linux",
            version = "Latest",
            description = "Lightweight musl-libc security-oriented Linux container.",
            estimatedSizeExpandedMb = 180,
            defaultShell = "/bin/sh"
        )
    )

    fun getDistroDir(distroId: String): File {
        return File(distrosBaseDir, distroId)
    }

    fun isDistroInstalled(distroId: String): Boolean {
        val dir = getDistroDir(distroId)
        return dir.exists() && dir.isDirectory
    }

    fun getInstalledDistros(): List<LinuxDistroInfo> {
        return availableDistros.filter { isDistroInstalled(it.id) }
    }

    /**
     * Calculates total disk footprint in MB of all installed Linux container distros.
     */
    fun calculateTotalDistroSizeMb(): Long {
        if (!distrosBaseDir.exists()) return 0L
        var totalBytes = 0L
        distrosBaseDir.walkTopDown().forEach { file ->
            if (file.isFile) totalBytes += file.length()
        }
        return totalBytes / (1024 * 1024)
    }
}
