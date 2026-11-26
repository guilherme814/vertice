package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageTest {
    @Test fun `registrar soma a contagem e guarda o ultimo uso`() {
        val m = UsageOps.record(UsageOps.record(emptyMap(), "a", 10), "a", 20)
        assertEquals(Usage(2, 20), m["a"])
    }

    @Test fun `passou do limite saem os de uso mais antigo`() {
        var m = emptyMap<String, Usage>()
        for (i in 0 until UsageOps.MAX + 5) m = UsageOps.record(m, "k$i", i.toLong())
        assertEquals(UsageOps.MAX, m.size)
        assertNull(m["k0"])
        assertEquals(Usage(1, (UsageOps.MAX + 4).toLong()), m["k${UsageOps.MAX + 4}"])
    }

    @Test fun `mais usados ordena por contagem, depois recencia, e ignora o que nao esta liberado`() {
        val m = mapOf("a" to Usage(5, 1), "b" to Usage(5, 9), "c" to Usage(9, 0), "x" to Usage(99, 0))
        assertEquals(listOf("c", "b", "a"), UsageOps.mostUsed(m, setOf("a", "b", "c"), 5))
        assertEquals(listOf("c"), UsageOps.mostUsed(m, setOf("a", "b", "c"), 1))
    }

    @Test fun `codec faz ida e volta e recusa lixo`() {
        val m = mapOf("com.a/.Main" to Usage(3, 77), "x|y,z" to Usage(1, 5))
        assertEquals(m, UsageCodec.decodeAll(UsageCodec.encodeAll(m)))
        assertNull(UsageCodec.decode("lixo"))
        assertNull(UsageCodec.decode("a|0|5"))
    }

    @Test fun `gaveta por uso traz o mais recente primeiro e os nunca abertos em ordem alfabetica`() {
        fun app(l: String) = AppModel(l, "p.$l", l, 0, -1)
        val usage = mapOf("C" to Usage(1, 50), "B" to Usage(1, 90))
        val r = DrawerList.sorted(listOf(app("A"), app("B"), app("C"), app("D")), DrawerSort.Recent, emptySet(), usage)
        assertEquals(listOf("B", "C", "A", "D"), r.map { it.label })
    }
}
