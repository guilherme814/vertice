package com.vertice.launcher.core

/** Tamanho dos ícones da Home e da gaveta. [scale] multiplica o tamanho-base de cada lugar. */
enum class IconSize(val label: String, val scale: Float) {
    Small("Pequeno", 0.85f),
    Medium("Médio", 1f),
    Large("Grande", 1.2f);

    companion object {
        fun fromName(name: String?): IconSize = entries.firstOrNull { it.name == name } ?: Medium
    }
}
