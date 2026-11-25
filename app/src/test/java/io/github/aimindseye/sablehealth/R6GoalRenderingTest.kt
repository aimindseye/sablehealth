package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class R6GoalRenderingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun goalSelectionPersistsAndCanBeTurnedOff() {
        val store = compose.activity.getSharedPreferences("r6-render-goal", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val state = SableAppState(store)
        compose.setContent { MaterialTheme { R6GoalSetting(state) } }
        compose.onNodeWithText("No goal").assertIsDisplayed().performClick()
        compose.onNodeWithText(formatNumber(8000.0)).performClick()
        compose.runOnIdle {
            check(state.r6.stepGoal == 8000)
            check(R6Preferences(store).stepGoal == 8000)
        }
        compose.onNodeWithText(formatNumber(8000.0)).performClick()
        compose.onNodeWithText("No goal").performClick()
        compose.runOnIdle {
            check(state.r6.stepGoal == 0)
            check(R6Preferences(store).stepGoal == 0)
        }
    }

    @Test
    fun ringReflectsZeroPartialAndExceededGoals() {
        val primary = Color(0xFF1466B9)
        val track = Color(0xFF656D75)
        val scheme = lightColorScheme(primary = primary, outlineVariant = track)
        val count = mutableStateOf(0.0)
        compose.setContent {
            MaterialTheme(colorScheme = scheme) {
                Column { R6StepRing(count.value, 8000) }
            }
        }
        compose.onNodeWithText("0% · ${formatNumber(8000.0)} goal").assertIsDisplayed()
        val zero = ringInk(primary.toArgb())
        compose.runOnIdle { count.value = 4000.0 }
        compose.onNodeWithText("50% · ${formatNumber(8000.0)} goal").assertIsDisplayed()
        val half = ringInk(primary.toArgb())
        compose.runOnIdle { count.value = 8000.0 }
        val full = ringInk(primary.toArgb())
        check(zero < half && half < full)
        compose.runOnIdle { count.value = 10000.0 }
        compose.onNodeWithText("125% · ${formatNumber(8000.0)} goal").assertIsDisplayed()
        check(ringInk(primary.toArgb()) == full) { "Ring should cap visually while showing true progress" }
    }

    private fun ringInk(color: Int): Int {
        val node = compose.onNodeWithContentDescription("Step goal progress").assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle { renderR6Bitmap(compose.activity, bounds).countInk(color) }
    }
}
