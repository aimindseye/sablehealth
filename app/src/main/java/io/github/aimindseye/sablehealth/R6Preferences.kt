package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.ChartKind
import io.github.aimindseye.sablehealth.core.ChartPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric

private const val WEEK = 7
private const val MONTH = 30

/** Persist presentation choices only; never persist measurements or record identifiers. */
internal class R6Preferences(
    private val preferences: SharedPreferences,
) {
    private val charts = mutableStateMapOf<LiveMetric, ChartKind>()
    private val periods = mutableStateMapOf<Section, Int>()
    private val details = mutableStateMapOf<Section, LiveMetric>()

    fun detailMetric(section: Section): LiveMetric? = details[section]

    fun chooseMetric(
        section: Section,
        metric: LiveMetric?,
    ) {
        if (metric == null) details.remove(section) else details[section] = metric
    }

    var stepGoal by mutableIntStateOf(preferences.getInt("r6.goal.steps", 0).coerceAtLeast(0))
        private set
    var sleepGraph by mutableStateOf(
        preferences.getString("r6.sleep.graph", null).takeIf { it in listOf("Timeline", "Donut", "Stacked") }
            ?: "Timeline",
    )
        private set
    var transitionLabels by mutableStateOf(
        preferences.getString("r6.sleep.labels", null).takeIf { it in listOf("Key times", "All times", "Selected") }
            ?: "Key times",
    )
        private set

    fun chooseStepGoal(goal: Int) {
        require(goal >= 0)
        stepGoal = goal
        preferences.edit().putInt("r6.goal.steps", goal).apply()
    }

    fun chooseSleepGraph(kind: String) {
        require(kind in listOf("Timeline", "Donut", "Stacked"))
        sleepGraph = kind
        preferences.edit().putString("r6.sleep.graph", kind).apply()
    }

    fun chooseTransitionLabels(labels: String) {
        require(labels in listOf("Key times", "All times", "Selected"))
        transitionLabels = labels
        preferences.edit().putString("r6.sleep.labels", labels).apply()
    }

    fun chart(metric: LiveMetric): ChartKind =
        charts[metric] ?: ChartPolicy.restore(metric, preferences.getString("r6.chart.${metric.name}", null))

    fun chooseChart(
        metric: LiveMetric,
        kind: ChartKind,
    ) {
        require(kind in ChartPolicy.options(metric))
        charts[metric] = kind
        preferences.edit().putString("r6.chart.${metric.name}", kind.name).apply()
    }

    fun period(section: Section): Int =
        periods[section] ?: preferences.getInt("r6.period.${section.name}", WEEK).let {
            if (it == MONTH) MONTH else WEEK
        }

    fun choosePeriod(
        section: Section,
        days: Int,
    ) {
        require(days == WEEK || days == MONTH)
        periods[section] = days
        preferences.edit().putInt("r6.period.${section.name}", days).apply()
    }
}
