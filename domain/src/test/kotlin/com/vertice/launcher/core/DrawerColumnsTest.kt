package com.vertice.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerColumnsTest {
    @Test fun `colunas ficam entre 3 e 6`() {
        assertEquals(3, DrawerColumns.coerce(0))
        assertEquals(6, DrawerColumns.coerce(99))
        assertEquals(5, DrawerColumns.coerce(5))
    }

    @Test fun `escala do icone diminui com mais colunas e tem limites`() {
        assertEquals(1f, DrawerColumns.iconScale(4), 0.0001f)
        assertEquals(1.15f, DrawerColumns.iconScale(3), 0.0001f)
        assertEquals(0.75f, DrawerColumns.iconScale(6), 0.0001f)
    }

    @Test fun `layout desconhecido volta a grade`() {
        assertEquals(DrawerLayout.Grid, DrawerLayout.fromName("x"))
        assertEquals(DrawerLayout.List, DrawerLayout.fromName("List"))
    }

    @Test fun `backup invalido usa o padrao`() {
        val text = BackupCodec.encode(Prefs()).replace("drawerColumns=4", "drawerColumns=zz")
        val r = BackupCodec.decode(text) as BackupCodec.Result.Ok
        assertEquals(DrawerColumns.DEFAULT, r.prefs.drawerColumns)
    }
}
