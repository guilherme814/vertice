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

/** Aparência do dock: vidro de ponta a ponta, vidro compacto (mais estreito) ou sem fundo. */
enum class DockStyle(val label: String) {
    Glass("Vidro"),
    Compact("Compacto"),
    Flat("Sem fundo");

    companion object {
        fun fromName(name: String?): DockStyle = entries.firstOrNull { it.name == name } ?: Glass
    }
}

/** Opacidade do dock em % do vidro escolhido (100 = como o nível de vidro define). */
object DockOpacity {
    const val MIN = 20
    const val MAX = 100
    const val DEFAULT = 100

    fun coerce(percent: Int) = percent.coerceIn(MIN, MAX)

    /** Opacidade final do corpo do vidro dado o [base] do nível de vidro. */
    fun alpha(base: Float, percent: Int): Float = (base * coerce(percent) / 100f).coerceIn(0f, 1f)
}

/** Tamanho do texto dos rótulos de app. [sp] é o corpo; [lineSp] a altura da linha. */
enum class LabelSize(val label: String, val sp: Int, val lineSp: Int) {
    Small("Pequeno", 10, 12),
    Medium("Médio", 11, 13),
    Large("Grande", 13, 15);

    companion object {
        fun fromName(name: String?): LabelSize = entries.firstOrNull { it.name == name } ?: Medium
    }
}

/** Indicador de páginas da Home. */
enum class IndicatorStyle(val label: String) {
    Pill("Pílula"),
    Dots("Pontos"),
    Bar("Barra"),
    Hidden("Oculto");

    companion object {
        fun fromName(name: String?): IndicatorStyle = entries.firstOrNull { it.name == name } ?: Pill
    }
}
