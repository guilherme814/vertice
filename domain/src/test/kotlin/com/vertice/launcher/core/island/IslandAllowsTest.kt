package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandAllowsTest {
    private val media = IslandEvent.Media("media", "p", "t", "a", true)
    private val charging = IslandEvent.Charging(percent = 50)
    private val bt = IslandEvent.Bluetooth("bt", "Fone")

    @Test fun `desligada nao permite nada`() {
        assertFalse(IslandSettings(enabled = false).allows(media))
    }

    @Test fun `cada tipo respeita seu interruptor`() {
        val s = IslandSettings(enabled = true, media = false, charging = true, bluetooth = false)
        assertFalse(s.allows(media))
        assertTrue(s.allows(charging))
        assertFalse(s.allows(bt))
        assertTrue(s.copy(bluetooth = true).allows(bt))
    }

    @Test fun `bluetooth nasce desligado`() {
        assertFalse(IslandSettings().bluetooth)
    }

    @Test fun `chamada leva os rotulos das acoes limitados a tres`() {
        val n = RawNotification("c", "p", category = RawNotification.CATEGORY_CALL, hasCallActions = true,
            actions = listOf("Recusar", "Atender", "Msg", "Extra"))
        val e = IslandClassifier.classify(n, IslandSettings(enabled = true), emptyMap()) as IslandEvent.Call
        assertEquals(listOf("Recusar", "Atender", "Msg"), e.actions)
    }
}
