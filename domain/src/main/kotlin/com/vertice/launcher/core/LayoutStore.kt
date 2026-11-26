package com.vertice.launcher.core

/**
 * Onde o layout da Home é guardado. O domínio só conhece este contrato;
 * hoje quem cumpre é o DataStore (módulo :data), amanhã pode ser outro.
 */
interface LayoutStore {
    /** Lê, transforma e grava numa operação atômica (duas edições seguidas nunca se sobrescrevem). */
    suspend fun editHome(transform: (HomeData) -> HomeData)

    /** Atalho para quem só mexe nas posições. */
    suspend fun editLayout(transform: (List<LayoutItem>) -> List<LayoutItem>) =
        editHome { it.copy(items = transform(it.items)) }
}
