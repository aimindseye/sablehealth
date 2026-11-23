package io.github.aimindseye.sablehealth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DemoHealth
import io.github.aimindseye.sablehealth.core.DemoMetric

@Composable
internal fun Dashboard(
    section: Section,
    demo: Boolean,
    status: ConnectStatus,
    onSources: () -> Unit,
) {
    when (section) {
        Section.TODAY -> {
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
        Section.SLEEP -> {
            SectionHeading("Sleep", "Last night · sleep sessions and stages")
            if (demo) Metrics(DemoHealth.sleep) else EmptyState()
            Notice(
                "Sleep quality",
                "Metrics will include sources, local wake date and coverage. " +
                    "Never treat missing sleep as zero.",
            )
        }
        Section.MOVE -> {
            SectionHeading("Move", "Activity and workouts")
            if (demo) Metrics(DemoHealth.move) else EmptyState()
            Notice("Workout history", "M2 will reconcile repeated events across sources without double counting.")
        }
        Section.VITALS -> {
            SectionHeading("Vitals", "Recorded values, not medical conclusions")
            if (demo) Metrics(DemoHealth.vitals) else EmptyState()
            Notice(
                "Reading freshness",
                "The data view will distinguish the last measurement from the last successful import.",
            )
        }
        Section.TRENDS -> {
            SectionHeading("Trends", "7 days · 30 days · 90 days")
            Notice(
                "Not enough history",
                "Time-series charts and personal comparisons are withheld until the needed coverage is present.",
            )
            if (demo) Notice("Example · 7 days", "Steps  6.9k  ·  Sleep  7h 23m  ·  values are synthetic")
        }
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TwoUp(items: List<DemoMetric>) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 300.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { metric -> Box(Modifier.weight(1f)) { MetricCard(metric) } }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Metrics(items: List<DemoMetric>) {
    items.forEach { MetricCard(it) }
}

@Composable
private fun MetricCard(metric: DemoMetric) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                metric.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                metric.value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(metric.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun Notice(title: String, detail: String, onClick: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    val outline = MaterialTheme.colorScheme.primary
    val outlineShape = MaterialTheme.shapes.medium
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 0.dp, outline, outlineShape)
            .clickable(onClick = onClick)
            .focusable()
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = outline)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyState() {
    Notice("No records imported", "This is a foundation build. Actual Health Connect import is scheduled for M1.")
}
