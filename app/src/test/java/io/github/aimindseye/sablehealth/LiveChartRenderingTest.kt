package io.github.aimindseye.sablehealth

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveStage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveChartRenderingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun negativeZeroPositiveAndMissingHistoryHaveDifferentDrawnRegions() {
        val today = LocalDate.of(2026, 11, 24)
        val days = (3L downTo 0L).map(today::minusDays)
        val points =
            listOf(
                LivePoint(days[0], -1.0, null, emptySet()),
                null,
                LivePoint(days[2], 0.0, null, emptySet()),
                LivePoint(days[3], 2.0, null, emptySet()),
            )
        compose.setContent {
            MaterialTheme {
                HistoryChart(days, points, LiveMetric.SKIN_TEMPERATURE)
            }
        }
        val node = compose.onNodeWithContentDescription("Skin temperature delta, 4 days; gaps are missing data")
        node.assertIsDisplayed()
        val image = renderChart(node)
        val missing = image.getPixel(image.width * 3 / 8, image.height / 2)
        val positive = image.getPixel(image.width * 7 / 8, image.height / 3)
        check(missing != positive) { "Missing history must not draw the same bar as a measured positive value" }
    }

    @Test
    fun allStageColorsAndGapsAreRenderedFromSessionIntervals() {
        val start = Instant.parse("2026-11-24T05:00:00Z")
        val names =
            listOf("Deep", "REM", "Light", "Awake", "Unknown", "Awake in bed", "Out of bed", "Asleep (unspecified)")
        val stages =
            names.mapIndexed { index, name ->
                val offset = index * 120L
                LiveStage(start.plusSeconds(offset), start.plusSeconds(offset + 60), name)
            }
        val session = LiveSleep(start, start.plusSeconds(960), "fixture.writer", stages)
        compose.setContent {
            MaterialTheme {
                Column { SleepTimeline(session) }
            }
        }
        val node = compose.onNodeWithContentDescription("Recorded sleep stages; gaps unknown")
        node.assertIsDisplayed()
        val image = renderChart(node)
        val deep = image.getPixel(image.width / 32, image.height / 2)
        val gap = image.getPixel(image.width * 3 / 32, image.height / 2)
        val rem = image.getPixel(image.width * 5 / 32, image.height / 2)
        check(deep != gap && rem != gap && deep != rem) { "Stages must be distinct from gaps and each other" }
    }

    /** Draw the actual Compose view into a bitmap; avoid Robolectric window PixelCopy/redraw waits. */
    private fun renderChart(node: SemanticsNodeInteraction): Bitmap {
        val bounds = node.fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle {
            val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            val root = content.getChildAt(0)
            val image = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(image))
            Bitmap.createBitmap(
                image,
                bounds.left.roundToInt(),
                bounds.top.roundToInt(),
                bounds.width.roundToInt(),
                bounds.height.roundToInt(),
            )
        }
    }
}
