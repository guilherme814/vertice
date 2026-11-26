package com.vertice.launcher.core

/** Contas do arrastar-e-soltar, puras (a interface só converte pixels e chama estas funções). */
object DragGeometry {
    data class Cell(val x: Int, val y: Int)

    /** Célula sob o ponto ([px],[py] relativos ao canto da área de [width] x [height]); null se fora da área. */
    fun cellAt(px: Float, py: Float, width: Float, height: Float, grid: GridConfig): Cell? {
        if (width <= 0f || height <= 0f || px < 0f || py < 0f || px >= width || py >= height) return null
        return Cell(
            (px / width * grid.columns).toInt().coerceIn(0, grid.columns - 1),
            (py / height * grid.rows).toInt().coerceIn(0, grid.rows - 1)
        )
    }

    /** Posição de inserção no dock: quantos ícones (pelos centros) estão à esquerda do ponto. */
    fun dockIndex(centers: List<Float>, px: Float): Int = centers.count { it < px }

    /** -1 perto da borda esquerda, +1 perto da direita, 0 no meio (para virar de página ao arrastar). */
    fun edgeAt(px: Float, width: Float, margin: Float): Int = when {
        width <= 0f -> 0
        px < margin -> -1
        px > width - margin -> 1
        else -> 0
    }
}
