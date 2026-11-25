package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeConfigTest {
    @Test fun `nome desconhecido cai no padrao`() {
        assertEquals(Accent.Mint, Accent.fromName("nada"))
        assertEquals(Accent.Mint, Accent.fromName(null))
        assertEquals(GlassLevel.Medium, GlassLevel.fromName("nada"))
    }

    @Test fun `vidro cresce e a pilula nunca passa de 0,95`() {
        val a = GlassLevel.entries.map { it.panelAlpha }
        assertEquals(a.sorted(), a)
        assertTrue(GlassLevel.entries.all { it.pillAlpha in it.panelAlpha..0.95f })
    }

    @Test fun `cores opacas`() {
        assertTrue(Accent.entries.all { (it.argb ushr 24) == 0xFFL })
        assertEquals(Accent.entries.size, Accent.entries.map { it.label }.toSet().size)
    }
}
