package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IndicatorTest {
    @Test fun `estilo desconhecido volta a pilula`() {
        assertEquals(IndicatorStyle.Pill, IndicatorStyle.fromName("?"))
        assertEquals(IndicatorStyle.Hidden, IndicatorStyle.fromName("Hidden"))
    }

    @Test fun `backup guarda o indicador`() {
        val r = BackupCodec.decode(BackupCodec.encode(Prefs(indicator = IndicatorStyle.Dots))) as BackupCodec.Result.Ok
        assertEquals(IndicatorStyle.Dots, r.prefs.indicator)
    }
}
