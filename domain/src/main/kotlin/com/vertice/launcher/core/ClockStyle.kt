package com.vertice.launcher.core

enum class ClockStyle(val label: String) {
    Classic("Clássico"),
    Stacked("Empilhado"),
    Minimal("Compacto"),
    Analog("Analógico");

    companion object {
        fun fromName(name: String?): ClockStyle = entries.firstOrNull { it.name == name } ?: Classic
    }
}

enum class ClockFormat(val label: String) {
    System("Sistema"),
    H24("24 horas"),
    H12("12 horas");

    companion object {
        fun fromName(name: String?): ClockFormat = entries.firstOrNull { it.name == name } ?: System
    }
}

/** Cor do relógio: branco ou a cor de destaque do tema. */
enum class ClockColor(val label: String) {
    White("Branco"),
    Accent("Destaque");

    companion object {
        fun fromName(name: String?): ClockColor = entries.firstOrNull { it.name == name } ?: White
    }
}

/** Tamanho do relógio, como fator sobre o tamanho do estilo escolhido. */
enum class ClockScale(val label: String, val factor: Float) {
    Small("Pequeno", 0.8f),
    Medium("Médio", 1f),
    Large("Grande", 1.2f);

    companion object {
        fun fromName(name: String?): ClockScale = entries.firstOrNull { it.name == name } ?: Medium
    }
}

/** Alinhamento horizontal do relógio na Home. */
enum class ClockAlign(val label: String) {
    Start("Esquerda"),
    Center("Centro"),
    End("Direita");

    companion object {
        fun fromName(name: String?): ClockAlign = entries.firstOrNull { it.name == name } ?: Start
    }
}
