package io.github.aimindseye.sablehealth.core

import java.time.Duration

private const val PERCENT = 100.0
private const val NANOS_PER_SECOND = 1_000_000_000.0

private val asleepTypes = setOf("Asleep (unspecified)", "Light", "Deep", "REM")
private val classifiedTypes = asleepTypes + setOf("Awake", "Awake in bed", "Out of bed")

enum class EfficiencyStatus {
    COMPLETE,
    BED_LOCATION_UNKNOWN,
    NO_IN_BED_TIME,
    INCOMPLETE_STAGES,
}

data class SleepEfficiency(
    val status: EfficiencyStatus,
    val sessionPercent: Double? = null,
    val inBedPercent: Double? = null,
    val asleepSeconds: Double = 0.0,
    val inBedSeconds: Double? = null,
)

/**
 * Sable sleep-efficiency v1: only a complete classified stage partition is eligible.
 * Generic Awake does not establish location. A session ratio is not an in-bed ratio.
 */
fun sleepEfficiency(session: LiveSleep): SleepEfficiency {
    val stages = session.stages.sortedBy { it.start }
    if (!completePartition(session, stages)) return SleepEfficiency(EfficiencyStatus.INCOMPLETE_STAGES)
    val asleep = stages.filter { it.type in asleepTypes }.sumOf(::stageSeconds)
    val total = seconds(Duration.between(session.start, session.end))
    val sessionPercent = PERCENT * asleep / total
    return if (stages.any { it.type == "Awake" }) {
        SleepEfficiency(EfficiencyStatus.BED_LOCATION_UNKNOWN, sessionPercent, asleepSeconds = asleep)
    } else {
        val bed = stages.filter { it.type != "Out of bed" }.sumOf(::stageSeconds)
        val status = if (bed > 0.0) EfficiencyStatus.COMPLETE else EfficiencyStatus.NO_IN_BED_TIME
        val efficiency = if (bed > 0.0) PERCENT * asleep / bed else null
        SleepEfficiency(status, sessionPercent, efficiency, asleep, bed)
    }
}

private fun completePartition(
    session: LiveSleep,
    stages: List<LiveStage>,
): Boolean {
    if (session.start >= session.end || stages.isEmpty()) return false
    val bounded = stages.first().start == session.start && stages.last().end == session.end
    val classified = stages.all { it.start < it.end && it.type in classifiedTypes }
    return bounded && classified && stages.zipWithNext().all { (left, right) -> left.end == right.start }
}

private fun stageSeconds(stage: LiveStage): Double = seconds(Duration.between(stage.start, stage.end))

private fun seconds(duration: Duration): Double = duration.seconds + duration.nano / NANOS_PER_SECOND
