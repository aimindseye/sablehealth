package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChartPolicyTest {
    @Test
    fun validPreferencesAndBounds() {
        LiveMetric.entries.forEach { metric ->
            assertTrue(ChartPolicy.restore(metric, "invalid") in ChartPolicy.options(metric))
            ChartPolicy.options(metric).forEach { kind ->
                assertEquals(kind, ChartPolicy.restore(metric, kind.name))
            }
        }
        assertEquals(ChartKind.LINE, ChartPolicy.restore(LiveMetric.HRV, "AREA"))
        assertEquals(-2.0..4.0, ChartPolicy.bounds(listOf(-2.0, null, 4.0), ChartKind.BAR))
        assertEquals(0.0..4.0, ChartPolicy.bounds(listOf(2.0, 4.0), ChartKind.AREA))
        assertEquals(-4.0..0.0, ChartPolicy.bounds(listOf(-4.0, -2.0), ChartKind.BAR))
        assertEquals(60.0..61.0, ChartPolicy.bounds(listOf(60.0), ChartKind.LINE))
        assertEquals(0.0..1.0, ChartPolicy.bounds(listOf(null, Double.NaN), ChartKind.POINTS))
        assertEquals(0, ChartPolicy.indexAt(-1f, 7))
        assertEquals(3, ChartPolicy.indexAt(.5f, 7))
        assertEquals(6, ChartPolicy.indexAt(1f, 7))
    }

    @Test
    fun gapsAndAmbiguousStagesDoNotBecomeTransitions() {
        val start = Instant.parse("2026-11-24T04:00:00Z")
        val light = LiveStage(start, start.plusSeconds(60), "Light")
        val deep = LiveStage(light.end, start.plusSeconds(180), "Deep")
        val rem = LiveStage(start.plusSeconds(240), start.plusSeconds(300), "REM")
        val invalid = LiveStage(start.minusSeconds(60), start, "Unknown")
        val session = LiveSleep(start, start.plusSeconds(360), "fixture", listOf(rem, invalid, deep, light))
        assertEquals(listOf(light, deep, rem), SleepPlotPolicy.intervals(session))
        assertTrue(SleepPlotPolicy.contiguous(light, deep))
        assertFalse(SleepPlotPolicy.contiguous(deep, rem))
        assertEquals(mapOf("Light" to 60L, "Deep" to 120L, "REM" to 60L), SleepPlotPolicy.secondsByStage(session))
        assertEquals(.5f, SleepPlotPolicy.fraction(session, deep.end), .001f)
        assertEquals(0f, SleepPlotPolicy.fraction(session, invalid.start), .001f)
        assertEquals(1f, SleepPlotPolicy.fraction(session, session.end.plusSeconds(60)), .001f)
        val overlap = LiveStage(start.plusSeconds(30), light.end, "Awake")
        assertEquals(listOf(deep, rem), SleepPlotPolicy.intervals(session.copy(stages = session.stages + overlap)))
    }
}
