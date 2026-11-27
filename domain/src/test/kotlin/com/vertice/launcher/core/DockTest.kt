package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DockTest {
    @Test fun `opacidade fica entre o minimo e 100 por cento`() {
        assertEquals(DockOpacity.MIN, DockOpacity.coerce(-10))
        assertEquals(DockOpacity.MAX, DockOpacity.coerce(500))
        assertEquals(60, DockOpacity.coerce(60))
    }

    @Test fun `opacidade escala o vidro do nivel e nunca passa de 1`() {
        assertEquals(0.3f, DockOpacity.alpha(0.6f, 50), 0.0001f)
        assertEquals(0.6f, DockOpacity.alpha(0.6f, 100), 0.0001f)
        assertEquals(1f, DockOpacity.alpha(5f, 100), 0.0001f)
    }

    @Test fun `estilo desconhecido volta ao vidro`() {
        assertEquals(DockStyle.Glass, DockStyle.fromName("x"))
        assertEquals(DockStyle.Flat, DockStyle.fromName("Flat"))
    }

    @Test fun `backup ignora opacidade invalida`() {
        val text = BackupCodec.encode(Prefs()).replace("dockOpacity=100", "dockOpacity=abc")
        val r = BackupCodec.decode(text) as BackupCodec.Result.Ok
        assertEquals(DockOpacity.DEFAULT, r.prefs.dockOpacity)
    }
}
