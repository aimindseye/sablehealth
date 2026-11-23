package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoChartsTest {
    @Test
    fun stepsAndRestingHeartRateHaveSevenSyntheticSamples() {
        assertEquals(7, DemoCharts.steps.size)
        assertEquals(7, DemoCharts.restingHr.size)
        assertEquals(7420f, DemoCharts.steps.last())
        assertEquals(61f, DemoCharts.restingHr.last())
    }

    @Test
    fun sleepStageTotalsMatchSyntheticTimeAsleep() {
        assertTrue(DemoCharts.stagesMatchTotalSleep())
        assertEquals(4, DemoHealth.sleep.size)
        assertEquals("7h 42m", DemoHealth.sleep.first().value)
    }
}
