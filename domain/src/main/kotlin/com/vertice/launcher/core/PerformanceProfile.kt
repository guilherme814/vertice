package com.vertice.launcher.core

/**
 * Perfis de desempenho (roteiro da Vértice: Ultra Lite / Equilibrado / Premium).
 *
 * Fase 1: só os parâmetros que já importam (animações, cache de ícones, resolução máxima).
 * Fases futuras penduram aqui blur, efeitos de vidro, taxa de atualização etc.,
 * sempre lendo do perfil — nenhuma tela decide sozinha se "pode" gastar GPU.
 */
enum class PerformanceProfile(
    val label: String,
    val animations: Boolean,
    val iconCacheMb: Int,
    val iconMaxPx: Int
) {
    UltraLite("Ultra Lite", animations = false, iconCacheMb = 6, iconMaxPx = 96),
    Balanced("Equilibrado", animations = true, iconCacheMb = 16, iconMaxPx = 144),
    Premium("Premium", animations = true, iconCacheMb = 32, iconMaxPx = 192);

    companion object {
        /** Sugestão inicial: aparelhos de pouca RAM começam no Ultra Lite. (Quem sabe se é pouca RAM é o :data.) */
        fun recommended(lowRam: Boolean): PerformanceProfile = if (lowRam) UltraLite else Balanced

        fun fromName(name: String?): PerformanceProfile? = entries.firstOrNull { it.name == name }
    }
}
