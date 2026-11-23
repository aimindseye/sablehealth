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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DemoMetric

@Composable
internal fun Dashboard(
    state: SableAppState,
    compactQ25: Boolean,
    onSources: () -> Unit,
) {
    when (state.section) {
        Section.TODAY -> TodaySection(state, onSources)
        Section.SLEEP -> SleepSection(state)
        Section.MOVE -> MoveSection(state, compactQ25)
        Section.VITALS -> VitalsSection(state, compactQ25)
        Section.TRENDS -> TrendsSection(state, compactQ25)
    }
}

@Composable
internal fun SectionHeading(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val TWO_COLUMN_MIN_WIDTH_DP = 320
private const val MAX_TWO_COLUMN_FONT_SCALE = 1.3f

@Composable
internal fun TwoUp(items: List<DemoMetric>) {
    BoxWithConstraints {
        val twoColumns =
            maxWidth >= TWO_COLUMN_MIN_WIDTH_DP.dp &&
                LocalConfiguration.current.fontScale <= MAX_TWO_COLUMN_FONT_SCALE
        val columns = if (twoColumns) 2 else 1
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
private fun MetricCard(metric: DemoMetric) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                metric.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                metric.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                metric.detail,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun Notice(
    title: String,
    detail: String,
    onClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val outline = MaterialTheme.colorScheme.primary
    val outlineShape = MaterialTheme.shapes.medium
    val modifier =
        if (onClick == null) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
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
