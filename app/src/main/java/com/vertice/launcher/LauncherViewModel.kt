package com.vertice.launcher

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.Container
import com.vertice.launcher.core.GridSpec
import com.vertice.launcher.core.LayoutItem
import com.vertice.launcher.core.LayoutOps
import com.vertice.launcher.core.Placement
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.Wallpaper
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.data.AppRepository
import com.vertice.launcher.data.IconCache
import com.vertice.launcher.data.LauncherPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val wallpaper: Wallpaper = Wallpaper.System
) {
    fun categoryOf(app: AppEntry): AppCategory = categories[app.key] ?: AppCategory.default
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LauncherPrefs(application)
    private val repo = AppRepository(application)
    private val recommended = PerformanceProfile.recommended(application)

    val icons = IconCache(application, recommended)

    val ui: StateFlow<HomeUi> = combine(repo.apps, prefs.prefs) { apps, p ->
        val profile = p.profile ?: recommended
        val byKey = apps.associateBy { it.key }
        // Apps desinstalados somem sozinhos do layout; a chave salva é ignorada (e limpa na próxima edição).
        val layout = LayoutOps.sanitize(p.layout, byKey.keys)
        val dockItems = layout.filter { it.container == Container.Dock }.sortedBy { it.x }
        val placed = layout.filter { it.container == Container.Home }
            .mapNotNull { item -> byKey[item.key]?.let { PlacedApp(it, item) } }
        HomeUi(
            loading = !p.seeded,
            profile = profile,
            apps = apps,
            dock = dockItems.mapNotNull { byKey[it.key] },
            placed = placed,
            pageCount = (placed.maxOfOrNull { it.item.page } ?: 0) + 1,
            placement = layout.associate { it.key to if (it.container == Container.Dock) Placement.Dock else Placement.Home },
            categories = apps.associate { app ->
                app.key to (AppCategory.fromName(p.categoryOverrides[app.key])
                    ?: AppCategory.detect(app.declaredCategory, app.packageName))
            },
            manualCategories = p.categoryOverrides.keys,
            clockStyle = p.clockStyle,
            clockFormat = p.clockFormat,
            showDate = p.showDate,
            wallpaper = p.wallpaper
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUi())

    init {
        // Primeira execução: escolhe o perfil pelo hardware e monta o dock inicial.
        viewModelScope.launch {
            val apps = repo.apps.first { it.isNotEmpty() }
            if (!prefs.current().seeded) {
                val wanted = repo.defaultDockPackages()
                val dock = wanted.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg }?.key }
                    .take(GridSpec.DOCK_SLOTS)
                    .mapIndexed { i, key -> LayoutItem(key, Container.Dock, 0, i, 0) }
                prefs.seed(recommended, dock)
            }
        }
        // O cache de ícones acompanha o perfil escolhido.
        viewModelScope.launch {
            prefs.prefs.collect { p -> icons.configure(p.profile ?: recommended) }
        }
    }

    fun openApp(app: AppEntry, options: Bundle?) = repo.launch(app, options)

    fun openAppInfo(app: AppEntry) = repo.openAppInfo(app)

    fun uninstall(app: AppEntry) = repo.uninstall(app)

    fun placementOf(app: AppEntry): Placement = ui.value.placement[app.key] ?: Placement.None

    /** Toda edição passa por aqui: transação única no DataStore, sempre normalizada. */
    private fun edit(op: (List<LayoutItem>) -> List<LayoutItem>) {
        viewModelScope.launch {
            val present = repo.apps.value.map { it.key }.toSet()
            prefs.editLayout { current ->
                val next = op(current)
                // Com a lista de apps ainda vazia, não apaga nada do que foi salvo.
                if (present.isEmpty()) next else LayoutOps.sanitize(next, present)
            }
        }
    }

    fun toggleDock(app: AppEntry) = edit {
        if (it.any { i -> i.key == app.key && i.container == Container.Dock }) LayoutOps.removeFromDock(it, app.key)
        else LayoutOps.addToDock(it, app.key) // dock cheio: nada muda
    }

    fun addToHome(app: AppEntry) = edit { LayoutOps.addToHome(it, app.key) }

    fun removeFromHome(app: AppEntry) = edit { LayoutOps.removeFromHome(it, app.key) }

    fun moveToCell(app: AppEntry, page: Int, x: Int, y: Int) = edit { LayoutOps.moveHome(it, app.key, page, x, y) }

    /** `category = null` volta a classificação automática do app. */
    fun setCategory(app: AppEntry, category: AppCategory?) {
        viewModelScope.launch { prefs.setCategory(app.key, category?.name) }
    }

    fun setClockStyle(style: ClockStyle) { viewModelScope.launch { prefs.setClockStyle(style) } }

    fun setClockFormat(format: ClockFormat) { viewModelScope.launch { prefs.setClockFormat(format) } }

    fun setShowDate(show: Boolean) { viewModelScope.launch { prefs.setShowDate(show) } }

    fun setWallpaper(w: Wallpaper) { viewModelScope.launch { prefs.setWallpaper(w) } }

    fun openClock() = repo.openClockApp()

    fun openCalendar() = repo.openCalendarApp()

    fun setProfile(profile: PerformanceProfile) {
        viewModelScope.launch { prefs.setProfile(profile) }
    }

    override fun onCleared() {
        repo.close()
        super.onCleared()
    }
}
