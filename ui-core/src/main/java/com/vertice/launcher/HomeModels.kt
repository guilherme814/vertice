package com.vertice.launcher

import android.os.Bundle
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.GridConfig
import com.vertice.launcher.core.IconSize
import com.vertice.launcher.core.LayoutItem
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.Placement
import com.vertice.launcher.core.Wallpaper
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.data.IconCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** App + onde ele está na grade da Home. */
data class PlacedApp(val app: AppEntry, val item: LayoutItem)

/** Estado já resolvido para a interface: chaves salvas viram apps reais. */
data class HomeUi(
    val loading: Boolean = true,
    val profile: PerformanceProfile = PerformanceProfile.Balanced,
    val apps: List<AppEntry> = emptyList(),
    /** Dock, na ordem da esquerda para a direita. */
    val dock: List<AppEntry> = emptyList(),
    /** Apps na grade da Home. */
    val placed: List<PlacedApp> = emptyList(),
    /** Quantas páginas a Home tem (no mínimo 1). */
    val pageCount: Int = 1,
    val placement: Map<String, Placement> = emptyMap(),
    /** Categoria final de cada app (chave do app -> categoria). */
    val categories: Map<String, AppCategory> = emptyMap(),
    /** Chaves com categoria definida pelo usuário (para oferecer "voltar ao automático"). */
    val manualCategories: Set<String> = emptySet(),
    val clockStyle: ClockStyle = ClockStyle.Classic,
    val clockFormat: ClockFormat = ClockFormat.System,
    val showDate: Boolean = true,
    val wallpaper: Wallpaper = Wallpaper.System,
    val grid: GridConfig = GridConfig.Default,
    val iconSize: IconSize = IconSize.Medium,
    val showLabels: Boolean = true
) {
    fun categoryOf(app: AppEntry): AppCategory = categories[app.key] ?: AppCategory.default
}

/**
 * O que as telas podem pedir. As telas conhecem só este contrato (não o ViewModel),
 * então vivem em módulos próprios e podem ser testadas com uma versão falsa.
 */
interface HomeActions {
    val ui: StateFlow<HomeUi>
    val icons: IconCache

    /** Bateria em %, null se desconhecida. */
    val batteryPercent: Flow<Int?>

    fun openApp(app: AppEntry, options: Bundle?)
    fun openAppInfo(app: AppEntry)
    fun uninstall(app: AppEntry)
    fun openClock()
    fun openCalendar()

    fun placementOf(app: AppEntry): Placement
    fun toggleDock(app: AppEntry)
    fun addToHome(app: AppEntry)
    fun removeFromHome(app: AppEntry)
    fun moveToCell(app: AppEntry, page: Int, x: Int, y: Int)

    /** `category = null` volta a classificação automática do app. */
    fun setCategory(app: AppEntry, category: AppCategory?)
    fun setClockStyle(style: ClockStyle)
    fun setClockFormat(format: ClockFormat)
    fun setShowDate(show: Boolean)
    fun setWallpaper(w: Wallpaper)
    fun setGrid(columns: Int, rows: Int)
    fun setIconSize(size: IconSize)
    fun setShowLabels(show: Boolean)

    /** Texto do backup das preferências atuais. */
    suspend fun backupText(): String

    /** Restaura de um backup; devolve a mensagem para mostrar ao usuário. */
    suspend fun restoreBackup(text: String): String
}
