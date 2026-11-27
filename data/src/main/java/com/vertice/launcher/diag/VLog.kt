package com.vertice.launcher.diag

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Registro da launcher. Regras de projeto:
 *  - nunca bloqueia a interface (fila + thread própria) e nunca derruba o app (tudo protegido);
 *  - arquivo com rotação (poucos MB no total), em armazenamento interno, sem permissão;
 *  - só sai do aparelho quando o usuário exporta/compartilha.
 */
object VLog {
    private const val MAX_FILE = 512 * 1024L
    private const val KEEP_ARCHIVES = 4
    private const val PREFIX = "vertice-log-"
    const val CURRENT = "vertice-log-current.txt"

    private object Clear
    private val ring = LineRing(500)
    private val queue = ArrayBlockingQueue<Any>(4000)
    private val dropped = AtomicInteger(0)

    @Volatile private var dir: File? = null
    @Volatile private var prefsStore: android.content.SharedPreferences? = null

    /** Detalhado: inclui linhas de depuração (D). Ligado por padrão enquanto estamos caçando problemas. */
    @Volatile var verbose: Boolean = true
        private set

    @Synchronized
    fun init(context: Context) {
        if (dir != null) return
        val d = File(context.applicationContext.filesDir, "logs").apply { mkdirs() }
        prefsStore = context.applicationContext.getSharedPreferences("diag", Context.MODE_PRIVATE)
        verbose = prefsStore?.getBoolean("verbose", true) ?: true
        dir = d
        Thread({ writerLoop(d) }, "vertice-log").apply { isDaemon = true; start() }
    }

    fun setVerbose(on: Boolean) {
        verbose = on
        try { prefsStore?.edit()?.putBoolean("verbose", on)?.apply() } catch (_: Throwable) {}
        i("Log", "Registro detalhado ${if (on) "ligado" else "desligado"}")
    }

    fun d(tag: String, msg: String) = log(LogLevel.Debug, tag, msg, null)
    fun i(tag: String, msg: String) = log(LogLevel.Info, tag, msg, null)
    fun w(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.Warn, tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = log(LogLevel.Error, tag, msg, t)

    fun log(level: LogLevel, tag: String, msg: String, t: Throwable?) {
        try {
            if (level == LogLevel.Debug && !verbose) return
            val full = if (t != null) msg + "\n" + Log.getStackTraceString(t) else msg
            val line = LogFormat.line(System.currentTimeMillis(), level, tag, Thread.currentThread().name, full)
            ring.add(line)
            Log.println(
                when (level) { LogLevel.Debug -> Log.DEBUG; LogLevel.Info -> Log.INFO; LogLevel.Warn -> Log.WARN; LogLevel.Error -> Log.ERROR },
                "Vertice/$tag", full
            )
            if (!queue.offer(line)) dropped.incrementAndGet()
        } catch (_: Throwable) {
            // O registro jamais pode causar um problema.
        }
    }

    /** Últimas linhas em memória (mais novas por último). */
    fun recent(n: Int = 60): List<String> = ring.last(n)

    fun directory(): File? = dir

    /** Espera (até [timeoutMs]) a thread de escrita esvaziar a fila — usado antes de exportar e no crash. */
    fun flush(timeoutMs: Long = 800) {
        try {
            val latch = CountDownLatch(1)
            if (queue.offer(latch)) latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: Throwable) {}
    }

    /** Arquivos de log do mais antigo para o mais novo (o atual por último). */
    fun logFiles(): List<File> {
        val d = dir ?: return emptyList()
        val all = d.listFiles { f -> f.name.startsWith(PREFIX) }?.toList() ?: return emptyList()
        val (current, archives) = all.partition { it.name == CURRENT }
        return archives.sortedBy { it.lastModified() } + current
    }

    /** Relatórios de crash e de ANR, do mais antigo para o mais novo. */
    fun reportFiles(): List<File> {
        val d = dir ?: return emptyList()
        return (d.listFiles { f -> f.name.startsWith("crash-") || f.name.startsWith("anr-") }?.toList() ?: emptyList())
            .sortedBy { it.lastModified() }
    }

    fun totalBytes(): Long = (logFiles() + reportFiles()).sumOf { it.length() }

    fun clear() {
        ring.clear()
        try { queue.offer(Clear) } catch (_: Throwable) {}
        flush()
    }

    // ---- thread de escrita ----

    private fun writerLoop(d: File) {
        var writer: BufferedWriter? = null
        var written = 0L
        val current = File(d, CURRENT)

        fun close() { try { writer?.close() } catch (_: Throwable) {}; writer = null }

        fun open() {
            if (writer == null) {
                written = current.length()
                writer = BufferedWriter(FileWriter(current, true))
            }
        }

        fun rotate() {
            close()
            val stamp = LogFormat.fileStamp(System.currentTimeMillis())
            var target = File(d, "$PREFIX$stamp.txt")
            var n = 1
            while (target.exists()) target = File(d, "$PREFIX$stamp-${n++}.txt")
            current.renameTo(target)
            val archives = d.listFiles { f -> f.name.startsWith(PREFIX) && f.name != CURRENT }
                ?.sortedByDescending { it.lastModified() } ?: return
            archives.drop(KEEP_ARCHIVES).forEach { it.delete() }
        }

        while (true) {
            val item = try { queue.take() } catch (_: InterruptedException) { return }
            try {
                when (item) {
                    is String -> {
                        open()
                        val w = writer ?: continue
                        w.write(item); w.write("\n")
                        written += item.length + 1
                        val lost = dropped.getAndSet(0)
                        if (lost > 0) { w.write("… $lost linhas descartadas (fila cheia)\n") }
                        if (written > MAX_FILE) rotate()
                    }
                    is CountDownLatch -> { writer?.flush(); item.countDown() }
                    Clear -> {
                        close()
                        d.listFiles { f -> f.name.startsWith(PREFIX) || f.name.startsWith("crash-") || f.name.startsWith("anr-") }
                            ?.forEach { it.delete() }
                    }
                }
                if (queue.isEmpty()) writer?.flush()
            } catch (t: Throwable) {
                // Disco cheio etc.: descarta esta linha e tenta de novo na próxima.
                close()
                if (item is CountDownLatch) item.countDown()
            }
        }
    }
}
