package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceProfileTest {
    private fun detect(m: String, n: String, d: String = "", kb: Boolean = true, nav: Boolean = false) =
        DeviceProfile.detect(m, n, d, kb, nav)

    @Test fun identifiesAllThreeProfiles() {
        assertEquals(DeviceProfile.Q25, detect("Zinwa", "Q25", nav = true))
        assertEquals(DeviceProfile.TITAN2, detect("Unihertz", "TITAN_2"))
        assertEquals(DeviceProfile.P26, detect("Zinwa", "P26"))
        assertEquals(DeviceProfile.P26, detect("Zinwa", "Passport 2026"))
    }

    @Test fun rejectsP26SubstringFalsePositive() {
        assertEquals(DeviceProfile.TOUCH, detect("Samsung", "SM-P260", kb = false))
    }

    @Test fun genericKeyboardFallback() {
        assertEquals(DeviceProfile.KEYBOARD, detect("Other", "Model", kb = true))
        assertEquals(DeviceProfile.TOUCH, detect("Other", "Phone", kb = false))
    }

    @Test fun mappingIsProfileAware() {
        assertEquals(UiAction.UP, KeyboardMapping.action(DeviceProfile.TITAN2, Press(37)))
        assertEquals(UiAction.UP, KeyboardMapping.action(DeviceProfile.P26, Press(37)))
        assertNull(KeyboardMapping.action(DeviceProfile.Q25, Press(37)))
        assertEquals(UiAction.UP, KeyboardMapping.action(DeviceProfile.Q25, Press(19)))
        assertEquals(UiAction.CONFIRM, KeyboardMapping.action(DeviceProfile.Q25, Press(23)))
    }

    @Test fun textAndModifiersAreNeverHijacked() {
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(37, editingText = true)))
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(37, alt = true)))
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(37, ctrl = true)))
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(37, meta = true)))
    }

    @Test fun nonNavigationRepeatsAreBlocked() {
        assertEquals(UiAction.DOWN, KeyboardMapping.action(DeviceProfile.TITAN2, Press(39, repeatCount = 2)))
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(66, repeatCount = 2)))
        assertNull(KeyboardMapping.action(DeviceProfile.TITAN2, Press(46, repeatCount = 2)))
    }

    @Test fun demoHasExplicitMissingState() {
        assertEquals("—", DemoHealth.vitals.last().value)
        assertEquals(4, DemoHealth.today.size)
    }
}
