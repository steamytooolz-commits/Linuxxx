package com.example

import com.example.core.RootfsDownloader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class RootfsDownloaderTest {

    @Test
    fun `candidateUrls contains valid URLs and mirrors`() {
        val downloader = RootfsDownloader()
        val urls = downloader.candidateUrls

        assertFalse("Candidate URLs must not be empty", urls.isEmpty())

        for (urlStr in urls) {
            val url = URL(urlStr)
            assertEquals("All candidate URLs must use HTTPS", "https", url.protocol)
            assertTrue(
                "Must be a .tar.xz or .tar.gz archive: $urlStr",
                urlStr.endsWith(".tar.xz") || urlStr.endsWith(".tar.gz")
            )
            assertTrue(
                "Must target Ubuntu Noble or 24.04: $urlStr",
                urlStr.contains("noble") || urlStr.contains("24.04")
            )
        }
    }

    private fun assertEquals(message: String, expected: String, actual: String) {
        org.junit.Assert.assertEquals(message, expected, actual)
    }
}
