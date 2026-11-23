package io.github.aimindseye.sablehealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Visual density follows Dashboard, without sacrificing the 48dp keyboard/touch target. */
@Composable
internal fun CompactLinkRow(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusColor: Color? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier =
            modifier
                .sizeIn(minHeight = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .border(if (focused) 2.dp else 0.dp, colors.primary, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .focusable(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (statusColor != null) {
                Box(Modifier.size(8.dp).background(statusColor, CircleShape))
            }
            Text("›", color = colors.primary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun CompactSummary(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.sizeIn(minHeight = 48.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val COMPACT_SEGMENT_SPACING_DP = 4

@Composable
internal fun CompactChoiceRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(COMPACT_SEGMENT_SPACING_DP.dp)) {
                options.forEach { (value, label) ->
                    Box(Modifier.weight(1f)) {
                        CompactChoiceButton(
                            label = label,
                            selected = selected == value,
                            onClick = { onSelect(value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val activeBorder = if (focused) 2.dp else 1.dp
    val focusColor = if (focused && selected) colors.onSurface else colors.primary
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .border(
                    activeBorder,
                    if (focused || selected) focusColor else colors.outline,
                    MaterialTheme.shapes.small,
                ).semantics { this.selected = selected }
                .clickable(onClick = onClick)
                .focusable(),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.primary else colors.surfaceVariant),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun CompactPageTitle(
    title: String,
    supporting: String,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
        Text(supporting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
