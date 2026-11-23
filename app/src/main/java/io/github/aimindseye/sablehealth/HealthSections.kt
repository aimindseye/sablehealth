package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.aimindseye.sablehealth.core.DemoHealth

@Composable
internal fun SleepSection(state: SableAppState) {
    ViewModeHeading(Section.SLEEP, "Last night · sessions and stages", state)
    if (!state.demo) {
        EmptyState()
    } else if (state.modeFor(Section.SLEEP) == ViewMode.TEXT) {
        TwoUp(DemoHealth.sleep)
    } else {
        TwoUp(DemoHealth.sleep.take(2))
        SleepStageChart()
    }
}

@Composable
internal fun MoveSection(
    state: SableAppState,
    compactQ25: Boolean,
) {
    ViewModeHeading(Section.MOVE, "Activity and seven-day steps", state)
    if (!state.demo) {
        EmptyState()
    } else if (state.modeFor(Section.MOVE) == ViewMode.TEXT) {
        TwoUp(DemoHealth.move)
    } else {
        TwoUp(DemoHealth.move.take(2))
        StepsChart(compact = compactQ25)
    }
}

@Composable
internal fun VitalsSection(
    state: SableAppState,
    compactQ25: Boolean,
) {
    ViewModeHeading(Section.VITALS, "Daily values and heart-rate history", state)
    if (!state.demo) {
        EmptyState()
    } else if (state.modeFor(Section.VITALS) == ViewMode.TEXT) {
        TwoUp(DemoHealth.vitals)
    } else {
        TwoUp(DemoHealth.vitals.take(2))
        RestingHeartRateChart(compact = compactQ25)
    }
}

@Composable
internal fun TrendsSection(
    state: SableAppState,
    compactQ25: Boolean,
) {
    ViewModeHeading(Section.TRENDS, "Historical comparisons · synthetic", state)
    if (!state.demo) {
        Notice(
            "Not enough history",
            "Trend charts are withheld until real data is available and coverage is sufficient.",
        )
    } else if (state.modeFor(Section.TRENDS) == ViewMode.TEXT) {
        TwoUp(DemoHealth.trendSummary)
    } else {
        StepsChart(compact = compactQ25)
        RestingHeartRateChart(compact = compactQ25)
    }
}
