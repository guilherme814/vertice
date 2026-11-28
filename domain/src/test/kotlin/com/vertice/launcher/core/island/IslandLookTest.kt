package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandLookTest {
    @Test fun `fundo desconhecido volta ao preto e o vidro e translucido`() {
        assertEquals(IslandBackground.Black, IslandBackground.fromName("?"))
        assertTrue(IslandBackground.Glass.alpha < 1f && IslandBackground.Black.alpha == 1f)
    }

    @Test fun `duracao da abertura e limitada`() {
        assertEquals(IslandSettings.EXPAND_MAX, IslandSettings(expandSeconds = 99).sanitized().expandSeconds)
        assertEquals(2_000L, IslandSettings(expandSeconds = 0).expandMs)
        assertEquals(4_000L, IslandSettings().expandMs)
    }

    @Test fun `sem abertura automatica a mensagem so mostra a capsula`() {
        val a = IslandArbiter().apply { autoExpandEnabled = false }
        val n = IslandEvent.Notification("n", "p", "t", "x", false, false)
        assertTrue(a.onEvent(n, 0, 5_000) is IslandState.Compact)
        assertEquals(5_000L, a.nextWakeAt())
    }

    @Test fun `duracao configurada vale para a abertura`() {
        val a = IslandArbiter().apply { autoExpandMs = 2_000 }
        val n = IslandEvent.Notification("n", "p", "t", "x", false, false)
        assertTrue(a.onEvent(n, 0, 5_000) is IslandState.Expanded)
        assertEquals(2_000L, a.nextWakeAt())
        assertTrue(a.tick(2_000) is IslandState.Compact)
    }
}
