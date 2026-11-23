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
import io.github.aimindseye.sablehealth.core.DemoMetric

@Composable
internal fun Dashboard(
    section: Section,
    demo: Boolean,
    status: ConnectStatus,
    onSources: () -> Unit,
) {
    when (section) {
        Section.TODAY -> TodaySection(demo, status, onSources)
        Section.SLEEP -> SleepSection(demo)
        Section.MOVE -> MoveSection(demo)
        Section.VITALS -> VitalsSection(demo)
        Section.TRENDS -> TrendsSection(demo)
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
internal fun TwoUp(items: List<DemoMetric>) {
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
internal fun Metrics(items: List<DemoMetric>) {
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
internal fun EmptyState() {
    Notice("No records imported", "This is a foundation build. Actual Health Connect import is scheduled for M1.")
}
