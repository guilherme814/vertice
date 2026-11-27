package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    private fun run(vararg e: NavEvent, from: NavState = NavState()) = e.fold(from) { s, ev -> Nav.reduce(s, ev) }

    @Test fun `abre e fecha a gaveta com Voltar`() {
        val open = run(NavEvent.OpenDrawer)
        assertEquals(Screen.Drawer, open.screen)
        assertTrue(Nav.handlesBack(open))
        assertEquals(NavState(), Nav.reduce(open, NavEvent.Back))
    }

    @Test fun `Voltar na Home pura nao e nosso`() = assertFalse(Nav.handlesBack(NavState()))

    @Test fun `botao Home fecha tudo e cancela o mover`() {
        assertEquals(NavState(), run(NavEvent.OpenSheet, NavEvent.HomePressed))
        assertEquals(NavState(), run(NavEvent.StartMove("a"), NavEvent.HomePressed))
    }

    @Test fun `nao abre gaveta nem folha por cima de outra tela`() {
        assertEquals(Screen.Drawer, run(NavEvent.OpenDrawer, NavEvent.OpenSheet).screen)
        assertEquals(Screen.Sheet, run(NavEvent.OpenSheet, NavEvent.OpenDrawer).screen)
    }

    @Test fun `durante o mover nao abre gaveta e Voltar so cancela o mover`() {
        val moving = run(NavEvent.StartMove("a"))
        assertEquals(moving, Nav.reduce(moving, NavEvent.OpenDrawer))
        assertEquals(moving, Nav.reduce(moving, NavEvent.OpenSheet))
        assertTrue(Nav.handlesBack(moving))
        assertEquals(NavState(), Nav.reduce(moving, NavEvent.Back))
    }

    @Test fun `mover so comeca na Home`() {
        val drawer = run(NavEvent.OpenDrawer)
        assertEquals(drawer, Nav.reduce(drawer, NavEvent.StartMove("a")))
    }

    @Test fun `abrir app fecha a gaveta e termina o mover`() {
        assertEquals(NavState(), run(NavEvent.OpenDrawer, NavEvent.AppLaunched))
        assertEquals(NavState(), run(NavEvent.StartMove("a"), NavEvent.AppLaunched))
    }

    @Test fun `EndMove limpa o app em movimento`() {
        assertEquals(NavState(), run(NavEvent.StartMove("a"), NavEvent.EndMove))
    }

    @Test fun `deslizar para baixo abre a gaveta com busca focada e Voltar limpa o foco`() {
        val s = run(NavEvent.OpenSearch)
        assertEquals(Screen.Drawer, s.screen)
        assertTrue(s.focusSearch)
        assertEquals(NavState(), Nav.reduce(s, NavEvent.Back))
        assertFalse(run(NavEvent.OpenDrawer).focusSearch)
    }

    @Test fun `busca nao abre durante o mover nem por cima de outra tela`() {
        val moving = run(NavEvent.StartMove("a"))
        assertEquals(moving, Nav.reduce(moving, NavEvent.OpenSearch))
        val sheet = run(NavEvent.OpenSheet)
        assertEquals(sheet, Nav.reduce(sheet, NavEvent.OpenSearch))
    }
}
