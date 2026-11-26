package com.vertice.launcher

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.vertice.launcher.core.Accent
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.DrawerSort
import com.vertice.launcher.core.GlassLevel
import com.vertice.launcher.core.BackupCodec
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
import com.vertice.launcher.core.Container
import com.vertice.launcher.core.HomeState
import com.vertice.launcher.core.GridConfig
import com.vertice.launcher.core.GridSpec
import com.vertice.launcher.core.IconShape
import com.vertice.launcher.core.IconSize
import com.vertice.launcher.core.IconStyle
import com.vertice.launcher.core.LayoutItem
import com.vertice.launcher.core.LayoutOps
import com.vertice.launcher.core.Placement
import com.vertice.launcher.core.SettingsStore
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.Wallpaper
import com.vertice.launcher.data.AppEntry
import com.vertice.launcher.platform.AppCatalog
import com.vertice.launcher.platform.AppLauncher
import com.vertice.launcher.platform.BatteryStatus
import com.vertice.launcher.data.IconCache
import com.vertice.launcher.diag.SystemSnapshot
import com.vertice.launcher.diag.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Recebe tudo pronto (veja [AppContainer]); não cria nem fecha nada sozinho.
 * Os dados vivem no processo, não na tela: girar/recriar a Activity não recarrega a lista de apps.
 */
