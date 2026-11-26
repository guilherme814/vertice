package com.vertice.launcher

import android.os.Bundle
import com.vertice.launcher.core.Accent
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.GlassLevel
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.GridConfig
import com.vertice.launcher.core.DrawerSort
import com.vertice.launcher.core.IconShape
import com.vertice.launcher.core.IconSize
import com.vertice.launcher.core.IconStyle
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
    val showLabels: Boolean = true,
    val accent: Accent = Accent.Mint,
    val glass: GlassLevel = GlassLevel.Medium,
    val iconStyle: IconStyle = IconStyle(),
    val drawerSort: DrawerSort = DrawerSort.AZ,
    val hidden: Set<String> = emptySet()
) {
    fun categoryOf(app: AppEntry): AppCategory = categories[app.key] ?: AppCategory.default
}

/** Abrir apps e telas do sistema. */
interface AppActions {
    fun openApp(app: AppEntry, options: Bundle?)
    fun openAppInfo(app: AppEntry)
    fun uninstall(app: AppEntry)
    fun openClock()
    fun openCalendar()
}

/** Organização: onde cada app fica (Home, dock, grade) e a categoria de cada um. */
interface OrganizeActions {
    fun placementOf(app: AppEntry): Placement
    fun toggleDock(app: AppEntry)
    fun addToHome(app: AppEntry)
    fun removeFromHome(app: AppEntry)
    fun moveToCell(app: AppEntry, page: Int, x: Int, y: Int)
    /** Põe o app no dock na posição [index] (arrastar e soltar). */
    fun moveToDock(app: AppEntry, index: Int)
    fun setGrid(columns: Int, rows: Int)

    /** `category = null` volta a classificação automática do app. */
    fun setCategory(app: AppEntry, category: AppCategory?)
}

/** Aparência: relógio, papel de parede, ícones. */
interface AppearanceActions {
    fun setClockStyle(style: ClockStyle)
    fun setClockFormat(format: ClockFormat)
    fun setShowDate(show: Boolean)
    fun setWallpaper(w: Wallpaper)
    fun setIconSize(size: IconSize)
    fun setShowLabels(show: Boolean)
    fun setAccent(accent: Accent)
    fun setGlass(glass: GlassLevel)
    fun setIconShape(shape: IconShape)
    fun setIconShadow(on: Boolean)
}

/** Gaveta: ordem e apps ocultos. */
interface DrawerActions {
    fun setDrawerSort(sort: DrawerSort)
    fun setHidden(app: AppEntry, hidden: Boolean)
}

/** Backup e restauração das preferências. */
interface BackupActions {
    /** Texto do backup das preferências atuais. */
    suspend fun backupText(): String

    /** Restaura de um backup; devolve a mensagem para mostrar ao usuário. */
    suspend fun restoreBackup(text: String): String
}

/**
 * O que as telas enxergam: estado pronto + contratos pequenos por assunto.
 * As telas conhecem só isto (não o ViewModel), então vivem em módulos próprios e testam com versão falsa.
 */
interface HomeActions : AppActions, OrganizeActions, AppearanceActions, BackupActions, DrawerActions {
    val ui: StateFlow<HomeUi>
    val icons: IconCache

    /** Bateria em %, null se desconhecida. */
    val batteryPercent: Flow<Int?>
}
