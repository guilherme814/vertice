package com.vertice.launcher.diag

import android.content.Context
import android.util.Log
import java.io.File

/** Grava um relatório completo antes de o processo morrer por exceção não tratada. */
object CrashReporter {

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val now = System.currentTimeMillis()
                val text = buildString {
                    appendLine("CRASH ${LogFormat.fileStamp(now)}")
                    appendLine("Thread: ${thread.name}")
                    appendLine(SystemSnapshot.header(app))
                    appendLine("Memória: ${SystemSnapshot.memoryLine(app)}")
                    appendLine()
                    appendLine(Log.getStackTraceString(throwable))
                    appendLine("--- últimas linhas do log ---")
                    VLog.recent(150).forEach { appendLine(it) }
                }
                VLog.directory()?.let { dir ->
                    File(dir, "crash-${LogFormat.fileStamp(now)}.txt").writeText(text)
                    dir.listFiles { f -> f.name.startsWith("crash-") }
                        ?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
                }
                VLog.e("Crash", "Exceção não tratada em '${thread.name}'", throwable)
                VLog.flush(500)
            } catch (_: Throwable) {
                // Nada pode impedir o sistema de encerrar o processo normalmente.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
