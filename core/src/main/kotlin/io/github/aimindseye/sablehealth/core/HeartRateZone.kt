package io.github.aimindseye.sablehealth.core

private const val MAX_INPUT_BPM = 300
private const val FIRST_PERCENT = 50
private const val LAST_PERCENT = 100
private const val BAND_PERCENT = 10
private const val PERCENT_SCALE = 100.0

data class HeartRateZone(
    val lowerPercent: Int,
    val upperPercent: Int,
    val lowerBpm: Double,
    val upperBpm: Double,
)

/** Five illustrative equal-width bands, not personalized exercise prescriptions. */
fun heartRateZones(
    maximum: Int?,
    resting: Int? = null,
): List<HeartRateZone> {
    if (maximum == null || maximum !in 1..MAX_INPUT_BPM) return emptyList()
    if (resting != null && resting !in 1 until maximum) return emptyList()
    val base = resting ?: 0
    val span = maximum - base
    return (FIRST_PERCENT until LAST_PERCENT step BAND_PERCENT).map { low ->
        val high = low + BAND_PERCENT
        HeartRateZone(low, high, base + span * low / PERCENT_SCALE, base + span * high / PERCENT_SCALE)
    }
}
