package io.github.aimindseye.sablehealth

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.aimindseye.sablehealth.core.CoverageState
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class M1DiscoveryCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unavailableDoesNotOfferScan() {
        show(DiscoveryState(), false)
        compose.onNodeWithText("Unavailable").assertIsDisplayed()
        compose.onNodeWithText("Scan allowed records").assertDoesNotExist()
    }

    @Test
    fun selectionDenialAndRetryAreVisible() {
        val state = DiscoveryState()
        var requested = emptySet<String>()
        show(state, request = { requested = it })
        click("Request selected READ access")
        assertEquals(3, requested.size)
        compose.runOnIdle { state.permissionResult(emptySet()) }
        visible("No permissions granted · you can retry")
        compose.runOnIdle { state.permissionResult(requested) }
        visible("Permission screen closed · select Scan to verify records")
        M1HealthConnectProbe.metrics.take(3).forEach { click("✓ ${it.name}") }
        click("Request selected READ access")
        visible("Select at least one category")
        click("○ Steps")
        click("Request selected READ access")
        assertEquals(setOf(M1HealthConnectProbe.metrics.first().permission), requested)
    }

    @Test
    fun scanResultsRequireReviewBeforeSharing() {
        val row =
            HealthMetricCoverage(
                type = "Steps",
                permission = "read.steps",
                state = CoverageState.RECORDS_FOUND,
                recordCount = 2,
                originPackages = listOf("com.example.writer"),
            )
        val state = DiscoveryState { _, _ -> listOf(row) }
        show(state)
        click("Scan allowed records")
        visible("Finished · record counts are not step or sample totals")
        visible("Writer apps: com.example.writer")
        compose.onNodeWithText("Share reviewed report").assertDoesNotExist()
        click("Review coverage report")
        visible("This includes timestamps and writer app names. Share only if you choose.")
        click("Share reviewed report")
        val chooser = shadowOf(compose.activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val shared = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals("text/plain", shared?.type)
        val report = shared?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(report.contains("RECORD_MEASUREMENT_VALUES_INCLUDED=false"))
        click("Review coverage report")
        compose.onNodeWithText("Share reviewed report").assertDoesNotExist()
    }

    @Test
    fun failedScanAndPermissionScreenCanRetry() {
        val state = DiscoveryState { _, _ -> error("synthetic failure") }
        show(state, request = { error("synthetic permission failure") })
        click("Request selected READ access")
        visible("Permission screen unavailable · check Health Connect")
        click("Scan allowed records")
        visible("Scan unavailable · check Health Connect and retry")
        compose.runOnIdle { assertTrue(!state.scanning && state.results.isEmpty()) }
    }

    @Test
    fun sourcesCoverageLinkNavigatesAndBackReturns() {
        val state = SableAppState(compose.activity.getSharedPreferences("coverage-test", Context.MODE_PRIVATE))
        state.page = Page.SOURCES
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { SourcesPage(state) }
            }
        }
        click("Check data availability")
        compose.runOnIdle {
            assertEquals(Page.M1_DISCOVERY, state.page)
            state.back()
            assertEquals(Page.SOURCES, state.page)
        }
    }

    private fun show(
        state: DiscoveryState,
        available: Boolean = true,
        request: (Set<String>) -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { M1DiscoveryPage(state, available, request) }
            }
        }
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }
}
