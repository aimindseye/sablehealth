package io.github.aimindseye.sablehealth

import androidx.compose.runtime.Composable
import io.github.aimindseye.sablehealth.core.DemoHealth

@Composable
internal fun TodaySection(demo: Boolean, status: ConnectStatus, onSources: () -> Unit) {
    SectionHeading(
        "Today",
        if (demo) "Your day at a glance · synthetic demonstration" else
            "Connect a source to see your readings",
    )
    if (demo) {
        TwoUp(DemoHealth.today)
        Notice(
            "Finding your usual",
            "Personal baselines appear only after sufficient valid history. " +
                "No recovery score is fabricated.",
        )
    } else {
        Notice(
            "No measurements yet",
            "M0 does not import health records. Health Connect integration follows in M1.",
        )
    }
    Notice("Source status: ${status.label}", status.detail, onClick = onSources)
}

@Composable
internal fun SleepSection(demo: Boolean) {
    SectionHeading("Sleep", "Last night · sleep sessions and stages")
    if (demo) Metrics(DemoHealth.sleep) else EmptyState()
    Notice(
        "Sleep quality",
        "Metrics will include sources, local wake date and coverage. " +
            "Never treat missing sleep as zero.",
    )
}

@Composable
internal fun MoveSection(demo: Boolean) {
    SectionHeading("Move", "Activity and workouts")
    if (demo) Metrics(DemoHealth.move) else EmptyState()
    Notice("Workout history", "M2 will reconcile repeated events across sources without double counting.")
}

@Composable
internal fun VitalsSection(demo: Boolean) {
    SectionHeading("Vitals", "Recorded values, not medical conclusions")
    if (demo) Metrics(DemoHealth.vitals) else EmptyState()
    Notice(
        "Reading freshness",
        "The data view will distinguish the last measurement from the last successful import.",
    )
}

@Composable
internal fun TrendsSection(demo: Boolean) {
    SectionHeading("Trends", "7 days · 30 days · 90 days")
    Notice(
        "Not enough history",
        "Time-series charts and personal comparisons are withheld until the needed coverage is present.",
    )
    if (demo) Notice("Example · 7 days", "Steps  6.9k  ·  Sleep  7h 23m  ·  values are synthetic")
}
