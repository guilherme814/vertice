package com.vertice.launcher.core

/** Uma "mancha" de luz do papel de parede: posição e raio em fração da tela. */
data class Blob(val color: Long, val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * Papéis de parede próprios, desenhados em código (gradientes em malha).
 * Zero imagens no APK, nítidos em qualquer resolução e desenhados uma única vez
 * (sem animação) — custo de bateria praticamente nulo.
 *
 * [System] não desenha nada: mostra o papel de parede do aparelho.
 */
enum class Wallpaper(val label: String, val base: Long, val blobs: List<Blob>) {
    System("Do aparelho", 0xFF090D14, emptyList()),

    Vertice(
        "Vértice", 0xFF090D14,
        listOf(
            Blob(0xFFB8F28B, 0.55f, 0.08f, 0.65f, 0.30f),
            Blob(0xFF1B3A5C, 0.15f, 0.80f, 0.90f, 0.75f),
            Blob(0xFF2B6CB0, 0.90f, 0.55f, 0.60f, 0.30f)
        )
    ),
    Aurora(
        "Aurora", 0xFF07121C,
        listOf(
            Blob(0xFF2DE2B1, 0.20f, 0.15f, 0.70f, 0.55f),
            Blob(0xFF7B5CFF, 0.85f, 0.35f, 0.60f, 0.50f),
            Blob(0xFF1E90FF, 0.30f, 0.85f, 0.80f, 0.40f)
        )
    ),
    Dusk(
        "Entardecer", 0xFF140A1F,
        listOf(
            Blob(0xFFFF7A3D, 0.80f, 0.10f, 0.70f, 0.55f),
            Blob(0xFFE5348F, 0.15f, 0.50f, 0.60f, 0.50f),
            Blob(0xFF5B2A9E, 0.70f, 0.90f, 0.80f, 0.50f)
        )
    ),
    Ocean(
        "Oceano", 0xFF03121F,
        listOf(
            Blob(0xFF00C2FF, 0.20f, 0.20f, 0.70f, 0.45f),
            Blob(0xFF0A4DFF, 0.80f, 0.60f, 0.80f, 0.50f),
            Blob(0xFF00E0C6, 0.40f, 0.95f, 0.60f, 0.30f)
        )
    ),
    Ember(
        "Brasa", 0xFF120806,
        listOf(
            Blob(0xFFFF3B1F, 0.30f, 0.90f, 0.80f, 0.50f),
            Blob(0xFFFFB02E, 0.85f, 0.20f, 0.55f, 0.40f),
            Blob(0xFF8A1030, 0.10f, 0.30f, 0.60f, 0.50f)
        )
    ),
    Forest(
        "Floresta", 0xFF07130C,
        listOf(
            Blob(0xFF3DDC84, 0.20f, 0.25f, 0.70f, 0.40f),
            Blob(0xFFB8F28B, 0.90f, 0.55f, 0.50f, 0.28f),
            Blob(0xFF0F7A66, 0.40f, 0.90f, 0.80f, 0.50f)
        )
    ),
    Sakura(
        "Sakura", 0xFF1A0B14,
        listOf(
            Blob(0xFFFF8FB8, 0.80f, 0.20f, 0.65f, 0.40f),
            Blob(0xFFFFB38A, 0.20f, 0.70f, 0.60f, 0.30f),
            Blob(0xFF6B2A66, 0.70f, 0.95f, 0.80f, 0.50f)
        )
    ),
    Nebula(
        "Nebulosa", 0xFF060614,
        listOf(
            Blob(0xFF4F46E5, 0.25f, 0.30f, 0.80f, 0.55f),
            Blob(0xFF22D3EE, 0.85f, 0.15f, 0.45f, 0.35f),
            Blob(0xFFC026D3, 0.60f, 0.85f, 0.70f, 0.40f)
        )
    ),
    Graphite(
        "Grafite", 0xFF0B0D10,
        listOf(
            Blob(0xFF3A4452, 0.20f, 0.20f, 0.80f, 0.50f),
            Blob(0xFF1F6FEB, 0.90f, 0.80f, 0.50f, 0.20f)
        )
    );

    val isSystem: Boolean get() = this == System

    companion object {
        fun fromName(name: String?): Wallpaper = entries.firstOrNull { it.name == name } ?: System
    }
}
