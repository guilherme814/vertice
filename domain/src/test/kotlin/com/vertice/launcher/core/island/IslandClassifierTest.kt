package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandClassifierTest {
    private val on = IslandSettings(enabled = true)
    private val wa = mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp"))
    private fun msg(pkg: String = "com.whatsapp", block: RawNotification.() -> RawNotification = { this }) =
        RawNotification("k-$pkg", pkg, title = "Ana", text = "oi").block()

    @Test fun `ilha desligada nao gera nada`() {
        assertNull(IslandClassifier.classify(msg(), IslandSettings(enabled = false), wa))
    }

    @Test fun `app habilitado vira notificacao com conteudo`() {
        val e = IslandClassifier.classify(msg(), on, wa) as IslandEvent.Notification
        assertEquals("Ana", e.title)
        assertEquals(false, e.contentHidden)
        assertEquals(false, e.persistent)
    }

    @Test fun `app fora da lista ou desligado e ignorado`() {
        assertNull(IslandClassifier.classify(msg("com.outro"), on, wa))
        assertNull(IslandClassifier.classify(msg(), on, mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp", enabled = false))))
    }

    @Test fun `conteudo oculto por configuracao ou por notificacao secreta`() {
        val hide = mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp", showContent = false))
        val a = IslandClassifier.classify(msg(), on, hide) as IslandEvent.Notification
        assertTrue(a.contentHidden); assertNull(a.title); assertNull(a.text)
        val b = IslandClassifier.classify(msg { copy(isSecret = true) }, on, wa) as IslandEvent.Notification
        assertTrue(b.contentHidden); assertNull(b.title)
    }

    @Test fun `resumo de grupo silenciosa e em andamento sao ruido`() {
        assertNull(IslandClassifier.classify(msg { copy(isGroupSummary = true) }, on, wa))
        assertNull(IslandClassifier.classify(msg { copy(isSilent = true) }, on, wa))
        assertNull(IslandClassifier.classify(msg { copy(isOngoing = true) }, on, wa))
    }

    @Test fun `chamada exige acoes de atender e respeita o interruptor`() {
        val call = RawNotification("c", "com.phone", category = RawNotification.CATEGORY_CALL, hasCallActions = true, title = "Mãe")
        val e = IslandClassifier.classify(call, on, emptyMap()) as IslandEvent.Call
        assertEquals("Mãe", e.caller); assertEquals(false, e.inProgress)
        assertNull(IslandClassifier.classify(call.copy(hasCallActions = false), on, emptyMap()))
        assertNull(IslandClassifier.classify(call, on.copy(calls = false), emptyMap()))
        assertEquals(true, (IslandClassifier.classify(call.copy(chronometerBase = 5L), on, emptyMap()) as IslandEvent.Call).inProgress)
    }

    @Test fun `cronometro vira timer mesmo sem app habilitado`() {
        val t = RawNotification("t", "com.clock", category = RawNotification.CATEGORY_ALARM, chronometerBase = 10L, title = "Timer")
        assertTrue(IslandClassifier.classify(t, on, emptyMap()) is IslandEvent.Timer)
        assertNull(IslandClassifier.classify(t, on.copy(timers = false), emptyMap()))
    }

    @Test fun `modo persistente do app e respeitado`() {
        val p = mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp", mode = IslandMode.Persistent))
        assertEquals(true, (IslandClassifier.classify(msg(), on, p) as IslandEvent.Notification).persistent)
    }

    @Test fun `toString nunca expoe titulo nem texto`() {
        val s = msg().toString() + IslandClassifier.classify(msg(), on, wa).toString()
        assertTrue("Ana" !in s && "oi" !in s)
    }
}
