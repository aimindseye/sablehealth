package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupRoutingCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val preferences
        get() = compose.activity.getSharedPreferences("sable_health_m0", Context.MODE_PRIVATE)

    @Test
    fun freshSetupFinishesAndReturnsToDashboard() {
        preferences.edit().clear().commit()
        compose.setContent { SableApp() }
        visible("Welcome")
        assertFalse(preferences.getBoolean("onboarded", false))
        repeat(5) { click("Next") }
        visible("Review")
        click("Finish")
        assertTrue(preferences.getBoolean("onboarded", false))
        visible("Dashboard")
    }

    @Test
    fun returningUserReentersFromSettingsAndSources() {
        preferences
            .edit()
            .clear()
            .putBoolean("onboarded", true)
            .putString("profile", "Q25")
            .commit()
        compose.setContent { SableApp() }
        compose.onNodeWithText("Welcome").assertDoesNotExist()
        click("Settings")
        click("Run complete setup", scroll = true)
        visible("Welcome")
        click("Back")
        visible("Customize experience")
        click("Choose setup steps", scroll = true)
        click("Skip")
        visible("Customize experience")
        click("Sources")
        click("Manage permissions", scroll = true)
        visible("Read permissions")
        click("Next")
        click("Next")
        click("Finish")
        visible("Data connections")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        visible("Dashboard")
    }

    @Test
    fun resumeAndManualRecheckUseActualLatestAccess() {
        preferences
            .edit()
            .clear()
            .putBoolean("onboarded", true)
            .putString("profile", "Q25")
            .commit()
        val version = mutableStateOf(0)
        val supported = setOf("read.steps")
        var granted = supported
        compose.setContent {
            SableApp(version.value) {
                ConnectStatus("Available", "Synthetic", granted.size, granted, supported)
            }
        }
        click("Settings")
        click("Check permissions", scroll = true)
        visible("1/1 allowed · 0 not granted")
        compose.runOnIdle {
            granted = emptySet()
            version.value++
        }
        visible("0/1 allowed · 1 not granted")
        compose.runOnIdle { granted = supported }
        click("Recheck permissions", scroll = true)
        visible("1/1 allowed · 0 not granted")
    }

    @Test
    fun restorationRetainsWizardStageAndPendingAppearance() {
        preferences.edit().clear().commit()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { SableApp() }
        repeat(4) { click("Next") }
        click("Dark", scroll = true)
        restoration.emulateSavedInstanceStateRestore()
        visible("Personalize")
        click("Next")
        click("Finish")
        assertTrue(preferences.getString("appearance", "") == "dark")
        assertTrue(preferences.getBoolean("onboarded", false))
    }

    private fun click(
        text: String,
        scroll: Boolean = false,
    ) {
        val node = compose.onNodeWithText(text)
        if (scroll) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).assertIsDisplayed()
    }
}
