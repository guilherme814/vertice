package com.vertice.launcher.core

import java.net.URLDecoder
import java.net.URLEncoder

/** Onde um app está: na grade da Home, no dock, ou só na gaveta. */
enum class Placement { None, Home, Dock }

enum class Container(val code: String) {
    Home("H"), Dock("D");

    companion object {
        fun fromCode(code: String): Container? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Um app posicionado. Mesma ideia do `ItemInfo` do Launcher3: contêiner + página + célula.
 * No dock, `x` é a ordem (0 = primeiro) e `page`/`y` valem 0.
 */
data class LayoutItem(val key: String, val container: Container, val page: Int, val x: Int, val y: Int)

/** Padrões da grade (a configuração do usuário vem de [GridConfig]). */
object GridSpec {
    const val COLUMNS = 4
    const val ROWS = 5
    const val DOCK_SLOTS = 5
    const val MIN_COLUMNS = 3
    const val MAX_COLUMNS = 6
    const val MIN_ROWS = 4
    const val MAX_ROWS = 7
}

/** Tamanho da grade da Home escolhido pelo usuário. Sempre dentro dos limites (valores fora são ajustados). */
data class GridConfig(val columns: Int = GridSpec.COLUMNS, val rows: Int = GridSpec.ROWS) {
    init {
        require(columns in GridSpec.MIN_COLUMNS..GridSpec.MAX_COLUMNS) { "colunas fora do limite: $columns" }
        require(rows in GridSpec.MIN_ROWS..GridSpec.MAX_ROWS) { "linhas fora do limite: $rows" }
    }

    companion object {
        val Default = GridConfig()
        fun coerced(columns: Int, rows: Int) = GridConfig(
            columns.coerceIn(GridSpec.MIN_COLUMNS, GridSpec.MAX_COLUMNS),
            rows.coerceIn(GridSpec.MIN_ROWS, GridSpec.MAX_ROWS)
        )
    }
}

/**
 * Operações sobre o layout. Tudo é função pura (sem Android), então roda em teste de JVM.
 *
 * Regras: um app aparece no máximo uma vez (Home OU dock); posições nunca se repetem;
 * remover um app NÃO desloca os outros (a grade guarda coordenadas, não uma lista ordenada).
 */
object LayoutOps {

    private data class Cell(val page: Int, val x: Int, val y: Int)

    private fun firstFree(occupied: Set<Cell>, grid: GridConfig): Cell {
        var page = 0
        while (true) {
            for (y in 0 until grid.rows) for (x in 0 until grid.columns) {
                val c = Cell(page, x, y)
                if (c !in occupied) return c
            }
            page++
        }
    }

    private fun homeCells(items: List<LayoutItem>) =
        items.filter { it.container == Container.Home }.map { Cell(it.page, it.x, it.y) }.toSet()

    /**
     * Deixa o layout consistente: tira apps que não existem mais e chaves repetidas, compacta o dock
     * (0..n-1), manda o excesso do dock para a Home, recoloca itens fora da grade ou em conflito na
     * primeira célula livre e remove páginas vazias. Idempotente.
     */
    fun sanitize(items: List<LayoutItem>, present: Set<String>, grid: GridConfig = GridConfig.Default): List<LayoutItem> {
        val ordered = items.filter { it.key in present }
            .sortedWith(compareBy({ it.container.ordinal }, { it.page }, { it.y }, { it.x }, { it.key }))
        val seen = HashSet<String>()
        val unique = ordered.filter { seen.add(it.key) }

        val dockRaw = unique.filter { it.container == Container.Dock }
        val dock = dockRaw.take(GridSpec.DOCK_SLOTS)
            .mapIndexed { i, it -> it.copy(page = 0, x = i, y = 0) }
        val overflow = dockRaw.drop(GridSpec.DOCK_SLOTS)
            .map { LayoutItem(it.key, Container.Home, -1, -1, -1) }

        val occupied = HashSet<Cell>()
        val placed = ArrayList<LayoutItem>()
        val pending = ArrayList<LayoutItem>()
        for (it in unique.filter { it.container == Container.Home } + overflow) {
            val valid = it.page >= 0 && it.x in 0 until grid.columns && it.y in 0 until grid.rows
            if (valid && occupied.add(Cell(it.page, it.x, it.y))) placed += it else pending += it
        }
        for (it in pending) {
            val c = firstFree(occupied, grid)
            occupied += c
            placed += LayoutItem(it.key, Container.Home, c.page, c.x, c.y)
        }

        val pageIndex = placed.map { it.page }.distinct().sorted().withIndex().associate { (i, p) -> p to i }
        val home = placed.map { it.copy(page = pageIndex.getValue(it.page)) }
        // Ordem canônica: o resultado não depende da ordem de entrada (e sanitize fica idempotente).
        return dock + home.sortedWith(compareBy({ it.page }, { it.y }, { it.x }))
    }

    /** Coloca na primeira célula livre da Home (tira o app do dock, se estiver lá). */
    fun addToHome(items: List<LayoutItem>, key: String, grid: GridConfig = GridConfig.Default): List<LayoutItem> {
        val rest = items.filter { it.key != key }
        val c = firstFree(homeCells(rest), grid)
        return rest + LayoutItem(key, Container.Home, c.page, c.x, c.y)
    }

    fun removeFromHome(items: List<LayoutItem>, key: String): List<LayoutItem> =
        items.filterNot { it.key == key && it.container == Container.Home }

    /** Move para o fim do dock. Se estiver cheio, devolve o layout igual. */
    fun addToDock(items: List<LayoutItem>, key: String): List<LayoutItem> {
        if (items.any { it.key == key && it.container == Container.Dock }) return items
        val dockCount = items.count { it.container == Container.Dock }
        if (dockCount >= GridSpec.DOCK_SLOTS) return items
        return items.filter { it.key != key } + LayoutItem(key, Container.Dock, 0, dockCount, 0)
    }

    fun removeFromDock(items: List<LayoutItem>, key: String): List<LayoutItem> =
        items.filterNot { it.key == key && it.container == Container.Dock }

    /**
     * Move para a célula indicada. Se estiver ocupada por outro app da Home: os dois trocam de lugar.
     * Se o app vinha do dock ou da gaveta, o ocupante vai para a primeira célula livre.
     * Posição fora da grade: não faz nada.
     */
    fun moveHome(
        items: List<LayoutItem>, key: String, page: Int, x: Int, y: Int, grid: GridConfig = GridConfig.Default
    ): List<LayoutItem> {
        if (page < 0 || x !in 0 until grid.columns || y !in 0 until grid.rows) return items
        val me = items.firstOrNull { it.key == key }
        val target = items.firstOrNull {
            it.container == Container.Home && it.page == page && it.x == x && it.y == y
        }
        if (target?.key == key) return items

        val rest = items.filter { it.key != key && it.key != target?.key }
        val moved = LayoutItem(key, Container.Home, page, x, y)
        if (target == null) return rest + moved

        return if (me != null && me.container == Container.Home) {
            rest + moved + target.copy(page = me.page, x = me.x, y = me.y)       // troca
        } else {
            addToHome(rest + moved, target.key, grid)                                  // ocupante é realocado
        }
    }
}

/** Texto <-> layout. Chaves são codificadas para poderem conter qualquer caractere. */
object LayoutCodec {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(item: LayoutItem) = "${enc(item.key)}|${item.container.code}|${item.page}|${item.x}|${item.y}"

    /** Linha inválida vira null (é ignorada, não derruba o app). */
    fun decode(line: String): LayoutItem? {
        val p = line.split('|')
        if (p.size != 5) return null
        val container = Container.fromCode(p[1]) ?: return null
        val page = p[2].toIntOrNull() ?: return null
        val x = p[3].toIntOrNull() ?: return null
        val y = p[4].toIntOrNull() ?: return null
        return try {
            LayoutItem(dec(p[0]), container, page, x, y)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun encodeAll(items: List<LayoutItem>): Set<String> = items.map(::encode).toSet()

    fun decodeAll(lines: Set<String>): List<LayoutItem> = lines.mapNotNull(::decode)
}
