package com.vertice.launcher.diag

import android.os.Build
import android.os.StrictMode
import java.util.concurrent.Executors

/**
 * Liga o StrictMode só para OBSERVAR: trabalho de disco/rede na thread principal e vazamentos
 * viram linhas no log (sem derrubar o app). Cada ponto de origem aparece uma única vez.
 */
object StrictModeBridge {
    private val seen = HashSet<Int>()

    fun install() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "vertice-strict").apply { isDaemon = true } }
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().detectNetwork().detectCustomSlowCalls()
                    .penaltyListener(exec) { v -> report("Thread principal", v) }
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects().detectActivityLeaks().detectLeakedRegistrationObjects()
                    .penaltyListener(exec) { v -> report("Vazamento", v) }
                    .build()
            )
        } catch (t: Throwable) {
            VLog.w("StrictMode", "Indisponível", t)
        }
    }

    private fun report(kind: String, v: Throwable) {
        val frames = v.stackTrace
        // Ruído do sistema (fontes do MIUI): nenhuma linha do app na pilha, nada que possamos corrigir.
        if (frames.none { it.className.startsWith("com.vertice") } && frames.any { it.className.startsWith("miui.") }) return
        val key = frames.take(8).joinToString("|") { it.toString() }.hashCode()
        synchronized(seen) {
            if (seen.size > 200 || !seen.add(key)) return
        }
        VLog.d("StrictMode", "$kind: ${v.javaClass.simpleName}\n${LogFormat.stack(frames, 12)}")
    }
}
