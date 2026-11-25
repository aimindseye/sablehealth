package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.aimindseye.sablehealth.core.CoverageState
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import io.github.aimindseye.sablehealth.core.SetupEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SourceOverviewCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val permission = M1HealthConnectProbe.metrics.first().permission

    private fun app(): SableAppState {
        val preferences = compose.activity.getSharedPreferences("source-overview-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        return SableAppState(preferences)
    }

    private fun grant(state: SableAppState) {
        state.connect = ConnectStatus("Available", "Synthetic", 1, setOf(permission), setOf(permission))
    }

    private fun seed(state: SableAppState) {
        grant(state)
        state.discovery.results =
            listOf(
                HealthMetricCoverage(
                    "Steps",
                    permission,
                    CoverageState.RECORDS_FOUND,
                    recordCount = 2,
                    lastObservedUtc = Instant.now().toString(),
                    originPackages = listOf("com.example.writer"),
                ),
            )
        state.discovery.scannedAt = Instant.now().toString()
    }

    @Test
    fun sharedRecordCoverageAndRevocation() {
        val state = app()
        seed(state)
        val snapshot = state.discovery
        var opened: SetupEntry? = null
        state.setupAction = { opened = it }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (state.page == Page.SOURCE_COVERAGE) {
                        R7aCoverageCategory(state)
                    } else {
                        M1DiscoveryPage(state.discovery, appState = state)
                    }
                }
            }
        }
        click("Activity")
        visible("Writer apps: com.example.writer")
        compose.onNodeWithText("Request selected READ access").assertDoesNotExist()
        click("Manage permissions")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, opened)
        compose.runOnIdle {
            grant(state)
            assertSame(snapshot, state.discovery)
            state.connect = ConnectStatus("Available", "Synthetic", 0, emptySet(), setOf(permission))
            assertNotSame(snapshot, state.discovery)
            assertTrue(state.discovery.results.isEmpty())
        }
        compose.onNodeWithText("Writer apps: com.example.writer").assertDoesNotExist()
        compose.onNodeWithText("Scan allowed records").assertDoesNotExist()
        compose.onNodeWithText("Share reviewed report").assertDoesNotExist()
    }

    @Test
    fun sourcesOverviewShowsObservedMetadataAndRoutesMissingAccess() {
        val state = app()
        seed(state)
        var opened: SetupEntry? = null
        state.setupAction = { opened = it }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (state.page == Page.SOURCE_DIAGNOSTICS) {
                        R7aSourceDiagnostics(state)
                    } else {
                        SourcesPage(state)
                    }
                }
            }
        }
        visible("Types found: 1")
        visible("1/1 allowed")
        click("Health permissions")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, opened)
        compose.runOnIdle { state.recheckPermissions() }
        visible("Not verified")
        click("Sync diagnostics")
        click("Refresh local records")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, opened)
        compose.runOnIdle { state.page = Page.SOURCES }
        click("Check data availability")
        compose.runOnIdle { assertEquals(Page.M1_DISCOVERY, state.page) }
    }

    @Test
    fun refreshIsExplicitAndSharesTheScanWithCoverageDetail() {
        var reads = 0
        val state =
            SableAppState(compose.activity.getSharedPreferences("refresh-test", Context.MODE_PRIVATE)) {
                DiscoveryState { _, now ->
                    reads++
                    listOf(
                        HealthMetricCoverage(
                            "Steps",
                            permission,
                            CoverageState.RECORDS_FOUND,
                            recordCount = 1,
                            lastObservedUtc = now.toString(),
                            originPackages = listOf("com.fitbit.FitbitMobile"),
                        ),
                    )
                }
            }
        grant(state)
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (state.page) {
                        Page.M1_DISCOVERY -> M1DiscoveryPage(state.discovery, appState = state)
                        Page.SOURCE_COVERAGE -> R7aCoverageCategory(state)
                        Page.SOURCE_DIAGNOSTICS -> R7aSourceDiagnostics(state)
                        else -> SourcesPage(state)
                    }
                }
            }
        }
        assertEquals(0, reads)
        visible("Not verified")
        click("Sync diagnostics")
        click("Refresh local records")
        compose.runOnIdle { state.page = Page.SOURCES }
        visible("Types found: 1")
        assertEquals(1, reads)
        compose.runOnIdle { state.page = Page.SOURCES }
        click("Check data availability")
        click("Activity")
        visible("Writer apps: com.fitbit.FitbitMobile")
        assertEquals(1, reads)
    }

    @Test
    fun detailsAndPrivacyKeepWriterSeparateFromCompanion() {
        val state = app()
        seed(state)
        state.page = Page.SOURCE_COMPANION
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (state.page) {
                        Page.SOURCE_PRIVACY -> SourcePrivacyPage()
                        Page.SOURCE_COMPANION -> R7aCompanionPage(state)
                        else -> SourceDetailPage(state)
                    }
                }
            }
        }
        visible("com.example.writer")
        visible("Last observed (UTC)")
        click("Paired and synced")
        compose.runOnIdle { assertTrue(state.companion.confirmed) }
        compose.runOnIdle { state.page = Page.SOURCE_DETAIL }
        click("Check availability again")
        compose.runOnIdle {
            assertTrue(state.discovery.results.isEmpty())
            state.page = Page.SOURCE_PRIVACY
        }
        visible("Scan metadata is transient. Review the report before explicitly sharing it.")
    }

    @Test
    fun settingsKeepsPreferencesAndCanonicalWizardEntries() {
        val state = app()
        grant(state)
        var opened: SetupEntry? = null
        state.setupAction = { opened = it }
        val widgets = state.dashboard.widgets.toList()
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (state.page == Page.SETTINGS_SETUP) {
                        R7aSetupSettings(state)
                    } else {
                        SettingsPage(state, DeviceProfile.Q25)
                    }
                }
            }
        }
        click("Permissions & setup")
        visible("1/1 allowed · 0 not granted")
        click("Run complete setup")
        assertEquals(SetupEntry.REVIEW_ALL, opened)
        click("Choose setup steps")
        assertEquals(SetupEntry.SELECTED_STEPS, opened)
        click("Check permissions")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, opened)
        compose.runOnIdle { state.page = Page.SETTINGS }
        choose("Appearance", "Dark")
        choose("Data mode", "No records")
        compose.runOnIdle {
            assertEquals("dark", state.appearance)
            assertTrue(!state.demo)
            assertEquals(widgets, state.dashboard.widgets.toList())
        }
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun choose(
        label: String,
        value: String,
    ) {
        compose.onNodeWithContentDescription(label).performScrollTo().performClick()
        compose.onNodeWithText(value).performClick()
        compose.waitForIdle()
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }
}
