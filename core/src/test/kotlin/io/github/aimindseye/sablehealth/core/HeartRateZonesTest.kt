package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateZonesTest {
    @Test
    fun percentMaximumAndReserveGoldenVectors() {
        val maximum = heartRateZones(200)
        val reserve = heartRateZones(200, 60)
        assertEquals(listOf(100.0, 120.0, 140.0, 160.0, 180.0), maximum.map { it.lowerBpm })
        assertEquals(listOf(120.0, 140.0, 160.0, 180.0, 200.0), maximum.map { it.upperBpm })
        assertEquals(listOf(130.0, 144.0, 158.0, 172.0, 186.0), reserve.map { it.lowerBpm })
        assertEquals(listOf(144.0, 158.0, 172.0, 186.0, 200.0), reserve.map { it.upperBpm })
        assertEquals(listOf(50, 60, 70, 80, 90), reserve.map { it.lowerPercent })
        assertEquals(listOf(60, 70, 80, 90, 100), reserve.map { it.upperPercent })
    }

    @Test
    fun invalidAndMissingInputsAreNotEstimated() {
        listOf(null, 0, -1, 301).forEach { assertTrue(heartRateZones(it).isEmpty()) }
        listOf(-1, 0, 180, 181).forEach { assertTrue(heartRateZones(180, it).isEmpty()) }
        assertEquals(5, heartRateZones(300, 1).size)
    }

    @Test
    fun fractionalBoundariesAreContinuous() {
        val bands = heartRateZones(193, 57)
        assertEquals(125.0, bands.first().lowerBpm, 1e-9)
        assertEquals(138.6, bands.first().upperBpm, 1e-9)
        bands.zipWithNext().forEach { (left, right) ->
            assertEquals(left.upperBpm, right.lowerBpm, 1e-9)
        }
        assertEquals(193.0, bands.last().upperBpm, 1e-9)
    }
}
