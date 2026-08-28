package com.example.speedlab.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementCalculationTest {
    @Test
    fun oneMegabitPerSecondIsCalculatedFromMonotonicInterval() {
        assertEquals(1.0, mbpsFromBytes(125_000L, 1_000_000_000L), 0.000001)
    }

    @Test
    fun unitsConvertFromCanonicalMbps() {
        assertEquals(100.0, SpeedUnit.MBPS.fromMbps(100.0), 0.0)
        assertEquals(12.5, SpeedUnit.MB_PER_SEC.fromMbps(100.0), 0.0)
        assertEquals(100_000.0, SpeedUnit.KBPS.fromMbps(100.0), 0.0)
        assertEquals(0.1, SpeedUnit.GBPS.fromMbps(100.0), 0.0)
    }

    @Test
    fun dataSaverHasMateriallyLowerTrafficCap() {
        val saver = estimatedMaximumBytes(AppSettings(profile = TestProfile.DATA_SAVER))
        val balanced = estimatedMaximumBytes(AppSettings(profile = TestProfile.BALANCED))
        assertTrue(saver < balanced / 4)
    }

    @Test
    fun uploadOnlyUsesOneDirectionBudget() {
        val settings = AppSettings(profile = TestProfile.BALANCED, testMode = TestMode.UPLOAD)
        val cap = settings.measurementConfig().maxTransferredBytes
        assertTrue(estimatedMaximumBytes(settings) <= cap + 32L * 1_024)
    }
}
