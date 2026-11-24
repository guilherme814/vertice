package com.vertice.launcher.diag

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File

/**
 * Por que o processo anterior morreu (Android 11+): travamento (ANR), pouca memória, crash nativo, usuário...
 * É o dado mais direto para separar "a launcher travou" de "o sistema matou a launcher".
 */
object ExitReasons {

    private val names = mapOf(
        0 to "DESCONHECIDO", 1 to "SAIU_SOZINHO", 2 to "SINAL(kill)", 3 to "POUCA_MEMORIA", 4 to "CRASH",
        5 to "CRASH_NATIVO", 6 to "ANR(travou)", 7 to "FALHA_NA_INICIALIZACAO", 8 to "MUDANCA_DE_PERMISSAO",
        9 to "USO_EXCESSIVO_DE_RECURSO", 10 to "PEDIDO_DO_USUARIO", 11 to "USUARIO_PAROU", 12 to "DEPENDENCIA_MORREU",
        13 to "OUTRO", 14 to "CONGELADO_PELO_SISTEMA", 15 to "ESTADO_DO_PACOTE", 16 to "PACOTE_ATUALIZADO"
    )

    fun logNew(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            VLog.i("Saída", "Motivo da morte do processo anterior indisponível (precisa de Android 11+)")
            return
        }
        try {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences("diag", Context.MODE_PRIVATE)
            val last = prefs.getLong("last_exit_ts", 0L)
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val list = am.getHistoricalProcessExitReasons(app.packageName, 0, 10).sortedBy { it.timestamp }
            var newest = last
            val fresh = list.filter { it.timestamp > last }
            if (fresh.isEmpty()) VLog.i("Saída", "Nenhuma morte de processo nova desde a última sessão")
            for (e in fresh) {
                VLog.w(
                    "Saída",
                    "Processo anterior terminou: motivo=${names[e.reason] ?: e.reason} status=${e.status} " +
                        "importância=${e.importance} pss=${e.pss / 1024}MB rss=${e.rss / 1024}MB " +
                        "quando=${LogFormat.fileStamp(e.timestamp)} descrição='${e.description}'"
                )
                if (e.reason == 6 /* ANR */) saveTrace(e)
                newest = maxOf(newest, e.timestamp)
            }
            prefs.edit().putLong("last_exit_ts", newest).apply()
        } catch (t: Throwable) {
            VLog.w("Saída", "Não foi possível ler os motivos de saída", t)
        }
    }

    private fun saveTrace(e: android.app.ApplicationExitInfo) {
        try {
            val dir = VLog.directory() ?: return
            val bytes = e.traceInputStream?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (out.size() < 64 * 1024) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: return
            File(dir, "anr-${LogFormat.fileStamp(e.timestamp)}.txt").writeBytes(bytes)
            dir.listFiles { f -> f.name.startsWith("anr-") }
                ?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { it.delete() }
        } catch (t: Throwable) {
            VLog.w("Saída", "Falha ao salvar o rastro do ANR", t)
        }
    }
}
