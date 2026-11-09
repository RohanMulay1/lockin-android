package com.rohan.lockin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun `ci release tags map to their run number`() {
        assertEquals(12, versionCodeFromTag("v1.1.12"))
        assertEquals(0, versionCodeFromTag("v1.0.0"))
    }

    @Test
    fun `malformed tags are rejected`() {
        assertNull(versionCodeFromTag("1.1.12"))
        assertNull(versionCodeFromTag("v1.1"))
        assertNull(versionCodeFromTag("v1.1.x"))
        assertNull(versionCodeFromTag("v1..2"))
    }

    @Test
    fun `only github https downloads are trusted`() {
        assertTrue(isTrustedDownloadUrl("https://github.com/RohanMulay1/lockin-android/releases/download/v1.1.12/LockIn.apk"))
        assertTrue(isTrustedDownloadUrl("https://objects.githubusercontent.com/x/LockIn.apk"))
        assertFalse(isTrustedDownloadUrl("http://github.com/x.apk"))
        assertFalse(isTrustedDownloadUrl("https://github.com.evil.io/x.apk"))
        assertFalse(isTrustedDownloadUrl("https://evil.io/github.com/x.apk"))
    }
}
