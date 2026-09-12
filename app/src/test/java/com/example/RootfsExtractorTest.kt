package com.example

import com.example.core.RootfsExtractor
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RootfsExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var extractor: RootfsExtractor
    private lateinit var outputDir: File

    @Before
    fun setup() {
        extractor = RootfsExtractor()
        outputDir = tempFolder.newFolder("rootfs_dest")
    }

    /**
     * Helper to write a tar.gz archive with specified entries.
     */
    private fun createTarGz(
        destFile: File,
        files: Map<String, ByteArray> = emptyMap(),
        symlinks: Map<String, String> = emptyMap(),
        directories: List<String> = emptyList()
    ) {
        FileOutputStream(destFile).use { fos ->
            GZIPOutputStream(fos).use { gzos ->
                TarArchiveOutputStream(gzos).use { tarOut ->
                    // Directories
                    for (dir in directories) {
                        val entry = TarArchiveEntry("$dir/")
                        tarOut.putArchiveEntry(entry)
                        tarOut.closeArchiveEntry()
                    }
                    // Symlinks
                    for ((linkPath, target) in symlinks) {
                        val entry = TarArchiveEntry(linkPath, TarArchiveEntry.LF_SYMLINK)
                        entry.linkName = target
                        tarOut.putArchiveEntry(entry)
                        tarOut.closeArchiveEntry()
                    }
                    // Regular files
                    for ((filePath, content) in files) {
                        val entry = TarArchiveEntry(filePath)
                        entry.size = content.size.toLong()
                        entry.mode = 0b111_101_101 // rwxr-xr-x
                        tarOut.putArchiveEntry(entry)
                        tarOut.write(content)
                        tarOut.closeArchiveEntry()
                    }
                }
            }
        }
    }

    @Test
    fun `extracts standard flat archive and resolves bin bash`() {
        val archive = tempFolder.newFile("standard_rootfs.tar.gz")
        val bashContent = "#!/bin/bash\necho hello\n".toByteArray()

        createTarGz(
            destFile = archive,
            directories = listOf("usr", "usr/bin"),
            symlinks = mapOf("bin" to "usr/bin"),
            files = mapOf("usr/bin/bash" to bashContent)
        )

        val result = extractor.extract(archive, outputDir) { _, _ -> }
        assertTrue("Extraction should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val bash = File(outputDir, "bin/bash")
        val usrBash = File(outputDir, "usr/bin/bash")

        assertTrue("usr/bin/bash must exist and have content", usrBash.exists() && usrBash.length() > 0)
        assertTrue("bin/bash must exist and have content", bash.exists() && bash.length() > 0)
    }

    @Test
    fun `extracts nested archive with wrapper directory and strips prefix`() {
        val archive = tempFolder.newFile("termux_ubuntu.tar.gz")
        val bashContent = "#!/bin/bash\necho noble\n".toByteArray()
        val prefix = "ubuntu-noble-aarch64"

        createTarGz(
            destFile = archive,
            directories = listOf(prefix, "$prefix/usr", "$prefix/usr/bin"),
            symlinks = mapOf("$prefix/bin" to "usr/bin"),
            files = mapOf("$prefix/usr/bin/bash" to bashContent)
        )

        val result = extractor.extract(archive, outputDir) { _, _ -> }
        assertTrue("Extraction of nested archive should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        // The prefix must be stripped so files reside directly under outputDir
        val bash = File(outputDir, "bin/bash")
        val usrBash = File(outputDir, "usr/bin/bash")

        assertTrue("usr/bin/bash should exist directly under rootfs", usrBash.exists() && usrBash.length() > 0)
        assertTrue("bin/bash should exist directly under rootfs", bash.exists() && bash.length() > 0)
        // Ensure wrapper folder was not created at destination root
        assertFalse("Wrapper folder should not remain in destDir", File(outputDir, prefix).exists())
    }

    @Test
    fun `flattenNestedRootfs promotes nested directory contents`() {
        val nestedDir = File(outputDir, "nested-ubuntu-24.04")
        val nestedUsrBin = File(nestedDir, "usr/bin").apply { mkdirs() }
        val nestedBash = File(nestedUsrBin, "bash")
        nestedBash.writeText("#!/bin/bash\necho nested\n")

        val nestedEtc = File(nestedDir, "etc").apply { mkdirs() }
        File(nestedEtc, "os-release").writeText("NAME=Ubuntu\n")

        // Before flattening: top level does not have /usr/bin/bash
        assertFalse(File(outputDir, "usr/bin/bash").exists())

        // Run flattener
        extractor.flattenNestedRootfs(outputDir)

        // After flattening: top level has /usr/bin/bash and /etc/os-release
        val promotedBash = File(outputDir, "usr/bin/bash")
        val promotedOsRelease = File(outputDir, "etc/os-release")

        assertTrue("Promoted bash must exist", promotedBash.exists() && promotedBash.length() > 0)
        assertTrue("Promoted os-release must exist", promotedOsRelease.exists())
        assertEquals("NAME=Ubuntu\n", promotedOsRelease.readText())
        assertFalse("Nested container folder should be removed", nestedDir.exists())
    }

    @Test
    fun `bootstraps bash from sh when bash binary is missing`() {
        val archive = tempFolder.newFile("sh_only_rootfs.tar.gz")
        val shContent = "#!/bin/sh\necho posix\n".toByteArray()

        createTarGz(
            destFile = archive,
            directories = listOf("usr", "usr/bin"),
            files = mapOf("usr/bin/sh" to shContent)
        )

        val result = extractor.extract(archive, outputDir) { _, _ -> }
        assertTrue("Extraction with sh fallback should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val bash = File(outputDir, "bin/bash")
        val usrBash = File(outputDir, "usr/bin/bash")

        assertTrue("bash must be bootstrapped from sh", bash.exists() && bash.length() > 0)
        assertTrue("usrBash must be bootstrapped from sh", usrBash.exists() && usrBash.length() > 0)
    }

    @Test
    fun `repairs broken zero-byte bin bash from valid usr bin bash`() {
        val archive = tempFolder.newFile("broken_symlink_rootfs.tar.gz")
        val bashContent = "#!/bin/bash\necho valid\n".toByteArray()

        createTarGz(
            destFile = archive,
            directories = listOf("bin", "usr", "usr/bin"),
            files = mapOf(
                "bin/bash" to ByteArray(0), // Corrupted 0-byte bash
                "usr/bin/bash" to bashContent
            )
        )

        val result = extractor.extract(archive, outputDir) { _, _ -> }
        assertTrue("Extraction should repair 0-byte bash: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val bash = File(outputDir, "bin/bash")
        assertTrue("bin/bash must be repaired to non-zero length", bash.exists() && bash.length() > 0)
        assertEquals(bashContent.size.toLong(), bash.length())
    }

    @Test
    fun `blocks archive path traversal attacks`() {
        val maliciousArchive = tempFolder.newFile("malicious.tar.gz")
        val payload = "hacked".toByteArray()

        FileOutputStream(maliciousArchive).use { fos ->
            GZIPOutputStream(fos).use { gzos ->
                TarArchiveOutputStream(gzos).use { tarOut ->
                    val entry = TarArchiveEntry("../../outside_file.txt")
                    entry.size = payload.size.toLong()
                    tarOut.putArchiveEntry(entry)
                    tarOut.write(payload)
                    tarOut.closeArchiveEntry()
                }
            }
        }

        val result = extractor.extract(maliciousArchive, outputDir) { _, _ -> }
        assertTrue("Path traversal should fail extraction", result.isFailure)
        assertTrue(
            "Exception should be SecurityException",
            result.exceptionOrNull() is SecurityException
        )
    }
}
