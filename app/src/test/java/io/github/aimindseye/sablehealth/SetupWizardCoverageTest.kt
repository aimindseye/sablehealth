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
import androidx.health.connect.client.HealthConnectClient
import io.github.aimindseye.sablehealth.core.CoverageState
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import io.github.aimindseye.sablehealth.core.SetupEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupWizardCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun state(): SableAppState {
        val preferences = compose.activity.getSharedPreferences("wizard-coverage", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        return SableAppState(preferences)
    }

    @Test
    fun completeSetupNavigatesAndCommitsOnlyAtFinish() {
        val state = state()
        var finished = false
        showWizard(state, SetupEntry.REVIEW_ALL) { finished = it }
        visible("Welcome")
        click("Next")
        click("Companion setup")
        assertFalse(state.companion.confirmed)
        click("Next")
        visible("Read permissions")
        click("Next")
        visible("Verify")
        click("Next")
        click("No records")
        assertTrue(state.demo)
        click("Next")
        visible("Review")
        click("Back")
        visible("Personalize")
        click("Next")
        click("Finish")
        assertTrue(finished)
        assertFalse(state.demo)
        assertTrue(state.companion.confirmed)
    }

    @Test
    fun selectedStagesReturnToPickerAndReview() {
        val state = state()
        var finished = false
        showWizard(state, SetupEntry.SELECTED_STEPS) { finished = it }
        listOf("Welcome", "Sources", "Read permissions", "Verify").forEach(::click)
        click("Next")
        visible("Personalize")
        click("Back")
        visible("Choose setup steps")
        click("Next")
        click("Next")
        visible("Review")
        click("Finish")
        assertTrue(finished)
    }

    @Test
    fun requestUsesActualMissingGrantsAndSupportsRetry() {
        val state = state()
        val supported = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
        val steps = M1HealthConnectProbe.metrics.first().permission
        var actual = ConnectStatus("Available", "Synthetic", 1, setOf(steps), supported)
        var requests = emptySet<String>()
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SetupPermissions(state, inspect = { actual }, requestOverride = { requests = it })
                }
            }
        }
        click("Grant missing selected")
        assertEquals(4, requests.size)
        assertFalse(steps in requests)
        click("All supported")
        click("Grant missing selected")
        assertEquals(supported - steps, requests)
        click("Steps")
        click("Recommended")
        click("Grant missing selected")
        assertEquals(4, requests.size)
        click("All supported")
        actual = actual.copy(grantedCount = supported.size, granted = supported)
        requests = emptySet()
        click("Grant missing selected")
        assertTrue(requests.isEmpty())
        visible("No missing supported selection. Check access or select categories.")
        click("Recheck permissions")
        visible("Access unknown · recheck")
    }

    @Test
    fun requestFailureIsVisibleAndRetryDoesNotGrantAutomatically() {
        val state = state()
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SetupPermissions(state, inspect = { error("synthetic error") }, requestOverride = {})
                }
            }
        }
        click("Grant missing selected")
        visible("Permission screen unavailable. Recheck and retry.")
        click("Grant missing selected")
        visible("Permission screen unavailable. Recheck and retry.")
    }

    @Test
    fun checkPermissionsAndSourcesUseSameEntry() {
        val state = state()
        var entry: SetupEntry? = null
        state.setupAction = { entry = it }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsPage(state, DeviceProfile.Q25)
                    SourcesPage(state)
                }
            }
        }
        click("Check permissions")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, entry)
        click("Run complete setup")
        assertEquals(SetupEntry.REVIEW_ALL, entry)
        click("Choose setup steps")
        assertEquals(SetupEntry.SELECTED_STEPS, entry)
        click("Manage permissions")
        assertEquals(SetupEntry.PERMISSIONS_ONLY, entry)
    }

    @Test
    fun firstRunSkipsWithoutPermissionRequest() {
        val state = state()
        var finished = false
        showWizard(state, SetupEntry.FIRST_RUN) { finished = it }
        click("Back")
        visible("Welcome")
        repeat(5) { click("Skip") }
        visible("Review")
        click("Finish")
        assertTrue(finished)
        assertTrue(state.demo)
    }

    @Test
    fun cancelDoesNotWriteCompanionOrAppearance() {
        val state = state()
        var closed: Boolean? = null
        showWizard(state, SetupEntry.REVIEW_ALL) { closed = it }
        click("Next")
        click("Companion setup")
        click("Back")
        click("Back")
        assertEquals(false, closed)
        assertFalse(state.companion.confirmed)
        assertEquals("system", state.appearance)
    }

    @Test
    fun verifyUsesExplicitLocalMetadataScan() {
        val state = state()
        val permission = M1HealthConnectProbe.metrics.first().permission
        state.connect = ConnectStatus("Available", "Synthetic", 1, setOf(permission), setOf(permission))
        val result =
            HealthMetricCoverage(
                type = "Steps",
                permission = permission,
                state = CoverageState.RECORDS_FOUND,
                recordCount = 2,
                lastObservedUtc = "2026-01-01T00:00:00Z",
                originPackages = listOf("com.example.writer"),
            )
        val discovery = DiscoveryState { _, _ -> listOf(result) }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SetupVerify(state, discovery)
                }
            }
        }
        assertTrue(discovery.results.isEmpty())
        click("Scan allowed records")
        visible("Writer apps: com.example.writer")
        visible("Last observed (UTC): 2026-01-01T00:00:00Z")
    }

    @Test
    fun androidSettingsActionIsExplicit() {
        val state = state()
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SetupPermissions(state, requestOverride = {})
                }
            }
        }
        click("Manage Android permissions")
        val intent: Intent = shadowOf(compose.activity).nextStartedActivity
        assertEquals(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS, intent.action)
    }

    private fun showWizard(
        state: SableAppState,
        entry: SetupEntry,
        close: (Boolean) -> Unit,
    ) {
        compose.setContent {
            MaterialTheme { SetupWizard(state, DeviceProfile.Q25, entry, close) }
        }
    }

    private fun click(text: String) {
        val node = compose.onNodeWithText(text)
        if (text !in setOf("Next", "Back", "Skip", "Finish")) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }
}
