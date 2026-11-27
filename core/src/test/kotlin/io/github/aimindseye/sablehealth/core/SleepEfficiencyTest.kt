package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SleepEfficiencyTest {
    private val start = Instant.parse("2026-11-25T23:00:00Z")

    @Test
    fun completeInBedAndSessionRatiosAreDistinct() {
        val session = fixture("Light", "Deep", "REM", "Asleep (unspecified)", "Awake in bed", "Out of bed")
        val result = sleepEfficiency(session)
        assertEquals(EfficiencyStatus.COMPLETE, result.status)
        assertEquals(100.0 * 4 / 6, requireNotNull(result.sessionPercent), 1e-9)
        assertEquals(80.0, requireNotNull(result.inBedPercent), 1e-9)
        assertEquals(240.0, result.asleepSeconds, 1e-9)
        assertEquals(300.0, requireNotNull(result.inBedSeconds), 1e-9)
    }

    @Test
    fun genericAwakeCannotEstablishInBedTime() {
        val result = sleepEfficiency(fixture("Deep", "Awake"))
        assertEquals(EfficiencyStatus.BED_LOCATION_UNKNOWN, result.status)
        assertEquals(50.0, requireNotNull(result.sessionPercent), 1e-9)
        assertNull(result.inBedPercent)
        assertNull(result.inBedSeconds)
    }

    @Test
    fun zeroAndNoDenominatorRemainDifferent() {
        val awake = sleepEfficiency(fixture("Awake in bed"))
        assertEquals(0.0, requireNotNull(awake.inBedPercent), 1e-9)
        val outside = sleepEfficiency(fixture("Out of bed"))
        assertEquals(EfficiencyStatus.NO_IN_BED_TIME, outside.status)
        assertNull(outside.inBedPercent)
        assertEquals(0.0, requireNotNull(outside.sessionPercent), 1e-9)
    }

    @Test
    fun gapsOverlapsInvalidAndUnknownStagesAbstain() {
        val valid = fixture("Deep", "REM")
        val first = valid.stages.first()
        val second = valid.stages.last()
        val cases =
            listOf(
                valid.copy(end = valid.start),
                valid.copy(stages = emptyList()),
                valid.copy(stages = listOf(first, second.copy(start = second.start.plusSeconds(1)))),
                valid.copy(stages = listOf(first, second.copy(start = second.start.minusSeconds(1)))),
                valid.copy(stages = listOf(first, second.copy(end = second.end.plusSeconds(1)))),
                valid.copy(stages = listOf(first.copy(end = first.start), second)),
                valid.copy(stages = listOf(first.copy(type = "Unknown"), second)),
                valid.copy(stages = listOf(first.copy(type = "Future stage"), second)),
                valid.copy(stages = listOf(second)),
            )
        cases.forEach { session ->
            val result = sleepEfficiency(session)
            assertEquals(EfficiencyStatus.INCOMPLETE_STAGES, result.status)
            assertNull(result.sessionPercent)
            assertNull(result.inBedPercent)
        }
    }

    @Test
    fun unsortedSubsecondIntervalsPreserveExactDuration() {
        val end = start.plusNanos(500_000_000)
        val split = start.plusNanos(125_000_000)
        val session =
            LiveSleep(
                start,
                end,
                "fixture",
                listOf(LiveStage(split, end, "Awake in bed"), LiveStage(start, split, "Light")),
            )
        assertEquals(25.0, requireNotNull(sleepEfficiency(session).inBedPercent), 1e-9)
    }

    private fun fixture(vararg types: String): LiveSleep {
        val stages =
            types.mapIndexed { index, type ->
                val at = start.plusSeconds(index * 60L)
                LiveStage(at, at.plusSeconds(60), type)
            }
        return LiveSleep(start, stages.last().end, "fixture", stages)
    }
}
