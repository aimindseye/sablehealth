package io.github.aimindseye.sablehealth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class M1RationaleCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<M1PermissionRationaleActivity>()

    @Test
    fun rationaleExplainsLocalDataAndCloses() {
        compose.onNodeWithText("Sable Health · Health Connect privacy").assertExists()
        compose.onNodeWithText("Health Connect permissions can be revoked", substring = true).assertExists()
        val activity = compose.activity
        compose.onNodeWithText("Close").performScrollTo().performClick()
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }
}
