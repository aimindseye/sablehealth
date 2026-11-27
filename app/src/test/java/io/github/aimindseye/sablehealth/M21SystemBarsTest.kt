package io.github.aimindseye.sablehealth

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class M21SystemBarsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lightAndDarkAppearanceUpdateBothSystemBars() {
        val appearance = mutableStateOf("light")
        compose.setContent { SableHealthTheme(appearance.value) { Text("Sable Health") } }
        assertBars(true)
        compose.runOnIdle { appearance.value = "dark" }
        assertBars(false)
        compose.runOnIdle { appearance.value = "light" }
        assertBars(true)
        compose.runOnIdle { appearance.value = "system" }
        val dark = compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        assertBars(dark != Configuration.UI_MODE_NIGHT_YES)
    }

    private fun assertBars(light: Boolean) {
        compose.runOnIdle {
            val window = compose.activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            check(controller.isAppearanceLightStatusBars == light)
            check(controller.isAppearanceLightNavigationBars == light)
        }
    }
}
