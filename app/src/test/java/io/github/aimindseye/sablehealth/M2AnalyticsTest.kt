package io.github.aimindseye.sablehealth

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.EfficiencyStatus
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h411dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class M2AnalyticsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val start = Instant.parse("2026-11-26T05:00:00Z")
    private val middle = start.plusSeconds(3600)
    private val end = middle.plusSeconds(900)

    @Test
    fun sleepDetailsExposeEvidenceAndAbstention() {
        val sleep =
            LiveSleep(
                start,
                end,
                "fixture.writer",
                listOf(LiveStage(start, middle, "Deep"), LiveStage(middle, end, "Awake in bed")),
            )
        val current = mutableStateOf(sleep)
        show("dark") { M2SleepDetails(current.value) }
        visible("80%")
        clickRow("Sleep efficiency")
        visible("Calculate & verify · sleep v1")
        visible("Writer: fixture.writer")
        visible("Recorded in bed")
        capture("dark-sleep")
        compose.runOnIdle { current.value = sleep.copy(stages = emptyList()) }
        visible("Not available")
        clickRow("Sleep efficiency")
        visible("Missing, unknown, invalid or overlapping stages. No estimate.")
        compose.runOnIdle {
            val stages = listOf(LiveStage(start, middle, "Deep"), LiveStage(middle, end, "Awake"))
            current.value = sleep.copy(stages = stages, writer = "")
        }
        clickRow("Sleep efficiency")
        visible("Awake location unknown. Only the session ratio is available.")
        visible("Writer: Unknown")
        visible("Asleep / session")
        compose.runOnIdle { current.value = sleep.copy(stages = listOf(LiveStage(start, end, "Out of bed"))) }
        clickRow("Sleep efficiency")
        visible("No recorded in-bed time; division is undefined.")
        check(EfficiencyStatus.entries.map(::efficiencyReason).distinct().size == EfficiencyStatus.entries.size)
    }

    @Test
    fun zoneCalculatorSupportsManualInputsAndClearsThemWhenClosed() {
        show("light") { M2HeartRateCalculator() }
        clickRow("Heart-rate zones")
        visible("Enter a whole-number maximum", substring = true)
        field("Maximum HR (bpm)", "200")
        visible("Maximum × fraction")
        visible("100–120 bpm")
        field("Maximum HR (bpm)", "123456789")
        visible("Enter a whole-number maximum", substring = true)
        field("Maximum HR (bpm)", "200")
        field("Resting HR (optional)", "60")
        visible("Rest + (maximum − rest) × fraction")
        visible("130–144 bpm")
        capture("light-zones")
        field("Resting HR (optional)", "200")
        visible("Enter a whole-number maximum", substring = true)
        compose
            .onNodeWithTag("r7a-action-Clear inputs")
            .performScrollTo()
            .performClick()
        visible("Enter a whole-number maximum", substring = true)
        field("Maximum HR (bpm)", "180")
        clickRow("Heart-rate zones")
        clickRow("Heart-rate zones")
        visible("Enter a whole-number maximum", substring = true)
        check(manualZones("180", "invalid").isEmpty())
        check(manualZones("invalid", "").isEmpty())
        check(manualZones(" 200 ", " 60 ").first().lowerBpm == 130.0)
    }

    @Test
    fun historyShowsMethodWritersAndMissingDataWithoutInterpolation() {
        val point = LivePoint(start.atOffset(ZoneOffset.UTC).toLocalDate(), 40.0, start, setOf("fixture.writer"))
        val records = mutableStateOf<List<LivePoint?>>(listOf(null, point))
        val status = mutableStateOf<LiveStatus?>(LiveStatus.PARTIAL)
        show("dark") { M2HistoryEvidence(LiveMetric.HRV, status.value, records.value, ZoneOffset.UTC) }
        clickRow("Method and sources")
        visible(historyMethod(LiveMetric.HRV))
        visible("Coverage: Partial history · method v1")
        visible("40 ms")
        visible(observation(point, ZoneOffset.UTC))
        capture("dark-provenance")
        compose.runOnIdle {
            records.value = listOf(null)
            status.value = null
        }
        visible("Coverage: Not loaded · method v1")
        visible("No observations in this period.")
        clickRow("Method and sources")
        check(LiveMetric.entries.all { historyMethod(it).isNotBlank() })
        check("not the lowest" in historyMethod(LiveMetric.RESTING_HR))
        check("not time asleep" in historyMethod(LiveMetric.SLEEP_DURATION))
    }

    private fun show(
        appearance: String,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            SableHealthTheme(appearance) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) { content() }
            }
        }
    }

    private fun clickRow(title: String) {
        compose.onNodeWithTag("r7a-row-$title").performScrollTo().performClick()
    }

    private fun field(
        title: String,
        value: String,
    ) {
        compose.onNodeWithTag("m2-bpm-$title").performScrollTo().performTextReplacement(value)
    }

    private fun visible(
        value: String,
        substring: Boolean = false,
    ) {
        compose.onNodeWithText(value, substring = substring).performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String) {
        val bounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val image = compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
        val directory = File("build/reports/m2-visual")
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "$name.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
