package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceProfileTest {
    @Test fun `pouca RAM comeca no Ultra Lite`() {
        assertEquals(PerformanceProfile.UltraLite, PerformanceProfile.recommended(lowRam = true))
        assertEquals(PerformanceProfile.Balanced, PerformanceProfile.recommended(lowRam = false))
    }

    @Test fun `perfis escalam em custo`() {
        val p = PerformanceProfile.entries
        assertTrue(p.zipWithNext().all { (a, b) -> a.iconCacheMb < b.iconCacheMb && a.iconMaxPx < b.iconMaxPx })
        assertFalse(PerformanceProfile.UltraLite.animations)
    }

    @Test fun `fromName`() {
        assertEquals(PerformanceProfile.Premium, PerformanceProfile.fromName("Premium"))
        assertNull(PerformanceProfile.fromName("x"))
    }
}
