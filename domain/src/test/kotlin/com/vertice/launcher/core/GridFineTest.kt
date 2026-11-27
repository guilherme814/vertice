package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GridFineTest {
    @Test fun `valores fora do limite sao corrigidos`() {
        val f = GridFine(marginH = -5, marginV = 500, iconPercent = 10).sanitized()
        assertEquals(GridFine.MARGIN_H_MIN, f.marginH)
        assertEquals(GridFine.MARGIN_V_MAX, f.marginV)
        assertEquals(GridFine.ICON_MIN, f.iconPercent)
    }

    @Test fun `escala do icone combina nivel e ajuste fino`() {
        assertEquals(1f, GridFine().iconScale(IconSize.Medium), 0.0001f)
        assertEquals(1.2f * 1.3f, GridFine(iconPercent = 130).iconScale(IconSize.Large), 0.0001f)
        assertEquals(0.85f * 0.7f, GridFine(iconPercent = 70).iconScale(IconSize.Small), 0.0001f)
        // Valor absurdo nunca passa do teto.
        assertEquals(1f * 1.3f, GridFine(iconPercent = 9999).iconScale(IconSize.Medium), 0.0001f)
    }

    @Test fun `backup guarda o ajuste fino e limita lixo`() {
        val p = Prefs(gridFine = GridFine(8, 16, 90))
        val r = BackupCodec.decode(BackupCodec.encode(p)) as BackupCodec.Result.Ok
        assertEquals(GridFine(8, 16, 90), r.prefs.gridFine)
        val bad = BackupCodec.encode(p).replace("iconPercent=90", "iconPercent=900")
        assertEquals(GridFine.ICON_MAX, (BackupCodec.decode(bad) as BackupCodec.Result.Ok).prefs.gridFine.iconPercent)
    }
}