class LauncherViewModel(
    private val prefs: SettingsStore,
    private val catalog: AppCatalog,
    private val launcher: AppLauncher,
    battery: BatteryStatus,
    override val icons: IconCache,
    private val recommended: PerformanceProfile
) : ViewModel(), HomeActions {

    // Classificação automática só depende do app: calculada uma vez, não a cada edição de preferência.
    private val autoCategory = HashMap<String, AppCategory>()

    override val batteryPercent = battery.percent

    override val ui: StateFlow<HomeUi> = combine(catalog.apps, prefs.prefs) { apps, p ->
        val byKey = apps.associateBy { it.key }
        // Toda a regra (layout, dock, páginas, categorias) vive no domínio; aqui só trocamos chaves por apps.
        val state = HomeState.from(apps.map { it.model }, p) { m ->
            autoCategory.getOrPut(m.key) { m.autoCategory }
        }
        HomeUi(
            // Só "pronta" com preferências E lista de apps: evita piscar uma Home vazia depois que o sistema mata o processo.
            loading = !p.seeded || apps.isEmpty(),
            profile = p.profile ?: recommended,
            apps = apps,
            dock = state.dock.mapNotNull { byKey[it] },
            placed = state.grid.mapNotNull { item -> byKey[item.key]?.let { PlacedApp(it, item) } },
            pageCount = state.pageCount,
            placement = state.placement,
            categories = state.categories,
            manualCategories = state.manualCategories,
            clockStyle = p.clockStyle,
            clockFormat = p.clockFormat,
            showDate = p.showDate,
            wallpaper = p.wallpaper,
            grid = p.grid,
            iconSize = p.iconSize,
            showLabels = p.showLabels,
            accent = p.accent,
            glass = p.glass,
            // Sombra custa desenho: o Ultra Lite nunca a usa.
            drawerSort = p.drawerSort,
            hidden = p.hiddenApps,
            iconStyle = IconStyle(p.iconShape, p.iconShadow && (p.profile ?: recommended) != PerformanceProfile.UltraLite)
        )
    }
        .distinctUntilChanged()          // recarga sem mudança real não redesenha a Home
        .flowOn(Dispatchers.Default)     // montar o estado não ocupa a thread da interface
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUi())

    init {
        // Marco de abertura: quando a Home tem dados reais para mostrar.
        viewModelScope.launch {
            val ready = ui.first { !it.loading }
            VLog.i("Abertura", "Home pronta: ${ready.apps.size} apps, ${ready.placed.size} na grade, ${ready.dock.size} no dock, " +
                "perfil=${ready.profile} (+${SystemSnapshot.sinceProcessStartMs()}ms desde o início do processo)")
        }
        // Primeira execução: escolhe o perfil pelo hardware e monta o dock inicial.
        viewModelScope.launch {
            val apps = catalog.apps.first { it.isNotEmpty() }
            if (!prefs.current().seeded) {
                val wanted = launcher.defaultDockPackages()
                val dock = wanted.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg }?.key }
                    .take(GridSpec.DOCK_SLOTS)
                    .mapIndexed { i, key -> LayoutItem(key, Container.Dock, 0, i, 0) }
                VLog.i("Prefs", "Primeira execução: perfil recomendado=$recommended, dock inicial=${dock.size} apps")
                prefs.seed(recommended, dock)
            }
        }
        // O cache de ícones acompanha o perfil escolhido.
        viewModelScope.launch {
            prefs.prefs.collect { p -> icons.configure(p.profile ?: recommended) }
        }
    }

    override fun openApp(app: AppEntry, options: Bundle?) = launcher.launch(app, options)

    override fun openAppInfo(app: AppEntry) = launcher.openAppInfo(app)

    override fun uninstall(app: AppEntry) = launcher.uninstall(app)

    override fun placementOf(app: AppEntry): Placement = ui.value.placement[app.key] ?: Placement.None

    /** Toda edição passa por aqui: transação única no DataStore, sempre normalizada. */
    private fun edit(name: String, grid: GridConfig = ui.value.grid, op: (List<LayoutItem>) -> List<LayoutItem>) {
        viewModelScope.launch {
            val present = catalog.apps.value.map { it.key }.toSet()
            try {
                prefs.editLayout { current ->
                    val next = op(current)
                    // Com a lista de apps ainda vazia, não apaga nada do que foi salvo.
                    val result = if (present.isEmpty()) next else LayoutOps.sanitize(next, present, grid)
                    VLog.d("Layout", "$name: ${current.size} -> ${result.size} itens${if (result == current) " (sem mudança)" else ""}")
                    result
                }
            } catch (e: Exception) {
                VLog.e("Layout", "Falha ao salvar a edição '$name'", e)
            }
        }
    }

    override fun toggleDock(app: AppEntry) = edit("dock ${app.packageName}") {
        if (it.any { i -> i.key == app.key && i.container == Container.Dock }) LayoutOps.removeFromDock(it, app.key)
        else LayoutOps.addToDock(it, app.key) // dock cheio: nada muda
    }

    override fun addToHome(app: AppEntry) = edit("colocar ${app.packageName}") { LayoutOps.addToHome(it, app.key, ui.value.grid) }

    override fun removeFromHome(app: AppEntry) = edit("tirar ${app.packageName}") { LayoutOps.removeFromHome(it, app.key) }

    override fun moveToCell(app: AppEntry, page: Int, x: Int, y: Int) = edit("mover ${app.packageName} p$page ($x,$y)") { LayoutOps.moveHome(it, app.key, page, x, y, ui.value.grid) }

    override fun moveToDock(app: AppEntry, index: Int) = edit("dock ${app.packageName} #$index") { LayoutOps.moveToDock(it, app.key, index) }

    /** `category = null` volta a classificação automática do app. */
    override fun setCategory(app: AppEntry, category: AppCategory?) {
        viewModelScope.launch { prefs.setCategory(app.key, category?.name) }
    }

    override fun setClockStyle(style: ClockStyle) { viewModelScope.launch { prefs.setClockStyle(style) } }

    override fun setClockFormat(format: ClockFormat) { viewModelScope.launch { prefs.setClockFormat(format) } }

    override fun setShowDate(show: Boolean) { viewModelScope.launch { prefs.setShowDate(show) } }

    override fun setWallpaper(w: Wallpaper) { viewModelScope.launch { prefs.setWallpaper(w) } }

    override fun setGrid(columns: Int, rows: Int) {
        val g = GridConfig.coerced(columns, rows)
        VLog.i("Layout", "Grade alterada para ${g.columns}x${g.rows}")
        viewModelScope.launch {
            prefs.setGrid(g)
            edit("grade ${g.columns}x${g.rows}", g) { it } // reorganiza e grava o que não cabe mais
        }
    }

    override fun setIconSize(size: IconSize) { viewModelScope.launch { prefs.setIconSize(size) } }

    override fun setShowLabels(show: Boolean) { viewModelScope.launch { prefs.setShowLabels(show) } }

    override fun setAccent(accent: Accent) { viewModelScope.launch { prefs.setAccent(accent) } }

    override fun setGlass(glass: GlassLevel) { viewModelScope.launch { prefs.setGlass(glass) } }

    override fun setIconShape(shape: IconShape) { viewModelScope.launch { prefs.setIconShape(shape) } }

    override fun setIconShadow(on: Boolean) { viewModelScope.launch { prefs.setIconShadow(on) } }

    override fun setDrawerSort(sort: DrawerSort) { viewModelScope.launch { prefs.setDrawerSort(sort) } }

    override fun setHidden(app: AppEntry, hidden: Boolean) { viewModelScope.launch { prefs.setHidden(app.key, hidden) } }

    override fun openClock() = launcher.openClockApp()

    override fun openCalendar() = launcher.openCalendarApp()

    fun setProfile(profile: PerformanceProfile) {
        VLog.i("Prefs", "Perfil de desempenho alterado para $profile")
        viewModelScope.launch { prefs.setProfile(profile) }
    }

    override suspend fun backupText(): String = BackupCodec.encode(prefs.current())

    override suspend fun restoreBackup(text: String): String = when (val r = BackupCodec.decode(text)) {
        is BackupCodec.Result.Fail -> { VLog.w("Backup", "Restauração recusada: ${r.reason}"); r.reason }
        is BackupCodec.Result.Ok -> {
            prefs.restore(r.prefs)
            VLog.i("Backup", "Backup restaurado: ${r.prefs.layout.size} itens de layout, ${r.ignoredLines} linhas ignoradas")
            "Backup restaurado" + if (r.ignoredLines > 0) " (${r.ignoredLines} linhas ignoradas)" else ""
        }
    }

    fun refreshApps() = catalog.refresh(immediate = true)

    /** Resumo de uso do cache de ícones, pedido pela Activity ao sair da tela. */
    fun logIconStats() = VLog.i("Ícones", icons.stats())

    override fun onCleared() {
        VLog.i("App", "ViewModel destruído (os dados continuam no processo)")
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                LauncherViewModel(
                    container.settings, container.catalog, container.launcher, container.battery,
                    container.icons, container.recommendedProfile
                )
            }
        }
    }
}
