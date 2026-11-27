package com.vertice.launcher.core

/** Cor de destaque. [System] usa a cor do papel de parede do Android 12+ (no resto, cai na Menta). */
enum class Accent(val label: String, val argb: Long) {
    Mint("Menta", 0xFFB8F28B),
    Sky("Céu", 0xFF7CC4FF),
    Violet("Violeta", 0xFFB69CFF),
    Rose("Rosa", 0xFFFF8FB1),
    Amber("Âmbar", 0xFFFFC857),
    Coral("Coral", 0xFFFF8A6B),
    Aqua("Água", 0xFF5EE6D8),
    Snow("Neve", 0xFFF3F6FA),
    System("Cor do sistema", 0xFFB8F28B);

    companion object {
        fun fromName(name: String?): Accent = entries.firstOrNull { it.name == name } ?: Mint
    }
}

/** Intensidade dos painéis translúcidos (dock, botões). Mais forte = mais opaco e mais legível. */
enum class GlassLevel(val label: String, val panelAlpha: Float) {
    Light("Leve", 0.35f),
    Medium("Média", 0.55f),
    Strong("Forte", 0.8f);

    /** Botões em pílula ficam um pouco mais opacos que o painel. */
    val pillAlpha: Float get() = (panelAlpha + 0.15f).coerceAtMost(0.95f)

    companion object {
        fun fromName(name: String?): GlassLevel = entries.firstOrNull { it.name == name } ?: Medium
    }
}
