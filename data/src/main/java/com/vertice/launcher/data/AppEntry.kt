package com.vertice.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import com.vertice.launcher.core.AppModel

/**
 * Um app lançável: o [model] (dados puros, do domínio) mais as alças do sistema para abri-lo
 * e desenhar seu ícone. O que fica salvo no layout é sempre a chave do modelo, nunca o rótulo.
 */
class AppEntry(
    val model: AppModel,
    val component: ComponentName,
    val user: UserHandle,
    val info: LauncherActivityInfo
) {
    val key: String get() = model.key
    val label: String get() = model.label
    val packageName: String get() = model.packageName
    val installTime: Long get() = model.installTime
    val declaredCategory: Int get() = model.declaredCategory

    // Igualdade pelo conteúdo (o `info` é só a alça do sistema). Sem isso, cada recarga da lista criaria
    // objetos "diferentes" e a tela inteira seria redesenhada à toa.
    override fun equals(other: Any?): Boolean =
        other is AppEntry && model == other.model && component == other.component

    override fun hashCode(): Int = model.hashCode() * 31 + component.hashCode()
}
