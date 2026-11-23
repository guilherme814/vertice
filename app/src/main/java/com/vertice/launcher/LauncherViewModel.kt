package com.vertice.launcher

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vertice.launcher.core.AppCategory
import com.vertice.launcher.core.ClockFormat
import com.vertice.launcher.core.ClockStyle
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

/** Estado já resolvido para a interface: chaves salvas viram apps reais. */
data class HomeUi(
    val loading: Boolean = true,
    val profile: PerformanceProfile = PerformanceProfile.Balanced,
    val apps: List<AppEntry> = emptyList(),
    val dock: List<AppEntry> = emptyList(),
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
        HomeUi(
            loading = !p.seeded,
            profile = profile,
            apps = apps,
            // Apps desinstalados somem do dock sozinhos; a chave salva é ignorada.
            dock = p.dock.mapNotNull { byKey[it] },
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
                prefs.seed(recommended, dock.take(MAX_DOCK))
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

    fun isInDock(app: AppEntry): Boolean = ui.value.dock.any { it.key == app.key }

    fun toggleDock(app: AppEntry) {
        viewModelScope.launch {
            val current = ui.value.dock.map { it.key }
            val next = when {
                app.key in current -> current - app.key
                current.size < MAX_DOCK -> current + app.key
                else -> current // dock cheio: o limite vira configuração numa fase futura
            }
            prefs.setDock(next)
        }
    }

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

    companion object {
        const val MAX_DOCK = 5
    }
}
