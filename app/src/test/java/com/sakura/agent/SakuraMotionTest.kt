package com.sakura.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic checks for the extra offline mascot motions. */
class SakuraMotionTest {
    @Test fun newMoodsAreStable() {
        assertEquals("enamorada", MascotExpressions.mood("Corazón"))
        assertEquals("dormida", MascotExpressions.mood("Dormir"))
        assertEquals("celebrando", MascotExpressions.mood("Celebrar"))
        assertEquals("girando", MascotExpressions.mood("Girar"))
    }

    @Test fun heartbeatIsBounded() {
        (0..20).forEach {
            val value = MascotExpressions.heartBeat(it / 20f)
            assertTrue(value in 0f..1f)
        }
    }

    @Test fun sleepingEyesOnlyInTheirWindow() {
        assertTrue(MascotExpressions.sleepingEyes(0.5f))
        assertFalse(MascotExpressions.sleepingEyes(0.9f))
    }

    @Test fun celebrateIsHappyButSleepIsNot() {
        assertTrue(MascotExpressions.happy("Celebrar"))
        assertFalse(MascotExpressions.happy("Dormir"))
    }

    @Test fun spinIsBounded() {
        val value = MascotExpressions.spin(0.25f)
        assertTrue(value in -1f..1f)
        assertEquals(0f, MascotExpressions.spin(0f), 0.0001f)
    }
}
