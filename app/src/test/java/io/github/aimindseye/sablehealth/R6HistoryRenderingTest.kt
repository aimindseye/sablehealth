package io.github.aimindseye.sablehealth

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.aimindseye.sablehealth.core.ChartKind
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class R6HistoryRenderingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val blue = Color(0xFF1466B9)
    private val rose = Color(0xFFAF345D)
    private val palette = lightColorScheme(primary = blue, tertiary = rose)
    private val today = LocalDate.of(2026, 11, 24)
    private val week = (6L downTo 0L).map(today::minusDays)

    @Test
    fun fourActivityStylesRenderDistinctMarksAndLeaveMissingDayEmpty() {
        val kind = mutableStateOf(ChartKind.BAR)
        val values = listOf(120.0, 240.0, 80.0, null, 100.0, 250.0, 300.0)
        compose.setContent {
            MaterialTheme(colorScheme = palette) {
                R6Plot(PlotData(week, values, LiveMetric.STEPS, kind.value), selected = 6)
            }
        }
        val node = chart("Steps", 7)
        val images =
            ChartKind.entries.map { style ->
                compose.runOnIdle { kind.value = style }
                val image = render(node)
                check(image.countInk(blue.toArgb()) > 0) { "$style must render recorded observations" }
                assertMiddleGap(image)
                image
            }
        images.forEachIndexed { index, image ->
            images.take(index).forEach { earlier -> check(!image.sameAs(earlier)) }
        }
    }

    @Test
    fun signedBarsAndHeartSeriesUseRealValuesAndMetricColor() {
        val metric = mutableStateOf(LiveMetric.SKIN_TEMPERATURE)
        val days = (29L downTo 0L).map(today::minusDays)
        val temperatures = List(30) { index -> if (index % 4 == 0) null else (index % 5 - 2).toDouble() }
        val heart = List(30) { index -> if (index % 4 == 0) null else (60 + index).toDouble() }
        compose.setContent {
            MaterialTheme(colorScheme = palette) {
                val isHeart = metric.value == LiveMetric.HEART_RATE
                val kind = if (isHeart) ChartKind.LINE else ChartKind.BAR
                R6Plot(PlotData(days, if (isHeart) heart else temperatures, metric.value, kind), compact = true)
            }
        }
        val signed = render(chart("Skin temperature delta", 30))
        check(signed.countInk(blue.toArgb(), 0, signed.width) > 0)
        compose.runOnIdle { metric.value = LiveMetric.HEART_RATE }
        val pulse = render(chart("Heart rate", 30))
        check(pulse.countInk(rose.toArgb()) > 0)
        check(!signed.sameAs(pulse))
    }

    @Test
    fun exactReadingAndMissingDayAreInspectableWithoutInventedZero() {
        val values = listOf(10.0, 20.0, 0.0, null, 50.0, 60.0, 70.0)
        val points =
            week.zip(values).map { (date, value) ->
                value?.let { LivePoint(date, it, null, emptySet()) }
            }
        compose.setContent {
            MaterialTheme {
                R6HistoryChart(week, points, LiveMetric.STEPS, ChartKind.BAR)
            }
        }
        compose.onNodeWithText("70").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous day").performClick()
        compose.onNodeWithText("60").assertIsDisplayed()
        val density = compose.activity.resources.displayMetrics.density
        chart("Steps", 7).performTouchInput {
            val left = 43f * density
            val x = left + (width - left - 12f * density) / 2f
            click(Offset(x, center.y))
        }
        compose.onNodeWithText("No record").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous day").performClick()
        compose.onNodeWithText("0").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next day").performClick()
        compose.onNodeWithText("No record").assertIsDisplayed()
        render(chart("Steps", 7))
    }

    private fun assertMiddleGap(image: Bitmap) {
        val density = compose.activity.resources.displayMetrics.density
        val left = 43f * density
        val cell = (image.width - left - 12f * density) / 7f
        val middle = left + 3.5f * cell
        val start = (middle - cell / 5f).roundToInt()
        val end = (middle + cell / 5f).roundToInt()
        check(image.countInk(blue.toArgb(), start, end) == 0) { "Missing day must not receive a data mark" }
    }

    private fun chart(
        metric: String,
        days: Int,
    ) = compose.onNodeWithContentDescription(
        "$metric, $days days; gaps are missing data",
    )

    private fun render(node: SemanticsNodeInteraction): Bitmap {
        node.assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
    }
}
