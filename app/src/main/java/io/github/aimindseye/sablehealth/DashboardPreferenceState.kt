package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DashboardWidgetSelection
import io.github.aimindseye.sablehealth.core.WidgetStyle

/** Preferences are local; choices do not imply an imported or exportable health record. */
internal class DashboardPreferenceState(
    private val preferences: SharedPreferences,
) {
    var widgets by mutableStateOf(DashboardWidgetSelection.restore(preferences.getString("dashboard.widgets.v1", null)))
        private set
    private val dashboardStyles =
        mutableStateMapOf<DashboardWidget, WidgetStyle>().apply {
            DashboardWidget.entries.forEach { widget ->
                this[widget] =
                    DashboardWidgetSelection.styleFor(
                        widget,
                        preferences.getString("dashboard.style.${widget.name}", null),
                    )
            }
        }

    fun styleFor(widget: DashboardWidget): WidgetStyle {
        return dashboardStyles[widget] ?: DashboardWidgetSelection.styleFor(widget, null)
    }

    fun save(
        widgets: List<DashboardWidget>,
        styles: Map<DashboardWidget, WidgetStyle>,
    ) {
        require(DashboardWidgetSelection.isValid(widgets)) { "Select exactly four distinct widgets" }
        this.widgets = widgets.toList()
        val edit = preferences.edit().putString("dashboard.widgets.v1", DashboardWidgetSelection.serialize(widgets))
        widgets.forEach { widget ->
            val style = DashboardWidgetSelection.styleFor(widget, styles[widget]?.name)
            dashboardStyles[widget] = style
            edit.putString("dashboard.style.${widget.name}", style.name)
        }
        edit.apply()
    }
}
