package io.github.aimindseye.sablehealth.core

/** Deterministic seven-day demonstration only. Never used by the real data-source layer. */
object DemoCharts {
    private const val MON_STEPS = 5900f
    private const val TUE_STEPS = 7900f
    private const val WED_STEPS = 6600f
    private const val THU_STEPS = 8700f
    private const val FRI_STEPS = 7200f
    private const val SAT_STEPS = 8200f
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

    val steps: List<Float> = listOf(
        MON_STEPS, TUE_STEPS, WED_STEPS, THU_STEPS, FRI_STEPS, SAT_STEPS, SUN_STEPS,
    )
    val restingHr: List<Float> = listOf(
        MON_HR, TUE_HR, WED_HR, THU_HR, FRI_HR, SAT_HR, SUN_HR,
    )
    val sleepStageMinutes: List<Int> = listOf(DEEP_MINUTES, REM_MINUTES, LIGHT_MINUTES)

    fun stagesMatchTotalSleep(): Boolean = sleepStageMinutes.sum() == TOTAL_ASLEEP_MINUTES
}
