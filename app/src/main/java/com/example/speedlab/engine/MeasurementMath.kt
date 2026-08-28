package com.example.speedlab.engine

import kotlin.math.abs

object MeasurementMath {
    fun robustThroughputMbps(samples: List<Double>): Double {
        val valid = samples.filter { it.isFinite() && it >= 0.0 }
        if (valid.isEmpty()) return 0.0

        val steady = valid.drop((valid.size * 0.20).toInt().coerceAtMost(valid.lastIndex))
        if (steady.size < 5) return steady.average()

        val sorted = steady.sorted()
        val trim = (sorted.size * 0.10).toInt().coerceAtLeast(1)
        val trimmed = sorted.subList(trim, sorted.size - trim)
        return if (trimmed.isEmpty()) sorted.average() else trimmed.average()
    }

    fun averageMbps(samples: List<Double>): Double =
        samples.filter { it.isFinite() && it >= 0.0 }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

    fun jitterMillis(latencies: List<Double>): Double {
        if (latencies.size < 2) return 0.0
        return latencies.zipWithNext { first, second -> abs(second - first) }.average()
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}
