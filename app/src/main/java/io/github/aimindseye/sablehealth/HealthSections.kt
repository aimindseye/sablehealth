package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        Text(
            "Baseline: finding your usual · no recovery score calculated",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        EmptyState()
    }
    Notice("Source: ${status.label}", status.detail, onClick = onSources)
}

@Composable
internal fun SleepSection(demo: Boolean) {
    SectionHeading("Sleep", "Last night · sleep sessions and stages")
    if (demo) {
        TwoUp(DemoHealth.sleep)
        SleepStageChart()
    } else {
        EmptyState()
    }
}

@Composable
internal fun MoveSection(demo: Boolean) {
    SectionHeading("Move", "Daily activity and movement")
    if (demo) {
        TwoUp(DemoHealth.move)
        StepsChart()
    } else {
        EmptyState()
    }
}

@Composable
internal fun VitalsSection(demo: Boolean) {
    SectionHeading("Vitals", "Recorded values, not diagnoses")
    if (demo) {
        TwoUp(DemoHealth.vitals)
        RestingHeartRateChart()
    } else {
        EmptyState()
    }
}

@Composable
internal fun TrendsSection(demo: Boolean) {
    SectionHeading("Trends", "Last seven days · synthetic examples")
    if (demo) {
        StepsChart()
        RestingHeartRateChart()
    } else {
        Notice(
            "Not enough history",
            "Trend charts are withheld until real data is available and coverage is sufficient.",
        )
    }
}
