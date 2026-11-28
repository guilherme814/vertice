package com.vertice.launcher

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Bundle
import com.vertice.launcher.diag.CrashReporter
import com.vertice.launcher.island.IslandRuntime
import com.vertice.launcher.diag.ExitReasons
import com.vertice.launcher.diag.FrameMonitor
import com.vertice.launcher.diag.StrictModeBridge
import com.vertice.launcher.diag.SystemSnapshot
import com.vertice.launcher.diag.VLog
import com.vertice.launcher.diag.Watchdog

/**
 * Liga o diagnóstico antes de qualquer outra coisa: assim até um crash na abertura fica registrado.
 */
class VerticeApp : Application() {

    /** Dependências do processo (criadas uma vez, compartilhadas por todas as telas). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // A ilha precisa estar pronta antes de o sistema ligar o serviço de notificações (mesmo processo).
        IslandRuntime.install(this, container.islandStore)
        VLog.init(this)
        CrashReporter.install(this)
        StrictModeBridge.install()
        VLog.i("App", "Application.onCreate (${SystemSnapshot.sinceProcessStartMs()}ms desde o início do processo)")
        VLog.i("Sistema", SystemSnapshot.header(this))
        VLog.i("Memória", SystemSnapshot.memoryLine(this))
        // Fora da thread principal: ler o histórico de saídas é E/S.
        Thread({ ExitReasons.logNew(this) }, "vertice-exit-reasons").apply { isDaemon = true; start() }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun log(a: Activity, what: String) =
                VLog.i("Ciclo", "${a.javaClass.simpleName}: $what (+${SystemSnapshot.sinceProcessStartMs()}ms)")

            override fun onActivityCreated(a: Activity, s: Bundle?) = log(a, "onCreate estadoSalvo=${s != null}")
            override fun onActivityStarted(a: Activity) = log(a, "onStart")
            override fun onActivityResumed(a: Activity) {
                log(a, "onResume")
                VLog.d("Memória", SystemSnapshot.memoryLine(this@VerticeApp))
                FrameMonitor.attach(a.window)
                Watchdog.start()
            }
            override fun onActivityPaused(a: Activity) {
                log(a, "onPause")
                Watchdog.stop()
                FrameMonitor.detach()
            }
            override fun onActivityStopped(a: Activity) = log(a, "onStop")
            override fun onActivitySaveInstanceState(a: Activity, s: Bundle) {}
            override fun onActivityDestroyed(a: Activity) = log(a, "onDestroy (finalizando=${a.isFinishing})")
        })
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val name = when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE (processo é o próximo a morrer)"
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            else -> "RUNNING_MODERATE"
        }
        VLog.w("Memória", "Sistema pede para liberar memória: nível $level $name | ${SystemSnapshot.memoryLine(this)}")
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        VLog.w("Memória", "onLowMemory | ${SystemSnapshot.memoryLine(this)}")
    }
}
