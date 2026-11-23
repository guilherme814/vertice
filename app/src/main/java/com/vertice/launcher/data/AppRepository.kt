package com.vertice.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator

/**
 * Lista os apps lançáveis (inclui perfil de trabalho) e se mantém atualizada quando
 * algo é instalado, removido ou atualizado — sem polling.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private var refreshJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh(immediate = true)
    }

    /** Recarrega (com debounce, porque instalar um app dispara vários eventos seguidos). */
    fun refresh(immediate: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            if (!immediate) delay(350)
            _apps.value = load()
        }
    }

    private fun load(): List<AppEntry> {
        val result = ArrayList<AppEntry>()
        val me = Process.myUserHandle()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val suffix = if (user == me) "" else "#$serial"
            for (info in launcherApps.getActivityList(null, user)) {
                val component = info.componentName
                if (component.packageName == appContext.packageName) continue
                result += AppEntry(
                    key = component.flattenToString() + suffix,
                    component = component,
                    label = info.label?.toString() ?: component.packageName,
                    installTime = info.firstInstallTime,
                    declaredCategory = info.applicationInfo.category,
                    user = user,
                    info = info
                )
            }
        }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return result.sortedWith(compareBy(collator) { it.label })
    }

    fun launch(app: AppEntry, options: Bundle?) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, options)
        } catch (_: Exception) {
            // App removido no meio do caminho ou perfil indisponível.
            refresh(immediate = true)
        }
    }

    fun openAppInfo(app: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (_: Exception) {
        }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /** Apps padrão para preencher o dock na primeira execução (telefone, mensagens, navegador, câmera). */
    fun defaultDockPackages(): List<String> {
        val pm = appContext.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        )
        return intents.mapNotNull { pm.resolveActivity(it, 0)?.activityInfo?.packageName }
            .filter { it != "android" }
            .distinct()
    }

    fun close() {
        launcherApps.unregisterCallback(callback)
        scope.cancel()
    }
}
