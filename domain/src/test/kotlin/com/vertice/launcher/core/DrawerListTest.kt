package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DrawerListTest {
    private fun app(label: String, t: Long = 0, key: String = label) = AppModel(key, "p.$key", label, t, -1)

    @Test fun `letra ignora acento e caixa e manda o resto para #`() {
        assertEquals('A', DrawerList.letterOf("água"))
        assertEquals('Z', DrawerList.letterOf("  zap"))
        assertEquals(DrawerList.OTHER, DrawerList.letterOf("9gag"))
        assertEquals(DrawerList.OTHER, DrawerList.letterOf(""))
    }

    @Test fun `AZ ordena sem acento, deixa # por ultimo e esconde ocultos`() {
        val apps = listOf(app("Zeta"), app("Água"), app("1Pass"), app("beta"), app("Oculto"))
        val r = DrawerList.sorted(apps, DrawerSort.AZ, setOf("Oculto")).map { it.label }
        assertEquals(listOf("Água", "beta", "Zeta", "1Pass"), r)
    }

    @Test fun `Novos traz o instalado por ultimo primeiro, empate por nome`() {
        val apps = listOf(app("B", 5), app("A", 5), app("C", 9))
        assertEquals(listOf("C", "A", "B"), DrawerList.sorted(apps, DrawerSort.Newest, emptySet()).map { it.label })
    }

    @Test fun `secoes seguem A a Z com # no fim e mantem a ordem interna`() {
        val s = DrawerList.sections(DrawerList.sorted(listOf(app("Bola"), app("Ábaco"), app("Ana"), app("7zip")), DrawerSort.AZ, emptySet()))
        assertEquals(listOf('A', 'B', DrawerList.OTHER), s.map { it.letter })
        assertEquals(listOf("Ábaco", "Ana"), s[0].items.map { it.label })
    }

    @Test fun `ocultar e reexibir persiste no store`() = kotlinx.coroutines.runBlocking {
        val st = InMemorySettings()
        st.setHidden("k", true)
        assertFalse(st.current().hiddenApps.isEmpty())
        st.setHidden("k", false)
        assertEquals(emptySet<String>(), st.current().hiddenApps)
    }
}
