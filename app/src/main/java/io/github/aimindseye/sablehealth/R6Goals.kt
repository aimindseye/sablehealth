package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val STEP_GOAL_2K = 2000
private const val STEP_GOAL_4K = 4000
private const val STEP_GOAL_6K = 6000
private const val STEP_GOAL_8K = 8000
private const val STEP_GOAL_10K = 10000
private const val STEP_GOAL_12K = 12000
private const val STEP_GOAL_15K = 15000
private const val RING_START_ANGLE = -90f
private val stepGoalChoices =
    listOf(
        0,
        STEP_GOAL_2K,
        STEP_GOAL_4K,
        STEP_GOAL_6K,
        STEP_GOAL_8K,
        STEP_GOAL_10K,
        STEP_GOAL_12K,
        STEP_GOAL_15K,
    )
private const val FULL_CIRCLE = 360f
private const val PERCENT = 100

@Composable
internal fun R6GoalSetting(state: SableAppState) {
    val goal = state.r6.stepGoal
    val label = if (goal == 0) "No goal" else formatNumber(goal.toDouble())
    val options = stepGoalChoices.map { if (it == 0) "No goal" else formatNumber(it.toDouble()) }
    R7aChoiceRow("Daily step goal", label, options) { choice ->
        state.r6.chooseStepGoal(stepGoalChoices[options.indexOf(choice)])
    }
}

@Composable
internal fun R6StepRing(
    steps: Double,
    goal: Int,
) {
    val fraction = (steps / goal).coerceAtLeast(0.0)
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().height(32.dp).semantics { contentDescription = "Step goal progress" }) {
            val stroke = 5.dp.toPx()
            val diameter = (minOf(size.width, size.height) - stroke).coerceAtLeast(1f)
            val start = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val dimensions = Size(diameter, diameter)
            drawArc(
                colors.outlineVariant,
                RING_START_ANGLE,
                FULL_CIRCLE,
                false,
                start,
                dimensions,
                style = Stroke(stroke),
            )
            drawArc(
                colors.primary,
                RING_START_ANGLE,
                FULL_CIRCLE * fraction.coerceAtMost(1.0).toFloat(),
                false,
                start,
                dimensions,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val progress = "${(fraction * PERCENT).roundToInt()}% · ${formatNumber(goal.toDouble())} goal"
        Text(progress, style = MaterialTheme.typography.labelSmall)
    }
}
