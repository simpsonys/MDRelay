package com.simpsonys.mdrelay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilityTest {

    @Test
    fun testDetectSharedUrl_withValidUrl() {
        val text = "Check out this amazing article at https://example.com/some/path?param=value and read it."
        val url = detectSharedUrl(text)
        assertEquals("https://example.com/some/path?param=value", url)
    }

    @Test
    fun testDetectSharedUrl_withHttpUrl() {
        val text = "Go to http://www.google.com for search."
        val url = detectSharedUrl(text)
        assertEquals("http://www.google.com", url)
    }

    @Test
    fun testDetectSharedUrl_withTrailingPunctuation() {
        // trailing characters: ), ], }, ., , should be trimmed
        assertEquals("https://example.com", detectSharedUrl("URL is (https://example.com)"))
        assertEquals("https://example.com", detectSharedUrl("URL is [https://example.com]"))
        assertEquals("https://example.com", detectSharedUrl("URL is {https://example.com}"))
        assertEquals("https://example.com", detectSharedUrl("URL is https://example.com."))
        assertEquals("https://example.com", detectSharedUrl("URL is https://example.com,"))
    }

    @Test
    fun testDetectSharedUrl_whenNoUrlPresent() {
        val text = "There is no web link in this description."
        val url = detectSharedUrl(text)
        assertNull(url)
    }

    @Test
    fun testSanitizeFilenameSegment_normalTitle() {
        val result = sanitizeFilenameSegment("My Special Report")
        assertEquals("My-Special-Report", result)
    }

    @Test
    fun testSanitizeFilenameSegment_stripsMdExtension() {
        // Should strip trailing .md extensions
        assertEquals("my-notes", sanitizeFilenameSegment("my-notes.md"))
        assertEquals("my-notes", sanitizeFilenameSegment("my-notes.MD"))
        assertEquals("my-notes", sanitizeFilenameSegment("my-notes.md.md"))
    }

    @Test
    fun testSanitizeFilenameSegment_stripsTimestampPrefix() {
        // YYYY-MM-DD_HHmm_ prefix should be stripped
        assertEquals("my-document", sanitizeFilenameSegment("2026-05-31_2045_my-document"))
        assertEquals("some-other-file", sanitizeFilenameSegment("2025-12-01_0900_some-other-file"))
    }

    @Test
    fun testSanitizeFilenameSegment_invalidCharacters() {
        // \ / : * ? " < > | \p{Cntrl} replaced by -. Note that '?' cuts the string!
        val result = sanitizeFilenameSegment("report/2026:special*char?file")
        assertEquals("report-2026-special-char", result)
    }

    @Test
    fun testSanitizeFilenameSegment_whitespaceAndCollapsingDashes() {
        // spaces replaced by - and multiple - collapsed to one
        val result = sanitizeFilenameSegment("hello     world  test---doc")
        assertEquals("hello-world-test-doc", result)
    }

    @Test
    fun testSanitizeFilenameSegment_lengthLimitation() {
        // Should be limited to 40 characters
        val longTitle = "a".repeat(60)
        val result = sanitizeFilenameSegment(longTitle)
        assertEquals("a".repeat(40), result)
    }

    @Test
    fun testSanitizeFilenameSegment_reservedWordsOrBlank() {
        // Reserved words ("md-relay", "shared-text", "mobile-capture") should return null
        assertNull(sanitizeFilenameSegment("md-relay"))
        assertNull(sanitizeFilenameSegment("SHARED-TEXT"))
        assertNull(sanitizeFilenameSegment("mobile-capture"))
        assertNull(sanitizeFilenameSegment(""))
        assertNull(sanitizeFilenameSegment("   "))
        assertNull(sanitizeFilenameSegment(null))
    }

    @Test
    fun testExtractFilenameFromUrl_simpleUrl() {
        val url = "https://example.com/notes/weekly-report.md"
        val filename = extractFilenameFromUrl(url)
        assertEquals("weekly-report.md", filename)
    }

    @Test
    fun testExtractFilenameFromUrl_userGistUrlWithQueryParams() {
        val url = "https://gist.githubusercontent.com/simpsonys/7bc1e71327a966e644c5f2a28597f199/raw/acbc5de7e430206c85ebfeadcb4d5b736c58b2e7/SIMPSONYS_FINANCE_Afternoon_2026-05-31.md?response-content-disposition=attachment?"
        val filename = extractFilenameFromUrl(url)
        assertEquals("SIMPSONYS_FINANCE_Afternoon_2026-05-31.md", filename)
    }

    @Test
    fun testExtractFilenameFromUrl_fallbackWhenReservedOrEmpty() {
        // "md-relay" is a reserved word and sanitized to null
        val url = "https://example.com/files/md-relay"
        val filename = extractFilenameFromUrl(url)
        assertEquals("web-capture.md", filename)
    }
}
