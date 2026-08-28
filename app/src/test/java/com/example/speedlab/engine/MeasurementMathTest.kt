package com.example.speedlab.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementMathTest {
    @Test
    fun robustResultDropsRampUpAndOutliers() {
        val samples = listOf(0.0, 0.0, 100.0, 101.0, 99.0, 100.0, 100.0, 102.0, 98.0, 100.0)
        assertEquals(100.0, MeasurementMath.robustThroughputMbps(samples), 0.0001)
    }

    @Test
    fun jitterUsesConsecutiveLatencyDifferences() {
        assertEquals(4.0, MeasurementMath.jitterMillis(listOf(10.0, 14.0, 18.0)), 0.0001)
    }

    @Test
    fun medianWorksForOddAndEvenProbeCounts() {
        assertEquals(3.0, MeasurementMath.median(listOf(5.0, 1.0, 3.0)), 0.0)
        assertEquals(2.5, MeasurementMath.median(listOf(4.0, 1.0, 3.0, 2.0)), 0.0)
    }
}
