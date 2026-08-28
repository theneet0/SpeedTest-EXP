package com.example.speedlab.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationValidationTest {
    @Test
    fun defaultSettingsAreValid() {
        assertTrue(AppSettings().validationErrors().isEmpty())
    }

    @Test
    fun unsafeCustomValuesAreRejectedAndResolvedValuesAreClamped() {
        val settings = AppSettings(
            profile = TestProfile.CUSTOM,
            customConnections = 99,
            customDownloadSeconds = 1,
            customMaxDataMb = 100_000,
        )
        assertTrue(settings.validationErrors().isNotEmpty())
        val resolved = settings.measurementConfig()
        assertEquals(16, resolved.parallelConnections)
        assertEquals(2_048L * 1_024 * 1_024, resolved.maxTransferredBytes)
        assertEquals(2_000L, resolved.downloadDurationMillis)
    }

    @Test
    fun customServerRequiresHttpsEndpoints() {
        val settings = AppSettings(
            serverMode = ServerMode.CUSTOM,
            customDownloadUrl = "http://example.com/down",
            customUploadUrl = "https://example.com/up",
        )
        assertTrue(settings.validationErrors().any { "HTTPS download" in it })
    }
}
