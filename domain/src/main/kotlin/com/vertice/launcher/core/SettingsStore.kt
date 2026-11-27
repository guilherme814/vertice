package com.vertice.launcher.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Tudo o que a launcher guarda. Tudo local: sem conta, sem rede. */
data class Prefs(
    /** null = ainda não escolhido (a primeira execução decide pelo hardware). */
    val profile: PerformanceProfile? = null,
    /** Posição de cada app na Home e no dock (contêiner + página + célula). */
    val layout: List<LayoutItem> = emptyList(),
    /** Pastas da Home (os apps de cada uma ficam só dentro dela). */
    val folders: List<Folder> = emptyList(),
    val seeded: Boolean = false,
    /** Correções manuais: chave do app -> nome da categoria. Vencem a classificação automática. */
    val categoryOverrides: Map<String, String> = emptyMap(),
    val clockStyle: ClockStyle = ClockStyle.Classic,
    val clockFormat: ClockFormat = ClockFormat.System,
    val showDate: Boolean = true,
    val wallpaper: Wallpaper = Wallpaper.System,
    val grid: GridConfig = GridConfig.Default,
    val iconSize: IconSize = IconSize.Medium,
    val showLabels: Boolean = true,
    val accent: Accent = Accent.Mint,
    val glass: GlassLevel = GlassLevel.Medium,
    val iconShape: IconShape = IconShape.Original,
    val iconShadow: Boolean = false,
    val drawerSort: DrawerSort = DrawerSort.AZ,
    /** Chaves dos apps escondidos da gaveta. */
    val hiddenApps: Set<String> = emptySet(),
    /** Uso local de cada app (não entra no backup). */
    val usage: Map<String, Usage> = emptyMap(),
    /** Modo Foco: com ele ligado, os apps de [focusBlocked] somem da gaveta, ficam apagados e não abrem. */
    val focusOn: Boolean = false,
    val focusBlocked: Set<String> = emptySet(),
    val dockStyle: DockStyle = DockStyle.Glass,
    val dockOpacity: Int = DockOpacity.DEFAULT,
    val labelSize: LabelSize = LabelSize.Medium,
    val labelBold: Boolean = false,
    val indicator: IndicatorStyle = IndicatorStyle.Pill,
    val drawerLayout: DrawerLayout = DrawerLayout.Grid,
    val drawerColumns: Int = DrawerColumns.DEFAULT,
    val customWallpaperA: Long = CustomWallpaper.DEFAULT_A,
    val customWallpaperB: Long = CustomWallpaper.DEFAULT_B,
    val clockColor: ClockColor = ClockColor.White,
    val clockScale: ClockScale = ClockScale.Medium,
    val clockAlign: ClockAlign = ClockAlign.Start,
    val gridFine: GridFine = GridFine()
)

/**
 * Contrato de persistência das preferências. A interface fica no domínio; o DataStore (módulo :data)
 * é só uma implementação — nos testes entra uma versão em memória.
 */
interface SettingsStore : LayoutStore {
    val prefs: Flow<Prefs>

    suspend fun current(): Prefs = prefs.first()

    suspend fun setProfile(profile: PerformanceProfile)

    /** `category = null` volta para a classificação automática. */
    suspend fun setCategory(key: String, category: String?)

    suspend fun setClockStyle(style: ClockStyle)
    suspend fun setClockFormat(format: ClockFormat)
    suspend fun setGridFine(fine: GridFine)
    suspend fun setClockLook(color: ClockColor, scale: ClockScale, align: ClockAlign)
    suspend fun setShowDate(show: Boolean)
    suspend fun setWallpaper(w: Wallpaper)
    suspend fun setGrid(grid: GridConfig)
    suspend fun setIconSize(size: IconSize)
    suspend fun setShowLabels(show: Boolean)
    suspend fun setAccent(accent: Accent)
    suspend fun setGlass(glass: GlassLevel)
    suspend fun setIconShape(shape: IconShape)
    suspend fun setIconShadow(on: Boolean)
    suspend fun setDrawerSort(sort: DrawerSort)
    suspend fun setHidden(key: String, hidden: Boolean)
    suspend fun recordUse(key: String, nowMs: Long)
    suspend fun setFocus(on: Boolean)
    suspend fun setDockStyle(style: DockStyle)
    suspend fun setLabelSize(size: LabelSize)
    suspend fun setLabelBold(on: Boolean)
    suspend fun setIndicator(style: IndicatorStyle)
    suspend fun setDrawerLayout(layout: DrawerLayout)
    suspend fun setCustomWallpaper(a: Long, b: Long)
    suspend fun setDrawerColumns(n: Int)
    suspend fun setDockOpacity(percent: Int)
    suspend fun setFocusBlocked(keys: Set<String>)

    /** Substitui todas as preferências (restauração de backup). Grava o formato atual. */
    suspend fun restore(prefs: Prefs)

    /** Primeira execução: grava perfil e layout iniciais e marca como semeado. */
    suspend fun seed(profile: PerformanceProfile, layout: List<LayoutItem>)
}
