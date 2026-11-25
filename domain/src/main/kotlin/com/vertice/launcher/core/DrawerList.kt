package com.vertice.launcher.core

import java.text.Normalizer

/** Ordem da gaveta. "Novos" = instalados mais recentemente primeiro. */
enum class DrawerSort(val label: String) {
    AZ("A–Z"),
    Newest("Novos");

    companion object {
        fun fromName(name: String?): DrawerSort = entries.firstOrNull { it.name == name } ?: AZ
    }
}

/** Grupo da gaveta: uma letra (ou '#') e seus apps. */
data class DrawerSection(val letter: Char, val items: List<AppModel>)

/** Texto sem acento e em minúsculas: base da busca e da ordenação. */
fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Regras da gaveta, puras: ordenar, agrupar por letra, esconder. */
object DrawerList {
    const val OTHER = '#'

    fun letterOf(label: String): Char {
        val c = label.trim().foldForSearch().firstOrNull() ?: return OTHER
        return if (c in 'a'..'z') c.uppercaseChar() else OTHER
    }

    /** Apps visíveis (ocultos fora) na ordem pedida. Desempate estável pelo nome. */
    fun sorted(apps: List<AppModel>, sort: DrawerSort, hidden: Set<String>): List<AppModel> {
        val visible = apps.filter { it.key !in hidden }
        val byName = compareBy<AppModel>({ letterOf(it.label) == OTHER }, { it.label.foldForSearch() }, { it.key })
        return when (sort) {
            DrawerSort.AZ -> visible.sortedWith(byName)
            DrawerSort.Newest -> visible.sortedWith(compareByDescending<AppModel> { it.installTime }.then(byName))
        }
    }

    /** Seções por letra, na ordem A..Z e '#' por último; mantém a ordem recebida dentro de cada uma. */
    fun sections(sortedApps: List<AppModel>): List<DrawerSection> =
        sortedApps.groupBy { letterOf(it.label) }
            .toSortedMap(compareBy<Char> { it == OTHER }.thenBy { it })
            .map { (l, items) -> DrawerSection(l, items) }
}
