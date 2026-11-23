package io.github.aimindseye.sablehealth.core

/** Catalog is UI configuration, not evidence that a Fitbit metric is exported to Health Connect. */
enum class WidgetCategory(
    val title: String,
) {
    ACTIVITY("Activity"),
    SLEEP("Sleep"),
    VITALS("Vitals"),
    INSIGHTS("Insights & sources"),
}

enum class WidgetPhase(
    val description: String,
) {
    M1_CANDIDATE("Health Connect candidate · verify with a connected device"),
    LATER_UNVERIFIED("Future / availability unverified"),
    DERIVED_LATER("Future derived metric · sufficient history required"),
    LOCAL_STATUS("Local app status"),
}

enum class DashboardWidget(
    val label: String,
    val category: WidgetCategory,
    val phase: WidgetPhase,
    val supportsGraph: Boolean = false,
) {
    STEPS("Steps", WidgetCategory.ACTIVITY, WidgetPhase.M1_CANDIDATE, true),
    HEART_RATE("Heart rate", WidgetCategory.VITALS, WidgetPhase.M1_CANDIDATE),
    SLEEP_DURATION("Sleep duration", WidgetCategory.SLEEP, WidgetPhase.M1_CANDIDATE),
    SLEEP_STAGES("Sleep stages", WidgetCategory.SLEEP, WidgetPhase.M1_CANDIDATE, true),
    RESTING_HR("Resting heart rate", WidgetCategory.VITALS, WidgetPhase.M1_CANDIDATE),
    HRV("HRV", WidgetCategory.VITALS, WidgetPhase.M1_CANDIDATE),
    ACTIVE_MINUTES("Active minutes", WidgetCategory.ACTIVITY, WidgetPhase.M1_CANDIDATE),
    DISTANCE("Distance", WidgetCategory.ACTIVITY, WidgetPhase.M1_CANDIDATE),
    CALORIES("Calories burned", WidgetCategory.ACTIVITY, WidgetPhase.M1_CANDIDATE),
    EXERCISE_SESSIONS("Exercise sessions", WidgetCategory.ACTIVITY, WidgetPhase.M1_CANDIDATE),
    SLEEP_SCHEDULE("Sleep schedule", WidgetCategory.SLEEP, WidgetPhase.M1_CANDIDATE),
    RESPIRATORY_RATE("Respiratory rate", WidgetCategory.VITALS, WidgetPhase.M1_CANDIDATE),
    SPO2("SpO₂", WidgetCategory.VITALS, WidgetPhase.LATER_UNVERIFIED),
    SKIN_TEMPERATURE("Skin temperature variation", WidgetCategory.VITALS, WidgetPhase.M1_CANDIDATE),
    HEART_RATE_ZONES("Heart rate zones", WidgetCategory.ACTIVITY, WidgetPhase.DERIVED_LATER),
    CARDIO_FITNESS("Cardio fitness", WidgetCategory.VITALS, WidgetPhase.LATER_UNVERIFIED),
    WEEKLY_ACTIVITY_GOAL("Weekly activity goal", WidgetCategory.ACTIVITY, WidgetPhase.DERIVED_LATER),
    READINESS("Readiness", WidgetCategory.INSIGHTS, WidgetPhase.DERIVED_LATER),
    PERSONAL_BASELINE("Personal baseline", WidgetCategory.INSIGHTS, WidgetPhase.DERIVED_LATER),
    DATA_FRESHNESS("Data freshness details", WidgetCategory.INSIGHTS, WidgetPhase.LOCAL_STATUS),
}

enum class WidgetStyle { TEXT, GRAPH }

/** Exactly four distinct IDs, with safe recovery from corrupted/older preferences. */
object DashboardWidgetSelection {
    const val COUNT = 4
    val defaults =
        listOf(
            DashboardWidget.STEPS,
            DashboardWidget.HEART_RATE,
            DashboardWidget.SLEEP_STAGES,
            DashboardWidget.HRV,
        )

    fun restore(raw: String?): List<DashboardWidget> {
        val parsed =
            raw
                .orEmpty()
                .split(',')
                .mapNotNull { token ->
                    DashboardWidget.entries.firstOrNull { it.name == token.trim() }
                }.distinct()
                .take(COUNT)
        return (parsed + defaults + DashboardWidget.entries).distinct().take(COUNT)
    }

    fun serialize(widgets: List<DashboardWidget>): String {
        require(isValid(widgets)) { "Dashboard requires four distinct widgets" }
        return widgets.joinToString(",") { it.name }
    }

    fun isValid(widgets: List<DashboardWidget>): Boolean = widgets.size == COUNT && widgets.distinct().size == COUNT

    fun toggle(
        widgets: List<DashboardWidget>,
        item: DashboardWidget,
    ): List<DashboardWidget> =
        when {
            item in widgets -> widgets - item
            widgets.size < COUNT -> widgets + item
            else -> widgets
        }

    fun move(
        widgets: List<DashboardWidget>,
        from: Int,
        offset: Int,
    ): List<DashboardWidget> {
        val to = from + offset
        if (from !in widgets.indices || to !in widgets.indices) return widgets
        return widgets.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun styleFor(
        widget: DashboardWidget,
        saved: String?,
    ): WidgetStyle {
        if (!widget.supportsGraph) return WidgetStyle.TEXT
        return WidgetStyle.entries.firstOrNull { it.name == saved } ?: WidgetStyle.GRAPH
    }
}

/** Sample values are opt-in demo data only. Absent entries remain visibly unavailable. */
data class WidgetDemoSample(
    val value: String,
    val detail: String,
)

object DashboardDemoSamples {
    private val samples =
        mapOf(
            DashboardWidget.STEPS to WidgetDemoSample("7,420", "of 9,000 · Demo"),
            DashboardWidget.HEART_RATE to WidgetDemoSample("74 bpm", "Latest sample · Demo"),
            DashboardWidget.SLEEP_DURATION to WidgetDemoSample("7h 42m", "Last night · Demo"),
            DashboardWidget.SLEEP_STAGES to WidgetDemoSample("7h 42m", "Last night · Demo"),
            DashboardWidget.RESTING_HR to WidgetDemoSample("61 bpm", "Resting value · Demo"),
            DashboardWidget.HRV to WidgetDemoSample("43 ms", "Overnight · Demo"),
            DashboardWidget.ACTIVE_MINUTES to WidgetDemoSample("42 min", "Active time · Demo"),
            DashboardWidget.DISTANCE to WidgetDemoSample("5.4 km", "Distance · Demo"),
            DashboardWidget.CALORIES to WidgetDemoSample("512 kcal", "Total burned · Demo"),
            DashboardWidget.DATA_FRESHNESS to WidgetDemoSample("No imports", "M0 demo · local status"),
        )

    fun forWidget(id: DashboardWidget): WidgetDemoSample? = samples[id]
}
