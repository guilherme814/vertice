package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconShapeTest {
    @Test fun `original usa a mascara do sistema, as demais exigem ícone sem mascara`() {
        assertFalse(IconShape.Original.needsFullBleed)
        assertTrue(IconShape.entries.filter { it != IconShape.Original }.all { it.needsFullBleed })
    }

    @Test fun `arredondamento fica entre 0 e 50 por cento e circulo e o maior`() {
        val p = IconShape.entries.mapNotNull { it.percent }
        assertTrue(p.all { it in 1..50 })
        assertEquals(50, IconShape.Circle.percent)
    }

    @Test fun `nome desconhecido volta ao original`() {
        assertEquals(IconShape.Original, IconShape.fromName("x"))
        assertEquals(IconShape.Squircle, IconShape.fromName("Squircle"))
    }
}
