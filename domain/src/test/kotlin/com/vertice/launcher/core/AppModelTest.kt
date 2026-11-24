package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppModelTest {
    @Test fun `categoria automatica usa a declarada e depois o pacote`() {
        assertEquals(AppCategory.Games, AppModel("k", "com.x", "X", 0, AppCategory.CAT_GAME).autoCategory)
        assertEquals(AppCategory.Social, AppModel("k", "org.telegram.messenger", "Telegram", 0, -1).autoCategory)
    }

    @Test fun `igualdade por conteudo`() {
        assertEquals(AppModel("k", "p", "L", 1, -1), AppModel("k", "p", "L", 1, -1))
    }
}
