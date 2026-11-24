package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DashboardWidgetSelection
import io.github.aimindseye.sablehealth.core.WidgetCategory
import io.github.aimindseye.sablehealth.core.WidgetStyle

/** Draft edits do not overwrite current Dashboard until all four slots are saved. */
@Composable
internal fun WidgetPickerPage(state: SableAppState) {
    var selected by remember(state) { mutableStateOf(state.dashboard.widgets.toList()) }
    var styles by remember(state) {
        mutableStateOf(DashboardWidget.entries.associateWith { state.dashboard.styleFor(it) })
    }
    SectionHeading("Customize Dashboard", "Choose four widgets · order sets their layout")
    Notice(
        "Dashboard layout",
        "Slots 1–2: hero cards · Slot 3: wide visual · Slot 4: compact row. " +
            "Source status is always shown separately.",
    )
    val selectionCount = "Selected: ${selected.size} of ${DashboardWidgetSelection.COUNT}"
    Text(selectionCount, style = MaterialTheme.typography.titleMedium)
    selected.forEachIndexed { index, widget ->
        SelectedWidgetRow(
            widget,
            index,
            selected.size,
            styles[widget] ?: WidgetStyle.TEXT,
            WidgetRowActions(
                onMove = { offset -> selected = DashboardWidgetSelection.move(selected, index, offset) },
                onRemove = { selected = DashboardWidgetSelection.toggle(selected, widget) },
                onStyle = { style -> styles = styles + (widget to style) },
            ),
        )
    }
    if (selected.size < DashboardWidgetSelection.COUNT) {
        Notice("Choose ${DashboardWidgetSelection.COUNT - selected.size} more", "Add widgets below before saving.")
    }
    WidgetCatalogSelection(selected, onToggle = { selected = DashboardWidgetSelection.toggle(selected, it) })
    FocusButton("Restore four defaults", {
        selected = DashboardWidgetSelection.defaults
        styles =
            DashboardWidget.entries.associateWith {
                DashboardWidgetSelection.styleFor(it, null)
            }
    })
    if (DashboardWidgetSelection.isValid(selected)) {
        FocusButton("Save four Dashboard widgets", {
            state.dashboard.save(selected, styles)
            state.section = Section.DASHBOARD
            state.page = Page.MAIN
        })
    } else {
        Text("Save is available once exactly four widgets are selected.", style = MaterialTheme.typography.bodySmall)
    }
    FocusButton("Cancel · back to Settings", { state.page = Page.SETTINGS })
}

private data class WidgetRowActions(
    val onMove: (Int) -> Unit,
    val onRemove: () -> Unit,
    val onStyle: (WidgetStyle) -> Unit,
)

@Composable
private fun SelectedWidgetRow(
    widget: DashboardWidget,
    index: Int,
    count: Int,
    mode: WidgetStyle,
    actions: WidgetRowActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${index + 1}. ${widget.label}",
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (index > 0) FocusButton("↑", { actions.onMove(-1) })
            if (index < count - 1) FocusButton("↓", { actions.onMove(1) })
            FocusButton("Remove", actions.onRemove)
        }
        if (widget.supportsGraph) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Display", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                FocusButton("Text", { actions.onStyle(WidgetStyle.TEXT) }, selected = mode == WidgetStyle.TEXT)
                FocusButton("Graph", { actions.onStyle(WidgetStyle.GRAPH) }, selected = mode == WidgetStyle.GRAPH)
            }
        }
    }
}

@Composable
private fun WidgetCatalogSelection(
    selected: List<DashboardWidget>,
    onToggle: (DashboardWidget) -> Unit,
) {
    WidgetCategory.entries.forEach { category ->
        Text(category.title, style = MaterialTheme.typography.titleMedium)
        DashboardWidget.entries.filter { it.category == category }.forEach { widget ->
            val checked = widget in selected
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(widget.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        widget.phase.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // At four, remove a current choice before adding a replacement.
                if (checked || selected.size < DashboardWidgetSelection.COUNT) {
                    FocusButton(if (checked) "Remove" else "Add", { onToggle(widget) }, selected = checked)
                } else {
                    Text("4/4", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Notice(
        "M0 limitation",
        "All numbers are synthetic demonstrations. Fitbit Air exports and live " +
            "Health Connect data must be verified in M1. Unverified widgets stay empty, never zero.",
    )
}
