package io.github.aimindseye.sablehealth

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class R7VisualPolicyTest {
    @Test
    fun timestampsUseMeasuredWidthAndDoNotOverlapOrClip() {
        assertEquals(0f, r7LabelLeft(3f, 40f, 200f, 0f))
        assertNull(r7LabelLeft(20f, 40f, 200f, 44f))
        assertEquals(60f, r7LabelLeft(80f, 40f, 200f, 44f))
        assertEquals(160f, r7LabelLeft(199f, 40f, 200f, 100f))
        assertNull(r7LabelLeft(200f, 40f, 200f, 170f))
        assertNull(r7LabelLeft(0f, 201f, 200f, 0f))
        assertNull(r7LabelLeft(0f, 0f, 200f, 0f))
    }

    @Test
    fun sessionDurationAndFocusAreNotInvented() {
        assertEquals("6h 31m", r7HoursMinutes(23460L))
        assertEquals("0h 0m", r7HoursMinutes(-1L))
        val shape = RoundedCornerShape(8.dp)
        assertEquals(Modifier, r7FocusOutline(false, Color.Blue, shape))
        assertNotEquals(Modifier, r7FocusOutline(true, Color.Blue, shape))
    }
}
