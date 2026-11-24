package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesUiContractTest {
    @Test
    fun userDeclarationIsNotPresentedAsVerifiedFitbitConnection() {
        val confirmed = SourcesUiContract.companionStatus(true)
        assertTrue(confirmed.contains("User-confirmed"))
        assertFalse(confirmed.contains("Connected to Sable Health"))
        assertTrue(SourcesUiContract.EXPORT_STATUS.contains("unverified"))
    }

    @Test
    fun unknownSetupAndMissingImportsAreNeverPresentedAsMeasuredZeros() {
        assertTrue(SourcesUiContract.companionStatus(false).contains("not confirmed"))
        assertEquals("None · M0", SourcesUiContract.IMPORTED_M0)
        assertFalse(SourcesUiContract.IMPORTED_M0.contains("0 records"))
    }
}
