package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** History/detail tabs remember independent Text/Graphs modes; Dashboard uses widget-level styles. */
@Composable
internal fun ViewModeHeading(
    section: Section,
    subtitle: String,
    state: SableAppState,
) {
    val mode = state.modeFor(section)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FocusButton(
            "Text",
            { state.chooseViewMode(section, ViewMode.TEXT) },
            selected = mode == ViewMode.TEXT,
        )
        FocusButton(
            "Graphs",
            { state.chooseViewMode(section, ViewMode.GRAPHS) },
            selected = mode == ViewMode.GRAPHS,
        )
    }
}
