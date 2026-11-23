package io.github.aimindseye.sablehealth.core

/** Illustrative chart statuses, never inferred from health data or presented as clinical risk. */
enum class DemoBand { INFORMATIONAL, GOAL_MET, BELOW_PLAN, SYNTHETIC_OUTLIER }

/** Deterministic synthetic data; no Fitbit, Health Connect, or medical-scoring dependence. */
object DemoCharts {
    const val TODAY_STEPS = 7420
    const val TODAY_GOAL_STEPS = 9000

    private const val MON_STEPS = 4208f
    private const val TUE_STEPS = 7038f
    private const val WED_STEPS = 9200f
    private const val THU_STEPS = 7421f
    private const val FRI_STEPS = 7189f
    private const val SAT_STEPS = 5936f
    private const val SUN_STEPS = 7420f
    private const val MON_HR = 63f
    private const val TUE_HR = 64f
    private const val WED_HR = 62f
    private const val THU_HR = 61f
    private const val FRI_HR = 62f
    private const val SAT_HR = 60f
    private const val SUN_HR = 61f
    private const val DEEP_MINUTES = 84
    private const val REM_MINUTES = 108
    private const val LIGHT_MINUTES = 270
    private const val TOTAL_ASLEEP_MINUTES = 462

    val steps = listOf(MON_STEPS, TUE_STEPS, WED_STEPS, THU_STEPS, FRI_STEPS, SAT_STEPS, SUN_STEPS)
    val restingHr = listOf(MON_HR, TUE_HR, WED_HR, THU_HR, FRI_HR, SAT_HR, SUN_HR)
    val sleepStageMinutes = listOf(DEEP_MINUTES, REM_MINUTES, LIGHT_MINUTES)
    val stepBands =
        listOf(
            DemoBand.SYNTHETIC_OUTLIER,
            DemoBand.INFORMATIONAL,
            DemoBand.GOAL_MET,
            DemoBand.INFORMATIONAL,
            DemoBand.INFORMATIONAL,
            DemoBand.BELOW_PLAN,
            DemoBand.INFORMATIONAL,
        )

    fun todayGoalFraction(): Float = (TODAY_STEPS.toFloat() / TODAY_GOAL_STEPS).coerceIn(0f, 1f)

    fun stagesMatchTotalSleep(): Boolean = sleepStageMinutes.sum() == TOTAL_ASLEEP_MINUTES

    fun validDemoBands(): Boolean =
        stepBands.size == steps.size &&
            stepBands[2] == DemoBand.GOAL_MET &&
            steps[2] >= TODAY_GOAL_STEPS &&
            steps.last().toInt() == TODAY_STEPS
}
