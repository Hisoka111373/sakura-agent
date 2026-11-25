package com.sakura.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutopilotPolicyTest {
    @Test fun onlyLowRiskCanBeAutomated() {
        assertFalse(AutopilotPolicy.autoApprove(true, "github_write"))
        assertFalse(AutopilotPolicy.autoApprove(true, "web_search"))
        assertTrue(AutopilotPolicy.autoApprove(true, "github_read"))
    }

    @Test fun disabledAutopilotNeverApproves() {
        assertFalse(AutopilotPolicy.autoApprove(false, "file_read"))
    }

    @Test fun unknownActionsAreHighRisk() {
        assertEquals(AutopilotPolicy.Risk.HIGH, AutopilotPolicy.risk("borrar_todo"))
        assertFalse(AutopilotPolicy.autoApprove(true, "borrar_todo"))
    }

    @Test fun descriptionsAreInformative() {
        assertTrue(AutopilotPolicy.description("github_write").contains("confirmación"))
        assertTrue(AutopilotPolicy.description("github_read").contains("automaticizar"))
    }
}
