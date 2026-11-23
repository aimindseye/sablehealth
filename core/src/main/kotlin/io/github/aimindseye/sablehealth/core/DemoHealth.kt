package io.github.aimindseye.sablehealth.core

/** Entirely synthetic; never mixed into Health Connect or recorded as actual user data. */
data class DemoMetric(
    val label: String,
    val value: String,
    val detail: String,
)

object DemoHealth {
    val today =
        listOf(
            DemoMetric("STEPS", "7,420", "Goal 9,000 · Demo"),
            DemoMetric("LAST NIGHT", "7h 42m", "Sleep · Demo"),
            DemoMetric("RESTING HR", "61 bpm", "Measured · Demo"),
            DemoMetric("HRV", "43 ms", "Overnight · Demo"),
        )
    val sleep =
        listOf(
            DemoMetric("TIME ASLEEP", "7h 42m", "11:15 PM – 7:18 AM"),
            DemoMetric("DEEP SLEEP", "1h 24m", "Synthetic sleep stage"),
            DemoMetric("REM", "1h 48m", "Synthetic sleep stage"),
            DemoMetric("LIGHT SLEEP", "4h 30m", "Synthetic sleep stage"),
        )
    val move =
        listOf(
            DemoMetric("STEPS", "7,420", "9,000 daily goal"),
            DemoMetric("ACTIVE", "42 min", "Synthetic active minutes"),
            DemoMetric("DISTANCE", "5.4 km", "Synthetic walk distance"),
        )
    val trendSummary =
        listOf(
            DemoMetric("STEPS", "7 days", "Synthetic activity history"),
            DemoMetric("RESTING HR", "7 days", "Synthetic heart-rate history"),
        )
    val vitals =
        listOf(
            DemoMetric("HEART RATE", "74 bpm", "Synthetic sample at 10:24"),
            DemoMetric("RESTING HR", "61 bpm", "Synthetic daily average"),
            DemoMetric("HRV", "43 ms", "Synthetic overnight value"),
            DemoMetric("SPO2", "—", "Not available in this demo"),
        )
}
