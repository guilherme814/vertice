package com.vertice.launcher.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Armazenamento em memória: prova que o contrato é implementável sem Android e serve de fake nos próximos testes. */
class InMemorySettings : SettingsStore {
    private val state = MutableStateFlow(Prefs())
    override val prefs = state.map { it }

    override suspend fun editHome(transform: (HomeData) -> HomeData) {
        val next = transform(HomeData(state.value.layout, state.value.folders))
        state.value = state.value.copy(layout = next.items, folders = next.folders)
    }
    override suspend fun setProfile(profile: PerformanceProfile) { state.value = state.value.copy(profile = profile) }
    override suspend fun setCategory(key: String, category: String?) {
        val m = state.value.categoryOverrides.toMutableMap()
        if (category == null) m.remove(key) else m[key] = category
        state.value = state.value.copy(categoryOverrides = m)
    }
    override suspend fun setClockStyle(style: ClockStyle) { state.value = state.value.copy(clockStyle = style) }
    override suspend fun setClockLook(color: ClockColor, scale: ClockScale, align: ClockAlign) { state.value = state.value.copy(clockColor = color, clockScale = scale, clockAlign = align) }
    override suspend fun setGridFine(fine: GridFine) { state.value = state.value.copy(gridFine = fine.sanitized()) }
    override suspend fun setClockFormat(format: ClockFormat) { state.value = state.value.copy(clockFormat = format) }
    override suspend fun setShowDate(show: Boolean) { state.value = state.value.copy(showDate = show) }
    override suspend fun setWallpaper(w: Wallpaper) { state.value = state.value.copy(wallpaper = w) }
    override suspend fun setGrid(grid: GridConfig) { state.value = state.value.copy(grid = grid) }
    override suspend fun setIconSize(size: IconSize) { state.value = state.value.copy(iconSize = size) }
    override suspend fun setShowLabels(show: Boolean) { state.value = state.value.copy(showLabels = show) }
    override suspend fun setAccent(accent: Accent) { state.value = state.value.copy(accent = accent) }
    override suspend fun setGlass(glass: GlassLevel) { state.value = state.value.copy(glass = glass) }
    override suspend fun setIconShape(shape: IconShape) { state.value = state.value.copy(iconShape = shape) }
    override suspend fun setIconShadow(on: Boolean) { state.value = state.value.copy(iconShadow = on) }
    override suspend fun setDrawerSort(sort: DrawerSort) { state.value = state.value.copy(drawerSort = sort) }
    override suspend fun setHidden(key: String, hidden: Boolean) {
        val h = state.value.hiddenApps
        state.value = state.value.copy(hiddenApps = if (hidden) h + key else h - key)
    }
    override suspend fun recordUse(key: String, nowMs: Long) { state.value = state.value.copy(usage = UsageOps.record(state.value.usage, key, nowMs)) }
    override suspend fun setDockStyle(style: DockStyle) { state.value = state.value.copy(dockStyle = style) }
    override suspend fun setDockOpacity(percent: Int) { state.value = state.value.copy(dockOpacity = DockOpacity.coerce(percent)) }
    override suspend fun setLabelSize(size: LabelSize) { state.value = state.value.copy(labelSize = size) }
    override suspend fun setCustomWallpaper(a: Long, b: Long) { state.value = state.value.copy(customWallpaperA = CustomWallpaper.sanitize(a), customWallpaperB = CustomWallpaper.sanitize(b)) }
    override suspend fun setDrawerLayout(layout: DrawerLayout) { state.value = state.value.copy(drawerLayout = layout) }
    override suspend fun setDrawerColumns(n: Int) { state.value = state.value.copy(drawerColumns = DrawerColumns.coerce(n)) }
    override suspend fun setIndicator(style: IndicatorStyle) { state.value = state.value.copy(indicator = style) }
    override suspend fun setLabelBold(on: Boolean) { state.value = state.value.copy(labelBold = on) }
    override suspend fun setFocus(on: Boolean) { state.value = state.value.copy(focusOn = on) }
    override suspend fun setFocusBlocked(keys: Set<String>) { state.value = state.value.copy(focusBlocked = keys) }
    override suspend fun restore(prefs: Prefs) { state.value = prefs.copy(seeded = true) }
    override suspend fun seed(profile: PerformanceProfile, layout: List<LayoutItem>) {
        state.value = state.value.copy(profile = profile, layout = layout, seeded = true)
    }
}

class SettingsStoreTest {
    @Test fun `seed marca como semeado e guarda o layout`() = runBlocking {
        val s = InMemorySettings()
        s.seed(PerformanceProfile.UltraLite, listOf(LayoutItem("a", Container.Dock, 0, 0, 0)))
        val p = s.current()
        assertTrue(p.seeded)
        assertEquals(PerformanceProfile.UltraLite, p.profile)
        assertEquals(1, p.layout.size)
    }

    @Test fun `editLayout aplica a transformacao sobre o estado atual`() = runBlocking {
        val s = InMemorySettings()
        s.editLayout { LayoutOps.addToHome(it, "a") }
        s.editLayout { LayoutOps.addToHome(it, "b") }
        assertEquals(setOf("a", "b"), s.current().layout.map { it.key }.toSet())
    }

    @Test fun `categoria manual entra e sai`() = runBlocking {
        val s = InMemorySettings()
        s.setCategory("k", "Games")
        assertEquals("Games", s.current().categoryOverrides["k"])
        s.setCategory("k", null)
        assertTrue(s.current().categoryOverrides.isEmpty())
    }

    @Test fun `restaurar substitui tudo`() = runBlocking {
        val s = InMemorySettings()
        s.setCategory("x", "Games")
        s.restore(Prefs(layout = listOf(LayoutItem("a", Container.Home, 0, 0, 0)), showDate = false))
        val p = s.current()
        assertTrue(p.seeded)
        assertTrue(p.categoryOverrides.isEmpty())
        assertEquals(false, p.showDate)
    }
}
