package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DashboardWidgetSelection
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.WidgetCategory
import io.github.aimindseye.sablehealth.core.WidgetStyle

private class R7aWidgetDraft(
    state: SableAppState,
) {
    var selected by mutableStateOf(state.dashboard.widgets.toList())
    var styles by mutableStateOf(DashboardWidget.entries.associateWith { state.dashboard.styleFor(it) })

    fun toggle(widget: DashboardWidget) {
        selected = DashboardWidgetSelection.toggle(selected, widget)
    }

    fun restore() {
        selected = DashboardWidgetSelection.defaults
        styles = DashboardWidget.entries.associateWith { DashboardWidgetSelection.styleFor(it, null) }
    }
}

@Composable
internal fun WidgetPickerPage(state: SableAppState) {
    val draft = remember(state) { R7aWidgetDraft(state) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.widgetBrowsing) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("${draft.selected.size} of ${DashboardWidgetSelection.COUNT} selected")
            if (state.widgetBrowsing) {
                R7aWidgetCatalog(draft)
                R7aAction("Return to selected widgets", { state.widgetBrowsing = false })
            } else {
                draft.selected.forEachIndexed { index, widget -> R7aWidgetRow(draft, widget, index) }
                R7aAction("Browse available widgets", { state.widgetBrowsing = true })
                R7aAction("Restore four defaults", draft::restore)
                R7aAction("Cancel · back to Settings", { state.page = Page.SETTINGS })
            }
        }
        if (!state.widgetBrowsing) {
            R7aAction(
                "Save changes",
                {
                    state.dashboard.save(draft.selected, draft.styles)
                    state.section = Section.DASHBOARD
                    state.page = Page.MAIN
                },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
                enabled = DashboardWidgetSelection.isValid(draft.selected),
            )
        }
    }
}

@Composable
private fun R7aWidgetRow(
    draft: R7aWidgetDraft,
    widget: DashboardWidget,
    index: Int,
) {
    Surface(color = r7MetricSurface(widgetTint(widget)), shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            R7aOrderMenu(draft, widget, index)
            Text(widget.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (widget.supportsGraph) {
                val current = if (draft.styles[widget] == WidgetStyle.GRAPH) "Graph" else "Text"
                R6Choice("${widget.label} display", current, listOf("Text", "Graph"), { value ->
                    val style = if (value == "Graph") WidgetStyle.GRAPH else WidgetStyle.TEXT
                    draft.styles = draft.styles + (widget to style)
                })
            } else {
                R7aNote("Text")
            }
            R7aAction(R7aPreferencesUi.ActionLabel("×", "Remove ${widget.label}"), { draft.toggle(widget) })
        }
    }
}

@Composable
private fun R7aOrderMenu(
    draft: R7aWidgetDraft,
    widget: DashboardWidget,
    index: Int,
) {
    var open by remember(widget) { mutableStateOf(false) }
    Column {
        R7aAction(R7aPreferencesUi.ActionLabel("☰", "Reorder ${widget.label}"), { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Move up") }, enabled = index > 0, onClick = {
                draft.selected = DashboardWidgetSelection.move(draft.selected, index, -1)
                open = false
            })
            DropdownMenuItem(text = { Text("Move down") }, enabled = index < draft.selected.lastIndex, onClick = {
                draft.selected = DashboardWidgetSelection.move(draft.selected, index, 1)
                open = false
            })
            DropdownMenuItem(text = { Text("Cancel") }, onClick = { open = false })
        }
    }
}

@Composable
private fun R7aWidgetCatalog(draft: R7aWidgetDraft) {
    WidgetCategory.entries.forEach { category ->
        R7aGroup(category.title) {
            DashboardWidget.entries.filter { it.category == category }.forEach { widget ->
                val selected = widget in draft.selected
                val canChoose = selected || draft.selected.size < DashboardWidgetSelection.COUNT
                R7aRow(
                    widget.label,
                    detail = widget.phase.description,
                    chosen = selected,
                    action =
                        if (canChoose) {
                            { draft.toggle(widget) }
                        } else {
                            null
                        },
                )
                R7aRule()
            }
        }
    }
    R7aNote("Slots 1–2: hero · Slot 3: wide · Slot 4: compact. Source status is separate.")
    R7aAction("Restore four defaults", draft::restore)
}

private fun widgetTint(widget: DashboardWidget): LiveMetric? =
    when (widget.category) {
        WidgetCategory.ACTIVITY -> LiveMetric.STEPS
        WidgetCategory.SLEEP -> LiveMetric.SLEEP_DURATION
        WidgetCategory.VITALS ->
            if (widget == DashboardWidget.HEART_RATE || widget == DashboardWidget.RESTING_HR) {
                LiveMetric.HEART_RATE
            } else {
                LiveMetric.HRV
            }
        WidgetCategory.INSIGHTS -> null
    }
