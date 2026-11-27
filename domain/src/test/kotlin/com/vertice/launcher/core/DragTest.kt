package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragTest {
    private val grid = GridConfig(4, 5)
    private fun dock(vararg k: String) = k.mapIndexed { i, key -> LayoutItem(key, Container.Dock, 0, i, 0) }
    private fun keys(l: List<LayoutItem>) = l.filter { it.container == Container.Dock }.sortedBy { it.x }.map { it.key }

    @Test fun `celula sob o ponto e nula fora da area`() {
        assertEquals(DragGeometry.Cell(0, 0), DragGeometry.cellAt(1f, 1f, 400f, 500f, grid))
        assertEquals(DragGeometry.Cell(3, 4), DragGeometry.cellAt(399f, 499f, 400f, 500f, grid))
        assertEquals(DragGeometry.Cell(2, 1), DragGeometry.cellAt(250f, 150f, 400f, 500f, grid))
        assertNull(DragGeometry.cellAt(-1f, 10f, 400f, 500f, grid))
        assertNull(DragGeometry.cellAt(400f, 10f, 400f, 500f, grid))
        assertNull(DragGeometry.cellAt(10f, 10f, 0f, 500f, grid))
    }

    @Test fun `miolo da celula e a metade central`() {
        // grade 4x5 em 400x500: células de 100x100
        assertEquals(true, DragGeometry.inCenter(150f, 150f, 400f, 500f, grid))
        assertEquals(false, DragGeometry.inCenter(110f, 150f, 400f, 500f, grid))
        assertEquals(false, DragGeometry.inCenter(150f, 190f, 400f, 500f, grid))
        assertEquals(false, DragGeometry.inCenter(-5f, 150f, 400f, 500f, grid))
    }

    @Test fun `indice do dock conta os centros a esquerda`() {
        val c = listOf(50f, 150f, 250f)
        assertEquals(0, DragGeometry.dockIndex(c, 10f))
        assertEquals(2, DragGeometry.dockIndex(c, 200f))
        assertEquals(3, DragGeometry.dockIndex(c, 999f))
        assertEquals(0, DragGeometry.dockIndex(emptyList(), 5f))
    }

    @Test fun `borda dispara virada de pagina`() {
        assertEquals(-1, DragGeometry.edgeAt(5f, 400f, 40f))
        assertEquals(1, DragGeometry.edgeAt(390f, 400f, 40f))
        assertEquals(0, DragGeometry.edgeAt(200f, 400f, 40f))
    }

    @Test fun `app da Home entra no dock na posicao pedida e sai da Home`() {
        val items = dock("a", "b") + LayoutItem("h", Container.Home, 0, 1, 1)
        val r = LayoutOps.moveToDock(items, "h", 1)
        assertEquals(listOf("a", "h", "b"), keys(r))
        assertEquals(0, r.count { it.container == Container.Home })
    }

    @Test fun `reordenar dentro do dock mantem o tamanho`() {
        val r = LayoutOps.moveToDock(dock("a", "b", "c"), "a", 2)
        assertEquals(listOf("b", "c", "a"), keys(r))
        assertEquals(listOf(0, 1, 2), r.sortedBy { it.x }.map { it.x })
    }

    @Test fun `dock cheio recusa app de fora mas deixa reordenar`() {
        val full = dock("a", "b", "c", "d", "e") + LayoutItem("h", Container.Home, 0, 0, 0)
        assertEquals(full, LayoutOps.moveToDock(full, "h", 0))
        assertEquals(listOf("e", "a", "b", "c", "d"), keys(LayoutOps.moveToDock(full, "e", 0)))
    }

    @Test fun `indice fora da faixa e corrigido`() {
        assertEquals(listOf("a", "x"), keys(LayoutOps.moveToDock(dock("a"), "x", 99)))
        assertEquals(listOf("x", "a"), keys(LayoutOps.moveToDock(dock("a"), "x", -5)))
    }
}
