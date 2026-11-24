package com.vertice.launcher.diag

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Junta tudo (cabeçalho, crashes, ANRs, logs) em UM arquivo de texto e o entrega ao usuário. */
object LogExporter {

    fun buildReport(context: Context): String {
        VLog.flush()
        val now = System.currentTimeMillis()
        return buildString {
            appendLine("RELATÓRIO VÉRTICE LAUNCHER — gerado em ${LogFormat.fileStamp(now)}")
            appendLine(SystemSnapshot.header(context))
            appendLine("Memória agora: ${SystemSnapshot.memoryLine(context)}")
            appendLine("Registro detalhado: ${VLog.verbose}")
            appendLine("Observação: contém nomes de pacotes de apps abertos; revise antes de compartilhar publicamente.")
            val reports = VLog.reportFiles()
            if (reports.isNotEmpty()) {
                appendLine()
                appendLine("##### RELATÓRIOS DE CRASH / ANR (${reports.size}) #####")
                reports.forEach { f ->
                    appendLine("----- ${f.name} -----")
                    appendLine(safeRead(f))
                }
            }
            appendLine()
            appendLine("##### LOG #####")
            VLog.logFiles().forEach { f ->
                appendLine("----- ${f.name} -----")
                append(safeRead(f))
            }
        }
    }

    /** Salva em Downloads (Android 10+, sem permissão). Devolve o nome do arquivo, ou null se não der. */
    fun saveToDownloads(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val name = "vertice-log-${LogFormat.fileStamp(System.currentTimeMillis())}.txt"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(buildReport(context).toByteArray()) } ?: return null
        VLog.i("Exportar", "Log salvo em Downloads/$name")
        return name
    }

    /** Cópia no cache do app para compartilhar via FileProvider (funciona em qualquer versão). */
    fun writeShareFile(context: Context): File {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "vertice-log-${LogFormat.fileStamp(System.currentTimeMillis())}.txt")
        f.writeText(buildReport(context))
        VLog.i("Exportar", "Arquivo para compartilhar: ${f.name} (${f.length() / 1024}KB)")
        return f
    }

    private fun safeRead(f: File): String = try { f.readText() } catch (t: Throwable) { "(erro ao ler ${f.name}: ${t.javaClass.simpleName})" }
}
