package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private fun app(key: String, pkg: String = "com.$key", declared: Int = -1) = AppModel(key, pkg, key.uppercase(), 0L, declared)
    private fun dock(key: String, i: Int) = LayoutItem(key, Container.Dock, 0, i, 0)
    private fun home(key: String, p: Int, x: Int, y: Int) = LayoutItem(key, Container.Home, p, x, y)

    @Test fun `dock sai na ordem e ignora app desinstalado`() {
        val s = HomeState.from(
            listOf(app("a"), app("c")),
            Prefs(layout = listOf(dock("c", 1), dock("morto", 0), dock("a", 2)))
        )
        assertEquals(listOf("c", "a"), s.dock)
        assertEquals(Placement.Dock, s.placement["a"])
        assertTrue("morto" !in s.placement)
    }

    @Test fun `paginas contam pela maior pagina usada e no minimo uma`() {
        assertEquals(1, HomeState.from(listOf(app("a")), Prefs()).pageCount)
        val s = HomeState.from(listOf(app("a"), app("b")), Prefs(layout = listOf(home("a", 0, 0, 0), home("b", 4, 1, 1))))
        assertEquals(2, s.pageCount) // páginas vazias do meio são removidas
    }

    @Test fun `categoria manual vence a automatica`() {
        val a = app("a", "com.whatsapp")
        val auto = HomeState.from(listOf(a), Prefs())
        assertEquals(AppCategory.Social, auto.categories["a"])
        val manual = HomeState.from(listOf(a), Prefs(categoryOverrides = mapOf("a" to "Games")))
        assertEquals(AppCategory.Games, manual.categories["a"])
        assertEquals(setOf("a"), manual.manualCategories)
    }

    @Test fun `categoria automatica pode ser memorizada pelo chamador`() {
        var calls = 0
        val apps = listOf(app("a"), app("b"))
        HomeState.from(apps, Prefs()) { calls++; AppCategory.Tools }
        assertEquals(2, calls)
    }

    @Test fun `grade traz so itens da Home`() {
        val s = HomeState.from(listOf(app("a"), app("b")), Prefs(layout = listOf(home("a", 0, 0, 0), dock("b", 0))))
        assertEquals(listOf("a"), s.grid.map { it.key })
    }
}
