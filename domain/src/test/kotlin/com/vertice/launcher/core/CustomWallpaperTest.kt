package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWallpaperTest {
    @Test fun `sanitize forca alfa opaco e mantem rgb`() {
        assertEquals(0xFF112233L, CustomWallpaper.sanitize(0x00112233L))
        assertEquals(0xFF112233L, CustomWallpaper.sanitize(-1L and 0x00112233L))
    }

    @Test fun `base e uma versao escura da cor A`() {
        val base = CustomWallpaper.base(0xFFFFFFFFL)
        assertEquals(0xFF, ((base shr 24) and 0xFF).toInt())
        assertTrue(((base shr 16) and 0xFF) < 30)
    }

    @Test fun `gera tres manchas dentro da tela`() {
        val blobs = CustomWallpaper.blobs(CustomWallpaper.DEFAULT_A, CustomWallpaper.DEFAULT_B)
        assertEquals(3, blobs.size)
        assertTrue(blobs.all { it.x in 0f..1f && it.y in 0f..1f && it.alpha in 0f..1f })
    }

    @Test fun `todos os papeis tem nome unico e manchas validas`() {
        assertEquals(Wallpaper.entries.size, Wallpaper.entries.map { it.label }.toSet().size)
        Wallpaper.entries.forEach { w -> assertTrue(w.blobs.all { it.alpha in 0f..1f && it.radius > 0f }) }
    }

    @Test fun `backup guarda as duas cores e ignora lixo`() {
        val p = Prefs(wallpaper = Wallpaper.Custom, customWallpaperA = 0xFFEF4444L, customWallpaperB = 0xFF22D3EEL)
        val r = BackupCodec.decode(BackupCodec.encode(p)) as BackupCodec.Result.Ok
        assertEquals(0xFFEF4444L, r.prefs.customWallpaperA)
        assertEquals(0xFF22D3EEL, r.prefs.customWallpaperB)
        val bad = BackupCodec.encode(p).replace(Regex("customWallpaperA=\\d+"), "customWallpaperA=xx")
        val r2 = BackupCodec.decode(bad) as BackupCodec.Result.Ok
        assertEquals(CustomWallpaper.DEFAULT_A, r2.prefs.customWallpaperA)
    }
}
