package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLifecycleCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun actualActivityPausesResumesAndCompletesFirstRun() {
        val preferences = compose.activity.getSharedPreferences("sable_health_m0", Context.MODE_PRIVATE)
        assertFalse(preferences.getBoolean("onboarded", false))
        compose.onNodeWithText("Welcome").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithText("Sources").assertIsDisplayed()
        repeat(4) { compose.onNodeWithText("Next").performClick() }
        compose.onNodeWithText("Finish").performClick()
        compose.waitForIdle()
        assertTrue(preferences.getBoolean("onboarded", false))
        compose.onNode(hasText("Dashboard") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithText("Finish").assertDoesNotExist()
    }
}
