package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandArbiterTest {
    private val temp = 5_000L
    private fun note(k: String, text: String = "x") = IslandEvent.Notification(k, "p", "t", text, false, false)
    private fun media(playing: Boolean = true, title: String = "Faixa") = IslandEvent.Media("media:p", "p", title, "A", playing)
    private fun call(k: String = "call") = IslandEvent.Call(k, "p", "Mãe", false, null)

    @Test fun `comeca oculta`() {
        assertEquals(IslandState.Hidden, IslandArbiter().tick(0))
    }

    @Test fun `mensagem abre sozinha, recolhe e depois expira`() {
        val a = IslandArbiter()
        assertTrue(a.onEvent(note("n"), 0, temp) is IslandState.Expanded)
        assertEquals(IslandArbiter.AUTO_EXPAND_MS, a.nextWakeAt())
        assertTrue(a.tick(IslandArbiter.AUTO_EXPAND_MS - 1) is IslandState.Expanded)
        assertTrue(a.tick(IslandArbiter.AUTO_EXPAND_MS) is IslandState.Compact)
        assertEquals(IslandState.Hidden, a.tick(temp))
        assertNull(a.nextWakeAt())
    }

    @Test fun `nao notificacao nao abre sozinha`() {
        val a = IslandArbiter()
        assertTrue(a.onEvent(IslandEvent.Charging(percent = 40), 0, temp) is IslandState.Compact)
    }

    @Test fun `mensagem nova na mesma conversa atualiza no lugar e renova o tempo`() {
        val a = IslandArbiter()
        a.onEvent(note("n", "um"), 0, temp)
        val s = a.onEvent(note("n", "dois"), 3000, temp)
        assertTrue(s is IslandState.Expanded)
        assertEquals("dois", (s.currentEvent as IslandEvent.Notification).text)
        assertEquals(1, a.size())
        assertEquals(3000 + IslandArbiter.AUTO_EXPAND_MS, a.nextWakeAt())
        assertTrue(a.tick(3000 + IslandArbiter.AUTO_EXPAND_MS - 1) is IslandState.Expanded)
    }

    @Test fun `depois de recolher, mensagem nova reabre`() {
        val a = IslandArbiter()
        a.onEvent(note("n", "um"), 0, temp)
        assertTrue(a.tick(IslandArbiter.AUTO_EXPAND_MS) is IslandState.Compact)
        assertTrue(a.onEvent(note("n", "dois"), IslandArbiter.AUTO_EXPAND_MS + 10, temp) is IslandState.Expanded)
    }

    @Test fun `conversa recem aberta nao e interrompida por outra`() {
        val a = IslandArbiter()
        a.onEvent(note("A"), 0, temp)
        assertEquals("A", a.onEvent(note("B"), 500, temp).currentEvent!!.key)
        assertEquals("A", a.tick(1799).currentEvent!!.key)
        val s = a.tick(IslandArbiter.HOLD_MS)
        assertEquals("B", s.currentEvent!!.key)
        assertTrue(s is IslandState.Expanded)   // a conversa que esperou abre quando chega a vez
    }

    @Test fun `chamada fura a trava`() {
        val a = IslandArbiter()
        a.onEvent(note("A"), 0, temp)
        a.onEvent(note("B"), 100, temp)
        assertEquals("call", a.onEvent(call(), 200, temp).currentEvent!!.key)
    }

    @Test fun `depois da trava a conversa nova vence normalmente`() {
        val a = IslandArbiter()
        a.onEvent(note("A"), 0, temp)
        assertEquals("B", a.onEvent(note("B"), IslandArbiter.HOLD_MS + 1, temp).currentEvent!!.key)
    }

    @Test fun `persistente fica ate ser removida`() {
        val a = IslandArbiter()
        a.onEvent(media(), 0, temp)
        assertTrue(a.tick(1_000_000) is IslandState.Compact)
        assertNull(a.nextWakeAt())
        assertEquals(IslandState.Hidden, a.onRemoved("media:p", 1_000_001))
    }

    @Test fun `maior prioridade vence e a menor volta depois`() {
        val a = IslandArbiter()
        a.onEvent(media(), 0, temp)
        assertEquals(IslandPriority.Media, a.onEvent(note("n"), 10, temp).currentEvent!!.priority)
        assertEquals(IslandPriority.Call, a.onEvent(call(), 20, temp).currentEvent!!.priority)
        assertEquals(IslandPriority.Media, a.onRemoved("call", 30).currentEvent!!.priority)
    }

    @Test fun `mesma chave atualiza em vez de duplicar`() {
        val a = IslandArbiter()
        a.onEvent(note("n", "um"), 0, temp)
        a.onEvent(note("n", "dois"), 100, temp)
        assertEquals(1, a.size())
        assertEquals("dois", (a.tick(101).currentEvent as IslandEvent.Notification).text)
        // Repostar renova a abertura automática.
        assertEquals(100 + IslandArbiter.AUTO_EXPAND_MS, a.nextWakeAt())
    }

    @Test fun `fila e limitada e cai o de menor prioridade`() {
        val a = IslandArbiter(maxEntries = 3)
        a.onEvent(call(), 0, temp)
        repeat(5) { a.onEvent(note("n$it"), 1L + it, temp) }
        assertEquals(3, a.size())
        assertEquals(IslandPriority.Call, a.tick(10).currentEvent!!.priority)
    }

    @Test fun `tocar expande e tocar de novo recolhe`() {
        val a = IslandArbiter()
        a.onEvent(media(), 0, temp)
        assertTrue(a.toggleExpand(1, temp) is IslandState.Expanded)
        assertTrue(a.toggleExpand(2, temp) is IslandState.Compact)
    }

    @Test fun `temporaria expandida nao some debaixo do dedo`() {
        val a = IslandArbiter()
        a.onEvent(note("n"), 0, temp)
        a.toggleExpand(4_000, temp)
        assertTrue(a.tick(temp + 100) is IslandState.Expanded)
        assertEquals(IslandState.Hidden, a.tick(4_000 + temp * 2))
    }

    @Test fun `dispensar esconde sem cancelar e conteudo novo traz de volta`() {
        val a = IslandArbiter()
        a.onEvent(media(title = "A"), 0, temp)
        assertEquals(IslandState.Hidden, a.dismiss(1))
        assertEquals(1, a.size())
        a.onEvent(media(title = "A"), 2, temp)            // igual: continua dispensada
        assertEquals(IslandState.Hidden, a.tick(3))
        assertTrue(a.onEvent(media(title = "B"), 4, temp) is IslandState.Compact)   // faixa nova
    }

    @Test fun `remover o evento expandido fecha o painel`() {
        val a = IslandArbiter()
        a.onEvent(media(), 0, temp)
        a.toggleExpand(1, temp)
        assertEquals(IslandState.Hidden, a.onRemoved("media:p", 2))
        assertTrue(a.onEvent(media(), 3, temp) is IslandState.Compact)
    }
}
