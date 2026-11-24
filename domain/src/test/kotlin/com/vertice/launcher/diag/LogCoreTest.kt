package com.vertice.launcher.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class LogCoreTest {

    @Test fun `linha tem data nivel tag e thread`() {
        val l = LogFormat.line(0L, LogLevel.Warn, "Home", "main", "oi", ZoneOffset.UTC)
        assertEquals("1970-01-01 00:00:00.000 W/Home [main] oi", l)
    }

    @Test fun `mensagem de varias linhas indenta a continuacao`() {
        val l = LogFormat.line(0L, LogLevel.Error, "Crash", "t", "a\nb", ZoneOffset.UTC)
        assertEquals(2, l.lines().size)
        assertTrue(l.lines()[1].startsWith("    b"))
    }

    @Test fun `selo de arquivo`() {
        assertEquals("19700101-000000", LogFormat.fileStamp(0L, ZoneOffset.UTC))
    }

    @Test fun `pilha limita quadros e avisa o resto`() {
        val frames = Array(5) { StackTraceElement("C", "m$it", "F.kt", it) }
        val s = LogFormat.stack(frames, 3)
        assertEquals(4, s.lines().size)
        assertTrue(s.lines().last().contains("+2"))
        assertEquals(5, LogFormat.stack(frames, 10).lines().size)
    }

    @Test fun `anel guarda so as ultimas linhas`() {
        val r = LineRing(3)
        (1..5).forEach { r.add("l$it") }
        assertEquals(listOf("l3", "l4", "l5"), r.last())
        assertEquals(listOf("l5"), r.last(1))
        r.clear()
        assertTrue(r.last().isEmpty())
    }

    @Test fun `faixas de quadro sao cumulativas`() {
        val s = FrameStats()
        listOf(10L, 40L, 150L, 800L).forEach(s::add)
        val x = s.summary()
        assertEquals(4, x.frames)
        assertEquals(3, x.slow)
        assertEquals(2, x.janky)
        assertEquals(1, x.frozen)
        assertEquals(800L, x.worstMs)
        assertEquals(250L, x.avgMs)
    }

    @Test fun `reset zera e resumo vazio nao divide por zero`() {
        val s = FrameStats()
        s.add(100)
        s.reset()
        assertEquals(FrameStats.Summary(0, 0, 0, 0, 0, 0), s.summary())
    }
}
