package com.vertice.launcher.core

/**
 * Um app, como o domínio o enxerga: só dados, sem nenhuma alça do sistema.
 * [key] identifica de forma estável (componente + perfil de trabalho) e é o que fica salvo no layout.
 */
data class AppModel(
    val key: String,
    val packageName: String,
    val label: String,
    val installTime: Long,
    /** Categoria declarada pelo app (ApplicationInfo.category) ou -1. */
    val declaredCategory: Int
) {
    /** Categoria automática (declarada pelo app ou deduzida do pacote). */
    val autoCategory: AppCategory get() = AppCategory.detect(declaredCategory, packageName)
}
