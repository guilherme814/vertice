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

/** Forma do ícone. [percent] = arredondamento do canto em % (50 = círculo); null = como o sistema entrega. */
enum class IconShape(val label: String, val percent: Int?) {
    Original("Original", null),
    Circle("Círculo", 50),
    Squircle("Squircle", 34),
    Rounded("Arredondado", 22),
    Square("Quadrado", 8);

    /** Formas próprias exigem o ícone "sem máscara" (camadas do ícone adaptativo inteiras). */
    val needsFullBleed: Boolean get() = percent != null

    companion object {
        fun fromName(name: String?): IconShape = entries.firstOrNull { it.name == name } ?: Original
    }
}

/** Estilo dos ícones em toda a launcher (Home, dock, gaveta). */
data class IconStyle(val shape: IconShape = IconShape.Original, val shadow: Boolean = false)
