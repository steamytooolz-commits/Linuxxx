package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ProotCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProotCommandBuilderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var builder: ProotCommandBuilder
    private lateinit var rootfsDir: File
    private lateinit var dataDir: File
    private lateinit var workspaceDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        builder = ProotCommandBuilder(context)
        rootfsDir = tempFolder.newFolder("rootfs")
        dataDir = tempFolder.newFolder("data")
        workspaceDir = tempFolder.newFolder("workspace")
    }

    @Test
    fun `resolves usr bin bash when present`() {
        val usrBin = File(rootfsDir, "usr/bin").apply { mkdirs() }
        File(usrBin, "bash").writeText("#!/bin/bash\n")

        val shell = builder.resolveShellPath(rootfsDir)
        assertEquals("/usr/bin/bash", shell)
    }

    @Test
    fun `resolves bin bash when usr bin bash is absent`() {
        val bin = File(rootfsDir, "bin").apply { mkdirs() }
        File(bin, "bash").writeText("#!/bin/bash\n")

        val shell = builder.resolveShellPath(rootfsDir)
        assertEquals("/bin/bash", shell)
    }

    @Test
    fun `resolves sh fallback when bash is completely absent`() {
        val bin = File(rootfsDir, "bin").apply { mkdirs() }
        File(bin, "sh").writeText("#!/bin/sh\n")

        val shell = builder.resolveShellPath(rootfsDir)
        assertEquals("/bin/sh", shell)
    }

    @Test
    fun `buildProotCommand contains essential bindings and startup script`() {
        val usrBin = File(rootfsDir, "usr/bin").apply { mkdirs() }
        File(usrBin, "bash").writeText("#!/bin/bash\n")

        val cmd = builder.buildProotCommand(rootfsDir, dataDir, workspaceDir)

        assertTrue("Must include -0 root flag", cmd.contains("-0"))
        assertTrue("Must include -r rootfs", cmd.contains("-r") && cmd.contains(rootfsDir.absolutePath))
        assertTrue("Must bind dev", cmd.contains("-b") && cmd.contains("/dev"))
        assertTrue("Must bind proc", cmd.contains("/proc"))
        assertTrue("Must bind sys", cmd.contains("/sys"))
        assertTrue("Must bind workspace", cmd.contains("${workspaceDir.absolutePath}:/root/workspace"))
        assertTrue("Must bind mysql", cmd.contains("${dataDir.absolutePath}/mysql:/var/lib/mysql"))
        assertTrue("Must bind redis", cmd.contains("${dataDir.absolutePath}/redis:/var/lib/redis"))
        assertTrue("Must bind mongodb", cmd.contains("${dataDir.absolutePath}/mongodb:/var/lib/mongodb"))
        assertEquals("/usr/bin/bash", cmd[cmd.size - 2])
        assertEquals("/start-all.sh", cmd[cmd.size - 1])
    }

    @Test
    fun `buildSetupCommand contains setup script`() {
        val usrBin = File(rootfsDir, "usr/bin").apply { mkdirs() }
        File(usrBin, "bash").writeText("#!/bin/bash\n")

        val cmd = builder.buildSetupCommand(rootfsDir, dataDir)

        assertTrue("Must include -r", cmd.contains("-r"))
        assertEquals("/usr/bin/bash", cmd[cmd.size - 2])
        assertEquals("/setup.sh", cmd[cmd.size - 1])
    }
}
