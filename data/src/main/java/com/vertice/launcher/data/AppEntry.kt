package com.vertice.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/**
 * Um app lançável. [key] identifica de forma estável (componente + perfil de trabalho)
 * e é o que fica salvo no dock — nunca o rótulo, que muda com o idioma.
 */
class AppEntry(
    val key: String,
    val component: ComponentName,
    val label: String,
    val installTime: Long,
    /** Categoria declarada pelo app (ApplicationInfo.category) ou -1. */
    val declaredCategory: Int,
    val user: UserHandle,
    val info: LauncherActivityInfo
) {
    val packageName: String get() = component.packageName

    // Igualdade pelo conteúdo (o `info` é só a alça do sistema). Sem isso, cada recarga da lista criaria
    // objetos "diferentes" e a tela inteira seria redesenhada à toa.
    override fun equals(other: Any?): Boolean =
        other is AppEntry && key == other.key && label == other.label &&
            installTime == other.installTime && declaredCategory == other.declaredCategory

    override fun hashCode(): Int = key.hashCode() * 31 + label.hashCode()
}
