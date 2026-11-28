package com.vertice.launcher

import android.app.Application
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.SettingsStore
import com.vertice.launcher.core.island.IslandStore
import com.vertice.launcher.data.IslandPrefs
import com.vertice.launcher.data.AppRepository
import com.vertice.launcher.data.DevicePerformance
import com.vertice.launcher.data.IconCache
import com.vertice.launcher.data.LauncherPrefs
import com.vertice.launcher.platform.AppCatalog
import com.vertice.launcher.platform.AppLauncher
import com.vertice.launcher.platform.BatteryMonitor
import com.vertice.launcher.platform.BatteryStatus

/**
 * Montagem manual das dependências (sem framework de injeção: a launcher é pequena e precisa abrir rápido).
 * Um só lugar decide quem implementa cada contrato; nos testes, outro container entra no lugar.
 * Itens pesados são `lazy`: só nascem quando alguém usa.
 */
class AppContainer(private val app: Application) {
    val settings: SettingsStore by lazy { LauncherPrefs(app) }
    val recommendedProfile: PerformanceProfile by lazy { DevicePerformance.recommended(app) }
    private val appRepository: AppRepository by lazy { AppRepository(app) }
    val catalog: AppCatalog get() = appRepository
    val launcher: AppLauncher get() = appRepository
    val battery: BatteryStatus by lazy { BatteryMonitor(app) }
    val icons: IconCache by lazy { IconCache(app, recommendedProfile) }
    val islandStore: IslandStore by lazy { IslandPrefs(app) }
}
