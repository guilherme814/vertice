package com.vertice.launcher.core

/**
 * O que a Home precisa saber, calculado só a partir de dados (apps + preferências).
 * Função pura: não conhece Android nem Compose, então toda a regra é testável na JVM.
 * A camada de cima só troca as chaves por apps de verdade.
 */
data class HomeState(
    /** Chaves do dock, da esquerda para a direita. */
    val dock: List<String>,
    /** Itens da grade da Home (já normalizados, sem apps que não existem mais). */
    val grid: List<LayoutItem>,
    /** Pastas vivas (2 apps ou mais). Seus itens de grade têm a chave [Folder.itemKey]. */
    val folders: List<Folder>,
    /** Quantas páginas a Home tem (no mínimo 1). */
    val pageCount: Int,
    val placement: Map<String, Placement>,
    /** Categoria final de cada app (manual vence a automática). */
    val categories: Map<String, AppCategory>,
    /** Chaves com categoria definida pelo usuário. */
    val manualCategories: Set<String>
) {
    companion object {
        /** [auto] permite à camada de cima memorizar a categoria automática (ela só depende do app). */
        fun from(
            apps: List<AppModel>,
            prefs: Prefs,
            auto: (AppModel) -> AppCategory = { it.autoCategory }
        ): HomeState {
            // Apps desinstalados somem sozinhos do layout; a chave salva é ignorada.
            val data = FolderOps.sanitize(HomeData(prefs.layout, prefs.folders), apps.map { it.key }.toSet(), prefs.grid)
            val layout = data.items
            val dock = layout.filter { it.container == Container.Dock }.sortedBy { it.x }.map { it.key }
            val grid = layout.filter { it.container == Container.Home }
            return HomeState(
                dock = dock,
                grid = grid,
                folders = data.folders,
                pageCount = (grid.maxOfOrNull { it.page } ?: 0) + 1,
                placement = layout.filterNot { FolderOps.isFolderKey(it.key) }
                    .associate { it.key to if (it.container == Container.Dock) Placement.Dock else Placement.Home } +
                    data.folders.flatMap { f -> f.keys.map { it to Placement.Folder } },
                categories = apps.associate { app ->
                    app.key to (AppCategory.fromName(prefs.categoryOverrides[app.key]) ?: auto(app))
                },
                manualCategories = prefs.categoryOverrides.keys
            )
        }
    }
}
