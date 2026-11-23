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
    val user: UserHandle,
    val info: LauncherActivityInfo
) {
    val packageName: String get() = component.packageName
}
