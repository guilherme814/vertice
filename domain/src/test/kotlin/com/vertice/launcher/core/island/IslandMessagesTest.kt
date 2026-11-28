package com.vertice.launcher.core.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslandMessagesTest {
    private val on = IslandSettings(enabled = true)
    private val wa = mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp"))

    @Test fun `usa a ultima mensagem, a penultima e o contador`() {
        val n = RawNotification("k", "com.whatsapp", title = "Guilherme", text = "velho",
            messages = listOf("penultima", "ultima"), unreadCount = 4)
        val e = IslandClassifier.classify(n, on, wa) as IslandEvent.Notification
        assertEquals("ultima", e.text)
        assertEquals("penultima", e.previous)
        assertEquals(4, e.unread)
    }

    @Test fun `sem estilo de mensagens cai no texto simples`() {
        val e = IslandClassifier.classify(RawNotification("k", "com.whatsapp", title = "A", text = "oi"), on, wa) as IslandEvent.Notification
        assertEquals("oi", e.text)
        assertNull(e.previous)
        assertEquals(1, e.unread)
    }

    @Test fun `conteudo oculto nao vaza mensagens`() {
        val hide = mapOf("com.whatsapp" to IslandAppConfig("com.whatsapp", showContent = false))
        val e = IslandClassifier.classify(
            RawNotification("k", "com.whatsapp", title = "A", messages = listOf("segredo1", "segredo2"), unreadCount = 2), on, hide
        ) as IslandEvent.Notification
        assertNull(e.text); assertNull(e.previous)
    }

    @Test fun `contador nunca fica abaixo de 1`() {
        val e = IslandClassifier.classify(RawNotification("k", "com.whatsapp", unreadCount = 0), on, wa) as IslandEvent.Notification
        assertEquals(1, e.unread)
    }
}
