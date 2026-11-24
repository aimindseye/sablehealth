package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupPlanTest {
    @Test
    fun firstRunAndReviewUseAllStages() {
        listOf(SetupEntry.FIRST_RUN, SetupEntry.REVIEW_ALL).forEach {
            assertEquals(SetupStage.entries.toList(), SetupPlan.stages(it, emptySet()))
        }
    }

    @Test
    fun permissionEntryHasNoPersonalization() {
        assertEquals(
            listOf(SetupStage.PERMISSIONS, SetupStage.VERIFY, SetupStage.REVIEW),
            SetupPlan.stages(SetupEntry.PERMISSIONS_ONLY, emptySet()),
        )
    }

    @Test
    fun selectedStepsUseCanonicalOrderAndAlwaysReview() {
        val stages = SetupPlan.stages(SetupEntry.SELECTED_STEPS, setOf(SetupStage.VERIFY, SetupStage.SOURCES))
        assertEquals(listOf(SetupStage.SOURCES, SetupStage.VERIFY, SetupStage.REVIEW), stages)
        assertEquals(listOf(SetupStage.REVIEW), SetupPlan.stages(SetupEntry.SELECTED_STEPS, emptySet()))
    }

    @Test
    fun requestsOnlySupportedSelectedAndNotAlreadyGranted() {
        val selected = setOf("steps", "sleep", "unsupported")
        val supported = setOf("steps", "sleep", "heart")
        assertEquals(setOf("sleep"), SetupPlan.missing(selected, supported, setOf("steps", "heart")))
        assertEquals(setOf("steps", "sleep"), SetupPlan.missing(selected, supported, emptySet()))
        assertTrue(SetupPlan.missing(selected, emptySet(), emptySet()).isEmpty())
        assertTrue(SetupPlan.missing(selected, supported, supported).isEmpty())
        assertTrue(SetupPlan.missing(emptySet(), supported, emptySet()).isEmpty())
    }

    @Test
    fun navigationDoesNotPassEndpoints() {
        assertEquals(1, SetupPlan.next(0, 3))
        assertEquals(2, SetupPlan.next(2, 3))
        assertEquals(0, SetupPlan.previous(0))
        assertEquals(1, SetupPlan.previous(2))
    }
}
