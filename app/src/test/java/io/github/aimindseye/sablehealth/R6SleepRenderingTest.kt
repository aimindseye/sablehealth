package io.github.aimindseye.sablehealth

import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class R6SleepRenderingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val blue = Color(0xFF1466B9)
    private val rose = Color(0xFFAF345D)
    private val missing = Color(0xFF656D75)
    private val palette = lightColorScheme(primary = blue, tertiary = rose, outlineVariant = missing)
    private val deep = Color(0xFF7775C8).toArgb()
    private val rem = Color(0xFFAA92D9).toArgb()
    private val start = Instant.parse("2026-11-24T04:00:00Z")
    private val zone = ZoneId.of("America/New_York")
    private val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), zone)
    private val session =
        LiveSleep(
            start,
            start.plusSeconds(3600),
            "fixture",
            listOf(
                stage(0, 600, "Light"),
                stage(600, 1200, "Deep"),
                stage(1200, 1500, "REM"),
                stage(1500, 1800, "Awake"),
                stage(2400, 2700, "Awake in bed"),
                stage(2700, 3000, "Out of bed"),
                stage(3000, 3300, "Asleep (unspecified)"),
                stage(3300, 3600, "Unknown"),
            ),
        )

    @Test
    fun recordedTransitionsAndLabelModesRenderWithStageColors() {
        val preferences = preferences()
        compose.setContent {
            MaterialTheme(colorScheme = palette) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    R6SleepSessionGraph(session, zone, preferences)
                }
            }
        }
        val key = render("Sleep stage transitions with recorded times")
        assertStageInk(key)
        assertTimelineGap(key)
        compose.onNodeWithContentDescription("Next stage").performClick()
        visible("Light → Deep → REM")
        val pattern = if (DateFormat.is24HourFormat(compose.activity)) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val from = formatter.format(start.plusSeconds(600).atZone(zone))
        val to = formatter.format(start.plusSeconds(1200).atZone(zone))
        visible("$from – $to")
        val selected = render("Sleep stage transitions with recorded times")
        check(!key.sameAs(selected))
        compose.runOnIdle { preferences.chooseTransitionLabels("Selected") }
        val selectedOnly = render("Sleep stage transitions with recorded times")
        check(!key.sameAs(selectedOnly))
        compose.runOnIdle { preferences.chooseTransitionLabels("All times") }
        visible("Scroll the timeline; select a stage for both exact times.")
        render("Sleep stage transitions with recorded times")
        compose.runOnIdle { preferences.chooseTransitionLabels("Key times") }
        val density = compose.activity.resources.displayMetrics.density
        compose
            .onNodeWithContentDescription("Sleep stage transitions with recorded times")
            .performTouchInput {
                val left = 49f * density
                val x = left + (width - left - 6f * density) * .46f
                click(Offset(x, center.y))
            }
        visible("Awake")
        render("Sleep stage transitions with recorded times")
    }

    @Test
    fun donutRendersMeasuredStagesAndUnknownTimeSeparately() {
        val preferences = preferences()
        preferences.chooseSleepGraph("Donut")
        val current = mutableStateOf(session)
        compose.setContent {
            MaterialTheme(colorScheme = palette) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    R6SleepSessionGraph(current.value, zone, preferences)
                }
            }
        }
        visible("Unrecorded: 10 min")
        val image = render("Sleep stage proportions including unrecorded time")
        assertStageInk(image)
        check(image.countInk(missing.toArgb()) > 0)
        compose.runOnIdle {
            current.value = session.copy(stages = listOf(stage(0, 3600, "Deep")))
        }
        compose.onNodeWithText("Unrecorded: 10 min").assertDoesNotExist()
        val complete = render("Sleep stage proportions including unrecorded time")
        check(complete.countInk(deep) > 0 && complete.countInk(missing.toArgb()) == 0)
        compose.runOnIdle { current.value = session.copy(stages = emptyList()) }
        visible("No stages exported")
    }

    @Test
    fun stackedHistoryRendersGapsAndSelectsActualNights() {
        val earlierEnd = session.end.minusSeconds(86400)
        val prior = LiveSleep(earlierEnd.minusSeconds(1800), earlierEnd, "fixture", emptyList())
        val snapshot = LiveSnapshot(window, emptyMap(), listOf(session, prior))
        val days = mutableStateOf(7)
        compose.setContent {
            MaterialTheme(colorScheme = palette) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    R6SleepHistoryBars(snapshot, days.value)
                }
            }
        }
        visible("2026-11-24 · 60 min session")
        val image = render("Sleep stage composition by night")
        assertStageInk(image)
        check(image.countInk(missing.toArgb()) > 0)
        compose.onNodeWithContentDescription("Previous night").performScrollTo().performClick()
        visible("2026-11-23 · 30 min session")
        compose.onNodeWithContentDescription("Previous night").performScrollTo().performClick()
        visible("2026-11-22 · No record")
        compose.onNodeWithContentDescription("Next night").performScrollTo().performClick()
        visible("2026-11-23 · 30 min session")
        compose.runOnIdle { days.value = 30 }
        visible("2/30 nights")
        render("Sleep stage composition by night")
        compose
            .onNodeWithContentDescription("Sleep stage composition by night")
            .performScrollTo()
            .performTouchInput { click(center) }
        visible("2026-11-10 · No record")
    }

    private fun stage(
        from: Long,
        to: Long,
        type: String,
    ) = LiveStage(start.plusSeconds(from), start.plusSeconds(to), type)

    private fun preferences(): R6Preferences {
        val store = compose.activity.getSharedPreferences("r6-render-sleep", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        return R6Preferences(store)
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertStageInk(image: Bitmap) {
        listOf(blue.toArgb(), rose.toArgb(), rem, deep).forEach { color ->
            check(image.countInk(color) > 0) { "Recorded stage missing from rendering: $color" }
        }
    }

    private fun assertTimelineGap(image: Bitmap) {
        val density = compose.activity.resources.displayMetrics.density
        val left = 49f * density
        val plot = image.width - left - 6f * density
        val from = (left + plot * .55f).roundToInt()
        val to = (left + plot * .60f).roundToInt()
        listOf(blue.toArgb(), rose.toArgb(), rem, deep).forEach { color ->
            check(image.countInk(color, from, to) == 0) { "Unrecorded interval must remain a gap" }
        }
    }

    private fun render(description: String): Bitmap {
        val node = compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
    }
}
