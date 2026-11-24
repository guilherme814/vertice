package com.vertice.launcher.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Tudo o que a launcher guarda. Tudo local: sem conta, sem rede. */
data class Prefs(
    /** null = ainda não escolhido (a primeira execução decide pelo hardware). */
    val profile: PerformanceProfile? = null,
    /** Posição de cada app na Home e no dock (contêiner + página + célula). */
    val layout: List<LayoutItem> = emptyList(),
    val seeded: Boolean = false,
    /** Correções manuais: chave do app -> nome da categoria. Vencem a classificação automática. */
    val categoryOverrides: Map<String, String> = emptyMap(),
    val clockStyle: ClockStyle = ClockStyle.Classic,
    val clockFormat: ClockFormat = ClockFormat.System,
    val showDate: Boolean = true,
    val wallpaper: Wallpaper = Wallpaper.System
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
    suspend fun setShowDate(show: Boolean)
    suspend fun setWallpaper(w: Wallpaper)

    /** Primeira execução: grava perfil e layout iniciais e marca como semeado. */
    suspend fun seed(profile: PerformanceProfile, layout: List<LayoutItem>)
}
