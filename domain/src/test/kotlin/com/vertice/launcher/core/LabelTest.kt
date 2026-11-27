package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelTest {
    @Test fun `tamanho desconhecido volta ao medio`() {
        assertEquals(LabelSize.Medium, LabelSize.fromName(null))
        assertEquals(LabelSize.Large, LabelSize.fromName("Large"))
    }

    @Test fun `altura da linha sempre maior que o corpo`() {
        LabelSize.entries.forEach { assertTrue(it.lineSp > it.sp) }
    }

    @Test fun `backup guarda tamanho e negrito`() {
        val p = Prefs(labelSize = LabelSize.Small, labelBold = true)
        val r = BackupCodec.decode(BackupCodec.encode(p)) as BackupCodec.Result.Ok
        assertEquals(LabelSize.Small, r.prefs.labelSize)
        assertTrue(r.prefs.labelBold)
    }
}
