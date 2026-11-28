package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandSettingsTest {
    @Test fun `limites sao aplicados`() {
        val s = IslandSettings(tempSeconds = 99, offsetDp = -4).sanitized()
        assertEquals(IslandSettings.TEMP_MAX, s.tempSeconds)
        assertEquals(IslandSettings.OFFSET_MIN, s.offsetDp)
        assertEquals(2_000L, IslandSettings(tempSeconds = 0).tempMs)
    }

    @Test fun `ilha nasce desligada`() {
        assertEquals(false, IslandSettings().enabled)
    }

    @Test fun `codec dos apps faz ida e volta e ignora lixo`() {
        val apps = listOf(
            IslandAppConfig("com.whatsapp", true, IslandMode.Persistent, false),
            IslandAppConfig("org.telegram.messenger", false, IslandMode.Temporary, true)
        )
        val decoded = IslandAppsCodec.decode(IslandAppsCodec.encode(apps) + "\nlixo\n|true|Temporary|true\na|b")
        assertEquals(apps.associateBy { it.packageName }, decoded)
        assertTrue(IslandAppsCodec.decode(null).isEmpty())
    }

    @Test fun `nomes desconhecidos voltam ao padrao`() {
        assertEquals(IslandWidth.Medium, IslandWidth.fromName("x"))
        assertEquals(IslandMode.Temporary, IslandMode.fromName(null))
        assertEquals(IslandTheme.HyperOS, IslandTheme.fromName("?"))
    }
}
