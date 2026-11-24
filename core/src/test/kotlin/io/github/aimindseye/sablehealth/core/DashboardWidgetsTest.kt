package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardWidgetsTest {
    @Test
    fun defaultSelectionHasFourUniqueWidgets() {
        assertTrue(DashboardWidgetSelection.isValid(DashboardWidgetSelection.defaults))
        assertEquals(DashboardWidget.STEPS, DashboardWidgetSelection.defaults.first())
        assertEquals(DashboardWidget.HRV, DashboardWidgetSelection.defaults.last())
    }

    @Test
    fun corruptedSettingsRestoreExactlyFour() {
        assertEquals(DashboardWidgetSelection.defaults, DashboardWidgetSelection.restore(null))
        val actual = DashboardWidgetSelection.restore("STEPS,STEPS,WRONG,HRV")
        assertTrue(DashboardWidgetSelection.isValid(actual))
        assertEquals(DashboardWidget.STEPS, actual.first())
        assertEquals(DashboardWidget.HRV, actual[1])
    }

    @Test
    fun selectionCannotExceedFourOrDuplicate() {
        val firstFour = DashboardWidgetSelection.defaults
        val unchanged = DashboardWidgetSelection.toggle(firstFour, DashboardWidget.RESPIRATORY_RATE)
        assertEquals(firstFour, unchanged)
        val three = DashboardWidgetSelection.toggle(firstFour, DashboardWidget.STEPS)
        assertFalse(DashboardWidgetSelection.isValid(three))
        val replacement = DashboardWidgetSelection.toggle(three, DashboardWidget.RESPIRATORY_RATE)
        assertTrue(DashboardWidgetSelection.isValid(replacement))
    }

    @Test
    fun orderingIsStableAndBoundsSafe() {
        val originals = DashboardWidgetSelection.defaults
        val swapped = DashboardWidgetSelection.move(originals, 2, -1)
        assertEquals(DashboardWidget.SLEEP_STAGES, swapped[1])
        assertEquals(originals, DashboardWidgetSelection.move(originals, 0, -1))
        assertEquals(originals, DashboardWidgetSelection.restore(DashboardWidgetSelection.serialize(originals)))
    }

    @Test
    fun unsupportedGraphStyleFallsBackToText() {
        assertEquals(WidgetStyle.TEXT, DashboardWidgetSelection.styleFor(DashboardWidget.HRV, "GRAPH"))
        assertEquals(WidgetStyle.GRAPH, DashboardWidgetSelection.styleFor(DashboardWidget.STEPS, null))
        assertEquals(WidgetStyle.TEXT, DashboardWidgetSelection.styleFor(DashboardWidget.SLEEP_STAGES, "TEXT"))
    }

    @Test
    fun unverifiedReadinessHasNoInventedSample() {
        assertEquals(null, DashboardDemoSamples.forWidget(DashboardWidget.READINESS))
        assertEquals(null, DashboardDemoSamples.forWidget(DashboardWidget.SPO2))
        assertTrue(DashboardWidget.entries.size >= 20)
    }
}
