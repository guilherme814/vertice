package com.vertice.launcher.core.island

import com.vertice.launcher.core.BackupCodec
import com.vertice.launcher.core.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslandBackupTest {
    private val snap = IslandSnapshot(
        IslandSettings(enabled = true, media = false, bluetooth = true, animate = false, tempSeconds = 7, offsetDp = 20, width = IslandWidth.Wide,
            background = IslandBackground.Glass, autoExpand = false, expandSeconds = 6),
        listOf(
            IslandAppConfig("com.whatsapp", true, IslandMode.Persistent, false),
            IslandAppConfig("org.telegram.messenger", false, IslandMode.Temporary, true)
        )
    )

    @Test fun `ida e volta junto com o backup geral`() {
        val r = BackupCodec.decode(BackupCodec.encode(Prefs(), snap)) as BackupCodec.Result.Ok
        assertEquals(snap.settings, r.island!!.settings)
        assertEquals(snap.apps.sortedBy { it.packageName }, r.island!!.apps.sortedBy { it.packageName })
        assertEquals(0, r.ignoredLines)
    }

    @Test fun `backup antigo sem ilha nao traz ilha`() {
        assertNull((BackupCodec.decode(BackupCodec.encode(Prefs())) as BackupCodec.Result.Ok).island)
    }

    @Test fun `valores invalidos viram padrao e linhas ruins sao descartadas`() {
        val text = BackupCodec.encode(Prefs(), snap)
            .replace("island.tempSeconds=7", "island.tempSeconds=999")
            .replace("island.width=Wide", "island.width=Gigante") + "island.app=lixo\nisland.app=%ZZ|true|Temporary|true\n"
        val i = (BackupCodec.decode(text) as BackupCodec.Result.Ok).island!!
        assertEquals(IslandSettings.TEMP_MAX, i.settings.tempSeconds)
        assertEquals(IslandWidth.Medium, i.settings.width)
        assertEquals(2, i.apps.size)
    }

    @Test fun `animacao nasce ligada`() {
        assertEquals(true, IslandSettings().animate)
    }
}
