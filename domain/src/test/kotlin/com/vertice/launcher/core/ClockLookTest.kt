package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockLookTest {
    @Test fun `valores desconhecidos voltam ao padrao`() {
        assertEquals(ClockColor.White, ClockColor.fromName("?"))
        assertEquals(ClockScale.Medium, ClockScale.fromName(null))
        assertEquals(ClockAlign.Start, ClockAlign.fromName("zz"))
    }

    @Test fun `fatores de tamanho crescem em ordem`() {
        val f = ClockScale.entries.map { it.factor }
        assertEquals(f.sorted(), f)
        assertTrue(f.all { it > 0f })
    }

    @Test fun `backup guarda cor tamanho e alinhamento`() {
        val p = Prefs(clockColor = ClockColor.Accent, clockScale = ClockScale.Small, clockAlign = ClockAlign.End)
        val r = BackupCodec.decode(BackupCodec.encode(p)) as BackupCodec.Result.Ok
        assertEquals(ClockColor.Accent, r.prefs.clockColor)
        assertEquals(ClockScale.Small, r.prefs.clockScale)
        assertEquals(ClockAlign.End, r.prefs.clockAlign)
    }
}
