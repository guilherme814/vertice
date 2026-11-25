package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val sample = Prefs(
        profile = PerformanceProfile.Premium,
        layout = listOf(
            LayoutItem("com.a/.Main", Container.Dock, 0, 0, 0),
            LayoutItem("com.b/.X\$Y|z;w#7 ç", Container.Home, 1, 2, 3)
        ),
        seeded = true,
        categoryOverrides = mapOf("com.a/.Main" to "Games", "k=ey&x" to "Social"),
        clockStyle = ClockStyle.Minimal,
        clockFormat = ClockFormat.H24,
        showDate = false,
        wallpaper = Wallpaper.entries.last(),
        grid = GridConfig(5, 6),
        iconSize = IconSize.Large,
        showLabels = false,
        accent = Accent.Violet,
        glass = GlassLevel.Strong
    )

    @Test fun `ida e volta preserva tudo`() {
        val r = BackupCodec.decode(BackupCodec.encode(sample)) as BackupCodec.Result.Ok
        assertEquals(sample, r.prefs)
        assertEquals(0, r.ignoredLines)
    }

    @Test fun `linhas desconhecidas ou quebradas sao ignoradas e contadas`() {
        val text = BackupCodec.encode(sample) + "futuro=1\nlixo\nlayout=nao-e-item\nprofile=Inexistente\n"
        val r = BackupCodec.decode(text) as BackupCodec.Result.Ok
        assertEquals(sample, r.prefs)
        assertEquals(4, r.ignoredLines)
    }

    @Test fun `cabecalho errado e recusado`() {
        assertTrue(BackupCodec.decode("qualquer coisa") is BackupCodec.Result.Fail)
        assertTrue(BackupCodec.decode("") is BackupCodec.Result.Fail)
        assertTrue(BackupCodec.decode("VERTICE-BACKUP x") is BackupCodec.Result.Fail)
    }

    @Test fun `backup de versao mais nova e recusado`() {
        assertTrue(BackupCodec.decode("VERTICE-BACKUP ${BackupCodec.VERSION + 1}\n") is BackupCodec.Result.Fail)
    }

    @Test fun `migracao do dock antigo`() {
        assertEquals(
            listOf(LayoutItem("a", Container.Dock, 0, 0, 0), LayoutItem("b", Container.Dock, 0, 1, 0)),
            SettingsSchema.layoutFromLegacyDock("a;;b")
        )
        assertTrue(SettingsSchema.layoutFromLegacyDock(null).isEmpty())
        assertTrue(SettingsSchema.layoutFromLegacyDock("").isEmpty())
    }
}
