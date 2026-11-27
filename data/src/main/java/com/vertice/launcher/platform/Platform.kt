package com.vertice.launcher.platform

import android.os.Bundle
import com.vertice.launcher.data.AppEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tudo o que a launcher pede ao Android passa por estas interfaces.
 * O ViewModel não sabe se é o sistema de verdade ou um falso de teste.
 */

/** Lista de apps instalados (inclui perfil de trabalho), atualizada sozinha. */
interface AppCatalog {
    val apps: StateFlow<List<AppEntry>>
    fun refresh(immediate: Boolean = false)
}

/** Abrir apps e telas do sistema. */
interface AppLauncher {
    fun launch(app: AppEntry, options: Bundle?)
    fun openAppInfo(app: AppEntry)
    fun uninstall(app: AppEntry)
    fun openClockApp()
    fun openCalendarApp()

    /** Pacotes para preencher o dock na primeira execução (telefone, mensagens, navegador, câmera). */
    fun defaultDockPackages(): List<String>
}

/** Nível da bateria em %, só quando o sistema avisa que mudou (sem polling). null = desconhecido. */
interface BatteryStatus {
    val percent: Flow<Int?>
}
