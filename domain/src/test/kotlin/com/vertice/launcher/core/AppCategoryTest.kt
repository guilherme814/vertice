package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppCategoryTest {
    @Test fun `categoria declarada vence o palpite pelo pacote`() {
        assertEquals(AppCategory.Social, AppCategory.detect(AppCategory.CAT_SOCIAL, "com.exemplo.qualquer"))
        assertEquals(AppCategory.Entertainment, AppCategory.detect(AppCategory.CAT_AUDIO, "com.x"))
        assertEquals(AppCategory.Entertainment, AppCategory.detect(AppCategory.CAT_VIDEO, "com.x"))
        assertEquals(AppCategory.Photos, AppCategory.detect(AppCategory.CAT_IMAGE, "com.x"))
        assertEquals(AppCategory.Games, AppCategory.detect(AppCategory.CAT_GAME, "com.x"))
        assertEquals(AppCategory.Productivity, AppCategory.detect(AppCategory.CAT_PRODUCTIVITY, "com.x"))
        assertEquals(AppCategory.News, AppCategory.detect(AppCategory.CAT_NEWS, "com.x"))
        assertEquals(AppCategory.Travel, AppCategory.detect(AppCategory.CAT_MAPS, "com.x"))
    }

    @Test fun `sem categoria declarada usa o nome do pacote`() {
        assertEquals(AppCategory.Social, AppCategory.detect(-1, "com.whatsapp"))
        assertEquals(AppCategory.Entertainment, AppCategory.detect(-1, "com.google.android.youtube"))
        assertEquals(AppCategory.Photos, AppCategory.detect(-1, "com.android.camera"))
        assertEquals(AppCategory.Games, AppCategory.detect(-1, "com.supercell.games"))
        assertEquals(AppCategory.Tools, AppCategory.detect(-1, "com.desconhecido.app"))
    }

    @Test fun `constantes batem com o Android`() {
        // ApplicationInfo.CATEGORY_GAME=0, AUDIO=1, VIDEO=2, IMAGE=3, SOCIAL=4, NEWS=5, MAPS=6, PRODUCTIVITY=7
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), listOf(
            AppCategory.CAT_GAME, AppCategory.CAT_AUDIO, AppCategory.CAT_VIDEO, AppCategory.CAT_IMAGE,
            AppCategory.CAT_SOCIAL, AppCategory.CAT_NEWS, AppCategory.CAT_MAPS, AppCategory.CAT_PRODUCTIVITY
        ))
    }

    @Test fun `fromName`() {
        assertEquals(AppCategory.Games, AppCategory.fromName("Games"))
        assertNull(AppCategory.fromName("Inexistente"))
        assertNull(AppCategory.fromName(null))
    }
}
