package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    private fun home(key: String, page: Int, x: Int, y: Int) = LayoutItem(key, Container.Home, page, x, y)
    private fun dock(key: String, i: Int) = LayoutItem(key, Container.Dock, 0, i, 0)
    private fun keys(vararg k: String) = k.toSet()
    private val perPage = GridSpec.COLUMNS * GridSpec.ROWS

    @Test fun `addToHome usa a primeira celula livre em ordem de linha`() {
        var l = emptyList<LayoutItem>()
        l = LayoutOps.addToHome(l, "a")
        l = LayoutOps.addToHome(l, "b")
        assertEquals(home("a", 0, 0, 0), l.first { it.key == "a" })
        assertEquals(home("b", 0, 1, 0), l.first { it.key == "b" })
    }

    @Test fun `addToHome abre nova pagina quando a primeira enche`() {
        var l = emptyList<LayoutItem>()
        for (i in 0 until perPage + 1) l = LayoutOps.addToHome(l, "app$i")
        assertEquals(home("app$perPage", 1, 0, 0), l.first { it.key == "app$perPage" })
    }

    @Test fun `remover um app nao desloca os outros e o buraco e reaproveitado`() {
        var l = listOf(home("a", 0, 0, 0), home("b", 0, 1, 0), home("c", 0, 2, 0))
        l = LayoutOps.removeFromHome(l, "b")
        assertEquals(home("c", 0, 2, 0), l.first { it.key == "c" })
        l = LayoutOps.addToHome(l, "d")
        assertEquals(home("d", 0, 1, 0), l.first { it.key == "d" })
    }

    @Test fun `addToHome tira o app do dock`() {
        val l = LayoutOps.addToHome(listOf(dock("a", 0)), "a")
        assertEquals(1, l.size)
        assertEquals(Container.Home, l.single().container)
    }

    @Test fun `moveHome para celula vazia`() {
        val l = LayoutOps.moveHome(listOf(home("a", 0, 0, 0)), "a", 1, 3, 4)
        assertEquals(listOf(home("a", 1, 3, 4)), l)
    }

    @Test fun `moveHome para celula ocupada troca os dois de lugar`() {
        val l = LayoutOps.moveHome(listOf(home("a", 0, 0, 0), home("b", 0, 2, 1)), "a", 0, 2, 1)
        assertEquals(home("a", 0, 2, 1), l.first { it.key == "a" })
        assertEquals(home("b", 0, 0, 0), l.first { it.key == "b" })
    }

    @Test fun `moveHome vindo do dock realoca o ocupante para a primeira celula livre`() {
        val l = LayoutOps.moveHome(listOf(dock("a", 0), home("b", 0, 0, 0)), "a", 0, 0, 0)
        assertEquals(home("a", 0, 0, 0), l.first { it.key == "a" })
        assertEquals(home("b", 0, 1, 0), l.first { it.key == "b" })
    }

    @Test fun `moveHome fora da grade nao faz nada`() {
        val l = listOf(home("a", 0, 0, 0))
        assertEquals(l, LayoutOps.moveHome(l, "a", 0, GridSpec.COLUMNS, 0))
        assertEquals(l, LayoutOps.moveHome(l, "a", 0, 0, GridSpec.ROWS))
        assertEquals(l, LayoutOps.moveHome(l, "a", -1, 0, 0))
    }

    @Test fun `addToDock respeita o limite`() {
        var l = emptyList<LayoutItem>()
        for (i in 0 until GridSpec.DOCK_SLOTS) l = LayoutOps.addToDock(l, "d$i")
        val cheio = LayoutOps.addToDock(l, "extra")
        assertEquals(l, cheio)
        assertNull(cheio.firstOrNull { it.key == "extra" })
    }

    @Test fun `addToDock move o app da Home para o dock`() {
        val l = LayoutOps.addToDock(listOf(home("a", 0, 0, 0)), "a")
        assertEquals(listOf(dock("a", 0)), l)
    }

    @Test fun `sanitize remove apps inexistentes e compacta o dock`() {
        val l = LayoutOps.sanitize(listOf(dock("a", 0), dock("morto", 1), dock("c", 2)), keys("a", "c"))
        assertEquals(listOf(dock("a", 0), dock("c", 1)), l)
    }

    @Test fun `sanitize remove paginas vazias`() {
        val l = LayoutOps.sanitize(listOf(home("a", 0, 0, 0), home("b", 3, 1, 1)), keys("a", "b"))
        assertEquals(home("b", 1, 1, 1), l.first { it.key == "b" })
    }

    @Test fun `sanitize resolve conflito de celula sem perder app`() {
        val l = LayoutOps.sanitize(listOf(home("a", 0, 0, 0), home("b", 0, 0, 0)), keys("a", "b"))
        assertEquals(2, l.size)
        assertEquals(2, l.map { Triple(it.page, it.x, it.y) }.toSet().size)
    }

    @Test fun `sanitize manda o excesso do dock para a Home`() {
        val itens = (0..GridSpec.DOCK_SLOTS).map { dock("d$it", it) }
        val l = LayoutOps.sanitize(itens, itens.map { it.key }.toSet())
        assertEquals(GridSpec.DOCK_SLOTS, l.count { it.container == Container.Dock })
        assertEquals(1, l.count { it.container == Container.Home })
    }

    @Test fun `sanitize recoloca item fora da grade`() {
        val l = LayoutOps.sanitize(listOf(home("a", 0, 99, 99)), keys("a"))
        assertEquals(home("a", 0, 0, 0), l.single())
    }

    @Test fun `sanitize remove chave repetida`() {
        val l = LayoutOps.sanitize(listOf(home("a", 0, 0, 0), dock("a", 0)), keys("a"))
        assertEquals(1, l.size)
    }

    @Test fun `sanitize e idempotente`() {
        val sujo = listOf(
            home("a", 2, 0, 0), home("b", 2, 0, 0), home("c", 5, 9, 9),
            dock("d", 4), dock("e", 4), dock("f", 7), dock("g", 0), dock("h", 1), dock("i", 2), dock("j", 3)
        )
        val present = sujo.map { it.key }.toSet()
        val uma = LayoutOps.sanitize(sujo, present)
        assertEquals(uma, LayoutOps.sanitize(uma, present))
    }

    @Test fun `codec faz ida e volta com caracteres especiais`() {
        val item = home("com.exemplo/.Main\$Alias|x;y#7 ç", 2, 3, 4)
        assertEquals(item, LayoutCodec.decode(LayoutCodec.encode(item)))
        val dockItem = dock("a/b#12", 3)
        assertEquals(dockItem, LayoutCodec.decode(LayoutCodec.encode(dockItem)))
    }

    @Test fun `codec ignora linhas invalidas`() {
        assertNull(LayoutCodec.decode("lixo"))
        assertNull(LayoutCodec.decode("a|X|0|0|0"))
        assertNull(LayoutCodec.decode("a|H|zero|0|0"))
        val ok = LayoutCodec.encode(home("a", 0, 1, 1))
        assertTrue(LayoutCodec.decodeAll(setOf(ok, "lixo")).size == 1)
    }
}
