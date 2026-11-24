package com.vertice.launcher

import android.app.Application
import com.vertice.launcher.core.PerformanceProfile
import com.vertice.launcher.core.SettingsStore
import com.vertice.launcher.data.AppRepository
import com.vertice.launcher.data.DevicePerformance
import com.vertice.launcher.data.IconCache
import com.vertice.launcher.data.LauncherPrefs

/**
 * Montagem manual das dependências (sem framework de injeção: a launcher é pequena e precisa abrir rápido).
 * Um só lugar decide quem implementa cada contrato; nos testes, outro container entra no lugar.
 * Itens pesados são `lazy`: só nascem quando alguém usa.
 */
class AppContainer(private val app: Application) {
    val settings: SettingsStore by lazy { LauncherPrefs(app) }
    val recommendedProfile: PerformanceProfile by lazy { DevicePerformance.recommended(app) }
    val apps: AppRepository by lazy { AppRepository(app) }
    val icons: IconCache by lazy { IconCache(app, recommendedProfile) }
}
